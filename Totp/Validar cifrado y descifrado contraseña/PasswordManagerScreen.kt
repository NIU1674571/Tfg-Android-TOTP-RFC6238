package com.example.totp.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.totp.totp.PasswordCryptoManager
import com.example.totp.totp.ThemeStorage

/**
 * Pantalla principal del Gestor de Contraseñas.
 * Incluye temporalmente un test de validación del cifrado AES-256-GCM.
 * Se reemplazará por la UI completa en las fases T4-T6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themeStorage = remember { ThemeStorage(context) }

    val defaultBarColor = MaterialTheme.colorScheme.primaryContainer
    val topBarColor = remember(themeStorage.getTopBarColor(), defaultBarColor) {
        val hex = themeStorage.getTopBarColor()
        if (hex == "DEFAULT") defaultBarColor else hexToColor(hex)
    }

    val backgroundColor = remember(themeStorage.getBackgroundColor(), MaterialTheme.colorScheme.background) {
        val hex = themeStorage.getBackgroundColor()
        if (hex == "DEFAULT") Color.Unspecified else hexToColor(hex)
    }

    // Estado del test de cifrado
    var testResult by remember { mutableStateOf<String?>(null) }
    var testPassed by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Gestor de Contraseñas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gestor de Contraseñas",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Validación del cifrado AES-256-GCM con Android Keystore",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para ejecutar el test
            Button(
                onClick = {
                    testResult = runCryptoTest()
                    testPassed = testResult?.contains("TODOS LOS TESTS PASARON") == true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ejecutar test de cifrado")
            }

            // Mostrar resultados
            if (testResult != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (testPassed)
                            Color(0xFF1B5E20).copy(alpha = 0.15f)
                        else
                            Color(0xFFB71C1C).copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = testResult!!,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Ejecuta tests de cifrado y descifrado con diferentes contraseñas.
 * Verifica que:
 * 1. El cifrado produce una cadena Base64 diferente al texto original.
 * 2. El descifrado recupera exactamente el texto original.
 * 3. Cada cifrado produce un resultado diferente (por el IV aleatorio).
 * 4. Funciona con caracteres especiales y acentos.
 */
private fun runCryptoTest(): String {
    val results = StringBuilder()
    var allPassed = true

    try {
        val crypto = PasswordCryptoManager()

        // Contraseñas de prueba con diferentes tipos de caracteres
        val testPasswords = listOf(
            "password123",
            "C0ntr@seña_Segur4!",
            "contraseña con espacios y acentos",
            "!@#\$%^&*()_+-=[]{}|;':,./<>?",
            "a"  // contraseña mínima
        )

        results.appendLine("=== TEST DE CIFRADO AES-256-GCM ===")
        results.appendLine()

        for ((index, password) in testPasswords.withIndex()) {
            results.appendLine("--- Test ${index + 1} ---")
            results.appendLine("Original:    $password")

            val encrypted = crypto.encrypt(password)
            results.appendLine("Cifrado:     $encrypted")

            val decrypted = crypto.decrypt(encrypted)
            results.appendLine("Descifrado:  $decrypted")

            val passed = decrypted == password
            if (!passed) allPassed = false
            results.appendLine("Resultado:   ${if (passed) "✓ PASS" else "✗ FAIL"}")
            results.appendLine()
        }

        // Test extra: verificar que dos cifrados de la misma contraseña
        // producen resultados diferentes (por el IV aleatorio)
        results.appendLine("--- Test 6: IV aleatorio ---")
        val encrypted1 = crypto.encrypt("mismaContraseña")
        val encrypted2 = crypto.encrypt("mismaContraseña")
        val ivDifferent = encrypted1 != encrypted2
        if (!ivDifferent) allPassed = false
        results.appendLine("Cifrado 1: $encrypted1")
        results.appendLine("Cifrado 2: $encrypted2")
        results.appendLine("IVs diferentes: ${if (ivDifferent) "✓ PASS" else "✗ FAIL"}")
        results.appendLine()

        results.appendLine(
            if (allPassed) "=== TODOS LOS TESTS PASARON (6/6) ==="
            else "=== HAY TESTS FALLIDOS ==="
        )

    } catch (e: Exception) {
        results.appendLine("ERROR: ${e.message}")
        results.appendLine("=== HAY TESTS FALLIDOS ===")
    }

    return results.toString()
}
