package ru.greenland.crm

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.greenland.crm.data.sync.GitHubSyncScheduler
import ru.greenland.crm.data.update.AppUpdateManager

@HiltAndroidApp
class GreenlandCrmApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: GitHubSyncScheduler

    @Inject
    lateinit var updateManager: AppUpdateManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.reschedulePeriodic()
        appScope.launch { updateManager.checkForUpdate() }
    }
}
