// dashboard/rss.js
export async function loadRSSConfig() {
  try {
    const res = await fetch("/api/settings/rss-url");
    const { rssUrl } = await res.json();
    document.getElementById("rss-url").value = rssUrl || "";

    const resImg = await fetch("/api/settings/rss-show-images");
    const { showImages } = await resImg.json();
    document.getElementById("rss-show-images").checked = !!showImages;

    document.getElementById("rss-show-images").addEventListener("change", async (e) => {
      await fetch("/api/settings/rss-show-images", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ key: "rss-show-images", value: e.target.checked })
      });
    });

  } catch (e) {
    console.error("Erreur chargement RSS :", e);
  }
}

export async function testRSS() {
  const status = document.getElementById("rss-status");
  status.textContent = "⏳ Test en cours...";
  try {
    const res = await fetch("/api/settings/rss-url/test");
    const { valid } = await res.json();
    status.textContent = valid ? "✅ RSS valide" : "🔴 RSS invalide";
    status.style.color = valid ? "lime" : "red";
  } catch (e) {
    status.textContent = "⚠️ Erreur réseau";
    status.style.color = "orange";
  }
}

export async function saveRSS() {
  const url = document.getElementById("rss-url").value.trim();
  if (!url) return alert("URL invalide.");

  try {
    await fetch("/api/settings/rss-url", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ key: "rss-url", value: url })
    });
    alert("Flux RSS enregistré !");
    testRSS();
  } catch (e) {
    alert("Erreur d’enregistrement.");
  }
}
window.testRSS = testRSS;
window.saveRSS = saveRSS;
