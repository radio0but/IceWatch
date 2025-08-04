// ==============================
// ✅ dashboard/index.js complet
// ==============================
import { updateSchedulerStatus } from "./schedulers.js";
import { marked } from "https://cdn.jsdelivr.net/npm/marked/lib/marked.esm.js";
import { setupAppearanceNotesAutoSave } from "./notes.js";
import {
  loadProperties,
  saveProperties,
  restartIceWatch,
  restartVideoScheduler,
  restartRadioScheduler,
  loadLog,
  resetAppearance
} from "./properties.js";
import { loadSchedule } from "./schedule.js";
import { loadRSSConfig } from "./rss.js";
import { fetchUsers } from "./users.js";

function setupSidebarNav() {
  const buttons = document.querySelectorAll(".sidebar-nav button");
  const contents = document.querySelectorAll(".tab-content");

  buttons.forEach(btn => {
    btn.addEventListener("click", () => {
      const tabId = btn.dataset.tab;

      buttons.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");

      contents.forEach(c => c.classList.remove("active"));
      const activeContent = document.getElementById(`tab-${tabId}`);
      if (activeContent) activeContent.classList.add("active");

      // Modules associés
      if (tabId === "users") fetchUsers();
      if (tabId === "appearance") loadProperties();
      if (tabId === "slots") loadSchedule();
      if (tabId === "rss") loadRSSConfig();
    });
  });

  // Active le premier par défaut
  buttons[0]?.click();
}

function setupSidebarToggle() {
  document.getElementById("sidebar-toggle")?.addEventListener("click", () => {
    document.body.classList.toggle("sidebar-collapsed");
  });
}
async function updateStreamStatus() {
  try {
    const res = await fetch("/admin/schedule/current");
    const json = await res.json(); // attendu : { audio: "live", video: "auto" }

    const icons = {
      live: "🟡 En direct",
      auto: "✅ Auto",
      vide: "🔵 Vide"
    };

    const label =
      `📻 ${icons[json.audio] || "?"} &nbsp;&nbsp;📺 ${icons[json.video] || "?"}`;

    const el = document.getElementById("stream-status");
    if (el) el.innerHTML = label;

  } catch (err) {
    console.error("Statut du stream indisponible", err);
    document.getElementById("stream-status").textContent = "❌ Hors ligne";
  }
}

// Appel initial + toutes les 10 sec
updateStreamStatus();
setInterval(updateStreamStatus, 10000);

