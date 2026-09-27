package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class TodoQuickCaptureTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "提取待办"
            subtitle = if (com.xiaomanjun.sleepdownschedule.feature.experimental.XiaomiShizukuBridge.isAuthorized()) {
                "点按截取当前屏幕"
            } else {
                "需要 Shizuku 授权"
            }
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val bridge = com.xiaomanjun.sleepdownschedule.feature.experimental.XiaomiShizukuBridge
        if (!bridge.isRunning() || !bridge.isAuthorized()) {
            startActivityAndCollapse(
                Intent(this, TodoActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(TodoQuickCaptureContract.EXTRA_REQUEST_SHIZUKU, true)
            )
            return
        }
        TodoScreenshotCapture.capture(this) { captured ->
            Handler(Looper.getMainLooper()).post {
                captured.fold(
                    onSuccess = { file ->
                        startActivityAndCollapse(
                            Intent(this, TodoActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                .putExtra(TodoQuickCaptureContract.EXTRA_SCREENSHOT_PATH, file.absolutePath)
                        )
                    },
                    onFailure = { error ->
                        Toast.makeText(this, error.message ?: "屏幕提取失败", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}
