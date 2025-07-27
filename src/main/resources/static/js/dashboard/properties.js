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
