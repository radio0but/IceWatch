#!/bin/bash

# ––– désactive toute interaction debconf / APT
export DEBIAN_FRONTEND=noninteractive
export DEBIAN_PRIORITY=critical
export APT_LISTCHANGES_FRONTEND=none

# ––– force needrestart à redémarrer sans poser de question
export NEEDRESTART_MODE=a

# ––– si vous utilisez ucf pour les fichiers de config
export UCF_FORCE_CONFFNEW=1

info() {
  echo -e "[INFO] $1"
}

echo
read -p "Fuseau horaire du serveur (défaut: America/Toronto) : " TIMEZONE
TIMEZONE=${TIMEZONE:-America/Toronto}

echo "[INFO] Application du fuseau horaire : $TIMEZONE"
timedatectl set-timezone "$TIMEZONE"
timedatectl set-ntp true

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

echo
read -s -p "Mot de passe pour l'utilisateur local 'admin' (ex: admin123) : " LOCAL_ADMIN_PW; echo
read -s -p "Mot de passe pour l'utilisateur local 'enseignant' (ex: radio2025) : " LOCAL_ENSEIGNANT_PW; echo

ICEWATCH_PORT=${ICEWATCH_PORT:-9090}

# Get IP & generate master token
IP_ADDR=$(hostname -I | awk '{print $1}')
MASTER_TOKEN=$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 48)

# Update system
apt update && apt upgrade -y

apt install -y apache2-utils

HASH_ADMIN=$(htpasswd -nbBC 10 "" "$LOCAL_ADMIN_PW" | tr -d ':\n')
HASH_ENSEIGNANT=$(htpasswd -nbBC 10 "" "$LOCAL_ENSEIGNANT_PW" | tr -d ':\n')



echo "🐘 Installation de PostgreSQL + sudo..."
apt install -y postgresql sudo

echo "🛠️ Configuration PostgreSQL (user icewatch)..."
sudo -u postgres psql <<EOF
CREATE USER icewatch WITH PASSWORD '${LOCAL_ADMIN_PW}';
ALTER USER icewatch CREATEDB;
CREATE DATABASE icewatchdb OWNER icewatch;
EOF


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
echo "📥 Téléchargement IceWatch..."
mkdir -p /opt/icewatch /etc/icewatch
curl -L https://github.com/radio0but/IceWatch/releases/download/v0.0.1/IceWatch.jar \
  -o /opt/icewatch/icewatch.jar

cat <<EOF > /etc/icewatch/application.properties
# === IceWatch
server.port=${ICEWATCH_PORT}
icewatch.master-token=${MASTER_TOKEN}
icewatch.allowed-domain=${ALLOWED_REFERER}
icewatch.owncast-url=http://localhost:8123
icewatch.icecast-stream-url=http://localhost:8000/radio
icewatch.admin-password=${LOCAL_ADMIN_PW}
icewatch.enseignant-password=${LOCAL_ENSEIGNANT_PW}

# === PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/icewatchdb
spring.datasource.username=icewatch
spring.datasource.password=${LOCAL_ADMIN_PW}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true
EOF

echo
read -p "Souhaitez-vous activer l’authentification LDAP maintenant ? (o/N): " ENABLE_LDAP
ENABLE_LDAP=$(echo "$ENABLE_LDAP" | tr '[:upper:]' '[:lower:]')

if [[ "$ENABLE_LDAP" == "o" || "$ENABLE_LDAP" == "oui" || "$ENABLE_LDAP" == "y" ]]; then
    read -p "Adresse du serveur LDAP (ex : ldap://192.168.0.8:389): " LDAP_URL
    read -p "Base DN LDAP (ex : dc=radio,dc=boogiepit,dc=com): " LDAP_BASE
    read -p "DN d’admin LDAP (ex : cn=admin,dc=radio,dc=boogiepit,dc=com): " LDAP_BIND_DN
    read -s -p "Mot de passe admin LDAP : " LDAP_BIND_PW; echo

    # Ajoute la config LDAP à l’application IceWatch
    cat <<EOF >> /etc/icewatch/application.properties
spring.ldap.urls=${LDAP_URL}
spring.ldap.base=${LDAP_BASE}
spring.ldap.username=${LDAP_BIND_DN}
spring.ldap.password=${LDAP_BIND_PW}
# Patterns pour utilisateurs/groupes LDAP standard
spring.security.ldap.user-dn-patterns=uid={0},ou=Users
spring.security.ldap.authorities.group-search-base=ou=Groups
spring.ldap.user-dn-patterns=uid={0},ou=Users,${LDAP_BASE}
EOF

    echo "✅ Authentification LDAP configurée."
else
    echo "ℹ️ Vous pourrez activer LDAP plus tard en éditant /etc/icewatch/application.properties"
    echo "   Utilisez les comptes locaux suivants pour tester :"
    echo "   - admin / admin123"
    echo "   - enseignant / radio2025"
