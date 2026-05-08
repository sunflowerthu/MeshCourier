package com.sunflowerthu.meshcourier.data.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sunflowerthu.meshcourier.MainActivity
import com.sunflowerthu.meshcourier.R
import com.sunflowerthu.meshcourier.domain.protocol.PacketEncoder
import com.sunflowerthu.meshcourier.domain.mesh.PacketAction
import com.sunflowerthu.meshcourier.domain.mesh.PacketProcessor
import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import com.sunflowerthu.meshcourier.domain.repository.NearbyNodesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import com.sunflowerthu.meshcourier.domain.usecase.ReceiveMessageUseCase
import com.sunflowerthu.meshcourier.domain.usecase.SendHelloUseCase
import com.sunflowerthu.meshcourier.domain.usecase.SendMessageUseCase
import dagger.hilt.android.AndroidEntryPoint
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class BleMeshService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var packetQueue: PacketQueue
    @Inject lateinit var receiveMessageUseCase: ReceiveMessageUseCase
    @Inject lateinit var sendHelloUseCase: SendHelloUseCase
    @Inject lateinit var nodeIdentity: NodeIdentity
    @Inject lateinit var messageRepository: MessageRepository
    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var sendMessageUseCase: SendMessageUseCase
    @Inject lateinit var nearbyNodesRepository: NearbyNodesRepository

    private lateinit var packetProcessor: PacketProcessor
    private var bleAdvertiser: BleAdvertiser? = null
    private var bleGattServer: BleGattServer? = null
    private var bleTransport: BleTransport? = null
    @Volatile private var bleScanner: BleScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var btAdapter: BluetoothAdapter? = null

    private val helloSentTo = ConcurrentHashMap<String, Long>()
    private val packetRetries = ConcurrentHashMap<java.util.UUID, Int>()

    private val recentDevices = ConcurrentHashMap<String, Long>()

    @Volatile private var lastNodeSeenAt = System.currentTimeMillis()
    @Volatile private var isLowPowerMode = false

    @Volatile private var burstUntil = 0L

    @Volatile private var currentScanMode = ScanSettings.SCAN_MODE_BALANCED

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildServiceNotification())
        scope.launch {
            (packetQueue as? PersistentPacketQueue)?.restore()
            retryPendingMessages()
            initMesh()
        }
        scope.launch { startLowPowerMonitor() }
        scope.launch { runSenderLoop() }
    }

    /**
     * Активная отправка очереди, не привязанная к BLE-сканеру.
     *
     * Тикает каждые SENDER_TICK_MS и, если в очереди есть пакеты и есть свежие
     * (видели за последние FRESH_DEVICE_TTL_MS) BLE-адреса соседей — отправляет.
     * Снимает зависимость доставки от частоты scan-колбеков: в SCAN_MODE_LOW_POWER
     * сканер молчит минутами, и без этого цикла очередь не дрейнится вообще.
     */
    private suspend fun runSenderLoop() {
        var ticks = 0L
        while (scope.isActive) {
            delay(SENDER_TICK_MS)
            ticks++
            if (ticks % EXPIRY_CHECK_EVERY_N_TICKS == 0L) {
                handleExpired(packetQueue.dropExpired())
            }

            evaluateBurst()

            if (packetQueue.isEmpty()) continue
            val now = System.currentTimeMillis()
            recentDevices.entries.removeAll { now - it.value > FRESH_DEVICE_TTL_MS }
            val freshMacs = recentDevices.keys.toList()
            if (freshMacs.isEmpty()) continue
            val mac = freshMacs.random()
            val device = runCatching { btAdapter?.getRemoteDevice(mac) }.getOrNull() ?: continue
            sendDrainedTo(device)
        }
    }

    private suspend fun handleExpired(expired: List<Packet>) {
        if (expired.isEmpty()) return
        expired.forEach { packet ->
            packetRetries.remove(packet.packetId)
            if (packet.packetType == PacketType.MESSAGE && packet.senderNodeId == nodeIdentity.nodeId) {
                runCatching {
                    messageRepository.getById(packet.messageId)?.let { msg ->
                        if (msg.status != MessageStatus.DELIVERED) {
                            messageRepository.update(msg.copy(status = MessageStatus.TTL_EXPIRED))
                        }
                    }
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun sendDrainedTo(device: BluetoothDevice) {
        val transport = bleTransport ?: return
        val packets = packetQueue.drainAll()
        packets.forEach { packet ->
            runCatching {
                transport.sendPacket(device, PacketEncoder.encode(packet))
                packetRetries.remove(packet.packetId)
                recentDevices[device.address] = System.currentTimeMillis()
                if (packet.senderNodeId == nodeIdentity.nodeId) {
                    nearbyNodesRepository.incrementSent()
                    if (packet.packetType == PacketType.MESSAGE) {
                        messageRepository.getById(packet.messageId)?.let { msg ->
                            if (msg.status == MessageStatus.PENDING) {
                                messageRepository.update(msg.copy(status = MessageStatus.IN_TRANSIT))
                            }
                        }
                    }
                } else {
                    nearbyNodesRepository.incrementRelayed()
                }
            }.onFailure { e ->
                recentDevices.remove(device.address)
                val retries = (packetRetries[packet.packetId] ?: 0) + 1
                if (retries <= MAX_PACKET_RETRIES) {
                    packetRetries[packet.packetId] = retries
                    packetQueue.enqueue(packet)
                } else {
                    packetRetries.remove(packet.packetId)
                    if (packet.packetType == PacketType.MESSAGE) {
                        messageRepository.getById(packet.messageId)?.let { msg ->
                            messageRepository.update(msg.copy(status = MessageStatus.TTL_EXPIRED))
                        }
                    }
                }
                Log.w(TAG, "Failed to send to ${device.address} (attempt $retries)", e)
            }
        }
    }

    private suspend fun retryPendingMessages() {
        runCatching {
            val now = System.currentTimeMillis()
            messageRepository.getPending().forEach { msg ->
                if (now - msg.createdAt > TTL_EXPIRY_MS) {
                    messageRepository.update(msg.copy(status = MessageStatus.TTL_EXPIRED))
                } else {
                    val toSend = if (msg.status == MessageStatus.IN_TRANSIT)
                        msg.copy(status = MessageStatus.PENDING) else msg
                    if (toSend !== msg) messageRepository.update(toSend)
                    sendMessageUseCase.execute(toSend)
                }
            }
        }.onFailure { Log.w(TAG, "Ошибка повторной отправки", it) }
    }

    private suspend fun startLowPowerMonitor() {
        while (scope.isActive) {
            delay(LOW_POWER_CHECK_INTERVAL_MS)
            val idleMs = System.currentTimeMillis() - lastNodeSeenAt
            isLowPowerMode = idleMs > LOW_POWER_IDLE_MS
            applyScanMode(targetScanMode())
        }
    }

    /**
     * Возвращает желаемый режим сканера. Приоритет: burst > low-power > balanced.
     * Все три источника изменения (sender-loop, low-power monitor, scanner-callback) идут
     * через эту функцию + applyScanMode — без неё restartScanner вызывался бы из разных
     * мест с конфликтующими решениями.
     */
    private fun targetScanMode(): Int {
        if (burstUntil > System.currentTimeMillis()) return ScanSettings.SCAN_MODE_LOW_LATENCY
        return if (isLowPowerMode) ScanSettings.SCAN_MODE_LOW_POWER
               else ScanSettings.SCAN_MODE_BALANCED
    }

    @Suppress("MissingPermission")
    private fun applyScanMode(target: Int) {
        if (target == currentScanMode) return
        currentScanMode = target
        Log.d(TAG, "Scan mode → ${scanModeName(target)}")
        restartScanner(target)
    }

    private fun scanModeName(mode: Int): String = when (mode) {
        ScanSettings.SCAN_MODE_LOW_LATENCY -> "LOW_LATENCY"
        ScanSettings.SCAN_MODE_BALANCED -> "BALANCED"
        ScanSettings.SCAN_MODE_LOW_POWER -> "LOW_POWER"
        else -> mode.toString()
    }

    /**
     * Если есть что отправлять, но ни одного свежего соседа — поднимаем сканер в LOW_LATENCY
     * на BURST_DURATION_MS, чтобы быстрее поймать advertise. Если соседи появились в кэше —
     * выходим из burst раньше.
     */
    private fun evaluateBurst() {
        val now = System.currentTimeMillis()
        val hasFreshNeighbor = recentDevices.values.any { now - it < FRESH_DEVICE_TTL_MS }
        val needBurst = !packetQueue.isEmpty() && !hasFreshNeighbor

        if (burstUntil == 0L && needBurst) {
            burstUntil = now + BURST_DURATION_MS
            Log.d(TAG, "Burst-scan ON")
            applyScanMode(targetScanMode())
        } else if (burstUntil != 0L && (now > burstUntil || hasFreshNeighbor)) {
            burstUntil = 0L
            Log.d(TAG, "Burst-scan OFF")
            applyScanMode(targetScanMode())
        }
    }

    @Suppress("MissingPermission")
    private fun initMesh() {
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = btManager.adapter
        btAdapter = adapter

        packetProcessor = PacketProcessor(nodeIdentity.nodeId, InMemorySeenPacketCache())

        val server = BleGattServer(packetProcessor, ::handleAction)
        gattServer = btManager.openGattServer(this, server.callback)
        gattServer?.let { server.init(it) }
        bleGattServer = server

        val leAdvertiser = adapter.bluetoothLeAdvertiser
        if (leAdvertiser == null) {
            Log.w(TAG, "BLE advertising not available")
            bleError.value = getString(R.string.ble_error_no_advertiser)
            return
        }
        bleError.value = null
        bleAdvertiser = BleAdvertiser(leAdvertiser, nodeIdentity.nodeId).also { it.start() }
        bleTransport = BleTransport(this)

        startScanner(ScanSettings.SCAN_MODE_BALANCED)
    }

    @Suppress("MissingPermission")
    private fun startScanner(scanMode: Int) {
        val adapter = btAdapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: run {
            bleError.value = getString(R.string.ble_error_no_scanner)
            return
        }

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(GattProtocol.SERVICE_UUID))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(scanMode)
            .build()

        val newScanner = BleScanner { result ->
            val mac = result.device.address
            lastNodeSeenAt = System.currentTimeMillis()
            recentDevices[mac] = lastNodeSeenAt

            val lastHello = helloSentTo[mac]
            if (lastHello == null || lastNodeSeenAt - lastHello > HELLO_RESEND_TTL_MS) {
                helloSentTo[mac] = lastNodeSeenAt
                sendHelloUseCase.execute()
            }

            if (packetQueue.isEmpty()) return@BleScanner
            scope.launch { sendDrainedTo(result.device) }
        }
        bleScanner = newScanner
        scanner.startScan(listOf(scanFilter), scanSettings, newScanner.callback)
    }

    @Suppress("MissingPermission")
    private fun restartScanner(scanMode: Int) {
        val adapter = btAdapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        runCatching { scanner.stopScan(bleScanner?.callback) }
        startScanner(scanMode)
    }

    private fun handleAction(action: PacketAction) {
        val fromAddress = when (action) {
            is PacketAction.Deliver -> action.fromAddress
            is PacketAction.Forward -> action.fromAddress
            PacketAction.Drop -> null
        }
        fromAddress?.let { recentDevices[it] = System.currentTimeMillis() }

        when (action) {
            is PacketAction.Deliver -> scope.launch {
                runCatching { receiveMessageUseCase.execute(action.packet) }
                    .onSuccess {
                        if (action.packet.packetType == PacketType.MESSAGE) {
                            showMessageNotification(action.packet)
                            action.fromAddress?.let { addr ->
                                runCatching { btAdapter?.getRemoteDevice(addr) }.getOrNull()
                                    ?.let { sendDrainedTo(it) }
                            }
                        }
                    }
                    .onFailure { Log.e(TAG, "Ошибка обработки входящего пакета", it) }
            }
            is PacketAction.Forward -> packetQueue.enqueue(action.packet)
            is PacketAction.Drop -> Unit
        }
    }

    private suspend fun showMessageNotification(packet: Packet) {
        val senderId = packet.senderNodeId
        val contactName = runCatching {
            contactRepository.observeDisplayName(senderId).first()
        }.getOrNull()
        val senderLabel = contactName ?: (senderId.value.take(8) + "…")

        val messageText = runCatching {
            val msg = messageRepository.getById(packet.messageId)
            when {
                msg == null -> null
                msg.mimeType?.startsWith("image/") == true ->
                    getString(R.string.msg_notification_image, msg.fileName ?: getString(R.string.conversations_attachment_image_default))
                msg.mimeType != null ->
                    getString(R.string.msg_notification_file, msg.fileName ?: getString(R.string.conversations_attachment_file_default))
                else -> String(msg.payload, Charsets.UTF_8)
            }
        }.getOrNull()

        val openIntent = PendingIntent.getActivity(
            this,
            senderId.value.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CONTACT_NODE_ID, senderId.value)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, MSG_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(senderLabel)
            .setContentText(messageText ?: getString(R.string.msg_notification_default))
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(msgNotificationId.getAndIncrement(), notification)
    }

    @Suppress("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REINIT_BLE -> {
                scope.launch {
                    runCatching {
                        bleAdvertiser?.stop()
                        btAdapter?.bluetoothLeScanner?.stopScan(bleScanner?.callback)
                        gattServer?.close()
                    }
                    helloSentTo.clear()
                    burstUntil = 0L
                    currentScanMode = ScanSettings.SCAN_MODE_BALANCED
                    initMesh()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        runCatching { bleAdvertiser?.stop() }
        runCatching { btAdapter?.bluetoothLeScanner?.stopScan(bleScanner?.callback) }
        runCatching { gattServer?.close() }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.service_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.service_channel_description) }
        )
        nm.createNotificationChannel(
            NotificationChannel(MSG_CHANNEL_ID, getString(R.string.msg_channel_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = getString(R.string.msg_channel_description) }
        )
    }

    private fun buildServiceNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, BleMeshService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .addAction(android.R.drawable.ic_delete, getString(R.string.service_notification_stop), stopIntent)
            .build()
    }

    companion object {
        private const val TAG = "BleMeshService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "mesh_channel"
        private const val MSG_CHANNEL_ID = "msg_channel"

        const val ACTION_REINIT_BLE = "com.sunflowerthu.meshcourier.REINIT_BLE"
        const val ACTION_STOP = "com.sunflowerthu.meshcourier.STOP"

        private const val MAX_PACKET_RETRIES = 3
        private const val TTL_EXPIRY_MS = 24 * 60 * 60 * 1000L
        private const val LOW_POWER_IDLE_MS = 5 * 60 * 1000L
        private const val LOW_POWER_CHECK_INTERVAL_MS = 60_000L

        private const val SENDER_TICK_MS = 500L
        private const val FRESH_DEVICE_TTL_MS = 60_000L
        private const val EXPIRY_CHECK_EVERY_N_TICKS = 120L
        private const val HELLO_RESEND_TTL_MS = 5 * 60 * 1000L
        private const val BURST_DURATION_MS = 30_000L

        private val msgNotificationId = AtomicInteger(1000)

        @Volatile
        var isRunning: Boolean = false
            private set

        val bleError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    }
}
