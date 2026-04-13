package com.example.totp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Convierte una cadena hexadecimal a un objeto Color de Compose.
 */
fun hexToColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (e: Exception) {
        Color.LightGray
    }
}

/**
 * Convierte un objeto Color de Compose a su representación hexadecimal.
 */
fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return "%02X%02X%02X".format(red, green, blue)
}
