package com.ginckl.thumbplan.data.backup

import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import com.ginckl.thumbplan.BuildConfig
import com.ginckl.thumbplan.data.local.AppDatabase
import com.ginckl.thumbplan.data.local.dao.CoinDao
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
import com.ginckl.thumbplan.services.alarm.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** What a backup file says about itself (read without restoring it). */
data class BackupInfo(
    val exportedAt: Long,
    val hash: String,
    val device: String,
    val appVersion: String,
    val tasks: Int,
    val coins: Int
)

class BackupException(message: String) : Exception(message)

/**
 * Backs the whole app up into one ZIP (`manifest.json`, `data.json`, `media/…`) and restores it.
 * The same file is what [SyncManager] keeps in the user's sync folder.
 */
@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    private val userDao: UserDao,
    private val taskDao: PlanTaskDao,
    private val doneDao: DoneTaskDao,
    private val coinDao: CoinDao,
    private val shopDao: ShopDao,
    private val settings: SettingsStore,
    private val scheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) {
    private class Data(val json: String, val mediaFiles: List<File>)

    /** Stable fingerprint of everything that would be backed up. */
    suspend fun localHash(): String = sha256(snapshot().json)

    suspend fun isLocalEmpty(): Boolean = withContext(Dispatchers.IO) {
        taskDao.getAll().isEmpty() && coinDao.getAll().isEmpty() && doneDao.getAll().isEmpty() && shopDao.getInventory().isEmpty()
    }

    suspend fun write(out: OutputStream): BackupInfo = withContext(Dispatchers.IO) {
        val data = snapshot()
        val info = BackupInfo(
            exportedAt = System.currentTimeMillis(),
            hash = sha256(data.json),
            device = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            appVersion = BuildConfig.VERSION_NAME,
            tasks = JSONObject(data.json).getJSONArray("tasks").length(),
            coins = JSONObject(data.json).getJSONArray("users").optJSONObject(0)?.optInt("totalCoins") ?: 0
        )
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(
                JSONObject()
                    .put("format", FORMAT)
                    .put("exportedAt", info.exportedAt)
                    .put("hash", info.hash)
                    .put("device", info.device)
                    .put("appVersion", info.appVersion)
                    .put("tasks", info.tasks)
                    .put("coins", info.coins)
                    .toString().toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(data.json.toByteArray())
            zip.closeEntry()
            for (f in data.mediaFiles) {
                zip.putNextEntry(ZipEntry("media/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        info
    }

    /** Reads only the manifest of a backup. Returns null if [input] is not a ThumbPlan backup. */
    suspend fun readInfo(input: InputStream): BackupInfo? = withContext(Dispatchers.IO) {
        runCatching {
            ZipInputStream(input).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    if (e.name == "manifest.json") {
                        val m = JSONObject(String(zip.readBytes()))
                        if (m.optInt("format") != FORMAT) return@runCatching null
                        return@runCatching BackupInfo(
                            m.getLong("exportedAt"), m.getString("hash"), m.optString("device"),
                            m.optString("appVersion"), m.optInt("tasks"), m.optInt("coins")
                        )
                    }
                    e = zip.nextEntry
                }
                null
            }
        }.getOrNull()
    }

    /** Replaces ALL local data with the contents of the backup. Throws [BackupException] (and changes nothing) if it is invalid. */
    suspend fun restore(input: InputStream) = withContext(Dispatchers.IO) {
        // 1) parse everything first so a broken file can't wipe the user's data
        var manifestOk = false
        var dataJson: String? = null
        val media = HashMap<String, ByteArray>()
        runCatching {
            ZipInputStream(input).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    when {
                        e.name == "manifest.json" -> manifestOk = JSONObject(String(zip.readBytes())).optInt("format") == FORMAT
                        e.name == "data.json" -> dataJson = String(zip.readBytes())
                        e.name.startsWith("media/") && !e.isDirectory -> media[File(e.name).name] = zip.readBytes()
                    }
                    e = zip.nextEntry
                }
            }
        }.onFailure { throw BackupException("This file isn't a valid ThumbPlan backup.") }
        if (!manifestOk || dataJson == null) throw BackupException("This file isn't a valid ThumbPlan backup.")

        val root = try { JSONObject(dataJson!!) } catch (e: Exception) { throw BackupException("The backup is damaged.") }
        val users: List<UserEntity>
        val tasks: List<PlanTaskEntity>
        val done: List<DoneTaskEntity>
        val coins: List<CoinEntity>
        val inventory: List<InventoryEntity>
        val frozen: List<FrozenDayEntity>
        try {
            users = root.getJSONArray("users").objects().map(::userOf)
            tasks = root.getJSONArray("tasks").objects().map(::taskOf)
            done = root.getJSONArray("done").objects().map(::doneOf)
            coins = root.getJSONArray("coins").objects().map(::coinOf)
            inventory = root.getJSONArray("inventory").objects().map { InventoryEntity(it.getString("itemId"), it.getInt("quantity")) }
            frozen = root.getJSONArray("frozen").objects().map { FrozenDayEntity(it.getLong("epochDay")) }
        } catch (e: Exception) {
            throw BackupException("The backup is damaged.")
        }

        // 2) wipe + load
        for (t in taskDao.getPendingTasks()) scheduler.cancel(t)
        db.clearAllTables()
        // media first, so restored rows can point at real files
        for ((name, bytes) in media) File(context.filesDir, name).writeBytes(bytes)
        fun local(name: String?): String? = name?.let { File(context.filesDir, it) }?.takeIf { it.exists() }?.absolutePath
        db.withTransaction {
            users.forEach { userDao.insertUser(it) }
            taskDao.insertAll(tasks.map { it.copy(imagePath = local(it.imagePath), audioPath = local(it.audioPath)) })
            doneDao.insertAll(done)
            coinDao.insertAll(coins)
            shopDao.insertAllInventory(inventory)
            shopDao.insertAllFrozen(frozen)
        }
        root.optJSONObject("settings")?.let { s ->
            settings.importFromBackup(s.keys().asSequence().associateWith { s.getString(it) })
        }
        // 3) arm the alarms of the restored tasks
        val now = System.currentTimeMillis()
        for (t in taskDao.getPendingTasks()) if (t.scheduledTime > now) scheduler.schedule(t)
    }

    // ---------------------------------------------------------------- snapshot

    private suspend fun snapshot(): Data = withContext(Dispatchers.IO) {
        val media = LinkedHashMap<String, File>()
        fun mediaName(path: String?): String? {
            if (path.isNullOrBlank()) return null
            val f = File(path)
            if (!f.exists() || f.parentFile != context.filesDir) return null
            media[f.name] = f
            return f.name
        }
        val users = userDao.getAll().map(::json)
        val tasks = taskDao.getAll().map { json(it, mediaName(it.imagePath), mediaName(it.audioPath)) }
        val settingsJson = JSONObject().also { o -> settings.exportForBackup().toSortedMap().forEach { (k, v) -> o.put(k, v) } }
        val root = JSONObject()
            .put("users", JSONArray(users))
            .put("tasks", JSONArray(tasks))
            .put("done", JSONArray(doneDao.getAll().map(::json)))
            .put("coins", JSONArray(coinDao.getAll().map(::json)))
            .put("inventory", JSONArray(shopDao.getInventory().sortedBy { it.itemId }.map { JSONObject().put("itemId", it.itemId).put("quantity", it.quantity) }))
            .put("frozen", JSONArray(shopDao.getFrozenDays().sorted().map { JSONObject().put("epochDay", it) }))
            .put("settings", settingsJson)
        Data(root.toString(), media.values.toList())
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.str(k: String): String? = if (isNull(k)) null else getString(k)
    private fun JSONObject.lng(k: String): Long? = if (isNull(k)) null else getLong(k)
    private fun JSONObject.dbl(k: String): Double? = if (isNull(k)) null else getDouble(k)

    private fun json(u: UserEntity) = JSONObject()
        .put("id", u.id).put("name", u.name).put("username", u.username ?: JSONObject.NULL)
        .put("dateOfBirth", u.dateOfBirth ?: JSONObject.NULL).put("googleId", u.googleId ?: JSONObject.NULL)
        .put("appleId", u.appleId ?: JSONObject.NULL).put("avatarPath", JSONObject.NULL)
        .put("phone", u.phone ?: JSONObject.NULL).put("lastSeen", u.lastSeen ?: JSONObject.NULL)
        .put("referralCode", u.referralCode).put("streakCount", u.streakCount).put("totalCoins", u.totalCoins)

    private fun userOf(o: JSONObject) = UserEntity(
        id = o.getLong("id"), name = o.getString("name"), username = o.str("username"), dateOfBirth = o.lng("dateOfBirth"),
        googleId = o.lng("googleId"), appleId = o.lng("appleId"), avatarPath = null, phone = o.str("phone"),
        lastSeen = o.lng("lastSeen"), referralCode = o.getString("referralCode"),
        streakCount = o.getInt("streakCount"), totalCoins = o.getInt("totalCoins")
    )

    private fun json(t: PlanTaskEntity, image: String?, audio: String?) = JSONObject()
        .put("id", t.id).put("userId", t.userId).put("title", t.title).put("description", t.description ?: JSONObject.NULL)
        .put("category", t.category ?: JSONObject.NULL).put("colorHex", t.colorHex ?: JSONObject.NULL)
        .put("image", image ?: JSONObject.NULL).put("audio", audio ?: JSONObject.NULL)
        .put("scheduledTime", t.scheduledTime).put("rewardCoins", t.rewardCoins).put("isWeekend", t.isWeekend)
        .put("reminderIntervals", t.reminderIntervals).put("status", t.status).put("repeatMode", t.repeatMode)

    private fun taskOf(o: JSONObject) = PlanTaskEntity(
        id = o.getLong("id"), userId = o.getLong("userId"), title = o.getString("title"), description = o.str("description"),
        category = o.str("category"), colorHex = o.str("colorHex"),
        imagePath = o.str("image"), audioPath = o.str("audio"), // file names; resolved to real paths after the media is written
        scheduledTime = o.getLong("scheduledTime"), rewardCoins = o.getInt("rewardCoins"), isWeekend = o.getBoolean("isWeekend"),
        reminderIntervals = o.getString("reminderIntervals"), status = o.getString("status"), repeatMode = o.getString("repeatMode")
    )

    private fun json(d: DoneTaskEntity) = JSONObject()
        .put("id", d.id).put("taskId", d.taskId).put("doneTime", d.doneTime).put("comment", d.comment ?: JSONObject.NULL)
        .put("locationLabel", d.locationLabel ?: JSONObject.NULL).put("lat", d.lat ?: JSONObject.NULL).put("lng", d.lng ?: JSONObject.NULL)

    private fun doneOf(o: JSONObject) = DoneTaskEntity(
        id = o.getLong("id"), taskId = o.getLong("taskId"), doneTime = o.getLong("doneTime"), comment = o.str("comment"),
        locationLabel = o.str("locationLabel"), lat = o.dbl("lat"), lng = o.dbl("lng")
    )

    private fun json(c: CoinEntity) = JSONObject()
        .put("id", c.id).put("amount", c.amount).put("doneTaskId", c.doneTaskId ?: JSONObject.NULL)
        .put("inviteUserId", c.inviteUserId ?: JSONObject.NULL).put("daily", c.daily).put("label", c.label ?: JSONObject.NULL)

    private fun coinOf(o: JSONObject) = CoinEntity(
        id = o.getLong("id"), amount = o.getInt("amount"), doneTaskId = o.lng("doneTaskId"),
        inviteUserId = o.lng("inviteUserId"), daily = o.getBoolean("daily"), label = o.str("label")
    )

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val FORMAT = 1
    }
}
