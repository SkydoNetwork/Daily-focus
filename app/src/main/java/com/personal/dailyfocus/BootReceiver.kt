package com.personal.dailyfocus

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val repository = FocusRepository(context.applicationContext)
        if (!repository.isScreenOnDisplayEnabled() || !Settings.canDrawOverlays(context)) {
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return
        }

        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenOnDisplayService::class.java),
            )
        } catch (exception: IllegalStateException) {
            Log.e(TAG, "Could not restore the screen-on display after reboot.", exception)
        } catch (exception: SecurityException) {
            Log.e(TAG, "Android denied the screen-on service after reboot.", exception)
        }
    }

    private companion object {
        const val TAG = "DailyFocusBootReceiver"
    }
}
