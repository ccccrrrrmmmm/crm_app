package ru.greenland.crm.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.greenland.crm.data.update.AppUpdateManager
import ru.greenland.crm.data.update.UpdateInfo

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: AppUpdateManager,
) : ViewModel() {
    val availableUpdate: StateFlow<UpdateInfo?> = updateManager.availableUpdate
    val isDownloading: StateFlow<Boolean> = updateManager.isDownloading

    fun install() {
        val update = availableUpdate.value ?: return
        viewModelScope.launch { updateManager.downloadAndInstall(update) }
    }
}
