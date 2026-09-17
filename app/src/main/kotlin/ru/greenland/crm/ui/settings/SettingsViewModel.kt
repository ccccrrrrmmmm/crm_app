package ru.greenland.crm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.greenland.crm.data.remote.github.GitHubApi
import ru.greenland.crm.data.remote.github.GitHubApiException
import ru.greenland.crm.data.sync.GitHubSettingsStore
import ru.greenland.crm.data.sync.GitHubSyncManager
import ru.greenland.crm.data.sync.GitHubSyncScheduler
import ru.greenland.crm.data.sync.MIN_SYNC_INTERVAL_MINUTES
import ru.greenland.crm.data.sync.SyncStatus

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, ERROR }

data class SettingsFormState(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
    val intervalMinutes: String = MIN_SYNC_INTERVAL_MINUTES.toString(),
)

data class SettingsUiState(
    val form: SettingsFormState = SettingsFormState(),
    val isConfigured: Boolean = false,
    val isEditing: Boolean = true,
    val status: SyncStatus = SyncStatus(),
    val testState: ConnectionTestState = ConnectionTestState.IDLE,
    val testMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: GitHubSettingsStore,
    private val syncScheduler: GitHubSyncScheduler,
    private val syncManager: GitHubSyncManager,
    private val api: GitHubApi,
) : ViewModel() {

    private val form = MutableStateFlow(currentSettingsAsForm())
    private val isEditing = MutableStateFlow(!settingsStore.current().isConfigured)
    private val testState = MutableStateFlow(ConnectionTestState.IDLE)
    private val testMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        form, settingsStore.settings, syncManager.status, isEditing,
    ) { f, settings, status, editing ->
        SettingsUiState(
            form = f,
            isConfigured = settings.isConfigured,
            isEditing = editing,
            status = status,
            testState = testState.value,
            testMessage = testMessage.value,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(form = form.value, isEditing = isEditing.value))

    private fun currentSettingsAsForm(): SettingsFormState = settingsStore.current().let {
        SettingsFormState(
            owner = it.owner,
            repo = it.repo,
            branch = it.branch,
            token = it.token,
            intervalMinutes = it.syncIntervalMinutes.toString(),
        )
    }

    fun onOwnerChange(value: String) { form.value = form.value.copy(owner = value) }
    fun onRepoChange(value: String) { form.value = form.value.copy(repo = value) }
    fun onBranchChange(value: String) { form.value = form.value.copy(branch = value) }
    fun onTokenChange(value: String) { form.value = form.value.copy(token = value) }
    fun onIntervalChange(value: String) { form.value = form.value.copy(intervalMinutes = value.filter { it.isDigit() }) }

    /** Проверяет доступ к репозиторию с введёнными данными и только потом сохраняет. */
    fun connect() {
        val f = form.value
        val owner = f.owner.trim()
        val repo = f.repo.trim()
        val token = f.token.trim()
        if (owner.isEmpty() || repo.isEmpty() || token.isEmpty()) {
            testState.value = ConnectionTestState.ERROR
            testMessage.value = "Заполни владельца, репозиторий и токен"
            return
        }

        testState.value = ConnectionTestState.TESTING
        testMessage.value = null
        viewModelScope.launch {
            val result = api.checkAccess(owner, repo, token)
            result.onSuccess { repoInfo ->
                val interval = f.intervalMinutes.toIntOrNull()?.coerceAtLeast(MIN_SYNC_INTERVAL_MINUTES) ?: MIN_SYNC_INTERVAL_MINUTES
                val branch = f.branch.trim().ifBlank { repoInfo.defaultBranch }
                settingsStore.update(
                    owner = owner,
                    repo = repo,
                    branch = branch,
                    token = token,
                    syncIntervalMinutes = interval,
                )
                syncScheduler.reschedulePeriodic()
                form.value = f.copy(branch = branch, intervalMinutes = interval.toString())
                testState.value = ConnectionTestState.SUCCESS
                isEditing.value = false
                syncManager.syncNow()
            }.onFailure { error ->
                testState.value = ConnectionTestState.ERROR
                testMessage.value = friendlyError(error)
            }
        }
    }

    fun startEditing() {
        form.value = currentSettingsAsForm()
        testState.value = ConnectionTestState.IDLE
        testMessage.value = null
        isEditing.value = true
    }

    fun cancelEditing() {
        if (!settingsStore.current().isConfigured) return
        form.value = currentSettingsAsForm()
        testState.value = ConnectionTestState.IDLE
        isEditing.value = false
    }

    fun disconnect() {
        settingsStore.update(owner = "", repo = "", branch = "main", token = "", syncIntervalMinutes = MIN_SYNC_INTERVAL_MINUTES)
        syncScheduler.reschedulePeriodic()
        form.value = SettingsFormState()
        testState.value = ConnectionTestState.IDLE
        testMessage.value = null
        isEditing.value = true
    }

    fun syncNow() {
        viewModelScope.launch { syncManager.syncNow() }
    }

    private fun friendlyError(error: Throwable): String = when {
        error is GitHubApiException && error.code == 401 -> "Токен не подходит — проверь, что скопирован полностью и не истёк."
        error is GitHubApiException && error.code == 404 -> "Репозиторий не найден — проверь владельца и название, либо у токена нет к нему доступа."
        error is GitHubApiException && error.code == 403 -> "GitHub отклонил запрос (403) — убедись, что у токена отмечена галочка «repo»."
        else -> error.message ?: "Не удалось подключиться"
    }
}