fi

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
echo "[INFO] Démarrage d'IceWatch pour générer les tables PostgreSQL…"
systemctl start icewatch

# Attend que la table local_user soit disponible (Hibernate)
until sudo -u postgres psql -d icewatchdb -c "\dt" | grep -q local_user; do
  echo "⏳ En attente de la création de la table local_user..."
  sleep 2
done

sudo -u postgres psql -d icewatchdb <<EOF
INSERT INTO local_user (username, password, roles) VALUES
  ('admin',     '$HASH_ADMIN', 'USER,ADMIN'),
  ('enseignant','$HASH_ENSEIGNANT', 'USER')
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password, roles = EXCLUDED.roles;
EOF



echo "🔐 Ajout des permissions sudo pour icewatch (journalctl & restart services)..."
cat <<EOF > /etc/sudoers.d/icewatch-restart
icewatch ALL=(ALL) NOPASSWD: /bin/systemctl restart icewatch, \
                             /bin/systemctl restart video-scheduler.service, \
                             /bin/systemctl restart radio-scheduler.service

icewatch ALL=(ALL) NOPASSWD: /bin/journalctl -u icewatch, \
                             /bin/journalctl -u radio-scheduler.service, \
                             /bin/journalctl -u video-scheduler.service

icewatch ALL=(ALL) NOPASSWD: /bin/journalctl -u video-scheduler.service --no-pager -n 50
icewatch ALL=(ALL) NOPASSWD: /bin/journalctl -u radio-scheduler.service --no-pager -n 50
EOF

chmod 440 /etc/sudoers.d/icewatch-restart

visudo -cf /etc/sudoers.d/icewatch-restart


# ────────────────
# 9️⃣ Partage Samba
# ────────────────
echo "🔧 Installation et configuration du partage Samba (/srv/radioemissions)..."
apt-get install -y \
  -o Dpkg::Options::="--force-confdef" \
  -o Dpkg::Options::="--force-confold" \
  samba

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
  | sed -e "s|__ICECAST_HOST__|${IP_ADDR}|g" \
        -e "s|__SOURCE_PASSWORD__|${ICECAST_PASSWORD}|g" \
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
# === Téléchargement et exécution du script d'installation du Video Scheduler ===
SCHEDULER_INSTALL_URL="https://github.com/radio0but/IceWatch/releases/download/v0.0.1/install_video_scheduler.sh"
SCHEDULER_INSTALL_SCRIPT="/tmp/install_video_scheduler.sh"

# Téléchargement sécurisé
curl -fsSL "$SCHEDULER_INSTALL_URL" -o "$SCHEDULER_INSTALL_SCRIPT"
chmod +x "$SCHEDULER_INSTALL_SCRIPT"

# Exécution
"$SCHEDULER_INSTALL_SCRIPT"

# ─────────────────────────────────────────────
# ✅ Vérification/Création manuelle du service video-scheduler (fallback)
# ─────────────────────────────────────────────
SERVICE_FILE="/etc/systemd/system/video-scheduler.service"
SCRIPT_PATH="/opt/owncast-scheduler/video-scheduler.sh"

if [[ -x "$SCRIPT_PATH" ]]; then
  echo "[INFO] Vérification du service video-scheduler…"
  if [[ ! -f "$SERVICE_FILE" ]]; then
    echo "[INFO] Création du service systemd video-scheduler manquant."
    cat <<EOF > "$SERVICE_FILE"
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
  fi

  systemctl daemon-reload
  systemctl enable video-scheduler
  systemctl restart video-scheduler
  echo "[✅] Service video-scheduler actif."
else
  echo "[⚠️] Script $SCRIPT_PATH introuvable ou non exécutable. Service video-scheduler non activé."
fi

# ─────────────────────────────────────────────
# 📂 Vérification/ajout du partage Samba [owncastvideos]
# ─────────────────────────────────────────────
SHARE_BLOCK="[owncastvideos]
   comment = Vidéos horaires Owncast
   path = /srv/owncast-schedule
   browseable = yes
   writable = yes
   read only = no
   guest ok = yes
   create mask = 0666
   directory mask = 2775
   force user = nobody
   force group = nogroup"

if grep -q "^\[owncastvideos\]" /etc/samba/smb.conf; then
  echo "[INFO] Mise à jour du bloc Samba existant [owncastvideos]..."
  sed -i "/^\[owncastvideos\]/,/^$/c\\
$SHARE_BLOCK\\
" /etc/samba/smb.conf
else
  echo "[INFO] Ajout du bloc Samba [owncastvideos]…"
  echo -e "\n$SHARE_BLOCK\n" >> /etc/samba/smb.conf
fi
find /srv/owncast-schedule -type d -exec chmod 2775 {} \;
find /srv/owncast-schedule -type f -exec chmod 664 {} \;
chown -R nobody:nogroup /srv/owncast-schedule

systemctl restart smbd
echo "[✅] Partage Samba [owncastvideos] prêt."


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
