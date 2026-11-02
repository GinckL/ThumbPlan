package com.ginckl.thumbplan.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtil {
    /** Copies a picked image into app storage (picker URIs are temporary). Returns the absolute path or null. */
    fun copyUriToLocal(context: Context, uri: Uri): String? = copy(context, uri, "image_", "jpg")

    /** Copies a picked audio file into app storage so the alarm can play it later. */
    fun copyAudioToLocal(context: Context, uri: Uri): String? {
        val ext = context.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "mp3"
        return copy(context, uri, "audio_", ext)
    }

    private fun copy(context: Context, uri: Uri, prefix: String, ext: String): String? {
        val file = File(context.filesDir, "$prefix${UUID.randomUUID()}.$ext")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { out -> input.copyTo(out) }
            } ?: return null
            file.absolutePath
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
