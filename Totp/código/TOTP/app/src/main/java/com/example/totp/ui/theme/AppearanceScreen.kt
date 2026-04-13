package com.example.totp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.totp.totp.ThemeStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    onThemeChanged: () -> Unit
) {
    val context = LocalContext.current
    val themeStorage = remember { ThemeStorage(context) }

    var selectedTheme by remember { mutableStateOf(themeStorage.getThemeMode()) }
    var selectedTopBar by remember { mutableStateOf(themeStorage.getTopBarColor()) }
    var selectedCard by remember { mutableStateOf(themeStorage.getCardColor()) }
    var selectedBackground by remember { mutableStateOf(themeStorage.getBackgroundColor()) }

    val defaultBarColor = MaterialTheme.colorScheme.primaryContainer
    val topBarColor = remember(selectedTopBar, defaultBarColor) {
        if (selectedTopBar == "DEFAULT") defaultBarColor else hexToColor(selectedTopBar)
    }

    val backgroundColor = remember(selectedBackground) {
        if (selectedBackground == "DEFAULT") Color.Unspecified else hexToColor(selectedBackground)
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Apariencia") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección: Tema
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tema",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOption(
                            label = "Claro",
                            selected = selectedTheme == "light",
                            onClick = {
                                selectedTheme = "light"
                                themeStorage.setThemeMode("light")
                                onThemeChanged()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOption(
                            label = "Oscuro",
                            selected = selectedTheme == "dark",
                            onClick = {
                                selectedTheme = "dark"
                                themeStorage.setThemeMode("dark")
                                onThemeChanged()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOption(
                            label = "Sistema",
                            selected = selectedTheme == "system",
                            onClick = {
                                selectedTheme = "system"
                                themeStorage.setThemeMode("system")
                                onThemeChanged()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sección: Color de la barra superior
            ColorPickerSection(
                title = "Color de la barra superior",
                currentColor = selectedTopBar,
                onColorSelected = { color ->
                    selectedTopBar = color
                    themeStorage.setTopBarColor(color)
                    onThemeChanged()
                }
            )

            // Sección: Color de fondo
            ColorPickerSection(
                title = "Color de fondo",
                currentColor = selectedBackground,
                onColorSelected = { color ->
                    selectedBackground = color
                    themeStorage.setBackgroundColor(color)
                    onThemeChanged()
                }
            )

            // Sección: Color de las tarjetas
            ColorPickerSection(
                title = "Color de las tarjetas",
                currentColor = selectedCard,
                onColorSelected = { color ->
                    selectedCard = color
                    themeStorage.setCardColor(color)
                    onThemeChanged()
                }
            )

            // Botón restaurar
            OutlinedButton(
                onClick = {
                    themeStorage.resetToDefaults()
                    selectedTheme = "light"
                    selectedTopBar = "DEFAULT"
                    selectedCard = "DEFAULT"
                    selectedBackground = "DEFAULT"
                    onThemeChanged()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restaurar colores por defecto")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.clickable { onClick() },
        border = if (selected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ) else CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ColorPickerSection(
    title: String,
    currentColor: String,
    onColorSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                // Muestra el color actual
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentColor == "DEFAULT") MaterialTheme.colorScheme.primaryContainer
                            else hexToColor(currentColor)
                        )
                        .border(1.dp, Color.Gray, CircleShape)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Botón por defecto
                OutlinedButton(
                    onClick = {
                        onColorSelected("DEFAULT")
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar color por defecto")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paleta de colores HSL
                ColorPalette(
                    currentColor = currentColor,
                    onColorSelected = { hex ->
                        onColorSelected(hex)
                    }
                )
            }
        }
    }
}

@Composable
fun ColorPalette(
    currentColor: String,
    onColorSelected: (String) -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0.5f) }
    var lightness by remember { mutableFloatStateOf(0.5f) }

    val previewColor = Color.hsl(hue, saturation, lightness)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewColor)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = colorToHex(previewColor),
                color = if (lightness > 0.5f) Color.Black else Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(text = "Tono", style = MaterialTheme.typography.bodySmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = (0..360 step 30).map { Color.hsl(it.toFloat(), 0.8f, 0.5f) }
                    )
                )
        )
        Slider(
            value = hue,
            onValueChange = { hue = it },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Saturación", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = saturation,
            onValueChange = { saturation = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Luminosidad", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = lightness,
            onValueChange = { lightness = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Colores rápidos", style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickColors = listOf(
                "FFFFFF", "000000", "2E75B6", "E53935",
                "43A047", "FB8C00", "8E24AA", "00ACC1"
            )
            quickColors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(hexToColor(hex))
                        .border(
                            width = if (currentColor == hex) 3.dp else 1.dp,
                            color = if (currentColor == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                onColorSelected(colorToHex(previewColor))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aplicar este color")
        }
    }
}
