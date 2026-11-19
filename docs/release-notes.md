English fork of [Yunnijian/Shark](https://github.com/Yunnijian/Shark).

- English documentation and English UI strings (Chinese kept in `values-zh`)
- Signed with the fork's own release key, so future releases update in place
- Package `com.shark.kty` (installs alongside the old `com.pyb110.onekey` build)

Root is temporary and disappears after a reboot. The kernel build must match
one of the versions below **exactly**; anything else is rejected.

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

