## 📱 IceWatch – Système sécurisé de diffusion audio/vidéo

**IceWatch** est un backend Java (Spring Boot) conçu pour **sécuriser et proxyfier les flux audio et vidéo** diffusés via [Icecast](https://icecast.org) et [Owncast](https://owncast.online). Il permet de restreindre l'accès par jeton dynamique, de valider l'origine des requêtes (Referer), et de diffuser les flux dans un **portail sécurisé compatible LDAP**.

---

### 🎯 Fonctionnalités

- 🔐 **Accès par jeton dynamique** basé sur le `Referer`
- 🧾 **Token maître** pour les intégrations de confiance (ex. : Omnivox, apps mobiles)
- 🛡️ **Reverse proxy intégré** pour tous les flux Owncast/Icecast (1 seul domaine exposé)
- 🌐 **CORS configurable** pour l’intégration frontend
- ⚙️ **Configuration centralisée** (`application.properties`)
- 🔑 **Portail de connexion** avec support LDAP ou comptes locaux
- 🧑‍💼 **Tableau de bord admin** pour la gestion des utilisateurs et du système
- 🎮 **Ordonnanceur Owncast** : diffusion automatisée de dossiers vidéo
- 🎧 **Scheduler Radio (Liquidsoap)** : diffusion audio horaire via dossiers et script `run.sh`

---

### 🚀 Déploiement serveur

```bash
curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/install.sh | bash
```

Le script installe Icecast, Owncast, Liquidsoap et IceWatch automatiquement sur un serveur Debian ou LXC. Il configure aussi les partages Samba et les services systemd.

---

### 📁 Structure du projet

- `/src/main/java/` – Backend Spring Boot (authentification, proxy, sécurité)
- `/src/main/resources/static/` – Interface HTML + JavaScript
- `application.properties` – Fichier de configuration centralisé
- `scripts/run.sh` – Scheduler Liquidsoap (audio)
- `scripts/video-scheduler.sh` – Scheduler ffmpeg/Owncast (vidéo)
- `README.md`, `LICENSE`, etc.

---

### 🚤 Déploiement des postes clients

```bash
bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/InstallApps.sh) --plasma
```

Ce script configure automatiquement un poste Manjaro KDE avec :

- Mise à jour système
- Installation de Mixxx, OBS, QjackCtl, Kdenlive, etc.
- Import des paramètres visuels Plasma (optionnel)
- Montage automatique du partage `~/radioemissions`

Options disponibles :

- `--plasma` : installation + configuration graphique
- `--update` : mise à jour uniquement

---

### 🎵 Scheduler Radio (Liquidsoap)

- Utilise des dossiers par jour/heure (`radioemissions/jour/heure/`)
- `radio.liq` déclenche l'AutoDJ pour une plage horaire
- Fichier `live` permet d'interrompre le scheduler pour une diffusion en direct (via Mixxx)
- Scheduler activé par `run.sh` tournant en service systemd

---

### 🎥 Scheduler Vidéo (Owncast + FFmpeg)

- Dossiers organisés par jour/heure dans `/srv/owncast-schedule`
- Sous-dossier `video/` contient les vidéos à lire
- Fichier `play` active la diffusion automatique
- Fichier `live` permet d’interrompre pour du direct OBS
- Script `video-scheduler.sh` actif en tâche de fond

---

### 📒 Documentation complète

Voir le dossier `docs/` ou la [documentation pédagogique](https://github.com/radio0but/IceWatch/wiki) *(en développement)*.

---

## 📱 IceWatch – Secure audio/video proxy backend

**IceWatch** is a secure Spring Boot backend designed to proxy and protect audio/video streams served by **Icecast** and **Owncast**. It provides token-based access control, referer validation, and a secure frontend portal with optional LDAP support.

---

### 🎯 Features

- 🔐 **Token-based access** linked to the `Referer` header
- 🧾 **Master token** for trusted systems like Omnivox or mobile apps
- 🛡️ **Built-in reverse proxy** for Owncast and Icecast (1 exposed domain)
- 🌐 **Custom CORS config** for frontend integration
- ⚙️ **Centralized config** via `application.properties`
- 🔑 **Login portal** with LDAP/local authentication
- 🧑‍💼 **Admin dashboard** with user management and monitoring
- 🎮 **Owncast Scheduler**: automated video streaming from folder-based schedules
- 🎧 **Radio Scheduler (Liquidsoap)**: timed audio streaming via folder-based structure

---

### 🚀 Quick Install (server)

```bash
curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/install.sh | bash
```

Installs Icecast, Owncast, Liquidsoap, IceWatch, and configures Samba shares and systemd services.

---

### 🚤 Client Setup (Manjaro KDE)

```bash
bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/InstallApps.sh) --plasma
```

This installs and configures:

- Audio/video tools (Mixxx, OBS, QjackCtl, Kdenlive...)
- Plasma visual settings (optional)
- Mounts `~/radioemissions` automatically

Options:

- `--plasma` = install + Plasma config
- `--update` = update only

---

### 🎵 Radio Scheduler (Liquidsoap)

- Folder structure: `radioemissions/day/hour/`
- `radio.liq` triggers AutoDJ for a time slot
- `live` file interrupts AutoDJ for live shows
- Runs in background via `run.sh` (systemd)

---

### 🎥 Video Scheduler (Owncast + FFmpeg)

- Folders: `/srv/owncast-schedule/Day/Hour/video/`
- Add `play` to activate scheduled streaming
- Add `live` to interrupt and allow live OBS stream
- Script `video-scheduler.sh` runs in background

---

### 📒 Full Documentation

See `docs/` or [pedagogical guide](https://github.com/radio0but/IceWatch/wiki) *(in progress)*.


❤️ Made by Marc-André Legault 2025 — Supporting open-source pedagogy at Cégep Rosemont.

![Capture 1](https://imgur.com/eSz5pLe.png)
![Capture 2](https://imgur.com/oPFdxce.png)
![Capture 3](https://imgur.com/FUukYxH.png)
![Capture 4](https://imgur.com/IqIs9xE.png)
![Capture 5](https://imgur.com/RsfJI6r.png)
![Capture 6](https://imgur.com/HLyAXKg.png)
![Capture 7](https://imgur.com/Ej6zDcg.png)
![Capture 8](https://imgur.com/0tHdGwQ.png)
![Capture 9](https://imgur.com/c3NmD6G.png)
![Capture 10](https://imgur.com/1AOVGNQ.png)
![Capture 11](https://imgur.com/2guw9r9.png)
![Capture 12](https://imgur.com/VWgzcBd.png)
