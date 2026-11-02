package com.ginckl.thumbplan.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SyncUi(
    val folderUri: String? = null,
    val folderName: String? = null,
    val enabled: Boolean = false,
    val lastSyncTime: Long = 0L,
    val status: String = ""
)

/** Per-device sync settings (deliberately not part of the backup itself). */
@Singleton
class SyncStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("ThumbPlanSync", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        SyncUi(
            folderUri = prefs.getString("folderUri", null),
            folderName = prefs.getString("folderName", null),
            enabled = prefs.getBoolean("enabled", false),
            lastSyncTime = prefs.getLong("lastSyncTime", 0L),
            status = prefs.getString("status", "") ?: ""
        )
    )
    val state: StateFlow<SyncUi> = _state.asStateFlow()

    /** Hash of the data as of the last successful sync (the common ancestor for conflict detection). */
    var lastSyncedHash: String?
        get() = prefs.getString("lastHash", null)
        set(v) { prefs.edit().putString("lastHash", v).apply() }

    fun setFolder(uri: String, name: String?) {
        prefs.edit().putString("folderUri", uri).putString("folderName", name).putBoolean("enabled", true).remove("lastHash").apply()
        _state.value = _state.value.copy(folderUri = uri, folderName = name, enabled = true, status = "Folder connected")
    }

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean("enabled", on).apply()
        _state.value = _state.value.copy(enabled = on)
    }

    fun disconnect() {
        prefs.edit().clear().apply()
        _state.value = SyncUi()
    }

    fun report(status: String, success: Boolean) {
        val now = if (success) System.currentTimeMillis() else _state.value.lastSyncTime
        prefs.edit().putString("status", status).putLong("lastSyncTime", now).apply()
        _state.value = _state.value.copy(status = status, lastSyncTime = now)
    }
}
