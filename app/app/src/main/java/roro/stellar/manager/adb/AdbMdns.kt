package roro.stellar.manager.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import roro.stellar.manager.compat.LocalNetwork
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

/**
 * mDNS discovery modelled on Shizuku's implementation: discover once, resolve
 * the first matching service, and stop. The previous port (from Stellar) kept
 * stopping and restarting discovery every 500 ms, which on Android 17 usually
 * cancels the query before any response arrives.
 */
@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(
    context: Context,
    private val serviceType: String,
    private val observer: Observer<Int>,
    private val onMaxRefresh: (() -> Unit)? = null,
    private val onStatusUpdate: ((String) -> Unit)? = null,
    private val maxRefreshCount: Int = MAX_REFRESH_COUNT,
    private val onPermissionRequired: (() -> Unit)? = null
) {

    private val appContext: Context = context.applicationContext
    private val nsdManager: NsdManager = context.getSystemService(NsdManager::class.java)

    @Volatile
    private var running = false

    @Volatile
    private var registered = false

    private var listener: DiscoveryListener? = null
    private var serviceName: String? = null

    fun start() {
        if (running) return

        if (!LocalNetwork.hasAccess(appContext)) {
            Log.w(TAG, "ACCESS_LOCAL_NETWORK not granted; stopping discovery")
            onPermissionRequired?.invoke()
            return
        }

        running = true
        serviceName = null
        onStatusUpdate?.invoke("Starting service discovery…")
        val discovery = DiscoveryListener(this)
        listener = discovery
        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discovery)
            Log.v(TAG, "discovery started: $serviceType")
        } catch (e: Exception) {
            Log.e(TAG, "discovery failed to start", e)
            running = false
            onMaxRefresh?.invoke()
        }
    }

    fun stop() {
        if (!running) return
        running = false
        val discovery = listener
        listener = null
        if (registered && discovery != null) {
            registered = false
            runCatching { nsdManager.stopServiceDiscovery(discovery) }
        }
    }

    fun destroy() {
        stop()
    }

    private fun onDiscoveryStarted() {
        registered = true
        onStatusUpdate?.invoke("Searching for the pairing service…")
    }

    private fun onDiscoveryStopped() {
        registered = false
    }

    private fun onServiceFound(info: NsdServiceInfo) {
        if (!running) return
        Log.v(TAG, "service found: ${info.serviceName}")
        runCatching { nsdManager.resolveService(info, ResolveListener(this)) }
            .onFailure { Log.e(TAG, "resolveService failed", it) }
    }

    private fun onServiceResolved(resolved: NsdServiceInfo) {
        if (!running) return
        val host = resolved.host?.hostAddress ?: return
        val isLocal = runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence().any { nif ->
                nif.inetAddresses.asSequence().any { it.hostAddress == host }
            }
        }.getOrDefault(false)
        if (!isLocal) {
            Log.v(TAG, "ignoring non-local service $host:${resolved.port}")
            return
        }
        // The port must already be in use by adbd, otherwise it is not the
        // service we are looking for.
        if (!isPortAvailable(resolved.port)) {
            serviceName = resolved.serviceName
            Log.i(TAG, "service resolved: $host:${resolved.port}")
            stop()
            observer.onChanged(resolved.port)
        }
    }

    private fun onServiceLost(info: NsdServiceInfo) {
        if (info.serviceName == serviceName) observer.onChanged(-1)
    }

    private fun isPortAvailable(port: Int): Boolean = try {
        ServerSocket().use {
            it.bind(InetSocketAddress("127.0.0.1", port), 1)
            true
        }
    } catch (_: IOException) {
        false
    }

    internal class DiscoveryListener(private val adbMdns: AdbMdns) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.v(TAG, "onDiscoveryStarted: $serviceType")
            adbMdns.onDiscoveryStarted()
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "onStartDiscoveryFailed: $serviceType, $errorCode")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.v(TAG, "onDiscoveryStopped: $serviceType")
            adbMdns.onDiscoveryStopped()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "onStopDiscoveryFailed: $serviceType, $errorCode")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            adbMdns.onServiceFound(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            adbMdns.onServiceLost(serviceInfo)
        }
    }

    internal class ResolveListener(private val adbMdns: AdbMdns) : NsdManager.ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "onResolveFailed: ${nsdServiceInfo.serviceName}, $errorCode")
        }

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            adbMdns.onServiceResolved(nsdServiceInfo)
        }
    }

    companion object {
        const val TLS_CONNECT = "_adb-tls-connect._tcp"
        const val TLS_PAIRING = "_adb-tls-pairing._tcp"
        const val TAG = "AdbMdns"

        /** Seconds before giving up; the service treats it like the old max-refresh limit. */
        const val MAX_REFRESH_COUNT = 30
    }
}
