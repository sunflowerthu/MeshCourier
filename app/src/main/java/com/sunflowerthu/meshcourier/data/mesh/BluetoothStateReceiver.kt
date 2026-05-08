package com.sunflowerthu.meshcourier.data.mesh

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BluetoothStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        if (state == BluetoothAdapter.STATE_ON && BleMeshService.isRunning) {
            val reinit = Intent(context, BleMeshService::class.java)
                .setAction(BleMeshService.ACTION_REINIT_BLE)
            context.startForegroundService(reinit)
        }
    }
}