window.addEventListener("DOMContentLoaded", () => {
  setupSidebarNav();
  setupSidebarToggle();
  updateSchedulerStatus();
  setupAppearanceNotesAutoSave();
  loadProperties();

  // Actions boutons
  const withFeedback = async (btnId, spanId, action) => {
    const statusEl = document.getElementById(spanId);
    if (!statusEl) return;
    statusEl.innerText = "⏳";
    try {
      await action();
      statusEl.innerText = "✅";
    } catch (err) {
      console.error(err);
      statusEl.innerText = "❌";
    }
    setTimeout(() => (statusEl.innerText = ""), 1500);
  };

  document.getElementById("restart-button")?.addEventListener("click", () =>
    withFeedback("restart-button", "status-restart", restartIceWatch)
  );

  document.getElementById("restart-video-button")?.addEventListener("click", () =>
    withFeedback("restart-video-button", "status-restart-video", restartVideoScheduler)
  );

  document.getElementById("restart-radio-button")?.addEventListener("click", () =>
    withFeedback("restart-radio-button", "status-restart-radio", restartRadioScheduler)
  );

  document.getElementById("refresh-log-video")?.addEventListener("click", () =>
    withFeedback("refresh-log-video", "status-log-video", () => loadLog("video"))
  );

  document.getElementById("refresh-log-radio")?.addEventListener("click", () =>
    withFeedback("refresh-log-radio", "status-log-radio", () => loadLog("radio"))
  );

  document.getElementById("save-button")?.addEventListener("click", saveProperties);
  document.getElementById("reset-button")?.addEventListener("click", resetAppearance);

  // === Preview Portail ===
  const previewToggle = document.getElementById("preview-toggle");
  const previewPanel = document.getElementById("preview-panel");
  const previewIframe = document.getElementById("preview-iframe");
  const previewClose = document.getElementById("preview-close");
  const previewRefresh = document.getElementById("preview-refresh");

previewToggle?.addEventListener("click", () => {
  previewPanel.style.display = "flex";
  previewToggle.style.display = "none"; // 👈 cache le bouton
});

previewClose?.addEventListener("click", () => {
  previewPanel.style.display = "none";
  previewToggle.style.display = "block"; // 👈 le fait réapparaître
});

  previewRefresh?.addEventListener("click", () => {
    previewIframe.src = "/index";
  });

  // === Redimensionnement du preview panel
const resizeHandle = document.getElementById("preview-resize-handle");
let isResizing = false;

resizeHandle?.addEventListener("mousedown", (e) => {
  isResizing = true;
  document.body.style.cursor = "ew-resize";
  e.preventDefault();

  document.addEventListener("mousemove", resizePreview);
  document.addEventListener("mouseup", stopResizing);
  window.addEventListener("mouseup", stopResizing);

  // 🔄 Écoute aussi dans l’iframe
  const iframe = document.getElementById("preview-iframe");
  iframe?.contentWindow?.addEventListener("mouseup", stopResizing);
});

function resizePreview(e) {
  if (!isResizing) return;
  const screenWidth = window.innerWidth;
  const newWidth = screenWidth - e.clientX;
  if (newWidth > 280) {
    document.getElementById("preview-panel").style.width = `${newWidth}px`;
  }
}

function stopResizing() {
  if (!isResizing) return;
  isResizing = false;
  document.body.style.cursor = "";

  document.removeEventListener("mousemove", resizePreview);
  document.removeEventListener("mouseup", stopResizing);
  window.removeEventListener("mouseup", stopResizing);

  // 🔄 Nettoie aussi dans l’iframe
  const iframe = document.getElementById("preview-iframe");
  iframe?.contentWindow?.removeEventListener("mouseup", stopResizing);
}


function updateClock() {
  const now = new Date();
  const hh = now.getHours().toString().padStart(2, "0");
  const mm = now.getMinutes().toString().padStart(2, "0");
  const ss = now.getSeconds().toString().padStart(2, "0");
  document.getElementById("clock").textContent = `${hh}:${mm}:${ss}`;
}
setInterval(updateClock, 1000);
updateClock();

// === Barre de test des onglets facultatifs (prévisualisation du portail)
function updatePreviewTabButtonOnly(id, checked) {
  const iframe = document.getElementById("preview-iframe");
  if (!iframe?.contentWindow?.document) return;

  try {
    const doc = iframe.contentWindow.document;
    const button = doc.querySelector(`.tab-button[data-tab="${id}"]`);
    if (button) {
      button.style.display = checked ? "inline-block" : "none";
    }
  } catch (e) {
    console.warn("⚠️ Erreur accès DOM iframe :", e);
  }
}
document.getElementById("toggle-preview-rss")?.addEventListener("change", (e) =>
  updatePreviewTabButtonOnly("journal", e.target.checked)
);

document.getElementById("toggle-preview-logout")?.addEventListener("change", (e) =>
  updatePreviewTabButtonOnly("logout", e.target.checked)
);

// Réinitialise correctement l’état des boutons à chaque chargement d’iframe
document.getElementById("preview-iframe")?.addEventListener("load", () => {
  const showRss = document.getElementById("toggle-preview-rss")?.checked;
  const showLogout = document.getElementById("toggle-preview-logout")?.checked;
  updatePreviewTabButtonOnly("journal", showRss);
  updatePreviewTabButtonOnly("logout", showLogout);
});

function toggleElementInIframe(selector, visible) {
  const iframe = document.getElementById("preview-iframe");
  if (!iframe?.contentWindow?.document) return;

  try {
    const el = iframe.contentWindow.document.querySelector(selector);
    if (el) el.style.display = visible ? "" : "none";
  } catch (e) {
    console.warn("❌ Impossible de modifier l’élément dans l’iframe :", selector, e);
  }
}

document.getElementById("toggle-preview-description")?.addEventListener("change", (e) => {
  const show = e.target.checked;
  toggleElementInIframe("#audio-description", show);
  toggleElementInIframe("#video-description", show);
});

document.getElementById("toggle-preview-html")?.addEventListener("change", (e) => {
  const show = e.target.checked;
  toggleElementInIframe("#custom-html", show);
});

// Lors du chargement, applique les bons états
document.getElementById("preview-iframe")?.addEventListener("load", () => {
  const desc = document.getElementById("toggle-preview-description")?.checked;
  const html = document.getElementById("toggle-preview-html")?.checked;

  toggleElementInIframe("#audio-description", desc);
  toggleElementInIframe("#video-description", desc);
  toggleElementInIframe("#custom-html", html);
});
function setupMarkdownPreview(textareaId, previewId) {
  const textarea = document.getElementById(textareaId);
  const preview = document.getElementById(previewId);
  if (!textarea || !preview) return;

  const update = () => {
    const text = textarea.value.trim();
    preview.innerHTML = text ? marked.parse(text) : "<em>Aucun contenu…</em>";
  };

  textarea.addEventListener("input", update);
  update(); // appel initial
}

setupMarkdownPreview("audio-info", "preview-audio-info");
setupMarkdownPreview("video-info", "preview-video-info");
document.getElementById("append-to-notes")?.addEventListener("click", () => {
  const configText = document.getElementById("properties-editor").value.trim();
  const notesArea = document.getElementById("appearance-notes");

  if (!configText || !notesArea) return;

  const now = new Date();
  const timestamp = now.toLocaleString("fr-CA");
  const separator = `\n\n--- Copie de la configuration (${timestamp}) ---\n`;

  notesArea.value += `${separator}${configText}\n`;
  notesArea.dispatchEvent(new Event("input"));
  alert("Configuration ajoutée au calepin de notes !");
});

document.getElementById("export-properties")?.addEventListener("click", () => {
  const text = document.getElementById("properties-editor").value;
  const blob = new Blob([text], { type: "text/plain" });
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = url;
  a.download = `radio-appearance-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, "-")}.look`;
  a.click();

  URL.revokeObjectURL(url);
});

document.getElementById("import-properties")?.addEventListener("change", async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const text = await file.text();
  const editor = document.getElementById("properties-editor");
  if (editor) editor.value = text;

  alert("Configuration importée. N’oubliez pas de cliquer sur 📎 Enregistrer !");
});


document.getElementById("import-button")?.addEventListener("click", () => {
  document.getElementById("import-properties")?.click();
});


});
function refreshMarkdown(textareaId, previewId) {
  const textarea = document.getElementById(textareaId);
  const preview = document.getElementById(previewId);
  if (!textarea || !preview) return;

  const text = textarea.value.trim();
  preview.innerHTML = text ? marked.parse(text) : "<em>Aucun contenu…</em>";
}

window.refreshMarkdown = refreshMarkdown;