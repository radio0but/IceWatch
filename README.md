# IceWatch



**IceWatch** is a secure Spring Boot backend designed to proxy and protect audio/video streams served by **Icecast** and **Owncast**. It enables dynamic token-based access control, referer validation, and seamless frontend integration.

---

## 🎯 Features

- 🔐 **Token-based access**: Dynamic tokens tied to referer headers.
- 🧾 **Master token support**: For trusted external systems like mobile apps or portals.
- 🛡️ **Reverse proxy**: Proxies all Owncast and Icecast resources through a single domain.
- 🌐 **CORS configuration**: Supports frontend applications securely.
- ⚙️ **Centralized config**: Easily configurable via `application.properties` or environment variables.

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

- ``: Issues short-lived tokens if the request comes from a valid referer.
- ``: Proxies the Icecast stream, protected by token.
- ``: Proxies Owncast resources and embeds, with protection on sensitive routes.
- ``: Fetches current song title from Icecast.

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

## IceWatch Front-end Integration Examples

Below are small code snippets showing how to embed both the audio (Icecast) and video (Owncast) streams into your front-end. Replace the placeholders (`API_BASE`, `your-backend-domain.com`, `your-frontend-domain.com`) with your actual domains.

### 1. HTML Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>IceWatch Demo</title>
  <style> /* ... */ </style>
</head>
<body>
  <div class="post">
    <h2>🎙 Live Audio</h2>
    <div id="player-container">Loading audio…</div>
    <div id="metadata">Loading metadata…</div>
  </div>
  <div class="post">
    <h2>📺 Live Video</h2>
    <div id="video-container">Loading video…</div>
  </div>
  <script src="icewatch-demo.js"></script>
</body>
</html>
```

### 2. JavaScript (`icewatch-demo.js`)

```js
const API_BASE = 'https://your-backend-domain.com';

async function fetchToken() {
  const res = await fetch(`${API_BASE}/auth/token`, {
    headers: { 'Origin': 'https://your-frontend-domain.com' }
  });
  const { token } = await res.json();
  return token;
}

async function setupAudio(token) { /* ... */ }
async function updateMetadata() { /* ... */ }
async function setupVideo(token) { /* ... */ }

window.addEventListener('DOMContentLoaded', async () => {
  try {
    const token = await fetchToken();
    await setupAudio(token);
    updateMetadata();
    setInterval(updateMetadata, 15000);
    await setupVideo(token);
  } catch (err) {
    console.error('Initialization error:', err);
  }
});
```

> **Note:** The `Origin` header (or `Referer`) must match the domain you’ve whitelisted in IceWatch (`allowed-domain`). Adjust `API_BASE` to point at your deployed service.

---

## 📦 IceWatch Stack Installation Guide

This script automatically installs and configures the entire **IceWatch** stack, including Icecast2, Liquidsoap, Owncast, and IceWatch itself.

```bash
curl -L https://github.com/radio0but/IceWatch/releases/download/v0.0.1/installer.sh \
  -o install.sh && chmod +x install.sh && ./install.sh
```

Answer the interactive prompts (admin credentials, referer domain, Owncast password, API port).\
⚠️ When prompted to configure Icecast via debconf, choose **No** — the script handles it automatically.

---

## 📅 Scheduler & Automated Programming

IceWatch provides a built-in scheduler for AutoDJ and live shows, driven by a simple directory structure and the `run.sh` script:

1. **Shared Directory**: Mounted on client stations at `~/radioemissions`.

2. **Structure**:

   ```
   ~/radioemissions/
   ├── run.sh                # Scheduler script (launched as a systemd service)
   ├── dimanche/ …/ samedi/  # Folders for Sunday through Saturday
   │   ├── 01/ …/ 24/        # Hourly subfolders (01 to 24)
   │   │   ├── Music/        # Audio files (.mp3, .ogg…)
   │   │   ├── jingles/      # Jingles and announcements
   │   │   ├── live         # Empty file to trigger live mode
   │   │   └── radio.liq    # Liquidsoap script for AutoDJ
   ```

3. **Behavior** (checked every 30s):

   - If a `live` file exists in the current hour folder, switch immediately to direct mode (live broadcast).
   - Else if `radio.liq` is present, (re)start the AutoDJ stream using that script.
   - Otherwise, continue the existing stream without interruption.

4. **Managing Slots**:

   - **Activate AutoDJ**: Double-click `play.sh` on a client to copy the template `radio.liq` and start AutoDJ.
   - **Deactivate**: Remove the `radio.liq` file.
   - **Start Live**: Create an empty `live` file.
   - **End Live**: Delete the `live` file (AutoDJ resumes if `radio.liq` exists).

💡 *Any changes to files or scripts are picked up automatically at the next check.*

---

## 💻 Client Workstation Configuration

Client stations (Manjaro KDE Plasma) can be set up or updated in one command. Ensure the user has sudo rights and is on the internal network.

- **Full install (apps + Plasma config)**:

  ```bash
   bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/InstallApps.sh) --plasma
  ```

- **Apps only**:

  ```bash
  bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/InstallApps.sh)
  ```

- **Update existing apps**:

  ```bash
  bash <(curl -fsSL https://github.com/radio0but/IceWatch/releases/download/v0.0.1/InstallApps.sh) --update
  ```

*The script performs system updates, installs recommended audio/video packages, Chrome, Flatpaks, AppImages, and (with **`--plasma`**) imports KDE settings (themes, shortcuts, desktop layout). You can rerun with **`--update`** anytime. After a **`--plasma`** import, restart the graphical session for all changes to take effect.*

---

❤️ Author

Made by Marc-André Legault 2025\
Inspired by the need for secure, pedagogical streaming in educational settings.

