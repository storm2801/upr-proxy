# Guía de instalación extendida

Esta guía cubre **todo** el proceso de instalación de UPR Proxy en un
dispositivo Android, incluyendo los avisos de seguridad de Google, la
desactivación temporal de Play Protect, y la solución de problemas
comunes.

> 📌 **Si es tu primera vez instalando un APK fuera de Google Play,
> lee esta guía completa antes de empezar.**

---

## 📱 ¿En qué dispositivos funciona?

### Compatible con

| Requisito ↕▾ | Mínimo ↕▾ |
|---|---|
| **Sistema operativo** | Android 8.0 (Oreo, API 26) |
| **Arquitectura** | ARMv7 o ARM64 |
| **Espacio libre** | ~15 MB |
| **Conectividad** | Red WiFi de la UPR |

### NO compatible con

- ❌ iOS (iPhone, iPad)
- ❌ Windows Phone / Windows Mobile
- ❌ macOS, Windows, Linux (escritorio)
- ❌ Android 7.1 (Nougat) o anteriores
- ❌ Android TV, Wear OS, ChromeOS

> Si tu dispositivo no cumple los requisitos mínimos, la app
> **no se instalará** o se cerrará al abrir.

---

## ⚠️ Avisos de seguridad de Google

### ¿Por qué Google bloquea esta app?

Google **Play Protect** es un sistema de seguridad que analiza todas
las apps antes de instalarlas. Bloquea automáticamente las apps que:

1. No vienen de Google Play.
2. No tienen un certificado de firma verificado.
3. No han sido revisadas por Google.

**UPR Proxy** cumple las 3 condiciones anteriores porque:

- Se distribuye directamente desde GitHub (no está en Play Store).
- Firma con un keystore propio (no con uno de Google).
- Es una app institucional de la UPR, no pública.

**Esto no significa que la app sea peligrosa.** Simplemente significa
que Google no la "conoce".

### ¿Qué pasa si no desactivo Play Protect?

Cuando intentes instalar el APK, Android mostrará uno de estos
mensajes:

- *"Play Protect bloqueó la instalación"*
- *"Esta app puede ser peligrosa"*
- *"No se puede instalar la app"*

Y la instalación fallará. **Por eso es necesario desactivar Play
Protect temporalmente.**

---

## 🛠️ Instalación paso a paso

### Paso 1 — Descargar el APK

Abre el navegador del teléfono y ve a:

```

https://github.com/storm2801/upr-proxy/releases/latest

```

En la sección **"Assets"**, toca el archivo:

```

UPRProxy-X.X.X-betaXX-release.apk

```

**Espera a que termine la descarga.** El tamaño aproximado es **4 MB**.

Si el navegador te pregunta dónde guardar, elige **Descargas**.

### Paso 2 — Desactivar Play Protect temporalmente

> ⚠️ Este paso es **obligatorio** si quieres que la instalación
> funcione sin problemas.

1. Abre **Google Play Store** en tu teléfono.
2. Toca tu **foto de perfil** (esquina superior derecha).
3. Selecciona **Play Protect**.
4. Toca el **icono de engranaje** ⚙️ (arriba a la derecha).
5. Verás un interruptor llamado **"Analizar apps en busca de
   amenazas con Play Protect"**.
6. **Desactívalo.**
7. Confirma tocando **"Desactivar"** en el diálogo de advertencia.

**Resultado esperado:** el interruptor aparece en gris (desactivado).

> 💡 **¿Por qué es seguro hacer esto?**
> Play Protect se reactiva automáticamente en pocos días. Solo
> necesitas desactivarlo durante el tiempo que tardes en instalar la
> app (unos minutos). Después puedes volver a activarlo.

### Paso 3 — Habilitar la instalación de fuentes desconocidas

Los navegadores Android **no pueden instalar APKs por defecto**. Debes
darles permiso explícito:

1. Abre **Ajustes** del teléfono.
2. Ve a **Aplicaciones** → **Acceso especial a aplicaciones** →
   **Instalar apps desconocidas**.
   *Algunos fabricantes usan rutas distintas:*
   - **Samsung:** Ajustes → Biometría y seguridad → Instalar apps desconocidas.
   - **Xiaomi/Redmi:** Ajustes → Privacidad → Permisos especiales → Instalar apps desconocidas.
   - **Huawei:** Ajustes → Seguridad → Más ajustes → Instalar apps de fuentes externas.
