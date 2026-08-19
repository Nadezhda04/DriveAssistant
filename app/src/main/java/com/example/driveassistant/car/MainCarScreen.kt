package com.example.driveassistant.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class MainCarScreen(
    carContext: CarContext
) : Screen(carContext) {

    override fun onGetTemplate(): Template {

        val row = Row.Builder()
            .setTitle("Hello Drive Assistant!")
            .addText("Android Auto работи 🎉")
            .build()

        val pane = Pane.Builder()
            .addRow(row)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(
                Header.Builder()
                    .setTitle("Drive Assistant")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }
}