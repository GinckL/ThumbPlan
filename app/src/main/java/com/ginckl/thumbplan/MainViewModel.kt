package com.ginckl.thumbplan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ginckl.thumbplan.data.backup.BackupInfo
import com.ginckl.thumbplan.data.backup.BackupManager
import com.ginckl.thumbplan.data.backup.Resolution
import com.ginckl.thumbplan.data.backup.SyncManager
import com.ginckl.thumbplan.data.backup.SyncOutcome
import com.ginckl.thumbplan.data.backup.SyncStore
import com.ginckl.thumbplan.data.backup.SyncUi
import com.ginckl.thumbplan.data.local.dao.CoinHistoryItem
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.data.local.entity.UserEntity
import com.ginckl.thumbplan.data.prefs.SettingsStore
import com.ginckl.thumbplan.data.prefs.ThemeMode
import com.ginckl.thumbplan.data.repo.CompletionResult
import com.ginckl.thumbplan.data.repo.PurchaseResult
import com.ginckl.thumbplan.data.repo.TaskRepository
import com.ginckl.thumbplan.domain.Badge
import com.ginckl.thumbplan.domain.Badges
import com.ginckl.thumbplan.domain.ItemKind
import com.ginckl.thumbplan.domain.Level
import com.ginckl.thumbplan.domain.Progression
import com.ginckl.thumbplan.domain.ShopCatalog
import com.ginckl.thumbplan.domain.Stats
import com.ginckl.thumbplan.domain.Streaks
import com.ginckl.thumbplan.services.work.SyncWorker
import com.ginckl.thumbplan.util.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface UserState {
    data object Loading : UserState
    data object Missing : UserState
    data class Ready(val user: UserEntity) : UserState
}

/** Everything the Rewards screen needs, derived from completion history. */
data class Insights(
    val level: Level = Progression.level(0),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDone: Int = 0,
    val lifetimeCoins: Int = 0,
    /** Completions for the last 7 days, oldest first (last entry = today). */
    val last7: List<Pair<LocalDate, Int>> = emptyList(),
    val badges: List<Badge> = emptyList()
)

