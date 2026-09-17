package ru.greenland.crm.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GitHubSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: GitHubSyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = syncManager.syncNow()
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
