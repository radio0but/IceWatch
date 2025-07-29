// ==============================
// ✅ dashboard/index.js complet
// ==============================
import { setupTabs } from "./tabs.js";
import { updateSchedulerStatus } from "./schedulers.js";
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

window.addEventListener("DOMContentLoaded", () => {
  setupTabs();
  updateSchedulerStatus();
  setupAppearanceNotesAutoSave();
  loadProperties();

  // Boutons de redémarrage avec feedback
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
});
