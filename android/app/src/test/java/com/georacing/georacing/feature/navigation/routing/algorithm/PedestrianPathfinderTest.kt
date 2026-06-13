package com.georacing.georacing.feature.navigation.routing.algorithm

import com.georacing.georacing.feature.navigation.routing.models.CircuitEdge
import com.georacing.georacing.feature.navigation.routing.models.CircuitNode
import com.georacing.georacing.feature.navigation.routing.models.RoutePreference
import com.georacing.georacing.feature.navigation.routing.models.SurfaceType
import org.junit.Assert.assertEquals
import org.junit.Test

class PedestrianPathfinderTest {

    @Test
    fun `test thermal routing chooses longer shaded path when hot`() {
        // GRAFO DE PRUEBA:
        // Origen (A) ---> Destino (C)
        // Existen dos caminos:
        // Ruta 1 (Directa): A -> C (100 metros) PERO es asfalto a pleno sol (shadeFactor = 0.0)
        // Ruta 2 (Larga):   A -> B -> C (150 metros) PERO es césped y sombra total (shadeFactor = 1.0)

        val nodeA = CircuitNode("A", 41.569, 2.257)
        val nodeB = CircuitNode("B", 41.570, 2.258)
        val nodeC = CircuitNode("C", 41.571, 2.258)

        val nodes = mapOf("A" to nodeA, "B" to nodeB, "C" to nodeC)

        // Arista Directa (Corta pero mortal al sol)
        val edgeDirect = CircuitEdge(
            id = "E1", source = "A", destination = "C",
            distanceMeters = 100f,
            surfaceType = SurfaceType.ASPHALT,
            shadeFactor = 0.0f
        )

        // Aristas Ruta Larga (Frescas)
        val edgeSafe1 = CircuitEdge(
            id = "E2", source = "A", destination = "B",
            distanceMeters = 75f,
            surfaceType = SurfaceType.GRASS,
            shadeFactor = 1.0f
        )
        val edgeSafe2 = CircuitEdge(
            id = "E3", source = "B", destination = "C",
            distanceMeters = 75f,
            surfaceType = SurfaceType.GRASS,
            shadeFactor = 1.0f
        )

        val edges = listOf(edgeDirect, edgeSafe1, edgeSafe2)
        val pathfinder = PedestrianPathfinder(nodes, edges)

        // CASO 1: Preferencia = FASTEST (Minimizar Distancia siempre)
        val fastRoute = pathfinder.findRoute("A", "C", RoutePreference.FASTEST, 35.0f)
        assertEquals(1, fastRoute.size)
        assertEquals("E1", fastRoute[0].id) // Escoge la ruta de 100 metros

        // CASO 2: Preferencia = COOLEST, Temperatura Agradable (No hace falta sombra extrema)
        val coolRouteWinter = pathfinder.findRoute("A", "C", RoutePreference.COOLEST, 20.0f)
        assertEquals(1, coolRouteWinter.size)
        assertEquals("E1", coolRouteWinter[0].id) // Sigue escogiendo la corta porque no hay riesgo térmico

        // CASO 3: Preferencia = COOLEST, Temperatura Extrema (Resiliencia Industrial Activada)
        val coolRouteSummer = pathfinder.findRoute("A", "C", RoutePreference.COOLEST, 35.0f)
        assertEquals(2, coolRouteSummer.size)
        assertEquals("E2", coolRouteSummer[0].id)
        assertEquals("E3", coolRouteSummer[1].id) // Ha desviado al usuario por B para que esté en césped y sombra
    }
}
