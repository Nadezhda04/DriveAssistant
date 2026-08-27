package com.example.driveassistant.trip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.car.app.connection.CarConnection
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.driveassistant.R
import com.example.driveassistant.notification.NotificationHelper
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TripMonitoringService : Service() {

    companion object {
        private const val SERVICE_CHANNEL_ID = "trip_monitoring_service"
        private const val SERVICE_NOTIFICATION_ID = 2001
    }

    private lateinit var carConnection: CarConnection
    private lateinit var notificationHelper: NotificationHelper

    private var wasConnectedToAndroidAuto = false

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var database: AppDatabase

    private var activeTripId: Long? = null

    private val connectionObserver = Observer<Int> { connectionType ->

        when (connectionType) {

            CarConnection.CONNECTION_TYPE_PROJECTION -> {

                if (!wasConnectedToAndroidAuto) {

                    wasConnectedToAndroidAuto = true

                    serviceScope.launch {

                        val existingTrip =
                            database.tripDao().getActiveTrip()

                        if (existingTrip != null) {
                            activeTripId = existingTrip.id
                        } else {

                            activeTripId =
                                database.tripDao().insert(
                                    Trip()
                                )
                        }
                    }
                }
            }

            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> {

                if (wasConnectedToAndroidAuto) {

                    wasConnectedToAndroidAuto = false

                    serviceScope.launch {

                        val trip =
                            database.tripDao().getActiveTrip()

                        if (trip != null) {

                            database.tripDao().update(
                                trip.copy(
                                    endedAt = System.currentTimeMillis()
                                )
                            )

                            val noteCount =
                                database.voiceNoteDao()
                                    .countNotesForTrip(trip.id)

                            notificationHelper
                                .showTripEndedNotification(
                                    noteCount = noteCount
                                )

                            activeTripId = null
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)

        notificationHelper = NotificationHelper(this)

        createServiceNotificationChannel()
        startForegroundServiceNotification()

        carConnection = CarConnection(this)
        carConnection.type.observeForever(connectionObserver)
    }

    override fun onDestroy() {
        carConnection.type.removeObserver(connectionObserver)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createServiceNotificationChannel() {

        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Наблюдение на пътуването",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    private fun startForegroundServiceNotification() {

        val notification =
            NotificationCompat.Builder(
                this,
                SERVICE_CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Drive Assistant")
                .setContentText("Следене на автомобилната връзка")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        startForeground(
            SERVICE_NOTIFICATION_ID,
            notification
        )
    }
}