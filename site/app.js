"use strict";

(() => {
  const data = document.getElementById("preset-data");
  const explorer = document.querySelector(".quality-explorer");
  if (!data || !explorer) return;
  const presets = JSON.parse(data.textContent);
  const controls = [...document.querySelectorAll('.preset-picker input[type="radio"]')];
  const fields = {
    "preset-title": "label",
    "preset-copy": "description",
    "preset-motion": "motionLabel",
    "preset-glass": "glassLabel",
    "preset-fast": "fastLabel",
  };

  function select(index) {
    const preset = presets[index];
    if (!preset) return;
    explorer.dataset.preset = preset.id.toLowerCase();
    for (const [id, key] of Object.entries(fields)) {
      document.getElementById(id).textContent = preset[key];
    }
    document.getElementById("preset-number").textContent = `${String(index + 1).padStart(2, "0")} / ${String(presets.length).padStart(2, "0")}`;
    document.getElementById("preset-workload").textContent = `${preset.workload} workload`;
  }

  controls.forEach((control, index) => {
    control.addEventListener("change", () => {
      if (control.checked) select(index);
    });
  });
  select(controls.findIndex((control) => control.checked));
})();
