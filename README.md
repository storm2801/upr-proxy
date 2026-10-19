<div align="center">

<img src="docs/screenshots/logo.png" alt="UPR Proxy" width="120">

# UPR Proxy

**Proxy corporativo para Android de la Universidad de Pinar del Río.**

[![Build](https://github.com/storm2801/upr-proxy/actions/workflows/build.yml/badge.svg)](https://github.com/storm2801/upr-proxy/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/storm2801/upr-proxy?color=107530)](https://github.com/storm2801/upr-proxy/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-107530.svg)](LICENSE)

[Español](README.md) · [English](README_EN.md)

</div>

---

## ¿Qué es?

App Android que enruta **todo el tráfico del teléfono** por el proxy de la UPR (`proxy.upr.edu.cu:8080`) sin configurar nada en el WiFi.

## Compatibilidad

| | |
|---|---|
| ✅ **Android** | 8.0 o superior |
| ✅ **Arquitectura** | ARMv7 / ARM64 |
| ✅ **Red** | WiFi de la UPR |
| ❌ **No funciona** | iOS, Windows, macOS, Linux, Android TV |

## Captura

<div align="center">
<img src="docs/screenshots/main-disconnected.jpg" alt="Pantalla principal" width="280">
</div>

## Instalación

**1. Descarga el APK**

https://github.com/storm2801/upr-proxy/releases/latest

**2. Configura los permisos**

Al instalar, asegúrate de otorgar los permisos de batería necesarios:
Ajustes → Aplicaciones → UPR Proxy → Batería → **"Sin restricciones"** (Crucial en Samsung/Android 12+).

**3. Conecta**

Abre la app, introduce tu usuario y contraseña, y pulsa **Conectar**. Acepta la solicitud de conexión VPN. La app se encargará de gestionar el enrutamiento correctamente.

## Cómo funciona

La app crea una VPN local que captura todo el tráfico del teléfono, lo pasa por un puente SOCKS5 interno y lo envía al proxy de la UPR con autenticación Basic.

## Compilar

Requiere JDK 17 y Android SDK 34.

```bash
git clone https://github.com/storm2801/upr-proxy.git
cd upr-proxy
./gradlew assembleRelease
```

## Documentación

- [Instalación detallada](docs/INSTALL.md)
- [Arquitectura](docs/ARCHITECTURE.md)
- [Setup](docs/SETUP.md)
- [Ramas](docs/BRANCHES.md)
- [CI/CD](docs/CI-CD.md)

## Licencia

MIT — ver [LICENSE](LICENSE).

**Autor:** [@storm2801](https://github.com/storm2801)
