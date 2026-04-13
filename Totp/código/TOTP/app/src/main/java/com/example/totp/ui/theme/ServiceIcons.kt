package com.example.totp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Iconos y colores por defecto para servicios comunes.
 * Se usa la primera letra del servicio con un color específico.
 * Si el servicio no está en la lista, se genera un color a partir del nombre.
 */
object ServiceIcons {

    data class ServiceStyle(
        val letter: String,
        val backgroundColor: Color,
        val textColor: Color = Color.White
    )

    private val knownServices = mapOf(
        "google" to ServiceStyle("G", Color(0xFF4285F4)),
        "gmail" to ServiceStyle("G", Color(0xFFEA4335)),
        "github" to ServiceStyle("GH", Color(0xFF24292E)),
        "discord" to ServiceStyle("D", Color(0xFF5865F2)),
        "amazon" to ServiceStyle("A", Color(0xFFFF9900), Color.Black),
        "microsoft" to ServiceStyle("M", Color(0xFF00A4EF)),
        "apple" to ServiceStyle("A", Color(0xFF555555)),
        "facebook" to ServiceStyle("F", Color(0xFF1877F2)),
        "instagram" to ServiceStyle("I", Color(0xFFE4405F)),
        "twitter" to ServiceStyle("X", Color(0xFF000000)),
        "x" to ServiceStyle("X", Color(0xFF000000)),
        "twitch" to ServiceStyle("T", Color(0xFF9146FF)),
        "steam" to ServiceStyle("S", Color(0xFF1B2838)),
        "spotify" to ServiceStyle("S", Color(0xFF1DB954)),
        "dropbox" to ServiceStyle("D", Color(0xFF0061FF)),
        "reddit" to ServiceStyle("R", Color(0xFFFF4500)),
        "linkedin" to ServiceStyle("in", Color(0xFF0A66C2)),
        "paypal" to ServiceStyle("P", Color(0xFF003087)),
        "binance" to ServiceStyle("B", Color(0xFFF0B90B), Color.Black),
        "coinbase" to ServiceStyle("C", Color(0xFF0052FF)),
        "epic" to ServiceStyle("E", Color(0xFF2F2D2E)),
        "riot" to ServiceStyle("R", Color(0xFFD32936)),
        "adobe" to ServiceStyle("A", Color(0xFFFF0000)),
        "slack" to ServiceStyle("S", Color(0xFF4A154B)),
        "zoom" to ServiceStyle("Z", Color(0xFF2D8CFF)),
        "netflix" to ServiceStyle("N", Color(0xFFE50914)),
        "yahoo" to ServiceStyle("Y", Color(0xFF6001D2)),
        "proton" to ServiceStyle("P", Color(0xFF6D4AFF)),
        "bitwarden" to ServiceStyle("B", Color(0xFF175DDC)),
        "aws" to ServiceStyle("A", Color(0xFFFF9900), Color.Black),
        "cloudflare" to ServiceStyle("C", Color(0xFFF38020)),
        "digitalocean" to ServiceStyle("D", Color(0xFF0080FF)),
        "gitlab" to ServiceStyle("G", Color(0xFFFC6D26)),
        "wordpress" to ServiceStyle("W", Color(0xFF21759B)),
        "shopify" to ServiceStyle("S", Color(0xFF96BF48)),
        "figma" to ServiceStyle("F", Color(0xFFF24E1E)),
        "notion" to ServiceStyle("N", Color(0xFF000000)),
        "trello" to ServiceStyle("T", Color(0xFF0052CC)),
        "jira" to ServiceStyle("J", Color(0xFF0052CC)),
        "heroku" to ServiceStyle("H", Color(0xFF430098)),
        "stripe" to ServiceStyle("S", Color(0xFF635BFF)),
    )

    fun getServiceStyle(issuer: String): ServiceStyle {
        val key = issuer.lowercase().trim()

        // Buscar coincidencia exacta o parcial
        for ((serviceName, style) in knownServices) {
            if (key.contains(serviceName) || serviceName.contains(key)) {
                return style
            }
        }

        // Si no se encuentra, generar color a partir del nombre
        val hash = issuer.hashCode()
        val hue = (hash and 0xFF) * 360f / 256f
        val color = Color.hsl(hue, 0.6f, 0.4f)
        val letter = issuer.firstOrNull()?.uppercase() ?: "?"

        return ServiceStyle(letter, color)
    }
}
