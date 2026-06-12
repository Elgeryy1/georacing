package com.georacing.georacing.infrastructure.security

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Genera un fondo de pantalla táctico en caso de emergencia médica.
 * Graba los datos vitales a bajo nivel (Canvas) y fuerza el bloqueo de pantalla del SO
 * para que los médicos/policía vean la información aunque el terminal esté bloqueado.
 */
class MedicalWallpaperGenerator(
    private val context: Context
) {

    /**
     * Dibuja un nuevo Bitmap con la info médica y lo aplica en el Lock Screen.
     */
    suspend fun applyEmergencyWallpaper(
        bloodType: String,
        emergencyContact: String,
        ticketId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            // Configuración del Bitmap para minimizar uso de RAM. (RGB_565 es suficiente pero ARGB_8888 asegura texto nítido)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fondo negro puro (ahorro extremo OLED)
            canvas.drawColor(Color.BLACK)

            val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 60f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val paintWarning = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                textSize = 80f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            // Diseño y Pintado de la UI estática en Canvas
            val centerX = width / 2f
            var startY = height * 0.2f

            canvas.drawText("⚠️ MEDICAL EMERGENCY ⚠️", centerX, startY, paintWarning)
            
            startY += 200f
            paintText.textSize = 50f
            canvas.drawText("Blood Type:", centerX, startY, paintText)
            startY += 80f
            canvas.drawText(bloodType, centerX, startY, paintWarning) // Resaltado en rojo
            
            startY += 200f
            canvas.drawText("ICE (In Case of Emergency):", centerX, startY, paintText)
            startY += 80f
            paintText.textSize = 70f
            canvas.drawText(emergencyContact, centerX, startY, paintText)

            startY += 200f
            paintText.textSize = 40f
            canvas.drawText("Fan Ticket ID / Insurance:", centerX, startY, paintText)
            startY += 60f
            canvas.drawText(ticketId, centerX, startY, paintText)

            // Aplicar Wallpaper exclusivamente a la pantalla de bloqueo (FLAG_LOCK)
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // Requerirá permiso SET_WALLPAPER en el Manifest
            wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)

            bitmap.recycle() // Liberar memoria nativa
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
