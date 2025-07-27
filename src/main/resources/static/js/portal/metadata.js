// portal/metadata.js
import { API_BASE } from "./player.js";

let currentSlide = 0;
let isAutoScrolling = false;
let scrollInterval;
let lastTitle = "";

export function updateMetadata() {
  fetch(`${API_BASE}/radio/metadata/enriched`)
    .then(res => res.json())
    .then(data => {
      const title = data.title || "Inconnu";
      const artist = data.artist || "";
      const fullTitle = artist ? `${artist} – ${title}` : `En cours : ${title}`;
      if (fullTitle === lastTitle) return;
      lastTitle = fullTitle;

      const metadataEl = document.getElementById("metadata");
      metadataEl.classList.add("fade-out");
      setTimeout(() => {
        metadataEl.innerText = `🎶 ${fullTitle}`;
        metadataEl.classList.remove("fade-out");
        metadataEl.classList.add("fade-in");
        setTimeout(() => metadataEl.classList.remove("fade-in"), 500);
      }, 500);

      const allAlbums = data.allAlbums;
      const container = document.getElementById("carousel-inner");

      if (Array.isArray(allAlbums) && allAlbums.length > 0) {
        container.innerHTML = "";
        allAlbums.forEach(a => {
          const card = document.createElement("div");
          card.className = "album-card";
          card.innerHTML = `
            <p>Les informations proviennent de <strong>MusicBrainz</strong>, une base de données musicale ouverte et collaborative alimentée par la communauté.</p>
            <p>Selon MusicBrainz, le morceau <strong>${title}</strong> est disponible sur l'album ou single <strong>${a.album}</strong> sorti en <strong>${a.date || 'date inconnue'}</strong>.</p>
            <img src="${a.cover}" onerror="this.src='/album.png'" alt="Jaquette">
          `;
          container.appendChild(card);
        });
        document.getElementById("carousel-wrapper").style.display = "flex";
      } else {
        document.getElementById("carousel-wrapper").style.display = "none";
      }
    })
    .catch(err => {
      document.getElementById("metadata").innerText = "Erreur de chargement des métadonnées.";
      document.getElementById("carousel-wrapper").style.display = "none";
    });
}

export function startAutoScrollAfterDelay() {
  setTimeout(() => {
    isAutoScrolling = true;
    startAutoScroll();
  }, 1000);
}

function startAutoScroll() {
  const container = document.getElementById("carousel-inner");
  const total = container.children.length;
  const cardWidth = container.children[0].offsetWidth;
  scrollInterval = setInterval(() => {
    if (!isAutoScrolling) return;
    currentSlide = (currentSlide + 1) % total;
    container.style.transition = "transform 0.5s ease-in-out";
    container.style.transform = `translateX(-${currentSlide * cardWidth}px)`;
  }, 10000);
}

function stopAutoScroll() {
  clearInterval(scrollInterval);
  isAutoScrolling = false;
}

window.scrollCarousel = function (dir) {
  const container = document.getElementById("carousel-inner");
  const total = container.children.length;
  const cardWidth = container.children[0].offsetWidth;
  currentSlide = (currentSlide + dir + total) % total;
  container.style.transition = "transform 0.5s ease-in-out";
  container.style.transform = `translateX(-${currentSlide * cardWidth}px)`;
  stopAutoScroll();
};
