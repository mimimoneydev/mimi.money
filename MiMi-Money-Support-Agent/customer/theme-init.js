(function(){
  var stored=localStorage.getItem('mimi_support_dark_mode');
  var prefersDark=window.matchMedia&&window.matchMedia('(prefers-color-scheme:dark)').matches;
  if(stored==='true'||(stored===null&&prefersDark)){
    document.documentElement.setAttribute('data-theme','dark');
  }
})();
