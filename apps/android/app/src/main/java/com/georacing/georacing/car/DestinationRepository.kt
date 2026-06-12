package com.georacing.georacing.car

class DestinationRepository {
    fun getDestinations(): List<DestinationModel> = listOf(
        DestinationModel(
            id = "main_access",
            name = "Acceso Principal",
            description = "Entrada principal del Circuit de Barcelona-Catalunya",
            latitude = 41.5696,
            longitude = 2.2570,
            type = DestinationType.GATE
        ),
        DestinationModel(
            id = "porta_1",
            name = "Porta 1",
            description = "Acceso norte habitual para publico",
            latitude = 41.5693,
            longitude = 2.2561,
            type = DestinationType.GATE
        ),
        DestinationModel(
            id = "porta_3",
            name = "Porta 3",
            description = "Acceso muy usado en grandes premios",
            latitude = 41.5684,
            longitude = 2.2587,
            type = DestinationType.GATE
        ),
        DestinationModel(
            id = "porta_7",
            name = "Porta 7",
            description = "Entrada lado este cercana a tribunas G y H",
            latitude = 41.5714,
            longitude = 2.2620,
            type = DestinationType.GATE
        ),
        DestinationModel(
            id = "parking_north",
            name = "Parking Norte",
            description = "Parking general en zona norte",
            latitude = 41.5731,
            longitude = 2.2560,
            type = DestinationType.PARKING
        ),
        DestinationModel(
            id = "parking_south",
            name = "Parking Sur",
            description = "Parking general zona sur",
            latitude = 41.5658,
            longitude = 2.2580,
            type = DestinationType.PARKING
        ),
        DestinationModel(
            id = "paddock_access",
            name = "Acceso Paddock",
            description = "Entrada al paddock y boxes",
            latitude = 41.5705,
            longitude = 2.2576,
            type = DestinationType.PADDOCK
        ),
        DestinationModel(
            id = "fanzone",
            name = "Fan Zone",
            description = "Zona de actividades para fans",
            latitude = 41.5670,
            longitude = 2.2595,
            type = DestinationType.FAN_ZONE
        ),
        DestinationModel(
            id = "service_tower",
            name = "Torre de Control",
            description = "Oficinas y sala de briefing",
            latitude = 41.5697,
            longitude = 2.2575,
            type = DestinationType.SERVICE
        )
    )
}
