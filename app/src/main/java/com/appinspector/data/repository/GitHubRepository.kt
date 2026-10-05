package com.appinspector.data.repository

import android.content.Context
import android.util.Base64
import com.appinspector.data.local.GitHubCredentials
import com.appinspector.data.remote.GitHubApi
import com.appinspector.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class GitHubRepository(
    private val context: Context,
    private val credentials: GitHubCredentials = GitHubCredentials(context)
) {

    private val api: GitHubApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }

    private fun getAuthHeader(): String {
        val token = credentials.getToken().orEmpty()
        return if (token.isNotBlank()) "token $token" else ""
    }

    fun saveToken(token: String) = credentials.saveToken(token)
    fun getToken(): String? = credentials.getToken()
    fun clearToken() = credentials.clearToken()
    fun isLoggedIn(): Boolean = credentials.isLoggedIn()

    suspend fun getCurrentUser(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val user = api.getUser(getAuthHeader())
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRepos(): Result<List<RepoDto>> = withContext(Dispatchers.IO) {
        try {
            val repos = api.getUserRepos(getAuthHeader())
            Result.success(repos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranches(owner: String, repo: String): Result<List<BranchDto>> = withContext(Dispatchers.IO) {
        try {
            val branches = api.getBranches(getAuthHeader(), owner, repo)
            Result.success(branches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRepoTree(owner: String, repo: String, branch: String): Result<List<TreeItem>> = withContext(Dispatchers.IO) {
        try {
            val treeResponse = api.getTree(getAuthHeader(), owner, repo, branch, recursive = 1)
            Result.success(treeResponse.tree)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(owner: String, repo: String, path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = api.getFileContent(getAuthHeader(), owner, repo, path)
            Result.success(content.downloadUrl ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadProject(
        owner: String,
        repo: String,
        branch: String,
        onProgress: (downloadedFiles: Int, totalFiles: Int, currentFileName: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val treeResult = getRepoTree(owner, repo, branch)
            if (treeResult.isFailure) {
                return@withContext Result.failure(treeResult.exceptionOrNull() ?: Exception("Failed to fetch tree"))
            }

            val tree = treeResult.getOrNull() ?: emptyList()
            val fileItems = tree.filter { it.type == "blob" }
            val totalFiles = fileItems.size

            val targetDir = File(context.filesDir, "projects/$repo").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            var downloadedCount = 0

            for (item in fileItems) {
                val relativePath = item.path
                val localFile = File(targetDir, relativePath)
                localFile.parentFile?.mkdirs()

                onProgress(downloadedCount, totalFiles, relativePath)

                try {
                    val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/$relativePath"
                    val responseBody = api.downloadRawFile(getAuthHeader(), rawUrl)
                    FileOutputStream(localFile).use { output ->
                        responseBody.byteStream().copyTo(output)
                    }
                } catch (_: Exception) {
                    try {
                        val contentDto = api.getFileContent(getAuthHeader(), owner, repo, relativePath, branch)
                        if (!contentDto.downloadUrl.isNullOrBlank()) {
                            val responseBody = api.downloadRawFile(getAuthHeader(), contentDto.downloadUrl)
                            FileOutputStream(localFile).use { output ->
                                responseBody.byteStream().copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        localFile.writeText("// Failed to download: $relativePath (${e.message})")
                    }
                }

                downloadedCount++
                onProgress(downloadedCount, totalFiles, relativePath)
            }

            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushLocalProjectToGitHub(
        owner: String,
        repo: String,
        branch: String,
        localDir: File,
        commitMessage: String,
        onProgress: (Int, Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allFiles = localDir.walkTopDown()
                .filter { it.isFile && !it.path.contains("/.git/") && !it.path.contains("/build/") }
                .toList()

            val totalFiles = allFiles.size
            val treeEntries = mutableListOf<TreeEntryDto>()
            var processed = 0

            for (file in allFiles) {
                val relativePath = file.relativeTo(localDir).path.replace('\\', '/')
                val bytes = file.readBytes()
                val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)

                val blobResponse = api.createBlob(
                    authHeader = getAuthHeader(),
                    owner = owner,
                    repo = repo,
                    body = CreateBlobRequest(content = base64Content, encoding = "base64")
                )

                treeEntries.add(
                    TreeEntryDto(
                        path = relativePath,
                        mode = if (file.name == "gradlew" || file.canExecute()) "100755" else "100644",
                        type = "blob",
                        sha = blobResponse.sha
                    )
                )

                processed++
                onProgress(processed, totalFiles)
            }

            val branchesResult = getBranches(owner, repo)
            val currentBranch = branchesResult.getOrNull()?.find { it.name == branch }
            val parentSha = currentBranch?.commit?.sha

            val treeResponse = api.createTree(
                authHeader = getAuthHeader(),
                owner = owner,
                repo = repo,
                body = CreateTreeRequest(baseTree = null, tree = treeEntries)
            )

            val parents = if (parentSha != null) listOf(parentSha) else emptyList()
            val commitResponse = api.createCommit(
                authHeader = getAuthHeader(),
                owner = owner,
                repo = repo,
                body = CreateCommitRequest(
                    message = commitMessage,
                    tree = treeResponse.sha,
                    parents = parents
                )
            )

            api.updateRef(
                authHeader = getAuthHeader(),
                owner = owner,
                repo = repo,
                branch = branch,
                body = UpdateRefRequest(sha = commitResponse.sha, force = true)
            )

            Result.success(commitResponse.sha)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerBuildWorkflow(
        owner: String,
        repo: String,
        branch: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = try {
                api.triggerWorkflow(
                    authHeader = getAuthHeader(),
                    owner = owner,
                    repo = repo,
                    workflowId = WORKFLOW_FILE,
                    body = WorkflowDispatchRequest(ref = branch)
                )
            } catch (_: Exception) {
                api.triggerWorkflow(
                    authHeader = getAuthHeader(),
                    owner = owner,
                    repo = repo,
                    workflowId = "build_apk.yml",
                    body = WorkflowDispatchRequest(ref = branch)
                )
            }
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestRun(
        owner: String,
        repo: String
    ): Result<WorkflowRunDto?> = withContext(Dispatchers.IO) {
        try {
            val runsResponse = try {
                api.getWorkflowRuns(getAuthHeader(), owner, repo, WORKFLOW_FILE, perPage = 1)
            } catch (_: Exception) {
                api.getAllWorkflowRuns(getAuthHeader(), owner, repo, perPage = 1)
            }
            Result.success(runsResponse.workflow_runs.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollBuildUntilComplete(
        owner: String,
        repo: String,
        runId: Long? = null,
        onStatusUpdate: (WorkflowRunDto) -> Unit,
        pollIntervalMs: Long = 5000,
        timeoutMs: Long = 15 * 60 * 1000
    ): Result<WorkflowRunDto> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var currentRunId: Long? = runId

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (currentRunId == null) {
                val latestResult = getLatestRun(owner, repo)
                val run = latestResult.getOrNull()
                if (run != null) {
                    currentRunId = run.id
                    onStatusUpdate(run)
                    if (run.status == "completed") {
                        return@withContext Result.success(run)
                    }
                }
            } else {
                try {
                    val run = api.getWorkflowRun(getAuthHeader(), owner, repo, currentRunId)
                    onStatusUpdate(run)
                    if (run.status == "completed") {
                        return@withContext Result.success(run)
                    }
                } catch (_: Exception) {
                }
            }

            delay(pollIntervalMs)
        }

        Result.failure(Exception("Build monitoring timed out after ${timeoutMs / 60000} minutes."))
    }

    suspend fun downloadApkFromArtifacts(
        owner: String,
        repo: String,
        runId: Long,
        destinationFile: File,
        onProgress: (Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val artifactsResponse = api.getRunArtifacts(getAuthHeader(), owner, repo, runId)
            val artifact = artifactsResponse.artifacts.firstOrNull {
                !it.expired && (it.name.contains("apk", ignoreCase = true) || it.name.contains("app", ignoreCase = true))
            } ?: artifactsResponse.artifacts.firstOrNull { !it.expired }

            if (artifact == null) {
                return@withContext Result.failure(Exception("No APK artifacts found for run #$runId"))
            }

            val tempZip = File(context.cacheDir, "artifact_${artifact.id}.zip")
            val responseBody = api.downloadArtifact(getAuthHeader(), artifact.archive_download_url)
            val totalBytes = artifact.size_in_bytes

            FileOutputStream(tempZip).use { output ->
                val input = responseBody.byteStream()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalDownloaded = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalDownloaded += bytesRead
                    onProgress(totalDownloaded, totalBytes)
                }
            }

            destinationFile.parentFile?.mkdirs()
            var apkFound = false

            ZipInputStream(FileInputStream(tempZip)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".apk", ignoreCase = true)) {
                        FileOutputStream(destinationFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        apkFound = true
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            tempZip.delete()

            if (apkFound && destinationFile.exists() && destinationFile.length() > 0) {
                Result.success(destinationFile)
            } else {
                Result.failure(Exception("No valid .apk binary extracted from the artifact ZIP archive."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://api.github.com/"
        private const val WORKFLOW_FILE = "build.yml"
    }
}
