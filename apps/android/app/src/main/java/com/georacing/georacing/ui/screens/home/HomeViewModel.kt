package com.georacing.georacing.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.domain.model.AppMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.repository.CircuitStateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

/**
 * News article loaded from the API — mirrors iOS NewsRepository.NewsArticle
 */
data class NewsArticle(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: String,
    val imageUrl: String?,
    val timestamp: Long,
    val category: String = "general"
)

class HomeViewModel(
    circuitStateRepository: CircuitStateRepository,
    private val beaconScanner: com.georacing.georacing.data.ble.BeaconScanner? // 🆕 Optional Injection
) : ViewModel() {

    val circuitState: StateFlow<CircuitState?> = circuitStateRepository.getCircuitState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val appMode: StateFlow<AppMode> = circuitStateRepository.appMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppMode.ONLINE
        )

    // --- News from API (iOS parity: HomeViewModel.fetchData → NewsRepository) ---
    private val _newsItems = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsItems: StateFlow<List<NewsArticle>> = _newsItems.asStateFlow()

    private val _isLoadingNews = MutableStateFlow(false)
    val isLoadingNews: StateFlow<Boolean> = _isLoadingNews.asStateFlow()

    init {
        // 🆕 Pruning Loop (User Requirement: 2-3s loop, 5-10s TTL)
        beaconScanner?.let { scanner ->
            viewModelScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(3000) // Loop every 3s
                    scanner.pruneInactiveDevices(10000) // TTL 10s
                }
            }
        }

        // Load news from API on init
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                val response = FirestoreLikeClient.api.read("news")
                val articles = response.mapNotNull { map ->
                    try {
                        NewsArticle(
                            id = map["id"]?.toString() ?: return@mapNotNull null,
                            title = map["title"]?.toString() ?: "",
                            subtitle = map["subtitle"]?.toString() ?: "",
                            content = map["content"]?.toString() ?: "",
                            imageUrl = map["image_url"]?.toString(),
                            timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L,
                            category = map["category"]?.toString() ?: "general"
                        )
                    } catch (_: Exception) { null }
                }.sortedByDescending { it.timestamp }
                _newsItems.value = articles
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Error loading news: ${e.message}")
                // Fallback: keep empty or show mock
                if (_newsItems.value.isEmpty()) {
                    _newsItems.value = listOf(
                        NewsArticle(
                            id = "welcome",
                            title = "Welcome to GeoRacing",
                            subtitle = "Tu compañero digital para el circuito",
                            content = "Explora todas las funcionalidades de la app",
                            imageUrl = null,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            _isLoadingNews.value = false
        }
    }
}
