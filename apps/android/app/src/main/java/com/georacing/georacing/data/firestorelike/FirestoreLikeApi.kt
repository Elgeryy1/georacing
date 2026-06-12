package com.georacing.georacing.data.firestorelike

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FirestoreLikeApi {

    data class UpsertRequest(
        val table: String,
        val data: Map<String, Any?>
    )

    data class GetRequest(
        val table: String,
        val where: Map<String, Any?>?
    )
    
    data class DeleteRequest(
        val table: String,
        val where: Map<String, Any?>
    )

    // Using Map<String, Any?> for flexibility as requested.
    // Retrofit + Gson will handle map serialization.
    
    @POST("_upsert")
    suspend fun upsert(@Body body: UpsertRequest): Map<String, Any?> 
    // Response type uncertain from spec, usually generic map or success status. 
    // Spec says: "Upsert general ... autogenerar esquema" implies it might return the record/id or just 200.
    // Spec in user request: "suspend fun upsert(...): UpsertResponse". I'll use Any for now or Map.

    @POST("_get")
    suspend fun get(@Body body: GetRequest): List<Map<String, Any?>>

    @GET("_read")
    suspend fun read(@Query("table") table: String): List<Map<String, Any?>>

    @POST("_delete")
    suspend fun delete(@Body body: DeleteRequest): Map<String, Any?>
}
