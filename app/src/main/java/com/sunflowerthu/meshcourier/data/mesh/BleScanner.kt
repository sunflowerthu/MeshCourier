package com.sunflowerthu.meshcourier.data.mesh

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class BleScanner(
    private val onNodeFound: (ScanResult) -> Unit
) {

    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onNodeFound(result)
        }
    }
}