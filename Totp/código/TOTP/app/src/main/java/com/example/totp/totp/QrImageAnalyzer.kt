package com.example.totp.totp

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Analiza una imagen de la galería para buscar códigos QR.
 * Usa ML Kit Barcode Scanning para detectar y leer el QR.
 */
object QrImageAnalyzer {

    fun analyzeImageUri(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isEmpty()) {
                        onError("No se ha encontrado ningún código QR en la imagen")
                        return@addOnSuccessListener
                    }

                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.startsWith("otpauth://")) {
                            onSuccess(rawValue)
                            return@addOnSuccessListener
                        }
                    }

                    onError("La imagen contiene un QR pero no es una URI otpauth:// válida")
                }
                .addOnFailureListener { e ->
                    onError("Error al analizar la imagen: ${e.message}")
                }
        } catch (e: Exception) {
            onError("Error al abrir la imagen: ${e.message}")
        }
    }
}