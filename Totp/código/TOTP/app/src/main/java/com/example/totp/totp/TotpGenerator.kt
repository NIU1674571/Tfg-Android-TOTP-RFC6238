package com.example.totp.totp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Generador TOTP según RFC 6238.
 *
 * Parámetros:
 * - period: intervalo en segundos (por defecto 30)
 * - digits: longitud del código (por defecto 6)
 * - algorithm: algoritmo HMAC (por defecto HmacSHA1)
 */
class TotpGenerator(
    private val period: Int = 30,
    private val digits: Int = 6,
    private val algorithm: String = "HmacSHA1"
) {

    /**
     * Genera el código TOTP para una SecretKey en Base32
     * en el instante actual.
     */
    fun generateCode(secretKeyBase32: String): String {
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        return generateCode(secretKeyBase32, currentTimeSeconds)
    }

    /**
     * Genera el código TOTP para un momento específico.
     * Útil para testing con los vectores de prueba de la RFC.
     */
    fun generateCode(secretKeyBase32: String, timeInSeconds: Long): String {
        // Paso A: Calcular el contador de tiempo
        val timeCounter = timeInSeconds / period

        // Decodificar la SecretKey de Base32 a bytes
        val keyBytes = Base32.decode(secretKeyBase32)

        // Paso B: Calcular HMAC
        val hmac = calculateHmac(keyBytes, timeCounter)

        // Paso C: Truncamiento dinámico
        val code = dynamicTruncation(hmac)

        // Formatear a N dígitos con ceros a la izquierda
        return code.toString().padStart(digits, '0')
    }

    /**
     * Calcula los segundos restantes del periodo actual.
     * Útil para mostrar la cuenta atrás en la UI.
     */
    fun secondsRemaining(): Int {
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        return (period - (currentTimeSeconds % period)).toInt()
    }

    /**
     * Paso B del RFC 6238: HMAC
     * Entrada: clave secreta (bytes) + contador de tiempo (Long → 8 bytes big-endian)
     * Salida: hash de N bytes según el algoritmo
     */
    private fun calculateHmac(keyBytes: ByteArray, counter: Long): ByteArray {
        // Convertir el contador a 8 bytes big-endian
        val counterBytes = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        // Calcular HMAC
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        mac.init(keySpec)
        return mac.doFinal(counterBytes)
    }

    /**
     * Paso C del RFC 6238: Truncamiento dinámico
     * Toma los últimos 4 bits del hash como offset,
     * extrae 4 bytes desde ese offset,
     * y hace módulo 10^digits para obtener el código.
     */
    private fun dynamicTruncation(hmac: ByteArray): Int {
        // El offset viene de los 4 bits menos significativos del último byte
        val offset = (hmac[hmac.size - 1].toInt() and 0x0F)

        // Extraer 4 bytes desde el offset (31 bits, ignorando el bit de signo)
        val binary = ((hmac[offset].toInt() and 0x7F) shl 24) or
                ((hmac[offset + 1].toInt() and 0xFF) shl 16) or
                ((hmac[offset + 2].toInt() and 0xFF) shl 8) or
                (hmac[offset + 3].toInt() and 0xFF)

        // Módulo 10^digits para obtener N dígitos
        val mod = Math.pow(10.0, digits.toDouble()).toInt()
        return binary % mod
    }
}