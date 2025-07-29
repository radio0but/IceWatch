//radioPlayer.js
let API_BASE = location.origin;

async function fetchApiBase() {
  try {
    const res = await fetch("/config/frontend");
    if (res.ok) {
      const data = await res.json();
      if (data.apiBase) API_BASE = data.apiBase;
    }
  } catch (e) {
    console.warn("Impossible de récupérer API_BASE, utilisation de", API_BASE);
  }
}

async function fetchToken() {
  const res = await fetch(`${API_BASE}/auth/token`);
  const { token } = await res.json();
  return token;
}

export function setupCustomAudio(token) {
  const container = document.getElementById("player-container");
  container.innerHTML = `
    <div class="custom-player">
      <button id="play-pause" class="play">▶️</button>
      <span id="player-status">En pause</span>
      <input type="range" id="volume" min="0" max="1" step="0.01" value="1">
    </div>
  `;

  const audio = new Audio(`${API_BASE}/radio?token=${encodeURIComponent(token)}`);
  const playPause = document.getElementById("play-pause");
  const status = document.getElementById("player-status");
  const volumeSlider = document.getElementById("volume");

  playPause.addEventListener("click", () => {
    if (audio.paused) {
      audio.play();
      playPause.textContent = "⏸️";
      status.textContent = "Lecture en cours...";
    } else {
      audio.pause();
      playPause.textContent = "▶️";
      status.textContent = "En pause";
    }
  });

  volumeSlider.addEventListener("input", () => {
    audio.volume = volumeSlider.value;
  });
}
