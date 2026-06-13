package com.georacing.georacing.services

import android.content.Context
import android.widget.RemoteViews
import com.georacing.georacing.R

/**
 * Builds the RemoteViews for the Live Activity Notification.
 * Switched to XML RemoteViews for maximum stability and compatibility.
 */
object LiveStatusNotificationBuilder {

    fun createRemoteViews(context: Context, status: String, distance: String, time: String): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_live_status)
        
        remoteViews.setTextViewText(R.id.tvStatus, status)
        remoteViews.setTextViewText(R.id.tvDistance, distance)
        remoteViews.setTextViewText(R.id.tvTime, time)
        
        // Dynamic Progress (Simulated based on text)
        // In real app, pass progress int
        remoteViews.setProgressBar(R.id.progressBar, 100, 75, false)

        return remoteViews
    }
}
