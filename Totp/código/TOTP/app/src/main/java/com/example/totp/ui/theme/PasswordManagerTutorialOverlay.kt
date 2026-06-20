package com.example.totp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Tutorial overlay interactivo para el Gestor de Contraseñas.
 * Sigue la misma filosofía que TutorialOverlay del módulo TOTP:
 * combina mensajes informativos con interacciones obligatorias del
 * usuario para que aprenda haciendo.
 *
 * Pasos:
 * 0 - Bienvenida (CENTER, botón "Empezar")
 * 1 - Pulsar + (BOTTOM, espera al usuario)
 * 2 - Sobre el diálogo, explicar campos rellenados (OVER_DIALOG, botón "OK")
 * 3 - Oculto: usuario pulsa "Añadir"
 * 4 - Pulsar la tarjeta (BOTTOM, espera al usuario)
 * 5 - Oculto: usuario cierra el editor
 * 6 - Final (CENTER, botón "Finalizar")
 */
@Composable
fun PasswordManagerTutorialOverlay(
    currentStep: Int,
    onNextStep: () -> Unit,
    onSkip: () -> Unit
) {
    val steps = listOf(
        // Paso 0: Bienvenida
        TutorialInfo(
            message = "¡Bienvenido al Gestor de Contraseñas!\n\n" +
                    "Te guiaremos paso a paso para crear tu primera credencial.",
            buttonText = "Empezar",
            position = MessagePosition.CENTER
        ),
        // Paso 1: Pulsar +
        TutorialInfo(
            message = "Pulsa el botón + de abajo a la derecha para añadir tu primera credencial.",
            buttonText = null,
            position = MessagePosition.BOTTOM
        ),
        // Paso 2: Explicar campos rellenados (encima del diálogo)
        TutorialInfo(
            message = "Hemos rellenado los campos automáticamente:\n\n" +
                    "Servicio: Google\n" +
                    "Usuario: ejemplo@gmail.com\n" +
                    "Contraseña: MiContraseña123\n" +
                    "URL: https://google.com\n\n" +
                    "Puedes modificarlos si quieres o usar el generador de contraseñas.\n" +
                    "Pulsa \"OK\" para ver los campos y después \"Añadir\" para guardar.",
            buttonText = "OK",
            position = MessagePosition.OVER_DIALOG
        ),
        // Paso 3: Oculto, el usuario pulsa "Añadir"
        TutorialInfo(
            message = "",
            buttonText = null,
            position = MessagePosition.HIDDEN
        ),
        // Paso 4: Credencial creada, pulsar tarjeta para editar
        TutorialInfo(
            message = "¡Credencial creada!\n\n" +
                    "Pulsa sobre la tarjeta para editar sus datos. Desde ahí podrás " +
                    "modificar el servicio, el usuario, la contraseña o la URL.\n\n" +
                    "Cuando entres, pulsa \"Cancelar\" para volver.",
            buttonText = null,
            position = MessagePosition.BOTTOM
        ),
        // Paso 5: Oculto, el usuario ve el editor y pulsa "Cancelar"
        TutorialInfo(
            message = "",
            buttonText = null,
            position = MessagePosition.HIDDEN
        ),
        // Paso 6: Final
        TutorialInfo(
            message = "¡Ya estás listo!\n\n" +
                    "Cada tarjeta tiene tres botones:\n" +
                    "👁 mostrar u ocultar la contraseña\n" +
                    "📋 copiar al portapapeles\n" +
                    "🗑 eliminar la credencial\n\n" +
                    "🔍 Usa la búsqueda para filtrar por servicio o usuario.\n\n" +
                    "⚙ Desde el engranaje de arriba puedes personalizar los colores " +
                    "y volver a ver este tutorial.",
            buttonText = "Finalizar",
            position = MessagePosition.CENTER
        )
    )

    if (currentStep >= steps.size) return

    val step = steps[currentStep]

    // No mostrar nada en pasos ocultos
    if (step.position == MessagePosition.HIDDEN) return

    when (step.position) {
        MessagePosition.OVER_DIALOG -> {
            // Usa Dialog para aparecer ENCIMA de los AlertDialog
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    TutorialMessage(step, currentStep, steps.size, onNextStep, onSkip)
                }
            }
        }

        else -> {
            // Overlay normal
            Box(modifier = Modifier.fillMaxSize()) {
                // Fondo semitransparente solo en pasos con botón
                if (step.buttonText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { }
                    )
                }

                when (step.position) {
                    MessagePosition.TOP -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                        ) {
                            TutorialMessage(step, currentStep, steps.size, onNextStep, onSkip)
                        }
                    }

                    MessagePosition.CENTER -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TutorialMessage(step, currentStep, steps.size, onNextStep, onSkip)
                        }
                    }

                    MessagePosition.BOTTOM -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                        ) {
                            TutorialMessage(step, currentStep, steps.size, onNextStep, onSkip)
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}