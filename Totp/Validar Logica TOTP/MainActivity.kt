package com.example.totp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.totp.totp.TotpGenerator
import com.example.totp.totp.runTotpTests
import com.example.totp.ui.theme.TOTPTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TOTPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TotpTestScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpTestScreen() {
    val generator = remember { TotpGenerator() }

    // SecretKey de ejemplo (la del RFC para SHA1)
    val testKey = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    var currentCode by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var testResults by remember { mutableStateOf("") }

    // Ejecutar tests al iniciar
    LaunchedEffect(Unit) {
        testResults = runTotpTests()
    }

    // Temporizador que actualiza el código cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentCode = generator.generateCode(testKey)
            secondsLeft = generator.secondsRemaining()
            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TOTP - Tarjeta 2: Validación") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sección: Código TOTP en vivo
            Text(
                text = "Código TOTP en vivo (6 dígitos, SHA1)",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentCode.chunked(3).joinToString(" "),
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Expira en: ${secondsLeft}s")
                    LinearProgressIndicator(
                        progress = { secondsLeft / 30f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección: Resultados de los tests RFC 6238
            Text(
                text = "Tests RFC 6238 (Appendix B) - 8 dígitos",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = testResults,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
