package com.georacing.georacing.domain.model

enum class WidgetType {
    CONTEXTUAL_CARD, // Smart card: emergency/flag/offline/route/status (iOS parity)
    STATUS_CARD,
    ACTIONS_GRID,
    ECO_METER,
    NEWS_FEED,
    STAFF_ACTIONS, // Protected
    PARKING_INFO, // Status of parked car
    METEOROLOGY,
    AR_ACCESS,
    FIND_RESTROOMS,
    FOOD_OFFERS,
    ACHIEVEMENTS, // Gamification - Fan level & badges
    SEARCH_ACCESS, // Quick search bar
    CLICK_COLLECT, // Food stand ordering
    WRAPPED, // Post-event stats summary
    COLLECTIBLES, // Digital collectible cards
    PROXIMITY_CHAT // BLE-based nearby chat
}

data class DashboardLayout(
    val widgets: List<WidgetType>
) {
    companion object {
        val DEFAULT = DashboardLayout(
            listOf(
                WidgetType.METEOROLOGY,
                WidgetType.CONTEXTUAL_CARD,
                WidgetType.ACTIONS_GRID,
                WidgetType.ACHIEVEMENTS,
                WidgetType.CLICK_COLLECT,
                WidgetType.NEWS_FEED,
                WidgetType.PROXIMITY_CHAT,
                WidgetType.PARKING_INFO
            )
        )
    }
}
