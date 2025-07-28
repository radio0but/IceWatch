export async function loadEmissionDescriptions() {
  try {
    const res = await fetch("/admin/emission/current");
    if (!res.ok) return;

    const data = await res.json();
    const { audioInfo, videoInfo } = data;


// Description audio (radio)
if (audioInfo) {
  const old = document.querySelector("#radio-layout #audio-description");
  if (old) {
    const container = document.createElement("div");
    container.id = "audio-description";
    container.style.marginBottom = "1rem";
    container.style.padding = "0.5rem";
    container.style.background = "#222";
    container.style.borderRadius = "0.5rem";

    container.innerHTML = `
      <h3 style="margin-top:0;">ℹ️ Émission Radio</h3>
      ${window.marked ? marked.parse(audioInfo) : `<p>${audioInfo}</p>`}
    `;

    old.replaceWith(container);
  }
}


// Description vidéo (télévision)
if (videoInfo) {
  const old = document.querySelector("#video-layout #video-description");
  if (old) {
    const container = document.createElement("div");
    container.id = "video-description";
    container.style.marginBottom = "1rem";
    container.style.padding = "0.5rem";
    container.style.background = "#222";
    container.style.borderRadius = "0.5rem";

    container.innerHTML = `
      <h3 style="margin-top:0;">ℹ️ Émission Télévision</h3>
      ${window.marked ? marked.parse(videoInfo) : `<p>${videoInfo}</p>`}
    `;

    old.replaceWith(container);
  }
}

  } catch (err) {
    console.warn("Impossible de charger la description d’émission :", err);
  }
}
