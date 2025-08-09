import { setupTinyMCE, destroyTinyMCE } from "./tinymce.js";

export function openArticleEditor(article, onSave) {
    const modal = document.getElementById("article-editor-modal");
    const form = modal.querySelector("form");

    // Toujours détruire l'ancien éditeur TinyMCE avant d'en recréer un
    destroyTinyMCE("#article-content");

    // Si article fourni → édition, sinon → création
    form.querySelector("#article-title").value = article ? article.title : "";
    form.querySelector("#article-slug").value = article ? article.slug : "";
    form.querySelector("#article-published").checked = article ? article.published : true;

    // Initialiser TinyMCE avec contenu ou vide
    setupTinyMCE("#article-content", article ? article.content : "");

    modal.style.display = "block";

    form.onsubmit = async (e) => {
        e.preventDefault();

        const data = {
            title: form.querySelector("#article-title").value.trim(),
            slug: form.querySelector("#article-slug").value.trim(),
            content: tinymce.get("article-content").getContent(),
            published: form.querySelector("#article-published").checked
        };

        await onSave(data, article ? article.id : null);
        closeArticleEditor();
    };

    modal.querySelector(".btn-cancel").onclick = () => {
        closeArticleEditor();
    };

    function closeArticleEditor() {
        modal.style.display = "none";
        destroyTinyMCE("#article-content");
    }
}
