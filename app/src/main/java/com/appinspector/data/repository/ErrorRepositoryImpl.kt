package com.appinspector.data.repository

import com.appinspector.data.local.AppErrorDao
import com.appinspector.data.local.AppErrorEntity
import com.appinspector.domain.model.AppError
import com.appinspector.domain.repository.ErrorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ErrorRepositoryImpl(
    private val errorDao: AppErrorDao
) : ErrorRepository {

    override fun getAllErrors(): Flow<List<AppError>> {
        return errorDao.getAllErrors().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getErrorsByPackage(packageName: String): Flow<List<AppError>> {
        return errorDao.getErrorsByPackage(packageName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getErrorById(id: Long): Flow<AppError?> {
        return errorDao.getErrorById(id).map { it?.toDomain() }
    }

    override fun getErrorCount(): Flow<Int> {
        return errorDao.getErrorCount()
    }

    override suspend fun insertError(error: AppError): Long {
        return errorDao.insertError(AppErrorEntity.fromDomain(error))
    }

    override suspend fun saveError(error: AppError): Long {
        return insertError(error)
    }

    override suspend fun deleteError(error: AppError) {
        errorDao.deleteError(AppErrorEntity.fromDomain(error))
    }

    override suspend fun deleteAllErrors() {
        errorDao.deleteAllErrors()
    }
}
