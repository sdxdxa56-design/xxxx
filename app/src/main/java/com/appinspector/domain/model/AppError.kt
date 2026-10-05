package com.appinspector.domain.model

enum class ErrorSeverity {
    CRASH,
    ANR,
    ERROR,
    WARNING
}

data class AppError(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val tag: String,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val timestamp: Long,
    val severity: ErrorSeverity,
    val solutionSuggestion: String? = null
)
