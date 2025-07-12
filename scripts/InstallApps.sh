#!/bin/bash
# Installation Client Radio Rosemont v0.7 (multi-mode)

RED='\e[31m'
GREEN='\e[32m'
NC='\033[0m' # No Color

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${RED}[WARN]${NC} $1"; }

# === Fonction: Générer le layout plasma panels ===
generate_plasma_panel_layout_js() {
  local TARGET_USER="$1"
  local USER_HOME=$(eval echo "~$TARGET_USER")
  local LAYOUT_DIR="$USER_HOME/.local/share/radio-rose"
  local LAYOUT_JS="$LAYOUT_DIR/layout.js"

  mkdir -p "$LAYOUT_DIR"

  cat <<'EOS' > "$LAYOUT_JS"
/** RadioRosemont – top & bottom panels + wallpaper **/
panelIds.forEach(id => panelById(id).remove());

function getDefaultLayout() {
  return { panels:[
    { location:"top", height:36, expand:true, widgets:[
        "org.kde.plasma.showActivityManager",
        "org.kde.plasma.appmenu",
        "org.kde.plasma.panelspacer",
        "org.kde.plasma.systemtray",
        "org.kde.plasma.digitalclock"
    ]},
    { location:"bottom", alignment:"center", height:36, autoWidth:true, widgets:[
        "org.kde.plasma.kickoff",
        "org.kde.plasma.pager",
        "org.kde.plasma.icontasks",
        "org.kde.plasma.showdesktop"
    ]}
  ], desktops:[] };
}

// Appliquer le layout des panneaux
getDefaultLayout().panels.forEach(conf=>{
  p=new Panel(); p.location=conf.location; p.height=conf.height;
  [[conf.expand,"expand"],[conf.alignment,"alignment"],[conf.autoWidth,"autoWidth"]].forEach(([val,prop])=>{
    if(val) p[prop]=val;
  });
  if(conf.autoWidth){ p.lengthUnit="Pixel"; p.length=conf.widgets.length*conf.height*1.1; }
  conf.widgets.forEach(id=>p.addWidget(id));
});

// Définir le fond d’écran pour le bureau principal
const wallpaperPath = "/home/USERNAME/Images/wallpaper.png";
desktops().forEach(d => {
  d.wallpaperPlugin = "org.kde.image";
  d.currentConfigGroup = ["Wallpaper", "org.kde.image", "General"];
  d.writeConfig("Image", "file://" + wallpaperPath);
});
EOS

  # Remplace USERNAME par le vrai nom d'utilisateur dans le JS
  sed -i "s|USERNAME|$TARGET_USER|g" "$LAYOUT_JS"

  chmod 644 "$LAYOUT_JS"
  chown "$TARGET_USER:$TARGET_USER" "$LAYOUT_JS"
  echo "$LAYOUT_JS"
}


# === Fonction: Créer les activités KDE ===
create_kde_activities() {
  local TARGET_USER="$1"
  local USER_HOME=$(eval echo "~$TARGET_USER")

  export DISPLAY=:0
  export XAUTHORITY="$USER_HOME/.Xauthority"
  export DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$(id -u "$TARGET_USER")/bus"

  local ACTIVITIES=(
    "Production Audio"
    "Radio en direct"
    "Vidéo en direct"
    "Production Vidéo"
  )

  for ACTIVITY in "${ACTIVITIES[@]}"; do
    sudo -u "$TARGET_USER" DISPLAY="$DISPLAY" DBUS_SESSION_BUS_ADDRESS="$DBUS_SESSION_BUS_ADDRESS" \
      kactivities-cli --create-activity "$ACTIVITY"
    sleep 0.5
  done
}

