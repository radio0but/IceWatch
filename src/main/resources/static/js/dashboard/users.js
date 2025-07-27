// dashboard/users.js
export async function fetchUsers() {
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
            <button class="danger" onclick="promptResetPassword('${user.username}')">🔐 Réinitialiser</button>
        </td>
        `;

      tbody.appendChild(row);
    });
  } catch (error) {
    console.error("Erreur de chargement des utilisateurs :", error);
  }
}

window.confirmDeleteUser = function(username, isAdmin, remainingAdmins) {
  if (isAdmin && remainingAdmins <= 1) {
    alert("Impossible de supprimer le dernier administrateur.");
    return;
  }

  if (!confirm(`Voulez-vous vraiment supprimer l'utilisateur \"${username}\" ?`)) return;

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
};


window.promptResetPassword = function(username) {
  const win = window.open("", "_blank", "width=400,height=380");
  win.document.write(`
    <html>
      <head>
        <title>🔐 Réinitialiser mot de passe</title>
        <style>
          body { font-family: sans-serif; background:#222; color:#eee; padding:1rem; }
          label { display:block; margin-top:1rem; }
          input { width:100%; padding:0.5rem; margin-top:0.3rem; border-radius:5px; border:none; }
          button { margin-top:1.5rem; padding:0.6rem 1rem; background:#4caf50; color:white; border:none; border-radius:5px; cursor:pointer; }
          button:hover { background:#43a047; }
        </style>
      </head>
      <body>
        <h2>🔐 Réinitialiser le mot de passe</h2>
        <p>Utilisateur : <strong>${username}</strong></p>
        <form id="reset-form">
          <label>Nouveau mot de passe :</label>
          <input type="password" id="new-password" required />

          <label>Confirmer le mot de passe :</label>
          <input type="password" id="confirm-password" required />

          <label>Mot de passe de l’administrateur :</label>
          <input type="password" id="admin-password" required />

          <button type="submit">✅ Valider</button>
        </form>
        <p id="status" style="margin-top:1rem; color:#f44336;"></p>

        <script>
          document.getElementById("reset-form").addEventListener("submit", async (e) => {
            e.preventDefault();

            const newPassword = document.getElementById("new-password").value;
            const confirmPassword = document.getElementById("confirm-password").value;
            const adminPassword = document.getElementById("admin-password").value;
            const status = document.getElementById("status");

            if (newPassword !== confirmPassword) {
              status.textContent = "❌ Les mots de passe ne correspondent pas.";
              return;
            }

            try {
              const res = await fetch("/admin/update-password", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({
                  username: "${username}",
                  newPassword,
                  adminPassword
                })
              });

              if (res.ok) {
                status.style.color = "lime";
                status.textContent = "✅ Mot de passe mis à jour !";
              } else if (res.status === 403) {
                status.textContent = "❌ Mot de passe administrateur incorrect.";
              } else if (res.status === 404) {
                status.textContent = "Utilisateur introuvable.";
              } else {
                status.textContent = "Erreur : " + res.status;
              }
            } catch (err) {
              status.textContent = "Erreur réseau : " + err.message;
            }
          });
        </script>
      </body>
    </html>
  `);
};
