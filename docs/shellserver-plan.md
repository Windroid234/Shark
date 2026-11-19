# Experimental: persistent ShellServer

Status: **experimental branch only**. Do not tag or publish a release from this
work. `v2.5.1-en.2` stays the public Latest build.

## Goal

Replace the per-command ADB backend with a persistent privileged shell process
launched through `app_process` as the `shell` user, similar in role to the
upstream `ShellSupervisorService` + `ShellServer`, but with our own protocol.

## Why

- Avoid opening a new ADB connection for every command
- Keep one long-lived shell process for the whole root flow
- Provide the foundation for wired ADB activation (PC starts the server)
- Make reconnection cheaper after a short network drop

## Non-goals

- No public release until the whole flow is validated
- No changes to the stable branch while this is in progress

## Architecture

```
Shark app (uid app)
  │  LocalSocket (abstract name), token + peer-UID check
  ▼
ShellServer process (uid shell, app_process)
  │  ProcessBuilder / Runtime.exec
  ▼
shell commands (id, exploit binary, ksud, ...)
```

### Components

1. `com.shark.kty.shell.ShellServer`
   - `app_process` entry point
   - Creates an abstract `LocalServerSocket`
   - Accepts one client at a time, verifies peer UID == app UID
   - Executes commands with env/dir, streams stdout/stderr and exit code

2. `ShellServerClient`
   - Connects to the abstract socket
   - Sends `exec(cmd[], env[], dir)` and streams output
   - Returns a `PrivProcess` compatible wrapper

3. `ShellSupervisorService`
   - Foreground service owning the server lifecycle
   - Starts the server through the current ADB backend when needed
   - Restarts it if it dies; stores socket name + token in prefs

4. `PrivService`
   - New optional backend `SHELL_SERVER`
   - Falls back to `WIRELESS_ADB` when the server is not running

## Protocol (draft)

Binary frames via `DataInputStream` / `DataOutputStream`:

```
request:
  int  magic "SHSK"
  int  version = 1
  int  commandCount
  UTF  command[i]
  int  envCount
  UTF  env[i]
  UTF  dir ("" = inherit)

response:
  byte type: 1=stdout, 2=stderr, 3=exit
  int  length
  byte[length] payload (exit payload = int code)
```

The server closes the connection only after the command finishes, so the client
can expose normal streams and `waitFor()`.

## Startup

### Wireless (current path)

1. Pair and connect as today.
2. ShellSupervisorService runs:
   `app_process -Djava.class.path=<apk> /system/bin com.shark.kty.shell.ShellServer <socket>`
   through the existing ADB shell backend, with `setsid`, stdin `/dev/null`,
   output to a log file.
3. App connects to the abstract socket.

### Wired (later)

PC runs the same `adb shell app_process ...` command; the app then connects to
the socket. No separate protocol is needed.

## Security

- Abstract socket name contains a random suffix.
- Server checks the connecting peer UID (`LocalSocket.getPeerCredentials()`)
  and only accepts the app UID.
- A random token is still exchanged in the first frame as defence in depth.
- No TCP port is opened.

## Milestones

1. ShellServer with a single `exec` and one-client accept; manual `app_process`
   start via `adb shell`.
2. App starts the server through the wireless ADB backend and runs `id`.
3. `PrivService` backend `SHELL_SERVER`; root flow works end to end.
4. Supervisor keepalive and restart.
5. Short-disconnect reconnection (before reboot auto-reconnect).
6. Wired activation instructions using the same server.

## Risks

- SELinux may block abstract sockets or `app_process` from the shell domain
- Background/foreground service restrictions on Android 17
- Process lifetime: the shell process must not be killed by the ADB session end
- Security: a wrong peer check would expose a root-adjacent shell
- Debugging without a release: use debug APKs and logcat tags only

## Test plan

- Start server manually with `adb shell app_process ...`; verify socket connect
- Run `id`, `getenforce`, then the real exploit binary from the server
- Verify exit codes, env, cwd, stdout/stderr streaming
- Kill the server; verify supervisor restart
- Run the full wireless pairing -> root flow
- Only after all pass, consider merging back to a release branch
