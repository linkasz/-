package com.xiaomanjun.sleepdownschedule.feature.agent

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun WeatherLocationPermissionPrompt() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("shixu_weather_permission", 0) }
    var visible by remember { mutableStateOf(!preferences.getBoolean("explained", false) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) android.widget.Toast.makeText(context, "未开启定位，仍可告诉助手城市来查天气", android.widget.Toast.LENGTH_LONG).show()
    }
    fun dismiss() { preferences.edit().putBoolean("explained", true).apply(); visible = false }
    if (visible) AlertDialog(onDismissRequest = ::dismiss,
        title = { Text("使用当前位置查询天气") },
        text = { Text("授予大致位置后，助手可在你询问天气时查询当前位置预报。查询时坐标会发送给 Open-Meteo，不持续追踪，不写入日志或备份。拒绝后仍可指定城市查询。") },
        confirmButton = { TextButton(onClick = { dismiss(); request.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Text("允许定位") } },
        dismissButton = { TextButton(onClick = ::dismiss) { Text("暂不开启") } })
}
