#!/bin/bash

# Handle uninstall
if [[ "$1" == "--uninstall" ]]; then
    echo "🧹 Uninstalling IceWatch Stack..."
    systemctl stop icewatch
    systemctl disable icewatch
    docker rm -f owncast
    systemctl stop icecast2
    systemctl disable icecast2
    userdel icewatch 2>/dev/null
    rm -rf /opt/icewatch /etc/icewatch /etc/systemd/system/icewatch.service
    rm -rf /opt/owncast
    echo "✅ Uninstallation complete."
    exit 0
fi

echo "🚀 IceWatch Stack Installer for Debian (LXC-compatible)"
echo "This script installs Icecast, Liquidsoap, Owncast (Docker), and IceWatch."

# Ask for required information
read -p "Enter your Icecast admin username: " ICECAST_USER
read -s -p "Enter your Icecast admin password: " ICECAST_PASSWORD
echo
read -p "Enter your allowed frontend referer (e.g., https://example.com): " ALLOWED_REFERER
read -p "Enter your Owncast admin password: " OWNCAST_PASSWORD
read -p "Enter the port IceWatch should run on (default 9090): " ICEWATCH_PORT
ICEWATCH_PORT=${ICEWATCH_PORT:-9090}

# Get IP & generate master token
IP_ADDR=$(hostname -I | awk '{print $1}')
MASTER_TOKEN=$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 48)

# Update system
apt update && apt upgrade -y

# Install Icecast & Liquidsoap
apt install -y icecast2 liquidsoap default-jre curl docker.io docker-compose

# Ensure Icecast paths exist
mkdir -p /var/log/icecast2 /usr/share/icecast2/web /usr/share/icecast2/admin
chown -R icecast2:icecast /var/log/icecast2 /usr/share/icecast2

# Icecast config
echo "🧊 Writing Icecast config..."
cat <<EOC > /etc/icecast2/icecast.xml
<icecast>
    <location>Earth</location>
    <admin>icemaster@localhost</admin>
    <limits>
        <clients>100</clients>
        <sources>2</sources>
        <queue-size>524288</queue-size>
        <client-timeout>30</client-timeout>
        <header-timeout>15</header-timeout>
        <source-timeout>10</source-timeout>
        <burst-on-connect>1</burst-on-connect>
        <burst-size>65535</burst-size>
    </limits>
    <authentication>
        <source-password>${ICECAST_PASSWORD}</source-password>
        <relay-password>${ICECAST_PASSWORD}</relay-password>
        <admin-user>${ICECAST_USER}</admin-user>
        <admin-password>${ICECAST_PASSWORD}</admin-password>
    </authentication>
    <hostname>${IP_ADDR}</hostname>
    <listen-socket><port>8000</port></listen-socket>
    <listen-socket><port>8001</port><bind-address>${IP_ADDR}</bind-address></listen-socket>
    <http-headers><header name="Access-Control-Allow-Origin" value="*" /></http-headers>
    <fileserve>1</fileserve>
    <paths>
        <basedir>/usr/share/icecast2</basedir>
        <logdir>/var/log/icecast2</logdir>
        <webroot>/usr/share/icecast2/web</webroot>
        <adminroot>/usr/share/icecast2/admin</adminroot>
        <alias source="/" destination="/status.xsl"/>
    </paths>
    <logging>
        <accesslog>access.log</accesslog>
        <errorlog>error.log</errorlog>
        <loglevel>3</loglevel>
        <logsize>10000</logsize>
    </logging>
    <changeowner><user>icecast2</user><group>icecast</group></changeowner>
    <security><chroot>0</chroot></security>
</icecast>
EOC

# Enable Icecast
systemctl enable icecast2
systemctl restart icecast2

# Owncast (Docker)
echo "📦 Deploying Owncast..."
mkdir -p /opt/owncast
docker run -d --name owncast -p 8123:8080 -p 1935:1935 \
  -v /opt/owncast:/app/data \
  -e "ADMIN_PASSWORD=${OWNCAST_PASSWORD}" \
  gabekangas/owncast

# IceWatch
echo "📥 Downloading IceWatch..."
mkdir -p /opt/icewatch /etc/icewatch
curl -L https://github.com/radio0but/IceWatch/releases/download/v0.0.1/IceWatch.jar \
  -o /opt/icewatch/icewatch.jar

cat <<EOF > /etc/icewatch/application.properties
server.port=${ICEWATCH_PORT}
icewatch.master-token=${MASTER_TOKEN}
icewatch.allowed-domain=${ALLOWED_REFERER}
icewatch.owncast-url=http://localhost:8123
icewatch.icecast-stream-url=http://localhost:8000/radio
EOF

useradd -r -s /bin/false icewatch 2>/dev/null
chown -R icewatch:icewatch /opt/icewatch /etc/icewatch

cat <<EOF > /etc/systemd/system/icewatch.service
[Unit]
Description=IceWatch Radio Proxy
After=network.target

