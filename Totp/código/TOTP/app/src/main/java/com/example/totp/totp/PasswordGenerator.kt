package com.example.totp.totp

import java.security.SecureRandom

/**
 * Generador de contraseñas configurable.
 * Usa SecureRandom para garantizar aleatoriedad criptográfica.
 *
 * Parámetros configurables (OF-S3):
 * - length: longitud de la contraseña (4-128)
 * - includeUppercase: incluir letras mayúsculas (A-Z)
 * - includeLowercase: incluir letras minúsculas (a-z)
 * - includeNumbers: incluir números (0-9)
 * - includeSpecial: incluir caracteres especiales (!@#$%...)
 */
class PasswordGenerator {

    companion object {
        private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val NUMBERS = "0123456789"
        private const val SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        private val secureRandom = SecureRandom()
    }

    /**
     * Genera una contraseña aleatoria con los parámetros indicados.
     *
     * El algoritmo garantiza que la contraseña contiene al menos un carácter
     * de cada tipo seleccionado (si la longitud lo permite), y luego rellena
     * el resto con caracteres aleatorios del pool completo.
     * Finalmente, mezcla todos los caracteres para evitar patrones predecibles.
     *
     * @param length Longitud de la contraseña (mínimo 4, máximo 128).
     * @param includeUppercase Incluir mayúsculas.
     * @param includeLowercase Incluir minúsculas.
     * @param includeNumbers Incluir números.
     * @param includeSpecial Incluir caracteres especiales.
     * @return Contraseña generada.
     * @throws IllegalArgumentException si no se selecciona ningún tipo de carácter.
     */
    fun generate(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSpecial: Boolean = true
    ): String {
        // Construir el pool de caracteres disponibles
        val pool = StringBuilder()
        if (includeLowercase) pool.append(LOWERCASE)
        if (includeUppercase) pool.append(UPPERCASE)
        if (includeNumbers) pool.append(NUMBERS)
        if (includeSpecial) pool.append(SPECIAL)

        require(pool.isNotEmpty()) { "Debe seleccionar al menos un tipo de carácter" }

        val clampedLength = length.coerceIn(4, 128)
        val result = mutableListOf<Char>()

        // Garantizar al menos un carácter de cada tipo seleccionado
        if (includeLowercase) result.add(randomChar(LOWERCASE))
        if (includeUppercase) result.add(randomChar(UPPERCASE))
        if (includeNumbers) result.add(randomChar(NUMBERS))
        if (includeSpecial) result.add(randomChar(SPECIAL))

        // Rellenar el resto con caracteres aleatorios del pool completo
        val poolStr = pool.toString()
        while (result.size < clampedLength) {
            result.add(randomChar(poolStr))
        }

        // Mezclar para evitar que los primeros caracteres siempre sean
        // del mismo tipo (Fisher-Yates shuffle con SecureRandom)
        for (i in result.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return result.joinToString("")
    }

    /**
     * Selecciona un carácter aleatorio de una cadena usando SecureRandom.
     */
    private fun randomChar(source: String): Char {
        return source[secureRandom.nextInt(source.length)]
    }
}
