package com.georacing.georacing.domain.manager

import android.content.Context
import android.graphics.ImageFormat
import android.location.Location
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Sistema de auto-posicionamiento por QR para circuitos GeoRacing.
 *
 * Los marcadores QR contienen JSON con la posición exacta:
 * ```json
 * {
 *   "type": "georacing_marker",
 *   "id": "M-001",
 *   "lat": 41.5695,
 *   "lon": 2.2585,
 *   "zone": "TribunaPrincipal",
 *   "floor": 0,
 *   "accuracy_m": 1.0
 * }
 * ```
 *
 * Cuando el usuario escanea un QR, el sistema:
 * 1. Decodifica la posición exacta del marcador
 * 2. Compara con la posición GPS actual
 * 3. Aplica corrección de drift al GPS
 * 4. Muestra la posición corregida en el mapa/AR
 */
class QrPositioningManager(private val context: Context) {

    companion object {
        private const val TAG = "QrPositioning"
        private const val MARKER_TYPE = "georacing_marker"
        private const val CORRECTION_DECAY_MS = 60_000L // Corrección expira en 60s
        private const val MAX_CORRECTION_DISTANCE_M = 200.0 // No corregir si GPS está a >200m
    }

    // ── Modelos ──

    data class QrMarker(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val zone: String,
        val floor: Int = 0,
        val accuracyMeters: Double = 1.0
    )

    data class PositionCorrection(
        val marker: QrMarker,
        val latOffset: Double,   // Corrección a sumar al GPS lat
        val lonOffset: Double,   // Corrección a sumar al GPS lon
        val timestamp: Long,
        val gpsAccuracyAtScan: Float
    )

    // ── Estado ──

    private val _lastMarker = MutableStateFlow<QrMarker?>(null)
    val lastMarker: StateFlow<QrMarker?> = _lastMarker.asStateFlow()

    private val _correction = MutableStateFlow<PositionCorrection?>(null)
    val correction: StateFlow<PositionCorrection?> = _correction.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanHistory = MutableStateFlow<List<QrMarker>>(emptyList())
    val scanHistory: StateFlow<List<QrMarker>> = _scanHistory.asStateFlow()

    private var imageAnalysis: ImageAnalysis? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val qrReader = MultiFormatReader().apply {
        setHints(mapOf(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    // ── API Pública ──

    /**
     * Inicia el escaneo continuo de QR con CameraX.
     */
    fun startScanning(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor, QrAnalyzer())
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis
                )
                _isScanning.value = true
                Log.i(TAG, "📷 QR scanning started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting QR scanner", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Detiene el escaneo.
     */
    fun stopScanning() {
        _isScanning.value = false
        imageAnalysis = null
        Log.i(TAG, "📷 QR scanning stopped")
    }

    /**
     * Procesa una cadena QR manualmente (por ejemplo, desde un Activity result).
     */
    fun processQrContent(content: String, currentLocation: Location?): QrMarker? {
        val marker = parseQrContent(content) ?: return null
        onMarkerDetected(marker, currentLocation)
        return marker
    }

    /**
     * Aplica la corrección GPS almacenada a una ubicación.
     * Devuelve la ubicación corregida, o la original si no hay corrección válida.
     */
    fun applyCorrectedPosition(location: Location): Location {
        val corr = _correction.value ?: return location

        // Verificar si la corrección ha expirado
        if (System.currentTimeMillis() - corr.timestamp > CORRECTION_DECAY_MS) {
            _correction.value = null
            return location
        }

        // Aplicar con decay gradual
        val elapsed = System.currentTimeMillis() - corr.timestamp
        val decayFactor = 1.0 - (elapsed.toDouble() / CORRECTION_DECAY_MS)

        return Location(location).apply {
            latitude = location.latitude + corr.latOffset * decayFactor
            longitude = location.longitude + corr.lonOffset * decayFactor
            accuracy = corr.marker.accuracyMeters.toFloat() // Precisión del marcador
        }
    }

    /**
     * Devuelve la distancia de drift GPS detectada en el último escaneo (metros).
     */
    fun getLastDriftDistance(): Float? {
        val corr = _correction.value ?: return null
        val results = FloatArray(1)
        Location.distanceBetween(0.0, 0.0, corr.latOffset, corr.lonOffset, results)
        return results[0]
    }

    fun destroy() {
        stopScanning()
        analysisExecutor.shutdown()
    }

    // ── Lógica interna ──

    private fun onMarkerDetected(marker: QrMarker, currentLocation: Location?) {
        _lastMarker.value = marker
        _scanHistory.value = _scanHistory.value + marker

        if (currentLocation != null) {
            // Calcular drift GPS
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                marker.latitude, marker.longitude, results
            )
            val driftDistance = results[0]

            if (driftDistance < MAX_CORRECTION_DISTANCE_M) {
                val latOffset = marker.latitude - currentLocation.latitude
                val lonOffset = marker.longitude - currentLocation.longitude

                _correction.value = PositionCorrection(
                    marker = marker,
                    latOffset = latOffset,
                    lonOffset = lonOffset,
                    timestamp = System.currentTimeMillis(),
                    gpsAccuracyAtScan = currentLocation.accuracy
                )

                Log.i(TAG, "📍 Position corrected: drift=${driftDistance}m " +
                        "from marker ${marker.id} (${marker.zone})")
            } else {
                Log.w(TAG, "⚠️ Marker ${marker.id} too far from GPS " +
                        "(${driftDistance}m > ${MAX_CORRECTION_DISTANCE_M}m), ignoring")
            }
        }
    }

    private fun parseQrContent(content: String): QrMarker? {
        return try {
            val json = JSONObject(content)
            if (json.optString("type") != MARKER_TYPE) {
                Log.d(TAG, "QR not a GeoRacing marker: ${json.optString("type")}")
                return null
            }

            QrMarker(
                id = json.getString("id"),
                latitude = json.getDouble("lat"),
                longitude = json.getDouble("lon"),
                zone = json.optString("zone", "Unknown"),
                floor = json.optInt("floor", 0),
                accuracyMeters = json.optDouble("accuracy_m", 1.0)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse QR content: ${e.message}")
            null
        }
    }

    // ── QR Image Analyzer ──

    private inner class QrAnalyzer : ImageAnalysis.Analyzer {

        private var lastProcessedMs = 0L
        private val throttleMs = 500L // Procesar máximo 2 fps

        override fun analyze(imageProxy: ImageProxy) {
            val now = System.currentTimeMillis()
            if (now - lastProcessedMs < throttleMs) {
                imageProxy.close()
                return
            }
            lastProcessedMs = now

            try {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val source = PlanarYUVLuminanceSource(
                    bytes,
                    imageProxy.width, imageProxy.height,
                    0, 0,
                    imageProxy.width, imageProxy.height,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = qrReader.decodeWithState(binaryBitmap)

                val marker = parseQrContent(result.text)
                if (marker != null) {
                    Log.i(TAG, "🔲 QR detected: ${marker.id} @ ${marker.zone}")
                    onMarkerDetected(marker, null) // GPS location will be applied by caller
                }
            } catch (_: Exception) {
                // No QR found — normal, ignore
            } finally {
                qrReader.reset()
                imageProxy.close()
            }
        }
    }
}
