# Changelog

Todos los cambios importantes de este proyecto se documentan aquí.

Formato: [Keep a Changelog](https://keepachangelog.com/es/1.1.0/)
Versionado: [Semantic Versioning](https://semver.org/lang/es/)

## [Unreleased]

### Próximo (1.0.0-beta2)
- Puente SOCKS5 -> HTTP para que el tráfico realmente salga por el proxy
- Pantalla de logs en vivo
- Botón "Probar conexión"

---

## [1.0.0-beta1] — 2026-10-10

### Añadido
- Interfaz gráfica con logo institucional UPR
- 4 campos: Servidor, Puerto, Usuario, Contraseña
- Guardado de credenciales con SharedPreferences
- Botón conectar/desconectar con cambio de color
- Indicador de estado (Conectado / Desconectado)
- Servicio VPN basado en VpnService
- Notificación persistente mientras la VPN está activa
- Librería nativa hev-socks5-tunnel (AAR)
- Sistema de versiones visible en la app
- Compatible con Android 8.0+ (API 26)
- Build automático con GitHub Actions

### Pendiente
- El tráfico aún no se enruta al proxy (falta el puente SOCKS5->HTTP)
