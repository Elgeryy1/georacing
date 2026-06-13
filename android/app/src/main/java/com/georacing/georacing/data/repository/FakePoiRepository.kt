package com.georacing.georacing.data.repository

import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType
import com.georacing.georacing.domain.repository.PoiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePoiRepository : PoiRepository {
    override fun getPois(): Flow<List<Poi>> = flow {
        emit(
            listOf(
                Poi(
                    id = "1",
                    name = "WC Zona A",
                    type = PoiType.WC,
                    description = "Baños públicos cerca de la entrada",
                    zone = "Zone A",
                    mapX = 0.4f,
                    mapY = 0.85f
                ),
                Poi(
                    id = "2",
                    name = "Burger Point",
                    type = PoiType.FOOD,
                    description = "Hamburguesas y bebidas",
                    zone = "Zone B",
                    mapX = 0.6f,
                    mapY = 0.55f
                ),
                Poi(
                    id = "3",
                    name = "Merch Shop",
                    type = PoiType.MERCH,
                    description = "Camisetas y gorras oficiales",
                    zone = "Zone A",
                    mapX = 0.55f,
                    mapY = 0.75f
                )
            )
        )
    }

    /**
     * No-op: FakePoiRepository no sincroniza con red.
     */
    override suspend fun refreshPois() {
        // No-op - datos son fake/estáticos
    }
}
