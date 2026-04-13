package com.example.totp.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    accountId: Int,
    onCropped: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Cargar la imagen
    val bitmap = remember {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bmp = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap == null) {
        onBack()
        return
    }

    val imageBitmap = remember { bitmap.asImageBitmap() }

    // Estado de zoom y desplazamiento
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Tamaño del canvas
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recortar imagen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Guardar la imagen recortada
                        val savedPath = cropAndSave(
                            context = context,
                            originalBitmap = bitmap,
                            canvasWidth = canvasSize.width,
                            canvasHeight = canvasSize.height,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            accountId = accountId
                        )
                        onCropped(savedPath)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Aceptar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Instrucciones
            Text(
                text = "Pellizca para zoom · Arrastra para mover",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )

            // Canvas con la imagen y el recuadro de recorte
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height

                    // Calcular tamaño de la imagen escalada
                    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val baseWidth: Float
                    val baseHeight: Float

                    if (imageAspect > 1) {
                        baseWidth = canvasW
                        baseHeight = canvasW / imageAspect
                    } else {
                        baseHeight = canvasH
                        baseWidth = canvasH * imageAspect
                    }

                    val scaledWidth = baseWidth * scale
                    val scaledHeight = baseHeight * scale

                    val imgLeft = (canvasW - scaledWidth) / 2 + offsetX
                    val imgTop = (canvasH - scaledHeight) / 2 + offsetY

                    // Dibujar la imagen
                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(
                            imgLeft.toInt(),
                            imgTop.toInt()
                        ),
                        dstSize = IntSize(
                            scaledWidth.toInt(),
                            scaledHeight.toInt()
                        )
                    )

                    // Dibujar el recuadro de recorte cuadrado en el centro
                    val cropSize = minOf(canvasW, canvasH) * 0.7f
                    val cropLeft = (canvasW - cropSize) / 2
                    val cropTop = (canvasH - cropSize) / 2

                    // Oscurecer las zonas fuera del recorte
                    val overlayColor = Color.Black.copy(alpha = 0.5f)

                    // Arriba
                    drawRect(overlayColor, Offset.Zero, Size(canvasW, cropTop))
                    // Abajo
                    drawRect(overlayColor, Offset(0f, cropTop + cropSize), Size(canvasW, canvasH - cropTop - cropSize))
                    // Izquierda
                    drawRect(overlayColor, Offset(0f, cropTop), Size(cropLeft, cropSize))
                    // Derecha
                    drawRect(overlayColor, Offset(cropLeft + cropSize, cropTop), Size(canvasW - cropLeft - cropSize, cropSize))

                    // Borde del recorte
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cropLeft, cropTop),
                        size = Size(cropSize, cropSize),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun cropAndSave(
    context: android.content.Context,
    originalBitmap: Bitmap,
    canvasWidth: Int,
    canvasHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    accountId: Int
): String? {
    return try {
        val canvasW = canvasWidth.toFloat()
        val canvasH = canvasHeight.toFloat()

        val imageAspect = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val baseWidth: Float
        val baseHeight: Float

        if (imageAspect > 1) {
            baseWidth = canvasW
            baseHeight = canvasW / imageAspect
        } else {
            baseHeight = canvasH
            baseWidth = canvasH * imageAspect
        }

        val scaledWidth = baseWidth * scale
        val scaledHeight = baseHeight * scale

        val imgLeft = (canvasW - scaledWidth) / 2 + offsetX
        val imgTop = (canvasH - scaledHeight) / 2 + offsetY

        // Recuadro de recorte
        val cropSize = minOf(canvasW, canvasH) * 0.7f
        val cropLeft = (canvasW - cropSize) / 2
        val cropTop = (canvasH - cropSize) / 2

        // Convertir coordenadas del recuadro a coordenadas de la imagen original
        val srcLeft = ((cropLeft - imgLeft) / scaledWidth * originalBitmap.width).toInt()
            .coerceIn(0, originalBitmap.width - 1)
        val srcTop = ((cropTop - imgTop) / scaledHeight * originalBitmap.height).toInt()
            .coerceIn(0, originalBitmap.height - 1)
        val srcSize = (cropSize / scaledWidth * originalBitmap.width).toInt()
            .coerceIn(1, minOf(originalBitmap.width - srcLeft, originalBitmap.height - srcTop))

        val croppedBitmap = Bitmap.createBitmap(originalBitmap, srcLeft, srcTop, srcSize, srcSize)
        val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, 128, 128, true)

        // Borrar iconos anteriores
        context.filesDir.listFiles()?.forEach {
            if (it.name.startsWith("icon_${accountId}_")) {
                it.delete()
            }
        }

        val file = File(context.filesDir, "icon_${accountId}_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(file)
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
        outputStream.flush()
        outputStream.close()

        if (croppedBitmap != finalBitmap) croppedBitmap.recycle()
        finalBitmap.recycle()

        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
