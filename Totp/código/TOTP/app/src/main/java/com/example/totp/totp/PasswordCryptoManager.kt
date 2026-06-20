package com.example.totp.totp

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestor de cifrado para las contraseñas del Gestor de Contraseñas.
 *
 * A diferencia de SecureStorage (que usa EncryptedSharedPreferences para las
 * SecretKey TOTP), aquí se utiliza directamente el Android Keystore para
 * generar una clave AES-256 y cifrar/descifrar con AES-GCM.
 *
 * Flujo de cifrado:
 * 1. Se genera una clave AES-256 en el Android Keystore (solo la primera vez).
 * 2. Al cifrar, se genera un IV aleatorio de 12 bytes y se cifra con AES-GCM.
 * 3. Se concatena IV + ciphertext y se codifica en Base64.
 * 4. El resultado Base64 se almacena en el campo encryptedPassword de Room.
 *
 * Flujo de descifrado:
 * 1. Se decodifica el Base64.
 * 2. Se separan los primeros 12 bytes (IV) del resto (ciphertext).
 * 3. Se descifra con la clave del Keystore y se devuelve el texto plano.
 *
 * La clave de cifrado nunca sale del Android Keystore (almacén seguro
 * respaldado por hardware), por lo que las contraseñas cifradas en la
 * base de datos son irrecuperables sin acceso al Keystore del dispositivo.
 */
class PasswordCryptoManager {

    companion object {
        private const val KEY_ALIAS = "password_manager_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12  // bytes
        private const val GCM_TAG_LENGTH = 128 // bits
    }

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    init {
        // Generar la clave AES-256 en el Keystore si no existe
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    /**
     * Cifra una contraseña en texto plano.
     * @param plainText Contraseña en texto plano.
     * @return Cadena Base64 que contiene IV + ciphertext.
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv = cipher.iv // IV generado automáticamente (12 bytes para GCM)
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Concatenar IV + ciphertext para almacenarlo junto
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Descifra una contraseña cifrada.
     * @param encryptedBase64 Cadena Base64 que contiene IV + ciphertext.
     * @return Contraseña en texto plano.
     */
    fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        // Separar IV (primeros 12 bytes) y ciphertext (el resto)
        val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = combined.sliceArray(GCM_IV_LENGTH until combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Obtiene la clave AES del Android Keystore.
     */
    private fun getKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }
}