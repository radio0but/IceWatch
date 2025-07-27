// dashboard/schedulers.js
export async function restartVideoScheduler() {
  try {
    await fetch("/admin/settings/restart-video-scheduler", { method: "POST" });
    alert("🎬 Vidéo Scheduler redémarré !");
    updateSchedulerStatus();
  } catch (err) {
    alert("Erreur redémarrage vidéo-scheduler : " + err.message);
  }
}

export async function restartRadioScheduler() {
  try {
    await fetch("/admin/settings/restart-radio-scheduler", { method: "POST" });
    alert("📻 Radio Scheduler redémarré !");
    updateSchedulerStatus();
  } catch (err) {
    alert("Erreur redémarrage radio-scheduler : " + err.message);
  }
}

export async function updateSchedulerStatus() {
  try {
    const res = await fetch("/admin/settings/scheduler-status");
    const status = await res.json();

    updateStatusLabel("status-video", status.video);
    updateStatusLabel("status-radio", status.radio);
  } catch (err) {
    console.error("Erreur statut scheduler :", err);
  }
}

function updateStatusLabel(elementId, state) {
  const el = document.getElementById(elementId);
  if (!el) return;

  el.textContent = state === "active" ? "✅ Actif" :
                   state === "inactive" ? "🔴 Inactif" :
                   "❓ Inconnu";

  el.className = "status-label status-" + (["active", "inactive"].includes(state) ? state : "unknown");
}

export async function loadLog(type) {
  const url = type === "video"
    ? "/admin/settings/logs/video-scheduler"
    : "/admin/settings/logs/radio-scheduler";

  const textareaId = type === "video" ? "log-video" : "log-radio";

  try {
    const res = await fetch(url);
    const text = await res.text();
    document.getElementById(textareaId).value = text;
  } catch (err) {
    document.getElementById(textareaId).value = "Erreur lors du chargement des logs.";
  }
}