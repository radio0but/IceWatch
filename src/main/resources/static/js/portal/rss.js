// portal/rss.js
let rssItems = [];
let showingAll = false;
let rssImagesEnabled = false;

function extractImageFromItem(item) {
  const thumb = item.querySelector("media\\:thumbnail")?.getAttribute("url") ||
                item.querySelector("media\\:content")?.getAttribute("url");
  if (thumb) return thumb;

  const contentEncoded = item.getElementsByTagNameNS("*", "encoded")[0]?.textContent;
  const match = contentEncoded?.match(/<img[^>]+src=\"([^">]+)\"/i);
  return match?.[1] || "";
}

function updateRSSDisplay() {
  const list = document.getElementById("journal-articles");
  list.innerHTML = "";

  if (!rssItems.length) {
    list.innerHTML = "<li>Aucun article trouvé pour ce flux RSS.</li>";
    return;
  }

  const toShow = showingAll ? rssItems : rssItems.slice(0, 5);
  toShow.forEach(item => {
    const title = item.querySelector("title")?.textContent || "Sans titre";
    const link = item.querySelector("link")?.textContent || "#";
    const date = new Date(item.querySelector("pubDate")?.textContent || "").toLocaleDateString();
    const desc = item.querySelector("description")?.textContent || "";
    const img = extractImageFromItem(item);

    const li = document.createElement("li");
    li.innerHTML = `
      ${img && rssImagesEnabled ? `<img src="${img}" class="rss-thumb"><br>` : ""}
      <strong>${title}</strong> <em>(${date})</em><br>
      ${desc}<br>
      <a href="${link}" target="_blank">Lire l’article</a>
      <hr>`;
    list.appendChild(li);
  });

  const btn = document.getElementById("toggle-rss");
  btn.innerText = showingAll ? "🔽 Afficher moins" : "📜 Afficher tout";
  btn.style.display = rssItems.length > 5 ? "inline-block" : "none";
}

export async function initJournalRSS() {
  try {
    const urlRes = await fetch("/api/settings/rss-url");
    const { rssUrl } = await urlRes.json();

    const imgRes = await fetch("/api/settings/rss-show-images");
    const { showImages } = await imgRes.json();
    rssImagesEnabled = !!showImages;

    if (!rssUrl) return;

    const feedRes = await fetch(`/proxy/rss?url=${encodeURIComponent(rssUrl)}`);
    const xml = await feedRes.text();
    const parser = new DOMParser();
    const doc = parser.parseFromString(xml, "text/xml");
    rssItems = Array.from(doc.querySelectorAll("item"));

    if (rssItems.length > 0) {
      document.getElementById("journal-tab-button")?.classList.remove("hidden");
      document.getElementById("tab-journal")?.classList.remove("hidden");
    }
    updateRSSDisplay();

    document.getElementById("toggle-rss")?.addEventListener("click", () => {
      showingAll = !showingAll;
      updateRSSDisplay();
    });
  } catch (err) {
    console.warn("RSS invalide ou indisponible", err);
  }
}