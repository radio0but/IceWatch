// portal/tabs.js
export function setupTabs() {
  document.querySelectorAll(".tab-button").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-button").forEach(b => b.classList.remove("active"));
      document.querySelectorAll(".tab-content").forEach(tab => tab.classList.remove("active"));
      btn.classList.add("active");
      const target = document.getElementById(`tab-${btn.dataset.tab}`);
      if (target) target.classList.add("active");
    });
  });
}
