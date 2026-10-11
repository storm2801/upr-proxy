#!/usr/bin/env bash
#
# cleanup-branches.sh
#
# Borra ramas locales y remotas que YA estén fusionadas en `develop`.
# Nunca toca `main` ni `develop`.
#
# Uso:
#   bash scripts/cleanup-branches.sh            # modo interactivo (pregunta)
#   bash scripts/cleanup-branches.sh --dry-run  # solo muestra qué haría
#   bash scripts/cleanup-branches.sh --yes      # sin preguntar
#

set -euo pipefail

# --- Colores ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# --- Configuración ---
PROTECTED_BRANCHES=("main" "develop" "HEAD")
BASE_BRANCH="develop"
DRY_RUN=false
AUTO_YES=false

# --- Parsear argumentos ---
for arg in "$@"; do
  case $arg in
    --dry-run) DRY_RUN=true ;;
    --yes|-y)  AUTO_YES=true ;;
    -h|--help)
      echo "Uso: bash scripts/cleanup-branches.sh [--dry-run] [--yes]"
      exit 0
      ;;
  esac
done

# --- Verificar que estamos en un repo git ---
if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
  echo -e "${RED}Error:${NC} No estás dentro de un repositorio git."
  exit 1
fi

# --- Verificar que existe la rama base ---
if ! git rev-parse --verify "$BASE_BRANCH" > /dev/null 2>&1; then
  echo -e "${RED}Error:${NC} No existe la rama '$BASE_BRANCH'."
  exit 1
fi

# --- Guardar rama actual ---
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# --- Función: ¿es una rama protegida? ---
is_protected() {
  local b="$1"
  for p in "${PROTECTED_BRANCHES[@]}"; do
    [[ "$b" == "$p" ]] && return 0
  done
  return 1
}

# --- Cabecera ---
echo ""
echo -e "${BOLD}${BLUE}🧹 Limpieza de ramas fusionadas en '$BASE_BRANCH'${NC}"
echo -e "${BLUE}──────────────────────────────────────────────${NC}"
if $DRY_RUN; then
  echo -e "${YELLOW}Modo DRY-RUN: no se borrará nada.${NC}"
fi
echo ""

# --- 1. Ramas locales fusionadas ---
echo -e "${BOLD}📍 Ramas locales fusionadas en '$BASE_BRANCH':${NC}"
LOCAL_MERGED=$(git branch --merged "$BASE_BRANCH" | sed 's/^[* ] //' | grep -v '^$' || true)

if [[ -z "$LOCAL_MERGED" ]]; then
  echo "  (ninguna)"
  LOCALS_TO_DELETE=()
else
  LOCALS_TO_DELETE=()
  while IFS= read -r branch; do
    [[ -z "$branch" ]] && continue
    if is_protected "$branch"; then
      echo -e "  ${GREEN}✓${NC} $branch ${BLUE}(protegida, se omite)${NC}"
    else
      echo -e "  ${YELLOW}→${NC} $branch"
      LOCALS_TO_DELETE+=("$branch")
    fi
  done <<< "$LOCAL_MERGED"
fi

echo ""

# --- 2. Ramas remotas fusionadas ---
echo -e "${BOLD}📍 Ramas remotas fusionadas en 'origin/$BASE_BRANCH':${NC}"
REMOTE_MERGED=$(git branch -r --merged "origin/$BASE_BRANCH" 2>/dev/null \
  | sed 's/^[* ] //' \
  | grep -v '^origin/HEAD' \
  | grep -v '^origin/main$' \
  | grep -v '^origin/develop$' \
  | grep '^origin/' \
  | sed 's|^origin/||' || true)

if [[ -z "$REMOTE_MERGED" ]]; then
  echo "  (ninguna)"
  REMOTES_TO_DELETE=()
else
  REMOTES_TO_DELETE=()
  while IFS= read -r branch; do
    [[ -z "$branch" ]] && continue
    if is_protected "$branch"; then
      echo -e "  ${GREEN}✓${NC} $branch ${BLUE}(protegida, se omite)${NC}"
    else
      echo -e "  ${YELLOW}→${NC} $branch"
      REMOTES_TO_DELETE+=("$branch")
    fi
  done <<< "$REMOTE_MERGED"
fi

echo ""

# --- 3. Nada que hacer ---
TOTAL=$(( ${#LOCALS_TO_DELETE[@]} + ${#REMOTES_TO_DELETE[@]} ))
if [[ $TOTAL -eq 0 ]]; then
  echo -e "${GREEN}✨ No hay ramas para borrar. Todo limpio.${NC}"
  exit 0
fi

# --- 4. Confirmar ---
if ! $DRY_RUN && ! $AUTO_YES; then
  echo -e "${BOLD}Se van a borrar $TOTAL rama(s):${NC}"
  echo -e "  ${YELLOW}Locales:${NC}  ${#LOCALS_TO_DELETE[@]}"
  echo -e "  ${YELLOW}Remotas:${NC}  ${#REMOTES_TO_DELETE[@]}"
  echo ""
  read -r -p "¿Continuar? [s/N] " confirm
  if [[ ! "$confirm" =~ ^[sSyY]$ ]]; then
    echo -e "${RED}Cancelado.${NC}"
    exit 0
  fi
fi

# --- 5. Volver a una rama segura si estamos en una que se va a borrar ---
for b in "${LOCALS_TO_DELETE[@]}"; do
  if [[ "$b" == "$CURRENT_BRANCH" ]]; then
    echo -e "${YELLOW}Estás en '$CURRENT_BRANCH'. Cambiando a '$BASE_BRANCH'…${NC}"
    git checkout "$BASE_BRANCH"
    break
  fi
done

# --- 6. Borrar locales ---
if [[ ${#LOCALS_TO_DELETE[@]} -gt 0 ]]; then
  echo ""
  echo -e "${BOLD}🗑️  Borrando ramas locales…${NC}"
  for b in "${LOCALS_TO_DELETE[@]}"; do
    if $DRY_RUN; then
      echo -e "  ${YELLOW}[dry-run]${NC} git branch -d $b"
    else
      if git branch -d "$b" 2>/dev/null; then
        echo -e "  ${GREEN}✓${NC} $b"
      else
        echo -e "  ${RED}✗${NC} $b (falló, intenta con -D manualmente si estás seguro)"
      fi
    fi
  done
fi

# --- 7. Borrar remotas ---
if [[ ${#REMOTES_TO_DELETE[@]} -gt 0 ]]; then
  echo ""
  echo -e "${BOLD}🗑️  Borrando ramas remotas…${NC}"
  for b in "${REMOTES_TO_DELETE[@]}"; do
    if $DRY_RUN; then
      echo -e "  ${YELLOW}[dry-run]${NC} git push origin --delete $b"
    else
      if git push origin --delete "$b" 2>/dev/null; then
        echo -e "  ${GREEN}✓${NC} origin/$b"
      else
        echo -e "  ${RED}✗${NC} origin/$b (falló)"
      fi
    fi
  done
fi

echo ""
echo -e "${GREEN}${BOLD}✨ Limpieza completada.${NC}"
echo ""
