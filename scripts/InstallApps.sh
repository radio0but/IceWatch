#!/bin/bash
# Installation Client Radio Rosemont v0.7 (multi-mode)

RED='\e[31m'
GREEN='\e[32m'
NC='\033[0m' # No Color

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${RED}[WARN]${NC} $1"; }

# Vérifie option (--plasma ou --update)
IMPORT_PLASMA="no"
UPDATE_ONLY="no"

if [[ "$1" == "--plasma" ]]; then
    IMPORT_PLASMA="yes"
    warn "Mode : Installation + Import Plasma (--plasma)"
elif [[ "$1" == "--update" ]]; then
    UPDATE_ONLY="yes"
    warn "Mode : Mise à jour uniquement (--update)"
else
    warn "Mode : Installation complète (sans Plasma)"
fi

# === Mise à jour système ===
info "Mise à jour complète du système..."
sudo pacman -Syu --noconfirm

# === Mise à jour des paquets flatpak ===
info "Mise à jour des paquets flatpak..."
if ! command -v flatpak &> /dev/null; then
    info "Flatpak non trouvé, installation..."
    sudo pacman -S --noconfirm flatpak
    sudo flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo
fi
flatpak update -y

# === Mise à jour des AppImage (BUTT) ===
info "Mise à jour de BUTT AppImage..."
mkdir -p $HOME/.local/bin
rm -f $HOME/.local/bin/butt.AppImage
wget -O $HOME/.local/bin/butt.AppImage https://danielnoethen.de/butt/release/1.45.0/butt-1.45.0-x86_64.AppImage
chmod +x $HOME/.local/bin/butt.AppImage

# === Mise à jour de Google Chrome ===
info "Mise à jour de Google Chrome (manuel)..."
cd $HOME/Downloads

FILE=google-chrome-stable_current_x86_64.rpm
FOLDER=google-chrome-stable_current_x86_64

if [ -f "$FILE" ]; then
    warn "$FILE existe, suppression..."
    rm -f "$FILE"
fi

if [ -d "$FOLDER" ]; then
    warn "$FOLDER existe, suppression..."
    rm -rf "$FOLDER"
fi

info "Téléchargement de Chrome..."
wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm

info "Extraction du paquet Chrome..."
ark -ab google-chrome-stable_current_x86_64.rpm

cd google-chrome-stable_current_x86_64/

