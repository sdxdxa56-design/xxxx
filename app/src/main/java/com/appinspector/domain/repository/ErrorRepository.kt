package com.appinspector.domain.repository

import com.appinspector.domain.model.AppError
import kotlinx.coroutines.flow.Flow

interface ErrorRepository {
    fun getAllErrors(): Flow<List<AppError>>
    fun getErrorsByPackage(packageName: String): Flow<List<AppError>>
    fun getErrorById(id: Long): Flow<AppError?>
    fun getErrorCount(): Flow<Int>
    suspend fun insertError(error: AppError): Long
    suspend fun deleteError(error: AppError)
    suspend fun deleteAllErrors()
}
