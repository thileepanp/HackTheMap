/*
 * Copyright (c) 2018. Daimler AG.
 */

package me.noro.hackthetruck.repository.vehicle

import com.fleetboard.sdk.lib.android.log.Log
import com.fleetboard.sdk.lib.android.vehicle.IVehicleClientCallback
import com.fleetboard.sdk.lib.android.vehicle.VehicleClient
import com.fleetboard.sdk.lib.android.vehicle.VehicleClientException
import com.fleetboard.sdk.lib.vehicle.IVehicleError
import com.fleetboard.sdk.lib.vehicle.IVehicleMessage
import com.fleetboard.sdk.lib.vehicle.ValidState

///
// Nothing needs to be changed here
///
class VehicleClientCallback : IVehicleClientCallback {

    companion object {
        private const val TAG = "FB_VEHICLE_CLIENT"
    }

    var onNewVehicleMessage: ((IVehicleMessage) -> Unit)? = null


    override fun handleVehicleMessage(p0: IVehicleMessage?) {
        Log.i(TAG, "Handling vehicle message with processor: $p0")

        if (ValidState.VALID == p0!!.validState) {
            onNewVehicleMessage!!.invoke(p0)
        } else {
            Log.e(TAG, "Message not valid: $p0")
        }
    }

    override fun onVehicleClientConnected() {
        Log.i(TAG, "onVehicleClientConnected: Registering for topics")

        try {
            VehicleClient.INSTANCE.registerForTopics(*VehicleClientData.topicList)
        } catch (e: VehicleClientException) {
            Log.e(TAG, "onVehicleClientConnected: Registering topics failed with $e")
        }
    }

    override fun onVehicleClientDisconnected() {
        Log.i(TAG, "onVehicleClientDisconnected: Deregister from all topics")

        try {
            VehicleClient.INSTANCE.unregisterFromAllTopics()
        } catch (e: VehicleClientException) {
            Log.e(TAG, "onVehicleClientDisconnected: Deregister from all topics failed with $e")
        }
    }

    override fun onError(p0: IVehicleError?) {
        Log.e(TAG, "An error occurred: ${p0!!.errorMessage}")
    }
}
