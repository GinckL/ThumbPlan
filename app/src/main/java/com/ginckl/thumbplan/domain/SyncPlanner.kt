package com.ginckl.thumbplan.domain

enum class SyncAction { UPLOAD, DOWNLOAD, NOTHING, CONFLICT }

/**
 * Three-way decision for folder sync. [base] is the data fingerprint at the last successful sync, [local] the
 * fingerprint now, [remote] the one in the folder (null = no backup there yet).
 */
object SyncPlanner {
    fun decide(local: String, remote: String?, base: String?, localIsEmpty: Boolean, keepLocalOnConflict: Boolean? = null): SyncAction = when {
        remote == null -> SyncAction.UPLOAD
        remote == local -> SyncAction.NOTHING
        // brand-new install: take what is in the folder
        base == null && localIsEmpty -> SyncAction.DOWNLOAD
        local == base -> SyncAction.DOWNLOAD
        remote == base -> SyncAction.UPLOAD
        keepLocalOnConflict == true -> SyncAction.UPLOAD
        keepLocalOnConflict == false -> SyncAction.DOWNLOAD
        else -> SyncAction.CONFLICT
    }
}
