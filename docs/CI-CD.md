# CI/CD

Cada tag `v*` dispara un workflow que compila, firma y publica el APK automáticamente.

## Flujo

```
Tag v* → Checkout → JDK 17 → Keystore → Compilar → Release → version.json
```

Duración típica: 2-4 minutos.

## Triggers

| Evento | Acción |
|---|---|
| Push a main, develop, feature/*, fix/* | Compila y sube artifact |
| Push de tag v* | Release + actualiza version.json |
| Pull Request | Compila (valida) |

## Secrets necesarios

En Settings → Secrets and variables → Actions:

| Secret | Contenido |
|---|---|
| KEYSTORE_BASE64 | Salida de base64 -w0 upr-proxy.jks |
| KEYSTORE_PASSWORD | Contraseña del keystore |
| KEY_ALIAS | Alias (ej. upr-proxy) |
| KEY_PASSWORD | Contraseña de la clave |

## Crear el keystore

```bash
keytool -genkeypair -v -keystore upr-proxy.jks -alias upr-proxy -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 upr-proxy.jks > keystore.b64
```

Copia keystore.b64 al secret KEYSTORE_BASE64.

## Resultado

- Sin tag: APK como artifact (90 días).
- Con tag v*: Release público + version.json actualizado.
