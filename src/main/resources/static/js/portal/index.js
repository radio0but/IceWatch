import { fetchApiBase, fetchToken } from "./player.js";
import { setupAudio, setupVideo } from "./player.js";
import { updateMetadata, startAutoScrollAfterDelay } from "./metadata.js";
import { initJournalRSS } from "./rss.js";
import { setupTabs } from "./tabs.js";
import { loadEmissionDescriptions } from "./emission.js";

window.addEventListener("DOMContentLoaded", async () => {
  try {
    await fetchApiBase();
    const token = await fetchToken();
    await setupAudio(token);
    await setupVideo(token);
    updateMetadata();
    startAutoScrollAfterDelay();
    await initJournalRSS();
    await loadEmissionDescriptions(); // <== ici
  } catch (err) {
    console.error("Erreur d'initialisation :", err);
    document.getElementById("player-container").innerText = "Erreur de lecture audio.";
    document.getElementById("video-container").innerText = "Erreur de lecture vidéo.";
  }

  setInterval(updateMetadata, 15000);
  setupTabs();
  showAdminLinkIfNeeded();
});
export async function showAdminLinkIfNeeded() {
  try {
    const res = await fetch("/auth/me");
    const user = await res.json();

    if (user.role === "ADMIN") {
      document.getElementById("admin-link").style.display = "block";
    }
  } catch (err) {
    console.warn("Impossible de vérifier le rôle de l'utilisateur.");
  }
}

