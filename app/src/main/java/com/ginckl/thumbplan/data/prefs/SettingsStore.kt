package com.ginckl.thumbplan.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.ginckl.thumbplan.domain.ShopCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"), LIGHT("light", "Light"), DARK("dark", "Dark");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Small SharedPreferences wrapper exposing observable values. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ThumbPlanPrefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readTheme())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _snoozeMinutes = MutableStateFlow(prefs.getInt(KEY_SNOOZE, 10))
    val snoozeMinutes: StateFlow<Int> = _snoozeMinutes.asStateFlow()

    private val _vibrate = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE, true))
    val vibrate: StateFlow<Boolean> = _vibrate.asStateFlow()

    private val _sound = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val sound: StateFlow<Boolean> = _sound.asStateFlow()

    /** After swiping "complete" on the alarm screen: jump into the app (default) or just carry on. */
    private val _openAppAfterSwipe = MutableStateFlow(prefs.getBoolean(KEY_OPEN_AFTER, true))
    val openAppAfterSwipe: StateFlow<Boolean> = _openAppAfterSwipe.asStateFlow()

    private val _accent = MutableStateFlow(prefs.getString(KEY_ACCENT, ShopCatalog.DEFAULT_ACCENT) ?: ShopCatalog.DEFAULT_ACCENT)
    val accent: StateFlow<String> = _accent.asStateFlow()

    private val _avatar = MutableStateFlow(prefs.getString(KEY_AVATAR, ShopCatalog.DEFAULT_AVATAR) ?: ShopCatalog.DEFAULT_AVATAR)
    val avatar: StateFlow<String> = _avatar.asStateFlow()

    private val _tone = MutableStateFlow(prefs.getString(KEY_TONE, ShopCatalog.DEFAULT_TONE) ?: ShopCatalog.DEFAULT_TONE)
    val tone: StateFlow<String> = _tone.asStateFlow()

    // v1.0 stored a boolean "isDarkTheme" (default dark): keep the user's choice on upgrade.
    private fun readTheme(): ThemeMode {
        prefs.getString(KEY_THEME, null)?.let { return ThemeMode.from(it) }
        return if (prefs.contains(KEY_LEGACY_DARK)) {
            if (prefs.getBoolean(KEY_LEGACY_DARK, true)) ThemeMode.DARK else ThemeMode.LIGHT
        } else ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode; prefs.edit().putString(KEY_THEME, mode.key).apply() }
    fun setSnoozeMinutes(min: Int) { _snoozeMinutes.value = min; prefs.edit().putInt(KEY_SNOOZE, min).apply() }
    fun setVibrate(on: Boolean) { _vibrate.value = on; prefs.edit().putBoolean(KEY_VIBRATE, on).apply() }
    fun setSound(on: Boolean) { _sound.value = on; prefs.edit().putBoolean(KEY_SOUND, on).apply() }
    fun setOpenAppAfterSwipe(on: Boolean) { _openAppAfterSwipe.value = on; prefs.edit().putBoolean(KEY_OPEN_AFTER, on).apply() }
    fun setAccent(id: String) { _accent.value = id; prefs.edit().putString(KEY_ACCENT, id).apply() }
    fun setAvatar(id: String) { _avatar.value = id; prefs.edit().putString(KEY_AVATAR, id).apply() }
    fun setTone(id: String) { _tone.value = id; prefs.edit().putString(KEY_TONE, id).apply() }

    /** Preferences that travel with a backup (sync settings stay per device). */
    fun exportForBackup(): Map<String, String> = mapOf(
        KEY_THEME to _themeMode.value.key,
        KEY_SNOOZE to _snoozeMinutes.value.toString(),
        KEY_VIBRATE to _vibrate.value.toString(),
        KEY_SOUND to _sound.value.toString(),
        KEY_OPEN_AFTER to _openAppAfterSwipe.value.toString(),
        KEY_ACCENT to _accent.value,
        KEY_AVATAR to _avatar.value,
        KEY_TONE to _tone.value
    )

    fun importFromBackup(map: Map<String, String>) {
        map[KEY_THEME]?.let { setThemeMode(ThemeMode.from(it)) }
        map[KEY_SNOOZE]?.toIntOrNull()?.let { setSnoozeMinutes(it) }
        map[KEY_VIBRATE]?.toBooleanStrictOrNull()?.let { setVibrate(it) }
        map[KEY_SOUND]?.toBooleanStrictOrNull()?.let { setSound(it) }
        map[KEY_OPEN_AFTER]?.toBooleanStrictOrNull()?.let { setOpenAppAfterSwipe(it) }
        map[KEY_ACCENT]?.let { setAccent(it) }
        map[KEY_AVATAR]?.let { setAvatar(it) }
        map[KEY_TONE]?.let { setTone(it) }
    }

    fun clear() {
        prefs.edit().clear().apply()
        _themeMode.value = ThemeMode.SYSTEM
        _snoozeMinutes.value = 10
        _vibrate.value = true
        _sound.value = true
        _openAppAfterSwipe.value = true
        _accent.value = ShopCatalog.DEFAULT_ACCENT
        _avatar.value = ShopCatalog.DEFAULT_AVATAR
        _tone.value = ShopCatalog.DEFAULT_TONE
    }

    private companion object {
        const val KEY_THEME = "themeMode"
        const val KEY_LEGACY_DARK = "isDarkTheme"
        const val KEY_SNOOZE = "snoozeMinutes"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_SOUND = "sound"
        const val KEY_OPEN_AFTER = "openAppAfterSwipe"
        const val KEY_ACCENT = "accent"
        const val KEY_AVATAR = "avatar"
        const val KEY_TONE = "tone"
    }
}
