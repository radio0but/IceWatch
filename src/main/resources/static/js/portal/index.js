import { fetchApiBase, fetchToken, setupVideo } from "./player.js";
import { updateMetadata, startAutoScrollAfterDelay } from "./metadata.js";
import { setupTabs } from "./tabs.js";
import { loadEmissionDescriptions } from "./emission.js";
import { initParallaxAndStickyHeader } from "./parallaxHeader.js"
import { initJournalTab } from "./journal.js";

window.addEventListener("DOMContentLoaded", async () => {
  try {
    await fetchApiBase();
    initJournalTab();
    // === AUDIO ===
    const { initCustomAudioPlayer } = await import("./radioPlayer.js");
    await initCustomAudioPlayer();

    // === VIDEO ===
    const token = await fetchToken(); // important pour la vidéo
    await setupVideo(token);
      document.querySelectorAll("iframe").forEach(iframe => {
        if (iframe.src.includes("${token}")) {
          iframe.src = iframe.src.replace("${token}", encodeURIComponent(token));
        }
      });
    // === Autres modules ===
    updateMetadata();
    startAutoScrollAfterDelay();
    await loadEmissionDescriptions();
  } catch (err) {
    console.error("Erreur d'initialisation :", err);
    document.getElementById("player-container").innerText = "❌ Erreur de lecture audio.";
    document.getElementById("video-container").innerText = "❌ Erreur de lecture vidéo.";
  }
  initParallaxAndStickyHeader();
  setInterval(updateMetadata, 15000);
  setupTabs();
  showAdminLinkIfNeeded();
});

export async function showAdminLinkIfNeeded() {
  // Ne rien faire si la page est dans une iframe (ex: aperçu du dashboard)
  if (window !== window.parent) return;

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
