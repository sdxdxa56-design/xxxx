package com.appinspector.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appinspector.domain.model.AppError
import com.appinspector.domain.repository.ErrorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ErrorRepository
) : ViewModel() {

    val errors: StateFlow<List<AppError>> = repository.getAllErrors()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val errorCount: StateFlow<Int> = repository.getErrorCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun clearAllErrors() {
        viewModelScope.launch {
            repository.deleteAllErrors()
        }
    }

    fun deleteError(error: AppError) {
        viewModelScope.launch {
            repository.deleteError(error)
        }
    }
}
