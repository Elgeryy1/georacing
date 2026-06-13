package com.georacing.georacing.data.repository

import com.georacing.georacing.domain.model.ArrowDirection
import com.georacing.georacing.domain.model.BeaconConfig
import com.georacing.georacing.domain.repository.BeaconsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeBeaconsRepository : BeaconsRepository {
    override fun getBeacons(): Flow<List<BeaconConfig>> = flow {
        emit(
            listOf(
                BeaconConfig(
                    id = "BEACON_1",
                    name = "Acceso Principal",
                    zone = "Zone A",
                    mapX = 0.5f,
                    mapY = 0.8f,
                    messageNormal = "Bienvenido al Circuit de Barcelona-Catalunya",
                    messageEmergency = "Diríjase a la salida más cercana",
                    arrowDirection = ArrowDirection.UP
                ),
                BeaconConfig(
                    id = "BEACON_2",
                    name = "Tribuna Principal",
                    zone = "Zone B",
                    mapX = 0.5f,
                    mapY = 0.5f,
                    messageNormal = "Zona de Tribuna Principal",
                    messageEmergency = "Evacúe por las escaleras laterales",
                    arrowDirection = ArrowDirection.RIGHT
                )
            )
        )
    }
}
