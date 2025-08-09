import { setupTinyMCE } from "./tinymce.js";

let articles = [];
let currentId = null; // null = création, sinon édition

export async function initArticlesSection() {
    const container = document.getElementById("articles-section");
    if (!container) return;

    // Init TinyMCE pour le contenu HTML
    setupTinyMCE("#article-content");

    // Charger et afficher les articles existants
    await loadArticles();
    renderArticlesTable();

    // Bouton "Nouvel article" → réinitialise le formulaire
    document.getElementById("new-article-btn").addEventListener("click", () => {
        resetForm();
        currentId = null;
    });

    // Bouton "Enregistrer" (submit)
    document.querySelector("#article-editor form").addEventListener("submit", async (e) => {
        e.preventDefault();
        await saveArticleFromForm();
    });
}

async function loadArticles() {
    try {
        const res = await fetch("/admin/articles");
        if (!res.ok) throw new Error("Impossible de charger les articles");
        articles = await res.json();
    } catch (err) {
        console.error(err);
        articles = [];
    }
}

function renderArticlesTable() {
    const tbody = document.querySelector("#articles-table tbody");
    tbody.innerHTML = "";

    if (!articles.length) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;">Aucun article</td></tr>`;
        return;
    }

    articles.forEach(article => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${article.title}</td>
            <td>${article.slug}</td>
            <td>${article.published ? "✅" : "❌"}</td>
            <td>
                <button class="btn-edit" data-id="${article.id}">✏️</button>
                <button class="btn-delete" data-id="${article.id}">🗑️</button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    // Actions édition
    tbody.querySelectorAll(".btn-edit").forEach(btn => {
        btn.addEventListener("click", () => {
            const article = articles.find(a => a.id == btn.dataset.id);
            fillForm(article);
            currentId = article.id;
        });
    });

    // Actions suppression
    tbody.querySelectorAll(".btn-delete").forEach(btn => {
        btn.addEventListener("click", async () => {
            if (!confirm("Supprimer cet article ?")) return;
            await fetch(`/admin/articles/${btn.dataset.id}`, { method: "DELETE" });
            await loadArticles();
            renderArticlesTable();
        });
    });
}

function resetForm() {
    document.getElementById("article-title").value = "";
    document.getElementById("article-slug").value = "";
    document.getElementById("article-published").checked = true;
    tinymce.get("article-content")?.setContent("");
}

function fillForm(article) {
    document.getElementById("article-title").value = article.title;
    document.getElementById("article-slug").value = article.slug;
    document.getElementById("article-published").checked = article.published;
    tinymce.get("article-content")?.setContent(article.content || "");
}

async function saveArticleFromForm() {
    const articleData = {
        title: document.getElementById("article-title").value.trim(),
        slug: document.getElementById("article-slug").value.trim(),
        published: document.getElementById("article-published").checked,
        content: tinymce.get("article-content")?.getContent() || ""
    };

    const method = currentId ? "PUT" : "POST";
    const url = currentId ? `/admin/articles/${currentId}` : "/admin/articles";

    await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(articleData)
    });

    await loadArticles();
    renderArticlesTable();
    resetForm();
    currentId = null;
}
