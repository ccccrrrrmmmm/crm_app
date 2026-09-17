package ru.greenland.crm.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_NOW = "github-sync-now"
private const val WORK_PERIODIC = "github-sync-periodic"

/**
 * Ставит [GitHubSyncWorker] в очередь: сразу после локального изменения (одноразово, для
 * «пушим сразу после изменений») и по расписанию из настроек (подстраховка/pull от других).
 */
@Singleton
class GitHubSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: GitHubSettingsStore,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val networkConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun triggerSoon() {
        if (!settingsStore.current().isConfigured) return
        val request = OneTimeWorkRequestBuilder<GitHubSyncWorker>()
            .setConstraints(networkConstraints)
            .build()
        workManager.enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun reschedulePeriodic() {
        val settings = settingsStore.current()
        if (!settings.isConfigured) {
            workManager.cancelUniqueWork(WORK_PERIODIC)
            return
        }
        val intervalMinutes = settings.syncIntervalMinutes.coerceAtLeast(MIN_SYNC_INTERVAL_MINUTES)
        val request = PeriodicWorkRequestBuilder<GitHubSyncWorker>(intervalMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
