package com.georacing.georacing.domain.usecases

import com.georacing.georacing.domain.model.DashboardLayout
import com.georacing.georacing.domain.model.Interest
import com.georacing.georacing.domain.model.OnboardingAnswers
import com.georacing.georacing.domain.model.TransportMethod
import com.georacing.georacing.domain.model.UserType
import com.georacing.georacing.domain.model.WidgetType

class ProfileGenerator {

    fun generateDefaultLayout(answers: OnboardingAnswers): DashboardLayout {
        val widgets = mutableListOf<WidgetType>()

        // Core widgets
        widgets.add(WidgetType.METEOROLOGY)
        widgets.add(WidgetType.STATUS_CARD)

        // Interest logic - Tech
        if (answers.interests.contains(Interest.TECH)) {
            widgets.add(WidgetType.AR_ACCESS)
        }

        // Logic based on Transport
        if (answers.transportMethod == TransportMethod.CAR) {
            widgets.add(WidgetType.PARKING_INFO)
        }

        // Logic based on User Type
        when (answers.userType) {
            UserType.FAMILY -> {
                widgets.add(WidgetType.FIND_RESTROOMS)
                widgets.add(WidgetType.FOOD_OFFERS)
            }
            UserType.STAFF -> {
                widgets.add(WidgetType.STAFF_ACTIONS)
            }
            UserType.VIP -> {
                widgets.add(WidgetType.ACTIONS_GRID) // Priority actions for VIP
                if (!widgets.contains(WidgetType.FOOD_OFFERS)) {
                    widgets.add(WidgetType.FOOD_OFFERS)
                }
            }
            else -> { // FAN
                 widgets.add(WidgetType.ACTIONS_GRID)
            }
        }
        
        // Secondary interest logic
        if (answers.interests.contains(Interest.TECH) && !widgets.contains(WidgetType.ECO_METER)) {
             widgets.add(WidgetType.ECO_METER)
        }
        
        if (answers.interests.contains(Interest.FOOD) && !widgets.contains(WidgetType.FOOD_OFFERS)) {
            widgets.add(WidgetType.FOOD_OFFERS)
        }

        // Add news feed at the end usually
        widgets.add(WidgetType.NEWS_FEED)

        return DashboardLayout(widgets.distinct())
    }
}
