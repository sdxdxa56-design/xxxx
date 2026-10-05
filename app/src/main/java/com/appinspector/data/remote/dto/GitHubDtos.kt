package com.appinspector.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("login")
    val login: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar_url")
    val avatarUrl: String?
)

data class RepoDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("default_branch")
    val defaultBranch: String?,
    @SerializedName("private")
    val isPrivate: Boolean,
    @SerializedName("clone_url")
    val cloneUrl: String?
)

data class BranchDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("commit")
    val commit: CommitDto?
)

data class CommitDto(
    @SerializedName("sha")
    val sha: String,
    @SerializedName("url")
    val url: String?
)

data class ContentDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("sha")
    val sha: String,
    @SerializedName("download_url")
    val downloadUrl: String?,
    @SerializedName("size")
    val size: Long?
)

data class TreeResponse(
    @SerializedName("sha")
    val sha: String,
    @SerializedName("tree")
    val tree: List<TreeItem>,
    @SerializedName("truncated")
    val truncated: Boolean
)

data class TreeItem(
    @SerializedName("path")
    val path: String,
    @SerializedName("mode")
    val mode: String?,
    @SerializedName("type")
    val type: String,
    @SerializedName("sha")
    val sha: String,
    @SerializedName("size")
    val size: Long?
)

data class WorkflowDispatchRequest(
    @SerializedName("ref")
    val ref: String,
    @SerializedName("inputs")
    val inputs: Map<String, String> = emptyMap()
)

data class WorkflowRunsResponse(
    @SerializedName("total_count")
    val total_count: Int,
    @SerializedName("workflow_runs")
    val workflow_runs: List<WorkflowRunDto>
)

data class WorkflowRunDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("conclusion")
    val conclusion: String?,
    @SerializedName("html_url")
    val html_url: String,
    @SerializedName("head_sha")
    val head_sha: String,
    @SerializedName("created_at")
    val created_at: String,
    @SerializedName("updated_at")
    val updated_at: String,
    @SerializedName("artifacts_url")
    val artifacts_url: String?
)

data class ArtifactsResponse(
    @SerializedName("total_count")
    val total_count: Int,
    @SerializedName("artifacts")
    val artifacts: List<ArtifactDto>
)

data class ArtifactDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("size_in_bytes")
    val size_in_bytes: Long,
    @SerializedName("archive_download_url")
    val archive_download_url: String,
    @SerializedName("expired")
    val expired: Boolean
)

data class CreateBlobRequest(
    @SerializedName("content")
    val content: String,
    @SerializedName("encoding")
    val encoding: String = "base64"
)

data class CreateBlobResponse(
    @SerializedName("sha")
    val sha: String,
    @SerializedName("url")
    val url: String
)

data class CreateTreeRequest(
    @SerializedName("base_tree")
    val baseTree: String?,
    @SerializedName("tree")
    val tree: List<TreeEntryDto>
)

data class TreeEntryDto(
    @SerializedName("path")
    val path: String,
    @SerializedName("mode")
    val mode: String = "100644",
    @SerializedName("type")
    val type: String = "blob",
    @SerializedName("sha")
    val sha: String
)

data class CreateCommitRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("tree")
    val tree: String,
    @SerializedName("parents")
    val parents: List<String>
)

data class UpdateRefRequest(
    @SerializedName("sha")
    val sha: String,
    @SerializedName("force")
    val force: Boolean = true
)
