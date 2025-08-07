// dashboard/schedule.js
import { setupDualEditor } from "./tinymce.js";
import { setupTinyMCE } from "./tinymce.js";


function renderTree(tree, indent = 0) {
  if (!tree || typeof tree !== 'object') return "";

  const entries = Object.entries(tree);
  if (entries.length === 0) return '&nbsp;'.repeat(indent * 4) + "(vide)";

  return entries.map(([name, child]) => {
    const spacing = '&nbsp;'.repeat(indent * 4);
    if (child === null) {
      return `${spacing}📄 ${name}`;
    } else {
      return `${spacing}📁 ${name}<br>` + renderTree(child, indent + 1);
    }
  }).join("<br>");
}

export async function loadSchedule() {
  try {
    const res = await fetch("/admin/schedule");
    const data = await res.json();
    const grid = document.getElementById("schedule-grid");

    const jours = ["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"];
    const heures = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, "0"));

    grid.innerHTML = "<div class='schedule-header'></div>";
    for (const jour of jours) {
      grid.innerHTML += `<div class="schedule-header">${jour}</div>`;
    }

    for (const heure of heures) {
      grid.innerHTML += `<div class="schedule-row-label">${heure}h</div>`;
      for (const jour of jours) {
        const slot = data[jour]?.[heure] || { audio: "vide", video: "vide" };

        grid.innerHTML += `
  <div class="schedule-cell" onclick="showSlotContents('${jour}', '${heure}')">
    <div class="cell-radio ${getStatusClass(slot.audio)}">${slot.audio}</div>
    <div class="cell-video ${getStatusClass(slot.video)}">${slot.video}</div>
  </div>
`;
      }
    }

  } catch (err) {
    console.error("Erreur de chargement des créneaux :", err);
    document.getElementById("schedule-grid").innerHTML = "<p>Impossible de charger les créneaux.</p>";
  }
}

function getStatusClass(status) {
  if (status === "live") return "status-live";
  if (status === "auto") return "status-auto";
  return "status-empty";
}

window.showSlotContents = async function (jour, heure) {
  try {
    const res = await fetch(`/admin/schedule/contents?day=${encodeURIComponent(jour)}&hour=${encodeURIComponent(heure)}`);
    const data = await res.json();

    const audioTree = renderTree(data.audio);
    const videoTree = renderTree(data.video);

    // Charger les descriptions actuelles
    let audioInfo = "";
    let videoInfo = "";
    try {
      const infoRes = await fetch(`/admin/emission?day=${jour}&hour=${heure}`);
      if (infoRes.ok) {
        const json = await infoRes.json();
        audioInfo = json.audio || "";
        videoInfo = json.video || "";
      }
    } catch (e) {
      console.warn("Impossible de charger les descriptions", e);
    }

    // Affichage interne
    const detail = document.getElementById("slot-details");
    detail.innerHTML = `
      <div class="slot-toolbar">
        <button onclick="goToPreviousSlot()">⬅</button>
        <strong>📅 ${jour} à ${heure}h</strong>
        <button onclick="goToNextSlot()">➡</button>
        <button onclick="backToSchedule()" style="margin-left:auto;">🔙 Retour</button>
      </div>
              <div id="local-paths" style="margin-top: 2rem;">
          <h3>📁 Dossiers locaux (poste client uniquement)</h3>
          <p>
            🎧 <code id="audio-path">~/radioemissions/${jour}/${heure}</code>
            <button onclick="copyToClipboard('audio-path')">📋Copier</button>
          </p>
          <p>
            📺 <code id="video-path">~/owncastvideos/${getVideoFolder(jour)}/${heure}</code>
            <button onclick="copyToClipboard('video-path')">📋Copier</button>
          </p>
          <p style="color: #ff9800;" id="folder-warning">
            ⚠️ Fonctionne uniquement sur les ordinateurs clients du studio.
          </p>
        </div>
      <div class="slot-content">
        <h3>📝 Description</h3>

          <label>🎧 Radio :</label>
          <textarea id="audio-info" rows="4">${audioInfo}</textarea>
          <button type="button" onclick="refreshMarkdown('audio-info', 'preview-audio-info')">🔄 Actualiser aperçu</button>
          <div id="preview-audio-info" class="markdown-preview"><em>Aucun contenu…</em></div>

          <label>📺 Vidéo :</label>
          <textarea id="video-info">${videoInfo}</textarea>
          <button type="button" onclick="refreshMarkdown('video-info', 'preview-video-info')">🔄 Actualiser aperçu</button>
          <div id="preview-video-info" class="markdown-preview"><em>Aucun contenu…</em></div>

        <button onclick="saveCurrentEmission()">💾 Enregistrer</button>

        <div style="margin-top: 2rem;">
          <strong>📂 Audio :</strong><br>${audioTree}<br><br>
          <strong>📹 Vidéo :</strong><br>${videoTree}
        </div>
      </div>
    `;

    detail.dataset.jour = jour;
    detail.dataset.heure = heure;

    document.getElementById("schedule-grid").style.display = "none";
    detail.style.display = "block";
    setupTinyMCE('#audio-info');
    setupTinyMCE('#video-info');
  } catch (err) {
    alert("Erreur lors du chargement du contenu : " + err.message);
  }
};


