# Mantener las clases JNI del AAR de hev-socks5-tunnel
-keep class org.amnezia.awg.hevtunnel.** { *; }
-keepclassmembers class org.amnezia.awg.hevtunnel.** { *; }

# Mantener métodos nativos
-keepclasseswithmembernames class * {
    native <methods>;
}

# Mantener AndroidX Security (EncryptedSharedPreferences + Tink)
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 { *; }

# Mantener AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Ignorar clases que solo se usan en la JVM (no existen en Android)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn javax.naming.**
-dontwarn java.beans.**
-dontwarn org.joda.time.**

# Evitar warnings del AAR
-dontwarn org.amnezia.**
-dontwarn androidx.**
-dontwarn com.google.crypto.**
-dontwarn com.google.protobuf.**

# Mantener las clases propias de la app
-keep class com.upr.proxy.** { *; }