# === Fonction: Appliquer automatiquement le layout avec fond d'écran ===
apply_plasma_layout() {
  echo "💥  Configuration automatique du layout Plasma (RadioRosemont)…"
  TARGET_USER="${SUDO_USER:-$USER}"
  USER_HOME=$(eval echo "~$TARGET_USER")

  export DISPLAY=:0
  export XAUTHORITY="$USER_HOME/.Xauthority"
  export DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$(id -u "$TARGET_USER")/bus"

  if ! command -v qdbus &>/dev/null; then
    info "Installation de qt5-tools (qdbus)..."
    sudo pacman -S --noconfirm qt5-tools
  fi

  if ! command -v kactivities-cli &>/dev/null; then
    info "Installation de kactivities-cli..."
    sudo pacman -S --noconfirm kactivities5 kactivitymanagerd kactivities-stats5
  fi

  local LAYOUT_JS_PATH
  LAYOUT_JS_PATH=$(generate_plasma_panel_layout_js "$TARGET_USER")

 # === Fonds d'écran Plasma personnalisés (Radio Rosemont) ===
info "Téléchargement des fonds d'écran Plasma personnalisés..."

# Répertoire utilisateur (défaut : $HOME si TARGET_HOME vide)
WALLPAPER_DIR="${TARGET_HOME:-$HOME}/Images"
mkdir -p "$WALLPAPER_DIR"

# Liste des images à télécharger
WALLPAPER_BASE_URL="https://github.com/radio0but/IceWatch/releases/download/v0.0.1"
WALLPAPER_LIST=(
  "RadioDirect.png"
  "VideoDirect.png"
  "ProdAudio.png"
  "ProdVideo.png"
  "wallpaper.png"
  "chrome.png"
  "RosemontLogo.png"
  "IconeProdVid.png"
  "IconeProdAud.png"
  "IconeLiveVid.png"
  "IconeOnAir.png"
)

for FILE in "${WALLPAPER_LIST[@]}"; do
  URL="$WALLPAPER_BASE_URL/$FILE"
  DEST="$WALLPAPER_DIR/$FILE"
  info "Téléchargement de $FILE..."
  if curl -fsSL "$URL" -o "$DEST"; then
    [[ -n "$TARGET_USER" ]] && sudo chown "$TARGET_USER:$TARGET_USER" "$DEST"
  else
    warn "Échec du téléchargement de $FILE"
  fi
done

info "Fonds d'écran téléchargés dans $WALLPAPER_DIR"
  info "✅ Fichier layout généré à $LAYOUT_JS_PATH"

  echo "🔄 Application immédiate du layout…"
  sudo -u "$TARGET_USER" DISPLAY="$DISPLAY" DBUS_SESSION_BUS_ADDRESS="$DBUS_SESSION_BUS_ADDRESS" \
    qdbus org.kde.plasmashell /PlasmaShell org.kde.PlasmaShell.evaluateScript "$(cat "$LAYOUT_JS_PATH")"

  create_kde_activities "$TARGET_USER"
  info "Application du thème Rouge Rosemont (prompt Zsh)..."
  sudo sed -i \
  -e 's/POWERLEVEL9K_OS_ICON_FOREGROUND=.*/POWERLEVEL9K_OS_ICON_FOREGROUND=160/' \
  -e 's/POWERLEVEL9K_OS_ICON_BACKGROUND=.*/POWERLEVEL9K_OS_ICON_BACKGROUND=254/' \
  -e 's/POWERLEVEL9K_DIR_BACKGROUND=.*/POWERLEVEL9K_DIR_BACKGROUND=160/' \
  -e 's/POWERLEVEL9K_PROMPT_CHAR_OK_.*_FOREGROUND=.*/POWERLEVEL9K_PROMPT_CHAR_OK_{VIINS,VICMD,VIVIS,VIOWR}_FOREGROUND=160/' \
  /usr/share/zsh/p10k.zsh
PROFILE_NAME="RadioRosemont.profile"
PROFILE_PATH="$HOME/.local/share/konsole/$PROFILE_NAME"

info "Création du profil Konsole personnalisé : RadioRosemont..."

mkdir -p "$HOME/.local/share/konsole"

cat > "$PROFILE_PATH" <<EOF
[Appearance]
ColorScheme=WhiteOnBlack
Font=Hack Nerd Font Mono,11

[General]
Command=/bin/zsh
Environment=TERM=xterm-256color,COLORTERM=truecolor
Name=RadioRosemont
Parent=FALLBACK/
EOF

kwriteconfig5 --file konsolerc --group "Desktop Entry" --key DefaultProfile "$PROFILE_NAME"

info "Profil Konsole 'RadioRosemont' appliqué par défaut."

  info "Thème du prompt Manjaro modifié pour refléter les couleurs de Radio Rosemont."

  echo "✅ Section Plasma terminée."
}

