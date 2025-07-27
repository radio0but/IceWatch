export async function loadEmissionDescriptions() {
  try {
    const res = await fetch("/admin/emission/current");
    if (!res.ok) return;

    const data = await res.json();
    const { audio, video } = data;

    // Description audio (radio)
    if (audio) {
      const tabRadio = document.getElementById("tab-radio");
      if (tabRadio) {
        const old = document.getElementById("audio-description");
        if (old) old.remove();

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

        tabRadio.prepend(container);
      }
    }

    // Description vidéo (télévision)
    if (video) {
      const tabVideo = document.getElementById("tab-video");
      if (tabVideo) {
        const old = document.getElementById("video-description");
        if (old) old.remove();

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

        tabVideo.prepend(container);
      }
    }
  } catch (err) {
    console.warn("Impossible de charger la description d’émission :", err);
  }
}
