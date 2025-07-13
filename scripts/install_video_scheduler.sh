#!/bin/bash

set -e

# === Installateur du Video Scheduler pour Owncast ===
echo "\n🎨 Installation du Video Scheduler (Owncast)"

# Vérification de la présence de ffmpeg
if ! command -v ffmpeg &> /dev/null; then
  echo "📦 ffmpeg non détecté. Installation en cours..."
  apt update && apt install -y ffmpeg
fi

# Dossier des vidéos et du planning
SCHEDULE_DIR="/srv/owncast-schedule"
# Dossier du script exécutable sécurisé
SCRIPT_DIR="/opt/owncast-scheduler"
SERVICE_NAME="video-scheduler"
SCRIPT_URL="https://github.com/radio0but/IceWatch/releases/download/v0.0.1/video_scheduler.sh"
SCRIPT_PATH="$SCRIPT_DIR/$SERVICE_NAME.sh"

# 0. Arrêt du service s'il est en cours d'exécution
if systemctl is-active --quiet "$SERVICE_NAME"; then
  echo "⏹ Arrêt temporaire du service $SERVICE_NAME pour permettre les tests..."
  systemctl stop "$SERVICE_NAME"
fi

# 1. Création des dossiers
mkdir -p "$SCHEDULE_DIR"
mkdir -p "$SCRIPT_DIR"
echo "📁 Dossiers créés : $SCHEDULE_DIR et $SCRIPT_DIR"

# Sécurisation du dossier du scheduler
chmod 755 "$SCRIPT_DIR"
chown root:root "$SCRIPT_DIR"

# Sécurisation du dossier de planning (pour Samba + scheduler)
chmod 2775 "$SCHEDULE_DIR"
chown -R nobody:nogroup "$SCHEDULE_DIR"
find "$SCHEDULE_DIR" -type d -exec chmod 2775 {} \;
find "$SCHEDULE_DIR" -type f -exec chmod 664 {} \;

# Détection IP locale pour affichage
SERVER_IP=$(hostname -I | awk '{print $1}')

# 2. Gestion de la stream key
if [[ "$1" == --reuse-key=* ]]; then
  STREAM_KEY="${1#*=}"
  echo "🔁 Reprise avec la même clé : $STREAM_KEY"
else
  echo ""
  echo "Souhaitez-vous utiliser une clé personnalisée ? (laisser vide pour générer une clé automatiquement)"
  read -p "Clé personnalisée (facultatif) : " CUSTOM_KEY
  if [[ -n "$CUSTOM_KEY" ]]; then
    STREAM_KEY="$CUSTOM_KEY"
  else
    STREAM_KEY=$(< /dev/urandom tr -dc 'A-Za-z0-9@#%+=_' | head -c32)
  fi

  echo "\n🔑 Clé de diffusion utilisée : $STREAM_KEY"
  echo "Veuillez l'ajouter manuellement dans l'interface d'administration Owncast :"
  echo "  → http://$SERVER_IP:8123/admin"
  echo "  → Identifiants par défaut : admin / abc123"
  echo "  → Menu Paramètres > Streaming Keys"
  echo "  → Ajouter la clé suivante : $STREAM_KEY"

  echo
  read -p "Appuyez sur Entrée lorsque la clé a été ajoutée, ou tapez 'q' pour annuler : " confirm
  if [[ "$confirm" == "q" ]]; then
    echo "❌ Installation annulée."
    exit 1
  fi
fi

# Test de connexion RTMP local avec loglevel détaillé (3 tentatives)
echo "🔍 Test de la connexion RTMP avec ffmpeg (3 tentatives)..."
echo "(ce test prend quelques secondes, logs affichés)"

set +e
FFMPEG_LOG=$(mktemp)
ATTEMPTS=0
SUCCESS=false

while [[ $ATTEMPTS -lt 3 ]]; do
  ATTEMPTS=$((ATTEMPTS + 1))
  echo "Tentative $ATTEMPTS..."
  ffmpeg -loglevel debug -f lavfi -i testsrc=size=320x240:rate=10 \
    -f lavfi -i sine=frequency=1000:sample_rate=44100 \
    -t 5 -c:v libx264 -c:a aac -f flv "rtmp://$SERVER_IP:1935/live/$STREAM_KEY" &> "$FFMPEG_LOG"
  FFMPEG_EXIT=$?

  if [[ $FFMPEG_EXIT -eq 0 ]]; then
    SUCCESS=true
    break
  fi
  sleep 1
  echo "...échec"
done

set -e

