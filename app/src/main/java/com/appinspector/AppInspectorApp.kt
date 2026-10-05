package com.appinspector

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.appinspector.data.local.AppDatabase
import com.appinspector.data.repository.ErrorRepositoryImpl
import com.appinspector.domain.repository.ErrorRepository
import com.topjohnwu.superuser.Shell

class AppInspectorApp : Application() {

    lateinit var repository: ErrorRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )

        val database = AppDatabase.getInstance(this)
        repository = ErrorRepositoryImpl(database.errorDao())

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Logcat Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running background logcat analyzer"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "App Crash & ANR Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications for detected crashes"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "logcat_service_channel"
        const val CHANNEL_ALERTS_ID = "app_alerts_channel"

        lateinit var instance: AppInspectorApp
            private set
    }
}
