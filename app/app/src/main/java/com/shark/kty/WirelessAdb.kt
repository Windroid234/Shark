package com.shark.kty

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
 * Keeps a loopback ADB connection alive after wireless-debugging pairing and
 * exposes it as a privileged backend. Commands run as the `shell` user, which
 * is the same privilege level Shizuku/Stellar provide.
 */
object WirelessAdb {
    @Volatile
    var connected: Boolean = false
        private set

    @Volatile
    var port: Int = -1
        private set

    private var client: AdbClient? = null
    private val lock = Any()

    fun connect(context: android.content.Context, host: String, port: Int): Boolean {
        synchronized(lock) {
            closeLocked()
            StellarSettings.init(context)
            return runCatching {
                val key = AdbKey(
                    PreferenceAdbKeyStore(StellarSettings.getPreferences()),
                    "stellar"
                )
                val c = AdbClient(host, port, key)
                c.connect()
                client = c
                this.port = port
                connected = true
                true
            }.getOrElse {
                connected = false
                false
            }
        }
    }

    fun close() {
        synchronized(lock) { closeLocked() }
    }

    private fun closeLocked() {
        runCatching { client?.close() }
        client = null
        connected = false
        port = -1
    }

    /** Runs a shell command and returns (exit code, stdout). */
    fun runShell(command: String): Pair<Int, String> = synchronized(lock) {
        val c = client ?: return -1 to "no wireless ADB connection"
        val marker = "__SHARK_RC__"
        val output = ByteArrayOutputStream()
        return runCatching {
            c.shellCommand("($command); echo $marker=\$?") { chunk ->
                synchronized(output) { output.write(chunk) }
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
