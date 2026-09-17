package ru.greenland.crm.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
)
