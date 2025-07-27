// dashboard/index.js
import { setupTabs } from "./tabs.js";
import { updateSchedulerStatus } from "./schedulers.js";
import { setupAppearanceNotesAutoSave } from "./notes.js";

window.addEventListener("DOMContentLoaded", () => {
  setupTabs();
  updateSchedulerStatus();
  setupAppearanceNotesAutoSave();
});
