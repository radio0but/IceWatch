import { fetchUsers } from "./users.js";
import { loadProperties } from "./properties.js";
import { loadSchedule } from "./schedule.js";
import { loadRSSConfig } from "./rss.js";

export function setupTabs() {
  const tabButtons = document.querySelectorAll(".tab-button");
  const tabContents = document.querySelectorAll(".tab-content");

  tabButtons.forEach(button => {
    button.addEventListener("click", () => {
      const tabId = button.dataset.tab;

      tabButtons.forEach(b => b.classList.remove("active"));
      tabContents.forEach(c => c.classList.remove("active"));
      button.classList.add("active");
      document.getElementById(`tab-${tabId}`).classList.add("active");

      if (tabId === "users") fetchUsers();
      if (tabId === "appearance") loadProperties();
      if (tabId === "slots") loadSchedule();
      if (tabId === "rss") loadRSSConfig();
    });
  });

  if (tabButtons.length > 0) tabButtons[0].click();
}