3. Aparecerá una lista de apps. Busca **Chrome** (o el navegador que
   hayas usado).
4. **Activa** el permiso **"Permitir desde esta fuente"**.
5. Repite si usas otro navegador.

### Paso 4 — Instalar el APK

1. Abre el **gestor de archivos** del teléfono (puede llamarse
   "Archivos", "Mi Archivos", "Files", "Administrador de archivos"...).
2. Ve a la carpeta **Descargas** (Downloads).
3. Toca el archivo **`UPRProxy-X.X.X-betaXX-release.apk`**.
4. Android mostrará una pantalla de confirmación. Toca **Instalar**.
5. Si aparece *"Se ha bloqueado la instalación"*, es porque:
   - **No desactivaste Play Protect** → vuelve al Paso 2.
   - **No habilitaste fuentes desconocidas** → vuelve al Paso 3.
6. Espera 10-20 segundos.
7. Toca **Abrir**.

### Paso 5 — Reactivar Play Protect (opcional)

Ahora que la app está instalada, **puedes volver a activar Play
Protect**:

1. Abre **Google Play Store** → **Play Protect** → ⚙️.
2. **Activa** de nuevo el interruptor.
3. Si Play Protect analiza UPR Proxy y muestra una advertencia, toca
   **"Mantener"** o **"Ignorar"** (según la versión de Android).

> 💡 Si no reactivas Play Protect, no pasa nada crítico. Simplemente
> pierdes la protección adicional contra otras apps maliciosas. Se
> recomienda activarlo de nuevo.

### Paso 6 — Configurar la app

Al abrir la app por primera vez, sigue estos pasos:

1. **Onboarding:** aparecerán 3 pantallas explicando cómo funciona.
   Pulsa **Siguiente** hasta el final. Pulsa **Saltar** si ya la
   conoces.
2. **Configuración:**
   - **Servidor:** `proxy.upr.edu.cu`
   - **Puerto:** `8080`
   - **Usuario:** tu usuario institucional UPR
   - **Contraseña:** tu contraseña del proxy
3. Pulsa **Probar conexión** para verificar que todo funciona.
4. Pulsa **Conectar**.
5. Android pedirá permiso para crear una **VPN**. **Acepta.**
6. La app mostrará **"Conectado"** con un punto verde.

¡Listo! Todo el tráfico del teléfono pasará por el proxy de la UPR.

---

## 🔄 Cómo actualizar la app

Una vez instalada, **la app se actualiza sola**:

1. Abre la app.
2. Si hay una versión nueva, aparecerá un diálogo:
   **"Actualización disponible"**.
3. Pulsa **"Actualizar ahora"**.
4. La app te pedirá permiso de instalación (solo la primera vez).
5. Se descarga el nuevo APK y se instala automáticamente.

> 💡 **No tienes que desinstalar** la versión anterior. La app
> conserva tus credenciales y configuración.

---

## 🩹 Solución de problemas

### ❌ "App no instalada"

**Causa más común:** tienes una versión anterior instalada con una
**firma diferente** (por ejemplo, una compilación local con otro
keystore).

**Solución:**

1. Desinstala la versión anterior (Ajustes → Aplicaciones → UPR Proxy
   → Desinstalar).
2. Vuelve a instalar el APK.

### ❌ "Play Protect bloqueó la instalación"

**Causa:** Play Protect sigue activo.

