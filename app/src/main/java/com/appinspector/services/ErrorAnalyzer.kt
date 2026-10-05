package com.appinspector.services

import com.appinspector.domain.model.AppError
import com.appinspector.domain.model.ErrorSeverity
import java.util.regex.Pattern

object ErrorAnalyzer {

    private val CRASH_START_PATTERNS = listOf(
        "FATAL EXCEPTION",
        "AndroidRuntime: FATAL",
        "Process: ",
        "beginning of crash",
        "ActivityManager: ANR"
    )

    fun isCriticalErrorStart(line: String): Boolean {
        return CRASH_START_PATTERNS.any { line.contains(it, ignoreCase = true) } ||
               line.contains("ANR in", ignoreCase = true)
    }

    fun parseCrashChunk(chunk: String): AppError? {
        if (chunk.isBlank()) return null

        val isAnr = chunk.contains("ANR in") || chunk.contains("ActivityManager: ANR")
        val severity = if (isAnr) ErrorSeverity.ANR else ErrorSeverity.CRASH
        val tag = if (isAnr) "ActivityManager" else "AndroidRuntime"
        val errorType = if (isAnr) "ANR (Application Not Responding)" else extractExceptionType(chunk) ?: "Fatal Crash"

        val packageName = extractPackage(chunk) ?: "unknown.package"
        val appName = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        val message = chunk.lines().firstOrNull { it.isNotBlank() }?.take(300) ?: "Crash detected"

        return AppError(
            packageName = packageName,
            appName = appName,
            tag = tag,
            errorType = errorType,
            message = message,
            stackTrace = chunk.trim(),
            timestamp = System.currentTimeMillis(),
            severity = severity,
            solutionSuggestion = generateSuggestion(chunk)
        )
    }

    fun parseLogLine(line: String): AppError? {
        return parseCrashChunk(line)
    }

    private fun extractExceptionType(chunk: String): String? {
        val pattern = Pattern.compile("([a-zA-Z0-9_.]+(?:Exception|Error|Throwable))")
        val matcher = pattern.matcher(chunk)
        if (matcher.find()) {
            return matcher.group(1).substringAfterLast('.')
        }
        return null
    }

    private fun extractPackage(chunk: String): String? {
        val processMatcher = Pattern.compile("Process:\\s*([a-zA-Z0-9_.]+)", Pattern.CASE_INSENSITIVE).matcher(chunk)
        if (processMatcher.find()) {
            return processMatcher.group(1)
        }

        val anrMatcher = Pattern.compile("ANR in\\s*([a-zA-Z0-9_.]+)", Pattern.CASE_INSENSITIVE).matcher(chunk)
        if (anrMatcher.find()) {
            return anrMatcher.group(1)
        }

        val matcher = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+)").matcher(chunk)
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
            stackTrace.contains("ActivityNotFoundException") -> "Target Activity or Intent action not declared or installed."
            stackTrace.contains("ANR") -> "The main thread is blocked by heavy I/O or deadlock. Move operations to background coroutines."
            else -> "Inspect the full stack trace to locate failure point and exception cause."
        }
    }
}
