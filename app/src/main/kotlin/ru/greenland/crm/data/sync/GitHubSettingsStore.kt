package ru.greenland.crm.data.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "github_sync_settings"
private const val KEY_OWNER = "owner"
private const val KEY_REPO = "repo"
private const val KEY_BRANCH = "branch"
private const val KEY_TOKEN = "token"
private const val KEY_INTERVAL = "sync_interval_minutes"

/** WorkManager не запускает периодические задачи чаще, чем раз в 15 минут — это его жёсткий минимум. */
const val MIN_SYNC_INTERVAL_MINUTES = 15

/**
 * Общий репозиторий с данными подставляется по умолчанию, чтобы новому мастеру оставалось
 * вписать только свой личный токен — не нужно объяснять, куда именно синхронизировать данные.
 */
private const val DEFAULT_OWNER = "ccccrrrrmmmm"
private const val DEFAULT_REPO = "crm_db"

/**
 * Хранит настройки GitHub-синхронизации (владелец/репозиторий/ветка/токен/интервал).
 * Токен — секрет, поэтому весь файл зашифрован (EncryptedSharedPreferences), а не DataStore.
 */
@Singleton
class GitHubSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<GitHubSettings> = _settings.asStateFlow()

    fun current(): GitHubSettings = _settings.value

    fun update(
        owner: String = current().owner,
        repo: String = current().repo,
        branch: String = current().branch,
        token: String = current().token,
        syncIntervalMinutes: Int = current().syncIntervalMinutes,
    ) {
        prefs.edit()
            .putString(KEY_OWNER, owner)
            .putString(KEY_REPO, repo)
            .putString(KEY_BRANCH, branch.ifBlank { "main" })
            .putString(KEY_TOKEN, token)
            .putInt(KEY_INTERVAL, syncIntervalMinutes.coerceAtLeast(MIN_SYNC_INTERVAL_MINUTES))
            .apply()
        _settings.value = load()
    }

    private fun load(): GitHubSettings = GitHubSettings(
        owner = prefs.getString(KEY_OWNER, DEFAULT_OWNER).orEmpty(),
        repo = prefs.getString(KEY_REPO, DEFAULT_REPO).orEmpty(),
        branch = prefs.getString(KEY_BRANCH, "main").orEmpty().ifBlank { "main" },
        token = prefs.getString(KEY_TOKEN, "").orEmpty(),
        syncIntervalMinutes = prefs.getInt(KEY_INTERVAL, 15),
    )
}
