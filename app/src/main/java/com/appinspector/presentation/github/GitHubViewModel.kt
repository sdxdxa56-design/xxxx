package com.appinspector.presentation.github

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appinspector.data.remote.dto.BranchDto
import com.appinspector.data.remote.dto.RepoDto
import com.appinspector.data.remote.dto.UserDto
import com.appinspector.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class DownloadState {
    data object Idle : DownloadState()
    data class InProgress(
        val downloadedFiles: Int,
        val totalFiles: Int,
        val currentFileName: String,
        val progress: Float
    ) : DownloadState()
    data class Success(
        val downloadedDirectory: File,
        val totalFiles: Int
    ) : DownloadState()
    data class Error(
        val message: String
    ) : DownloadState()
}

class GitHubViewModel(
    application: Application,
    private val repository: GitHubRepository = GitHubRepository(application)
) : AndroidViewModel(application) {

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _repos = MutableStateFlow<List<RepoDto>>(emptyList())
    val repos: StateFlow<List<RepoDto>> = _repos.asStateFlow()

    private val _branches = MutableStateFlow<List<BranchDto>>(emptyList())
    val branches: StateFlow<List<BranchDto>> = _branches.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun loadCurrentUser(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.getCurrentUser()
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun loadRepos(onResult: (Result<List<RepoDto>>) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.getRepos()
            if (result.isSuccess) {
                _repos.value = result.getOrNull() ?: emptyList()
            }
            onResult(result)
        }
    }

    fun loadBranches(owner: String, repo: String, onResult: (Result<List<BranchDto>>) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.getBranches(owner, repo)
            if (result.isSuccess) {
                _branches.value = result.getOrNull() ?: emptyList()
            }
            onResult(result)
        }
    }

    fun downloadProject(owner: String, repo: String, branch: String) {
        _downloadState.value = DownloadState.InProgress(
            downloadedFiles = 0,
            totalFiles = 0,
            currentFileName = "Initializing...",
            progress = 0f
        )

        viewModelScope.launch {
            val result = repository.downloadProject(
                owner = owner,
                repo = repo,
                branch = branch
            ) { downloaded, total, currentFile ->
                val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                _downloadState.value = DownloadState.InProgress(
                    downloadedFiles = downloaded,
                    totalFiles = total,
                    currentFileName = currentFile,
                    progress = progress
                )
            }

            if (result.isSuccess) {
                val dir = result.getOrNull()
                if (dir != null) {
                    val finalTotal = (_downloadState.value as? DownloadState.InProgress)?.totalFiles ?: 0
                    _downloadState.value = DownloadState.Success(
                        downloadedDirectory = dir,
                        totalFiles = finalTotal
                    )
                } else {
                    _downloadState.value = DownloadState.Error("Unknown download error")
                }
            } else {
                _downloadState.value = DownloadState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to download project files."
                )
            }
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
