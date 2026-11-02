package com.ginckl.thumbplan.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ginckl.thumbplan.domain.SyncAction
import com.ginckl.thumbplan.domain.SyncPlanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncOutcome {
    data object Uploaded : SyncOutcome
    data class Downloaded(val info: BackupInfo) : SyncOutcome
    data object UpToDate : SyncOutcome

    /** Both this device and the sync folder changed since the last sync - the user has to pick a side. */
    data class Conflict(val remote: BackupInfo) : SyncOutcome
    data object NotConfigured : SyncOutcome
    data class Error(val message: String) : SyncOutcome
}

enum class Resolution { KEEP_THIS_DEVICE, USE_BACKUP }

/**
 * Folder-based sync: a backup ZIP lives in a folder the user picks (Google Drive, Dropbox, OneDrive, a USB drive,
 * Syncthing ... anything the system file picker can open). Devices pointed at the same folder converge:
 *   - only this device changed  -> upload
 *   - only the folder changed   -> download and restore
 *   - both changed              -> conflict, the user chooses
 */
@Singleton
class SyncManager @Inject constructor(
    private val backup: BackupManager,
    private val store: SyncStore,
    @ApplicationContext private val context: Context
) {
    private val lock = Mutex()

    suspend fun connect(treeUri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        store.setFolder(treeUri.toString(), DocumentFile.fromTreeUri(context, treeUri)?.name)
    }

    fun disconnect() = store.disconnect()

    suspend fun sync(resolution: Resolution? = null): SyncOutcome = lock.withLock {
        withContext(Dispatchers.IO) {
            val outcome = try {
                doSync(resolution)
            } catch (e: Exception) {
                SyncOutcome.Error(e.message ?: "Sync failed")
            }
            store.report(
                when (outcome) {
                    SyncOutcome.Uploaded -> "Backed up to your sync folder"
                    is SyncOutcome.Downloaded -> "Restored the latest backup"
                    SyncOutcome.UpToDate -> "Up to date"
                    is SyncOutcome.Conflict -> "Conflict: changes on two devices - open Settings to choose"
                    SyncOutcome.NotConfigured -> "No sync folder chosen"
                    is SyncOutcome.Error -> "Sync failed: ${outcome.message}"
                },
                success = outcome is SyncOutcome.Uploaded || outcome is SyncOutcome.Downloaded || outcome == SyncOutcome.UpToDate
            )
            outcome
        }
    }

    private suspend fun doSync(resolution: Resolution?): SyncOutcome {
        val uri = store.state.value.folderUri ?: return SyncOutcome.NotConfigured
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(uri))
        if (tree == null || !tree.exists() || !tree.canWrite()) return SyncOutcome.Error("The sync folder is no longer available")

        val remoteFile = tree.findFile(FILE_NAME)
        val remote = remoteFile?.let { f -> context.contentResolver.openInputStream(f.uri)?.use { backup.readInfo(it) } }
        val local = backup.localHash()

        if (remoteFile == null || remote == null) {
            upload(tree, remoteFile)
            store.lastSyncedHash = local
            return SyncOutcome.Uploaded
        }
        val base = store.lastSyncedHash
        val keepLocal = when (resolution) { Resolution.KEEP_THIS_DEVICE -> true; Resolution.USE_BACKUP -> false; null -> null }
        return when (SyncPlanner.decide(local, remote.hash, base, backup.isLocalEmpty(), keepLocal)) {
            SyncAction.NOTHING -> { store.lastSyncedHash = local; SyncOutcome.UpToDate }
            SyncAction.DOWNLOAD -> download(remoteFile, remote)
            SyncAction.UPLOAD -> { upload(tree, remoteFile); store.lastSyncedHash = local; SyncOutcome.Uploaded }
            SyncAction.CONFLICT -> SyncOutcome.Conflict(remote)
        }
    }

    private suspend fun download(file: DocumentFile, info: BackupInfo): SyncOutcome {
        context.contentResolver.openInputStream(file.uri)?.use { backup.restore(it) } ?: throw BackupException("Could not read the backup")
        store.lastSyncedHash = info.hash
        return SyncOutcome.Downloaded(info)
    }

    private suspend fun upload(tree: DocumentFile, existing: DocumentFile?) {
        // build to a temp file first so a failure can never leave a half-written backup in the folder
        val tmp = File.createTempFile("sync", ".zip", context.cacheDir)
        try {
            tmp.outputStream().use { backup.write(it) }
            val target = existing ?: tree.createFile("application/zip", FILE_NAME)
                ?: throw BackupException("Could not create the backup file in the folder")
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
                ?: throw BackupException("Could not write to the sync folder")
        } finally {
            tmp.delete()
        }
    }

    companion object {
        const val FILE_NAME = "thumbplan-sync.zip"
    }
}
