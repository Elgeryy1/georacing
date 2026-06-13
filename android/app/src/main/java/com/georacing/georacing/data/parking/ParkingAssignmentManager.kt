package com.georacing.georacing.data.parking

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gestor de asignación dinámica de parking según el tipo de entrada del usuario.
 * 
 * El sistema asigna automáticamente un parking basándose en:
 * - Tipo de entrada (General, VIP, Paddock, Prensa, Staff)
 * - Disponibilidad (en producción, se consultaría backend)
 * - Proximidad a la puerta de acceso correspondiente
 */
object ParkingAssignmentManager {

    private const val TAG = "ParkingAssignment"

    enum class TicketType(val displayName: String) {
        GENERAL("General"),
        VIP("VIP / Hospitality"),
        PADDOCK("Paddock Club"),
        PRESS("Prensa / Media"),
        STAFF("Staff / Equipo")
    }

    data class ParkingAssignment(
        val parkingId: String,
        val parkingName: String,
        val latitude: Double,
        val longitude: Double,
        val gateName: String, // Puerta de acceso recomendada
        val gateLat: Double,
        val gateLon: Double,
        val zone: String // Zona del circuito
    )

    // Asignaciones según tipo de entrada
    private val assignments = mapOf(
        TicketType.GENERAL to ParkingAssignment(
            parkingId = "parking_c",
            parkingName = "Parking C (General)",
            latitude = 41.5660,
            longitude = 2.2565,
            gateName = "Porta 7",
            gateLat = 41.5675,
            gateLon = 2.2555,
            zone = "Sur"
        ),
        TicketType.VIP to ParkingAssignment(
            parkingId = "parking_a",
            parkingName = "Parking A (VIP)",
            latitude = 41.5715,
            longitude = 2.2555,
            gateName = "Acceso Principal",
            gateLat = 41.5693,
            gateLon = 2.2577,
            zone = "Norte - Hospitality"
        ),
        TicketType.PADDOCK to ParkingAssignment(
            parkingId = "parking_paddock",
            parkingName = "Parking Paddock",
            latitude = 41.5720,
            longitude = 2.2570,
            gateName = "Acceso Paddock",
            gateLat = 41.5702,
            gateLon = 2.2575,
            zone = "Paddock"
        ),
        TicketType.PRESS to ParkingAssignment(
            parkingId = "parking_b",
            parkingName = "Parking B (Media)",
            latitude = 41.5710,
            longitude = 2.2545,
            gateName = "Porta 1",
            gateLat = 41.5700,
            gateLon = 2.2590,
            zone = "Norte - Media Center"
        ),
        TicketType.STAFF to ParkingAssignment(
            parkingId = "parking_staff",
            parkingName = "Parking Staff",
            latitude = 41.5718,
            longitude = 2.2560,
            gateName = "Acceso Paddock",
            gateLat = 41.5702,
            gateLon = 2.2575,
            zone = "Zona de Servicio"
        )
    )

    /**
     * Obtiene la asignación de parking según el tipo de entrada.
     */
    fun getAssignment(ticketType: TicketType): ParkingAssignment {
        val assignment = assignments[ticketType] ?: assignments[TicketType.GENERAL]!!
        Log.i(TAG, "🅿️ Parking asignado: ${assignment.parkingName} (entrada: ${ticketType.displayName})")
        return assignment
    }

    /**
     * Obtiene la asignación por string de tipo de entrada (ej: desde QR o Firestore).
     */
    fun getAssignmentByCode(code: String): ParkingAssignment {
        val ticketType = when (code.uppercase()) {
            "A", "VIP", "HOSPITALITY" -> TicketType.VIP
            "B", "PRESS", "MEDIA" -> TicketType.PRESS
            "C", "GENERAL", "GA" -> TicketType.GENERAL
            "P", "PADDOCK" -> TicketType.PADDOCK
            "S", "STAFF", "TEAM" -> TicketType.STAFF
            else -> TicketType.GENERAL
        }
        return getAssignment(ticketType)
    }

    /**
     * Lista todos los parkings disponibles.
     */
    fun getAllParkings(): List<ParkingAssignment> = assignments.values.toList()
}
