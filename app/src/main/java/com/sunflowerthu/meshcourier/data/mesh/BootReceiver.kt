package com.sunflowerthu.meshcourier.data.mesh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val serviceIntent = Intent(context, BleMeshService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
