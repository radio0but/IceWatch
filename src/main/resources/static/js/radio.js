let currentSlide = 0;
let isAutoScrolling = false;
let scrollInterval;
const API_BASE = "https://radio.boogiepit.com";
let lastTitle = "";

async function fetchToken() {
  const res = await fetch(`${API_BASE}/auth/token`);
  const { token } = await res.json();
  return token;
}

async function setupAudio(token) {
  const audio = document.createElement("audio");
  audio.src = `${API_BASE}/radio?token=${encodeURIComponent(token)}`;
  audio.controls = true;
  audio.autoplay = false;
  document.getElementById("player-container").innerHTML = "";
  document.getElementById("player-container").appendChild(audio);
}

async function setupVideo(token) {
  const iframe = document.createElement("iframe");
  iframe.src = `${API_BASE}/owncast/embed/video?token=${encodeURIComponent(token)}`;
  iframe.width = "550";
  iframe.height = "350";
  iframe.allowFullscreen = true;
  iframe.style.border = "none";
  document.getElementById("video-container").innerHTML = "";
  document.getElementById("video-container").appendChild(iframe);
}

function scrollCarousel(dir) {
  const container = document.getElementById("carousel-inner");
  const total = container.children.length;
  const cardWidth = container.children[0].offsetWidth;
  currentSlide = (currentSlide + dir + total) % total;

  container.style.transition = "transform 0.5s ease-in-out";
  container.style.transform = `translateX(-${currentSlide * cardWidth}px)`;

  stopAutoScroll();
}

function startAutoScroll() {
  const container = document.getElementById("carousel-inner");
  const total = container.children.length;
  const cardWidth = container.children[0].offsetWidth;

  function scrollContinuously() {
    if (!isAutoScrolling) return;
    container.style.transition = "transform 0.5s ease-in-out";
    currentSlide = (currentSlide + 1) % total;
    container.style.transform = `translateX(-${currentSlide * cardWidth}px)`;
  }

  scrollInterval = setInterval(scrollContinuously, 10000);
}

function stopAutoScroll() {
  clearInterval(scrollInterval);
  isAutoScrolling = false;
}

function startAutoScrollAfterDelay() {
  setTimeout(() => {
    isAutoScrolling = true;
    startAutoScroll();
  }, 1000);
}

async function updateMetadata() {
  try {
    const res = await fetch(`${API_BASE}/radio/metadata/enriched`);
    const data = await res.json();
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

      setTimeout(() => {
        metadataEl.classList.remove("fade-in");
      }, 500);
    }, 500);

    const allAlbums = data.allAlbums;
    const container = document.getElementById("carousel-inner");

    if (Array.isArray(allAlbums) && allAlbums.length > 0) {
      container.innerHTML = "";

      allAlbums.forEach(a => {
        const albumCard = document.createElement('div');
        albumCard.classList.add('album-card');
        albumCard.innerHTML = `
          <p>Les informations proviennent de <strong>MusicBrainz</strong>, une base de données musicale ouverte et collaborative alimentée par la communauté.</p>
          <p>Selon MusicBrainz, le morceau <strong>${title}</strong> est disponible sur l'album ou single <strong>${a.album}</strong> sorti en <strong>${a.date || 'date inconnue'}</strong>.</p>
          <img src="${a.cover}" onerror="this.src='/album.png'" alt="Jaquette">
        `;
        container.appendChild(albumCard);
      });

      document.getElementById("carousel-wrapper").style.display = "flex";
    } else {
      document.getElementById("carousel-wrapper").style.display = "none";
    }
  } catch (err) {
    document.getElementById("metadata").innerText = "Erreur de chargement des métadonnées.";
    document.getElementById("carousel-wrapper").style.display = "none";
  }
}

window.addEventListener("DOMContentLoaded", async () => {
  try {
    const token = await fetchToken();
    await setupAudio(token);
    await setupVideo(token);
    updateMetadata();
    startAutoScrollAfterDelay();
  } catch (err) {
    console.error("Erreur d'initialisation :", err);
    document.getElementById("player-container").innerText = "Erreur de lecture audio.";
    document.getElementById("video-container").innerText = "Erreur de lecture vidéo.";
  }

  setInterval(updateMetadata, 15000);

  document.querySelectorAll(".tab-button").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-button").forEach(b => b.classList.remove("active"));
      document.querySelectorAll(".tab-content").forEach(tab => tab.classList.remove("active"));
      btn.classList.add("active");
      const target = document.getElementById(`tab-${btn.dataset.tab}`);
      if (target) target.classList.add("active");
    });
  });

  document.getElementById("prev-button").addEventListener("click", () => {
    scrollCarousel(-1);
    stopAutoScroll();
  });

  document.getElementById("next-button").addEventListener("click", () => {
    scrollCarousel(1);
    stopAutoScroll();
  });
});
