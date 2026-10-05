package com.appinspector.data.repository

import android.content.Context
import android.util.Base64
import com.appinspector.data.local.GitHubCredentials
import com.appinspector.data.remote.GitHubApi
import com.appinspector.data.remote.dto.BranchDto
import com.appinspector.data.remote.dto.RepoDto
import com.appinspector.data.remote.dto.TreeItem
import com.appinspector.data.remote.dto.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GitHubRepository(
    private val context: Context,
    private val credentials: GitHubCredentials = GitHubCredentials(context)
) {

    private val api: GitHubApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
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

    companion object {
        private const val BASE_URL = "https://api.github.com/"
    }
}
