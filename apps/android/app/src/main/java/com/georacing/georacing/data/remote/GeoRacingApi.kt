package com.georacing.georacing.data.remote

import com.georacing.georacing.data.remote.dto.BeaconConfigDto
import com.georacing.georacing.data.remote.dto.CircuitStateDto
import com.georacing.georacing.data.remote.dto.IncidentReportDto
import com.georacing.georacing.data.remote.dto.PoiDto
import com.georacing.georacing.data.remote.dto.EnsureColumnRequest
import com.georacing.georacing.data.remote.dto.EnsureTableRequest
import com.georacing.georacing.data.remote.dto.GroupCreateRequest
import com.georacing.georacing.data.remote.dto.UpsertRequest
import com.georacing.georacing.data.remote.dto.UserRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Query

interface GeoRacingApi {
    @GET("beacons")
    suspend fun getBeacons(): List<BeaconConfigDto>

    @GET("pois")
    suspend fun getPois(): List<PoiDto>

    @GET("state")
    suspend fun getCircuitState(): CircuitStateDto

    @POST("incidents")
    suspend fun sendIncident(@Body incident: IncidentReportDto)

    @POST("group-gps")
    suspend fun insertGroupLocation(@Body request: com.georacing.georacing.data.remote.dto.GroupLocationRequest)

    @GET("group-gps/{groupName}")
    suspend fun getGroupMembers(@retrofit2.http.Path("groupName") groupName: String): List<com.georacing.georacing.data.remote.dto.GroupMemberDto>

    @POST("users")
    suspend fun createUser(@Body request: UserRequest)

    @POST("groups")
    suspend fun createGroup(@Body request: GroupCreateRequest)

    // Schema helpers
    @POST("_ensure_table")
    suspend fun ensureTable(@Body request: EnsureTableRequest)

    @POST("_ensure_column")
    suspend fun ensureColumn(@Body request: EnsureColumnRequest)

    @POST("_upsert")
    suspend fun upsert(@Body request: UpsertRequest)

    @GET("_read")
    suspend fun readTable(@Query("table") table: String): List<Map<String, Any>>
}
