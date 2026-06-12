package com.georacing.georacing.features.ar

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object QRCodePositioningManager {
    private const val TAG = "QRCodePositioning"
    private const val QR_PREFIX = "GEO:" // Payload format: GEO:lat,lon,id

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processImage(
        imageProxy: ImageProxy,
        context: Context,
        onLocationRecalibrated: (Double, Double, String) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        handleBarcode(barcode, context, onLocationRecalibrated)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "QR Scan failed", it)
                }
                .addOnCompleteListener {
                    // Must close imageProxy to allow next frame
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleBarcode(
        barcode: Barcode,
        context: Context,
        onLocationRecalibrated: (Double, Double, String) -> Unit
    ) {
        val rawValue = barcode.rawValue ?: return

        if (rawValue.startsWith(QR_PREFIX)) {
            // Parse Payload: GEO:41.56,2.26,GATE_1
            try {
                val content = rawValue.removePrefix(QR_PREFIX)
                val parts = content.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].toDouble()
                    val lon = parts[1].toDouble()
                    val id = if (parts.size > 2) parts[2] else "FIX_POINT"

                    Log.i(TAG, "QR Positioning Match: $lat, $lon ($id)")

                    // In a real app, you would inject this into LocationRepository
                    onLocationRecalibrated(lat, lon, id)

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "📍 Ubicación Recalibrada: $id", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Invalid GEO QR format", e)
            }
        }
    }
}
