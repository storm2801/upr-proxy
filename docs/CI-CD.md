# CI/CD — Integración y entrega continua

Pipeline de GitHub Actions que compila, firma y publica cada release.

## 🎯 Objetivo

Automatizar todo desde un comando:

    git tag v1.0.0-betaN
    git push origin v1.0.0-betaN

En 2-4 minutos:
1. Compila el APK release firmado.
2. Crea un Release público en GitHub.
3. Sube el APK como asset descargable.
4. Actualiza version.json en main.
5. Hace commit y push automático.

## 📄 Workflow

Ubicación: .github/workflows/build.yml

Triggers:

| Evento | Acción |
|---|---|
| Push a main, develop, feature/**, fix/** | Compila y sube artifact |
| Push de tag v* | Compila + Release + version.json |
| Pull Request a main o develop | Compila (valida) |
| Manual (workflow_dispatch) | Lanzar a mano |

## 🔧 Pasos del job

1. Checkout del repositorio (fetch-depth: 0)
2. Configurar JDK 17 (temurin)
3. Cache de Gradle
4. Restaurar keystore desde secretos (base64 → jks)
5. Crear keystore.properties
6. Compilar: ./gradlew assembleRelease
7. Renombrar APK con la versión
8. Subir como artifact (90 días)
9. Crear Release (solo tags v*)
10. Actualizar version.json (solo tags v*)

## 🔐 Secrets necesarios

Configúralos en Settings → Secrets and variables → Actions:

| Secret | Cómo generarlo |
|---|---|
| KEYSTORE_BASE64 | base64 -w0 upr-proxy.jks |
| KEYSTORE_PASSWORD | Contraseña del keystore |
| KEY_ALIAS | Alias (ej. upr-proxy) |
| KEY_PASSWORD | Contraseña de la clave |

Crear keystore:

    keytool -genkeypair -v -keystore upr-proxy.jks -alias upr-proxy -keyalg RSA -keysize 2048 -validity 10000
    base64 -w0 upr-proxy.jks > keystore.b64

NUNCA subir upr-proxy.jks ni keystore.properties al repositorio.

## 📦 Resultados

Build normal: APK como artifact en Actions (90 días).
Build con tag v*: Release público + version.json actualizado + diálogo de actualización en los móviles.

## 🚨 Solución de problemas

**Permission denied: ./gradlew** → Verifica que gradlew está commiteado.

**Keystore tampered** → KEYSTORE_BASE64 incompleto o contraseña mal.

**SDK location not found** → El runner ya incluye SDK en /usr/local/lib/android/sdk.

**Tag no dispara workflow** → El tag debe empezar con v y estar pusheado.

## 📊 Duración típica

- Checkout + setup: ~15 s
- Cache Gradle: ~5 s
- Compilar release: ~1 min 30 s
- Renombrar + subir: ~10 s
- Crear Release: ~10 s
- Actualizar version.json: ~15 s
- Total: ~2 min 30 s

## 🔗 Referencias

- GitHub Actions: https://docs.github.com/en/actions
- softprops/action-gh-release: https://github.com/softprops/action-gh-release
