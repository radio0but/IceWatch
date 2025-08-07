import { setupTinyMCE } from "./tinymce.js";

let pages = [];
let editingId = null;

export async function initCustomPages() {
  await loadPages();
  setupTinyMCE(); // Init vide pour la création

  document
    .getElementById("custom-page-form")
    .addEventListener("submit", async (e) => {
      e.preventDefault();
      await savePage();
    });

  document
    .getElementById("page-title")
    .addEventListener("input", () => autoFillSlug());
}

async function loadPages() {
  try {
    const res = await fetch("/admin/pages");
    pages = await res.json();
    renderPagesTable();
  } catch (err) {
    console.error("Erreur de chargement des pages :", err);
  }
}
function wrapHtmlContent(rawHtml) {
  // Si l'utilisateur a déjà mis un wrapper, on n'ajoute rien
  if (rawHtml.includes('class="page-wrapper"')) return rawHtml;

  return `
    <div class="page-wrapper">
      ${rawHtml}
    </div>
  `;
}

function renderPagesTable() {
  const tbody = document.getElementById("pages-table");
  tbody.innerHTML = "";

  for (const page of pages) {
    const tr = document.createElement("tr");

    tr.innerHTML = `
      <td>${page.title}</td>
      <td><code>/pages/${page.slug}</code></td>
      <td style="text-align:center;">${page.enabled ? "✅" : "❌"}</td>
      <td>
        <button onclick="editPage(${page.id})">✏️</button>
        <button onclick="deletePage(${page.id})">🗑️</button>
        <button onclick="togglePage(${page.id})">🔄</button>
      </td>
    `;

    tbody.appendChild(tr);
  }
}

async function savePage() {
  const editor = tinymce.get("page-html");
  if (editor) editor.save(); // 🔁 Met à jour le <textarea> à partir du contenu TinyMCE

  const payload = {
    title: document.getElementById("page-title").value.trim(),
    slug: document.getElementById("page-slug").value.trim(),
    htmlContent: document.getElementById("page-html").value.trim(),
    enabled: document.getElementById("page-enabled").checked,
  };

  const method = editingId ? "PUT" : "POST";
  const url = editingId ? `/admin/pages/${editingId}` : `/admin/pages`;

  try {
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!res.ok) throw new Error("Erreur HTTP");

    resetPageForm();
    await loadPages();
  } catch (err) {
    alert("Erreur lors de l’enregistrement de la page.");
    console.error(err);
  }
}

window.editPage = function (id) {
  const page = pages.find((p) => p.id === id);
  if (!page) return;

  editingId = page.id;
  document.getElementById("page-id").value = page.id;
  document.getElementById("page-title").value = page.title;
  document.getElementById("page-slug").value = page.slug;
  document.getElementById("page-enabled").checked = page.enabled;

  const textarea = document.getElementById("page-html");

  // 1. Supprime l’éditeur existant
  const existingEditor = tinymce.get("page-html");
  if (existingEditor) existingEditor.remove();

  // 2. Injecte le contenu HTML brut dans le textarea (pendant qu’il est visible)
  textarea.style.display = ""; // le rendre visible si jamais il est caché
  textarea.value = page.htmlContent || "";

  // 3. Réinitialise TinyMCE après un micro délai (pour que DOM se stabilise)
  setTimeout(() => {
    setupTinyMCE();
  }, 100);
};



window.deletePage = async function (id) {
  if (!confirm("Supprimer cette page ?")) return;
  try {
    await fetch(`/admin/pages/${id}`, { method: "DELETE" });
    await loadPages();
  } catch (err) {
    alert("Erreur lors de la suppression.");
    console.error(err);
  }
};

window.togglePage = async function (id) {
  try {
    await fetch(`/admin/pages/${id}/toggle`, { method: "PATCH" });
    await loadPages();
  } catch (err) {
    alert("Erreur lors du basculement.");
    console.error(err);
  }
};

window.resetPageForm = function () {
  editingId = null;
  document.getElementById("custom-page-form").reset();
  document.getElementById("page-id").value = "";

  const existing = tinymce.get("page-html");
  if (existing) existing.remove();

  document.getElementById("page-html").value = `
<div class="page-wrapper" style="background: rgba(0, 0, 0, 0.7); padding: 1.5rem; max-width: 720px; margin: 2rem auto; border-radius: 8px; font-family: Inconsolata; color: inherit;">
  <h2>Titre de la page</h2>
  <p>Ceci est un paragraphe d’exemple. Vous pouvez modifier ce contenu librement dans l’éditeur.</p>
  <p>Appuyez sur <strong>Entrée</strong> pour ajouter un nouveau paragraphe, ou insérez des titres, listes, images, etc.</p>
</div>
`.trim();

  setupTinyMCE();
};



function autoFillSlug() {
  const title = document.getElementById("page-title").value;
  const slugField = document.getElementById("page-slug");
  if (!slugField.value || slugField.value.length < 1) {
    slugField.value = title
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/(^-|-$)/g, "");
  }
}
