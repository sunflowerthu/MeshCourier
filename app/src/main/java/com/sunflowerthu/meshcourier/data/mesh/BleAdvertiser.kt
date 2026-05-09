package com.sunflowerthu.meshcourier.data.mesh

import android.Manifest
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import com.sunflowerthu.meshcourier.domain.models.NodeId
import java.nio.ByteBuffer

class BleAdvertiser(
    private val advertiser: BluetoothLeAdvertiser,
    private val nodeId: NodeId
) {

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun start() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(GattProtocol.SERVICE_UUID))
            .addManufacturerData(MANUFACTURER_ID, nodeIdToBytes())
            .build()

        advertiser.startAdvertising(settings, data, callback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stop() {
        advertiser.stopAdvertising(callback)
    }

    private fun nodeIdToBytes(): ByteArray =
        ByteBuffer.allocate(8).putLong(nodeId.value.hashCode().toLong()).array()

    private val callback = object : AdvertiseCallback() {}

    companion object {
        private const val MANUFACTURER_ID = 0x1234
    }
}
