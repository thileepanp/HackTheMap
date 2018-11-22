/*
 * Copyright (c) 2018. Daimler AG.
 */

package me.noro.hackthetruck

import me.noro.hackthetruck.repository.vehicle.VehicleClientCallback
import me.noro.hackthetruck.repository.vehicle.VehicleDataRepository
import me.noro.hackthetruck.services.DataSimulationService


///
// Bundle all your new repositories, services and activities together for
// easy dependency injection
///
object Injector {

    private fun vehicleDataRepository(): VehicleDataRepository {
        val fleetboardVehicleDataRepository = VehicleDataRepository
        fleetboardVehicleDataRepository.vehicleClientCallback = Injector.vehicleClientCallback()

        return fleetboardVehicleDataRepository
    }

    private fun vehicleClientCallback(): VehicleClientCallback {
        return VehicleClientCallback()
    }

    private fun dataSimulationService(): DataSimulationService {
        return DataSimulationService
    }

    fun inject(mainActivity: MainActivity) {
        mainActivity.vehicleDataRepository = Injector.vehicleDataRepository()
        mainActivity.dataSimulationService = Injector.dataSimulationService()
    }
}
