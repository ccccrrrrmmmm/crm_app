package ru.greenland.crm.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.greenland.crm.BuildConfig

private const val UPDATE_REPO_OWNER = "ccccrrrrmmmm"
private const val UPDATE_REPO_NAME = "crm_app"

/**
 * Приложение не из Google Play, поэтому автообновление делаем сами: проверяем последний
 * GitHub Release (публичный репозиторий, без токена), и если версия там новее — скачиваем
 * APK и открываем системную установку. Один тап «Установить» от пользователя всё равно
 * нужен — Android не даёт поставить приложение со стороны совсем без подтверждения.
 */
@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    suspend fun checkForUpdate() {
        val info = fetchLatestRelease() ?: return
        if (info.versionCode > BuildConfig.VERSION_CODE) {
            _availableUpdate.value = info
        }
    }

    private suspend fun fetchLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$UPDATE_REPO_OWNER/$UPDATE_REPO_NAME/releases/latest")
                .addHeader("Accept", "application/vnd.github+json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val release = json.decodeFromString(GitHubReleaseResponse.serializer(), response.body!!.string())
                val versionCode = release.tagName.substringAfterLast('.').toIntOrNull() ?: return@use null
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@use null
                UpdateInfo(versionName = release.tagName, versionCode = versionCode, downloadUrl = apk.downloadUrl)
            }
        }.getOrNull()
    }

    /** Скачивает APK во внутренний кэш и открывает системный экран установки. */
    suspend fun downloadAndInstall(update: UpdateInfo) {
        _isDownloading.value = true
        try {
            val file = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(dir, "greenland-crm-${update.versionCode}.apk")
                val request = Request.Builder().url(update.downloadUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Не удалось скачать обновление: ${response.code}")
                    target.outputStream().use { out -> response.body!!.byteStream().copyTo(out) }
                }
                target
            }
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        } finally {
            _isDownloading.value = false
        }
    }
}
