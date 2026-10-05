package com.appinspector.data.remote

import com.appinspector.data.remote.dto.BranchDto
import com.appinspector.data.remote.dto.ContentDto
import com.appinspector.data.remote.dto.RepoDto
import com.appinspector.data.remote.dto.TreeResponse
import com.appinspector.data.remote.dto.UserDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface GitHubApi {

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") authHeader: String
    ): UserDto

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") authHeader: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("type") type: String = "all"
    ): List<RepoDto>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): List<BranchDto>

    @GET("repos/{owner}/{repo}/git/trees/{branch}")
    suspend fun getTree(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Query("recursive") recursive: Int = 1
    ): TreeResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String? = null
    ): ContentDto

    @GET
    suspend fun downloadRawFile(
        @Header("Authorization") authHeader: String,
        @Url url: String
    ): ResponseBody
}
