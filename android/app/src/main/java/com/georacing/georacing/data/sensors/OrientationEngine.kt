package com.georacing.georacing.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Provides device orientation (Azimuth & Pitch) using Accelerometer + Magnetometer.
 * Uses a Low-Pass Filter to smooth out jitter.
 */
class OrientationEngine(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    data class Orientation(
        val azimuth: Float, // 0-360 degrees
        val pitch: Float    // -90 to +90 degrees
    )

    fun getOrientationFlow(): Flow<Orientation> = callbackFlow {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravity = lowPassFilter(event.values.clone(), gravity)
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = lowPassFilter(event.values.clone(), geomagnetic)
                }

                if (gravity != null && geomagnetic != null) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)

                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)

                        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() // 0..360 (needs normalization)
                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

                        // Normalize Azimuth to 0-360
                        val normalizedAzimuth = (azimuth + 360) % 360

                        trySend(Orientation(normalizedAzimuth, pitch))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No-op
            }
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Simple Low-Pass Filter
    private fun lowPassFilter(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input
        val alpha = 0.1f // Smooth factor (Lower = Slower/Smoother)
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
        return output
    }
}
