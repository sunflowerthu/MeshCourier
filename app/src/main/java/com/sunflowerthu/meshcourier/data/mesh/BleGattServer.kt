package com.sunflowerthu.meshcourier.data.mesh

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import com.sunflowerthu.meshcourier.domain.protocol.PacketDecoder
import com.sunflowerthu.meshcourier.domain.mesh.PacketAction
import com.sunflowerthu.meshcourier.domain.mesh.PacketProcessor
import java.util.concurrent.ConcurrentHashMap

/**
 * Принимает входящие BLE-пакеты от других узлов.
 */
class BleGattServer(
    private val packetProcessor: PacketProcessor,
    private val onAction: (PacketAction) -> Unit
) {
    private val assemblers = ConcurrentHashMap<String, PacketAssembler>()

    private lateinit var gattServer: BluetoothGattServer

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun init(server: BluetoothGattServer) {
        gattServer = server

        val characteristic = BluetoothGattCharacteristic(
            GattProtocol.PACKET_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val service = BluetoothGattService(
            GattProtocol.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).also { it.addCharacteristic(characteristic) }

        gattServer.addService(service)
    }

    val callback = object : BluetoothGattServerCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid != GattProtocol.PACKET_CHAR_UUID) return

            val assembler = assemblers.getOrPut(device.address) { PacketAssembler() }
            val complete = assembler.append(value)

            if (complete != null) {
                assemblers.remove(device.address)
                runCatching { PacketDecoder.decode(complete) }
                    .onSuccess { packet ->
                        val action = when (val a = packetProcessor.process(packet)) {
                            is PacketAction.Deliver -> a.copy(fromAddress = device.address)
                            is PacketAction.Forward -> a.copy(fromAddress = device.address)
                            PacketAction.Drop -> a
                        }
                        onAction(action)
                    }
            }

            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                assemblers.remove(device.address)
            }
        }
    }
}
