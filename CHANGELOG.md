# Changelog

Todos los cambios importantes del proyecto se documentan aquí.

Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).
Versionado: [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased]

---

## [1.0.0-beta17] — 2026-10-11

### Añadido
- Documentación completa: README bilingüe, arquitectura, setup, instalación,
  ramas, CI/CD, código de conducta y política de seguridad.
- Plantillas de GitHub para PRs e issues.
- Script de limpieza de ramas.

### Cambiado
- Se permiten capturas de pantalla (eliminado `FLAG_SECURE`).

---

## [1.0.0-beta16] — 2026-10-11

### Cambiado
- Revertido el fondo animado de puntitos. Se mantiene la interfaz limpia
  con inputs sin iconos y placeholder `********`.

---

## [1.0.0-beta15] — 2026-10-11

### Añadido
- Fondo animado de puntitos (DotFieldView).
- Inputs sin iconos.

### Notas
- Revertido en beta16 por problemas de legibilidad.

---

## [1.0.0-beta14] — 2026-10-11

### Añadido
- Nuevo diálogo de actualización rediseñado.

### Cambiado
- Flujo de actualización: permiso → descarga → instalación.
- `quickToast()` cancela el toast anterior.
- Menú dev reorganizado en 4 secciones.

### Arreglado
- Botón "Salir" del header de Logs eliminado.

---

## [1.0.0-beta13] — 2026-10-11

### Añadido
- `UpdateDialog.showPermissionRequired()` con explicación clara.
- Doble check del permiso antes de abrir el instalador.

### Arreglado
- Botones del menú dev reescritos sin `Widget.Material3.*`.

---

## [1.0.0-beta12] — 2026-10-11

### Añadido
- `quickToast()` para evitar el encolado de toasts.

### Cambiado
- Menú dev reorganizado en 4 secciones.

---

## [1.0.0-beta11] — 2026-10-10

### Añadido
- Sistema de actualización automática completo.
- Chequeo silencioso al arrancar.
- `version.json` público.

---

## [1.0.0-beta10] — 2026-10-10

### Añadido
- Menú de diagnóstico completo.

### Arreglado
- El texto "dev" ya no queda pegado al salir del modo.

---

## [1.0.0-beta9] — 2026-10-10

### Añadido
- Splash screen nativo (Android 12+).
- Transiciones slide-in.
- `SparklineView` con velocidad en los últimos 30 s.
- Filtros de logs por nivel.

---

## [1.0.0-beta8] — 2026-10-10

### Añadido
- Onboarding de 3 slides.
- Diálogos de error personalizados.
- Animaciones (pulso, vibración, escala).

---

## [1.0.0-beta7] — 2026-10-10

### Cambiado
- Workflow compila `assembleRelease` firmado.
- El keystore se reconstruye desde los Secrets de GitHub.

---

## [1.0.0-beta6] — 2026-10-10

### Añadido
- Paleta oficial UPR.
- 4 iconos vectoriales en los inputs.
- Notificación rediseñada.

### Cambiado
- Tema claro forzado.

---

## [1.0.0-beta5] — 2026-10-10

### Seguridad
- Credenciales cifradas con `EncryptedSharedPreferences`.
- `allowBackup=false`.
- `FLAG_SECURE` en `MainActivity` y `LogsActivity`.
- Autofill deshabilitado.
- Portapapeles con limpieza automática en 60 s.

---

## [1.0.0-beta4] — 2026-10-10

### Arreglado
- La VPN ya no bloquea al desconectar.
- El servicio no se relanza solo.
- El botón "Desconectar" de la notificación funciona.
- Se verifica el proxy antes de conectar.

---

## [1.0.0-beta3] — 2026-10-10

### Añadido
- Estado en vivo.
- Botón "Probar conexión al proxy".
- Auto-reconexión.
- Notificación con botón "Desconectar".

---

## [1.0.0-beta2] — 2026-10-10

### Añadido
- Puente SOCKS5→HTTP.
- Pantalla de logs.
- `LogBuffer` y `L`.

---

## [1.0.0-beta1] — 2026-10-10

### Añadido
- Interfaz Android con logo UPR.
- 4 campos: Servidor, Puerto, Usuario, Contraseña.
- Servicio VPN con notificación persistente.
- Compatible con Android 8.0+ (API 26).
- Build automático con GitHub Actions.
