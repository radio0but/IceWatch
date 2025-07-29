// dashboard/properties.js
import { loadAppearanceNotes } from "./notes.js";

export async function loadProperties() {
  try {
    const res = await fetch("/admin/settings");
    const text = await res.text();
    document.getElementById("properties-editor").value = text;
  } catch (error) {
    console.error("Erreur de chargement de la configuration :", error);
  }
  loadAppearanceNotes();
}

export async function saveProperties() {
  const text = document.getElementById("properties-editor").value;
  try {
    await fetch("/admin/settings", {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: text
    });
    alert("Modifications enregistrées.");
  } catch (error) {
    alert("Erreur d’enregistrement : " + error.message);
  }
}

export async function restartIceWatch() {
  try {
    await fetch("/admin/settings/restart", { method: "POST" });
    alert("IceWatch redémarre...");
  } catch (error) {
    alert("Erreur lors du redémarrage : " + error.message);
  }
}
export async function loadLog(type) {
  const serviceName = type === "video" ? "video-scheduler" : "radio-scheduler";
  try {
    const res = await fetch(`/admin/settings/logs/${serviceName}`);
    const text = await res.text();
    const textarea = document.getElementById(`log-${type}`);
    if (textarea) {
      textarea.value = text;
    }
  } catch (err) {
    const textarea = document.getElementById(`log-${type}`);
    if (textarea) {
      textarea.value = `Erreur de chargement des logs ${type} : ${err.message}`;
    }
  }
}
export async function restartVideoScheduler() {
  try {
    await fetch("/admin/settings/restart-video-scheduler", { method: "POST" });
    alert("Vidéo Scheduler redémarré.");
  } catch (error) {
    alert("Erreur redémarrage vidéo scheduler : " + error.message);
  }
}

export async function restartRadioScheduler() {
  try {
    await fetch("/admin/settings/restart-radio-scheduler", { method: "POST" });
    alert("Radio Scheduler redémarré.");
  } catch (error) {
    alert("Erreur redémarrage radio scheduler : " + error.message);
  }
}

export async function resetAppearance() {
  try {
    await fetch("/admin/settings/reset", { method: "POST" });
    alert("Apparence réinitialisée.");
  } catch (error) {
    alert("Erreur lors de la réinitialisation : " + error.message);
  }
}
