package com.appinspector.presentation.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appinspector.domain.model.AppError
import com.appinspector.domain.repository.ErrorRepository
import com.appinspector.services.ErrorAnalyzer
import com.appinspector.services.RootManager
import com.appinspector.services.RootLogcatReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MonitorViewModel(
    private val repository: ErrorRepository
) : ViewModel() {

    val errors: StateFlow<List<AppError>> = repository.getAllErrors()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _liveLogs = MutableStateFlow<List<String>>(emptyList())
    val liveLogs: StateFlow<List<String>> = _liveLogs.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var rootReader: RootLogcatReader? = null
    private var standardProcess: Process? = null

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true

        RootManager.hasRootPermissionAsync { hasRoot ->
            if (hasRoot) {
                listenWithRoot()
            } else {
                listenStandard()
            }
        }
    }

    private fun listenWithRoot() {
        val reader = RootLogcatReader()
        rootReader = reader
        viewModelScope.launch(Dispatchers.IO) {
            reader.startReading()
                .catch {
                    listenStandard()
                }
                .collectLatest { line ->
                    appendLog(line)
                }
        }
    }

    private fun listenStandard() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val proc = ProcessBuilder("logcat", "-v", "time", "*:E", "*:F").start()
                standardProcess = proc
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line
                    if (!currentLine.isNullOrBlank()) {
                        appendLog(currentLine)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun appendLog(line: String) {
        val current = _liveLogs.value.takeLast(199).toMutableList()
        current.add(line)
        _liveLogs.value = current

        val error = ErrorAnalyzer.parseLogLine(line)
        if (error != null) {
            viewModelScope.launch {
                repository.insertError(error)
            }
        }
    }

    fun stopListening() {
        _isListening.value = false
        rootReader?.stopReading()
        try {
            standardProcess?.destroy()
        } catch (_: Exception) {
        }
    }

    fun clearLiveLogs() {
        _liveLogs.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
