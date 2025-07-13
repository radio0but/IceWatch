#!/bin/bash

# === Montage du partage Samba : owncastvideos ===
RED='\e[31m'
GREEN='\e[32m'
NC='\033[0m'

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${RED}[WARN]${NC} $1"; }

SHARE_NAME="owncastvideos"
LOCAL_MOUNT="$HOME/$SHARE_NAME"

exec 3</dev/tty
read -u 3 -rp "Entrez l'adresse IP du serveur IceWatch (ex. 192.168.0.170) : " SERVER_IP

REMOTE="//${SERVER_IP}/${SHARE_NAME}"

info "Installation de cifs-utils (si nécessaire)..."
sudo pacman -S --noconfirm cifs-utils

if ! modinfo cifs &> /dev/null; then
  warn "❌ Le module CIFS est introuvable. Redémarrez le système et relancez ce script."
  exit 1
fi

if [ ! -d "$LOCAL_MOUNT" ]; then
  info "Création du point de montage local : $LOCAL_MOUNT"
  mkdir -p "$LOCAL_MOUNT"
fi

FSTAB_ENTRY="${REMOTE} ${LOCAL_MOUNT} cifs guest,uid=$(id -u),gid=$(id -g),iocharset=utf8 0 0"
if ! grep -qs "^${REMOTE} " /etc/fstab; then
  info "Ajout à /etc/fstab pour montage automatique"
  echo "$FSTAB_ENTRY" | sudo tee -a /etc/fstab > /dev/null
fi

info "Montage immédiat du partage $SHARE_NAME..."
if sudo mount -t cifs "$REMOTE" "$LOCAL_MOUNT" -o guest,uid=$(id -u),gid=$(id -g),iocharset=utf8; then
  info "✅ Partage monté avec succès sous $LOCAL_MOUNT"
else
  warn "❌ Échec du montage. Vérifiez l’adresse IP ou les logs de dmesg"
  sudo dmesg | tail -n 20
fi
