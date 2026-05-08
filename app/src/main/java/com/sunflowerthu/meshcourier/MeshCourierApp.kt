package com.sunflowerthu.meshcourier

import android.app.Application
import android.util.Log
import com.sunflowerthu.meshcourier.data.crypto.CspInitializer
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MeshCourierApp : Application() {

    @Inject lateinit var cryptoManager: CryptoManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val ok = CspInitializer.init(this@MeshCourierApp)
            if (!ok) Log.e(TAG, "КриптоПро CSP не инициализирован")
            runCatching { cryptoManager.loadPersistedKeys() }
                .onFailure { Log.e(TAG, "Ошибка загрузки ключей", it) }
        }
    }

    companion object {
        private const val TAG = "MeshCourierApp"
    }
}
