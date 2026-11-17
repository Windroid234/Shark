package roro.stellar.manager.util

import java.io.BufferedReader

object EnvironmentUtils {
    /** Returns `service.adb.tcp.port` as exposed by the system, or -1. */
    fun getAdbTcpPort(): Int {
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "service.adb.tcp.port"))
            val value = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
            process.waitFor()
            value.toIntOrNull() ?: -1
        }.getOrDefault(-1)
    }
}
