#!/bin/bash

# Utilitaire de conversion DOCX → Markdown pour les admins
# Dépendance : pandoc (installée automatiquement si absente)
# Auteur : IceWatch Admin Toolkit

set -e

# === Couleurs ===
GREEN='\033[1;32m'
RED='\033[1;31m'
NC='\033[0m'

echo -e "${GREEN}[INFO] Vérification de Pandoc...${NC}"

if ! command -v pandoc &>/dev/null; then
    echo -e "${RED}[WARN] pandoc non installé. Installation en cours...${NC}"
    sudo pacman -S --noconfirm pandoc
else
    echo -e "${GREEN}[OK] pandoc est déjà installé.${NC}"
fi

# === Dossier source (ou actuel) ===
TARGET_DIR="${1:-.}"

echo -e "${GREEN}[INFO] Dossier à traiter : ${TARGET_DIR}${NC}"

# === Conversion ===
shopt -s nullglob
FILES=("$TARGET_DIR"/*.docx)

if [ ${#FILES[@]} -eq 0 ]; then
    echo -e "${RED}[AUCUN] Aucun fichier .docx trouvé dans ${TARGET_DIR}${NC}"
    exit 1
fi

for file in "${FILES[@]}"; do
    base=$(basename "$file" .docx)
    output="${TARGET_DIR}/${base}.md"

    echo -e "${GREEN}[CONVERT] ${file} → ${output}${NC}"
    pandoc "$file" -f docx -t markdown -o "$output"
done

echo -e "${GREEN}[TERMINE] Tous les fichiers DOCX ont été convertis.${NC}"
