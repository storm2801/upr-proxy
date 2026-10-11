# Estrategia de ramas y versionado

## 🌳 Ramas

| Rama | Propósito | Protegida |
|---|---|---|
| main | Versiones estables. Cada commit es un Release. | ✅ |
| develop | Integración continua. Todo pasa por aquí. | ✅ |
| feature/* | Una funcionalidad por rama. | ❌ |
| fix/* | Corrección puntual. | ❌ |

## 📋 Flujo típico

Crear rama:

    git checkout develop
    git pull origin develop
    git checkout -b feature/mi-funcion

Trabajar y subir:

    git add -A
    git commit -m "feat: añadir función X"
    git push -u origin feature/mi-funcion

Abrir PR hacia develop en GitHub.

## 🚀 Publicar release

    git checkout main
    git pull origin main
    git merge develop --no-edit
    git push origin main

    sed -i 's/versionCode [0-9]*/versionCode 17/' app/build.gradle
    sed -i 's/versionName "[^"]*"/versionName "1.0.0-beta17"/' app/build.gradle
    sed -i 's/Versión 1\.0\.0-beta[0-9]*/Versión 1.0.0-beta17/' app/src/main/res/layout/activity_main.xml

    nano version.json
    git add -A
    git commit -m "chore: bump a 1.0.0-beta17"
    git push origin main

    git tag -a v1.0.0-beta17 -m "Release 1.0.0-beta17"
    git push origin v1.0.0-beta17

    git checkout develop
    git merge main --no-edit
    git push origin develop

## 🏷️ Versionado

MAJOR.MINOR.PATCH[-PRE]

| Parte | Cuándo sube |
|---|---|
| MAJOR | Cambios incompatibles |
| MINOR | Funcionalidad nueva |
| PATCH | Correcciones |
| -betaN | Fase de pruebas |
| -rcN | Release candidate |

Ejemplos: 1.0.0-beta1, 1.0.0-beta16, 1.0.0-rc1, 1.0.0, 1.1.0, 1.0.1

Reglas:
- El versionCode (entero) siempre sube.
- versionName en build.gradle y version.json deben coincidir.
- Cada tag v* en main genera un Release automático.

## 🧹 Limpieza de ramas

    bash scripts/cleanup-branches.sh

Manual:

    git branch --merged develop
    git branch -d feature/mi-funcion
    git push origin --delete feature/mi-funcion

## 🚫 Protección de ramas

En GitHub Settings → Branches → Branch protection rules:

- Requerir Pull Request antes del merge
- Requerir aprobación de 1 revisor
- Requerir que pasen los checks de CI
- Prohibir force-push

## 📌 Referencias

- Git Flow: https://nvie.com/posts/a-successful-git-branching-model/
- Conventional Commits: https://www.conventionalcommits.org/es/v1.0.0/
