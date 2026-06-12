package com.georacing.georacing.domain.repository

import com.georacing.georacing.domain.model.IncidentReport

interface IncidentsRepository {
    suspend fun getIncidents(): kotlinx.coroutines.flow.Flow<List<IncidentReport>>
    suspend fun reportIncident(incident: IncidentReport)
}
