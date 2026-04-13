package com.example.totp.totp

import android.content.Context
import android.content.SharedPreferences

/**
 * Almacenamiento de las preferencias de tema/colores de la app.
 * Se usa SharedPreferences normal (no cifrado) porque no es información sensible.
 */
class ThemeStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "totp_theme_prefs", Context.MODE_PRIVATE
    )

    // Tema: "light", "dark", "system"
    fun getThemeMode(): String = prefs.getString("theme_mode", "light") ?: "light"
    fun setThemeMode(mode: String) = prefs.edit().putString("theme_mode", mode).apply()

    // Color de la barra superior (hex sin #)
    fun getTopBarColor(): String = prefs.getString("top_bar_color", "DEFAULT") ?: "DEFAULT"
    fun setTopBarColor(color: String) = prefs.edit().putString("top_bar_color", color).apply()

    // Color de las tarjetas de cuentas (hex sin #)
    fun getCardColor(): String = prefs.getString("card_color", "DEFAULT") ?: "DEFAULT"
    fun setCardColor(color: String) = prefs.edit().putString("card_color", color).apply()

    // Color de fondo (hex sin #)
    fun getBackgroundColor(): String = prefs.getString("background_color", "DEFAULT") ?: "DEFAULT"
    fun setBackgroundColor(color: String) = prefs.edit().putString("background_color", color).apply()

    // Restaurar colores por defecto
    fun resetToDefaults() {
        prefs.edit()
            .putString("theme_mode", "light")
            .putString("top_bar_color", "DEFAULT")
            .putString("card_color", "DEFAULT")
            .putString("background_color", "DEFAULT")
            .apply()
    }
}