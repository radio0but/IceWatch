//Journal.js
import { initJournalRSS } from "./rss.js";

export async function initJournalTab() {
    const journalList = document.getElementById("journal-articles");
    if (!journalList) return; // Pas d'onglet journal

    if (journalList.classList.contains("rss-feed")) {
        initJournalRSS();
    } else if (journalList.classList.contains("internal-feed")) {
        loadInternalArticles();
    }
}

async function loadInternalArticles() {
    try {
        const res = await fetch("/public/articles");
        if (!res.ok) throw new Error("Impossible de charger les articles");
        const articles = await res.json();

        const list = document.getElementById("journal-articles");
        list.innerHTML = "";

        if (!articles.length) {
            list.innerHTML = "<li>Aucun article publié pour le moment.</li>";
            return;
        }

        articles
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) // plus récent d'abord
    .forEach(article => {
        const li = document.createElement("li");
        li.classList.add("journal-item");
        li.innerHTML = `
            <h3 class="journal-title">${article.title}</h3>
            <div class="journal-content">
                ${article.content}
            </div>
            <small class="journal-date">
                Publié le ${new Date(article.createdAt).toLocaleDateString("fr-FR")}
            </small>
        `;
        list.appendChild(li);
    });

    } catch (err) {
        console.error(err);
        document.getElementById("journal-articles").innerHTML =
            "<li>Erreur lors du chargement des articles internes.</li>";
    }
}
