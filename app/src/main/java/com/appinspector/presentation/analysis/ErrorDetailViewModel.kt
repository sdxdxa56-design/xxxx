package com.appinspector.presentation.analysis

import androidx.lifecycle.ViewModel
import com.appinspector.domain.model.AppError
import com.appinspector.domain.repository.ErrorRepository
import kotlinx.coroutines.flow.Flow

class ErrorDetailViewModel(
    private val repository: ErrorRepository
) : ViewModel() {

    fun getError(errorId: Long): Flow<AppError?> {
        return repository.getErrorById(errorId)
    }

    suspend fun deleteError(error: AppError) {
        repository.deleteError(error)
    }
}
