package roro.stellar.manager

import android.content.Context
import android.content.SharedPreferences

/**
 * Minimal settings bridge for the ported wireless-ADB code.
 * The fork keeps its preferences in the existing "pyb110" store.
 */
object StellarSettings {
    const val TCPIP_PORT = "adb_tcpip_port"
    const val TCPIP_PORT_ENABLED = "adb_tcpip_port_enabled"

    private var preferences: SharedPreferences? = null

    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.applicationContext
                .getSharedPreferences("pyb110", Context.MODE_PRIVATE)
        }
    }

    fun getPreferences(): SharedPreferences =
        checkNotNull(preferences) { "StellarSettings.init() was not called" }
}
