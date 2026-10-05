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
