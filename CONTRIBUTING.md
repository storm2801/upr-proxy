# Guía de contribución

¡Gracias por tu interés en mejorar **UPR Proxy**! Este documento describe el flujo de trabajo, las convenciones y los estándares del proyecto.

---

## 📋 Antes de empezar

- Lee el [`README.md`](README.md) y [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) para entender el proyecto.
- Revisa los [issues abiertos](https://github.com/storm2801/upr-proxy/issues) para no duplicar trabajo.
- Asegúrate de tener el entorno configurado según [`docs/SETUP.md`](docs/SETUP.md).

---

## 🌳 Flujo de trabajo con ramas

Este proyecto sigue un esquema **Git Flow simplificado**:

```

main ──────●────────────────●────●───────●────►  (tags / releases)
\              /    /       /
develop      ●───●───●────●────●───────●───────►  (integración)
\     /    \     /
feature/x          ●───●      ●───●              (una función por rama)

```

### Reglas

1. **Nunca trabajes directamente sobre `main`.**
2. Toda función o corrección se desarrolla en una rama a partir de `develop`:
   - `feature/nombre-descriptivo` — nueva funcionalidad.
   - `fix/nombre-descriptivo` — corrección de bug.
   - `docs/nombre-descriptivo` — solo documentación.
   - `refactor/nombre-descriptivo` — reorganización sin cambio de comportamiento.
3. Cuando termines, abre un **Pull Request** hacia `develop`.
4. Solo los mantenedores hacen merge de `develop` a `main` (mediante tag).

### Crear una rama nueva

```bash
git checkout develop
git pull origin develop
git checkout -b feature/mi-funcion
```

---

## ✍️ Convención de commits

Se sigue la especificación [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
<tipo>(<ámbito opcional>): <descripción corta>

[cuerpo opcional]

[footer opcional]
```

### Tipos permitidos

| Tipo ↕▾ ↕▾ | Uso ↕▾ ↕▾ |
|---|---|
| −`feat` | Nueva funcionalidad |
| −`fix` | Corrección de bug |
| −`docs` | Cambios solo en documentación |
| −`style` | Formato, espacios, sin cambio funcional |
| −`refactor` | Reorganización de código sin cambiar comportamiento |
| −`perf` | Mejora de rendimiento |
| −`test` | Añadir o corregir tests |
| −`build` | Cambios en build/Gradle |
| −`ci` | Cambios en CI/CD (workflows) |
| −`chore` | Mantenimiento general |
⚙

### Ejemplos

```
git commit -m "feat(ui): añadir indicador de velocidad en la notificación"
git commit -m "fix(vpn): cerrar sockets al desconectar correctamente"
git commit -m "docs: actualizar guía de setup con requisitos de JDK"
```

---

## 📝 Pull Requests

Al abrir un PR, se cargará automáticamente la plantilla de `.github/PULL_REQUEST_TEMPLATE.md`. Rellena todas las secciones.

### Antes de abrir el PR

- □  
El código compila localmente: `./gradlew assembleRelease`.
- □  
Se ha probado en un dispositivo o emulador Android.
- □  
La documentación afectada está actualizada (README, CHANGELOG, etc.).
- □  
El commit sigue la convención de arriba.
- □  
No se han añadido credenciales ni secrets al repositorio.
- □  
El changelog (`CHANGELOG.md`) está actualizado si aplica.

### Revisión

- Un mantenedor revisará el PR y podrá pedir cambios.
- Se puede tardar **1-3 días** en responder. Ten paciencia.
- Si no hay actividad después de una semana, puedes mencionar al autor con `@storm2801`.

---

## 🎨 Estilo de código

### Java (Android)

- Seguir el [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- Indentación: **4 espacios**.
- Nombres:

- Clases: `PascalCase`
- Métodos y variables: `camelCase`
- Constantes: `UPPER_SNAKE_CASE`
- Evitar código muerto o comentarios innecesarios.
- Los logs internos usan la clase `L` (envuelve `android.util.Log` + `LogBuffer`).

### XML (layouts)

- Indentación: **4 espacios**.
- Atributos ordenados: primero `android:id`, luego `android:layout_*`, luego `android:*`, finalmente `app:*`.
- Colores y cadenas **siempre** mediante referencias (`@color/`, `@string/`). Nunca valores literales.

---

## 🔐 Información sensible

**Nunca subas al repositorio:**

- `keystore.properties`
- Archivos `.jks` o `.keystore`
- Contraseñas, tokens o claves API
- Datos reales de usuarios (nombres, correos, etc.)
- URLs privadas o endpoints internos no documentados públicamente

El archivo `.gitignore` ya excluye estos archivos. Verifica con `git status` antes de cada commit.

Si por error subes algo sensible:

1. **Rotación inmediata:** cambia la contraseña o revoca la clave filtrada.
2. Limpia el historial con [`git-filter-repo`](https://github.com/newren/git-filter-repo).
3. Fuerza el push: `git push --force-with-lease origin <rama>`.
4. Notifica al mantenedor en privado.

---

## 🧪 Tests

Actualmente el proyecto no tiene tests automatizados. Se aceptan contribuciones que añadan:

- Tests unitarios con **JUnit 4** (clases de lógica pura: `Socks5HttpBridge`, `ProxyStatus`, `UpdateChecker`).
- Tests instrumentados con **Espresso** para la UI (opcional).

Cuando se añada infraestructura de tests, se documentará aquí.

---

## 🐛 Reportar bugs

Usa la [plantilla de bug](https://github.com/storm2801/upr-proxy/issues/new?template=bug_report.md). Incluye:

- Versión de la app (visible en la pantalla principal).
- Modelo de dispositivo y versión de Android.
- Pasos para reproducir.
- Logs (si puedes obtenerlos con `adb logcat`).
- Captura de pantalla o vídeo si aplica.

---

## 💡 Sugerir funciones

Usa la [plantilla de feature](https://github.com/storm2801/upr-proxy/issues/new?template=feature_request.md). Describe:

- El problema que resuelve.
- Cómo lo usaría un usuario típico.
- Alternativas consideradas.

---

## 📜 Licencia

Al contribuir, aceptas que tu código se distribuya bajo la licencia [MIT](https://license/) del proyecto.

---

¡Gracias por ayudar a mejorar UPR Proxy! 💚

</BDS:create_file>

<BDS:create_file fileName="CHANGELOG.md">

```markdown
# Changelog

Todos los cambios importantes de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Planeado
- Tests unitarios para `Socks5HttpBridge` y `ProxyStatus`.
- Icono adaptativo redondeado para el launcher.
- Modo multi-perfil (guardar varias credenciales).
- Exportar/importar configuración por QR.

---

## [1.0.0-beta16] — 2026-10-11

### Cambiado
- **Revertido:** se elimina el fondo animado de puntitos (DotFieldView).
  El diseño vuelve al fondo claro simple.
- Se mantiene la interfaz limpia: **inputs sin iconos** y **placeholder
  de contraseña con 8 asteriscos**.

### Eliminado
- `DotFieldView.java` (vista personalizada del fondo animado).
- `top_veil_gradient.xml` (velo blanco superior).
- Efecto cristal en las tarjetas (vuelven a fondo blanco opaco).

---

## [1.0.0-beta15] — 2026-10-11

### Añadido
- **Fondo animado de puntitos** (DotFieldView) con efecto puntillista
  tipo lámpara de lava sobre el fondo de la pantalla principal.
- Paleta de 8 verdes armónicos.
- Inputs sin iconos (diseño más limpio).
- Placeholder de contraseña cambiado a `********`.

### Cambiado
- Tarjetas con ligera transparencia (efecto cristal).

### Notas
- Revertido en beta16 por problemas de legibilidad del header.

---

## [1.0.0-beta14] — 2026-10-11

### Añadido
- Nuevo diálogo de actualización rediseñado: cabecera verde con icono,
  comparación visual "instalada → nueva", changelog con bullets
  automáticos y fecha en español.

### Cambiado
- **Flujo de actualización reordenado:** ahora se pide el permiso de
  instalación **antes** de descargar. Al volver de Ajustes con el
  permiso concedido, la descarga arranca sola.
- `MainActivity` chequea actualizaciones en cada apertura y muestra el
  diálogo si hay novedad.
- Menú de diagnóstico: quitados los toasts `"Faltan N"`. Ahora solo
  aparece `"Modo desarrollador activado"`.
- `quickToast()` cancela el toast anterior para mostrar uno nuevo en
  menos de 1 segundo (antes se encolaban y tardaban 2 s cada uno).

### Arreglado
- El botón "Salir" del header de Logs se elimina (salía del modo
  diagnóstico sin querer).

---

## [1.0.0-beta13] — 2026-10-11

### Añadido
- `UpdateDialog.showPermissionRequired()`: diálogo con explicación clara
  del permiso de instalación y apertura directa de Ajustes.
- `installApk()`: doble check defensivo del permiso antes de abrir el
  instalador.
- Logging exhaustivo en `DevMenuActivity` para cada acción.

### Arreglado
- **Menú de diagnóstico:** botones reescritos con atributos nativos
  de MaterialButton (borde verde explícito). Antes usaban
  `Widget.Material3.Button.OutlinedButton`, que en algunos
  dispositivos no cargaba y los dejaba como texto plano.

---

## [1.0.0-beta12] — 2026-10-11

### Añadido
- `quickToast()`: sistema de toasts que cancela el anterior antes de
  mostrar el nuevo.

### Cambiado
- Menú de diagnóstico reorganizado en **4 secciones**:
  ACTUALIZACIÓN / DIAGNÓSTICO / VPN / DATOS.
- Encabezados de sección en mayúsculas verde suave.

### Arreglado
- Botones del menú de diagnóstico que parecían no responder por el
  encolado de toasts.

---

## [1.0.0-beta11] — 2026-10-10

### Añadido
- **Sistema de actualización completo:**
  - `UpdateChecker`: consulta `version.json` en el repo público y
    compara `versionCode`.
  - `UpdateDownloader`: descarga el APK con barra de progreso.
  - `UpdateDialog`: diálogo con changelog y apertura del instalador
    vía `FileProvider`.
  - `REQUEST_INSTALL_PACKAGES` + `FileProvider` con `file_paths.xml`.
- Botón "Buscar actualizaciones" en el menú de diagnóstico.
- Chequeo silencioso al arrancar (máx. 1 vez cada 6 horas).
- `version.json` actualizado a beta11.

---

## [1.0.0-beta10] — 2026-10-10

### Añadido
- Menú de diagnóstico completo con:
  - Estado en vivo: VPN, conexiones, datos, versión, dispositivo.
  - Diagnóstico: ver logs, copiar logs, probar conexión, info sistema.
  - Mantenimiento: forzar reconexión, borrar logs, borrar credenciales,
    volver a mostrar onboarding.
  - Salir del modo con confirmación.

### Cambiado
- Toasts del menú de diagnóstico acortados (SHORT en vez de LONG).
- Al tocar la versión con modo activo, ahora abre el **menú** en lugar
  de ir directo a los logs.

### Arreglado
- El texto "dev" ya no queda pegado al salir del modo (mostrarVersion()
  ahora se llama en `onResume`).

---

## [1.0.0-beta9] — 2026-10-10

### Añadido
- Splash screen nativo (Android 12+).
- Transiciones slide-in entre actividades.
- ProgressBar circular en el botón al verificar el proxy.
- `SparklineView`: gráfico en tiempo real de los últimos 30 puntos de
  velocidad.
- Contadores separados de subida (`bytesSent`) y bajada
  (`bytesReceived`) en `ProxyStatus`, con velocidades independientes.
- `LogsActivity`: chips de filtro por nivel (Todos / Info / Avisos /
  Errores).
- `ErrorDialog`: fondo blanco tipo tarjeta (rounded 24 dp) con icono
  circular rojo.

### Cambiado
- Notificación VPN rediseñada con ↑ subida ↓ bajada y desglose en
  `BigTextStyle`. Se actualiza cada 2 s.
- Placeholder de usuario genérico (`ejemplo.usuario`) en vez de un
  nombre real.

---

## [1.0.0-beta8] — 2026-10-10

### Añadido
- `OnboardingActivity`: 3 slides de bienvenida. Se muestran solo la
  primera vez.
- `ErrorDialog` personalizado con icono circular rojo, título y
  mensaje contextual.
- Mensajes de error específicos según el caso:
  - Timeout → "El proxy no responde"
  - `UnknownHost` → "No se encuentra el servidor"
  - HTTP 407 → "Credenciales incorrectas"
  - `ConnectException` → "No se puede llegar al proxy"
- Animaciones: pulso del botón al verificar, pulso del punto de estado,
  escala del texto de versión, vibración corta.

---

## [1.0.0-beta7] — 2026-10-10

### Cambiado
- Workflow de GitHub Actions ahora compila `assembleRelease` en vez de
  `assembleDebug`.
- El keystore se reconstruye desde *Secrets* antes de compilar. Así el
  APK generado por Actions tiene la **misma firma** que los builds
  locales.

### Añadido
- Reglas de ProGuard para `androidx.security` y Tink (evitan warnings
  de R8).

### Arreglado
- Labels de los inputs siempre arriba (`expandedHintEnabled=false`).
- Logo con fondo transparente (fuera del recuadro blanco).

---

## [1.0.0-beta6] — 2026-10-10

### Añadido
- Paleta oficial UPR:
  - `#107530` (verde principal)
  - `#105F2B` (verde oscuro)
  - `#1E6F3A` (verde medio)
  - `#9DC0A9` (verde claro)
  - `#4F8D64` (verde suave)
  - `#D9E5DE` (verde de fondo)
- 4 iconos vectoriales para los inputs (nube, puerto, persona, candado).
- Toggle de ojo para la contraseña.
- Notificación rediseñada: icono vectorial monocromo, color de acento
  verde UPR, `BigTextStyle` con desglose completo, actualización cada
  2 s.

### Cambiado
- Tema claro forzado (`Light.NoActionBar`). Eliminado `values-night`.
- `forceDarkAllowed=false`: la app nunca cambia a modo oscuro.
- Inputs rediseñados: label flotante arriba, texto en verde oscuro,
  bordes redondeados 12 dp, autofill deshabilitado.
- `ProxyStatus.updateSpeed()`: cálculo de velocidad cada segundo.

---

## [1.0.0-beta5] — 2026-10-10

### Seguridad
- `allowBackup=false` + `dataExtractionRules`: nadie puede extraer las
  credenciales con `adb backup` ni transferirlas con *device transfer*.
- Credenciales cifradas con `EncryptedSharedPreferences` (AES-256-GCM,
  MasterKey del sistema). Migración automática desde el almacén viejo
  en texto plano.
- `FLAG_SECURE` en `MainActivity` y `LogsActivity`: bloquea screenshots
  y la miniatura de "Apps recientes".
- Autofill deshabilitado en los campos servidor/usuario/contraseña.
- Portapapeles con limpieza automática en 60 s tras copiar logs.
- `networkSecurityConfig`: solo se permite cleartext a
  `proxy.upr.edu.cu` y direcciones locales.
- `exported=false` explícito en `LogsActivity` y en el servicio.
- `POST_NOTIFICATIONS` y `ACCESS_NETWORK_STATE` declarados.
- `screenOrientation` fijo + `configChanges` para evitar recreación.

---

## [1.0.0-beta4] — 2026-10-10

### Arreglado
- **La VPN no se desconectaba** al pulsar Desconectar (solo se podía
  desinstalando): `TProxyStopService` bloqueaba `onDestroy` en el hilo
  principal. Ahora el shutdown corre en hilo aparte con timeout de 3 s,
  y el TUN se cierra **primero** para romper el `read()` del motor
  nativo.
- El servicio se relanzaba solo (`START_STICKY` → `START_NOT_STICKY`).
- El botón "Desconectar" de la notificación no hacía nada: ahora maneja
  `ACTION_STOP` explícitamente.
- Se conectaba sin verificar el proxy (dejaba sin red): `MainActivity`
  ahora ejecuta `TestConnection` **antes** de crear la VPN.

### Añadido
- `addDisallowedApplication`: la propia app queda excluida del túnel
  para evitar bucles.
- `Socks5HttpBridge.stop()` cierra agresivamente todos los sockets
  abiertos para que la desconexión sea instantánea.

---

## [1.0.0-beta3] — 2026-10-10

### Añadido
- Estado en vivo en la UI: conexiones activas, bytes enviados, uptime,
  punto verde/naranja/rojo según actividad.
- Botón "Probar conexión al proxy": hace un `CONNECT` de prueba sin
  activar la VPN.
- Auto-reconexión: si se cae la red y vuelve, relanza la VPN.
- Notificación con botón "Desconectar".
- Clases nuevas: `ProxyStatus`, `TestConnection`.
- Drawables de puntos de estado (`dot_green`, `dot_orange`, `dot_red`).

### Cambiado
- `MainActivity` lee el estado real del servicio VPN al abrir: ya no
  aparece desconectado si la VPN sigue activa.

---

## [1.0.0-beta2] — 2026-10-10

### Añadido
- **Puente SOCKS5→HTTP** que permite que el tráfico del móvil salga
  realmente por `proxy.upr.edu.cu:8080` con autenticación básica.
- Pantalla de logs (diagnóstico avanzado):
  - Sistema de logs en vivo con RecyclerView.
  - Botones Copiar y Borrar.
- `LogBuffer` (buffer circular de 1000 líneas) y `L` (wrapper de Log).
- Workflow de GitHub Actions compila también en ramas `feature/*` y
  `fix/*`.

### Cambiado
- `ProxyForwarder` ahora orquesta el puente y el túnel nativo.
- `ProxyVpnService` guarda el forwarder para hacer shutdown limpio y
  maneja `onRevoke()` del sistema.

### Conocido
- Si el proxy no soporta autenticación `Basic`, habrá que añadir NTLM.

---

## [1.0.0-beta1] — 2026-10-10

### Añadido
- Interfaz gráfica con logo institucional UPR.
- 4 campos: Servidor, Puerto, Usuario, Contraseña.
- Guardado de credenciales con `SharedPreferences`.
- Servicio VPN basado en `VpnService`.
- Notificación persistente.
- Librería nativa `hev-socks5-tunnel` (AAR).
- Compatible con Android 8.0+ (API 26).
- Build automático con GitHub Actions.
- Licencia MIT.

### Pendiente (resuelto en beta2)
- El tráfico no se enruta al proxy.

---

## Formato de versiones

- **`1.0.0-betaN`** — Fase de pruebas beta.
- **`1.0.0-rcN`** — Release candidate (próximamente).
- **`1.0.0`** — Versión estable.

[Unreleased]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta16...HEAD
[1.0.0-beta16]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta15...v1.0.0-beta16
[1.0.0-beta15]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta14...v1.0.0-beta15
[1.0.0-beta14]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta13...v1.0.0-beta14
[1.0.0-beta13]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta12...v1.0.0-beta13
[1.0.0-beta12]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta11...v1.0.0-beta12
[1.0.0-beta11]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta10...v1.0.0-beta11
[1.0.0-beta10]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta9...v1.0.0-beta10
[1.0.0-beta9]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta8...v1.0.0-beta9
[1.0.0-beta8]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta7...v1.0.0-beta8
[1.0.0-beta7]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta6...v1.0.0-beta7
[1.0.0-beta6]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta5...v1.0.0-beta6
[1.0.0-beta5]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta4...v1.0.0-beta5
[1.0.0-beta4]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta3...v1.0.0-beta4
[1.0.0-beta3]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta2...v1.0.0-beta3
[1.0.0-beta2]: https://github.com/storm2801/upr-proxy/compare/v1.0.0-beta1...v1.0.0-beta2
[1.0.0-beta1]: https://github.com/storm2801/upr-proxy/releases/tag/v1.0.0-beta1

