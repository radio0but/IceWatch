#!/bin/bash

# === Convertisseur Vidéo Compatible Scheduler IceWatch ===
# Envoie les vidéos converties dans un dossier "verified/" à côté du fichier source
# Auteur : IceWatch Toolkit
# Usage : ./convert-video-compatible.sh video1.mp4 video2.mkv ...

set -e

# === Couleurs ===
GREEN='\033[1;32m'
RED='\033[1;31m'
NC='\033[0m'

# === Vérification de ffmpeg ===
if ! command -v ffmpeg &>/dev/null; then
  echo -e "${RED}[ERREUR] ffmpeg non trouvé.${NC}"
  echo -e "${GREEN}[INFO] Installation...${NC}"
  sudo pacman -S --noconfirm ffmpeg
fi

# === Aide si aucun fichier passé ===
if [[ "$#" -eq 0 ]]; then
  echo -e "${RED}Utilisation : $0 fichier1.mp4 fichier2.mkv ...${NC}"
  exit 1
fi

for input in "$@"; do
  if [[ ! -f "$input" ]]; then
    echo -e "${RED}[IGNORÉ] Fichier introuvable : $input${NC}"
    continue
  fi

  input_dir="$(dirname "$input")"
  input_name="$(basename -- "$input")"
  base_name="${input_name%.*}"
  verified_dir="${input_dir}/verified"
  output="${verified_dir}/${base_name}.mp4"

  mkdir -p "$verified_dir"

  echo -e "${GREEN}[CONVERSION] $input → $output${NC}"

  ffmpeg -y -i "$input" \
    -c:v libx264 -preset fast -b:v 1500k \
    -c:a aac -b:a 128k -ar 44100 \
    -movflags +faststart \
    "$output"

  echo -e "${GREEN}[OK] Vidéo prête : $output${NC}"
done
