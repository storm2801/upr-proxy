<div align="center">

<img src="docs/screenshots/logo.png" alt="UPR Proxy" width="120">

# UPR Proxy

**Corporate proxy for Android from the University of Pinar del Río.**

[![Build](https://github.com/storm2801/upr-proxy/actions/workflows/build.yml/badge.svg)](https://github.com/storm2801/upr-proxy/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/storm2801/upr-proxy?color=107530)](https://github.com/storm2801/upr-proxy/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-107530.svg)](LICENSE)

[Español](README.md) · [English](README_EN.md)

</div>

---

## What is it?

Android app that routes **all phone traffic** through the UPR proxy (`proxy.upr.edu.cu:8080`) without touching the WiFi settings.

## Compatibility

| | |
|---|---|
| ✅ **Android** | 8.0 or later |
| ✅ **Architecture** | ARMv7 / ARM64 |
| ✅ **Network** | UPR WiFi |
| ❌ **Not compatible** | iOS, Windows, macOS, Linux, Android TV |

## Screenshot

<div align="center">
<img src="docs/screenshots/main-disconnected.jpg" alt="Main screen" width="280">
</div>

## Installation

**1. Download the APK**

https://github.com/storm2801/upr-proxy/releases/latest

**2. Temporarily disable Play Protect**

Play Store → profile → Play Protect → ⚙️ → turn off "Scan apps".

**3. Allow unknown apps**

Settings → Apps → Special access → Install unknown apps → pick your browser → enable.

**4. Install the APK**

File manager → Downloads → tap the APK → Install.

**5. Configure**

| Field | Value |
|---|---|
| Server | `proxy.upr.edu.cu` |
| Port | `8080` |
| User | your UPR username |
| Password | your proxy password |

Tap **Connect** and accept the VPN permission.

## How it works

The app creates a local VPN that captures all phone traffic, routes it through an internal SOCKS5 bridge, and sends it to the UPR proxy with Basic authentication.

## Build

Requires JDK 17 and Android SDK 34.

```bash
git clone https://github.com/storm2801/upr-proxy.git
cd upr-proxy
./gradlew assembleRelease
```

## Documentation

- [Installation](docs/INSTALL.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Setup](docs/SETUP.md)
- [Branches](docs/BRANCHES.md)
- [CI/CD](docs/CI-CD.md)

## License

MIT — see [LICENSE](LICENSE).

**Author:** [@storm2801](https://github.com/storm2801)
