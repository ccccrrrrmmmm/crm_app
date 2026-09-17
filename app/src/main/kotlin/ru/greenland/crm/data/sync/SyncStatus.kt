package ru.greenland.crm.data.sync

data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null,
)
