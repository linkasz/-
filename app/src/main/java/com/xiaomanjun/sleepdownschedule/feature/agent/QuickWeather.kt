package com.xiaomanjun.sleepdownschedule.feature.agent

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** One shared flight per process; retained results merge equal intentions for five seconds. */
internal class WeatherSingleFlight<T>(private val now: () -> Long, private val ttl: Long = 5_000L) {
    private val lock = Mutex()
    private val results = LinkedHashMap<String, Pair<Long, T>>()
    suspend fun get(key: String, fetch: suspend () -> T): T = lock.withLock {
        results[key]?.takeIf { now() - it.first in 0 until ttl }?.let { return@withLock it.second }
        val result = fetch()
        results[key] = now() to result
        while (results.size > 16) results.remove(results.keys.first())
        result
    }
}

internal fun weatherDescription(code: Int): String = when (code) {
    0 -> "晴朗"; 1, 2 -> "多云"; 3 -> "阴天"; 45, 48 -> "雾"
    51, 53, 55, 56, 57 -> "毛毛雨"; 61, 63, 65, 66, 67, 80, 81, 82 -> "雨"
    71, 73, 75, 77, 85, 86 -> "雪"; 95, 96, 99 -> "雷雨"; else -> "天气状况未知"
}

internal fun quickWeatherPayload(root: JsonObject, city: String, date: LocalDate, traceId: String): JsonObject {
    val daily = root["daily"]?.jsonObject ?: error("天气服务未返回每日预报")
    val index = daily["time"]?.jsonArray?.indexOfFirst { it.jsonPrimitive.content == date.toString() } ?: -1
    require(index >= 0) { "指定日期没有可用预报" }
    fun dailyNumber(key: String): Double = daily[key]?.jsonArray?.getOrNull(index)?.jsonPrimitive?.doubleOrNull
        ?: error("天气服务缺少 $key")
    val low = dailyNumber("temperature_2m_min")
    val high = dailyNumber("temperature_2m_max")
    val probability = dailyNumber("precipitation_probability_max")
    val description = weatherDescription(dailyNumber("weather_code").toInt())
    val current = (root["current"] as? JsonObject)
        ?.takeIf { it["time"]?.jsonPrimitive?.contentOrNull?.take(10) == date.toString() }
    val currentTemperature = current?.get("temperature_2m")?.jsonPrimitive?.doubleOrNull
    val text = "$city $date $description，${low}～${high}℃，降水概率 ${probability.toInt()}%。"
    return buildJsonObject {
        put("ok", true); put("city", city); put("date", date.toString()); put("text", text); put("description", description)
        put("traceId", traceId); put("source", "Open-Meteo"); put("timezone", root["timezone"] ?: JsonNull)
        put("temperature", buildJsonObject { put("min", low); put("max", high); put("current", currentTemperature?.let(::JsonPrimitive) ?: JsonNull) })
        put("precipitationProbability", probability)
        // Never invent a wind direction or grade from a speed alone.
        put("current", current ?: JsonNull)
    }
}

internal fun naturalWeatherReply(payload: JsonObject, today: LocalDate, persona: AgentPersona = AgentPersona()): String {
    if (payload["ok"]?.jsonPrimitive?.booleanOrNull != true)
        return payload["text"]?.jsonPrimitive?.contentOrNull ?: "暂时查不到天气，稍后再试一下吧。"
    val date = payload["date"]?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val whenText = when (date) { today -> "今天"; today.plusDays(1) -> "明天"; today.minusDays(1) -> "昨天"; else -> date?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: "这一天" }
    val city = payload["city"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val place = if (city.isBlank() || city == "当前位置") "你这边" else city
    val temperatures = payload["temperature"] as? JsonObject
    fun number(key: String): String? = temperatures?.get(key)?.jsonPrimitive?.doubleOrNull
        ?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }
    val low = number("min"); val high = number("max")
    val description = payload["description"]?.jsonPrimitive?.contentOrNull ?: "天气状况暂不明确"
    val probability = payload["precipitationProbability"]?.jsonPrimitive?.doubleOrNull
    val summary = "$place$whenText${if (description == "雨") "有雨" else description}${if (low != null && high != null) "，$low～$high℃" else ""}。"
    if (persona.style == "concise") return summary + (probability?.let { "降水概率${it.toInt()}%。" } ?: "")
    val advice = when {
        description == "雷雨" -> "出门带上伞，遇到雷雨尽量避开空旷处。"
        probability != null && probability >= 60 -> "下雨的可能性比较高，出门记得带伞。"
        description == "雪" -> "出门注意保暖和路面湿滑。"
        temperatures?.get("max")?.jsonPrimitive?.doubleOrNull?.let { it >= 32 } == true -> "天气比较热，外出注意防晒和补水。"
        else -> ""
    }
    return summary + advice
}

