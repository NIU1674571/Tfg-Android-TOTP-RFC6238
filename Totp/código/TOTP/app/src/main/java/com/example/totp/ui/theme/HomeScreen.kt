package com.example.totp.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.totp.totp.ThemeStorage

/**
 * Pantalla principal de selección de módulo.
 * Tras el login, el usuario puede elegir entre:
 * - TOTP Authenticator
 * - Gestor de Contraseñas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTotp: () -> Unit,
    onNavigateToPasswordManager: () -> Unit
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

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selecciona un módulo",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tarjeta TOTP
            ModuleCard(
                title = "Autenticador TOTP",
                description = "Genera códigos de verificación en dos pasos (2FA) para tus cuentas.",
                icon = Icons.Default.Key,
                onClick = onNavigateToTotp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta Gestor de Contraseñas
            ModuleCard(
                title = "Gestor de Contraseñas",
                description = "Almacena y gestiona tus credenciales de forma segura y cifrada.",
                icon = Icons.Default.Lock,
                onClick = onNavigateToPasswordManager
            )
        }
    }
}

/**
 * Tarjeta reutilizable para cada módulo de la pantalla Home.
 */
@Composable
private fun ModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val themeStorage = remember { ThemeStorage(context) }

    val defaultCardColor = MaterialTheme.colorScheme.surfaceVariant
    val cardColor = remember(themeStorage.getCardColor(), defaultCardColor) {
        val hex = themeStorage.getCardColor()
        if (hex == "DEFAULT") defaultCardColor else hexToColor(hex)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}