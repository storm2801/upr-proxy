# Guía de contribución

## Ramas

| Rama | Propósito |
|---|---|
| `main` | Versiones estables |
| `develop` | Integración de funciones en curso |
| `feature/nombre` | Una función nueva a la vez |
| `fix/nombre` | Corrección puntual |

## Crear una función nueva

    git checkout develop
    git pull origin develop
    git checkout -b feature/nombre-descriptivo
    # ... trabajar ...
    git add -A
    git commit -m "Añade X cosa"
    git push -u origin feature/nombre-descriptivo

## Convención de commits

Formato: `<tipo>: <descripción corta>`

| Tipo | Uso |
|---|---|
| `feat:` | Función nueva |
| `fix:` | Corrección de bug |
| `docs:` | Solo documentación |
| `refactor:` | Reorganización sin cambiar comportamiento |
| `chore:` | Mantenimiento |
| `test:` | Tests |

## Versionado

Semantic Versioning con sufijos:

- `1.0.0-beta1` — fase de prueba
- `1.0.0-rc1` — release candidate
- `1.0.0` — estable

Subir versión: `bash scripts/bump-version.sh [patch|minor|major|beta]`