[Service]
Type=simple
User=icewatch
Group=icewatch
WorkingDirectory=/opt/icewatch
ExecStart=/usr/bin/java -jar /opt/icewatch/icewatch.jar --spring.config.location=file:/etc/icewatch/application.properties
Restart=always
RestartSec=5
Environment=JAVA_OPTS=-Xmx256m

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reexec
systemctl daemon-reload
systemctl enable icewatch
systemctl start icewatch


# ────────────────
# 9️⃣ Partage Samba
# ────────────────
echo "🔧 Installation et configuration du partage Samba (/srv/radioemissions)..."

# 1. Installer Samba
apt install -y samba

# 2. Créer le dossier de partage si nécessaire
mkdir -p /srv/radioemissions
chown -R nobody:nogroup /srv/radioemissions
chmod 2755 /srv/radioemissions

# 3. Ajouter la section de partage dans smb.conf
cat <<EOF >> /etc/samba/smb.conf

[radioemissions]
   comment = Radio émissions (AutoDJ / Live)
   path = /srv/radioemissions
   browseable = yes
   read only = no
   guest ok = yes
   force user = nobody
   create mask = 0664
   directory mask = 2775
EOF

# 4. Redémarrer Samba pour prendre en compte
systemctl enable smbd
systemctl restart smbd

echo "✅ Samba configuré : vous pouvez maintenant monter \\\\${IP_ADDR}\\radioemissions en lecture/écriture."

# ─────────────────────────────────────────────
# 🔄 Download & deploy du scheduler run.sh
# ─────────────────────────────────────────────
info "Téléchargement du scheduler run.sh dans /srv/radioemissions…"
curl -sL \
  https://github.com/radio0but/IceWatch/releases/download/v0.0.1/run.sh \
  -o /srv/radioemissions/run.sh \
  && chmod +x /srv/radioemissions/run.sh

# ─────────────────────────────────────────────
# ⚙️ Création du service systemd radio-scheduler
# ─────────────────────────────────────────────
cat <<EOF > /etc/systemd/system/radio-scheduler.service
[Unit]
Description=Scheduler des émissions RadioRosemont
After=network.target

[Service]
Type=simple
ExecStart=/srv/radioemissions/run.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

info "Activation du service radio-scheduler…"
systemctl daemon-reload
systemctl enable radio-scheduler
systemctl start radio-scheduler

# ─────────────────────────────────────────────
# 🔄 Déploiement du template Liquidsoap + play.sh
# ─────────────────────────────────────────────

TEMPLATE_URL="https://github.com/radio0but/IceWatch/releases/download/v0.0.1/radio.liq"
BASE_DIR="/srv/radioemissions"
TEMPLATE="$BASE_DIR/radio.liq.template"
DAYS=(dimanche lundi mardi mercredi jeudi vendredi samedi)

echo "[INFO] Téléchargement du template Liquidsoap…"
curl -sL "$TEMPLATE_URL" \
  | sed -e "s|__ICECAST_HOST__|${ICECAST_HOST}|g" \
        -e "s|__SOURCE_PASSWORD__|${SOURCE_PASSWORD}|g" \
  > "$TEMPLATE"
chown nobody:nogroup "$TEMPLATE"
chmod 664           "$TEMPLATE"
echo "[INFO] Template déposé : $TEMPLATE"

echo "[INFO] Création de play.sh dans chaque dossier horaire…"
for day in "${DAYS[@]}"; do
  for h in $(seq -w 1 24); do
    HOUR_DIR="$BASE_DIR/$day/$h"
    PLAY="$HOUR_DIR/play.sh"

    cat > "$PLAY" << 'EOF'
#!/usr/bin/env bash
set -euo pipefail

# Chemin du script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Template est à la racine du partage
TEMPLATE_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
TEMPLATE="$TEMPLATE_DIR/radio.liq.template"

if [[ -f "$TEMPLATE" ]]; then
  cp "$TEMPLATE" "$SCRIPT_DIR/radio.liq"
  echo "✔ radio.liq créé dans $SCRIPT_DIR"
else
  echo "✖ Template introuvable ($TEMPLATE)" >&2
  exit 1
fi
EOF

    chown nobody:nogroup "$PLAY"
    chmod 2775           "$PLAY"
    echo "  → play.sh déployé dans $HOUR_DIR"
  done
done

echo "[INFO] play.sh déployés dans tous les dossiers horaires."


# Final Info
echo
echo "🎉 ✅ IceWatch Stack successfully deployed!"
echo
echo "🔗 Access information:"
echo "🔊 Icecast Admin Panel: http://${IP_ADDR}:8000/admin/"
echo "    User:     ${ICECAST_USER}"
echo "    Password: ${ICECAST_PASSWORD}"
echo
echo "📺 Owncast Admin Panel: http://${IP_ADDR}:8123/admin/"
echo "    User:     admin"
echo "    Password: abc123 (default)"
echo "    RTMP URL: rtmp://${IP_ADDR}:1935/live"
echo "    Stream Key: abc123"
echo
echo "🛡️ IceWatch Token API: http://${IP_ADDR}:${ICEWATCH_PORT}/auth/token"
echo "    Allowed Referer: ${ALLOWED_REFERER}"
echo "    Master Token: ${MASTER_TOKEN}"
echo
echo "💡 To uninstall later: run this script with '--uninstall'"
