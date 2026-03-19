package com.example.totp.model

/**
 * Modelo de datos para una cuenta TOTP.
 * En la Tarjeta 4 se convierte en una entidad de Room.
 */
data class TotpAccount(
    val id: Int,
    val name: String,           // nombre de usuario o correo
    val issuer: String,         // nombre del servicio (Google, GitHub...)
    val secretKey: String,      // SecretKey en Base32
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,
    val period: Int = 30
)
