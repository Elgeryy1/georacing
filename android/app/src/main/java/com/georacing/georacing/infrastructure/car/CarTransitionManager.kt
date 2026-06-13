package com.georacing.georacing.infrastructure.car

import android.content.Context
import android.util.Log
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.georacing.georacing.debug.ScenarioSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class CarConnectionState {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
}

class CarTransitionManager(private val context: Context) : DefaultLifecycleObserver {

    private val _carConnectionState = MutableStateFlow(CarConnectionState.UNKNOWN)
    val carConnectionState: StateFlow<CarConnectionState> = _carConnectionState.asStateFlow()

    private val _realConnectionState = MutableStateFlow(CarConnectionState.UNKNOWN)

    private var carConnection: LiveData<Int>? = null

    // Callback for the critical transition event
    var onParkingTransitionDetected: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var monitoringJob: Job? = null

    private val connectionObserver = Observer<Int> { connectionState ->
        val newState = when (connectionState) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> CarConnectionState.CONNECTED
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> CarConnectionState.DISCONNECTED
            CarConnection.CONNECTION_TYPE_NATIVE -> CarConnectionState.CONNECTED // Assuming native is also connected
            else -> CarConnectionState.UNKNOWN
        }
        _realConnectionState.value = newState
    }

    fun startMonitoring(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
        try {
            carConnection = CarConnection(context).type
            carConnection?.observe(lifecycleOwner, connectionObserver)
        } catch (e: Exception) {
            Log.e("CarTransitionManager", "Error initializing CarConnection", e)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            combine(
                _realConnectionState,
                ScenarioSimulator.forcedCarConnection
            ) { real, forced ->
                if (forced != null) {
                    if (forced) CarConnectionState.CONNECTED else CarConnectionState.DISCONNECTED
                } else {
                    real
                }
            }.collect { newState ->
                val oldState = _carConnectionState.value
                if (oldState != newState) {
                    _carConnectionState.value = newState
                    Log.d("CarTransitionManager", "Car Config Changed: $oldState -> $newState")

                    // CRITICAL LOGIC: Transition from CONNECTED -> DISCONNECTED
                    if (oldState == CarConnectionState.CONNECTED && newState == CarConnectionState.DISCONNECTED) {
                        Log.i("CarTransitionManager", "Parking Transition Detected!")
                        onParkingTransitionDetected?.invoke()
                    }
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        monitoringJob?.cancel()
        super.onStop(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        carConnection?.removeObserver(connectionObserver)
        super.onDestroy(owner)
    }

    companion object {
        @Volatile
        private var INSTANCE: CarTransitionManager? = null

        fun getInstance(context: Context): CarTransitionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CarTransitionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
