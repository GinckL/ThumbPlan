package com.ginckl.thumbplan.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.ginckl.thumbplan.data.local.AppDatabase
import com.ginckl.thumbplan.data.local.dao.CoinDao
import com.ginckl.thumbplan.data.local.dao.CoinHistoryItem
import com.ginckl.thumbplan.data.local.dao.DoneTaskDao
import com.ginckl.thumbplan.data.local.dao.PlanTaskDao
import com.ginckl.thumbplan.data.local.dao.ShopDao
import com.ginckl.thumbplan.data.local.dao.UserDao
import com.ginckl.thumbplan.data.local.entity.CoinEntity
import com.ginckl.thumbplan.data.local.entity.DoneTaskEntity
import com.ginckl.thumbplan.data.local.entity.FrozenDayEntity
import com.ginckl.thumbplan.data.local.entity.InventoryEntity
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.data.local.entity.UserEntity
import com.ginckl.thumbplan.data.prefs.SettingsStore
import com.ginckl.thumbplan.domain.FreezePlanner
import com.ginckl.thumbplan.domain.Recurrence
import com.ginckl.thumbplan.domain.RepeatMode
import com.ginckl.thumbplan.domain.ShopCatalog
import com.ginckl.thumbplan.domain.Streaks
import com.ginckl.thumbplan.services.alarm.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CompletionResult {
    data class Completed(
        val task: PlanTaskEntity,
        val coins: Int,
        val streak: Int,
        val doneTaskId: Long,
        val boosted: Boolean = false
    ) : CompletionResult

    data object AlreadyDone : CompletionResult
    data object NotFound : CompletionResult

    /** Task is scheduled for a later day; completing it now would let users farm coins. */
    data object TooEarly : CompletionResult
}

sealed interface PurchaseResult {
    data class Bought(val itemName: String) : PurchaseResult
    data object NotEnoughCoins : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object StackFull : PurchaseResult
    data object Unknown : PurchaseResult
}

