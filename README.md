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

❤️ Réalisé par Marc-André Legault 2025 — Pour une pédagogie libre et open-source au Cégep Rosemont.


## 🇬🇧 English Notice – About IceWatch

**IceWatch** is currently a self-hosted web platform developed **entirely in French**, intended for local radio/video broadcasting in educational or community environments.

While the software was initially just a backend proxy for securing Icecast/Owncast streams, it has grown into a complete platform featuring a secured portal, user login (LDAP/local), an admin dashboard, a custom media player, and broadcast scheduling tools.

---

### 🔧 How to Use IceWatch in Your Language

Although the interface and documentation are in French, **you can still use IceWatch in your environment** by:

1. **Cloning the repository and building the project** manually:

   ```bash
   git clone https://github.com/radio0but/IceWatch.git
   cd IceWatch
   mvn clean package
   ```

   This will generate the file `./target/icewatch.jar`.

2. **Running the install script** to deploy dependencies:

   ```bash
   curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/install.sh | bash
   ```

   Then, replace the generated JAR with your custom build:

   ```bash
   sudo mv target/icewatch.jar /opt/icewatch/
   sudo systemctl restart icewatch
   ```

---

### 🌍 Want to Translate or Adapt IceWatch?

The app is hardcoded in French for now, but we welcome help to make it multilingual! If you want to contribute:

- Fork the project
- Create an English translation of the HTML/JS content in `static/`
- Suggest improvements to structure for language packs
- Share your version or open a pull request

We encourage community forks if you'd like to adapt the project for broader use!

In the meantime, if you want to translate IceWatch manually, here are the files where most user-facing text is defined:

- Frontend HTML/JS: located in `src/main/resources/static/`
- Server-rendered pages: see `StaticPageController.java`
- Default appearance text (titles, messages): can be edited directly through the admin dashboard

You can duplicate and translate these manually while we work on a future internationalization system.

> 📝 *Note: The current documentation and UI are only available in French for now.*



❤️ Made by Marc-André Legault 2025 — Supporting open-source pedagogy at Cégep Rosemont.

![Capture 1](https://imgur.com/eSz5pLe.png)
![Capture 2](https://imgur.com/oPFdxce.png)
![Capture 3](https://imgur.com/FUukYxH.png)
![Capture 4](https://imgur.com/IqIs9xE.png)
![Capture 5](https://imgur.com/RsfJI6r.png)
![Capture 7](https://imgur.com/Ej6zDcg.png)
![Capture 8](https://imgur.com/0tHdGwQ.png)
![Capture 9](https://imgur.com/c3NmD6G.png)
![Capture 10](https://imgur.com/1AOVGNQ.png)
![Capture 11](https://imgur.com/2guw9r9.png)
![Capture 12](https://imgur.com/VWgzcBd.png)
