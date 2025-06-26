#!/bin/bash
# Installation Client Radio Rosemont v0.7 (multi-mode)

RED='\e[31m'
GREEN='\e[32m'
NC='\033[0m' # No Color

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${RED}[WARN]${NC} $1"; }

# Vérifie option (--plasma ou --update)

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
exec 3</dev/tty
read -u 3 -rp "Entrez l'adresse IP ou le nom d'hôte du serveur IceWatch (par ex. 192.168.0.170) : " SERVER_IP
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
systemctl daemon-reload
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

# ──────────────────────────────────────────────────
# 🔲 Section Plasma Layout (si --plasma)
# ──────────────────────────────────────────────────
if [[ " $* " == *" --plasma "* ]]; then
  echo "🖥️  Configuration automatique du layout Plasma (RadioRosemont)…"

  # 1️⃣ Détection de l'utilisateur courant (pour autostart)
  TARGET_USER="${SUDO_USER:-$USER}"
  USER_HOME=$(eval echo "~$TARGET_USER")

  # 2️⃣ Déploiement du JS de layout
  LAYOUT_DIR="$USER_HOME/.local/share/radio-rose"
  LAYOUT_JS="$LAYOUT_DIR/layout.js"
  mkdir -p "$LAYOUT_DIR"

  cat > "$LAYOUT_JS" <<'EOS'
/**
 * RadioRosemont – top & bottom panels
 * Idempotent : supprime d'abord tous les panels existants
 */
panelIds.forEach(id => panelById(id).remove());

function getDefaultLayout() {
  return {
    panels: [
      // Barre du haut full-width
      {
        location: "top", height: 36, expand: true,
        widgets: [
          "org.kde.plasma.activitymanager",
          "org.kde.plasma.appmenu",
          "org.kde.plasma.panelspacer",
          "org.kde.plasma.systemtray",
          "org.kde.plasma.digitalclock"
        ]
      },
      // Barre du bas centered auto-width
      {
        location: "bottom", alignment: "center",
        height: 36, autoWidth: true,
        widgets: [
          "org.kde.plasma.kickoff",
          "org.kde.plasma.pager",
          "org.kde.plasma.icontasks",
          "org.kde.plasma.showdesktop"
        ]
      }
    ],
    desktops: []
  };
}

getDefaultLayout().panels.forEach(conf => {
  const p = new Panel();
  p.location = conf.location;
  p.height   = conf.height;
  if (conf.expand)    p.expand     = true;
  if (conf.alignment) p.alignment  = conf.alignment;
  if (conf.autoWidth) {
    p.lengthUnit = "Pixel";
    p.length     = conf.widgets.length * conf.height * 1.1;
  }
  conf.widgets.forEach(id => p.addWidget(id));
});
EOS

  chmod 644 "$LAYOUT_JS"
  echo "  → Layout JS déployé dans $LAYOUT_JS"

  # 3️⃣ Création du .desktop d’autostart
  AUTOSTART_DIR="$USER_HOME/.config/autostart"
  DESKTOP_FILE="$AUTOSTART_DIR/radio-rose-layout.desktop"
  mkdir -p "$AUTOSTART_DIR"

  cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Exec=sh -c 'qdbus org.kde.plasmashell /PlasmaShell org.kde.PlasmaShell.evaluateScript "\$(cat $LAYOUT_JS)"'
Hidden=false
NoDisplay=false
X-GIO-NoFuse=true
Name=RadioRosemont Layout
Comment=Applique le layout top/bottom panels RadioRosemont
EOF

  chmod 644 "$DESKTOP_FILE"
  chown -R "$TARGET_USER":"$TARGET_USER" "$LAYOUT_DIR" "$AUTOSTART_DIR"
  echo "  → Autostart configuré pour l’utilisateur $TARGET_USER"

  # 4️⃣ Application immédiate si tu es en session graphique de TARGET_USER
  if [ "$USER" = "$TARGET_USER" ] && [ -n "$DISPLAY" ] && pgrep -u "$TARGET_USER" plasmashell >/dev/null; then
    echo "🔄 Application immédiate du layout…"
    su - "$TARGET_USER" -c "qdbus org.kde.plasmashell /PlasmaShell org.kde.PlasmaShell.evaluateScript \"\$(cat $LAYOUT_JS)\""
  fi

  echo "✅ Section Plasma terminée."
fi


# === Fin ===
warn "Installation complète terminée !"
