package com.personal.dailyfocus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_USER_PRESENT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return
        }

        val repository = FocusRepository(context.applicationContext)
        if (
            repository.isScreenOnDisplayEnabled() &&
            repository.shouldOpenOnUnlockToday()
        ) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(launchIntent)
        }
    }
}
