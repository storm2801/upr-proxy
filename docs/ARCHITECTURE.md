# Arquitectura

## Flujo del tráfico

```
Apps → VPN local → SOCKS5 → Proxy UPR → Internet
```

La app crea una VPN local que captura todo el tráfico IP, lo pasa por un SOCKS5 interno y lo envía al proxy con autenticación Basic.

## Componentes

| Clase | Función |
|---|---|
| `MainActivity` | Formulario, verificación del proxy, métricas. |
| `ProxyVpnService` | Crea la interfaz TUN, mantiene la notificación. |
| `ProxyForwarder` | Arranca el puente SOCKS5 y el motor nativo. |
| `Socks5HttpBridge` | SOCKS5 en 127.0.0.1:1080 → CONNECT al proxy. |
| `ProxyStatus` | Estado global: conexiones, bytes, velocidad. |
| `TestConnection` | CONNECT de prueba sin activar la VPN. |
| `SecurePrefs` | Credenciales cifradas (AES-256-GCM). |
| `UpdateChecker` | Consulta version.json en el repo público. |

## Flujo de conexión

1. Usuario pulsa **Conectar**.
2. Se hace una prueba de conexión al proxy.
3. Si responde, se pide permiso de VPN al sistema.
4. Se crea la interfaz TUN.
5. Se arranca el puente SOCKS5.
6. El tráfico empieza a fluir.

## Seguridad

| Medida | Implementación |
|---|---|
| Credenciales cifradas | EncryptedSharedPreferences + AES-256-GCM |
| Sin backup | `allowBackup=false` |
| Sin autofill | `importantForAutofill=no` |
| HTTPS obligatorio | `networkSecurityConfig` |
| Componentes internos | `exported=false` |
| Portapapeles | Limpieza a los 60 s |
