package com.example.driveassistant.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator

class DriveCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        return DriveSession()
    }
}

class DriveSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return MainCarScreen(carContext)
    }
}