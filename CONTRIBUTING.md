# Contribuir

## Flujo

1. Fork del repositorio.
2. Crear rama desde develop:

   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/mi-funcion
   ```

3. Commit con Conventional Commits:

   - `feat:` Nueva función
   - `fix:` Corrección
   - `docs:` Solo documentación
   - `refactor:` Reorganización
   - `chore:` Mantenimiento

4. Push y abrir PR hacia develop.

## Reglas

- No subir credenciales ni keystores.
- El código debe compilar: ./gradlew assembleRelease.
- Actualizar CHANGELOG.md si aplica.
- Leer el Código de conducta.

## Reportar bugs

Usa las plantillas de issue con versión, dispositivo, pasos y logs.
