package com.georacing.georacing.data.repository

import com.georacing.georacing.domain.model.CircuitNode
import com.georacing.georacing.domain.model.Confidence
import com.georacing.georacing.domain.model.NodeType

/**
 * Single Source of Truth for critical Circuit Locations.
 * 
 * Dataset Version: 2026.01.29 (Official)
 */
object CircuitLocationsRepository {

    private val nodes = listOf(
        // 🟢 GATES (HIGH CONFIDENCE)
        CircuitNode("gate_1", "Gate 1", NodeType.GATE, 41.5736169, 2.2577213, Confidence.HIGH, "Official Dataset", "Acceso Principal"),
        CircuitNode("gate_2", "Gate 2", NodeType.GATE, 41.57429, 2.26399, Confidence.HIGH, "Official Dataset", "Zona Norte"),
        CircuitNode("gate_3", "Gate 3", NodeType.GATE, 41.57037, 2.26365, Confidence.HIGH, "Official Dataset", "Zona Este"),
        CircuitNode("gate_4", "Gate 4", NodeType.GATE, 41.56639, 2.26010, Confidence.HIGH, "Official Dataset", "Zona Sur-Este"),
        CircuitNode("gate_5", "Gate 5", NodeType.GATE, 41.56582, 2.25851, Confidence.HIGH, "Official Dataset", "Zona Sur"),
        CircuitNode("gate_6", "Gate 6", NodeType.GATE, 41.56356, 2.25171, Confidence.HIGH, "Official Dataset", "Zona Sur-Oeste"),
        CircuitNode("gate_7", "Gate 7", NodeType.GATE, 41.56917, 2.25379, Confidence.HIGH, "Official Dataset", "Zona Oeste"),

        // 🅿️ PARKINGS (HIGH CONFIDENCE)
        CircuitNode("parking_a1_h1", "Parking A1/H1", NodeType.PARKING, 41.56926, 2.25309, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_c", "Parking C", NodeType.PARKING, 41.56691, 2.26100, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_h2_in6", "Parking H2/In6", NodeType.PARKING, 41.56855, 2.26273, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_paddock", "Paddock", NodeType.PARKING, 41.57040, 2.25970, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_d", "Parking D", NodeType.PARKING, 41.5640457, 2.2504530, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_f", "Parking F", NodeType.PARKING, 41.5625943, 2.2539039, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_a2", "Parking A2", NodeType.PARKING, 41.5752291, 2.2607307, Confidence.HIGH, "Official Dataset"),
        CircuitNode("parking_a3", "Parking A3", NodeType.PARKING, 41.5753531, 2.2649349, Confidence.HIGH, "Official Dataset"),

        // 🟡 PARKINGS (MEDIUM CONFIDENCE)
        CircuitNode("parking_gate_7", "Parking Puerta 7", NodeType.PARKING, 41.5694654, 2.2537845, Confidence.MEDIUM, "Official Dataset"),
        CircuitNode("parking_pa_approx", "Parking P A", NodeType.PARKING, 41.575186, 2.2636151, Confidence.MEDIUM, "Official Dataset"),
        CircuitNode("parking_b_approx", "Parking B", NodeType.PARKING, 41.5750478, 2.2544808, Confidence.MEDIUM, "Official Dataset"),

        // ❌ PARKINGS NO DEFINIDOS (PENDING)
        // Se definen con coordenadas 0.0, 0.0 o placeholder para evitar uso en navegación
        CircuitNode("parking_pa_pending", "P.A (Pendiente)", NodeType.PARKING, 0.0, 0.0, Confidence.PENDING, "Missing Data"),
        CircuitNode("parking_pb_pending", "P.B (Pendiente)", NodeType.PARKING, 0.0, 0.0, Confidence.PENDING, "Missing Data"),
        CircuitNode("parking_pbus_pending", "P.Bus (Pendiente)", NodeType.PARKING, 0.0, 0.0, Confidence.PENDING, "Missing Data"),
        CircuitNode("parking_pdu_pending", "P.Du (Pendiente)", NodeType.PARKING, 0.0, 0.0, Confidence.PENDING, "Missing Data"),
        CircuitNode("parking_po_pending", "P.O (Pendiente)", NodeType.PARKING, 0.0, 0.0, Confidence.PENDING, "Missing Data")
    )

    // Public API ==================================================================================

    fun getAllNodes(): List<CircuitNode> = nodes

    fun getGates(): List<CircuitNode> = nodes.filter { it.type == NodeType.GATE }

    /**
     * Returns ONLY actionable parkings (HIGH or MEDIUM confidence).
     * Filters out PENDING parkings to prevent invalid routing.
     */
    fun getNavigableParkings(): List<CircuitNode> {
        return nodes.filter { 
            it.type == NodeType.PARKING && 
            it.confidence != Confidence.PENDING 
        }
    }
    
    fun getAllParkingsIncludingPending(): List<CircuitNode> = nodes.filter { it.type == NodeType.PARKING }

    fun getNodeById(id: String): CircuitNode? = nodes.find { it.id == id }
    
    fun getByNameLoose(query: String): CircuitNode? {
        return nodes.find { it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true) }
    }
}
