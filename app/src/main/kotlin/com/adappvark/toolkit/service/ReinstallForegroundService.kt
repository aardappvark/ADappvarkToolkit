package com.adappvark.toolkit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.adappvark.toolkit.MainActivity
import com.adappvark.toolkit.R

/**
 * Foreground service that keeps ADappvark in a foreground process state
 * during bulk reinstall operations.
 *
 * Android 14+ (API 34) blocks background activity launches (BAL).
 * When ADappvark opens the dApp Store and goes to background, it can no longer
 * launch activities (neither bringAppToForeground nor openDAppStore deep links).
 * Running a foreground service keeps the app's process in a foreground state,
 * allowing it to launch activities even when the Activity isn't visible.
 */
class ReinstallForegroundService : Service() {

    companion object {
        private const val TAG = "ReinstallFgService"
        private const val CHANNEL_ID = "reinstall_channel"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            val intent = Intent(context, ReinstallForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Foreground service start requested")
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReinstallForegroundService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Foreground service stop requested")
        }

        fun updateProgress(context: Context, current: Int, total: Int, appName: String) {
            val intent = Intent(context, ReinstallForegroundService::class.java).apply {
                putExtra("action", "update")
                putExtra("current", current)
                putExtra("total", total)
                putExtra("appName", appName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification("Preparing reinstall...", 0, 0)
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        if (action == "update") {
            val current = intent.getIntExtra("current", 0)
            val total = intent.getIntExtra("total", 0)
            val appName = intent.getStringExtra("appName") ?: ""
            val notification = buildNotification(
                "Reinstalling $current of $total: $appName",
                current,
                total
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Foreground service destroyed")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reinstall Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during bulk app reinstall"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, current: Int, total: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AardAppvark Reinstall")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (total > 0) {
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }
}
