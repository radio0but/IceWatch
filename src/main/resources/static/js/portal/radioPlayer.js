//radioPlayer.js
import { API_BASE, fetchApiBase, fetchToken } from "./player.js";

export async function initCustomAudioPlayer() {
  await fetchApiBase();
  const tokenFromUrl = new URLSearchParams(location.search).get("token");
  const token = tokenFromUrl || await fetchToken();

  if (!token) {
    document.getElementById("player-container").innerHTML =
      "<p>❌ Impossible d’obtenir un token.</p>";
    return;
  }

  setupCustomAudio(token);
}




function setupCustomAudio(token) {
  const container = document.getElementById("player-container");
container.innerHTML = `
<div class="custom-player">
  <div class="top-bar">
  <button id="play-pause" class="play">
    <img id="play-icon" src="/img/btn-play.png" alt="Play" width="24" height="24">
  </button>
  <span id="player-status">En pause</span>
</div>

  <div class="progress-container">
    <input type="range" id="progress-bar" min="0" step="0.01" value="0">
    <span id="buffer-end">
      <img src="/img/icon-buffer-end.png" alt="Fin du buffer" width="16" height="16" style="vertical-align:middle; margin-right:4px;">
      0:00
    </span>
  </div>

<div class="volume-container">
  <button id="mute-toggle">
    <img src="/img/icon-volume-mute.png" alt="Mute" width="24">
  </button>
  <input type="range" id="volume" min="0" max="1" step="0.01" value="1">
  <span id="time-display">
    <span id="current-time">0 : 00</span> / <span id="total-time">--</span>
  </span>
</div>
</div>


`;

  const audio = new Audio(`${API_BASE}/radio?token=${encodeURIComponent(token)}`);
  audio.preload = "auto";
  audio.autoplay = false;

  const playPause = document.getElementById("play-pause");
  const status = document.getElementById("player-status");
  const volumeSlider = document.getElementById("volume");

  const progressBar = document.getElementById("progress-bar");
  const currentTimeDisplay = document.getElementById("current-time");
  const bufferEndLabel = document.getElementById("buffer-end");
const muteButton = document.getElementById("mute-toggle");

muteButton.addEventListener("click", () => {
  audio.muted = !audio.muted;
  muteButton.querySelector("img").src = audio.muted
    ? "/img/icon-volume-mute.png"
    : "/img/icon-volume-plus.png"; // ou une autre icône "volume normal"
});

playPause.addEventListener("click", () => {
  const icon = document.getElementById("play-icon");
  if (audio.paused) {
    audio.play();
    icon.src = "/img/btn-pause.png";
    icon.alt = "Pause";
    status.textContent = "Lecture en cours...";
  } else {
    audio.pause();
    icon.src = "/img/btn-play.png";
    icon.alt = "Play";
    status.textContent = "En pause";
  }
});


  volumeSlider.addEventListener("input", () => {
    audio.volume = volumeSlider.value;
  });

  // ⏱️ Mise à jour de la barre de progression
  setInterval(() => {
    if (audio.buffered.length > 0) {
      const end = audio.buffered.end(audio.buffered.length - 1); // Fin du buffer
      progressBar.max = end.toFixed(2); // max possible (fin du buffer)
      progressBar.value = audio.currentTime.toFixed(2); // position actuelle
      bufferEndLabel.innerHTML = `
  <img src="/img/icon-buffer-end.png" alt="Fin du buffer" width="16" height="16" style="vertical-align:middle; margin-right:4px;">
  ${formatTime(end)}
`;
      currentTimeDisplay.textContent = formatTime(audio.currentTime);
    }
  }, 1000);

  // 👇 Permet de reculer dans le buffer si l’utilisateur clique sur la barre
  progressBar.addEventListener("input", () => {
    const seekTime = parseFloat(progressBar.value);
    const end = audio.buffered.end(audio.buffered.length - 1);
    if (seekTime <= end) {
      audio.currentTime = seekTime;
    }
  });

  function formatTime(sec) {
    if (!isFinite(sec)) return "∞";
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m}:${s < 10 ? "0" : ""}${s}`;
  }
}


