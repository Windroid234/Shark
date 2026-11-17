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
- vivo / iQOO devices (6.12.58 builds)

### 6.6.118 / 6.6.127 (Android 15)
- OnePlus Ace6 (PLQ110), OnePlus Ace5 Pro (PKR110)
- OnePlus Pad2 Pro (OPD2413)
- OPPO Find X8 Ultra (PKJ110), OPPO Find N5 (PKH110)
- OPPO Pad4 Pro (OPD2409)
- Realme GT8 (RMX6699)
- OPPO K13 Turbo Pro (PLE110)
- vivo / iQOO devices (6.6.127 build)

### vivo / iQOO builds
- `6.12.58-android16-6-g690108013dcc-abogki576129896-4k`
- `6.12.58-android16-6-g0a092cc0037a-abogki521987229-4k`
- `6.12.58-android16-6-gdd6fd5c98918-abogki543627742-4k`
- `6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k`
- `6.6.127-android15-8-gd489d4ad2a70-abogki567717946-4k`

The complete kernel-version table is in the
[README](https://github.com/kpgc10kai/Shark#supported-devices).
