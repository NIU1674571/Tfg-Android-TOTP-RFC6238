package com.example.totp.totp

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Almacenamiento seguro para las credenciales de login.
 * Usa EncryptedSharedPreferences para cifrar el usuario y la contraseña.
 * La contraseña se guarda hasheada con SHA-256 (nunca en texto plano).
 */
class AuthStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val authPrefs = EncryptedSharedPreferences.create(
        context,
        "totp_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Comprueba si ya hay un usuario registrado.
     */
    fun isUserRegistered(): Boolean {
        return authPrefs.contains("username")
    }

    /**
     * Registra un nuevo usuario con contraseña hasheada.
     */
    fun registerUser(username: String, password: String) {
        authPrefs.edit()
            .putString("username", username)
            .putString("password_hash", hashPassword(password))
            .apply()
    }

    /**
     * Verifica las credenciales de login.
     */
    fun verifyLogin(username: String, password: String): Boolean {
        val storedUsername = authPrefs.getString("username", null)
        val storedHash = authPrefs.getString("password_hash", null)

        return storedUsername == username && storedHash == hashPassword(password)
    }

    /**
     * Obtiene el nombre de usuario registrado.
     */
    fun getUsername(): String {
        return authPrefs.getString("username", "") ?: ""
    }

    /**
     * Comprueba si la biometría está activada.
     */
    fun isBiometricEnabled(): Boolean {
        return authPrefs.getBoolean("biometric_enabled", false)
    }

    /**
     * Activa o desactiva la biometría.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        authPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    /**
     * Hashea la contraseña con SHA-256.
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}