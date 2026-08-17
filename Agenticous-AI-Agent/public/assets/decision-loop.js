(() => {
  "use strict";

  const root = document.querySelector("[data-decision-loop]");
  if (!root) return;

  const phases = [
    ["OBSERVE", "Receiving the wallet address and collecting live blockchain explorer data."],
    ["ANALYZE", "Inspecting and normalizing wallet transaction activity."],
    ["AI REASON", "Tiered Gemini intelligence is analyzing the verified transaction context."],
    ["TXs VERIFY", "Cross-checking transaction evidence against blockchain explorers."],
    ["ORGANISE", "Preparing a concise, sourced seven-day transaction report."],
    ["EXECUTE", "Returning the final transaction report to the Support agent."],
  ];

  const phaseElements = Array.from(root.querySelectorAll("[data-phase]"));
  const status = root.querySelector("[data-loop-status]");
  const currentPhase = root.querySelector("[data-current-phase]");
  const currentDetail = root.querySelector("[data-current-detail]");
  const cycleCount = root.querySelector("[data-cycle-count]");
  const countdown = root.querySelector("[data-countdown]");
  const lastUpdate = root.querySelector("[data-last-update]");
  const cycleSeconds = Number.parseInt(root.dataset.cycleSeconds || "30", 10);
  const phaseSeconds = Math.max(1, Math.floor(cycleSeconds / phases.length));
  let cycle = 1;
  let elapsed = 0;

  function setHealth(isOnline) {
    status.classList.toggle("is-online", isOnline);
    status.classList.toggle("is-offline", !isOnline);
    status.lastChild.textContent = isOnline ? " ONLINE" : " RETRYING";
  }

  async function checkHealth() {
    try {
      const response = await fetch("/healthz", {
        headers: { accept: "application/json" },
        cache: "no-store",
      });
      const payload = response.ok ? await response.json() : null;
      setHealth(payload?.status === "ok");
    } catch {
      setHealth(false);
    }
  }

  function render() {
    const phaseIndex = Math.min(phases.length - 1, Math.floor(elapsed / phaseSeconds));
    const secondsIntoPhase = elapsed % phaseSeconds;
    const secondsRemaining = Math.max(1, phaseSeconds - secondsIntoPhase);

    phaseElements.forEach((element, index) => {
      element.classList.toggle("is-active", index === phaseIndex);
      element.classList.toggle("is-complete", index < phaseIndex);
    });

    currentPhase.textContent = `${String(phaseIndex + 1).padStart(2, "0")} / 06 — ${phases[phaseIndex][0]}`;
    currentDetail.textContent = phases[phaseIndex][1];
    cycleCount.textContent = `#${cycle}`;
    countdown.textContent = `${secondsRemaining}s`;
    lastUpdate.textContent = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  }

  function tick() {
    render();
    elapsed += 1;

    if (elapsed >= cycleSeconds) {
      elapsed = 0;
      cycle += 1;
      checkHealth();
    }
  }

  checkHealth();
  tick();
  window.setInterval(tick, 1000);
})();
