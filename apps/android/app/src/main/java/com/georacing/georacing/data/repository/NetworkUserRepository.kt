package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.EnsureColumnRequest
import com.georacing.georacing.data.remote.dto.EnsureTableRequest
import com.georacing.georacing.data.remote.dto.UpsertRequest
import com.georacing.georacing.data.remote.dto.UserRequest
import java.util.zip.CRC32

class NetworkUserRepository {

    companion object {
        private const val TAG = "NetworkUserRepo"
        private const val TABLE = "users"
        private const val MAX_RETRIES = 2
    }

    private var schemaEnsured = false

    private suspend fun ensureSchema() {
        if (schemaEnsured) return
        RetrofitClient.api.ensureTable(EnsureTableRequest(TABLE))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(TABLE, "uid", "VARCHAR(200)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(TABLE, "display_name", "VARCHAR(255)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(TABLE, "email", "VARCHAR(255)"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(TABLE, "photo_url", "TEXT"))
        RetrofitClient.api.ensureColumn(EnsureColumnRequest(TABLE, "last_login", "DOUBLE"))
        schemaEnsured = true
    }

    private fun stableId(value: String): Int {
        val crc = CRC32().apply { update(value.toByteArray()) }.value
        return (crc % Int.MAX_VALUE).toInt()
    }

    suspend fun registerUser(uid: String, name: String?, email: String?, photoUrl: String? = null): Result<Unit> {
        var attempt = 0
        var lastError: Exception? = null

        while (attempt < MAX_RETRIES) {
            try {
                ensureSchema()

                val request = UpsertRequest(
                    table = TABLE,
                    data = mapOf(
                        "id" to java.util.UUID.randomUUID().toString(), // Firebase Auth UUID / DB varchar
                        "uid" to uid,
                        "display_name" to name,
                        "email" to email,
                        "photo_url" to photoUrl,
                        "last_login" to System.currentTimeMillis().toDouble()
                    )
                )
                RetrofitClient.api.upsert(request)
                Log.d(TAG, "User registered/updated via upsert: $uid (attempt ${attempt + 1})")
                return Result.success(Unit)
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
                Log.e(TAG, "Error registering user $uid (attempt ${attempt + 1})", e)
                schemaEnsured = false // force re-ensure next loop
                attempt++
                if (attempt >= MAX_RETRIES) break
                // pequeño backoff
                kotlinx.coroutines.delay(300)
            }
        }

        return Result.failure(lastError ?: Exception("Unknown error registering user $uid"))
    }
}
