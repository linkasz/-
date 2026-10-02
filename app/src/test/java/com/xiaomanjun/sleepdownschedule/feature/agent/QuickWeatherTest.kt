package com.xiaomanjun.sleepdownschedule.feature.agent

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class QuickWeatherTest {
    @Test fun equalRequestsShareOneFetchAndExpireAtFiveSeconds() = runBlocking {
        var now = 100L; var reads = 0
        val flight = WeatherSingleFlight<Int>({ now })
        val values = (1..3).map { async { flight.get("长沙|2026-10-01") { delay(20); ++reads } } }.awaitAll()
        assertEquals(listOf(1, 1, 1), values)
        now += 4_999; assertEquals(1, flight.get("长沙|2026-10-01") { ++reads })
        now++; assertEquals(2, flight.get("长沙|2026-10-01") { ++reads })
        assertEquals(3, flight.get("北京|2026-10-01") { ++reads })
    }
    @Test fun cancellationDoesNotCacheAnIncompleteResultOrKeepTheLock() = runBlocking {
        val flight = WeatherSingleFlight<Int>({ 100L })
        val started = CompletableDeferred<Unit>()
        val job = launch { flight.get("x") { started.complete(Unit); delay(10_000); 1 } }
        started.await(); job.cancelAndJoin()
        assertEquals(2, flight.get("x") { 2 })
    }
    @Test fun intentUsesRealDateAndDoesNotSwallowCombinedTasks() {
        val date = LocalDate.of(2026, 10, 1)
        assertEquals(null to date, quickWeatherIntent("今天天气怎么样？", date))
        assertEquals("长沙" to date.plusDays(1), quickWeatherIntent("长沙明天的天气如何", date))
        assertEquals(null to date, quickWeatherIntent("我这里今天的天气怎么样呢？", date))
        assertNull(quickWeatherIntent("今天天气会下雨吗", date))
        assertNull(quickWeatherIntent("你能说说天气不错吗", date))
        assertNull(quickWeatherIntent("今天天气怎么样，同时新建待办", date))
        assertNull(quickWeatherIntent("这个月有什么课程", date))
    }
    @Test fun forecastSelectsExactDayAndNeverCopiesCurrentTemperatureIntoTomorrow() {
        val root = Json.parseToJsonElement("""{"timezone":"Asia/Shanghai","current":{"time":"2026-10-01T12:00","temperature_2m":22},"daily":{"time":["2026-10-01","2026-10-02"],"weather_code":[2,61],"temperature_2m_min":[18,17],"temperature_2m_max":[26,24],"precipitation_probability_max":[10,80]}}""").jsonObject
        val today = quickWeatherPayload(root, "长沙", LocalDate.of(2026,10,1), "trace")
        assertTrue(today["ok"]!!.jsonPrimitive.boolean)
        assertEquals(22.0, today["temperature"]!!.jsonObject["current"]!!.jsonPrimitive.double, 0.01)
        val tomorrow = quickWeatherPayload(root, "长沙", LocalDate.of(2026,10,2), "trace")
        assertEquals(JsonNull, tomorrow["temperature"]!!.jsonObject["current"])
        assertEquals(JsonNull, tomorrow["current"])
        assertTrue(tomorrow["text"]!!.jsonPrimitive.content.contains("80%"))
    }
    @Test(expected = IllegalArgumentException::class) fun missingForecastDayDoesNotInventWeather() {
        quickWeatherPayload(Json.parseToJsonElement("""{"daily":{"time":[]}}""").jsonObject, "长沙", LocalDate.of(2026,10,1), "trace")
    }
    @Test fun portableToolDefinitionExposesCityDateWithoutAProviderInvalidDotName() {
        val function = agentToolDefinitions().map { it.jsonObject["function"]!!.jsonObject }
            .single { it["name"]!!.jsonPrimitive.content == "WEATHER_QUICK" }
        assertTrue(function["description"]!!.jsonPrimitive.content.contains("weather.quick"))
        assertEquals(setOf("city", "date"), function["parameters"]!!.jsonObject["properties"]!!.jsonObject.keys)
    }
    @Test fun malformedToolDateRemainsAnExplicitArgumentToValidateRatherThanGuess() {
        val call = parseAgentToolDecision("""{"choices":[{"message":{"content":"","tool_calls":[{"id":"id","type":"function","function":{"name":"weather.quick","arguments":{"city":"长沙","date":"not-a-date"}}}]},"finish_reason":"tool_calls"}]}""").calls.single()
        assertEquals(AgentToolName.WEATHER_QUICK, call.name)
        assertEquals("not-a-date", call.arguments["date"])
    }
}
