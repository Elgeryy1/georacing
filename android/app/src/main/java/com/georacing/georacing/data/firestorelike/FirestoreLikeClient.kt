package com.georacing.georacing.data.firestorelike

import com.georacing.georacing.data.remote.ApiClient

object FirestoreLikeClient {
    val api: FirestoreLikeApi by lazy {
        ApiClient.retrofit.create(FirestoreLikeApi::class.java)
    }
}
