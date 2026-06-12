package com.georacing.georacing.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.app.WallpaperManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Generador de Lock Screen Médico.
 * 
 * Crea un Bitmap con información vital del usuario:
 * - QR de entrada (para identificación)
 * - Grupo sanguíneo
 * - Alergias
 * - Contacto de emergencia
 * 
 * El usuario puede establecerlo como fondo de pantalla para
 * que el personal de emergencia pueda ver los datos sin desbloquear.
 */
object MedicalLockScreenGenerator {

    private const val TAG = "MedicalLockScreen"
    
    // Dimensiones estándar de wallpaper (1080x1920 para Full HD)
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val QR_SIZE = 400
    
    /**
     * Genera el Bitmap del Lock Screen médico.
     * 
     * @param qrData Datos para el código QR (ej: ID de entrada, URL perfil)
     * @param userName Nombre del usuario
     * @param bloodType Grupo sanguíneo (A+, B-, etc.)
     * @param allergies Lista de alergias separadas por coma
     * @param emergencyContact Nombre del contacto de emergencia
     * @param emergencyPhone Teléfono del contacto de emergencia
     * @param medicalNotes Notas adicionales
     * @return Bitmap listo para usar como wallpaper
     */
    fun generateBitmap(
        qrData: String,
        userName: String,
        bloodType: String?,
        allergies: String?,
        emergencyContact: String?,
        emergencyPhone: String?,
        medicalNotes: String? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fondo negro OLED (ahorra batería)
        canvas.drawColor(Color.BLACK)
        
        // Paints
        val titlePaint = Paint().apply {
            color = Color.RED
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val subtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val valuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val bloodTypePaint = Paint().apply {
            color = Color.RED
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        var yPos = 150f
        
        // Título de emergencia
        canvas.drawText("🆘 EMERGENCIA MÉDICA", WIDTH / 2f, yPos, titlePaint)
        yPos += 80f
        
        // Nombre
        canvas.drawText(userName.uppercase(), WIDTH / 2f, yPos, subtitlePaint)
        yPos += 100f
        
        // QR Code
        val qrBitmap = generateQRCode(qrData, QR_SIZE)
        val qrLeft = (WIDTH - QR_SIZE) / 2f
        canvas.drawBitmap(qrBitmap, qrLeft, yPos, null)
        yPos += QR_SIZE + 40f
        
        canvas.drawText("Escanear para identificación", WIDTH / 2f, yPos, labelPaint)
        yPos += 100f
        
        // Grupo Sanguíneo (Grande y prominente)
        if (!bloodType.isNullOrBlank()) {
            canvas.drawText("GRUPO SANGUÍNEO", WIDTH / 2f, yPos, labelPaint)
            yPos += 100f
            canvas.drawText(bloodType.uppercase(), WIDTH / 2f, yPos, bloodTypePaint)
            yPos += 120f
        }
        
        // Alergias
        if (!allergies.isNullOrBlank()) {
            canvas.drawText("⚠️ ALERGIAS", WIDTH / 2f, yPos, labelPaint)
            yPos += 60f
            // Dividir en líneas si es muy largo
            val lines = wrapText(allergies, 35)
            for (line in lines) {
                canvas.drawText(line.uppercase(), WIDTH / 2f, yPos, valuePaint)
                yPos += 70f
            }
            yPos += 30f
        }
        
        // Contacto de Emergencia
        if (!emergencyContact.isNullOrBlank() || !emergencyPhone.isNullOrBlank()) {
            canvas.drawText("📞 CONTACTO EMERGENCIA", WIDTH / 2f, yPos, labelPaint)
            yPos += 60f
            
            if (!emergencyContact.isNullOrBlank()) {
                canvas.drawText(emergencyContact, WIDTH / 2f, yPos, valuePaint)
                yPos += 70f
            }
            if (!emergencyPhone.isNullOrBlank()) {
                canvas.drawText(emergencyPhone, WIDTH / 2f, yPos, valuePaint)
                yPos += 70f
            }
            yPos += 30f
        }
        
        // Notas médicas
        if (!medicalNotes.isNullOrBlank()) {
            canvas.drawText("📋 NOTAS MÉDICAS", WIDTH / 2f, yPos, labelPaint)
            yPos += 60f
            val lines = wrapText(medicalNotes, 40)
            for (line in lines) {
                canvas.drawText(line, WIDTH / 2f, yPos, valuePaint.apply { textSize = 40f })
                yPos += 50f
            }
        }
        
        // Footer
        val footerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("GeoRacing - Circuit de Barcelona-Catalunya", WIDTH / 2f, HEIGHT - 80f, footerPaint)
        canvas.drawText("Esta información puede salvar una vida", WIDTH / 2f, HEIGHT - 40f, footerPaint)
        
        return bitmap
    }
    
    /**
     * Genera un código QR como Bitmap.
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.WHITE else Color.BLACK)
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
            // Retornar bitmap vacío en caso de error
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).drawColor(Color.RED)
            }
        }
    }
    
    /**
     * Divide texto largo en líneas.
     */
    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.length <= maxCharsPerLine) return listOf(text)
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            if (currentLine.length + word.length + 1 > maxCharsPerLine) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString().trim())
                    currentLine = StringBuilder()
                }
            }
            currentLine.append(word).append(" ")
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString().trim())
        }
        
        return lines
    }
    
    /**
     * Guarda el Bitmap en la galería.
     * 
     * @return URI del archivo guardado, o null si falla
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String = "georacing_medical_lockscreen"): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GeoRacing")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                
                Log.d(TAG, "Image saved to gallery: $uri")
            }
            
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to gallery", e)
            null
        }
    }
    
    /**
     * Establece el Bitmap como fondo de pantalla de bloqueo.
     * 
     * @return true si se estableció correctamente
     */
    fun setAsLockScreenWallpaper(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )
            } else {
                // En versiones anteriores, solo podemos establecer ambos
                wallpaperManager.setBitmap(bitmap)
            }
            
            Log.d(TAG, "Lock screen wallpaper set successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting lock screen wallpaper", e)
            false
        }
    }
}
