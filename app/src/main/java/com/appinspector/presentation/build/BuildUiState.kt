package com.appinspector.presentation.build

import java.io.File

sealed class BuildUiState {
    data object Idle : BuildUiState()
    data class Pushing(val current: Int, val total: Int) : BuildUiState()
    data class Queued(val runId: Long, val htmlUrl: String) : BuildUiState()
    data class Running(val status: String, val elapsedSeconds: Long, val htmlUrl: String = "") : BuildUiState()
    data class Success(val apkFile: File, val htmlUrl: String) : BuildUiState()
    data class Failure(val message: String, val htmlUrl: String?) : BuildUiState()
}
