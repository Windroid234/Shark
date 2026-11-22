English fork of [Yunnijian/Shark](https://github.com/Yunnijian/Shark).

- English documentation and English UI strings (Chinese kept in `values-zh`)
- Signed with the fork's own release key, so future releases update in place
- Package `com.shark.kty` (installs alongside the old `com.pyb110.onekey` build)

Root is temporary and disappears after a reboot. The kernel build must match
one of the versions below **exactly**; anything else is rejected.

## What's new in 2.5.1-en.4

- **Wired ADB activation**: `tools/wired-adb.sh` starts the privileged shell
  server over a USB ADB connection. Shark detects it as the Shizuku backend,
  so One-click root works without Wireless debugging pairing.
- **Pairing helper status**: Extras now shows whether the accessibility pairing
  helper is enabled and opens the Accessibility settings directly.
- **Bounded pairing attempts**: pairing uses shorter socket timeouts and a
  20-second candidate deadline instead of leaving `Pairing…` running.
- **Faster update checks**: GitHub release asset `digest` values are used
  first, avoiding a `.sha256.txt` download on every check. Gitee and older
  responses still fall back to the checksum file or release body.
- **Shell supervisor cleanup**: after the privileged shell is running for
  10 seconds, the supervisor stops and removes its notification; the detached
  watchdog keeps ShellServer alive.
- **Diagnostics**: `--help` and the `probe` subcommand are documented, and the
  no-argument path now reports failure instead of printing a misleading
  `ROOT_SHELL_OK`.
- **Release tooling**: `tools/archive-release.sh` archives a superseded release
  by removing its assets, marking it as a pre-release, and leaving a latest
  download notice.

## 2.5.1-en.3

- **6.6.118 KASLR alignment fix (Issue #6)**: the plant no longer assumes a
  2 MiB-aligned kernel image. It sweeps 64 KiB candidates for the low-21-bit
  adjustment, so devices whose virtual slide includes a physical-placement
  offset calibrate instead of reporting `timers=0`.
- **Automatic wireless pairing**: with the accessibility helper enabled, Shark
  reads the six-digit code from the Settings pairing dialog and submits it
  automatically. The notification code field remains available as a fallback.
- **Pairing notification cleanup**: stale and duplicate pairing notifications
  are cleared on success, and the code field no longer remains after pairing.
- **Shell supervisor timeout**: if no backend appears within 30 seconds, the
  supervisor stops and removes its notification instead of waiting forever.
- **Documentation**: the manual and README now cover automatic pairing,
  notification permissions, and the `timers=0` / `aligned=0` troubleshooting
  case.

## Fork features

- **Wireless ADB pairing** (experimental): pair from Settings, auto-grant
  `WRITE_SECURE_SETTINGS`, and run the root flow over the loopback ADB
  connection — no PC or separate Stellar/Shizuku app
- **Auto root on boot** with an English UI
- All upstream kernel profiles, including the Xiaomi 18 Pro / 18 Fold builds

## Supported devices

### 6.18.21 (Android 17)
- OnePlus 16 (PYB110)
- Xiaomi 18 Pro (M610BB)
- Xiaomi 18 Fold (lhasa) — experimental, offsets taken from upstream and not verified against the matching firmware
- Xiaomi Pad 7S Pro (violin) — experimental, offsets taken from upstream 2.5.1
- Xiaomi Pad 9 Pro Max (yingtian) — experimental, offsets taken from upstream 2.5.1

### 6.12.69 (Android 16)
- OnePlus 15 (PLK110), OnePlus 15T (PLZ110)
- OnePlus Ace7 Pro (PMS110), OnePlus Ace7 (PYF110)
- OnePlus Ace6T (PLR110), OnePlus Ace6 Ultra (PMB110)
- OnePlus Pad3 Pro (OPD2513)
- OPPO Find X9 Ultra (PMA110), Find X9 Pro (PLG110), Find X9 (PLJ110)
- OPPO Find X9s Pro (PME110EX), Find X10 (PMX110)
- OPPO Pad Mini (OPD2515), OPPO Pad 5 Pro (OPD2511)
- Realme GT8 Pro (RMX5200), Realme Neo8 (RMX8899)

### 6.12.58 / 6.12.38 (Android 16)
- OnePlus Pad3 Pro (OPD2513), OPPO Pad Mini (OPD2515)
- OnePlus Ace6T (PLR110), Realme Neo8 (RMX8899)

### 6.6.118 (Android 15)
- OnePlus Ace6 (PLQ110), OnePlus Ace5 Pro (PKR110)
- OnePlus Pad2 Pro (OPD2413)
- OPPO Find X8 Ultra (PKJ110), OPPO Find N5 (PKH110)
- OPPO Pad4 Pro (OPD2409)
- Realme GT8 (RMX6699)
- OPPO K13 Turbo Pro (PLE110)

The complete kernel-version table is in the
[README](https://github.com/kpgc10kai/Shark#supported-devices).

## Release policy

Only the latest build of each version line keeps downloadable assets. Superseded, broken or intermediate builds keep a notice page as a pre-release with the APK removed, so old links point to https://github.com/kpgc10kai/Shark/releases/latest.