/** The "task completed" celebration shown inside the app after swiping on the alarm page. */
data class Celebration(val title: String, val coins: Int, val streak: Int, val doneTaskId: Long, val taskId: Long, val boosted: Boolean)

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Deleted(val task: PlanTaskEntity) : UiEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val settings: SettingsStore,
    private val backup: BackupManager,
    private val sync: SyncManager,
    private val syncStore: SyncStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ---------------------------------------------------------------- settings
    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val snoozeMinutes: StateFlow<Int> = settings.snoozeMinutes
    val vibrate: StateFlow<Boolean> = settings.vibrate
    val sound: StateFlow<Boolean> = settings.sound
    val openAppAfterSwipe: StateFlow<Boolean> = settings.openAppAfterSwipe
    val accent: StateFlow<String> = settings.accent
    val avatar: StateFlow<String> = settings.avatar
    val tone: StateFlow<String> = settings.tone

    // ---------------------------------------------------------------- data
    val userState: StateFlow<UserState> = repo.user
        .map { if (it == null) UserState.Missing else UserState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserState.Loading)

    val tasks: StateFlow<List<PlanTaskEntity>> = repo.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coinHistory: StateFlow<List<CoinHistoryItem>> = repo.coinHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<Map<String, Int>> = repo.inventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val insights: StateFlow<Insights> = combine(repo.doneTimes, repo.frozenDays, repo.lifetimeCoins) { times, frozen, lifetime ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = Streaks.toLocalDates(times, zone) + frozen.map { LocalDate.ofEpochDay(it) }
        val perDay = times.groupingBy { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.eachCount()
        val current = Streaks.current(days, today)
        val longest = Streaks.longest(days)
        val early = times.count { Instant.ofEpochMilli(it).atZone(zone).hour < 8 }
        Insights(
            level = Progression.level(lifetime),
            currentStreak = current,
            longestStreak = longest,
            totalDone = times.size,
            lifetimeCoins = lifetime,
            last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to (perDay[it] ?: 0) },
            badges = Badges.evaluate(Stats(times.size, lifetime, current, longest, early))
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Insights())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    val selectedTaskId: StateFlow<Long?> = _selectedTaskId.asStateFlow()

    /** Task being edited in the editor sheet; null = closed, id 0 = creating. */
    private val _editor = MutableStateFlow<PlanTaskEntity?>(null)
    val editor: StateFlow<PlanTaskEntity?> = _editor.asStateFlow()

    private val _celebration = MutableStateFlow<Celebration?>(null)
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()

    private val pendingFileDeletes = mutableMapOf<Long, Job>()

    fun openTask(id: Long?) { _selectedTaskId.value = id }
    fun startCreate() { _editor.value = PlanTaskEntity(id = 0, userId = 0, title = "", scheduledTime = 0) }
    fun startEdit(task: PlanTaskEntity) { _editor.value = task }
    fun closeEditor() { _editor.value = null }

    fun completeOnboarding(name: String) = viewModelScope.launch { repo.createUser(name) }
    fun renameUser(name: String) = viewModelScope.launch { repo.renameUser(name) }

    fun showCelebration(c: Celebration) { _celebration.value = c }

    fun finishCelebration(note: String, openTask: Boolean) {
        val c = _celebration.value ?: return
        _celebration.value = null
        viewModelScope.launch {
            repo.saveCompletionNote(c.doneTaskId, note)
            if (openTask) _selectedTaskId.value = c.taskId
        }
    }

    /** Called whenever the app comes to the foreground. */
    fun onAppStart() {
        viewModelScope.launch {
            val covered = repo.applyStreakFreezes()
            if (covered > 0) _events.emit(UiEvent.Message("🧊 A Streak Freeze saved your streak ($covered ${if (covered == 1) "day" else "days"} covered)"))
        }
        if (syncStore.state.value.enabled) syncNow(silent = true)
    }

    // ---------------------------------------------------------------- tasks
    fun saveTask(task: PlanTaskEntity) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repo.saveTask(task)
                _events.emit(UiEvent.Message("Task added"))
            } else {
                repo.updateTask(task)
                _events.emit(UiEvent.Message("Task updated"))
            }
        }
    }

    fun completeTask(task: PlanTaskEntity) {
        viewModelScope.launch {
            when (val r = repo.completeTask(task.id)) {
                is CompletionResult.Completed -> {
                    val streak = if (r.streak > 1) " · 🔥 ${r.streak}-day streak" else ""
                    val boost = if (r.boosted) " ⚡ doubled!" else ""
                    _events.emit(UiEvent.Message("+${r.coins} coins$boost$streak"))
                }
                CompletionResult.TooEarly -> _events.emit(UiEvent.Message("This task is planned for a later day - come back then to earn its coins."))
                CompletionResult.AlreadyDone, CompletionResult.NotFound -> Unit
            }
        }
    }

    fun deleteTask(task: PlanTaskEntity) {
        viewModelScope.launch {
            repo.deleteTask(task, deleteFiles = false)
            if (_selectedTaskId.value == task.id) _selectedTaskId.value = null
            _events.emit(UiEvent.Deleted(task))
            // Keep the photo/audio around while the Undo snackbar is visible, then clean up.
            pendingFileDeletes[task.id]?.cancel()
            pendingFileDeletes[task.id] = launch {
                delay(12_000)
                FileUtil.delete(task.imagePath)
                FileUtil.delete(task.audioPath)
                pendingFileDeletes.remove(task.id)
            }
        }
    }

    fun undoDelete(task: PlanTaskEntity) {
        pendingFileDeletes.remove(task.id)?.cancel()
        viewModelScope.launch { repo.restoreTask(task) }
    }

    // ---------------------------------------------------------------- shop
    fun owns(itemId: String, inventory: Map<String, Int> = this.inventory.value): Boolean {
        val item = ShopCatalog.find(itemId) ?: return false
        return ShopCatalog.isFree(item) || (inventory[itemId] ?: 0) > 0
    }

    fun buy(itemId: String) {
        viewModelScope.launch {
            val item = ShopCatalog.find(itemId) ?: return@launch
            when (val r = repo.buy(itemId)) {
                is PurchaseResult.Bought -> {
                    when (item.kind) { // equip right away: that's the fun part
                        ItemKind.ACCENT -> settings.setAccent(item.id)
                        ItemKind.AVATAR -> settings.setAvatar(item.id)
                        ItemKind.TONE -> settings.setTone(item.id)
                        ItemKind.BOOSTER -> Unit
                    }
                    _events.emit(UiEvent.Message("${item.emoji} ${r.itemName} unlocked!"))
                }
                PurchaseResult.NotEnoughCoins -> _events.emit(UiEvent.Message("Not enough coins yet - finish a few tasks!"))
                PurchaseResult.AlreadyOwned -> _events.emit(UiEvent.Message("You already own this"))
                PurchaseResult.StackFull -> _events.emit(UiEvent.Message("You can hold at most ${ShopCatalog.MAX_STACK}"))
                PurchaseResult.Unknown -> Unit
            }
        }
    }

    fun equip(itemId: String) {
        val item = ShopCatalog.find(itemId) ?: return
        if (!owns(itemId)) return
        when (item.kind) {
            ItemKind.ACCENT -> settings.setAccent(item.id)
            ItemKind.AVATAR -> settings.setAvatar(item.id)
            ItemKind.TONE -> settings.setTone(item.id)
            ItemKind.BOOSTER -> Unit
        }
    }

    // ---------------------------------------------------------------- settings actions
    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setSnoozeMinutes(min: Int) = settings.setSnoozeMinutes(min)
    fun setVibrate(on: Boolean) = settings.setVibrate(on)
    fun setSound(on: Boolean) = settings.setSound(on)
    fun setOpenAppAfterSwipe(on: Boolean) = settings.setOpenAppAfterSwipe(on)

    fun reschedule() = viewModelScope.launch { repo.rescheduleAll() }

    fun resetAll() {
        viewModelScope.launch {
            repo.resetAll()
            syncStore.lastSyncedHash = null
            _selectedTaskId.value = null
            _events.emit(UiEvent.Message("All data erased"))
        }
    }

    // ---------------------------------------------------------------- backup & sync
    val syncUi: StateFlow<SyncUi> = syncStore.state

    private val _syncConflict = MutableStateFlow<BackupInfo?>(null)
    val syncConflict: StateFlow<BackupInfo?> = _syncConflict.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** An import the user picked that is waiting for "replace everything?" confirmation. */
    private val _pendingImport = MutableStateFlow<Pair<Uri, BackupInfo>?>(null)
    val pendingImport: StateFlow<Pair<Uri, BackupInfo>?> = _pendingImport.asStateFlow()

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { backup.write(it) } ?: error("no stream") }
            }.isSuccess
            _events.emit(UiEvent.Message(if (ok) "Backup saved" else "Couldn't save the backup"))
        }
    }

    fun prepareImport(uri: Uri) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { backup.readInfo(it) } }
            if (info == null) _events.emit(UiEvent.Message("That file isn't a ThumbPlan backup"))
            else _pendingImport.value = uri to info
        }
    }

    fun cancelImport() { _pendingImport.value = null }

    /** Used on the welcome screen: there is nothing to overwrite yet, so no confirmation. */
    fun restoreFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { backup.restore(it) } ?: error("no stream") }
            }
            _events.emit(UiEvent.Message(if (result.isSuccess) "Backup restored" else (result.exceptionOrNull()?.message ?: "Restore failed")))
        }
    }

    fun confirmImport() {
        val (uri, _) = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { backup.restore(it) } ?: error("no stream") }
            }
            _events.emit(UiEvent.Message(if (result.isSuccess) "Backup restored" else (result.exceptionOrNull()?.message ?: "Restore failed")))
        }
    }

    fun connectSyncFolder(uri: Uri) {
        viewModelScope.launch {
            sync.connect(uri)
            SyncWorker.schedulePeriodic(context)
            syncNow()
        }
    }

    fun setSyncEnabled(on: Boolean) {
        syncStore.setEnabled(on)
        if (on) { SyncWorker.schedulePeriodic(context); syncNow() } else SyncWorker.cancelPeriodic(context)
    }

    fun disconnectSync() {
        sync.disconnect()
        SyncWorker.cancelPeriodic(context)
    }

    fun syncNow(silent: Boolean = false, resolution: Resolution? = null) {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            val outcome = sync.sync(resolution)
            _syncing.value = false
            when (outcome) {
                is SyncOutcome.Conflict -> _syncConflict.value = outcome.remote
                is SyncOutcome.Downloaded -> _events.emit(UiEvent.Message("☁️ Synced from your other device"))
                is SyncOutcome.Uploaded -> if (!silent) _events.emit(UiEvent.Message("☁️ Backed up"))
                SyncOutcome.UpToDate -> if (!silent) _events.emit(UiEvent.Message("Already up to date"))
                is SyncOutcome.Error -> if (!silent) _events.emit(UiEvent.Message(outcome.message))
                SyncOutcome.NotConfigured -> Unit
            }
        }
    }

    fun resolveConflict(resolution: Resolution?) {
        _syncConflict.value = null
        if (resolution != null) syncNow(resolution = resolution)
    }
}
