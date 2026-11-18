package com.shark.kty

import android.util.Log
import roro.stellar.manager.StellarSettings
import roro.stellar.manager.adb.AdbClient
import roro.stellar.manager.adb.AdbKey
import roro.stellar.manager.adb.PreferenceAdbKeyStore
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Loopback ADB backend for the wireless-debugging activation flow.
 *
 * Every command gets its own short-lived ADB connection. AdbClient uses a
 * fixed local stream id, so sharing one connection between a long-running
 * exploit and the state/preflight commands makes their streams collide and the
 * exploit exits immediately with -1.
 */
object WirelessAdb {
    private const val TAG = "WirelessAdb"

    @Volatile
    var connected: Boolean = false
        private set

    @Volatile
    var port: Int = -1
        private set

    private var host: String = "127.0.0.1"
    private var key: AdbKey? = null
    private val lock = Any()

    fun connect(context: android.content.Context, host: String, port: Int): Boolean {
        synchronized(lock) {
            StellarSettings.init(context)
            val newKey = runCatching {
                AdbKey(PreferenceAdbKeyStore(StellarSettings.getPreferences()), "stellar")
            }.getOrElse {
                Log.e(TAG, "failed to load ADB key", it)
                connected = false
                return false
            }
            // Verify that the paired connection is usable before reporting success.
            val ok = runCatching {
                AdbClient(host, port, newKey).use { it.connect() }
                true
            }.getOrElse {
                Log.e(TAG, "failed to connect to $host:$port", it)
                false
            }
            if (ok) {
                this.host = host
                this.port = port
                this.key = newKey
                connected = true
            } else {
                connected = false
            }
            return ok
        }
    }

    fun close() {
        synchronized(lock) {
            connected = false
            port = -1
            key = null
        }
    }

    /** Runs a shell command on a fresh connection and returns (exit code, stdout). */
    fun runShell(command: String): Pair<Int, String> {
        val output = StringBuilder()
        val rc = streamShell(command) { output.append(it) }
        return rc to output.toString()
    }

    /**
     * Streams shell output line by line on a fresh connection. The trailing
     * exit-code marker is consumed internally and never forwarded.
     *
     * The exploit can park a holder process that inherits stdout, so the ADB
     * stream may never send A_CLSE. As soon as the marker line arrives we stop
     * reading and return the exit code instead of waiting forever.
     */
    fun streamShell(command: String, onLine: (String) -> Unit): Int {
        val currentKey = key ?: return -1
        val currentPort = port
        val marker = "__SHARK_RC__"
        var rc = 0
        return try {
            AdbClient(host, currentPort, currentKey).use { client ->
                client.connect()
                val pending = StringBuilder()
                client.shellCommand("($command); echo $marker=\$?") { chunk ->
                    pending.append(String(chunk, Charsets.UTF_8))
                    while (true) {
                        val end = pending.indexOf("\n")
                        if (end < 0) break
                        val line = pending.substring(0, end).trimEnd('\r')
                        pending.delete(0, end + 1)
                        if (line.startsWith("$marker=")) {
                            rc = line.substringAfter('=').toIntOrNull() ?: 0
                            throw MarkerSeen(rc)
                        }
                        onLine(line + "\n")
                    }
                }
                if (pending.isNotEmpty()) {
                    val line = pending.toString().trimEnd('\r')
                    if (line.startsWith("$marker=")) {
                        rc = line.substringAfter('=').toIntOrNull() ?: 0
                    } else {
                        onLine(line + "\n")
                    }
                }
            }
            rc
        } catch (marker: MarkerSeen) {
            marker.rc
        } catch (t: Throwable) {
            Log.e(TAG, "shell command failed: $command", t)
            -1
        }
    }

    private class MarkerSeen(val rc: Int) : RuntimeException()

    /**
     * Process-compatible wrapper. Output is streamed through a pipe so the
     * existing log readers keep working, and waitFor() returns the exit code.
     *
     * The environment and working directory matter: the late-load worker reads
     * PYB110_* variables to find ksud and the app files directory. Stellar and
     * Shizuku pass them directly, so the wireless backend has to export them
     * into the shell before running the command.
     */
    fun newProcess(cmd: Array<String>, env: Array<String>? = null, dir: String? = null): PrivProcess {
        val outPipe = PipedOutputStream()
        val outIn = PipedInputStream(outPipe, 64 * 1024)
        var exitCode = -1
        val command = buildShellCommand(cmd, env, dir)
        val thread = Thread {
            val code = streamShell(command) { text ->
                runCatching {
                    outPipe.write(text.toByteArray())
                    outPipe.flush()
                }
            }
            exitCode = code
            runCatching { outPipe.close() }
        }.apply {
            isDaemon = true
            start()
        }
        return PrivProcess(
            outIn,
            object : OutputStream() { override fun write(b: Int) {} },
            ByteArrayInputStream(ByteArray(0))
        ) {
            runCatching { thread.join() }
            exitCode
        }
    }

    private fun buildShellCommand(cmd: Array<String>, env: Array<String>?, dir: String?): String {
        val args = cmd.joinToString(" ") { quote(it) }
        if (env.isNullOrEmpty() && dir == null) return args
        val script = buildString {
            env?.forEach { entry ->
                val key = entry.substringBefore('=')
                val value = entry.substringAfter('=', "")
                if (key.isNotEmpty()) append("export ").append(key).append('=').append(quote(value)).append("; ")
            }
            if (dir != null) append("cd ").append(quote(dir)).append(" && ")
            append(args)
        }
        return "sh -c " + quote(script)
    }

    private fun quote(arg: String): String =
        "'" + arg.replace("'", "'\\''") + "'"
}
