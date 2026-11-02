package com.personal.dailyfocus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ScreenOnDisplayService : Service() {
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FocusRepository(applicationContext).isScreenOnDisplayEnabled()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (!hasRequiredPermissions()) {
            FocusRepository(applicationContext).setScreenOnDisplayEnabled(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (screenReceiver == null) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_SCREEN_ON) {
                        showDailyDisplayIfNeeded()
                    }
                }
            }
            screenReceiver = receiver
            ContextCompat.registerReceiver(
                this,
                receiver,
                IntentFilter(Intent.ACTION_SCREEN_ON),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        screenReceiver?.let(::unregisterReceiver)
        screenReceiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasRequiredPermissions(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun showDailyDisplayIfNeeded() {
        val repository = FocusRepository(applicationContext)
        if (
            !repository.isScreenOnDisplayEnabled() ||
            !repository.shouldOpenOnUnlockToday()
        ) {
            return
        }

        val displayIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(displayIntent)
        } catch (exception: SecurityException) {
            Log.e(TAG, "Android denied the daily display launch after screen-on.", exception)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.screen_on_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.screen_on_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val homeIntent = PendingIntent.getActivity(
            this,
            HOME_REQUEST_CODE,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.screen_on_notification_title))
            .setContentText(getString(R.string.screen_on_notification_text))
            .setContentIntent(homeIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "daily_focus_screen_on"
        const val NOTIFICATION_ID = 301
        const val HOME_REQUEST_CODE = 302
        const val TAG = "ScreenOnDisplayService"
    }
}
