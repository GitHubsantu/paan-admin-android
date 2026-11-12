package com.paan.safaadmin.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.paan.safaadmin.R

object NotificationChannels {
    // A channel's sound can't be changed after it exists. To change the sound later, use a new id (v2).
    const val NEW_ORDER = "new_orders_v1"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.new_order}")
        // "Ringtone" usage: plays at ringtone volume, like an incoming call.
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(NEW_ORDER, "New orders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings when a customer places an order"
                setSound(sound, attrs)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 900)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            },
        )
    }
}