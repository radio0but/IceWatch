// === Point d'entrée ===
window.addEventListener("DOMContentLoaded", () => {
  setupTabs(); // active le premier onglet
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
  <div class="schedule-cell">
    <div class="cell-radio ${getStatusClass(slot.audio)}">${slot.audio}</div>
    <div class="cell-video ${getStatusClass(slot.video)}">${slot.video}</div>
  </div>`;

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