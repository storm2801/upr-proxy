# Guía de setup para desarrolladores

Esta guía te lleva paso a paso desde cero hasta tener el APK compilado y firmado.

## 📋 Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Sistema operativo | CachyOS, Arch, Fedora 38+, Ubuntu 22.04+ o WSL2 |
| Java Development Kit | **17** |
| Android SDK | Platform 34 + Build-tools 34.0.0 |
| Git | 2.30+ |

## 1️⃣ Instalar JDK 17

### CachyOS / Arch / Manjaro

    sudo pacman -S jdk17-openjdk
    sudo archlinux-java set java-17-openjdk
    java -version

### Fedora / RHEL

    sudo dnf install java-17-openjdk-devel

### Ubuntu / Debian

    sudo apt install openjdk-17-jdk

Configurar JAVA_HOME:

    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
    echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc

## 2️⃣ Instalar Android SDK

### Opción A — Android Studio

Descarga desde https://developer.android.com/studio e instala:
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Android SDK Command-line Tools

### Opción B — Solo command-line tools

    sudo pacman -S unzip wget
    mkdir -p ~/Android/Sdk/cmdline-tools
    cd ~/Android/Sdk/cmdline-tools
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip commandlinetools-linux-*.zip
    mv cmdline-tools latest
    export ANDROID_HOME=$HOME/Android/Sdk
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
    echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
    echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
    source ~/.bashrc
    sdkmanager --licenses
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

## 3️⃣ Clonar y compilar

    git clone https://github.com/storm2801/upr-proxy.git
    cd upr-proxy
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    ./gradlew assembleDebug

## 4️⃣ Compilar release firmado (opcional)

Crear keystore:

    keytool -genkeypair -v -keystore upr-proxy.jks -alias upr-proxy -keyalg RSA -keysize 2048 -validity 10000

Crear keystore.properties en la raíz:

    storeFile=../upr-proxy.jks
    storePassword=TU_PASSWORD
    keyAlias=upr-proxy
    keyPassword=TU_PASSWORD_CLAVE

Compilar:

    ./gradlew assembleRelease

## 5️⃣ Comandos útiles

| Comando | Uso |
|---|---|
| ./gradlew assembleDebug | APK debug |
| ./gradlew assembleRelease | APK release firmado |
| ./gradlew clean | Limpiar build |
| ./gradlew installDebug | Instalar en dispositivo |
| ./gradlew lint | Android Lint |

## 6️⃣ Solución de problemas

**SDK location not found** → Verifica que existe local.properties con sdk.dir.

**JAVA_HOME inválido** → sudo archlinux-java set java-17-openjdk

**Faltan paquetes SDK** → sdkmanager "platforms;android-34" "build-tools;34.0.0"

**App not installed** → Desinstala la versión anterior (firma diferente).

## 📚 Recursos

- VpnService: https://developer.android.com/guide/topics/connectivity/vpn
- Gradle: https://docs.gradle.org/
- Firma de apps: https://developer.android.com/studio/publish/app-signing

Consulta CONTRIBUTING.md para empezar a contribuir.