# === Options d'installation ===
IMPORT_PLASMA="no"
UPDATE_ONLY="no"
ONLY_PLASMA="no"

for arg in "$@"; do
  case "$arg" in
    --plasma) IMPORT_PLASMA="yes" ;;
    --update) UPDATE_ONLY="yes" ;;
    --onlyplasma) ONLY_PLASMA="yes"; IMPORT_PLASMA="yes" ;;
  esac
done

if [[ "$ONLY_PLASMA" == "yes" ]]; then
  warn "Mode : Configuration Plasma uniquement (--onlyplasma)"
elif [[ "$IMPORT_PLASMA" == "yes" ]]; then
  warn "Mode : Installation + Import Plasma (--plasma)"
elif [[ "$UPDATE_ONLY" == "yes" ]]; then
  warn "Mode : Mise à jour uniquement (--update)"
else
  warn "Mode : Installation complète (sans Plasma)"
fi

if [[ "$IMPORT_PLASMA" == "yes" ]]; then
  apply_plasma_layout
  if [[ "$ONLY_PLASMA" == "yes" ]]; then
    warn "Fin du mode --onlyplasma"
    exit 0
  fi
fi

# (le reste du script suit — inchangé)

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

# === Ajout de l'alias tupdate (si absent) ===
INSTALL_LINE="alias tupdate='bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/InstallApps.sh) --update'"

# Fonction pour ajouter dans un fichier si la ligne est absente
add_alias_if_missing() {
    local file="$1"
    if [ -f "$file" ]; then
        if ! grep -Fxq "$INSTALL_LINE" "$file"; then
            echo "" >> "$file"
            echo "# Alias Radio Rosemont (mise à jour IceWatch)" >> "$file"
            echo "$INSTALL_LINE" >> "$file"
            echo "[INFO] Alias tupdate ajouté dans $file"
        else
            echo "[INFO] Alias tupdate déjà présent dans $file"
        fi
    fi
}

# Ajout dans ~/.zshrc et ~/.bashrc (supporte les deux shells)
add_alias_if_missing "$HOME/.zshrc"
add_alias_if_missing "$HOME/.bashrc"
# === Fin de la section update-only ===
if [[ "$UPDATE_ONLY" == "yes" ]]; then
    warn "Mise à jour terminée (mode update-only)."

    exit 0
fi
# === Montage du partage radioemissions via SMB/CIFS ===
info "Configuration du montage du partage radioemissions…"


# Demande de l'adresse IP ou du nom d'hôte du serveur IceWatch

exec 3</dev/tty
read -u 3 -rp "Entrez l'adresse IP ou le nom d'hôte du serveur IceWatch (par ex. 192.168.0.170) : " SERVER_IP


# Installer cifs-utils si nécessaire
sudo pacman -S --noconfirm cifs-utils

# si le module cifs n'est pas déjà chargé, on le charge
if ! modinfo cifs &> /dev/null; then
  warn "Le module CIFS est introuvable pour $(uname -r)."
  warn "Redémarer et réexécuter le script"
  exit 1
fi

# --- Début de la section montage dynamique ---


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


warn "Installation complète terminée !"





