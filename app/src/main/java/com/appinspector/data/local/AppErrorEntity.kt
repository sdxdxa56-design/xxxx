package com.appinspector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appinspector.domain.model.AppError
import com.appinspector.domain.model.ErrorSeverity

@Entity(tableName = "app_errors")
data class AppErrorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val tag: String,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val timestamp: Long,
    val severity: String,
    val solutionSuggestion: String?
) {
    fun toDomain(): AppError {
        return AppError(
            id = id,
            packageName = packageName,
            appName = appName,
            tag = tag,
            errorType = errorType,
            message = message,
            stackTrace = stackTrace,
            timestamp = timestamp,
            severity = try {
                ErrorSeverity.valueOf(severity)
            } catch (_: Exception) {
                ErrorSeverity.ERROR
            },
            solutionSuggestion = solutionSuggestion
        )
    }

    companion object {
        fun fromDomain(domain: AppError): AppErrorEntity {
            return AppErrorEntity(
                id = domain.id,
                packageName = domain.packageName,
                appName = domain.appName,
                tag = domain.tag,
                errorType = domain.errorType,
                message = domain.message,
                stackTrace = domain.stackTrace,
                timestamp = domain.timestamp,
                severity = domain.severity.name,
                solutionSuggestion = domain.solutionSuggestion
            )
        }
    }
}
