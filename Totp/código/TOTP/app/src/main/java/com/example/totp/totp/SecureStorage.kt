package com.example.totp.totp

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacenamiento seguro para las SecretKey TOTP.
 * Usa EncryptedSharedPreferences de Jetpack Security para cifrar
 * las claves en reposo. La clave maestra se genera y almacena
 * en el Android Keystore, que es un almacén seguro a nivel de hardware.
 *
 * Las SecretKey se guardan cifradas con AES-256-GCM y la clave
 * de cifrado nunca sale del Keystore.
 */
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "totp_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Guarda una SecretKey cifrada, asociada al id de la cuenta.
     */
    fun saveSecretKey(accountId: Int, secretKey: String) {
        securePrefs.edit().putString("secret_$accountId", secretKey).apply()
    }

    /**
     * Recupera una SecretKey descifrada por el id de la cuenta.
     */
    fun getSecretKey(accountId: Int): String? {
        return securePrefs.getString("secret_$accountId", null)
    }

    /**
     * Elimina la SecretKey cifrada de una cuenta.
     */
    fun deleteSecretKey(accountId: Int) {
        securePrefs.edit().remove("secret_$accountId").apply()
    }
}