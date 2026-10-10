<p align="center">
  <img src="docs/logo.png" alt="Logo UPR" width="180">
</p>

<h1 align="center">UPR Proxy</h1>

<p align="center">Proxy corporativo de la Universidad de Pinar del Río para Android</p>

---

## Objetivo

Enruta todo el tráfico del teléfono a través del proxy corporativo
de la UPR (`proxy.upr.edu.cu:8080`), sin configurar el proxy en el WiFi.

## Características

- Interfaz simple con 4 campos: Servidor, Puerto, Usuario, Contraseña
- Guarda los datos en el teléfono para no escribirlos cada vez
- Un solo botón para conectar/desconectar
- VPN local que captura el tráfico y lo redirige al proxy HTTP
- Compatible con Android 8.0 (API 26) en adelante
- Sin publicidad, sin telemetría

## Uso

1. Instala el APK en el teléfono
2. Conéctate al WiFi de la UPR
3. Abre la app y rellena:
   - Servidor: `proxy.upr.edu.cu`
   - Puerto: `8080`
   - Usuario: tu usuario de la UPR
   - Contraseña: tu contraseña del proxy
4. Pulsa Conectar y acepta el permiso de VPN

## Compilar

Requisitos: Linux, JDK 17, Android SDK (build-tools 34.0.0, platform android-34)

    git clone https://github.com/storm2801/upr-proxy.git
    cd upr-proxy
    bash scripts/setup-sdk.sh
    bash scripts/build.sh

El APK queda en `UPRProxy-<version>.apk`.

## Ramas

- `main` — versiones estables
- `develop` — trabajo en curso
- `feature/*` — funciones nuevas
- `fix/*` — correcciones

## Versiones

Ver [CHANGELOG.md](CHANGELOG.md).

- **1.0.0-beta1** — Esqueleto de la app, UI completa, VPN sin enrutar

## Licencia

MIT — ver [LICENSE](LICENSE).

## Autor

Desarrollado en el área de informatización de la UPR por **mr_manu**.
