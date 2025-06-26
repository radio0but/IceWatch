#!/bin/bash
# client.sh - Installe les clients Radio Rosemont
# Version 1.1 — par Marc-André et ChatGPT — 2025

RED='\e[31m'
GREEN='\e[32m'
NC='\033[0m'

INSTALL_SCRIPT="InstallApps.sh"
INSTALL_URL="https://github.com/radio0but/IceWatch/releases/download/v0.0.1/$INSTALL_SCRIPT"

# Fonction info
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${RED}[WARN]${NC} $1"; }

# On récupère les arguments
ARGS="$@"

# Aller dans le home utilisateur
cd $HOME

# Nettoyer d'anciens fichiers si présents
info "Nettoyage d'éventuels anciens fichiers..."
rm -f "$INSTALL_SCRIPT"
rm -f PlasmaConfig_*.tar.gz

# Télécharger InstallApps.sh
info "Téléchargement de $INSTALL_SCRIPT..."
wget -q --show-progress "$INSTALL_URL"

# Rendre exécutable
chmod +x "$INSTALL_SCRIPT"

# Si --plasma présent → télécharger l'archive Plasma

# Exécution de InstallApps.sh avec les mêmes arguments
info "Exécution de $INSTALL_SCRIPT avec les options : $ARGS"
./$INSTALL_SCRIPT $ARGS

# Nettoyage final
info "Nettoyage final..."
rm -f "$INSTALL_SCRIPT"
rm -f PlasmaConfig_*.tar.gz


# Nettoyage fichiers temporaires Chrome
info "Nettoyage des fichiers temporaires de Chrome..."
rm -f "google-chrome-stable_current_x86_64.rpm"
rm -rf "google-chrome-stable_current_x86_64"


# Auto-suppression du script
SCRIPT_PATH=$(realpath "$0")
info "Suppression du script lui-même ($SCRIPT_PATH)..."
rm -f "$SCRIPT_PATH"

warn "Installation terminée !"
