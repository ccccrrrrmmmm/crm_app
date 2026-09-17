package ru.greenland.crm.data.remote.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepoInfo(
    @SerialName("default_branch") val defaultBranch: String,
    @SerialName("full_name") val fullName: String,
)

@Serializable
data class GitHubContentResponse(
    val content: String? = null,
    val encoding: String? = null,
    val sha: String,
    val path: String,
)

@Serializable
data class PutContentRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String,
)

@Serializable
data class PutContentResponse(
    val content: GitHubContentInfo? = null,
)

@Serializable
data class GitHubContentInfo(
    val sha: String,
    val path: String,
)

@Serializable
data class DeleteContentRequest(
    val message: String,
    val sha: String,
    val branch: String,
)

@Serializable
data class GitHubTreeResponse(
    val sha: String,
    val tree: List<GitHubTreeEntry> = emptyList(),
    val truncated: Boolean = false,
)

@Serializable
data class GitHubTreeEntry(
    val path: String,
    val type: String,
    val sha: String,
)

@Serializable
data class GitHubBlobResponse(
    val sha: String,
    val content: String,
    val encoding: String,
)