/**
 * Single source of truth for tasks, coins, streaks and the shop. Used by the UI *and* by the alarm
 * components so that completing a task always follows the same rules.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase,
    private val taskDao: PlanTaskDao,
    private val userDao: UserDao,
    private val doneDao: DoneTaskDao,
    private val coinDao: CoinDao,
    private val shopDao: ShopDao,
    private val scheduler: AlarmScheduler,
    private val settings: SettingsStore,
    @ApplicationContext private val context: Context
) {
    val tasks: Flow<List<PlanTaskEntity>> = taskDao.getAllTasks()
    val user: Flow<UserEntity?> = userDao.getLocalUser()
    val doneTimes: Flow<List<Long>> = doneDao.observeDoneTimes()
    val frozenDays: Flow<List<Long>> = shopDao.observeFrozenDays()
    val lifetimeCoins: Flow<Int> = coinDao.observeLifetimeCoins()
    val coinHistory: Flow<List<CoinHistoryItem>> = coinDao.observeHistory()
    val inventory: Flow<Map<String, Int>> = shopDao.observeInventory().map { list -> list.associate { it.itemId to it.quantity } }

    suspend fun getTask(id: Long): PlanTaskEntity? = taskDao.getTaskById(id)

    suspend fun createUser(name: String) {
        if (userDao.getLocalUserSync() == null) userDao.insertUser(UserEntity(name = name.trim().ifBlank { "Friend" }))
    }

    suspend fun renameUser(name: String) {
        val u = userDao.getLocalUserSync() ?: return
        if (name.isNotBlank()) userDao.updateUser(u.copy(name = name.trim()))
    }

    private suspend fun requireUser(): UserEntity =
        userDao.getLocalUserSync() ?: run {
            userDao.insertUser(UserEntity(name = "Friend"))
            userDao.getLocalUserSync()!!
        }

    // ---------------------------------------------------------------- tasks

    suspend fun saveTask(task: PlanTaskEntity): Long {
        val user = requireUser()
        val id = taskDao.insertTask(task.copy(id = 0, userId = user.id))
        scheduler.schedule(task.copy(id = id, userId = user.id))
        return id
    }

    suspend fun updateTask(task: PlanTaskEntity) {
        scheduler.cancel(task)
        taskDao.updateTask(task)
        if (task.status == "pending") scheduler.schedule(task)
    }

    suspend fun deleteTask(task: PlanTaskEntity, deleteFiles: Boolean = true) {
        scheduler.cancel(task)
        taskDao.deleteTaskById(task.id)
        if (deleteFiles) {
            deleteLocalFile(task.imagePath)
            deleteLocalFile(task.audioPath)
        }
    }

    /** Re-inserts a task removed by mistake (undo). Files are kept until the snackbar expires, so paths stay valid. */
    suspend fun restoreTask(task: PlanTaskEntity) {
        val id = taskDao.insertTask(task)
        scheduler.schedule(task.copy(id = id))
    }

    suspend fun completeTask(taskId: Long, now: Long = System.currentTimeMillis()): CompletionResult {
        val zone = ZoneId.systemDefault()
        val outcome = db.withTransaction {
            val task = taskDao.getTaskById(taskId) ?: return@withTransaction CompletionResult.NotFound
            if (task.status == "completed") return@withTransaction CompletionResult.AlreadyDone
            val endOfToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            if (task.scheduledTime >= endOfToday) return@withTransaction CompletionResult.TooEarly

            // A Double Coins power-up is consumed by the next completed task.
            val boosts = shopDao.quantity(ShopCatalog.BOOST) ?: 0
            val boosted = boosts > 0
            val coins = if (boosted) task.rewardCoins * 2 else task.rewardCoins
            if (boosted) shopDao.upsert(InventoryEntity(ShopCatalog.BOOST, boosts - 1))

            taskDao.updateTask(task.copy(status = "completed"))
            val doneId = doneDao.insertDoneTask(DoneTaskEntity(taskId = task.id, doneTime = now))
            coinDao.insertCoin(CoinEntity(amount = coins, doneTaskId = doneId, label = if (boosted) "⚡ Double Coins" else null))
            val streak = currentStreak(zone)
            val user = requireUser()
            userDao.updateUser(user.copy(totalCoins = user.totalCoins + coins, streakCount = streak, lastSeen = now))
            CompletionResult.Completed(task, coins, streak, doneId, boosted)
        }
        if (outcome is CompletionResult.Completed) {
            scheduler.cancel(outcome.task)
            ensureNextOccurrence(outcome.task)
        }
        return outcome
    }

    private suspend fun currentStreak(zone: ZoneId): Int {
        val days = Streaks.toLocalDates(doneDao.getDoneTimes(), zone) + frozenSet()
        return Streaks.current(days, LocalDate.now(zone))
    }

    private suspend fun frozenSet(): Set<LocalDate> = shopDao.getFrozenDays().map { LocalDate.ofEpochDay(it) }.toSet()

    suspend fun saveCompletionNote(doneTaskId: Long, note: String) {
        if (note.isNotBlank()) doneDao.updateComment(doneTaskId, note.trim())
    }

    /** For repeating tasks make sure the next occurrence exists (idempotent). */
    suspend fun ensureNextOccurrence(task: PlanTaskEntity, now: Long = System.currentTimeMillis()) {
        val mode = RepeatMode.from(task.repeatMode)
        if (mode == RepeatMode.NONE) return
        var next = Recurrence.next(task.scheduledTime, mode) ?: return
        // Skip occurrences that are already in the past (e.g. phone was off for days).
        while (next <= now) next = Recurrence.next(next, mode) ?: return
        if (taskDao.countPendingSeries(task.title, task.repeatMode, task.scheduledTime) > 0) return
        val id = taskDao.insertTask(task.copy(id = 0, scheduledTime = next, status = "pending"))
        scheduler.schedule(task.copy(id = id, scheduledTime = next, status = "pending"))
    }

    fun snooze(task: PlanTaskEntity) {
        scheduler.snooze(task, System.currentTimeMillis() + settings.snoozeMinutes.value * 60_000L)
    }

    /** Re-arms every pending alarm (after reboot, app update, clock/timezone change, permission grant, restore). */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        for (task in taskDao.getPendingTasks()) {
            if (task.scheduledTime > now) scheduler.schedule(task) else ensureNextOccurrence(task, now)
        }
    }

    // ---------------------------------------------------------------- shop

    /** Spends coins on [itemId]. Everything happens in one transaction so a crash can't lose coins. */
    suspend fun buy(itemId: String): PurchaseResult {
        val item = ShopCatalog.find(itemId) ?: return PurchaseResult.Unknown
        return db.withTransaction {
            val owned = shopDao.quantity(item.id) ?: 0
            if (!item.consumable && (owned > 0 || ShopCatalog.isFree(item))) return@withTransaction PurchaseResult.AlreadyOwned
            if (item.consumable && owned >= ShopCatalog.MAX_STACK) return@withTransaction PurchaseResult.StackFull
            val user = requireUser()
            if (user.totalCoins < item.price) return@withTransaction PurchaseResult.NotEnoughCoins
            userDao.updateUser(user.copy(totalCoins = user.totalCoins - item.price))
            coinDao.insertCoin(CoinEntity(amount = -item.price, label = "${item.emoji} ${item.name}"))
            shopDao.upsert(InventoryEntity(item.id, owned + 1))
            PurchaseResult.Bought(item.name)
        }
    }

    /** Covers missed days with Streak Freezes when that saves the streak. Safe to call any time. */
    suspend fun applyStreakFreezes(today: LocalDate = LocalDate.now()): Int {
        val zone = ZoneId.systemDefault()
        return db.withTransaction {
            val available = shopDao.quantity(ShopCatalog.FREEZE) ?: 0
            if (available <= 0) return@withTransaction 0
            val active = Streaks.toLocalDates(doneDao.getDoneTimes(), zone) + frozenSet()
            val toFreeze = FreezePlanner.plan(active, today, available)
            if (toFreeze.isEmpty()) return@withTransaction 0
            toFreeze.forEach { shopDao.insertFrozen(FrozenDayEntity(it.toEpochDay())) }
            shopDao.upsert(InventoryEntity(ShopCatalog.FREEZE, available - toFreeze.size))
            toFreeze.size
        }
    }

    // ---------------------------------------------------------------- reset

    suspend fun resetAll() = withContext(Dispatchers.IO) {
        for (task in taskDao.getPendingTasks()) scheduler.cancel(task)
        db.clearAllTables()
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("image_") || it.name.startsWith("audio_") }
            ?.forEach { it.delete() }
        settings.clear()
    }

    private fun deleteLocalFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val f = File(path)
            if (f.exists() && f.parentFile == context.filesDir) f.delete()
        }
    }
}