warn "Copie de Chrome (nécessite sudo)..."
sudo cp -R ./* /

info "Google Chrome mis à jour avec succès."

# === Fin de la section update-only ===
if [[ "$UPDATE_ONLY" == "yes" ]]; then
    warn "Mise à jour terminée (mode update-only)."
    exit 0
fi
# === Montage du partage radioemissions via SMB/CIFS ===
info "Configuration du montage du partage radioemissions…"

# Installer cifs-utils si nécessaire

sudo pacman -S --noconfirm cifs-utils

# si le module cifs n'est pas déjà chargé, on le charge
if ! modinfo cifs &> /dev/null; then
  warn "Le module CIFS est introuvable pour $(uname -r)."
  warn "Redémarer et réexécuter le script"
  exit 1
fi

# --- Début de la section montage dynamique ---

# Demande de l'adresse IP ou du nom d'hôte du serveur IceWatch
read -rp "Entrez l'adresse IP ou le nom d'hôte du serveur IceWatch (par ex. 192.168.0.170) : " SERVER_IP

# Définition des variables de montage
SERVER="//${SERVER_IP}/radioemissions"
MOUNT_POINT="$HOME/radioemissions"
FSTAB_ENTRY="${SERVER} ${MOUNT_POINT} cifs guest,uid=$(id -u),gid=$(id -g),iocharset=utf8 0 0"

# Création du point de montage s'il n'existe pas
if [ ! -d "$MOUNT_POINT" ]; then
  info "Création du point de montage : $MOUNT_POINT"
  mkdir -p "$MOUNT_POINT"
fi

# Ajout de l'entrée dans /etc/fstab si absente
if ! grep -qs "^${SERVER} " /etc/fstab; then
  info "Ajout de l'entrée dans /etc/fstab"
  echo "$FSTAB_ENTRY" | sudo tee -a /etc/fstab > /dev/null
fi

# Montage immédiat du partage
info "Montage du partage ${SERVER}…"
if sudo mount -t cifs "${SERVER}" "${MOUNT_POINT}" \
    -o guest,uid=$(id -u),gid=$(id -g),iocharset=utf8; then
  info "Partage radioemissions monté sous ${MOUNT_POINT}"
else
  warn "Échec du montage, vérifiez l’adresse, le module CIFS ou dmesg pour plus d’infos"
  sudo dmesg | tail -n 20
fi
# --- Fin de la section montage dynamique ---


# === Installation du groupe pro-audio ===
info "Installation du groupe pro-audio..."
sudo pacman -S --noconfirm pro-audio

# === Réinstallation de PipeWire-JACK ===
info "Réinstallation de pipewire-jack (après pro-audio)..."
sudo pacman -S --noconfirm pipewire-jack pipewire-alsa pipewire-pulse wireplumber

# === Installation des applications complémentaires (via pacman) ===
info "Installation des logiciels complémentaires (via pacman)..."

sudo pacman -S --noconfirm \
  obs-studio \
  reaper \
  carla \
  kdenlive \
  krita \
  gimp \
  inkscape \
  hydrogen \
  audacity

# === Installation des applications complémentaires (via flatpak) ===
info "Installation des logiciels complémentaires (via flatpak)..."

flatpak install -y flathub com.vscodium.codium
flatpak install -y flathub org.openshot.OpenShot
flatpak install -y flathub com.cuperino.qprompt
flatpak install -y flathub io.jamulus.Jamulus

# === Section Plasma (si demandé) ===
if [[ "$IMPORT_PLASMA" == "yes" ]]; then
    ARCHIVE="$HOME/PlasmaConfig_*.tar.gz"
    TARGET_USER="$USER"
    TARGET_HOME="/home/$USER"

    # Vérifier que l'archive existe
    if ls $ARCHIVE 1> /dev/null 2>&1; then
        ARCHIVE_FILE=$(ls -t $ARCHIVE | head -n 1)
        info "Archive trouvée : $ARCHIVE_FILE"
    else
        warn "Aucune archive PlasmaConfig_*.tar.gz trouvée dans $HOME !"
        exit 1
    fi

    TMPDIR="$HOME/.plasma-import-tmp"
    rm -rf "$TMPDIR"
    mkdir -p "$TMPDIR"

    info "Extraction de l'archive..."
    tar -xzf "$ARCHIVE_FILE" -C "$TMPDIR"

    info "Déploiement de ~/.config..."
    cp -r "$TMPDIR/.config/"* "$TARGET_HOME/.config/"

    info "Déploiement de ~/.local/share..."
    cp -r "$TMPDIR/.local/share/"* "$TARGET_HOME/.local/share/"

    for EXTRA in ".icons" ".fonts" ".themes"; do
        if [ -d "$TMPDIR/$EXTRA" ]; then
            info "Déploiement de $EXTRA ..."
            cp -r "$TMPDIR/$EXTRA" "$TARGET_HOME/"
        fi
    done

    info "Réglage des permissions..."
    sudo chown -R "$TARGET_USER:$TARGET_USER" \
        "$TARGET_HOME/.config" \
        "$TARGET_HOME/.local/share" \
        "$TARGET_HOME/.icons" \
        "$TARGET_HOME/.fonts" \
        "$TARGET_HOME/.themes" 2>/dev/null

    rm -rf "$TMPDIR"

    info "Relance de Plasma..."
    if systemctl --user status plasma-plasmashell.service > /dev/null 2>&1; then
        systemctl --user restart plasma-plasmashell.service
        info "Plasma relancé (Wayland)"
    else
        kquitapp6 plasmashell && kstart plasmashell
        info "Plasma relancé (X11)"
    fi

    warn "Déploiement Plasma terminé ! Redémarre KWin si nécessaire."
else
    info "Section Plasma ignorée."
fi

# === Fin ===
warn "Installation complète terminée !"
