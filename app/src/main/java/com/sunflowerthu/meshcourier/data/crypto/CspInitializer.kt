package com.sunflowerthu.meshcourier.data.crypto

import android.content.Context
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.JCSP.CSPConfig
import java.security.Security

object CspInitializer {

    @Volatile
    private var initialized = false

    fun init(context: Context): Boolean {
        if (initialized) return true
        val code = CSPConfig.init(context)
        if (code != CSPConfig.CSP_INIT_OK) return false
        if (Security.getProvider(JCSP.PROVIDER_NAME) == null) {
            Security.addProvider(JCSP())
        }
        initialized = true
        return true
    }
}
