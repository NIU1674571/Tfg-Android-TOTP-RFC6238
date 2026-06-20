package com.example.totp.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.totp.totp.AuthStorage
import com.example.totp.totp.ThemeStorage
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onTutorialClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authStorage = remember { AuthStorage(context) }
    val themeStorage = remember { ThemeStorage(context) }

    var biometricEnabled by remember { mutableStateOf(authStorage.isBiometricEnabled()) }

    val defaultBarColor = MaterialTheme.colorScheme.primaryContainer
    val topBarColor = remember(themeStorage.getTopBarColor(), defaultBarColor) {
        val hex = themeStorage.getTopBarColor()
        if (hex == "DEFAULT") defaultBarColor else hexToColor(hex)
    }

    val backgroundColor = remember(themeStorage.getBackgroundColor(), MaterialTheme.colorScheme.background) {
        val hex = themeStorage.getBackgroundColor()
        if (hex == "DEFAULT") Color.Unspecified else hexToColor(hex)
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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
                .padding(16.dp)
        ) {
            // Apariencia
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppearanceClick() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Apariencia",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tema, colores de la barra, fondo y tarjetas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tutorial
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTutorialClick() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Ver tutorial",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Volver a ver la guía de uso de la app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biometría
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Inicio con biometría",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Usar huella para iniciar sesión",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { enabled ->
                            biometricEnabled = enabled
                            authStorage.setBiometricEnabled(enabled)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cuenta
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Cuenta",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Usuario: ${authStorage.getUsername()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
