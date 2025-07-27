// dashboard/schedule.js
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

    // Ouvrir le popup
    const win = window.open("", "_blank", "width=650,height=600");
    win.document.write(`
      <html>
        <head>
          <title>${jour} ${heure}h</title>
          <style>
            body {
              font-family: monospace;
              background: #111;
              color: #eee;
              padding: 1rem;
            }
            textarea {
              width: 100%;
              background: #222;
              color: #eee;
              border: 1px solid #444;
              padding: 0.5rem;
              margin-top: 0.5rem;
              font-family: monospace;
              resize: vertical;
            }
            button {
              margin-top: 1rem;
              padding: 0.5rem 1rem;
              background: #28a745;
              color: white;
              border: none;
              cursor: pointer;
            }
          </style>
        </head>
        <body>
          <h2>📅 ${jour} à ${heure}h</h2>
          <div><strong>📂 Audio :</strong><br>${audioTree}</div><br>
          <div><strong>📹 Vidéo :</strong><br>${videoTree}</div><hr>
          <h3>📝 Description</h3>
          <label>🎧 Radio :</label>
          <textarea id="audio-info" rows="4">${audioInfo}</textarea>
          <label>📺 Vidéo :</label>
          <textarea id="video-info" rows="4">${videoInfo}</textarea>
          <button onclick="window.saveEmission()">💾 Enregistrer</button>

          <script>
            window.saveEmission = async function () {
              const audioInfo = document.getElementById("audio-info").value;
              const videoInfo = document.getElementById("video-info").value;

              const payload = {
              day: "${jour}",
              hour: "${heure}",
              audio: audioInfo,
              video: videoInfo
            };


              try {
                const res = await fetch("/admin/emission", {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify(payload)
                });

                if (res.ok) {
                  alert("✅ Description enregistrée !");
                } else {
                  alert("❌ Erreur d'enregistrement");
                }
              } catch (err) {
                alert("❌ Erreur réseau");
              }
            };
          </script>
        </body>
      </html>
    `);

  } catch (err) {
    alert("Erreur lors du chargement du contenu : " + err.message);
  }
};
