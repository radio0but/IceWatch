// === Point d'entrée ===
window.addEventListener("DOMContentLoaded", () => {
  setupTabs();         // active les onglets
  updateSchedulerStatus();  // affiche les statuts ✅/🔴 des services au chargement
});


// === Gestion des onglets ===
function setupTabs() {
  const tabButtons = document.querySelectorAll(".tab-button");
  const tabContents = document.querySelectorAll(".tab-content");

  tabButtons.forEach(button => {
    button.addEventListener("click", () => {
      const tabId = button.dataset.tab;

      // Activer onglet
      tabButtons.forEach(b => b.classList.remove("active"));
      tabContents.forEach(c => c.classList.remove("active"));
      button.classList.add("active");
      document.getElementById(`tab-${tabId}`).classList.add("active");

      // Charger dynamiquement le contenu de chaque onglet
      if (tabId === "users") fetchUsers();
      if (tabId === "settings") loadProperties();
      if (tabId === "slots") loadSchedule();  // ← le nouveau onglet des créneaux
    });
  });

  // Activer le premier onglet
  if (tabButtons.length > 0) tabButtons[0].click();
}


// === Utilisateurs ===
async function fetchUsers() {
  try {
    const response = await fetch("/admin/list");
    const users = await response.json();
    const tbody = document.getElementById("user-list");
    tbody.innerHTML = "";

    const remainingAdmins = users.filter(u => u.roles.includes("ADMIN")).length;

    users.forEach(user => {
      const isAdmin = user.roles.includes("ADMIN");
      const row = document.createElement("tr");

      row.innerHTML = `
        <td>${user.username}</td>
        <td>${user.roles}</td>
        <td>
          <button class="danger" onclick="confirmDeleteUser('${user.username}', ${isAdmin}, ${remainingAdmins})">🗑 Supprimer</button>
        </td>
      `;

      tbody.appendChild(row);
    });
  } catch (error) {
    console.error("Erreur de chargement des utilisateurs :", error);
  }
}
function confirmDeleteUser(username, isAdmin, remainingAdmins) {
  if (isAdmin && remainingAdmins <= 1) {
    alert("Impossible de supprimer le dernier administrateur.");
    return;
  }

  if (!confirm(`Voulez-vous vraiment supprimer l'utilisateur "${username}" ?`)) return;

  fetch(`/admin/delete/${encodeURIComponent(username)}`, {
    method: 'DELETE'
  })
    .then(res => {
      if (res.ok) {
        alert("Utilisateur supprimé.");
        fetchUsers();
      } else {
        alert("Erreur lors de la suppression.");
      }
    });
}


// === Paramètres (application.properties) ===
async function loadProperties() {
  try {
    const res = await fetch("/admin/settings");
    const text = await res.text();
    document.getElementById("properties-editor").value = text;
  } catch (error) {
    console.error("Erreur de chargement de la configuration :", error);
  }
}

async function saveProperties() {
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

async function restartIceWatch() {
  try {
    await fetch("/admin/settings/restart", { method: "POST" });
    alert("IceWatch redémarre...");
  } catch (error) {
    alert("Erreur lors du redémarrage : " + error.message);
  }
}
// === Créneaux ===
async function loadSchedule() {
  try {
    const res = await fetch("/admin/schedule");
    const data = await res.json();
    const grid = document.getElementById("schedule-grid");

    const jours = ["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"];
    const heures = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, "0"));

    // En-têtes
    grid.innerHTML = "<div class='schedule-header'></div>";
    for (const jour of jours) {
      grid.innerHTML += `<div class="schedule-header">${jour}</div>`;
    }

    // Lignes heure par heure
    for (const heure of heures) {
      grid.innerHTML += `<div class="schedule-row-label">${heure}h</div>`;
      for (const jour of jours) {
        const slot = data[jour]?.[heure] || { audio: "vide", video: "vide" };
        let cssClass = "status-empty";
        if (slot.audio === "live" || slot.video === "live") cssClass = "status-live";
        else if (slot.audio === "auto" || slot.video === "auto") cssClass = "status-automation";

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
async function resetAppearance() {
  if (!confirm("Confirmer la réinitialisation de l’apparence ?")) return;
  try {
    await fetch("/admin/settings/reset", { method: "POST" });
    alert("Apparence réinitialisée.");
    loadProperties(); // recharge la zone de texte
  } catch (err) {
    alert("Erreur de réinitialisation : " + err.message);
  }
}

async function restartVideoScheduler() {
  try {
    await fetch("/admin/settings/restart-video-scheduler", { method: "POST" });
    alert("🎬 Vidéo Scheduler redémarré !");
  } catch (err) {
    alert("Erreur redémarrage vidéo-scheduler : " + err.message);
  }
}

async function restartRadioScheduler() {
  try {
    await fetch("/admin/settings/restart-radio-scheduler", { method: "POST" });
    alert("📻 Radio Scheduler redémarré !");
  } catch (err) {
    alert("Erreur redémarrage radio-scheduler : " + err.message);
  }
}

async function updateSchedulerStatus() {
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

// Recharger le statut après chaque redémarrage :
async function restartVideoScheduler() {
  try {
    await fetch("/admin/settings/restart-video-scheduler", { method: "POST" });
    alert("🎬 Vidéo Scheduler redémarré !");
    updateSchedulerStatus();
  } catch (err) {
    alert("Erreur redémarrage vidéo-scheduler : " + err.message);
  }
}

async function restartRadioScheduler() {
  try {
    await fetch("/admin/settings/restart-radio-scheduler", { method: "POST" });
    alert("📻 Radio Scheduler redémarré !");
    updateSchedulerStatus();
  } catch (err) {
    alert("Erreur redémarrage radio-scheduler : " + err.message);
  }
}


async function loadLog(type) {
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


async function showSlotContents(jour, heure) {
  try {
    const res = await fetch(`/admin/schedule/contents?day=${encodeURIComponent(jour)}&hour=${encodeURIComponent(heure)}`);
    const data = await res.json();

    const audioTree = renderTree(data.audio);
    const videoTree = renderTree(data.video);

    const contenu = `
      <strong>📂 Audio :</strong><br>${audioTree}<br><br>
      <strong>📹 Vidéo :</strong><br>${videoTree}
    `;

    // 👉 Ouverture dans une popup HTML propre
    const win = window.open("", "_blank", "width=600,height=500");
    win.document.write(`
      <html>
        <head><title>${jour} ${heure}h</title></head>
        <body style="font-family:monospace; background:#111; color:#eee; padding:1rem;">
          <h2>📅 ${jour} à ${heure}h</h2>
          ${contenu}
        </body>
      </html>
    `);

  } catch (err) {
    alert("Erreur lors du chargement du contenu : " + err.message);
  }
}
