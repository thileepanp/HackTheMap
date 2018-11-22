/*
 * Copyright (c) 2018. Daimler AG.
 */

package me.noro.hackthetruck.repository.vehicle

import android.content.Context
import com.fleetboard.sdk.lib.android.common.SDKInitializer
import com.fleetboard.sdk.lib.android.common.SDKInitializerException
import com.fleetboard.sdk.lib.android.log.Log
import com.fleetboard.sdk.lib.android.vehicle.VehicleClient
import com.fleetboard.sdk.lib.android.vehicle.VehicleClientException
import com.fleetboard.sdk.lib.vehicle.IVehicleMessage

///
// This repository handles the connection with the vehicle and the messages sent from the vehicle
// Here you have to define how to post the values of your topics of interest to all the subscribed
// activities or classes within this application
///
object VehicleDataRepository : IVehicleDataRepository, IVehicleDataPublisher {
    private const val TAG = "VEHICLE_DATA_REPO"

    override var subscribers: MutableList<IVehicleDataSubscriber> = mutableListOf()

    lateinit var vehicleClientCallback: VehicleClientCallback


    override fun initializeSdk(context: Context): Boolean {

        return try {
            SDKInitializer.INSTANCE.init(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Initialization of the SDK has failed: $e")
            false
        }
    }

    override fun deinitializeSdk() {
        try {
            SDKInitializer.INSTANCE.terminate()
        } catch (e: SDKInitializerException) {
            Log.e(TAG, "Termination of SDK failed: $e")
        }
    }

    override fun connectVehicle(context: Context): Boolean {
        try {
            if (!VehicleClient.INSTANCE.isConnected) {
                VehicleClient.INSTANCE.connect(vehicleClientCallback, context)

                vehicleClientCallback.onNewVehicleMessage = { handleMessage(it) }
            }
        } catch (e: VehicleClientException) {
            Log.e(TAG, "Connect VehicleClient failed: $e")
        }

        return VehicleClient.INSTANCE.isConnected
    }

    override fun disconnectVehicle() {
        try {
            if (VehicleClient.INSTANCE.isConnected) {
                VehicleClient.INSTANCE.disconnect()
            }
        } catch (e: VehicleClientException) {
            Log.e(TAG, "Disconnect VehicleClient failed: $e")
        }
    }

    ///
    // Here you need to define how to handle the incoming messages
    ///
    override fun handleMessage(message: IVehicleMessage) {
        // Handle every new incoming valid message and decide on how to post the data
        // within the application
        // You have to do this for each registered topic separately
        if (message.value != null) {

            when (message.topic) {
                VehicleClientData.topicList[0] -> {
                    postVehicleSpeed(message.valueAsFloat)
                }

                VehicleClientData.topicList[1] -> {
                    postTotalVehicleDistance(message.valueAsLong)
                }
            }
        }
    }

    private fun postVehicleSpeed(speed: Float) {
        subscribers.forEach { it.onVehicleSpeed(speed) }
    }

    private fun postTotalVehicleDistance(totalDistance: Long) {
        subscribers.forEach { it.onTotalVehicleDistance(totalDistance) }
    }

    override fun register(subscriber: IVehicleDataSubscriber) {
        subscribers.add(subscriber)
    }

    override fun remove(subscriber: IVehicleDataSubscriber) {
        subscribers.remove(subscriber)
    }
}