**Solución:** repite el [Paso 2](#paso-2--desactivar-play-protect-temporalmente) y vuelve a intentar.

### ❌ "No se puede abrir el archivo"

**Causa:** el APK está **incompleto** o **corrupto**.

**Solución:**

1. Abre el gestor de archivos.
2. Borra el APK descargado.
3. Vuelve a descargarlo.
4. **Verifica el tamaño:** debe ser **~4 MB**. Si es mucho más
   pequeño (500 KB), la descarga se cortó.

### ❌ "Este dispositivo no es compatible"

**Causa:** tu Android es **anterior a 8.0**.

**Solución:** no hay solución. El dispositivo no es compatible con la
app. Considera actualizar el sistema operativo (si el fabricante lo
permite) o usar otro dispositivo.

### ❌ La app se instala pero no conecta

**Causa 1:** no estás en la red WiFi de la UPR.

**Causa 2:** las credenciales son incorrectas.

**Solución:**

1. Verifica que estás conectado al WiFi de la universidad.
2. En la app, pulsa **"Probar conexión"**. El mensaje de error te dirá
   exactamente qué falla:
   - **"Timeout"** → estás fuera de la red UPR.
   - **"No se pudo resolver"** → problema de DNS o red.
   - **"Credenciales incorrectas"** → usuario o contraseña mal.
   - **"No se pudo conectar"** → el proxy no responde.

### ❌ La notificación de VPN no aparece

**Causa:** Android 13+ requiere el permiso `POST_NOTIFICATIONS`.

**Solución:**

1. Ajustes → Aplicaciones → UPR Proxy → Notificaciones.
2. Activa la opción.

### ❌ "La VPN no se desconecta"

**Causa:** bug conocido en algunas versiones de Android.

**Solución:** la app tiene un watchdog de 6 segundos. Si después de
ese tiempo sigue activa:

1. Reinicia el teléfono.
2. Reporta el bug en
   [Issues](https://github.com/storm2801/upr-proxy/issues/new?template=bug_report.md)
   con: modelo, versión de Android y qué estabas haciendo.

### ❌ "El botón Conectar no hace nada"

**Causa:** probablemente ya hay una conexión activa que no se ve.

**Solución:**

1. Desliza la barra de notificaciones hacia abajo.
2. Busca la notificación **"UPR Proxy activo"**.
3. Pulsa **"Desconectar"**.
4. Vuelve a la app e intenta de nuevo.

---

## 📊 Consumo y rendimiento

| Recurso ↕▾ | Consumo aproximado ↕▾ |
|---|---|
| **Batería** | Similar a una VPN normal: ~5 % por hora en uso activo |
| **RAM** | ~40 MB en primer plano, ~20 MB en segundo plano |
| **Datos** | 0 MB adicionales (no añade overhead significativo) |
| **Almacenamiento** | ~10 MB (APK) + ~4 MB en caché de actualizaciones |

---

## 🔒 Privacidad

**UPR Proxy NO recopila datos.** No hay telemetría, ni analytics, ni
publicidad, ni servidores externos más allá del proxy de la UPR y
GitHub (para el sistema de actualizaciones).

- **Credenciales:** cifradas localmente, nunca se envían a terceros.
- **Logs:** solo en memoria, nunca se suben a ningún servidor.
- **Tráfico:** va directamente al proxy de la UPR, sin intermediarios.

---

## ❓ Preguntas frecuentes

### ¿Funciona sin WiFi?

No. El proxy de la UPR solo es alcanzable desde dentro de la red
universitaria. Si estás en datos móviles o en otra WiFi, no conectará.

### ¿Funciona con VPN activa?

No simultáneamente. Android solo permite **una VPN activa a la vez**.
Si tienes otra VPN conectada, desactívala antes.

### ¿Necesito root?

No. La app funciona sin root.

### ¿Consume muchos datos?

No. La app no añade overhead significativo. Solo enruta el tráfico que
ya ibas a consumir.

### ¿Puedo usarla sin las credenciales?

No. El proxy de la UPR requiere autenticación con usuario y contraseña
institucionales.

### ¿Es legal?

Sí. Es una herramienta oficial distribuida para uso institucional. El
uso indebido (compartir credenciales, intentar evadir controles) puede
acarrear sanciones disciplinarias.

### ¿Dónde reporto un bug?

En [Issues](https://github.com/storm2801/upr-proxy/issues), usando la
plantilla de bug.

### ¿Cómo desinstalo la app?

1. Mantén pulsado el icono de UPR Proxy.
2. Selecciona **Desinstalar**.
3. Confirma.

> 💡 **Antes de desinstalar**, desconecta la VPN si está activa.

---

## 📚 Recursos adicionales

- 📖 [README principal](../README.md)
- 🏗️ [Documentación técnica](../docs/ARCHITECTURE.md)
- 💻 [Guía de desarrollo](../docs/SETUP.md)
- 🐛 [Reportar un bug](https://github.com/storm2801/upr-proxy/issues/new?template=bug_report.md)
- 💡 [Sugerir una mejora](https://github.com/storm2801/upr-proxy/issues/new?template=feature_request.md)
