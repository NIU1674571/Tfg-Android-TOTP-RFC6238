package com.example.totp.totp

import android.net.Uri

/**
 * Parser de URIs otpauth:// según el formato de Google Authenticator.
 * Formato: otpauth://totp/Emisor:usuario?secret=CLAVE&issuer=Emisor&algorithm=SHA1&digits=6&period=30
 *
 * Fuente: https://github.com/google/google-authenticator/wiki/Key-Uri-Format
 */
data class OtpAuthData(
    val name: String,
    val issuer: String,
    val secretKey: String,
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,
    val period: Int = 30
)

object OtpAuthParser {

    fun parse(uriString: String): OtpAuthData? {
        try {
            val uri = Uri.parse(uriString)

            // Verificar que es otpauth://totp/
            if (uri.scheme != "otpauth" || uri.host != "totp") {
                return null
            }

            // Obtener la etiqueta (path): puede ser "Emisor:usuario" o solo "usuario"
            val label = uri.path?.removePrefix("/") ?: return null
            val labelParts = label.split(":")
            val name: String
            val issuerFromLabel: String

            if (labelParts.size >= 2) {
                issuerFromLabel = labelParts[0].trim()
                name = labelParts[1].trim()
            } else {
                issuerFromLabel = ""
                name = labelParts[0].trim()
            }

            // Obtener parámetros
            val secretKey = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer") ?: issuerFromLabel
            val algorithmParam = uri.getQueryParameter("algorithm") ?: "SHA1"
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

            // Convertir el nombre del algoritmo al formato que usa javax.crypto
            val algorithm = when (algorithmParam.uppercase()) {
                "SHA1" -> "HmacSHA1"
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }


            return OtpAuthData(
                name = name,
                issuer = issuer,
                secretKey = secretKey.uppercase().replace(" ", ""),
                algorithm = algorithm,
                digits = digits,
                period = period
            )
        } catch (e: Exception) {
            return null
        }
    }
}