// Naviguer
window.backToSchedule = () => {
  if (tinymce.get("audio-info")) tinymce.get("audio-info").remove();
  if (tinymce.get("video-info")) tinymce.get("video-info").remove();
  document.getElementById("slot-details").style.display = "none";
  document.getElementById("schedule-grid").style.display = "grid";
};

window.goToNextSlot = () => {
  navigateSlot(1);
};

window.goToPreviousSlot = () => {
  navigateSlot(-1);
};

function navigateSlot(direction) {
  const detail = document.getElementById("slot-details");
  const jours = ["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"];
  const jour = detail.dataset.jour;
  const heure = parseInt(detail.dataset.heure);

  let jourIndex = jours.indexOf(jour);
  let newHeure = heure + direction;
  if (newHeure < 0) {
    newHeure = 23;
    jourIndex = (jourIndex - 1 + 7) % 7;
  } else if (newHeure > 23) {
    newHeure = 0;
    jourIndex = (jourIndex + 1) % 7;
  }

  const newJour = jours[jourIndex];
  const newHeureStr = String(newHeure).padStart(2, "0");
  showSlotContents(newJour, newHeureStr);
}

// Sauvegarde
window.saveCurrentEmission = async function () {
  const detail = document.getElementById("slot-details");
  const jour = detail.dataset.jour;
  const heure = detail.dataset.heure;
  if (tinymce.get("audio-info")) tinymce.get("audio-info").save();
  if (tinymce.get("video-info")) tinymce.get("video-info").save();
  const audioInfo = document.getElementById("audio-info").value;
  const videoInfo = document.getElementById("video-info").value;
  

  const jours = ["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"];

  const popup = window.open("", "_blank", "width=500,height=400");
popup.document.write(`
  <html>
  <head>
    <title>Plage de propagation</title>
    <style>
  body {
  font-family: 'Segoe UI', sans-serif;
  background: #121212;
  color: #f1f1f1;
  padding: 1.5rem;
}

h2 {
  margin-top: 0;
  font-size: 1.4rem;
  font-weight: bold;
  color: #f44336;
}

label {
  display: block;
  margin-top: 1rem;
  font-weight: 500;
}

select {
  margin: 0.3rem 0.5rem;
  padding: 0.4rem;
  background: #1e1e1e;
  color: #eee;
  border: 1px solid #444;
  border-radius: 4px;
  font-size: 1rem;
}

button {
  margin-top: 1.5rem;
  padding: 0.6rem 1.2rem;
  font-size: 1rem;
  font-weight: bold;
  color: white;
  background: #2196f3;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

button:hover {
  background: #1976d2;
}

</style>

  </head>
  <body>
    <h2>📌 Propager les descriptions</h2>

    <label>🎧 Jusqu’à quand pour l’audio ?</label>
    <select id="end-audio-day">${jours.map(j => `<option value="${j}">${capitalize(j)}</option>`)}</select>
    <select id="end-audio-hour">${Array.from({length:24}, (_,i) => `<option value="${String(i).padStart(2,"0")}">${String(i).padStart(2,"0")}h</option>`).join("")}</select>

    <label>📺 Jusqu’à quand pour la vidéo ?</label>
    <select id="end-video-day">${jours.map(j => `<option value="${j}">${capitalize(j)}</option>`)}</select>
    <select id="end-video-hour">${Array.from({length:24}, (_,i) => `<option value="${String(i).padStart(2,"0")}">${String(i).padStart(2,"0")}h</option>`).join("")}</select>

    <button onclick="window.sendSave()">💾 Enregistrer</button>

    <script>
      document.getElementById("end-audio-day").value = "${jour}";
      document.getElementById("end-audio-hour").value = "${heure}";
      document.getElementById("end-video-day").value = "${jour}";
      document.getElementById("end-video-hour").value = "${heure}";

      window.sendSave = async function () {
        const payload = {
          startDay: "${jour}",
          startHour: "${heure}",
          endAudioDay: document.getElementById("end-audio-day").value,
          endAudioHour: document.getElementById("end-audio-hour").value,
          endVideoDay: document.getElementById("end-video-day").value,
          endVideoHour: document.getElementById("end-video-hour").value,
          audio: ${JSON.stringify(audioInfo)},
          video: ${JSON.stringify(videoInfo)}
        };

        const res = await fetch("/admin/schedule/batch", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        });

        if (res.ok) {
          alert("✅ Description propagée !");
          window.close();
        } else {
          alert("❌ Erreur lors de l’enregistrement");
        }
      };
    </script>
  </body>
  </html>
`);

};

function getVideoFolder(jour) {
  const jours = ["dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"];
  const index = jours.indexOf(jour);
  return index === -1 ? jour : `${index + 1}${capitalize(jour)}`;
}

function capitalize(str) {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

function copyToClipboard(elementId) {
  const el = document.getElementById(elementId);
  const text = el.textContent;

  navigator.clipboard.writeText(text).then(() => {
    el.classList.add("copied");
    const warning = document.getElementById("folder-warning");
    warning.textContent = "✅ Chemin copié ! Collez-le dans Dolphin.";

    setTimeout(() => {
      el.classList.remove("copied");
      warning.textContent = "⚠️ Fonctionne uniquement sur les ordinateurs clients du studio.";
    }, 2000);
  });
}
window.copyToClipboard = copyToClipboard;

