package com.example.totp.totp

/**
 * Decodificador Base32 según RFC 4648.
 * Convierte una cadena Base32 (A-Z, 2-7) en un array de bytes.
 * Necesario porque las SecretKey TOTP se distribuyen en Base32.
 */
object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        // Limpiar: mayúsculas, quitar espacios y padding '='
        val cleanInput = input.uppercase().replace(" ", "").replace("=", "")

        val output = ByteArray(cleanInput.length * 5 / 8)
        var buffer = 0L
        var bitsLeft = 0
        var outputIndex = 0

        for (char in cleanInput) {
            val value = ALPHABET.indexOf(char)
            if (value == -1) {
                throw IllegalArgumentException(
                    "Carácter inválido en Base32: '$char'"
                )
            }

            // Acumular 5 bits por cada carácter Base32
            buffer = (buffer shl 5) or value.toLong()
            bitsLeft += 5

            // Cuando tengamos 8 bits o más, extraer un byte
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[outputIndex] = (buffer shr bitsLeft).toByte()
                outputIndex++
                buffer = buffer and ((1L shl bitsLeft) - 1)
            }
        }

        return output.copyOf(outputIndex)
    }
}