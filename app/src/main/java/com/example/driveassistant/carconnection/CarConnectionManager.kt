package com.example.driveassistant.carconnection

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LiveData

class CarConnectionManager(
    context: Context
) {

    private val carConnection = CarConnection(context)

    val connectionType: LiveData<Int>
        get() = carConnection.type
}