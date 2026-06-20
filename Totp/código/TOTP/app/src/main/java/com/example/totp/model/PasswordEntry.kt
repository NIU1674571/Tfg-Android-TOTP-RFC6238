package com.example.totp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para las credenciales del Gestor de Contraseñas.
 * Cada registro almacena los datos de un servicio/cuenta:
 * - serviceName: nombre del servicio (ej: Google, GitHub)
 * - username: nombre de usuario o email
 * - encryptedPassword: contraseña cifrada (nunca en texto plano)
 * - url: URL del servicio (opcional)
 * - iconUri: ruta del icono personalizado del servicio (opcional)
 */
@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceName: String,
    val username: String,
    val encryptedPassword: String,
    val url: String = "",
    val iconUri: String? = null
)