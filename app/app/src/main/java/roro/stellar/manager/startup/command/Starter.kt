package roro.stellar.manager.startup.command

/**
 * Placeholder for the privileged service start command.
 *
 * The full port will launch the Shark shell service through
 * `app_process` (same role as Stellar's Starter). Until that service is
 * implemented, the pairing flow still grants WRITE_SECURE_SETTINGS and
 * connects over ADB, but no privileged backend is started.
 */
object Starter {
    const val wirelessDebuggingCommand = "true"
}
