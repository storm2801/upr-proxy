# UPR Proxy

Proxy para Android de la UPR.

Enruta el trafico por el proxy de la UPR.

## Instalacion

1. Descarga el APK de Releases.
2. Concede bateria sin restricciones.
3. Usuario + contrasena + Conectar.

## Uso

Si el chequeo falla, pulsa
"Conectar de todos modos".

## Como funciona

Apps -> TUN -> hev-socks5-tunnel
     -> Socks5HttpBridge :1080
     -> proxy.upr.edu.cu:8080
     -> Internet

## Compilar

Requisitos: JDK 17, SDK 34, NDK r26b.

git clone URL
./gradlew assembleDebug

## Troubleshooting

Connection reset: MTU 1280.
Timeout DNS: usar 1.1.1.1.
No conecta: probar otro puerto.

## Licencia

MIT. Ver LICENSE.
