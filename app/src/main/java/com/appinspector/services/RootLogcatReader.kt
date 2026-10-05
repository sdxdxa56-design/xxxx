package com.appinspector.services

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class RootLogcatReader {

    @Volatile
    private var process: Process? = null

    fun startReading(): Flow<String> = callbackFlow {
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                close(IllegalStateException("Root permission is not available or granted"))
                return@getShell
            }

            val job = launch(Dispatchers.IO) {
                try {
                    val command = arrayOf("su", "-c", "logcat -v time *:E *:F AndroidRuntime:E ActivityManager:E")
                    val proc = ProcessBuilder(*command)
                        .redirectErrorStream(true)
                        .start()
                    process = proc

                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    var line: String?

                    while (isActive && reader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (!currentLine.isNullOrBlank()) {
                            trySend(currentLine)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        close(e)
                    }
                } finally {
                    stopReading()
                }
            }

            invokeOnClose {
                job.cancel()
                stopReading()
            }
        }

        awaitClose {
            stopReading()
        }
    }.flowOn(Dispatchers.IO)

    fun stopReading() {
        try {
            process?.destroy()
            process = null
        } catch (_: Exception) {
        }
    }
}