if [[ "$SUCCESS" != true ]]; then
  echo "\n🚧 La connexion a échoué après $ATTEMPTS tentatives."
  echo "--- Derniers messages ffmpeg ---"
  tail -n 10 "$FFMPEG_LOG"
  echo "--------------------------------"
  echo "(La clé peut être incorrecte ou Owncast ne l'a pas encore enregistrée.)"
  while true; do
    echo "Que souhaitez-vous faire ?"
    select choice in "Relancer le test avec la même clé" "Générer une nouvelle clé et recommencer" "Entrer une nouvelle clé manuellement" "Continuer sans vérifier" "Annuler l'installation"; do
      case $REPLY in
        1)
          echo "🔁 Re-test avec la même clé..."
          exec "$0" "--reuse-key=$STREAM_KEY"
          ;;
        2)
          echo "🔁 Nouvelle clé en cours..."
          exec "$0"
          ;;
        3)
          read -p "Nouvelle clé personnalisée : " CUSTOM_KEY
          exec "$0" "--reuse-key=$CUSTOM_KEY"
          ;;
        4)
          echo "⚠ Continuer sans vérification. Le service pourrait ne pas fonctionner."
          break 2
          ;;
        5)
          echo "❌ Installation annulée."
          exit 1
          ;;
        *)
          echo "Choix invalide."
          ;;
      esac
    done
  done
else
  echo "✅ Connexion testée avec succès."
fi

# 3. Téléchargement du script principal
curl -sL "$SCRIPT_URL" -o "$SCRIPT_PATH"
sed -i "s|^STREAM_KEY=\"\\\${STREAM_KEY:-changeme}\"|STREAM_KEY=\"$STREAM_KEY\"|" "$SCRIPT_PATH"
sed -i "s|^OWNCAST_RTMP=.*|OWNCAST_RTMP=\"rtmp://$SERVER_IP:1935/live\"|" "$SCRIPT_PATH"
chmod 755 "$SCRIPT_PATH"
chown root:root "$SCRIPT_PATH"
echo "📅 Script téléchargé et sécurisé : $SCRIPT_PATH"

# 4. Création du service systemd si différent
SERVICE_FILE="/etc/systemd/system/$SERVICE_NAME.service"
read -r -d '' SYSTEMD_CONTENT <<EOF
[Unit]
Description=Owncast Video Scheduler
After=network.target

[Service]
Type=simple
ExecStart=$SCRIPT_PATH
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

echo "$SYSTEMD_CONTENT" | cmp -s - "$SERVICE_FILE" || echo "$SYSTEMD_CONTENT" > "$SERVICE_FILE"

echo "🔧 Service systemd installé : $SERVICE_NAME"

# 5. Activation et démarrage
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl restart "$SERVICE_NAME"
echo "✅ Service $SERVICE_NAME activé et démarré"

# 6. Ajout ou modification du partage Samba
SHARE_BLOCK="[owncastvideos]
   comment = Vidéos horaires Owncast
   path = $SCHEDULE_DIR
   browseable = yes
   writable = yes
   read only = no
   guest ok = yes
   create mask = 0666
   directory mask = 2775
   force user = nobody
   force group = nogroup"

if grep -q "\[owncastvideos\]" /etc/samba/smb.conf; then
  echo "🔁 Mise à jour de la configuration Samba existante pour [owncastvideos]..."
  sed -i "/\[owncastvideos\]/,/^$/c\
$SHARE_BLOCK\
" /etc/samba/smb.conf
else
  echo "➕ Ajout du partage Samba [owncastvideos]..."
  echo -e "\n$SHARE_BLOCK\n" >> /etc/samba/smb.conf
fi

systemctl restart smbd

# 7. Informations finales
echo "\n🚀 Interface Owncast disponible ici : http://$SERVER_IP:8123"
echo "🔑 Identifiants d'administration par défaut : admin / abc123"
echo "\nPour commencer :"
echo "1. Connectez-vous à l'interface Owncast."
echo "2. Allez dans Paramètres > Streaming Keys."
echo "3. Ajoutez cette clé : $STREAM_KEY"
echo "4. Déposez des vidéos dans le dossier de diffusion selon l'horaire."
echo "5. Créez un fichier \"play\" pour activer une plage horaire."

echo "\n🎉 Installation complète. Vous pouvez maintenant déposer vos vidéos dans :"
echo "  \\\$HOSTNAME\\owncastvideos\\JOUR\\HEURE\\video\\"
echo "N'oubliez pas de créer un fichier \"play\" pour activer la diffusion."
