package com.example.woundscanner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import com.example.woundscanner.data.WoundDatabase
import com.example.woundscanner.workers.FollowUpReminderWorker
import java.util.concurrent.TimeUnit

class WoundApplication : Application() {

    // Database instance
    val database: WoundDatabase by lazy {
        WoundDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize notification channels
        createNotificationChannels()

        // Schedule background workers
        setupBackgroundWork()

        // Initialize crash reporting (if needed)
        setupCrashReporting()

        // Initialize analytics (if needed)
        setupAnalytics()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Follow-up reminders channel
            val followUpChannel = NotificationChannel(
                CHANNEL_FOLLOW_UP,
                getString(R.string.notification_followup_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_followup_text, "wound")
                enableLights(true)
                enableVibration(true)
            }

            // Treatment reminders channel
            val treatmentChannel = NotificationChannel(
                CHANNEL_TREATMENT,
                getString(R.string.notification_treatment_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_treatment_text, "wound")
                enableLights(true)
                enableVibration(true)
            }

            // Register the channels
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(followUpChannel, treatmentChannel))
        }
    }

    private fun setupBackgroundWork() {
        // Set up follow-up reminder worker
        val followUpWorkRequest = PeriodicWorkRequestBuilder<FollowUpReminderWorker>(
            1, TimeUnit.DAYS,
            15, TimeUnit.MINUTES // Flex period
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_FOLLOW_UP_REMINDERS,
            ExistingPeriodicWorkPolicy.KEEP,
            followUpWorkRequest
        )
    }

    private fun setupCrashReporting() {
        // Initialize crash reporting service (e.g., Firebase Crashlytics)
        // Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun setupAnalytics() {
        // Initialize analytics service (e.g., Firebase Analytics)
        // Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    companion object {
        private var instance: WoundApplication? = null

        fun getInstance(): WoundApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }

        // Notification channel IDs
        const val CHANNEL_FOLLOW_UP = "follow_up_reminders"
        const val CHANNEL_TREATMENT = "treatment_reminders"

        // Work manager unique work names
        const val WORK_FOLLOW_UP_REMINDERS = "follow_up_reminders"

        // Shared Preferences
        const val PREFS_NAME = "wound_scanner_prefs"
        const val PREF_FIRST_LAUNCH = "first_launch"
        const val PREF_LAST_SYNC = "last_sync"
        const val PREF_USER_ID = "user_id"

        // Feature flags
        var ENABLE_ML_FEATURES = true
        var ENABLE_CLOUD_SYNC = false
        var ENABLE_ANALYTICS = true

        // Cache configuration
        const val MAX_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
        const val CACHE_EXPIRY_DAYS = 7

        // Image configuration
        const val MAX_IMAGE_SIZE = 1920
        const val IMAGE_QUALITY = 90
        const val MAX_STORED_IMAGES = 1000

        // Database configuration
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "wound_scanner.db"

        // API configuration
        const val API_TIMEOUT = 30L // seconds
        const val API_RETRY_COUNT = 3
    }

    // Extension function to get shared preferences
    fun getAppPreferences(): android.content.SharedPreferences {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Extension function to check if it's first launch
    fun isFirstLaunch(): Boolean {
        return getAppPreferences().getBoolean(PREF_FIRST_LAUNCH, true)
    }

    // Extension function to mark first launch complete
    fun markFirstLaunchComplete() {
        getAppPreferences().edit().putBoolean(PREF_FIRST_LAUNCH, false).apply()
    }

    // Extension function to get/set last sync time
    var lastSyncTime: Long
        get() = getAppPreferences().getLong(PREF_LAST_SYNC, 0)
        set(value) = getAppPreferences().edit().putLong(PREF_LAST_SYNC, value).apply()

    // Extension function to get/set user ID
    var userId: String?
        get() = getAppPreferences().getString(PREF_USER_ID, null)
        set(value) = getAppPreferences().edit().putString(PREF_USER_ID, value).apply()

    // Extension function to clear app data
    fun clearAppData() {
        // Clear preferences
        getAppPreferences().edit().clear().apply()

        // Clear database
        WorkManager.getInstance(this).cancelAllWork()

        // Clear cached images
        clearImageCache()

        // Reset feature flags
        ENABLE_ML_FEATURES = true
        ENABLE_CLOUD_SYNC = false
        ENABLE_ANALYTICS = true
    }

    private fun clearImageCache() {
        val cacheDir = cacheDir
        cacheDir.deleteRecursively()
    }
}
