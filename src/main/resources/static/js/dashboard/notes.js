// dashboard/notes.js
export function setupAppearanceNotesAutoSave() {
  const textarea = document.getElementById("appearance-notes");
  if (!textarea) return;

  let timeout;
  textarea.addEventListener("input", () => {
    clearTimeout(timeout);
    timeout = setTimeout(() => {
      fetch("/api/settings/appearance-notes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ notes: textarea.value })
      }).catch(err => {
        console.warn("Erreur enregistrement des notes :", err);
      });
    }, 800);
  });
}

export async function loadAppearanceNotes() {
  try {
    const res = await fetch("/api/settings/appearance-notes");
    const { notes } = await res.json();
    const textarea = document.getElementById("appearance-notes");
    if (textarea) textarea.value = notes || "";
  } catch (e) {
    console.warn("Erreur chargement des notes :", e);
  }
}