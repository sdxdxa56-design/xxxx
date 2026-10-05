package com.appinspector.services

import com.appinspector.domain.model.AppError
import com.appinspector.domain.model.ErrorSeverity
import java.util.regex.Pattern

object ErrorAnalyzer {

    private val CRASH_PATTERN = Pattern.compile("FATAL EXCEPTION: (.*)|Process: ([a-zA-Z0-9_.]+), PID:")
    private val ANR_PATTERN = Pattern.compile("ANR in ([a-zA-Z0-9_.]+)")

    fun parseLogLine(line: String): AppError? {
        if (line.contains("FATAL EXCEPTION") || line.contains("AndroidRuntime")) {
            val packageName = extractPackage(line) ?: "system.crash"
            return AppError(
                packageName = packageName,
                appName = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                tag = "AndroidRuntime",
                errorType = "Fatal Crash",
                message = line.take(300),
                stackTrace = line,
                timestamp = System.currentTimeMillis(),
                severity = ErrorSeverity.CRASH,
                solutionSuggestion = generateSuggestion(line)
            )
        }

        if (line.contains("ANR in") || line.contains("ActivityManager: ANR")) {
            val packageName = extractPackage(line) ?: "system.anr"
            return AppError(
                packageName = packageName,
                appName = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                tag = "ActivityManager",
                errorType = "ANR (Application Not Responding)",
                message = line.take(300),
                stackTrace = line,
                timestamp = System.currentTimeMillis(),
                severity = ErrorSeverity.ANR,
                solutionSuggestion = "The main thread is blocked by heavy I/O or deadlock. Move operations to background coroutine Dispatchers.IO."
            )
        }

        return null
    }

    private fun extractPackage(line: String): String? {
        val matcher = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+)").matcher(line)
        while (matcher.find()) {
            val candidate = matcher.group(1)
            if (candidate.contains(".") && !candidate.startsWith("java.") && !candidate.startsWith("android.")) {
                return candidate
            }
        }
        return null
    }

    private fun generateSuggestion(stackTrace: String): String {
        return when {
            stackTrace.contains("NullPointerException") -> "Null safety check needed: verify optional fields before dereferencing."
            stackTrace.contains("IndexOutOfBoundsException") -> "Index bounds validation needed before accessing collections."
            stackTrace.contains("ClassNotFoundException") -> "Missing dependency or ProGuard rule issue."
            stackTrace.contains("SecurityException") -> "Missing runtime permission or security configuration in manifest."
            else -> "Inspect the full stack trace to locate failure point."
        }
    }
}
