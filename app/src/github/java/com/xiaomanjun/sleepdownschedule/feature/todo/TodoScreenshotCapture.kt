package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import androidx.annotation.Keep
import com.xiaomanjun.sleepdownschedule.feature.experimental.XiaomiShizukuBridge
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import rikka.shizuku.Shizuku

/** Runs Android's user-confirmed screencap command through Shizuku's shell-identity UserService. */
internal object TodoScreenshotCapture {
    private val executor = Executors.newSingleThreadExecutor()
    private const val DESCRIPTOR = "com.scheduleplus.student.todo.IScreenshotCapture"
    private const val TRANSACTION_CAPTURE = IBinder.FIRST_CALL_TRANSACTION

    fun capture(context: Context, result: (Result<File>) -> Unit) {
        if (!XiaomiShizukuBridge.isRunning() || !XiaomiShizukuBridge.isAuthorized()) {
            result(Result.failure(IllegalStateException("请先启动并授权 Shizuku")))
            return
        }
        val directory = File(context.getExternalFilesDir(null), "quick_capture").apply { mkdirs() }
        directory.listFiles().orEmpty().forEach { it.delete() }
        val output = File(directory, "screen-${System.currentTimeMillis()}.png")
        val args = Shizuku.UserServiceArgs(ComponentName(context, TodoScreenshotUserService::class.java))
            .daemon(false)
            .processNameSuffix("scheduleplus_screenshot")
            .tag("scheduleplus-screenshot")
            .version(1)
        val completed = AtomicBoolean(false)
        lateinit var connection: ServiceConnection
        fun finish(captured: Result<File>) {
            if (completed.compareAndSet(false, true)) result(captured)
        }
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                executor.execute {
                    val captured = runCatching {
                        val request = Parcel.obtain()
                        val response = Parcel.obtain()
                        try {
                            request.writeInterfaceToken(DESCRIPTOR)
                            request.writeString(output.absolutePath)
                            check(service.transact(TRANSACTION_CAPTURE, request, response, 0)) {
                                "Shizuku 没有返回截图结果"
                            }
                            response.readException()
                            val exitCode = response.readInt()
                            val error = response.readString()
                            check(exitCode == 0 && output.isFile && output.length() > 0L) {
                                error ?: "无法读取当前屏幕"
                            }
                            output
                        } finally {
                            request.recycle()
                            response.recycle()
                        }
                    }
                    runCatching { Shizuku.unbindUserService(args, connection, true) }
                    if (captured.isFailure) output.delete()
                    finish(captured)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                output.delete()
                finish(Result.failure(IllegalStateException("Shizuku 截图服务已断开")))
            }
        }
        runCatching { Shizuku.bindUserService(args, connection) }
            .onFailure { finish(Result.failure(it)) }
    }

    @Keep
    class TodoScreenshotUserService : Binder() {
        init {
            attachInterface(null, DESCRIPTOR)
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != TRANSACTION_CAPTURE) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(DESCRIPTOR)
            val path = data.readString() ?: error("截图路径为空")
            val response = runCatching {
                runCatching {
                    ProcessBuilder("/system/bin/cmd", "statusbar", "collapse").start().waitFor()
                }
                Thread.sleep(450L)
                val process = ProcessBuilder("/system/bin/screencap", "-p", path)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                exitCode to if (exitCode == 0) null else output.take(200)
            }.getOrElse { -1 to (it.message ?: "screencap 执行失败") }
            reply?.writeNoException()
            reply?.writeInt(response.first)
            reply?.writeString(response.second)
            return true
        }
    }
}
