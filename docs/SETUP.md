# Setup de desarrollo

## Requisitos

- Linux o WSL2
- JDK **17**
- Android SDK **34** (platform + build-tools)
- Git

## 1. Instalar JDK 17

**CachyOS / Arch / Manjaro:**

```bash
sudo pacman -S jdk17-openjdk
sudo archlinux-java set java-17-openjdk
```

**Fedora:** sudo dnf install java-17-openjdk-devel

**Ubuntu:** sudo apt install openjdk-17-jdk

## 2. Instalar Android SDK

```bash
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip
mv cmdline-tools latest

export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

Añade las dos líneas export a ~/.bashrc.

```bash
sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

## 3. Compilar

```bash
git clone https://github.com/storm2801/upr-proxy.git
cd upr-proxy
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
```

APK en app/build/outputs/apk/debug/app-debug.apk.

## 4. Release firmado (opcional)

```bash
keytool -genkeypair -v -keystore upr-proxy.jks -alias upr-proxy -keyalg RSA -keysize 2048 -validity 10000
```

Crea keystore.properties en la raíz:

```properties
storeFile=../upr-proxy.jks
storePassword=TU_PASSWORD
keyAlias=upr-proxy
keyPassword=TU_PASSWORD_CLAVE
```

Luego: ./gradlew assembleRelease

> Nunca subas upr-proxy.jks ni keystore.properties al repositorio.
