#!/usr/bin/env bash

# === Variables ===
JAR_NAME="icewatch.jar"
LOCAL_JAR_PATH="target/$JAR_NAME"
REMOTE_USER="root"
REMOTE_HOST="192.168.0.170"
REMOTE_PATH="/opt/icewatch/$JAR_NAME"
SERVICE_NAME="icewatch.service"

# === Vérifie si le fichier existe localement ===
if [[ ! -f "$LOCAL_JAR_PATH" ]]; then
  echo "❌ Le fichier $LOCAL_JAR_PATH est introuvable. Compile d'abord avec mvn clean package."
  exit 1
fi

echo "📦 Transfert de $JAR_NAME vers $REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH"
scp "$LOCAL_JAR_PATH" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH"

if [[ $? -ne 0 ]]; then
  echo "❌ Échec du transfert SCP."
  exit 1
fi

echo "🔁 Redémarrage de $SERVICE_NAME sur $REMOTE_HOST"
ssh "$REMOTE_USER@$REMOTE_HOST" "systemctl restart $SERVICE_NAME"

if [[ $? -eq 0 ]]; then
  echo "✅ Déploiement terminé avec succès."
else
  echo "⚠️ Problème lors du redémarrage du service."
fi
