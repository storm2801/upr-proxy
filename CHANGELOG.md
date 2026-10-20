# Changelog

Todos los cambios importantes del proyecto se documentan aquí.

Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).
Versionado: [Semantic Versioning](https://semver.org/lang/es/).

## [1.0.0-beta37] — 2026-10-19

### Optimizado
- **Logs de Red**: Silenciados los mensajes informativos constantes de CONNECT exitosos en `Socks5HttpBridge`, reduciendo drásticamente el uso de memoria y CPU, y dejando visible solo los errores críticos.

### Arreglado
- **Actualización**: Sincronización estricta de versiones entre `develop` y `main` para evitar que la app vuelva a descargar la misma versión en bucle.

---

## [1.0.0-beta31] — 2026-10-19

### Arreglado
- **CRÍTICO — IPv6 roto**: el proxy de la UPR (squid) devolvía `503 Service
  Unavailable` para todo `CONNECT` a direcciones IPv6 (AAAA). Esto rompía
  Facebook, WhatsApp y otras apps que resolvían a IPv6 primero. Se eliminó
  IPv6 del túnel VPN y se forza IPv4 exclusivamente, así el kernel del móvil
  usa siempre el registro A.
- **CRÍTICO — reintentos ante 503**: el puente SOCKS5 ahora reintentá la
  conexión al proxy hasta 2 veces con backoff exponencial cuando el proxy
  responde 503 (transitorio por congestión), en vez de fallar inmediatamente.
- **DNS del túnel corregido**: se usan los DNS reales de la UPR
  (`10.2.144.1`, `10.2.1.8`) declarados en el Builder en vez del mapdns
  ficticio `198.18.0.2`, que no resolvía nada y causaba timeouts.
- **Exclusiones de rutas**: se excluyen del túnel las IPs del proxy
  (`10.2.1.80`) y los DNS locales, evitando loops de routing cuando el
  puente SOCKS5 se conecta al proxy a través de la propia VPN.
- **Config YAML del motor nativo**: se quitó la sección `mapdns` rota y el
  bloque IPv6, que eran la causa raíz de los fallos de resolución.

---

## [1.0.0-beta30] — 2026-10-19

### Arreglado
- **Configuración de red del túnel**: se aseguran `addRoute("0.0.0.0", 0)` y
  `addRoute("::", 0)` junto con `addAddress` IPv4/IPv6, para que todo el tráfico
  del móvil sea redirigido por la VPN y pase por el proxy.
- **DNS del sistema en el túnel**: se añade `addDnsServer` con la dirección del
  DNS virtual mapdns (198.18.0.2) para que las consultas de dominios se resuelvan
  dentro del túnel y no fallen por timeout.
- **Handshake CONNECT robusto**: se mantiene el fallback a IP directa
  (`10.2.1.80`) cuando el dominio `proxy.upr.edu.cu` no resuelve, y se
  formatea correctamente el host en el comando CONNECT tanto para dominio como
  para IP literal.
- **Timeouts**: CONNECT al proxy aumentado a 15 s y timeout de lectura a 60 s,
  evitando fallos de conexión en redes con latencia alta.

### Mejorado
- **Diagnóstico en tiempo real**: la pantalla principal muestra SSID, IP local,
  gateway, DNS y proxy del WiFi actual, junto con el estado del proxy y la VPN.

---

## [1.0.0-beta26] — 2026-10-15

### Añadido
- **Reporte de diagnóstico completo** en el menú dev. Genera un archivo
  .txt con:
  * Info completa del dispositivo (fabricante, modelo, API, arquitecturas)
  * Memoria RAM y almacenamiento
  * Resolución de pantalla
  * Info de la app (versión, package, firma SHA-256, tamaño)
  * Configuración guardada (sin contraseña)
  * Estado de la VPN (uptime, conexiones, bytes)
  * Estado de la red (SSID, IP, DNS, proxy del WiFi)
  * Últimos 300 logs
  Compartible por WhatsApp, email, etc.

### Eliminado
- Botón 'Información del sistema' (ahora está en el reporte)
- Botón 'Copiar todos los logs' (el reporte los incluye)
- Botón 'Borrar logs' (poco útil)

---

## [1.0.0-beta29] — 2026-10-18

### Arreglado
- **Tunelización DNS completa**: implementado `mapdns` en la configuración YAML del túnel nativo para capturar las consultas DNS y mapearlas internamente, permitiendo resolver dominios correctamente a través del túnel.
- **Soporte IPv6**: configurado el túnel con dirección IPv6 (fd00:1::2) para habilitar tráfico IPv6 a través del proxy cuando sea necesario.
- **Bug de handshake**: corregido el formato del host en el comando `CONNECT` HTTP cuando se utilizan IPs literales.

---

## [1.0.0-beta28] — 2026-10-15

### Cambiado
- El reporte de diagnóstico ahora se muestra en un diálogo en pantalla,
  no se comparte directamente. El usuario puede verlo, copiarlo con un
  botón en la esquina superior derecha, o cerrarlo.
- Texto del botón simplificado: 'Generar reporte' (antes tenía emoji y
  texto largo).

---

## [1.0.0-beta27] — 2026-10-15

### Añadido
- **Reporte de diagnóstico completo** en el menú dev
- Botón 'Generar reporte de diagnóstico' en el menú dev

### Eliminado
- Botón 'Información del sistema'
- Botón 'Copiar todos los logs'
- Botón 'Borrar logs'

### Notas
- Beta26 se descarta por error de secuencia en el tag

---

## [1.0.0-beta25] — 2026-10-14

### Añadido — Bloque 1 completo
- **UprLoginActivity**: WebView con autofill del portal cautivo de la UPR
  (upr-wifi.upr.edu.cu:8003). Rellena usuario y contraseña automáticamente
  y detecta cuando el login es exitoso.
- **VpnTileService**: Quick Settings Tile para conectar/desconectar la
  VPN desde el panel de notificaciones rápidas sin abrir la app.
- **Botón 'Configurar WiFi (proxy)'** en la pantalla principal que abre
  los ajustes de WiFi directamente.
- **Botón 'Abrir intranet UPR'** ahora abre el WebView embebido con
  autofill en vez de Chrome.

### Cambiado
- network_security_config: dominios upr-wifi.upr.edu.cu e
  intranet.upr.edu.cu permitidos para cleartext.

---

## [1.0.0-beta24] — 2026-10-14

### Añadido
- Botón 'Abrir intranet UPR' en la pantalla principal. Abre
  intranet.upr.edu.cu en el navegador por defecto.

### Notas
- Prueba del sistema de actualizaciones automáticas.

---

## [1.0.0-beta23] — 2026-10-14

### Arreglado
- **URL de version.json cambiada a github.com**. El proxy de la UPR
  bloqueaba raw.githubusercontent.com, así que la app nunca podía
  consultar si había actualización. A partir de esta versión, las
  actualizaciones automáticas funcionan.

### Notas
- Beta22 se subió por error con el nombre del APK equivocado. Se
  salta directamente a beta23.

---

## [1.0.0-beta21] — 2026-10-14

### Arreglado
- **BUG CRÍTICO: pipes secuenciales**. Los flujos cliente→proxy y
  proxy→cliente se ejecutaban uno tras otro, bloqueando la respuesta.
  Ahora corren en hilos paralelos. ESTE era el bug que impedía que la
  app funcionara: los CONNECT llegaban al proxy, el proxy respondía
  200, pero la respuesta nunca llegaba a las apps.
- MTU reducido de 8500 a 1400 (evita fragmentación).
- DNS de la UPR (10.2.144.1, 10.2.1.8) añadidos al túnel para que la
  resolución de dominios funcione.

---

## [1.0.0-beta21] — 2026-10-14

### Arreglado
- **BUG CRÍTICO: pipes secuenciales**. Los flujos cliente→proxy y
  proxy→cliente se ejecutaban uno tras otro, bloqueando la respuesta.
  Ahora corren en hilos paralelos. ESTE era el bug que impedía que la
  app funcionara: los CONNECT llegaban al proxy, el proxy respondía
  200, pero la respuesta nunca llegaba a las apps.
- MTU reducido de 8500 a 1400 (evita fragmentación).
- DNS de la UPR (10.2.144.1, 10.2.1.8) añadidos al túnel para que la
  resolución de dominios funcione.

---

## [1.0.0-beta20] — 2026-10-14

### Arreglado
- **Bloqueo excesivo del proxy**: la app ya no impide conectar cuando el
  WiFi tiene proxy configurado. En la UPR es necesario tener el proxy
  del WiFi activo para llegar al servidor, así que el bloqueo era
  contraproducente.
- **DNS del dominio no resuelve sin proxy**: si `proxy.upr.edu.cu` no
  resuelve (WiFi sin proxy), la app prueba automáticamente la IP
  directa `10.2.1.80` como fallback.

### Resultado
- Funciona con WiFi **con proxy** (caso normal en la UPR).
- Funciona con WiFi **sin proxy** (usa IP directa como fallback).

---

## [1.0.0-beta19] — 2026-10-12

### Arreglado
- **Crash al forzar reconexión**: eliminado excludeRoute() que colgaba
  el hilo principal en Samsung y provocaba ANR.
- **Permiso VPN**: Forzar reconexión ahora pide permiso al sistema si
  no se ha concedido.
- **DNS**: se resuelve FUERA del túnel (Android usa los DNS del WiFi
  cuando el Builder no los declara). Evita que las consultas UDP se
  pierdan en el proxy HTTP.
- **Doble proxy**: la app avisa si el WiFi tiene un proxy configurado
  y ofrece abrir los ajustes de WiFi para quitarlo.

### Añadido
- Permiso de notificaciones en Android 13+ (POST_NOTIFICATIONS).

### Simplificado
- Una sola configuración de VPN (sin sistema de estrategias).

---

## [1.0.0-beta18] — 2026-10-12

### Arreglado
- **DNS del sistema dentro del túnel VPN**: antes las consultas DNS se
  perdían porque el proxy HTTP solo maneja TCP. Ahora la app lee los DNS
  del WiFi con ConnectivityManager y los añade al Builder de la VPN.
- **DNS IPv6 ignorado**: algunos WiFi tienen DNS IPv6 que rompían
  establish() en móviles Samsung. Ahora solo se procesan DNS IPv4.
- **Compatibility con marcas**: sistema de 3 estrategias en cascada.
  La app intenta automáticamente la mejor configuración según el móvil.

### Añadido
- scripts/build.sh: compilación en CachyOS/Arch sin credenciales.

---

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

## [1.0.0-beta32] — 2026-10-19

### Arreglado
- **Loop de enrutamiento**: Se corrigió la exclusión de rutas en la VPN. El
  tráfico destinado al proxy UPR (10.2.1.80) ahora se enruta correctamente
  fuera del túnel, evitando que la VPN intente tunelizar su propia conexión.
- **Reducción de logs**: Eliminado log nivel DEBUG para cada conexión CONNECT
  al proxy, evitando que el buffer de logs se llene en segundos.

---

## [1.0.0-beta33] — 2026-10-19

### Arreglado
- **Persistencia en Samsung/Android 12+**: El servicio VPN ahora utiliza
   y  para evitar ser cerrado
  por el optimizador de batería de Samsung al pasar a segundo plano.
- **Estabilidad**: El servicio ya no se detiene automáticamente tras la
  iniciación debido a restricciones de políticas de servicios en segundo plano.

---

## [1.0.0-beta34] — 2026-10-19

### Arreglado
- **Loop de enrutamiento**: Se refinó la exclusión de rutas en la VPN. Ahora
  se utilizan rutas específicas de host (/32) para las IPs del proxy y DNS,
  asegurando que el tráfico no sea interceptado por la VPN bajo ninguna circunstancia.

---

## [1.0.0-beta35] — 2026-10-19

### Arreglado
- **Loop de routing**: Refuerzo en la exclusión de rutas del proxy UPR para
  garantizar que el tráfico de control no sea capturado por el túnel,
  estabilizando la conexión en dispositivos Samsung.

---

## [1.0.0-beta36] — 2026-10-19

### Arreglado
- **Bucle de actualización**: Se corrigió el conflicto de versiones entre 
  y  que hacía que el móvil volviera a instalar la misma versión.

---

## [1.0.0-beta38] — 2026-10-20

### Arreglado
- **Proxy 403 Forbidden**: Se ha añadido un encabezado de navegador legítimo
  en el túnel para evitar que el servidor proxy de la UPR detecte la app como
  no autorizada.
- **Login Intranet**: Se cambió el acceso a la intranet para que abra el
  navegador externo, permitiendo una gestión correcta de las cookies y
  sesiones del portal cautivo.

---

## [1.0.0-beta39] — 2026-10-20

### Arreglado
- **Handshake 403 Forbidden**: Ajustada la cabecera  y el  para que el tráfico del túnel sea indistinguible de un navegador web estándar ante el proxy de la UPR.

---
