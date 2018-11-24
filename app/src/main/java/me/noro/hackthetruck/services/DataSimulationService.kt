/*
 * Copyright (c) 2018. Daimler AG.
 */

package me.noro.hackthetruck.services

import android.os.Handler
import me.noro.hackthetruck.repository.vehicle.VehicleDataRepository.handleMessage
import com.fleetboard.sdk.lib.vehicle.ValidState
import com.fleetboard.sdk.lib.vehicle.VehicleMessage
import com.fleetboard.sdk.lib.vehicle.VehicleTopicConsts

///
// Helper service for simulating incoming data from the FMS interface or CAN bus
///
object DataSimulationService {

    // Define a set of messages
    var messages: Array<VehicleMessage> = arrayOf(
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 9.3f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.CURRENT_FUEL_CONSUMPTION, 26.2f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 25.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 45.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 65.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 85.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.CURRENT_FUEL_CONSUMPTION, 36.7f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 91.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.TOTAL_VEHICLE_DISTANCE, 134586L, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 93.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.CURRENT_FUEL_CONSUMPTION, 46.3f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 95.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.CURRENT_FUEL_CONSUMPTION, 47.1f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 87.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.VEHICLE_SPEED, 90.4f, ValidState.VALID),
            VehicleMessage(VehicleTopicConsts.CURRENT_FUEL_CONSUMPTION, 49.5f, ValidState.VALID))


    init {
        getDataFromSensor()
    }

    // Simulate data coming from the sensors every time interval [2sec]
    private fun getDataFromSensor() {
        val handler = Handler()
        var element = 0

        val runnable = object : Runnable {
            override fun run() {
                if (element < messages.size) handleMessage(messages[element])

                element++
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(runnable, 2000)
    }
}
