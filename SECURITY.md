# Política de seguridad

## 🔐 Versiones soportadas

Solo la **última versión publicada** recibe parches de seguridad.

| Versión ↕▾ | Soportada ↕▾ |
|---|---|
| `1.0.0-beta16` (última) | ✅ |
| Versiones anteriores | ❌ |

---

## 🐛 Reportar una vulnerabilidad

Si encuentras un problema de seguridad **no lo publiques como issue**.
Envíalo de forma privada siguiendo uno de estos canales:

### Opción 1 — GitHub Security Advisories (recomendado)

Usa el formulario oficial:
👉 [Report a vulnerability](https://github.com/storm2801/upr-proxy/security/advisories/new)

Es privado, cifrado y permite coordinar la divulgación.

### Opción 2 — Contacto directo

Si prefieres el correo, escribe a `storm2801` (contacto vía perfil de
GitHub: [@storm2801](https://github.com/storm2801)).

### Qué incluir

Para acelerar la respuesta, incluye:

1. **Descripción:** qué es lo que falla.
2. **Impacto:** qué puede conseguir un atacante.
3. **Pasos para reproducir:** idealmente con una prueba de concepto.
4. **Versión afectada** de la app.
5. **Dispositivo y versión de Android.**
6. **Mitigaciones conocidas** (si las conoces).
7. **Cómo quieres ser acreditado** (nombre, alias, o anónimo).

---

## ⏱️ Tiempos de respuesta

| Etapa ↕▾ | Tiempo objetivo ↕▾ |
|---|---|
| Acuse de recibo | ≤ 48 h |
| Evaluación inicial | ≤ 7 días |
| Corrección (parche) | ≤ 30 días |
| Divulgación pública | Coordinada contigo |

---

## 🎯 Alcance

### En scope

- Fallos en el manejo de credenciales (almacenamiento, transmisión).
- Bypass del cifrado de `EncryptedSharedPreferences`.
- Fuga de datos a través de logs, notificaciones o archivos temporales.
- Ejecución de código remoto.
- Escalada de privilegios dentro de la app.
- Vulnerabilidades en la lógica del puente SOCKS5→HTTP.
- Manipulación del sistema de actualización (MITM en `version.json`).

### Fuera de scope

- Ataques que requieren un dispositivo rooteado.
- Vulnerabilidades del **proxy corporativo de la UPR** (repórtalas al
  área de redes de la universidad).
- Vulnerabilidades de la librería nativa `hev-socks5-tunnel`
  (repórtalas al proyecto original).
- Ingeniería social.
- Ataques DoS locales (el modelo de amenaza del proyecto no cubre
  resistencia a DoS).
- Falta de hardening en versiones antiguas de Android (solo se
  soporta API 26+).

---

## 🛡️ Medidas de seguridad implementadas

Para referencia, la app ya aplica:

| Medida ↕▾ | Implementación ↕▾ |
|---|---|
| Cifrado de credenciales en reposo | `EncryptedSharedPreferences` + AES-256-GCM + MasterKey del sistema |
| Bloqueo de backup | `allowBackup=false` + `dataExtractionRules` |
| Bloqueo de screenshots | `FLAG_SECURE` en actividades sensibles |
| Autofill deshabilitado | `importantForAutofill="no"` en los inputs |
| Tráfico cifrado | `networkSecurityConfig` bloquea cleartext excepto al proxy |
| Limpieza del portapapeles | Auto-limpieza 60 s tras copiar logs |
| Componentes no exportados | `exported=false` en todas las activities internas |
| Firma verificada | APK firmado con keystore controlado por el mantenedor |
| Actualización autenticada | HTTPS + GitHub Releases |

---

## 📜 Divulgación responsable

Pedimos a los investigadores que:

- Nos den tiempo razonable para parchear antes de publicar.
- No exploten la vulnerabilidad más allá de lo necesario para
  demostrarla.
- No accedan a datos de terceros.
- No realicen ataques DoS.

En contrapartida, nos comprometemos a:

- Responder en los plazos indicados.
- Acreditar al investigador en el `CHANGELOG.md` y en las notas del
  Release correspondiente (si así lo desea).
- No emprender acciones legales contra investigadores que actúen de
  buena fe.

---

## 🏆 Reconocimientos

Investigadores que hayan reportado vulnerabilidades válidas:

*(Ninguno todavía. Sé el primero en aparecer aquí.)*

---

Gracias por ayudar a mantener UPR Proxy seguro. 🔒
