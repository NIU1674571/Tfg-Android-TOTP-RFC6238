package com.example.totp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room que representa una cuenta TOTP.
 * Se almacena en la tabla "totp_accounts" de la base de datos local.
 */
@Entity(tableName = "totp_accounts")
data class TotpAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // nombre de usuario o correo
    val issuer: String,         // nombre del servicio (Google, GitHub...)
    val secretKey: String,      // SecretKey en Base32
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,
    val period: Int = 30
)