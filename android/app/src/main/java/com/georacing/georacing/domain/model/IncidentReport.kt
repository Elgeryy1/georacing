package com.georacing.georacing.domain.model

data class IncidentReport(
    val category: IncidentCategory,
    val description: String,
    val beaconId: String?,
    val zone: String?,
    val timestamp: Long,
    val photoData: ByteArray? = null // Compressed JPEG image data
)

enum class IncidentCategory(val displayName: String) {
    LIMPIEZA("Limpieza"),
    ACCESIBILIDAD("Accesibilidad"),
    COMIDA_BEBIDA("Comida/Bebida"),
    SEÑALIZACIÓN("Señalización"),
    SEGURIDAD("Seguridad"),
    INFRAESTRUCTURA("Infraestructura"),
    SERVICIO_MEDICO("Servicio Médico"),
    PERDIDOS_ENCONTRADOS("Objetos Perdidos"),
    OTRA("Otra")
}
