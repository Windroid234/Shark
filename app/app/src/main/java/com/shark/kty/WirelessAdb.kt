package com.shark.kty

import android.util.Log
import roro.stellar.manager.StellarSettings
import roro.stellar.manager.adb.AdbClient
import roro.stellar.manager.adb.AdbKey
import roro.stellar.manager.adb.PreferenceAdbKeyStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
        val currentKey = key ?: return -1 to "no wireless ADB connection"
        val currentPort = port
        val marker = "__SHARK_RC__"
        val output = ByteArrayOutputStream()
        return runCatching {
            AdbClient(host, currentPort, currentKey).use { client ->
                client.connect()
                client.shellCommand("($command); echo $marker=\$?") { chunk ->
                    synchronized(output) { output.write(chunk) }
                }
            }
            val text = output.toString(Charsets.UTF_8.name())
            val lines = text.split('\n')
            var rc = 0
            val kept = StringBuilder()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("$marker=")) {
                    rc = trimmed.substringAfter('=').toIntOrNull() ?: 0
                } else {
                    kept.append(line).append('\n')
                }
            }
            rc to kept.toString()
        }.getOrElse {
            Log.e(TAG, "shell command failed: $command", it)
            -1 to (it.message ?: "adb shell failed")
        }
    }

    /**
     * Process-compatible wrapper. Output is streamed through a pipe so the
     * existing log readers keep working, and waitFor() returns the exit code.
     */
    fun newProcess(cmd: Array<String>): PrivProcess {
        val outPipe = PipedOutputStream()
        val outIn = PipedInputStream(outPipe, 64 * 1024)
        var exitCode = -1
        val command = cmd.joinToString(" ") { quote(it) }
        val thread = Thread {
            val (code, text) = runShell(command)
            exitCode = code
            runCatching {
                outPipe.write(text.toByteArray())
                outPipe.flush()
            }
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

    private fun quote(arg: String): String =
        "'" + arg.replace("'", "'\\''") + "'"
}
