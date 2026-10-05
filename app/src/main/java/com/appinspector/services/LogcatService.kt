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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val rootReader = RootLogcatReader()
    private var useRoot = false
    private var standardProcess: Process? = null

    private val crashBuffer = StringBuilder()
    private var isCollecting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        RootManager.checkRoot(object : RootCallback {
            override fun onResult(available: Boolean, granted: Boolean) {
                useRoot = available && granted
                startReadingLogcat()
            }
        })
    }

    private fun startReadingLogcat() {
        if (useRoot) {
            serviceScope.launch {
                rootReader.startReading()
                    .catch {
                        startStandardStream()
                    }
                    .collectLatest { line ->
                        processLine(line)
                    }
            }
        } else {
            startStandardStream()
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
                        processLine(currentLine)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun processLine(line: String) {
        _latestLogLine.value = line
        if (ErrorAnalyzer.isCriticalErrorStart(line)) {
            if (isCollecting && crashBuffer.isNotEmpty()) {
                saveCrash(crashBuffer.toString())
                crashBuffer.clear()
            }
            isCollecting = true
            crashBuffer.append(line).append("\n")
            return
        }
        if (isCollecting) {
            if (line.contains("at ") || line.contains("AndroidRuntime")
                || line.contains("Process:")
                || line.trim().startsWith("java.")) {
                crashBuffer.append(line).append("\n")
            } else {
                saveCrash(crashBuffer.toString())
                crashBuffer.clear()
                isCollecting = false
            }
        }
    }

    private fun saveCrash(chunk: String) {
        val parsed = ErrorAnalyzer.parseCrashChunk(chunk) ?: return
        serviceScope.launch {
            (application as AppInspectorApp).errorRepository.saveError(parsed)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AppInspectorApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("App Inspector Running")
            .setContentText("Actively monitoring system and app errors in real-time")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        rootReader.stopReading()
        try {
            standardProcess?.destroy()
            standardProcess = null
        } catch (_: Exception) {
        }
        if (isCollecting && crashBuffer.isNotEmpty()) {
            saveCrash(crashBuffer.toString())
            crashBuffer.clear()
        }
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _latestLogLine = MutableStateFlow("")
        val latestLogLine: StateFlow<String> = _latestLogLine.asStateFlow()

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
