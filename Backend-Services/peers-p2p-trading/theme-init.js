(function () {
  try {
    var stored = localStorage.getItem('p2p_dark_mode');
    var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme:dark)').matches;
    if (stored === 'true' || (!stored && prefersDark)) {
      document.documentElement.setAttribute('data-theme', 'dark');
    }
  } catch (e) {}
})();
