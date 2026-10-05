package com.appinspector.services

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object ProcessTracker {

    fun findPidForPackage(packageName: String): Int? {
        if (packageName.isBlank()) return null

        try {
            val procDir = File("/proc")
            val pidDirs = procDir.listFiles { file: File -> file.isDirectory && file.name.all { it.isDigit() } }
            if (pidDirs != null) {
                for (dir in pidDirs) {
                    val cmdlineFile = File(dir, "cmdline")
                    if (cmdlineFile.exists() && cmdlineFile.canRead()) {
                        val cmdline = cmdlineFile.readText().replace('\u0000', ' ').trim()
                        if (cmdline == packageName || cmdline.startsWith("$packageName:") || cmdline.startsWith(packageName)) {
                            return dir.name.toIntOrNull()
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        try {
            val result = Shell.cmd("pidof $packageName").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                val pidStr = result.out.firstOrNull()?.trim()?.split(" ")?.firstOrNull()
                val pid = pidStr?.toIntOrNull()
                if (pid != null) return pid
            }

            val psResult = Shell.cmd("ps -A | grep $packageName").exec()
            for (line in psResult.out) {
                val tokens = line.trim().split(Regex("\\s+"))
                if (tokens.size >= 2 && tokens.any { it.contains(packageName) }) {
                    val candidatePid = tokens[1].toIntOrNull()
                    if (candidatePid != null) return candidatePid
                }
            }
        } catch (_: Exception) {
        }

        try {
            val process = ProcessBuilder("ps", "-A").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line.orEmpty()
                if (currentLine.contains(packageName)) {
                    val tokens = currentLine.trim().split(Regex("\\s+"))
                    if (tokens.size >= 2) {
                        val candidatePid = tokens[1].toIntOrNull()
                        if (candidatePid != null) return candidatePid
                    }
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    suspend fun waitForPid(
        packageName: String,
        timeoutMs: Long = 30000,
        pollIntervalMs: Long = 500
    ): Int? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val pid = findPidForPackage(packageName)
            if (pid != null && pid > 0) {
                return@withContext pid
            }
            delay(pollIntervalMs)
        }
        null
    }
}
