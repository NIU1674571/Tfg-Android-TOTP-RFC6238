package com.example.totp.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TutorialPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        TutorialPage(
            icon = Icons.Default.Lock,
            title = "Bienvenido a Autenticador TOTP",
            description = "Tu app de autenticación de dos factores (2FA). " +
                    "Genera códigos temporales para proteger tus cuentas online."
        ),
        TutorialPage(
            icon = Icons.Default.Add,
            title = "Añadir cuentas",
            description = "Pulsa el botón + para añadir una cuenta. " +
                    "Puedes introducir los datos manualmente, escanear un código QR " +
                    "con la cámara o importar una imagen de QR desde la galería."
        ),
        TutorialPage(
            icon = Icons.Default.Edit,
            title = "Editar cuentas",
            description = "Pulsa sobre cualquier cuenta para editar sus datos: " +
                    "nombre, servicio, SecretKey, algoritmo (SHA1, SHA256, SHA512), " +
                    "número de dígitos y periodo en segundos. " +
                    "También puedes cambiar el icono de la cuenta."
        ),
        TutorialPage(
            icon = Icons.Default.Timer,
            title = "Códigos temporales",
            description = "Cada código tiene un temporizador que indica cuánto tiempo queda " +
                    "antes de que se genere uno nuevo. La barra de progreso se adapta " +
                    "al periodo configurado de cada cuenta (por defecto 30 segundos). " +
                    "Puedes modificar el periodo al crear o editar una cuenta."
        ),
        TutorialPage(
            icon = Icons.Default.ContentCopy,
            title = "Copiar código",
            description = "Pulsa el icono de copiar junto al código para copiarlo " +
                    "al portapapeles y pegarlo directamente en la web o app donde lo necesites."
        ),
        TutorialPage(
            icon = Icons.Default.Security,
            title = "Seguridad",
            description = "Tus claves secretas (SecretKey) se almacenan cifradas en el dispositivo " +
                    "con AES-256. Nadie puede acceder a ellas sin tu contraseña o tu huella. " +
                    "Puedes activar la biometría en Ajustes."
        ),
        TutorialPage(
            icon = Icons.Default.Palette,
            title = "Personalización",
            description = "En Ajustes > Apariencia puedes cambiar el tema (claro, oscuro o del sistema), " +
                    "el color de la barra superior, el fondo y las tarjetas de tarjetas."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Páginas del tutorial
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                TutorialPageContent(pages[page])
            }

            // Indicadores de página (puntos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón Saltar
                TextButton(
                    onClick = onFinish
                ) {
                    Text("Saltar")
                }

                // Botón Siguiente o Empezar
                if (pagerState.currentPage < pages.size - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("Siguiente")
                    }
                } else {
                    Button(
                        onClick = onFinish
                    ) {
                        Text("Empezar")
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}
