# Changelog

Todos los cambios importantes de este proyecto se documentan aquí.

Formato: [Keep a Changelog](https://keepachangelog.com/es/1.1.0/)
Versionado: [Semantic Versioning](https://semver.org/lang/es/)

## [Unreleased]

---

## [1.0.0-beta2] — 2026-10-10

### Añadido
- **Puente SOCKS5→HTTP** que permite que el tráfico del móvil salga
  realmente por `proxy.upr.edu.cu:8080` con autenticación básica.
- **Pantalla de logs** oculta tras un "menú de desarrollador":
  - Toca 7 veces el texto de la versión para activarla.
  - Después, cada toque en la versión abre los logs en vivo.
  - Botones "Copiar" y "Borrar".
- **LogBuffer y L**: sistema de logs en memoria que alimenta la pantalla.
- Workflow de GitHub Actions compila también en ramas `feature/*` y `fix/*`.

### Cambiado
- `ProxyForwarder` ahora orquesta el puente y el túnel nativo.
- `ProxyVpnService` guarda el forwarder para hacer shutdown limpio y
  maneja `onRevoke()` del sistema.

### Conocido
- El funcionamiento real del proxy todavía no se ha probado en la red de la UPR.
- Si el proxy no soporta autenticación `Basic`, habrá que añadir NTLM.

---

## [1.0.0-beta1] — 2026-10-10

### Añadido
- Interfaz gráfica con logo institucional UPR.
- 4 campos: Servidor, Puerto, Usuario, Contraseña.
- Guardado de credenciales con SharedPreferences.
- Servicio VPN basado en VpnService.
- Notificación persistente.
- Librería nativa hev-socks5-tunnel (AAR).
- Compatible con Android 8.0+ (API 26).
- Build automático con GitHub Actions.

### Pendiente
- El tráfico no se enruta al proxy (se resolvió en beta2).