internal object QuickWeather {
    private val flight = WeatherSingleFlight<JsonObject>(SystemClock::elapsedRealtime)
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    suspend fun query(context: Context, city: String?, date: LocalDate, traceId: String): JsonObject {
        val name = city?.trim().orEmpty()
        Log.i("ShixuWeather", "traceId=$traceId intent=weather.quick city=${if (name.isEmpty()) "device_location" else name.take(80)} date=$date query_start")
        val validation = when {
            name.length > 80 -> "城市名称过长"
            date.isBefore(LocalDate.now().minusDays(1)) || date.isAfter(LocalDate.now().plusDays(15)) -> "只支持当前可用的近日天气预报"
            else -> null
        }
        if (validation != null) {
            Log.i("ShixuWeather", "traceId=$traceId query_end ok=false type=validation")
            return buildJsonObject { put("ok", false); put("traceId", traceId); put("text", validation) }
        }
        var fetched = false
        val payload = flight.get("${name.lowercase(java.util.Locale.ROOT)}|$date") {
            fetched = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val point = if (name.isEmpty()) {
                        val location = currentWeatherLocation(context) ?: error("无法获取当前位置，请开启定位服务并授予大致位置权限，或告诉我城市")
                        Triple(location.latitude, location.longitude, "当前位置")
                    } else {
                        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
                            .addQueryParameter("name", name).addQueryParameter("count", "1")
                            .addQueryParameter("language", "zh").addQueryParameter("format", "json").build()
                        val found = request(url.toString())["results"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?: error("没有找到城市：$name，请提供更明确的城市名称")
                        Triple(found.getValue("latitude").jsonPrimitive.double, found.getValue("longitude").jsonPrimitive.double,
                            found["name"]?.jsonPrimitive?.contentOrNull ?: name)
                    }
                    val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
                        .addQueryParameter("latitude", point.first.toString()).addQueryParameter("longitude", point.second.toString())
                        .addQueryParameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m")
                        .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                        .addQueryParameter("timezone", "auto").addQueryParameter("start_date", date.toString()).addQueryParameter("end_date", date.toString()).build()
                    quickWeatherPayload(request(url.toString()), point.third, date, traceId)
                }
                Log.i("ShixuWeather", "traceId=$traceId query_end ok=true")
                result
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                Log.w("ShixuWeather", "traceId=$traceId query_end ok=false type=${error.javaClass.simpleName}")
                buildJsonObject { put("ok", false); put("traceId", traceId); put("text", error.message ?: "天气查询失败，请稍后再试") }
            }
        }
        if (!fetched) Log.i("ShixuWeather", "traceId=$traceId query_end ok=${payload["ok"]} merged=true")
        // Cached facts belong to this request even when the network flight had another trace.
        return JsonObject(payload + ("traceId" to JsonPrimitive(traceId)))
    }

    private suspend fun request(url: String): JsonObject {
        for (attempt in 0..1) {
            try {
                return client.newCall(Request.Builder().url(url).build()).awaitWeather().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code >= 500 || response.code == 429) throw IOException("天气服务暂时不可用（${response.code}）")
                        error("天气查询被拒绝（${response.code}）")
                    }
                    Json.parseToJsonElement(response.body?.string() ?: error("天气服务返回空内容")).jsonObject
                }
            } catch (error: IOException) {
                if (attempt == 1) throw IOException("天气网络查询失败，已重试一次", error)
                delay(250)
            }
        }
        error("天气查询失败")
    }
}

private suspend fun okhttp3.Call.awaitWeather(): okhttp3.Response = suspendCancellableCoroutine { c ->
    c.invokeOnCancellation { cancel() }
    enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) { if (c.isActive) c.resumeWith(Result.failure(e)) }
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            if (c.isActive) c.resume(response) { _, value, _ -> value.close() } else response.close()
        }
    })
}

@SuppressLint("MissingPermission")
internal suspend fun currentWeatherLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = manager.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
    val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }
        .maxByOrNull { it.elapsedRealtimeNanos }
    if (recent != null) return recent
    val provider = providers.firstOrNull { it == LocationManager.NETWORK_PROVIDER } ?: providers.firstOrNull() ?: return null
    return withTimeoutOrNull(8_000) {
        suspendCancellableCoroutine { c ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (c.isActive) c.resume(location)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Legacy listener") override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
            }
            c.invokeOnCancellation { manager.removeUpdates(listener) }
            try {
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                // Cancellation can race registration; remove a listener registered after cancel.
                if (!c.isActive) manager.removeUpdates(listener)
            }
            catch (e: Exception) { manager.removeUpdates(listener); if (c.isActive) c.resume(null) }
        }
    }
}
