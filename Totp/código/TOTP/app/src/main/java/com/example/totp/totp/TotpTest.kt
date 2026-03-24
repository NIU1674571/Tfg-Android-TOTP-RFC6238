package com.example.totp.totp

/**
 * Test de los vectores de prueba del RFC 6238 (Appendix B).
 * IMPORTANTE: El RFC usa 8 dígitos para los tests.
 *
 * SecretKeys en ASCII (según el RFC):
 * - SHA1:   "12345678901234567890" (20 bytes)
 * - SHA256: "12345678901234567890123456789012" (32 bytes)
 * - SHA512: "1234567890123456789012345678901234567890123456789012345678901234" (64 bytes)
 */
fun runTotpTests(): String {
    // SecretKeys en Base32 para cada algoritmo
    val keySha1 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    val keySha256 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA"
    val keySha512 =  "GEZDGNBVGY3TQOJQ" + 	   // bytes 1-10
                "GEZDGNBVGY3TQOJQ" +         // bytes 11-20
                "GEZDGNBVGY3TQOJQ" +         // bytes 21-30
                "GEZDGNBVGY3TQOJQ" +         // bytes 31-40
                "GEZDGNBVGY3TQOJQ" +         // bytes 41-50
                "GEZDGNBVGY3TQOJQ" +         // bytes 51-60
                "GEZDGNA"                      	   // bytes 61-64

    // Generadores con 8 dígitos (como el RFC)
    val sha1Gen = TotpGenerator(period = 30, digits = 8, algorithm = "HmacSHA1")
    val sha256Gen = TotpGenerator(period = 30, digits = 8, algorithm = "HmacSHA256")
    val sha512Gen = TotpGenerator(period = 30, digits = 8, algorithm = "HmacSHA512")

    data class TestCase(
        val time: Long,
        val expected: String,
        val algorithm: String,
        val generator: TotpGenerator,
        val key: String
    )

    // Los 18 vectores del RFC 6238 Appendix B
    val testCases = listOf(
        // SHA1
        TestCase(59L, "94287082", "SHA1", sha1Gen, keySha1),
        TestCase(1111111109L, "07081804", "SHA1", sha1Gen, keySha1),
        TestCase(1111111111L, "14050471", "SHA1", sha1Gen, keySha1),
        TestCase(1234567890L, "89005924", "SHA1", sha1Gen, keySha1),
        TestCase(2000000000L, "69279037", "SHA1", sha1Gen, keySha1),
        TestCase(20000000000L, "65353130", "SHA1", sha1Gen, keySha1),

        // SHA256
        TestCase(59L, "46119246", "SHA256", sha256Gen, keySha256),
        TestCase(1111111109L, "68084774", "SHA256", sha256Gen, keySha256),
        TestCase(1111111111L, "67062674", "SHA256", sha256Gen, keySha256),
        TestCase(1234567890L, "91819424", "SHA256", sha256Gen, keySha256),
        TestCase(2000000000L, "90698825", "SHA256", sha256Gen, keySha256),
        TestCase(20000000000L, "77737706", "SHA256", sha256Gen, keySha256),

        // SHA512
        TestCase(59L, "90693936", "SHA512", sha512Gen, keySha512),
        TestCase(1111111109L, "25091201", "SHA512", sha512Gen, keySha512),
        TestCase(1111111111L, "99943326", "SHA512", sha512Gen, keySha512),
        TestCase(1234567890L, "93441116", "SHA512", sha512Gen, keySha512),
        TestCase(2000000000L, "38618901", "SHA512", sha512Gen, keySha512),
        TestCase(20000000000L, "47863826", "SHA512", sha512Gen, keySha512)
    )

    val results = StringBuilder()
    var allPassed = true
    var currentAlgo = ""

    for (test in testCases) {
        if (test.algorithm != currentAlgo) {
            currentAlgo = test.algorithm
            results.appendLine("--- $currentAlgo ---")
        }

        val result = test.generator.generateCode(test.key, test.time)
        val passed = result == test.expected
        if (!passed) allPassed = false

        results.appendLine(
            "T=${test.time} → $result | Esperado: ${test.expected} | " +
                    if (passed) "✓" else "✗ FAIL"
        )
    }

    results.appendLine()
    results.appendLine(
        if (allPassed) "=== 18/18 TESTS PASARON ==="
        else "=== HAY TESTS FALLIDOS ==="
    )

    return results.toString()
}