package com.georacing.georacing.car

/**
 * Data model for a Point of Interest (POI) in the circuit.
 */
data class PoiModel(
    val id: String,
    val name: String,
    val description: String,
    val type: PoiType,
    val latitude: Double,
    val longitude: Double
)

/**
 * Types of POIs available.
 */
enum class PoiType {
    PARKING,
    GATE,
    FANZONE,
    SERVICE,
    MEDICAL,
    OTHER
}

/**
 * Repository providing access to real GPS POI data for Circuit de Barcelona-Catalunya.
 * 
 * Coordenadas verificadas por GIS Developer - 2026-01-29
 */
object PoiRepository {

    private val allPois = listOf(
        // =====================================================================
        // ACCESOS / PUERTAS (Gates) - Real GPS Coordinates
        // =====================================================================
        PoiModel(
            id = "gate_1",
            name = "Puerta 1",
            description = "Acceso Este - Pelouse/Tribuna G",
            type = PoiType.GATE,
            latitude = 41.5652,
            longitude = 2.2660
        ),
        PoiModel(
            id = "gate_2",
            name = "Puerta 2",
            description = "Acceso Sur - Tribuna H/Escuela",
            type = PoiType.GATE,
            latitude = 41.5628,
            longitude = 2.2635
        ),
        PoiModel(
            id = "gate_3",
            name = "Puerta 3",
            description = "Acceso Oeste - Entrada Principal",
            type = PoiType.GATE,
            latitude = 41.5694,
            longitude = 2.2549
        ),
        PoiModel(
            id = "gate_4",
            name = "Puerta 4",
            description = "Acceso Noroeste - Tribunas J/K",
            type = PoiType.GATE,
            latitude = 41.5735,
            longitude = 2.2562
        ),
        PoiModel(
            id = "gate_5",
            name = "Puerta 5",
            description = "Acceso Norte - Tribuna Principal Norte",
            type = PoiType.GATE,
            latitude = 41.5752,
            longitude = 2.2588
        ),
        PoiModel(
            id = "gate_6",
            name = "Puerta 6",
            description = "Acceso Norte - Tribuna A/F",
            type = PoiType.GATE,
            latitude = 41.5768,
            longitude = 2.2615
        ),
        PoiModel(
            id = "gate_7",
            name = "Puerta 7",
            description = "Acceso Nordeste - Zona Estadio/Tribuna L",
            type = PoiType.GATE,
            latitude = 41.5742,
            longitude = 2.2675
        ),

        // =====================================================================
        // PARKINGS PÚBLICOS - Real GPS Coordinates
        // =====================================================================
        PoiModel(
            id = "parking_a",
            name = "Parking A",
            description = "Norte - Para Gates 5, 6, 7",
            type = PoiType.PARKING,
            latitude = 41.5775,
            longitude = 2.2610
        ),
        PoiModel(
            id = "parking_b",
            name = "Parking B",
            description = "Oeste - Cerca de Puerta 4",
            type = PoiType.PARKING,
            latitude = 41.5745,
            longitude = 2.2555
        ),
        PoiModel(
            id = "parking_c",
            name = "Parking C",
            description = "Principal - Cerca de Puerta 3",
            type = PoiType.PARKING,
            latitude = 41.5685,
            longitude = 2.2530
        ),
        PoiModel(
            id = "parking_d",
            name = "Parking D",
            description = "Sur - Cerca de Puerta 1/2",
            type = PoiType.PARKING,
            latitude = 41.5610,
            longitude = 2.2640
        ),

        // =====================================================================
        // PUNTOS DE INTERÉS CRÍTICOS - Real GPS Coordinates
        // =====================================================================
        PoiModel(
            id = "paddock_entrance",
            name = "Acceso Paddock",
            description = "Entrada exclusiva acreditados/VIP",
            type = PoiType.GATE,
            latitude = 41.5682,
            longitude = 2.2571
        ),
        PoiModel(
            id = "medical_center",
            name = "Centro Médico",
            description = "Hospital del Circuito",
            type = PoiType.MEDICAL,
            latitude = 41.5675,
            longitude = 2.2565
        ),
        PoiModel(
            id = "helipad",
            name = "Helipuerto",
            description = "Evacuación aérea de emergencia",
            type = PoiType.SERVICE,
            latitude = 41.5660,
            longitude = 2.2580
        )
    )

    fun getAllPois(): List<PoiModel> = allPois

    fun getById(id: String): PoiModel? {
        return allPois.find { it.id == id }
    }

    fun getByType(type: PoiType): List<PoiModel> {
        return allPois.filter { it.type == type }
    }
    
    fun getGates(): List<PoiModel> = getByType(PoiType.GATE)
    
    fun getParkings(): List<PoiModel> = getByType(PoiType.PARKING)
}
