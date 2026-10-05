package com.appinspector.data.remote

import com.appinspector.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

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

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun triggerWorkflow(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body body: WorkflowDispatchRequest
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs")
    suspend fun getWorkflowRuns(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Query("per_page") perPage: Int = 5
    ): WorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getAllWorkflowRuns(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 5
    ): WorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getWorkflowRun(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): WorkflowRunDto

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getRunArtifacts(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): ArtifactsResponse

    @Streaming
    @GET
    suspend fun downloadArtifact(
        @Header("Authorization") authHeader: String,
        @Url url: String
    ): ResponseBody

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateBlobRequest
    ): CreateBlobResponse

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateTreeRequest
    ): TreeResponse

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateCommitRequest
    ): CommitDto

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateRef(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body body: UpdateRefRequest
    ): Response<Unit>
}
