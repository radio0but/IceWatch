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
    container.className = "description-box audio-description";

    container.innerHTML = `
      <h3 style="margin-top:0;">ℹ️ Émission Radio</h3>
      ${window.marked ? marked.parse(audioInfo) : `<p>${audioInfo}</p>`}
    `;

    old.replaceWith(container);
  }
}

if (videoInfo) {
  const old = document.querySelector("#video-layout #video-description");
  if (old) {
    const container = document.createElement("div");
    container.id = "video-description";
    container.className = "description-box video-description";

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
