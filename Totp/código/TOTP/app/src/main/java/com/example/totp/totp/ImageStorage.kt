package com.example.totp.totp

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorage {

    fun saveImageFromUri(context: Context, uri: Uri, accountId: Int): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Borrar iconos anteriores de esta cuenta
            context.filesDir.listFiles()?.forEach {
                if (it.name.startsWith("icon_${accountId}_")) {
                    it.delete()
                }
            }

            // Usar timestamp para evitar problemas de caché
            val file = File(context.filesDir, "icon_${accountId}_${System.currentTimeMillis()}.png")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteImage(context: Context, accountId: Int) {
        context.filesDir.listFiles()?.forEach {
            if (it.name.startsWith("icon_${accountId}_")) {
                it.delete()
            }
        }
    }
}
