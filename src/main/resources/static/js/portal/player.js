// portal/player.js
export let API_BASE = location.origin;

export async function fetchApiBase() {
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

export async function fetchToken() {
  const res = await fetch(`${API_BASE}/auth/token`);
  const { token } = await res.json();
  return token;
}

export async function setupAudio(token) {
  const audio = document.createElement("audio");
  audio.src = `${API_BASE}/radio?token=${encodeURIComponent(token)}`;
  audio.controls = true;
  audio.autoplay = false;
  document.getElementById("player-container").innerHTML = "";
  document.getElementById("player-container").appendChild(audio);
}

export async function setupVideo(token) {
  const iframe = document.createElement("iframe");
  iframe.src = `${API_BASE}/owncast/embed/video?token=${encodeURIComponent(token)}`;
  iframe.width = "550";
  iframe.height = "350";
  iframe.allowFullscreen = true;
  iframe.style.border = "none";
  document.getElementById("video-container").innerHTML = "";
  document.getElementById("video-container").appendChild(iframe);
}
