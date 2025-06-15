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

```properties
server.port=9090

icewatch.master-token=MASTER_SECRET_TOKEN
icewatch.allowed-domain=https://your-frontend-domain.com
icewatch.owncast-url=http://localhost:8123
icewatch.icecast-stream-url=http://localhost:8000/radio
⚠️ Be sure to change MASTER_SECRET_TOKEN before deploying in production!

🚀 How It Works
/auth/token: Issues short-lived tokens if the request comes from a valid referer.

/radio: Proxies the Icecast stream, protected by token.

/owncast/**: Proxies Owncast resources and embeds, with protection on sensitive routes.

/radio/metadata: Fetches current song title from Icecast.

🧪 Development
bash
Copier
# Build and run
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
📄 License
MIT — free to use, modify, and contribute.
Feel free to submit pull requests or open issues!

❤️ Author
Made by Marc-André Legault
Project inspired by the need for secure streaming in educational settings.
