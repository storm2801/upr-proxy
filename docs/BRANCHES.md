# Ramas y versionado

## Ramas

| Rama | Uso |
|---|---|
| `main` | Versiones públicas estables |
| `develop` | Integración continua |
| `feature/*` | Una función por rama |
| `fix/*` | Corrección puntual |

## Flujo

```bash
git checkout develop
git pull origin develop
git checkout -b feature/mi-funcion
git add -A
git commit -m "feat: nueva función"
git push -u origin feature/mi-funcion
```

Abrir un PR hacia develop.

## Publicar una release

```bash
git checkout main
git merge develop --no-edit
git push origin main

sed -i "s/versionCode [0-9]*/versionCode 17/" app/build.gradle
sed -i "s/versionName \\"[^\\"]*\\"/versionName \\"1.0.0-beta17\\"/" app/build.gradle

git add -A
git commit -m "chore: bump a 1.0.0-beta17"
git push origin main

git tag -a v1.0.0-beta17 -m "Release 1.0.0-beta17"
git push origin v1.0.0-beta17

git checkout develop
git merge main --no-edit
git push origin develop
```

## Versionado

Formato: MAJOR.MINOR.PATCH[-betaN|-rcN]

- 1.0.0-betaN → Fase de pruebas
- 1.0.0-rcN → Release candidate
- 1.0.0 → Estable
- 1.0.1 → Corrección
- 1.1.0 → Nueva función
