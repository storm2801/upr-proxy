# Mantener las clases JNI del AAR de hev-socks5-tunnel
-keep class org.amnezia.awg.hevtunnel.** { *; }
-keepclassmembers class org.amnezia.awg.hevtunnel.** { *; }

# Mantener métodos nativos
-keepclasseswithmembernames class * {
    native <methods>;
}

# Mantener AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Evitar warnings
-dontwarn org.amnezia.**
-dontwarn androidx.**

# Mantener las clases propias de la app
-keep class com.upr.proxy.** { *; }
