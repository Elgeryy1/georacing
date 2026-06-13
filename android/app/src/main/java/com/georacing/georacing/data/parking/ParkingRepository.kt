package com.georacing.georacing.data.parking

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.parkingDataStore: DataStore<Preferences> by preferencesDataStore(name = "parking_prefs")

data class ParkingLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val photoUri: String?
)

class ParkingRepository(private val context: Context) {

    val parkingLocation: Flow<ParkingLocation?> = context.parkingDataStore.data.map { prefs ->
        val lat = prefs[PARKING_LAT]
        val lng = prefs[PARKING_LNG]
        val time = prefs[PARKING_TIMESTAMP]
        val photo = prefs[PARKING_PHOTO_URI]

        if (lat != null && lng != null && time != null) {
            ParkingLocation(lat, lng, time, photo)
        } else {
            null
        }
    }

    suspend fun saveParkingLocation(location: ParkingLocation) {
        context.parkingDataStore.edit { prefs ->
            prefs[PARKING_LAT] = location.latitude
            prefs[PARKING_LNG] = location.longitude
            prefs[PARKING_TIMESTAMP] = location.timestamp
            if (location.photoUri != null) {
                prefs[PARKING_PHOTO_URI] = location.photoUri
            } else {
                prefs.remove(PARKING_PHOTO_URI)
            }
        }
    }

    suspend fun clearParking() {
        context.parkingDataStore.edit { prefs ->
            prefs.remove(PARKING_LAT)
            prefs.remove(PARKING_LNG)
            prefs.remove(PARKING_TIMESTAMP)
            prefs.remove(PARKING_PHOTO_URI)
        }
    }

    companion object {
        val PARKING_LAT = doublePreferencesKey("parking_lat")
        val PARKING_LNG = doublePreferencesKey("parking_lng")
        val PARKING_TIMESTAMP = longPreferencesKey("parking_timestamp")
        val PARKING_PHOTO_URI = stringPreferencesKey("parking_photo_uri")
    }
}
