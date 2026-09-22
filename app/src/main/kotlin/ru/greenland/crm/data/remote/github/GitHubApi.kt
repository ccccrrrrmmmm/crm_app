package ru.greenland.crm.data.remote.github

import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.greenland.crm.data.sync.GitHubSettings
import ru.greenland.crm.data.sync.GitHubSettingsStore

class GitHubApiException(val code: Int, val bodySnippet: String?) :
    Exception("GitHub API error $code${bodySnippet?.let { ": $it" }.orEmpty()}")

/**
 * Тонкий клиент над GitHub REST API (Contents + Git Data), используем репозиторий как
 * версионированное хранилище JSON-записей и фото. Каждый вызов — один HTTP-запрос,
 * никакого локального `git` не требуется.
 */
@Singleton
class GitHubApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val settingsStore: GitHubSettingsStore,
) {
    private val jsonMediaType = "application/json".toMediaType()

    private fun baseUrl(settings: GitHubSettings) = "https://api.github.com/repos/${settings.owner}/${settings.repo}"

    private fun requestBuilder(settings: GitHubSettings): Request.Builder =
        Request.Builder()
            .addHeader("Authorization", "token ${settings.token}")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")

    /**
     * Лёгкая проверка «репозиторий существует и токен даёт доступ» — используется в UI
     * настроек перед сохранением, чтобы не узнавать об опечатке только на первой синхронизации.
     * Не трогает сохранённые настройки — проверяет ровно те значения, что передали.
     */
    suspend fun checkAccess(owner: String, repo: String, token: String): Result<GitHubRepoInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .url("https://api.github.com/repos/$owner/$repo")
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw GitHubApiException(response.code, response.body?.string()?.take(300))
                    json.decodeFromString(GitHubRepoInfo.serializer(), response.body!!.string())
                }
            }
        }

    suspend fun getFile(path: String): GitHubContentResponse? = withContext(Dispatchers.IO) {
        val settings = settingsStore.current()
        val request = requestBuilder(settings)
            .url("${baseUrl(settings)}/contents/$path?ref=${settings.branch}")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                response.isSuccessful -> json.decodeFromString(GitHubContentResponse.serializer(), response.body!!.string())
                else -> throw GitHubApiException(response.code, response.body?.string()?.take(300))
            }
        }
    }

    private fun buildPutRequest(path: String, encodedContent: String, message: String, sha: String?, settings: GitHubSettings): Request {
        val requestBody = json.encodeToString(
            PutContentRequest.serializer(),
            PutContentRequest(message = message, content = encodedContent, sha = sha, branch = settings.branch),
        )
        return requestBuilder(settings)
            .url("${baseUrl(settings)}/contents/$path")
            .put(requestBody.toRequestBody(jsonMediaType))
            .build()
    }

    /**
     * Создаёт или обновляет файл. Возвращает sha нового блоба.
     *
     * `sha` — тот, что мы знаем ЛОКАЛЬНО (или null, если считаем файл новым). Это не всегда
     * совпадает с тем, что реально на GitHub прямо сейчас: например, когда второе устройство
     * подключается впервые, его локальный кэш sha ещё пустой, а часть файлов уже запушило
     * первое устройство. GitHub в таких случаях отвечает 409 или 422 ("sha wasn't supplied").
     * Вместо того чтобы падать, на такой ответ мы просто спрашиваем у GitHub актуальный sha
     * и повторяем запрос уже с ним — самовосстановление, не важно, из-за чего именно разъехались.
     */
    suspend fun putFile(path: String, contentBytes: ByteArray, message: String, sha: String?): String =
        withContext(Dispatchers.IO) {
            val settings = settingsStore.current()
            val encoded = Base64.getEncoder().encodeToString(contentBytes)

            var response = okHttpClient.newCall(buildPutRequest(path, encoded, message, sha, settings)).execute()

            if (response.code == 409) {
                // Только что опустевший репозиторий: GitHub иногда ещё не успевает "увидеть"
                // ветку main, которую сам же создал долей секунды раньше — короткая пауза
                // и та же попытка обычно решают дело без похода за настоящим sha.
                response.close()
                delay(700)
                response = okHttpClient.newCall(buildPutRequest(path, encoded, message, sha, settings)).execute()
            }

            if (response.code == 409 || response.code == 422) {
                response.close()
                val actualSha = getFile(path)?.sha
                response = okHttpClient.newCall(buildPutRequest(path, encoded, message, actualSha, settings)).execute()
            }

            response.use {
                if (!it.isSuccessful) throw GitHubApiException(it.code, it.body?.string()?.take(300))
                val parsed = json.decodeFromString(PutContentResponse.serializer(), it.body!!.string())
                parsed.content?.sha ?: throw IllegalStateException("GitHub не вернул sha для $path")
            }
        }

    suspend fun deleteFile(path: String, sha: String, message: String) = withContext(Dispatchers.IO) {
        val settings = settingsStore.current()
        val requestBody = json.encodeToString(
            DeleteContentRequest.serializer(),
            DeleteContentRequest(message = message, sha = sha, branch = settings.branch),
        )
        val request = requestBuilder(settings)
            .url("${baseUrl(settings)}/contents/$path")
            .delete(requestBody.toRequestBody(jsonMediaType))
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw GitHubApiException(response.code, response.body?.string()?.take(300))
            }
        }
    }

    /** Полное дерево файлов ветки за один запрос. Пустой список — если репозиторий/ветка ещё пустые. */
    suspend fun getTree(): List<GitHubTreeEntry> = withContext(Dispatchers.IO) {
        val settings = settingsStore.current()
        val request = requestBuilder(settings)
            .url("${baseUrl(settings)}/git/trees/${settings.branch}?recursive=1")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            when {
                // 404 — нет такой ветки, 409 — репозиторий вообще пустой (ещё нет ни одного
                // коммита). GitHub возвращает разные коды в зависимости от случая, для нас
                // оба значат одно и то же: "пока ничего скачивать".
                response.code == 404 || response.code == 409 -> emptyList()
                response.isSuccessful -> {
                    val parsed = json.decodeFromString(GitHubTreeResponse.serializer(), response.body!!.string())
                    parsed.tree.filter { it.type == "blob" }
                }
                else -> throw GitHubApiException(response.code, response.body?.string()?.take(300))
            }
        }
    }

    suspend fun getBlob(sha: String): ByteArray = withContext(Dispatchers.IO) {
        val settings = settingsStore.current()
        val request = requestBuilder(settings)
            .url("${baseUrl(settings)}/git/blobs/$sha")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw GitHubApiException(response.code, response.body?.string()?.take(300))
            val parsed = json.decodeFromString(GitHubBlobResponse.serializer(), response.body!!.string())
            Base64.getMimeDecoder().decode(parsed.content)
        }
    }
}
