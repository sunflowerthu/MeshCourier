package com.sunflowerthu.meshcourier.data.mesh

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer
import kotlin.coroutines.resume

/**
 * GATT-клиент: подключается к удалённому узлу и отправляет байты пакета чанками.
 *
 * Протокол фрагментации (соответствует PacketAssembler на стороне получателя):
 *   Чанк 0: [2 байта — общая длина данных][данные, до MAX_CHUNK_SIZE байт]
 *   Чанк 1+: [данные, до DEFAULT_MTU байт]
 */
class BleTransport(private val context: Context) {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun sendPacket(device: BluetoothDevice, packetBytes: ByteArray): Boolean {
        val chunkCount = (packetBytes.size + GattProtocol.DEFAULT_MTU - 1) / GattProtocol.DEFAULT_MTU
        val timeout = CONNECTION_TIMEOUT_MS + chunkCount * CHUNK_TIMEOUT_MS
        return withTimeout(timeout) {
            suspendCancellableCoroutine { cont ->
                val chunks = packetBytes.toChunks()
                var chunkIndex = 0
                var gatt: BluetoothGatt? = null

                val gattCallback = object : BluetoothGattCallback() {

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> g.requestMtu(GattProtocol.DEFAULT_MTU + 3)
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                g.close()
                                if (!cont.isCompleted) cont.resume(false)
                            }
                        }
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                        g.discoverServices()
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            g.disconnect(); return
                        }
                        writeChunk(g, chunks[chunkIndex])
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onCharacteristicWrite(
                        g: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int
                    ) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            g.disconnect(); return
                        }
                        chunkIndex++
                        if (chunkIndex < chunks.size) {
                            writeChunk(g, chunks[chunkIndex])
                        } else {
                            // Все чанки отправлены — успех
                            cont.resume(true)
                            g.disconnect()
                        }
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    private fun writeChunk(g: BluetoothGatt, chunk: ByteArray) {
                        val service = g.getService(GattProtocol.SERVICE_UUID)
                            ?: run  { g.disconnect(); return }
                        val char = service.getCharacteristic(GattProtocol.PACKET_CHAR_UUID)
                            ?: run { g.disconnect(); return }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            g.writeCharacteristic(
                                char,
                                chunk,
                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            char.value = chunk
                            @Suppress("DEPRECATION")
                            g.writeCharacteristic(char)
                        }
                    }
                }

                gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

                cont.invokeOnCancellation {
                    gatt?.disconnect()
                    gatt?.close()
                }
            }
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 8_000L
        private const val CHUNK_TIMEOUT_MS = 80L

        // Разбивает байты пакета на BLE-чанки
        fun ByteArray.toChunks(): List<ByteArray> {
            val chunks = mutableListOf<ByteArray>()

            // Первый чанк: 2 байта длины + данные
            val firstDataSize = minOf(GattProtocol.MAX_CHUNK_SIZE, size)
            val firstChunk = ByteBuffer.allocate(GattProtocol.HEADER_SIZE + firstDataSize)
                .putShort(size.toShort())
                .put(this, 0, firstDataSize)
                .array()
            chunks.add(firstChunk)

            // Остальные чанки: просто данные
            var offset = firstDataSize
            while (offset < size) {
                val end = minOf(offset + GattProtocol.DEFAULT_MTU, size)
                chunks.add(copyOfRange(offset, end))
                offset = end
            }

            return chunks
        }
    }
}
