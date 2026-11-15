# UniRoot-Next

**One-tap root for Samsung Galaxy devices — New exploit (DirtyFrag CVE-2026-43284, unprivileged) + Old exploit profiles, with auto-root at every boot.**

An updated and actively maintained continuation of [UniRoot](https://github.com/kuuky29/UniRoot), originally created by **kuuky**.

<p align="center"><b>Neither I nor the original author are responsible for bricked phones.</b></p>

---

## What's new in UniRoot-Next

- **KernelSU-Next `v3.4.2` (`33355-5`) support** — added a built-in `3.4.2-kdp-samsung` profile and updated all bundled Samsung KDP+DEFEX `.ko` modules (`android12-5.10` through `android17-6.18`) to properly report `version = 33355`, `uapi_version = 5`, and `v3.4.2` (removing the `Kernel update required` warning).
- **Fixed in-app `ksud` updater (`two slots` error)** — filtered out embedded `susfs` `.ko` streams when patching official KernelSU-Next `ksud` binaries on-device, added `aarch64_${kmi}_kernelsu.ko` asset fallback, and allowed re-patching to overwrite stale cached profiles.
- **Fixed New Method profile & Instant Root staging** — `GhostSamRunner` and `MainActivity` now properly load and stage the selected/updated `ksud` profile instead of falling back to bundled assets.
- **Persistent release signing & CI releases** — added a permanent release keystore (`app/release.keystore`) so new builds update cleanly in-place without reinstalling, plus a GitHub Actions workflow to build and attach release APKs automatically on version tags.

## Core Features (V5 Base)

- **New exploit (fast)** — the unprivileged DirtyFrag engine (CVE-2026-43284): IpSecManager allocates the SA, the native engine corrupts the kernel page cache, ksud is late-loaded from memory. No profile, no Shizuku, no setup — press **Root now**.
- **ksud updater, in the app** — a full-width button (New exploit section) downloads the **latest official ksud** (KernelSU Next *or* classic, per the selected flavor) and patches it **on device** with the bundled **Samsung KDP+DEFEX .kos**, then the result is used immediately (Next → auto-selected profile, classic → `ksud-classic-latest`).
- **ksud profiles** — profile bands (same style as the old exploit profiles) for both flavors: bundled + downloaded profiles, selectable, and deletable from **Advanced settings**.
- **Auto-root at boot** — a foreground boot service waits for the first unlock and re-runs the chain (up to 6 attempts), with a partial WakeLock so the phone can't sleep mid-exploit.
- **Progress UI** — animated progress bar with live engine stages (IpSec SA → SPI → ksud staged → arming → **Phone rooted ✓**), and a bottom progress popup for the ksud updater. Fully in English.
- **Root state banner** — clean Rooted / Not rooted banner driven by a live `su -c id` probe (no more false "rooted" after a reboot).

## Old exploit (slow)

The classic profile pipeline stays untouched: CVE-2026-43499 payloads (local or Shizuku), Root My Galaxy integration, per-device validated profiles (S25 ZZHL/ZZI4, S26 Ultra ZZHK, S93XX, Oppo), profile editor, instant root, and run-log archives.

## Build

```sh
./gradlew assembleRelease   # -> app/build/outputs/apk/release/
```

## Credits

This project builds directly upon the original **UniRoot** and stands on the shoulders of:

* **kuuky** — [UniRoot](https://github.com/kuuky29/UniRoot): the original UniRoot app and V5 architecture this fork continues
* **diabl0w** — [DFRoot](https://github.com/diabl0w/DFRoot): the DirtyFrag engine this whole project is built around
* **snothin** — [GhostSam](https://github.com/snothin/GhostSam): the Samsung-patched kos, the ksud patching approach and the staged native API
* **polygraphene** — the DFRoot fork lineage and the DEFEX bypass work
* **tiann** — [KernelSU](https://github.com/tiann/KernelSU)
* **rifsxd & the KernelSU-Next team** — [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)
* **YuKongA** — [GhostLock](https://github.com/YuKongA/GhostLock): the UI this app's interface is based on

Thank you all 🙏
