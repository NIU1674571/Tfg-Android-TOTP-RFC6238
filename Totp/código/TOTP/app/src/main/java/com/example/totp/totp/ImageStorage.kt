package com.example.totp.totp

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorage {

    /**
     * Guarda una imagen desde un Uri en el almacenamiento interno de la app.
     * El parámetro prefix permite distinguir entre iconos de distintos módulos
     * para evitar colisiones de nombres entre cuentas TOTP y credenciales
     * del Gestor de Contraseñas que pudieran tener el mismo ID.
     *
     * - prefix "icon" (por defecto): iconos de cuentas TOTP
     * - prefix "pwd_icon": iconos de credenciales del Gestor de Contraseñas
     */
    fun saveImageFromUri(
        context: Context,
        uri: Uri,
        accountId: Int,
        prefix: String = "icon"
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Borrar iconos anteriores con el mismo prefijo y id
            context.filesDir.listFiles()?.forEach {
                if (it.name.startsWith("${prefix}_${accountId}_")) {
                    it.delete()
                }
            }

            // Usar timestamp para evitar problemas de caché
            val file = File(
                context.filesDir,
                "${prefix}_${accountId}_${System.currentTimeMillis()}.png"
            )
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteImage(context: Context, accountId: Int, prefix: String = "icon") {
        context.filesDir.listFiles()?.forEach {
            if (it.name.startsWith("${prefix}_${accountId}_")) {
                it.delete()
            }
        }
    }
}
