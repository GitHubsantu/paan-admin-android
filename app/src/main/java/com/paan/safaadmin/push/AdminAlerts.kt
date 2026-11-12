package com.paan.safaadmin.push

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import com.paan.safaadmin.MainActivity
import com.paan.safaadmin.R

data class NewOrderAlert(
    val orderId: String,
    val orderNumber: String,
    val token: String,
    val itemsCount: String,
    val total: String,
    val customer: String,
    val summary: String,
    val title: String,
    val body: String,
) {
    fun toBundle(): Bundle = bundleOf(
        "order_id" to orderId, "order_number" to orderNumber, "token" to token, "items_count" to itemsCount,
        "total" to total, "customer_name" to customer, "summary" to summary, "title" to title, "body" to body,
    )

    companion object {
        private val KEYS = listOf(
            "order_id", "order_number", "token", "items_count", "total", "customer_name", "summary", "title", "body",
        )

        fun from(d: Map<String, String>) = NewOrderAlert(
            orderId = d["order_id"].orEmpty(),
            orderNumber = d["order_number"] ?: "New order",
            token = d["token"].orEmpty(),
            itemsCount = d["items_count"] ?: "?",
            total = d["total"] ?: "?",
            customer = d["customer_name"] ?: "Customer",
            summary = d["summary"].orEmpty(),
            title = d["title"] ?: "New order",
            body = d["body"].orEmpty(),
        )

        fun from(intent: Intent) =
            from(KEYS.mapNotNull { k -> intent.getStringExtra(k)?.let { k to it } }.toMap())

        fun sample() = NewOrderAlert(
            orderId = "test", orderNumber = "PAAN-TEST", token = "47", itemsCount = "3", total = "160",
            customer = "Test customer",
            summary = "2 x Shahi Meetha Paan, 1 x Belgian Dark Chocolate Paan",
            title = "New order PAAN-TEST", body = "3 items - Rs 160 - Test customer",
        )
    }
}

object AdminAlerts {

    fun openOrderIntent(context: Context, orderId: String) =
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_ORDER_ID, orderId)
        }

    /**
     * Rings with the custom sound. On a locked/off screen it opens the full-screen alert like a call;
     * on an unlocked phone Android shows a heads-up banner instead. The sound repeats until the order is opened,
     * dismissed, or 60 seconds pass.
     */
    @SuppressLint("MissingPermission") // skipped by the system when the permission is off
    fun showNewOrder(context: Context, a: NewOrderAlert) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val id = a.orderId.hashCode()
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val fullScreen = PendingIntent.getActivity(
            context, id,
            Intent(context, NewOrderAlertActivity::class.java).apply {
                putExtras(a.toBundle())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            flags,
        )
        val open = PendingIntent.getActivity(context, id + 1, openOrderIntent(context, a.orderId), flags)

        val notification = NotificationCompat.Builder(context, NotificationChannels.NEW_ORDER)
            .setSmallIcon(R.drawable.ic_stat_order)
            .setContentTitle(a.title)
            .setContentText(a.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.summary.ifBlank { a.body }))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(open)
            .addAction(0, "View order", open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(60_000)
            .build()
            .apply { this.flags = this.flags or Notification.FLAG_INSISTENT }   // repeat the sound

        manager.notify(id, notification)
    }
}