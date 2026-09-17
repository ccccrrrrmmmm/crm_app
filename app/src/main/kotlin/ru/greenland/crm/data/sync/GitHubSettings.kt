package ru.greenland.crm.data.sync

data class GitHubSettings(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
    val syncIntervalMinutes: Int = 15,
) {
    val isConfigured: Boolean
        get() = owner.isNotBlank() && repo.isNotBlank() && token.isNotBlank()
}
