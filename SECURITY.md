# Política de seguridad

## Reportar vulnerabilidades

No abras un issue público. Usa:

- GitHub Security Advisories
- Contacto directo: @storm2801

Incluye: descripción, impacto, pasos para reproducir, versión afectada.

## Tiempos

| Etapa | Objetivo |
|---|---|
| Acuse de recibo | ≤ 48 h |
| Evaluación inicial | ≤ 7 días |
| Corrección | ≤ 30 días |

## Medidas implementadas

| Medida | Implementación |
|---|---|
| Credenciales cifradas | AES-256-GCM con MasterKey |
| Sin backup | allowBackup=false |
| Sin autofill | importantForAutofill=no |
| HTTPS obligatorio | networkSecurityConfig |
| Componentes internos | exported=false |
| Portapapeles | Limpieza a los 60 s |
