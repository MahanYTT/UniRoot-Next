<div align="center">

# UniRoot Next — One-Tap Root for Samsung Galaxy (KernelSU-Next & KernelSU)

**Unprivileged One-Tap Android Root for Samsung Galaxy S25 / S25+ / S25 Ultra, S26 Ultra, Z Fold 8 & GKI 2.0 Devices — with Full KernelSU-Next v3.4.2, Zygisk (Vector / Zygisk Next / ReZygisk), LSPosed & Auto-Root on Boot Support**

[![Latest Release](https://img.shields.io/github/v/release/MahanYTT/UniRoot-Next?style=for-the-badge&logo=github&label=Latest%20Release&color=2563eb)](https://github.com/MahanYTT/UniRoot-Next/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/MahanYTT/UniRoot-Next/total?style=for-the-badge&logo=android&label=Downloads&color=16a34a)](https://github.com/MahanYTT/UniRoot-Next/releases/latest)
[![KernelSU-Next](https://img.shields.io/badge/KernelSU--Next-v3.4.2%20(33355--5)-7c3aed?style=for-the-badge)](https://github.com/KernelSU-Next/KernelSU-Next)
[![Android GKI](https://img.shields.io/badge/GKI%20Kernels-5.10%20%E2%80%93%206.18%20(Android%2012%E2%80%9317)-0284c7?style=for-the-badge)](https://github.com/MahanYTT/UniRoot-Next)
[![Samsung KDP/DEFEX](https://img.shields.io/badge/Samsung-KDP%20%2B%20DEFEX%20Bypass-ea580c?style=for-the-badge)](https://github.com/MahanYTT/UniRoot-Next)

<p align="center">
  <a href="https://github.com/MahanYTT/UniRoot-Next/releases/latest"><b>⬇️ Download Latest APK</b></a> •
  <a href="#-key-features"><b>Features</b></a> •
  <a href="#-supported-devices--kernel-kmi-compatibility"><b>Supported Devices</b></a> •
  <a href="#-how-to-root-step-by-step-guide"><b>How to Root</b></a> •
  <a href="#-zygisk-vector--lsposed-support"><b>Zygisk & LSPosed</b></a> •
  <a href="#-faq--troubleshooting"><b>FAQ</b></a>
</p>

Developed & actively maintained by **[MahanYTT](https://github.com/MahanYTT)** ([MahanYTT/UniRoot-Next](https://github.com/MahanYTT/UniRoot-Next)) — an enhanced continuation of [UniRoot](https://github.com/kuuky29/UniRoot), originally created by **kuuky**.

> **Disclaimer:** Neither the developer nor any contributors are responsible for bricked devices, bootloops, voided warranties, or data loss. Use at your own risk.

</div>

---

## 📖 Overview

**UniRoot Next** is an all-in-one Android rooting application for **Samsung Galaxy** smartphones and **Android GKI 2.0** devices running **One UI 6, One UI 7, One UI 8 and One UI 9** (`Linux 5.10` through `6.18`, `Android 12` to `Android 17`).

It delivers **instant, one-tap temporary & boot-persistent root access** using **KernelSU-Next (`v3.4.2`, UAPI v5)** or **KernelSU Classic**—**without unlocking the bootloader**, without tripping **Samsung Knox (`0x0`)**, and without requiring a PC, Odin, or custom recovery (TWRP).

All bundled **KernelSU-Next** and **KernelSU** Loadable Kernel Modules (`kernelsu.ko`) are pre-patched for **Samsung KDP (Kernel Data Protection)**, **RKP (Real-time Kernel Protection)**, and **Samsung DEFEX (Defeat Exploit)** so root access, superuser grants, metamodule overlay mounts, **Zygisk (Vector / Zygisk Next / ReZygisk)**, and **LSPosed** work out of the box on locked Samsung firmware.

---

## ✨ What's New in UniRoot Next (`v5.3.0`)

- **Complete UI Overhaul (3-Tab Home / Logs / Settings Architecture)**
  - Redesigned light (*Alabaster Slate*) and dark (*Obsidian Slate*) Miuix themes with a live pulse Hero Status Card, smooth sliding-thumb segmented controls, tactile *Root Now* CTA (morphing into a live progress bar while running), 4-step root execution & 5-step `ksud` patcher checklists, searchable & tone-filterable Live Console + status-rail Run History, and foldable/tablet two-column adaptation (`>= 720dp`).
  - **New Exploit** and **KernelSU-Next** are now first and selected by default across both the UI and boot auto-root preferences.
- **Built-In App Self-Updater (GitHub Releases)**
  - Checks `MahanYTT/UniRoot-Next` releases automatically (rate-limit safe 6-hour cache + `If-None-Match` ETag), renders Markdown changelogs, streams APK downloads with live MB/s progress and cancel support, verifies SHA-256 integrity + release signing certificate compatibility, handles Android's *Install Unknown Apps* permission flow cleanly (even across process restarts), and auto-cleans stale APKs.
- **Overhauled Device Detection & Per-Method Compatibility Engine**
  - Fixed GKI KMI extraction (`6.6.127-android15-...` → `android15-6.6`, eliminating `· null`), fixed Samsung One UI integer decoding (`80100` → `8.1`), corrected and expanded Samsung (S22–S26, Z Fold/Flip 4–8, A34–A56, Tab S9/S10) + non-Samsung marketing names, added friendly SoC names, Bootloader, Knox, SELinux, and installed Manager version detection — all running off the main thread.
  - Collapsed Home card shows a 5-chip Compatibility Matrix (`KMI`, `ABI`, `Knox`, `SELinux`, `Manager`) and expands into full hardware telemetry with RAM/Storage bars and one-tap *Copy Diagnostics*.
- **Smart `ksud` Updater & Patcher Reliability Fixes**
  - Recognizes bundled `3.4.2-kdp-samsung` as up to date for upstream `v3.4.2` via semantic version comparison + background ETag checks, replacing the permanent blue button with a calm *"up to date"* strip that only expands into an *Update* CTA when a newer upstream release exists.
  - Fixed native `Inflater` leaks (~1.1M handles/run), 64 MB upfront buffer churn, the 256 KB tail scan blind spot, `compressBest()` slot-overflow guard, and Root My Galaxy's exact-size download off-by-one bug.

### Earlier Highlights (`v5.2.1`)

- **Full LSPosed, Vector (Zygisk) & Global Module Mount Support**
  - **Fixed `KSU_IOCTL_REPORT_EVENT` (`EVENT_SERVICES = 4`) across all 8 Samsung KDP+DEFEX `.ko` modules (`android12-5.10` → `android17-6.18`)**: KernelSU-Next `v3.4.2` (`UAPI v5`) verifies that `ioctl(KSU_IOCTL_REPORT_EVENT, &4)` returns `1` before running `/data/adb/service.d/*.sh` and module `service.sh` scripts. All 8 bundled Samsung KDP modules now return `1`, ensuring **LSPosed (`lspd`)**, **Vector**, and all service-stage root modules launch reliably.
  - **Fixed KernelSU-Next `late-load` Private Mount Namespace & Soft-Reboot Pipeline**: Patched `ksud` Next `3.4.2` and `3.4.0` (plus the in-app `DfKsudUpdater` patcher) so `late-load` transitions directly into `emulated_soft_reboot` (`switch_mnt_ns(1)` → `stop` → `on_post_fs_data` → `start` → `on_services` → `on_boot_completed`). This escapes the temporary `MS_PRIVATE` mount namespace into **PID 1's global mount namespace (`/proc/1/ns/mnt`)**, allowing Zygisk (`Vector` / `Zygisk Next` / `ReZygisk`), **LSPosed**, and metamodule overlay mounts (`metamount.sh`) to hook Zygote and persist across all apps.
- **Native KernelSU-Next `v3.4.2` (`33355`, UAPI v5) Support**
  - Added the built-in `3.4.2-kdp-samsung` profile and updated all 8 Samsung KDP+DEFEX `.ko` modules to report `version = 33355`, `uapi_version = 5`, and tag `v3.4.2` (eliminating the *"Kernel update required"* warning in KernelSU-Next Manager).
- **Persistent Release Keystore & Automated GitHub Releases**
  - Signed with a persistent release key so APK updates install cleanly in-place over previous versions without losing app settings or profiles.

---

## 🚀 Key Features

| Feature | Description |
| :--- | :--- |
| **⚡ New Method (Fast / Unprivileged)** | Powered by the **DirtyFrag (`CVE-2026-43284`)** engine — selected by default. Allocates an IPsec SA via Android's public `IpSecManager`, patches the kernel page cache, and late-loads `ksud` directly from memory. **No Shizuku, no ADB, no PC, and no device profile required** — just tap **Root now**. |
| **🛡️ Samsung KDP & DEFEX Bypass** | Includes 8 pre-patched Samsung KDP + DEFEX `kernelsu.ko` kernel modules (`5.10` to `6.18`) for both **KernelSU-Next** and **KernelSU Classic**, bypassing Samsung Knox Kernel Data Protection and DEFEX execution restrictions on stock kernels. |
| **📲 In-App Self-Updater** | Automatically checks `MahanYTT/UniRoot-Next` GitHub Releases with ETag caching, displays release notes, streams APK updates with SHA-256 and signing-certificate verification, and launches Android's installer in one tap. |
| **🔄 Smart `ksud` Updater & On-Device Patcher** | Automatically checks upstream `KernelSU-Next` / `KernelSU` releases against bundled & dynamic profiles, showing a calm *up to date* strip when current and offering one-tap on-device raw DEFLATE patching with the 8 Samsung KDP+DEFEX `.ko` modules whenever a newer version drops. |
| **🧩 Zygisk (Vector / ReZygisk) & LSPosed Ready** | Full lifecycle execution (`post-fs-data.d`, module `post-fs-data.sh`, `system.prop`, `sepolicy.rule`, `metamount.sh`, `service.d`, `service.sh`, and `boot-completed.sh`) inside PID 1's global mount namespace via automated `ksud` soft-reboot. |
| **🔁 Auto-Root on Boot & Instant Root** | Foreground boot service (`DfBootService`) and `DFReroot` second-stage helper automatically re-root your device on every boot right after the first screen unlock, keeping a partial WakeLock so rooting never stalls. |
| **🎛️ Dual Flavor & Profile Selector** | Switch seamlessly between **KernelSU-Next** (`com.rifsxd.ksunext`) and **KernelSU Classic** (`me.weishu.kernelsu`), and choose between built-in profiles (`3.4.2-kdp-samsung`, `3.4.0-kdp-samsung`, `3.3.0-s938x-proven`) or dynamically patched releases. |
| **🐢 Old Method (Classic Profiles)** | Retains full compatibility with the classic `CVE-2026-43499` pipeline (Local & Shizuku UID 2000 modes), validated device profiles, custom `.ko`/`ksud`/`.so` profile editor, and the **Root My Galaxy** online GitHub feed. |

---

## 📱 Supported Devices & Kernel KMI Compatibility

### 1. New Method (DirtyFrag `CVE-2026-43284` — Universal GKI 2.0)

The **New Method (Fast)** supports **all 8 Android GKI 2.0 Kernel Module Interfaces (KMIs)** bundled with Samsung KDP + DEFEX bypass modules:

| Android GKI KMI | Kernel Version | Android Version | Supported Samsung & GKI Devices (Examples) | KernelSU-Next | KernelSU Classic |
| :--- | :--- | :--- | :--- | :---: | :---: |
| **`android12-5.10`** | Linux `5.10.x` | Android 12+ | Galaxy S22 / S22+ / S22 Ultra (`SM-S901`, `SM-S906`, `SM-S908`), Galaxy Z Fold 4 / Flip 4 | ✅ `v3.4.2` | ✅ |
| **`android13-5.10`** | Linux `5.10.x` | Android 13+ | GKI 5.10 Android 13 devices | ✅ `v3.4.2` | ✅ |
| **`android13-5.15`** | Linux `5.15.x` | Android 13+ | Generic GKI 5.15 Android 13 devices *(Note: Galaxy S23 series is currently not supported)* | ⚠️ Bundled KMI (S23 unsupported) | ⚠️ |
| **`android14-5.15`** | Linux `5.15.x` | Android 14+ | GKI 5.15 Android 14 devices | ✅ `v3.4.2` | ✅ |
| **`android14-6.1`** | Linux `6.1.x` | Android 14+ | Galaxy S24 / S24+ / S24 Ultra (`SM-S921`, `SM-S926`, `SM-S928`), Galaxy Z Fold 6 / Flip 6 | ✅ `v3.4.2` | ✅ |
| **`android15-6.6`** | Linux `6.6.x` | Android 15 (One UI 7) | Galaxy S25 / S25+ / S25 Ultra (`SM-S931`, `SM-S936`, `SM-S938`), Galaxy Z Fold 7 / Flip 7 | ✅ `v3.4.2` | ✅ |
| **`android16-6.12`** | Linux `6.12.x` | Android 16 (One UI 8) | Galaxy S26 / S26+ / S26 Ultra (`SM-S948`), Galaxy Z Fold 8 (`SM-F976`, `SM-F971`) | ✅ `v3.4.2` | ✅ |
| **`android17-6.18`** | Linux `6.18.x` | Android 17+ | Next-gen GKI 6.18 devices | ✅ `v3.4.2` | ✅ |

> **Note:** The New Method requires a firmware security patch prior to the `CVE-2026-43284` kernel fix and an active GKI kernel matching one of the 8 KMIs above.

### 2. Old Method (Pre-Configured Device Profiles — `CVE-2026-43499`)

For devices using the classic profile-based method, the following validated profiles are bundled out of the box (plus automatic cloud matching via the **Root My Galaxy** GitHub feed):

| Profile Name | Target Device / Model | SoC | Kernel Build | Mode | Flavors Included |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`S93XX (Samsung S25)`** | Samsung Galaxy S25 / S25+ / S25 Ultra (`SM-S931x`, `SM-S936x`, `SM-S938x`) | Snapdragon 8 Elite | `6.6.x` | Local | KernelSU & KernelSU-Next |
| **`S25 6.6.127 ZZHL`** | Samsung Galaxy S25 Series (`ZZHL` One UI build) | Snapdragon 8 Elite | `6.6.127` | Local | KernelSU & KernelSU-Next |
| **`S25 6.6.127 ZZI4`** | Samsung Galaxy S25 Series (`ZZI4` One UI build) | Snapdragon 8 Elite | `6.6.127` | Local | KernelSU & KernelSU-Next |
| **`S26 Ultra 6.12.69 ZZHK`** | Samsung Galaxy S26 Ultra (`SM-S948x`, `ZZHK` build) | Snapdragon 8 Elite Gen 5 | `6.12.69` | Shizuku (`UID 2000`) | KernelSU & KernelSU-Next |
| **`F976X 6.12.58`** | Samsung Galaxy Z Fold 8 (`SM-F976x` / `SM-F971x`) | Snapdragon 8 Elite Gen 5 | `6.12.58` | Shizuku (`UID 2000`) | KernelSU & KernelSU-Next |
| **`Oppo X9`** | Oppo Find X9 Series | Dimensity / Snapdragon | `6.6.x` / `6.12.x` | Local / Shizuku | KernelSU |

---

## 🛠️ How to Root (Step-by-Step Guide)

### Prerequisites
1. Download and install the latest **[UniRoot Next APK](https://github.com/MahanYTT/UniRoot-Next/releases/latest)**.
2. Install the matching root manager app for your chosen flavor:
   - **KernelSU-Next Manager** (`com.rifsxd.ksunext`) — **[Download from KernelSU-Next Releases](https://github.com/KernelSU-Next/KernelSU-Next/releases)** *(Recommended: `v3.4.2`)*
   - **KernelSU Manager** (`me.weishu.kernelsu`) — **[Download from KernelSU Releases](https://github.com/tiann/KernelSU/releases)**

### Rooting with the New Method (Fast — Recommended)
1. Open **UniRoot Next** and select **New exploit (fast)** at the top.
2. Choose your preferred flavor: **KernelSU Next** (recommended) or **KernelSU**.
3. Verify the **ksud profile** selector is set to **`3.4.2 · kdp-samsung`** (or tap **⬇ Update ksud Next** to fetch and patch the newest upstream release on-device).
4. *(Optional)* Enable **Auto soft reboot** if you use **Zygisk (Vector / Zygisk Next / ReZygisk)**, **LSPosed**, or systemless overlay modules (`metamount.sh`).
5. *(Optional)* Enable **Start on boot** if you want **UniRoot Next** to automatically re-root your phone after every reboot.
6. Tap **Root now**.
   - Within a few seconds, the progress bar will advance through `IpSec SA allocated` → `SPI ready` → `ksud staged, arming exploit` → **`Phone rooted ✓`**.
   - If **Auto soft reboot** is enabled, `ksud` will automatically perform a clean userspace soft-reboot so Zygote restarts with all Zygisk hooks and module mounts active.

---

## ❓ FAQ & Troubleshooting

<details>
<summary><b>Q: KernelSU-Next Manager says "Kernel update required" or shows an older version. How do I fix it?</b></summary>

Make sure you are on **UniRoot Next `v5.2.1` or newer** and that the **`3.4.2 · kdp-samsung`** profile is selected under **ksud profile**. All 8 bundled Samsung KDP `.ko` modules report `version = 33355` (`v3.4.2`) and `uapi_version = 5`. If you already rooted earlier on this boot with an older profile, **reboot your phone once** and tap **Root now** again (a loaded kernel module cannot be hot-swapped without a reboot).
</details>

<details>
<summary><b>Q: LSPosed or Vector (Zygisk) shows "Not installed" or "Service not running" after rooting.</b></summary>

Ensure **Auto soft reboot** is turned **ON** on the UniRoot Next home screen before tapping **Root now**. Zygisk modules (`Vector`, `Zygisk Next`, `ReZygisk`) and `LSPosed` must hook Zygote at startup (`post-fs-data` → Zygote start → `service.sh`). With **Auto soft reboot** enabled, `ksud` automatically performs a clean userspace soft-reboot in PID 1's mount namespace right after loading `kernelsu.ko`.
</details>

<details>
<summary><b>Q: Does UniRoot Next trip Samsung Knox (0x1) or require an unlocked bootloader?</b></summary>

**No.** UniRoot Next loads KernelSU-Next / KernelSU in-memory via an unprivileged kernel vulnerability after boot. The bootloader remains locked, boot partitions (`boot.img` / `init_boot.img`) are untouched, and the physical Knox warranty bit remains `0x0`.
</details>

<details>
<summary><b>Q: Does root persist after a full device reboot?</b></summary>

Because the bootloader is locked and partitions are not modified, `kernelsu.ko` lives in RAM for the current boot session. However, enabling **Start on boot** (New Method) or **Instant Root** automatically re-roots the device in the background on every boot right after your first unlock.
</details>

---

## 🏗️ Building from Source

```powershell
git clone https://github.com/MahanYTT/UniRoot-Next.git
cd UniRoot-Next
./gradlew assembleRelease
```

The signed release APK will be output to `app/build/outputs/apk/release/app-release.apk`.

---

## 🙏 Credits & Acknowledgments

Developed & maintained by **[MahanYTT](https://github.com/MahanYTT)** ([MahanYTT/UniRoot-Next](https://github.com/MahanYTT/UniRoot-Next)).

This project builds upon the original **UniRoot** and stands on the shoulders of the Android kernel & rooting community:

- **[MahanYTT](https://github.com/MahanYTT)** — [UniRoot-Next](https://github.com/MahanYTT/UniRoot-Next): Lead developer & maintainer of UniRoot Next (KernelSU-Next `v3.4.2` UAPI v5 Samsung KDP/DEFEX module binary patches, `EVENT_SERVICES=4` & global mount namespace `emulated_soft_reboot` pipeline for Zygisk/Vector/LSPosed, in-app `ksud` updater & profile fixes, CI/CD release pipeline)
- **[kuuky](https://github.com/kuuky29)** — [UniRoot](https://github.com/kuuky29/UniRoot): Original UniRoot creator and V5 base architecture
- **[diabl0w](https://github.com/diabl0w)** — [DFRoot](https://github.com/diabl0w/DFRoot): The DirtyFrag (`CVE-2026-43284`) engine
- **[snothin](https://github.com/snothin)** — [GhostSam](https://github.com/snothin/GhostSam): Samsung KDP/DEFEX `.ko` modules, `ksud` DEFLATE patching approach, and staged native API
- **[polygraphene](https://github.com/polygraphene)** — DFRoot / DFReroot lineage and Samsung DEFEX bypass research
- **[rifsxd & the KernelSU-Next Team](https://github.com/KernelSU-Next)** — [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)
- **[tiann (weishu)](https://github.com/tiann)** — [KernelSU](https://github.com/tiann/KernelSU)
- **[YuKongA](https://github.com/YuKongA)** — [GhostLock](https://github.com/YuKongA/GhostLock) & [Miuix](https://github.com/YuKongA/miuix): Compose UI design & component library

---

<div align="center">
<sub>Keywords: Samsung Galaxy Root • Root Galaxy S25 Ultra without Bootloader Unlock • Root Galaxy S26 Ultra • Root Galaxy Z Fold 8 • One UI 7 Root • One UI 8 Root • KernelSU-Next v3.4.2 Samsung KDP DEFEX • DirtyFrag CVE-2026-43284 • CVE-2026-43499 • Zygisk Vector LSPosed Samsung • GKI 2.0 LKM Late-Load Root • Knox 0x0 Root</sub>
</div>
