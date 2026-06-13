package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.model.GroupMemberLocation
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.EnsureColumnRequest
import com.georacing.georacing.data.remote.dto.EnsureTableRequest
import com.georacing.georacing.data.remote.dto.GroupLocationRequest
import com.georacing.georacing.data.remote.dto.UpsertRequest
import com.georacing.georacing.data.remote.dto.toDomain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.CRC32

class NetworkGroupRepository {
    
    companion object {
        private const val TAG = "NetworkGroupRepo"
        private const val GROUPS_TABLE = "groups"
        private const val GPS_TABLE = "group_gps"
    }

    private var schemaEnsured = false

    private suspend fun ensureSchema() {
        if (schemaEnsured) return

        // groups table
        RetrofitClient.api.ensureTable(EnsureTableRequest(GROUPS_TABLE))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GROUPS_TABLE, "owner_user_id", "VARCHAR(200)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GROUPS_TABLE, "name", "VARCHAR(255)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GROUPS_TABLE, "created_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GROUPS_TABLE, "updated_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"))

        // group_gps table
        RetrofitClient.api.ensureTable(EnsureTableRequest(GPS_TABLE))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GPS_TABLE, "user_uuid", "VARCHAR(200)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GPS_TABLE, "group_name", "VARCHAR(200)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GPS_TABLE, "lat", "DOUBLE"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GPS_TABLE, "lon", "DOUBLE"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GPS_TABLE, "displayName", "VARCHAR(255)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(GPS_TABLE, "last_seen", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"))

        schemaEnsured = true
    }

    private fun stableId(value: String): Int {
        val crc = CRC32().apply { update(value.toByteArray()) }.value
        return (crc % Int.MAX_VALUE).toInt()
    }

    suspend fun warmupSchema() {
        ensureSchema()
    }

    private fun gpsRowId(groupName: String, userId: String): Int =
        stableId("$groupName|$userId")

    private fun nowTimestampString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(java.util.Date())
    }

    suspend fun sendLocation(userId: String, groupName: String, lat: Double, lon: Double, displayName: String): Result<Unit> {
        return try {
            ensureSchema()
            val gpsId = gpsRowId(groupName, userId)
            val request = UpsertRequest(
                table = GPS_TABLE,
                data = mapOf(
                    "id" to gpsId,
                    "user_uuid" to userId,
                    "group_name" to groupName,
                    "lat" to lat,
                    "lon" to lon,
                    "displayName" to displayName,
                    "last_seen" to nowTimestampString()
                )
            )
            RetrofitClient.api.upsert(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending location", e)
            Result.failure(e)
        }
    }

    suspend fun removeUserFromGroup(userId: String, groupName: String): Result<Unit> {
        return try {
            ensureSchema()
            val gpsId = gpsRowId(groupName, userId)
            val request = UpsertRequest(
                table = GPS_TABLE,
                data = mapOf(
                    "id" to gpsId,
                    "user_uuid" to userId,
                    "group_name" to "", // sacar del grupo para que no aparezca en SELECT por group_name
                    "lat" to null,
                    "lon" to null,
                    "displayName" to null,
                    "last_seen" to nowTimestampString()
                )
            )
            RetrofitClient.api.upsert(request)
            Log.d(TAG, "User $userId removed from group $groupName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing user $userId from group $groupName", e)
            Result.failure(e)
        }
    }

    suspend fun createGroup(groupId: String, ownerUserId: String, groupName: String): Result<Unit> {
        return try {
            ensureSchema()
            val groupRowId = stableId(groupId)
            val request = UpsertRequest(
                table = GROUPS_TABLE,
                data = mapOf(
                    "id" to groupRowId,
                    "owner_user_id" to ownerUserId,
                    "name" to groupName,
                    "created_at" to nowTimestampString(),
                    "updated_at" to nowTimestampString()
                )
            )
            RetrofitClient.api.upsert(request)
            Log.d(TAG, "Group created/updated via upsert: $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group $groupId", e)
            Result.failure(e)
        }
    }

    fun getGroupMembers(groupName: String, pollIntervalMs: Long = 10_000L): Flow<List<GroupMemberLocation>> = flow {
        emit(emptyList()) // Ensure initial emission for combine operator
        while (true) {
            try {
                val membersDto = RetrofitClient.api.getGroupMembers(groupName)
                val members = membersDto.map { it.toDomain() }.distinctBy { it.userId }
                emit(members)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching group members", e)
                // Emit empty list or handle error appropriately. 
                // For now, we might want to keep the last known state or emit empty if it's a hard failure.
                // But to avoid clearing the map on transient errors, we might skip emitting emptyList() here unless necessary.
                // However, if the group is empty, the API returns empty list.
                // Let's emit empty list only if it's a 404 or similar, but for network errors, maybe just log?
                // For simplicity in this migration:
                // emit(emptyList()) 
            }
            delay(pollIntervalMs) // Poll configurable
        }
    }
}
