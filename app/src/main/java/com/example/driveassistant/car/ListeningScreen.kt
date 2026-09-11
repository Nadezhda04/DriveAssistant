package com.example.driveassistant.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class ListeningScreen(
    carContext: CarContext
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder(
            "Говорете сега..."
        )
            .setTitle("Слушам")
            .build()
    }
}