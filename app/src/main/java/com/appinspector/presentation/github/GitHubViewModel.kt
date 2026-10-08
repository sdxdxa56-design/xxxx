package com.appinspector.presentation.github

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appinspector.data.remote.dto.BranchDto
import com.appinspector.data.remote.dto.RepoDto
import com.appinspector.data.remote.dto.UserDto
import com.appinspector.data.repository.GitHubRepository
import com.appinspector.presentation.build.BuildUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    application: Application
) : AndroidViewModel(application) {

    private val repository: GitHubRepository = GitHubRepository(application)

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _repos = MutableStateFlow<List<RepoDto>>(emptyList())
    val repos: StateFlow<List<RepoDto>> = _repos.asStateFlow()

    private val _branches = MutableStateFlow<List<BranchDto>>(emptyList())
    val branches: StateFlow<List<BranchDto>> = _branches.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _buildState = MutableStateFlow<BuildUiState>(BuildUiState.Idle)
    val buildState: StateFlow<BuildUiState> = _buildState.asStateFlow()

    private var buildJob: Job? = null

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

    fun buildProject(owner: String, repo: String, branch: String, localProjectDir: File) {
        buildJob?.cancel()
        buildJob = viewModelScope.launch {
            try {
                // Step 1: Push local project changes to GitHub
                _buildState.value = BuildUiState.Pushing(current = 0, total = 0)
                val pushResult = repository.pushLocalProjectToGitHub(
                    owner = owner,
                    repo = repo,
                    branch = branch,
                    localDir = localProjectDir,
                    commitMessage = "Auto build from App Inspector [CI]"
                ) { current, total ->
                    _buildState.value = BuildUiState.Pushing(current = current, total = total)
                }

                if (pushResult.isFailure) {
                    _buildState.value = BuildUiState.Failure(
                        message = "Push failed: ${pushResult.exceptionOrNull()?.message}",
                        htmlUrl = null
                    )
                    return@launch
                }

                // Step 2: Trigger build workflow on GitHub Actions
                val triggerResult = repository.triggerBuildWorkflow(owner, repo, branch)
                if (triggerResult.isFailure) {
                    _buildState.value = BuildUiState.Failure(
                        message = "Trigger failed: ${triggerResult.exceptionOrNull()?.message}",
                        htmlUrl = null
                    )
                    return@launch
                }

                _buildState.value = BuildUiState.Queued(runId = 0L, htmlUrl = "https://github.com/$owner/$repo/actions")
                delay(3000)

                val startTime = System.currentTimeMillis()

                // Step 3: Poll until build completes
                val pollResult = repository.pollBuildUntilComplete(
                    owner = owner,
                    repo = repo,
                    onStatusUpdate = { run ->
                        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                        if (run.status == "queued") {
                            _buildState.value = BuildUiState.Queued(runId = run.id, htmlUrl = run.html_url)
                        } else if (run.status == "in_progress") {
                            _buildState.value = BuildUiState.Running(
                                status = run.status,
                                elapsedSeconds = elapsedSeconds,
                                htmlUrl = run.html_url
                            )
                        }
                    }
                )

                if (pollResult.isFailure) {
                    _buildState.value = BuildUiState.Failure(
                        message = "Build polling failed: ${pollResult.exceptionOrNull()?.message}",
                        htmlUrl = "https://github.com/$owner/$repo/actions"
                    )
                    return@launch
                }

                val finalRun = pollResult.getOrNull()
                if (finalRun == null || finalRun.conclusion != "success") {
                    _buildState.value = BuildUiState.Failure(
                        message = "GitHub Actions build finished with status '${finalRun?.status}' and conclusion '${finalRun?.conclusion}'.",
                        htmlUrl = finalRun?.html_url ?: "https://github.com/$owner/$repo/actions"
                    )
                    return@launch
                }

                // Step 4: Download generated APK artifact
                val outputApk = File(getApplication<Application>().filesDir, "builds/$repo/${repo}-debug.apk")
                val downloadResult = repository.downloadApkFromArtifacts(
                    owner = owner,
                    repo = repo,
                    runId = finalRun.id,
                    destinationFile = outputApk,
                    onProgress = { _, _ -> }
                )

                if (downloadResult.isSuccess && downloadResult.getOrNull() != null) {
                    _buildState.value = BuildUiState.Success(
                        apkFile = downloadResult.getOrNull()!!,
                        htmlUrl = finalRun.html_url
                    )
                } else {
                    _buildState.value = BuildUiState.Failure(
                        message = "APK artifact download failed: ${downloadResult.exceptionOrNull()?.message}",
                        htmlUrl = finalRun.html_url
                    )
                }

            } catch (e: Exception) {
                _buildState.value = BuildUiState.Failure(
                    message = e.message ?: "Unexpected error during build pipeline.",
                    htmlUrl = null
                )
            }
        }
    }

    fun cancelBuild() {
        buildJob?.cancel()
        buildJob = null
        _buildState.value = BuildUiState.Idle
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
