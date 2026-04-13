package com.example.totp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TutorialOverlay(
    currentStep: Int,
    onNextStep: () -> Unit,
    onSkip: () -> Unit
) {
    val steps = listOf(
        // Paso 0: Bienvenida
        TutorialInfo(
            message = "¡Bienvenido a TOTP Authenticator!\n\nTe guiaremos paso a paso para crear tu primera cuenta.",
            buttonText = "Empezar",
            position = MessagePosition.CENTER
        ),
        // Paso 1: Pulsar +
        TutorialInfo(
            message = "Pulsa el botón + de abajo a la derecha para añadir tu primera cuenta.",
            buttonText = null, // Sin botón, el usuario debe pulsar +
            position = MessagePosition.CENTER_TOP
        ),
        // Paso 2: Pulsar \"Introducir manualmente\"
        TutorialInfo(
            message = "Selecciona \"Introducir manualmente\" en el menú.",
            buttonText = null,
            position = MessagePosition.CENTER_TOP
        ),
        // Paso 3: Explicar campos rellenados (encima del diálogo, con botón OK)
        TutorialInfo(
            message = "Hemos rellenado los campos automáticamente:\n\n" +
                    "Servicio: Google\n" +
                    "Cuenta: ejemplo@gmail.com\n" +
                    "SecretKey: JBSWY3DPEHPK3PXP\n\n" +
                    "Algoritmo: SHA1\n" +
                    "Dígitos: 6\n" +
                    "Periodo: 30 segundos\n\n" +
                    "Puedes modificarlos si quieres.\nPulsa \"OK\" para ver los campos.",
            buttonText = "OK",
            position = MessagePosition.OVER_DIALOG
        ),
        // Paso 4: Sin mensaje, el usuario ve los campos y pulsa \"Añadir\"
        TutorialInfo(
            message = "",
            buttonText = null,
            position = MessagePosition.HIDDEN
        ),
        // Paso 5: Cuenta creada, pulsar tarjeta
        TutorialInfo(
            message = "¡Cuenta creada! Verás el código TOTP generándose.\n\n" +
                    "Pulsa sobre la tarjeta para editar sus datos. " +
                    "Desde ahí podrás modificar el nombre, servicio, SecretKey, algoritmo, dígitos, periodo e icono.\n\n" +
                    "Cuando entres, pulsa \"Cancelar\" para volver.",
            buttonText = null,
            position = MessagePosition.BOTTOM
        ),
        // Paso 6: Sin mensaje, el usuario ve el editor y pulsa \"Cancelar\"
        TutorialInfo(
            message = "",
            buttonText = null,
            position = MessagePosition.HIDDEN
        ),
        // Paso 7: Final
        TutorialInfo(
            message = "¡Ya estás listo!\n\n" +
                    "📋 Pulsa el icono de copiar junto al código para copiarlo al portapapeles.\n\n" +
                    "⚙ Desde el engranaje de arriba puedes activar la biometría, " +
                    "personalizar los colores y volver a ver este tutorial.",
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

                    MessagePosition.CENTER_TOP -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 140.dp, start = 16.dp, end = 16.dp)
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

@Composable
fun TutorialMessage(
    step: TutorialInfo,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.widthIn(max = 340.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicador de progreso
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 8.dp else 5.dp)
                            .background(
                                when {
                                    index == currentStep -> Color.White
                                    index < currentStep -> Color(0xFF4CAF50)
                                    else -> Color.Gray
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = step.message,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Saltar tutorial", color = Color.Gray, fontSize = 12.sp)
                }

                if (step.buttonText != null) {
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(step.buttonText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class TutorialInfo(
    val message: String,
    val buttonText: String?,
    val position: MessagePosition
)

enum class MessagePosition {
    TOP,
    CENTER_TOP,
    CENTER,
    BOTTOM,
    OVER_DIALOG,
    HIDDEN
}
