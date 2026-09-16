package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    private const val CHANNEL_ID = "sahayog_coop_channel"
    private const val CHANNEL_NAME = "Sahayog Cooperative Alerts"
    private const val CHANNEL_DESC = "Notifications for booking requests, worker acceptance, proof, and disputes"

    private var channelCreated = false

    fun init(context: Context) {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
        channelCreated = true
    }

    private fun canPostNotification(context: Context): Boolean {
        init(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyNewRequest(
        context: Context,
        service: String,
        amountPaise: Long,
        location: String
    ) {
        val amountRupees = amountPaise / 100
        sendNotification(
            context = context,
            id = 1001,
            title = "📋 New Service Request Available!",
            body = "$service (Offer: ₹$amountRupees) at $location. Tap to view and accept."
        )
    }

    fun notifyWorkerAccepted(
        context: Context,
        workerName: String,
        service: String
    ) {
        sendNotification(
            context = context,
            id = 1002,
            title = "✅ Worker Accepted Your Request!",
            body = "$workerName accepted your $service request. Escrow is safely held."
        )
    }

    fun notifyProofSubmitted(
        context: Context,
        workerName: String,
        service: String
    ) {
        sendNotification(
            context = context,
            id = 1003,
            title = "📸 Completion Proof Submitted",
            body = "$workerName uploaded GPS & photo proof for $service. Awaiting verification."
        )
    }

    fun notifyDisputeCreated(
        context: Context,
        service: String,
        comment: String
    ) {
        sendNotification(
            context = context,
            id = 1004,
            title = "⚠️ Dispute Logged for Review",
            body = "Dispute filed for $service: \"$comment\". Escrow remains strictly secured."
        )
    }

    fun notifyStatusChanged(
        context: Context,
        service: String,
        statusText: String
    ) {
        sendNotification(
            context = context,
            id = 1005,
            title = "Sahayog Status Update",
            body = "$service is now $statusText."
        )
    }

    private fun sendNotification(
        context: Context,
        id: Int,
        title: String,
        body: String
    ) {
        try {
            init(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            if (canPostNotification(context)) {
                notificationManager.notify(id, builder.build())
            }
        } catch (e: Exception) {
            // Graceful fallback if notification permission or manager not available
        }
    }
}
