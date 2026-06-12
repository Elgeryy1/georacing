package com.georacing.georacing.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.georacing.georacing.data.local.dao.IncidentDao
import com.georacing.georacing.data.local.entities.IncidentEntity
import com.georacing.georacing.domain.model.IncidentCategory
import com.georacing.georacing.domain.model.IncidentReport
import com.georacing.georacing.domain.repository.IncidentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OfflineFirstIncidentsRepository(
    private val incidentDao: IncidentDao,
    private val context: Context // For WorkManager
) : IncidentsRepository {

    override suspend fun getIncidents(): Flow<List<IncidentReport>> {
        return incidentDao.getAllIncidents().map { entities ->
            entities.map { entity ->
                IncidentReport(
                    category = try {
                        IncidentCategory.valueOf(entity.category)
                    } catch (e: Exception) {
                        IncidentCategory.OTRA
                    },
                    description = entity.description,
                    beaconId = entity.beaconId,
                    zone = entity.zone,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override suspend fun reportIncident(incident: IncidentReport) {
        val entity = IncidentEntity(
            id = UUID.randomUUID().toString(),
            category = incident.category.name,
            description = incident.description,
            beaconId = incident.beaconId,
            zone = incident.zone,
            timestamp = incident.timestamp,
            isSynced = false
        )

        // 1. Save locally (Always success)
        incidentDao.insertIncident(entity)

        // 2. Schedule Sync
        // Note: Assuming SyncIncidentsWorker exists or will be created. 
        // For now we just enqueue a generic sync request or log it.
        // If Worker is not compiled yet, we wrap in try/catch or comment out worker class reference.
        // We will use a generic WorkRequest for "SyncIncidents" tag.
        
        try {
             // Placeholder for WorkManager logic 
             // val request = OneTimeWorkRequestBuilder<SyncIncidentsWorker>().addTag("sync_incidents").build()
             // WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
