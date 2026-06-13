package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.domain.model.IncidentReport
import com.georacing.georacing.domain.repository.IncidentsRepository
import kotlinx.coroutines.delay

class FakeIncidentsRepository : IncidentsRepository {
    override suspend fun getIncidents(): kotlinx.coroutines.flow.Flow<List<IncidentReport>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun reportIncident(incident: IncidentReport) {
        // Simulate network delay
        delay(1000)
        Log.d("FakeIncidentsRepo", "Incident sent: $incident")
    }
}
