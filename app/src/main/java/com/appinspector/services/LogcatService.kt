package com.appinspector.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.appinspector.AppInspectorApp
import com.appinspector.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var rootReader: RootLogcatReader? = null
    private var standardProcess: Process? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startLogcatStream()
    }

    private fun startLogcatStream() {
        serviceScope.launch {
            RootManager.hasRootPermissionAsync { hasRoot ->
                if (hasRoot) {
                    startRootStream()
                } else {
                    startStandardStream()
                }
            }
        }
    }

    private fun startRootStream() {
        val reader = RootLogcatReader()
        rootReader = reader
        serviceScope.launch {
            reader.startReading()
                .catch {
                    startStandardStream()
                }
                .collectLatest { line ->
                    handleLogLine(line)
                }
        }
    }

    private fun startStandardStream() {
        serviceScope.launch {
            try {
                val command = arrayOf("logcat", "-v", "time", "*:E", "*:F")
                val proc = ProcessBuilder(*command).start()
                standardProcess = proc
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line
                    if (!currentLine.isNullOrBlank()) {
                        handleLogLine(currentLine)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun handleLogLine(line: String) {
        val error = ErrorAnalyzer.parseLogLine(line) ?: return
        AppInspectorApp.instance.repository.insertError(error)
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AppInspectorApp.CHANNEL_SERVICE_ID)
            .setContentTitle("App Inspector Running")
            .setContentText("Actively monitoring system and app errors in real-time")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        rootReader?.stopReading()
        try {
            standardProcess?.destroy()
        } catch (_: Exception) {
        }
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LogcatService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LogcatService::class.java)
            context.stopService(intent)
        }
    }
}
