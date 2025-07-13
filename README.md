# IceWatch

**IceWatch** is a secure Spring Boot backend designed to proxy and protect audio/video streams served by **Icecast** and **Owncast**. It enables dynamic token-based access control, referer validation, and seamless frontend integration through a secured portal.

---

## 🎯 Features

- 🔐 **Token-based access**: Dynamic tokens tied to referer headers.
- 🧾 **Master token support**: For trusted external systems like mobile apps or portals.
- 🛡️ **Reverse proxy**: Proxies all Owncast and Icecast resources through a single domain.
- 🌐 **CORS configuration**: Supports frontend applications securely.
- ⚙️ **Centralized config**: Easily configurable via `application.properties` or environment variables.
- 🔑 **Login Portal**: A simple authentication system supports local accounts or LDAP integration.
- 🏢 **Admin Dashboard**: Reserved area for user management, token inspection, and stream status.
- 🎬 **Owncast Video Scheduler**: Automatically streams pre-scheduled video folders using ffmpeg, with support for switching to live mode.

---

## 🛠 Configuration

In `src/main/resources/application.properties`, you can set:

```
server.port=9090
icewatch.master-token=MASTER_SECRET_TOKEN
icewatch.allowed-domain=https://your-frontend-domain.com
icewatch.owncast-url=http://localhost:8123
icewatch.icecast-stream-url=http://localhost:8000/radio
```

⚠️ Be sure to change `MASTER_SECRET_TOKEN` before deploying in production!

---

## 🚀 How It Works

- `/auth/token`: Issues short-lived tokens if the request comes from a valid referer.
- `/radio`: Proxies the Icecast stream, protected by token.
- `/owncast/**`: Proxies Owncast resources and embeds, with protection on sensitive routes.
- `/radio/metadata`: Fetches current song title from Icecast.
- `/login`: Login page with username/password form (supports LDAP if enabled).
- `/dashboard`: Admin area for managing users and checking system status.

---

## 🧪 Development

```bash
# Build and run
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## 📄 License

MIT — free to use, modify, and contribute. Feel free to submit pull requests or open issues!

---



---

## 📦 IceWatch Stack Installation

```bash
curl -L https://github.com/radio0but/IceWatch/releases/download/v0.0.1/installer.sh \
  -o install.sh && chmod +x install.sh && ./install.sh
```

### ❓ Installer Prompts

The installer will guide you through the following interactive setup:

- Icecast admin username and password (source + relay)
- Domain name authorized as Referer (e.g., [https://radio.boogiepit.com](https://radio.boogiepit.com))
- Owncast admin password
- Port for IceWatch API (default: 9090)
- Passwords for local users `admin` and `enseignant`
- LDAP activation prompt (optional)
  - If enabled: LDAP URL, base DN, bind DN and password will be requested
- Samba share for `/srv/radioemissions`
- Download of `run.sh` scheduler script for Liquidsoap
- Download of Liquidsoap template (`radio.liq.template`) and setup of `play.sh`
- Automatic creation of day/hour folders
- Final step: install and configure the **Owncast video scheduler** (with ffmpeg test and stream key verification)

🚨 When prompted about configuring Icecast via debconf, choose **No** — the script configures it for you automatically.

---

## 📅 Scheduler & AutoDJ (Audio)

- Playlist folders by day/hour (e.g., `7Samedi/14/`)
- Add a `radio.liq` file to enable AutoDJ
- Add a `live` file to switch to live
- Automatically switches back if `live` is deleted

---

## 🎬 Video Scheduler

IceWatch includes a headless video scheduler for **Owncast**, allowing pre-programmed video playback via RTMP.

### How It Works

- Videos are organized by day and hour folders (e.g., `/srv/owncast-schedule/7Samedi/14/`)
- If a `play` file is found in the current hour's folder, it triggers automatic streaming via ffmpeg
- If a `live` file exists, it immediately stops the schedule and leaves control to the live broadcaster
- Automatically resumes when the `live` file is removed

### Example Folder

```
/srv/owncast-schedule/
├── 7Samedi/
│   └── 14/
│       ├── play
│       ├── video1.mp4
│       └── video2.mp4
```

---

## 💻 Client Setup (Manjaro KDE)

```bash
bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/client.sh) --plasma
```

Alternate modes: `--update`, `--plasma`, or run without options for full install.

---

❤️ Made by Marc-André Legault 2025 — Supporting open-source pedagogy at Cégep Rosemont.

