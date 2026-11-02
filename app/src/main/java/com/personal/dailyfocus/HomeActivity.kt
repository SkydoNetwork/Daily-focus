package com.personal.dailyfocus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class HomeActivity : AppCompatActivity() {
    private lateinit var repository: FocusRepository

    private val overlaySettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                continueScreenOnSetup()
            } else {
                Toast.makeText(this, R.string.screen_on_overlay_permission, Toast.LENGTH_LONG).show()
                refreshActivities()
            }
        }
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableScreenOnDisplay()
            } else {
                Toast.makeText(this, R.string.screen_on_notification_permission, Toast.LENGTH_LONG).show()
                refreshActivities()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FocusRepository(applicationContext)
        setContentView(R.layout.activity_home)

        findViewById<Button>(R.id.editActivitiesButton).setOnClickListener {
            startActivity(Intent(this, EditPrioritiesActivity::class.java))
        }
        findViewById<Button>(R.id.openDailyDisplayButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.savedWordsButton).setOnClickListener {
            openVocabularySection(VocabularyLibraryActivity.MODE_SAVED)
        }
        findViewById<Button>(R.id.vocabularyHistoryButton).setOnClickListener {
            openVocabularySection(VocabularyLibraryActivity.MODE_HISTORY)
        }
        findViewById<Button>(R.id.wordReviewButton).setOnClickListener {
            openVocabularySection(VocabularyLibraryActivity.MODE_REVIEW)
        }
        findViewById<SwitchMaterial>(R.id.lockScreenFocusSwitch)
            .setOnCheckedChangeListener { _, enabled ->
                toggleScreenOnDisplay(enabled)
            }
    }

    private fun openVocabularySection(mode: String) {
        startActivity(
            Intent(this, VocabularyLibraryActivity::class.java)
                .putExtra(VocabularyLibraryActivity.EXTRA_MODE, mode),
        )
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            refreshActivities()
        }
    }

    private fun refreshActivities() {
        findViewById<TextView>(R.id.homeReading).text =
            repository.getPriorityDescription("reading")
        findViewById<TextView>(R.id.homeTrading).text =
            repository.getPriorityDescription("trading")
        findViewById<TextView>(R.id.homeCoding).text =
            repository.getPriorityDescription("coding")
        findViewById<TextView>(R.id.homeDesigning).text =
            repository.getPriorityDescription("designing")

        val focusSwitch = findViewById<SwitchMaterial>(R.id.lockScreenFocusSwitch)
        val status = findViewById<TextView>(R.id.screenOnStatus)
        val supported = Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
        if (
            repository.isScreenOnDisplayEnabled() &&
            (!supported || !hasScreenOnPermissions())
        ) {
            stopService(Intent(this, ScreenOnDisplayService::class.java))
            repository.setScreenOnDisplayEnabled(false)
        }
        if (!supported) {
            findViewById<View>(R.id.screenOnSetupCard).visibility = View.GONE
        } else {
            findViewById<View>(R.id.screenOnSetupCard).visibility = View.VISIBLE
            val enabled = repository.isScreenOnDisplayEnabled()
            focusSwitch.isChecked = enabled
            status.setText(if (enabled) R.string.screen_on_enabled else R.string.screen_on_disabled)
        }
    }

    private fun toggleScreenOnDisplay(enabled: Boolean) {
        if (!enabled) {
            if (!repository.isScreenOnDisplayEnabled()) {
                return
            }
            if (repository.setScreenOnDisplayEnabled(false)) {
                stopService(Intent(this, ScreenOnDisplayService::class.java))
                refreshActivities()
            } else {
                showSetupError()
            }
            return
        }

        if (repository.isScreenOnDisplayEnabled()) {
            return
        }
        continueScreenOnSetup()
    }

    private fun continueScreenOnSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            overlaySettingsLauncher.launch(settingsIntent)
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        enableScreenOnDisplay()
    }

    private fun enableScreenOnDisplay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.screen_on_overlay_permission, Toast.LENGTH_LONG).show()
            return
        }
        if (!repository.setScreenOnDisplayEnabled(true)) {
            showSetupError()
            return
        }
        try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, ScreenOnDisplayService::class.java),
            )
            refreshActivities()
        } catch (exception: SecurityException) {
            repository.setScreenOnDisplayEnabled(false)
            showSetupError()
        } catch (exception: IllegalStateException) {
            repository.setScreenOnDisplayEnabled(false)
            showSetupError()
        }
    }

    private fun showSetupError() {
        Toast.makeText(this, R.string.screen_on_setup_failed, Toast.LENGTH_LONG).show()
        refreshActivities()
    }

    private fun hasScreenOnPermissions(): Boolean {
        return Settings.canDrawOverlays(this) &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                )
    }
}
