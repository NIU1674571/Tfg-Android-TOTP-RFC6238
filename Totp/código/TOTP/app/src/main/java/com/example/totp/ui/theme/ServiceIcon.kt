package com.example.totp.ui.theme

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Muestra el icono de un servicio.
 * Si hay una imagen personalizada (URI), la muestra.
 * Si no, muestra la letra del servicio con su color por defecto.
 */
@Composable
fun ServiceIcon(
    issuer: String,
    customImageUri: String? = null,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    if (customImageUri != null && customImageUri.isNotBlank()) {
        // Imagen personalizada del usuario
        AsyncImage(
            model = Uri.parse(customImageUri),
            contentDescription = "Icono de $issuer",
            modifier = modifier
                .size(size.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        // Icono por defecto basado en el nombre del servicio
        val style = ServiceIcons.getServiceStyle(issuer)

        Box(
            modifier = modifier
                .size(size.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(style.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = style.letter,
                color = style.textColor,
                fontSize = if (style.letter.length > 1) 16.sp else 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
