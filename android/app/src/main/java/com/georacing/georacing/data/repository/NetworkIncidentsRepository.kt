package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.toDto
import com.georacing.georacing.domain.model.IncidentReport
import com.georacing.georacing.domain.repository.IncidentsRepository

class NetworkIncidentsRepository : IncidentsRepository {
    override suspend fun getIncidents(): kotlinx.coroutines.flow.Flow<List<IncidentReport>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun reportIncident(incident: IncidentReport) {
        try {
            RetrofitClient.api.sendIncident(incident.toDto())
        } catch (e: Exception) {
            Log.e("NetworkIncidentsRepo", "Error reporting incident", e)
            throw e
        }
    }
}
