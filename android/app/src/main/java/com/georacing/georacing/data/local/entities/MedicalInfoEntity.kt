package com.georacing.georacing.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity para Información Médica del Usuario.
 * Tabla singleton (1 fila) con datos vitales para emergencias.
 * 
 * Usado para generar el Lock Screen médico con QR + datos vitales.
 */
@Entity(tableName = "medical_info")
data class MedicalInfoEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    
    /** Grupo sanguíneo (A+, A-, B+, B-, AB+, AB-, O+, O-) */
    val bloodType: String? = null,
    
    /** Alergias conocidas (medicamentos, alimentos, etc.) */
    val allergies: String? = null,
    
    /** Condiciones médicas relevantes (diabetes, epilepsia, etc.) */
    val medicalConditions: String? = null,
    
    /** Nombre del contacto de emergencia */
    val emergencyContactName: String? = null,
    
    /** Teléfono del contacto de emergencia */
    val emergencyContactPhone: String? = null,
    
    /** Notas adicionales para personal médico */
    val medicalNotes: String? = null,
    
    /** Timestamp de última actualización */
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
