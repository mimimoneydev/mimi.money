var App = (function () {
  var API_BASE = '';
  var API_SECRET = '';
  var WS = null;
  var WS_RECONNECT_TIMER = null;
  var WS_RECONNECT_ATTEMPT = 0;
  var REQUEST_TIMEOUT_MS = 20000;

  var S = {
    tab: 0,
    gcSubTab: 0,
    view: 'main',
    viewArgs: {},
    history: [],
    settings: null,
    profile: null,
    adsList: [],
    ordersList: [],
    disputeList: [],
    myAdsList: [],
    wallets: [],
    paymentMethods: [],
    adminPaymentMethods: [],
    feedBackList: [],
    gcPageData: null,
    gcAdsList: [],
    gcOrdersList: [],
    gcCardList: [],
    gcUserAdList: [],
    adsDetails: null,
    orderDetails: null,
    gcOrderDetails: null,
    createAdsData: {},
    createAdsSettings: null,
    loading: false,
    homeTxType: 1,
    homeCoin: 1,
    homeCurrency: 0,
    homePayment: 0,
    homeCountry: 0,
    homeAmount: '',
    homePage: 1,
    homeHasMore: true,
    ordersPage: 1,
    ordersHasMore: true,
    ordersCoin: 'all',
    ordersStatus: 'all',
    ordersFromDate: '',
    ordersToDate: '',
    myAdsTxType: 1,
    myAdsPage: 1,
    myAdsHasMore: true,
    gcFilterPrice: 'any',
    gcFilterPayType: '1',
    gcFilterCurrencyType: '',
    gcFilterPayMethod: '',
    gcFilterCountry: '',
    gcOrdersStatus: '',
    gcOrdersPage: 1,
    gcAdsStatus: '',
    gcAdsPage: 1,
    gcListPage: 1,
    p2pToken: null,
    p2pUser: null,
    p2pUserId: null,
    isP2PLoggedIn: false,
    pendingAuthView: null,
    countdownIntervals: []
  };

  function api(path, opts) {
    var url = API_BASE + path;
    var hdrs = {};
    var controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
    var timeoutId = null;
    if (S.p2pToken) {
      hdrs['Authorization'] = 'Bearer ' + S.p2pToken;
      hdrs['token'] = S.p2pToken;
    }
    if (API_SECRET) hdrs['userapisecret'] = API_SECRET;
    if (opts && opts.headers) Object.assign(hdrs, opts.headers);
    var fetchOpts = { method: (opts && opts.method) || 'GET', headers: hdrs };
    if (controller) {
      fetchOpts.signal = controller.signal;
      timeoutId = setTimeout(function () { controller.abort(); }, REQUEST_TIMEOUT_MS);
    }
    if (opts && opts.body) {
      if (opts.isFormData) {
        fetchOpts.body = opts.body;
        delete hdrs['Content-Type'];
      } else {
        hdrs['Content-Type'] = 'application/json';
        fetchOpts.body = JSON.stringify(opts.body);
      }
    }
    fetchOpts.headers = hdrs;
    return fetch(url, fetchOpts)
      .then(function (r) {
        var ct = r.headers.get('content-type') || '';
        if (ct.indexOf('application/json') >= 0) {
          return r.json().then(function (data) {
            if (!r.ok && data && typeof data === 'object' && !data.message) data.message = 'Request failed (' + r.status + ')';
            return data;
          });
        }
        return r.text().then(function (txt) {
          try { return JSON.parse(txt); }
          catch (e) { return { success: false, message: r.ok ? 'Server returned an invalid response' : 'Request failed (' + r.status + ')' }; }
        });
      })
      .catch(function (e) {
        return { success: false, message: e && e.name === 'AbortError' ? 'Request timed out. Please try again.' : ((e && e.message) || 'Network request failed') };
      })
      .then(function (result) {
        if (timeoutId) clearTimeout(timeoutId);
        return result;
      });
  }

  function apiGet(path, params) {
    var qs = '';
    if (params) {
      var parts = [];
      for (var k in params) {
        if (params[k] !== undefined && params[k] !== null && params[k] !== '')
          parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
      }
      if (parts.length) qs = '?' + parts.join('&');
    }
    return api(path + qs);
  }

  function apiPost(path, body) { return api(path, { method: 'POST', body: body }); }
  function toFormData(data) {
    if (data instanceof FormData) return data;
    var fd = new FormData();
    for (var k in data) {
      if (data[k] !== undefined && data[k] !== null) fd.append(k, data[k]);
    }
    return fd;
  }
  function apiPostFD(path, fd) { return api(path, { method: 'POST', body: toFormData(fd), isFormData: true }); }

  function fmt(n, d) {
    if (n == null) return '0';
    d = d || 2;
    return parseFloat(n).toFixed(d).replace(/\.?0+$/, '');
  }

  function fmtPrice(n, c) { return fmt(n) + ' ' + (c || ''); }
  function fmtCrypto(n) { return fmt(n, 8); }

  function fmtDate(d) {
    if (!d) return '';
    try {
      var dt = new Date(d);
      return dt.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) +
        ' ' + dt.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    } catch (e) { return d; }
  }

  function twoDigit(n) { return n < 10 ? '0' + n : '' + n; }

  function statusText(s, isDispute) {
    if (isDispute) {
      switch (s) {
        case 6: return 'Refund By Admin';
        case 7: return 'Released By Admin';
        default: return 'Disputed';
      }
    }
    switch (s) {
      case 0: return 'Time Expired';
      case 1: return 'In Escrow';
      case 2: return 'Payment Done';
      case 3: return 'Transfer Done';
      case 4: return 'Canceled';
      case 5: return 'Disputed';
      case 6: return 'Refund By Admin';
      case 7: return 'Released By Admin';
    }
    return 'Unknown';
  }

  function statusColor(s, isDispute) {
    if (isDispute) {
      switch (s) { case 6: return 'amber'; case 7: return 'pri'; default: return 'amber'; }
    }
    switch (s) {
      case 0: return 'red'; case 1: return 'blue'; case 2: return 'green';
      case 3: return 'green'; case 4: return 'red'; case 5: return 'amber';
      case 6: return 'amber'; case 7: return 'amber';
    }
    return 'txt3';
  }

  function gcStatusText(s) {
    switch (s) {
      case 0: return 'Deactivate'; case 1: return 'Active'; case 2: return 'Success';
      case 3: return 'Canceled'; case 4: return 'Ongoing';
    }
    return 'Unknown';
  }

  function gcStatusColor(s) {
    switch (s) {
      case 0: return 'red'; case 1: return 'blue'; case 2: return 'green';
      case 3: return 'red'; case 4: return 'amber';
    }
    return 'txt3';
  }

  function toast(msg, err) {
    var t = document.createElement('div');
    t.className = 'toast' + (err ? ' error' : '');
    t.textContent = msg;
    document.body.appendChild(t);
    setTimeout(function () { t.remove(); }, 3000);
  }

  function go(view, args) {
    S.history.push({ view: S.view, args: S.viewArgs });
    S.view = view;
    S.viewArgs = args || {};
    render();
  }

  function goBack() {
    if (S.history.length) {
      var prev = S.history.pop();
      S.view = prev.view;
      S.viewArgs = prev.args;
      render();
    }
  }

  function setTab(i) { S.tab = i; S.gcSubTab = 0; render(); }

  function isLoggedIn() {
    return !!(S.isP2PLoggedIn && S.p2pToken);
  }

  function requireLogin(view, args) {
    S.pendingAuthView = { view: view, args: args || {} };
    go('login');
  }

  function h(tag, attrs) {
    var el = document.createElement(tag);
    if (attrs) {
      for (var k in attrs) {
        if (k === 'className') el.className = attrs[k];
        else if (k.startsWith('on') && typeof attrs[k] === 'function') el.addEventListener(k.slice(2).toLowerCase(), attrs[k]);
        else if ((k === 'href' || k === 'src') && !isSafeUrl(attrs[k], k === 'src')) continue;
        else el.setAttribute(k, attrs[k]);
      }
    }
    var children = Array.prototype.slice.call(arguments, 2);
    if (children.length) {
      children.forEach(function (c) {
        if (c == null) return;
        if (Array.isArray(c)) c.forEach(function (cc) {
          if (cc == null) return;
          if (typeof cc === 'string' || typeof cc === 'number') el.appendChild(document.createTextNode(cc));
          else el.appendChild(cc);
        });
        else if (typeof c === 'string' || typeof c === 'number') el.appendChild(document.createTextNode(c));
        else el.appendChild(c);
      });
    }
    return el;
  }

  function isSafeUrl(value, allowImages) {
    if (value == null) return true;
    var raw = String(value).trim();
    if (!raw) return true;
    if (raw.charAt(0) === '#' || raw.charAt(0) === '/') return true;
    try {
      var u = new URL(raw, window.location.href);
      if (u.protocol === 'http:' || u.protocol === 'https:') return true;
      if (allowImages && u.protocol === 'data:' && /^data:image\/(?:png|jpe?g|gif|webp);/i.test(raw)) return true;
    } catch (e) {}
    return false;
  }

  function $(sel) { return document.querySelector(sel); }

  function clearIntervals() {
    S.countdownIntervals.forEach(function (id) { clearInterval(id); });
    S.countdownIntervals = [];
  }

  function render() {
    clearIntervals();
    var app = document.getElementById('app');
    if (!app) return;
    try {
      app.innerHTML = '';
      if (S.view === 'main') renderMain(app);
      else if (S.view === 'adsDetails') renderAdsDetails(app);
      else if (S.view === 'orderDetails') renderOrderDetails(app);
      else if (S.view === 'createAds') renderCreateAds(app);
      else if (S.view === 'profile') renderProfile(app);
      else if (S.view === 'addPayment') renderAddPayment(app);
      else if (S.view === 'gcAdDetails') renderGCAdDetails(app);
      else if (S.view === 'gcOrderDetails') renderGCOrderDetails(app);
      else if (S.view === 'gcCreateAd') renderGCCreateAd(app);
      else if (S.view === 'login') renderLogin(app);
      else if (S.view === 'register') renderRegister(app);
      else if (S.view === 'forgotPassword') renderForgotPassword(app);
    } catch (e) {
      app.textContent = '';
      app.appendChild(h('div', { style: 'padding:20px;color:red;font-size:14px' }, 'Render error'));
    }
  }

  function applyTheme() {
    var dark = localStorage.getItem('p2p_dark_mode') === 'true';
    if (!localStorage.getItem('p2p_dark_mode') && window.matchMedia && window.matchMedia('(prefers-color-scheme:dark)').matches) dark = true;
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : '');
  }

  function toggleTheme() {
    var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    localStorage.setItem('p2p_dark_mode', isDark ? 'false' : 'true');
    applyTheme();
  }

  function renderHeader(app, title, showBack, extraRight) {
    var hd = h('div', { className: 'header' });
    if (showBack) hd.appendChild(h('button', { className: 'back-btn', onclick: goBack }, '\u2190'));
    hd.appendChild(h('h1', {}, title));
    var themeBtn = h('button', {
      className: 'back-btn',
      onclick: toggleTheme,
      style: 'font-size:16px'
    }, document.documentElement.getAttribute('data-theme') === 'dark' ? '\u2600' : '\u263E');
    hd.appendChild(themeBtn);
    if (extraRight) hd.appendChild(extraRight);
    app.appendChild(hd);
  }

  function renderBottomNav(app) {
    var nav = h('div', { className: 'bottom-nav' });
    var items = [
      { icon: '\u2302', label: 'Home', tab: 0 },
      { icon: '\u2630', label: 'Orders', tab: 1 },
      { icon: '\u263A', label: 'Profile', tab: 2 },
      { icon: '\uD83D\uDC5B', label: 'Wallet', tab: 3 },
      { icon: '\u270E', label: 'My Ads', tab: 4 },
      { icon: '\u2606', label: 'Gift Card', tab: 5 }
    ];
    items.forEach(function (item) {
      var btn = h('button', {
        className: S.tab === item.tab ? 'active' : '',
        onclick: function () { setTab(item.tab); }
      });
      btn.appendChild(h('span', { className: 'nav-icon' }, item.icon));
      btn.appendChild(h('span', { className: 'nav-label' }, item.label));
      nav.appendChild(btn);
    });
    app.appendChild(nav);
  }

  function renderMain(app) {
    renderHeader(app, 'P2P Trading', false);
    var ct = h('div', { className: 'content' });
    switch (S.tab) {
      case 0: renderHome(ct); break;
      case 1: renderOrders(ct); break;
      case 2: renderUserCenter(ct); break;
      case 3: renderWallet(ct); break;
      case 4: renderMyAds(ct); break;
      case 5: renderGiftCard(ct); break;
    }
    app.appendChild(ct);
    renderBottomNav(app);
  }

  function renderHome(ct) {
    if (!S.settings) { loadHomeData(); ct.appendChild(renderLoading()); return; }
    var row = h('div', { className: 'row between mb12' });
    var seg = h('div', { className: 'seg' });
    seg.appendChild(h('button', { className: 'buy-tab' + (S.homeTxType === 1 ? ' active' : ''), onclick: function () { S.homeTxType = 1; loadAdsList(false); render(); } }, 'Buy'));
    seg.appendChild(h('button', { className: S.homeTxType === 2 ? 'active' : '', onclick: function () { S.homeTxType = 2; loadAdsList(false); render(); } }, 'Sell'));
    row.appendChild(seg);
    if (S.settings.assets && S.settings.assets.length) {
      var sel = h('select', { className: 'input', style: 'width:auto;max-width:120px;', onchange: function () { S.homeCoin = parseInt(this.value); loadAdsList(false); render(); } });
      S.settings.assets.forEach(function (a, i) { sel.appendChild(h('option', { value: String(i + 1) }, a.coin_type || '')); });
      sel.value = String(S.homeCoin);
      row.appendChild(sel);
    }
    var filterBtn = h('button', { className: 'btn btn-sm btn-out', onclick: function () { showFilterModal(); } }, '\u2699 Filter');
    row.appendChild(filterBtn);
    ct.appendChild(row);
    if (S.loading && S.adsList.length === 0) { ct.appendChild(renderLoading()); return; }
    if (S.adsList.length === 0) { ct.appendChild(renderEmpty('No ads found')); return; }
    S.adsList.forEach(function (ad) { ct.appendChild(renderAdCard(ad)); });
    if (S.homeHasMore) {
      ct.appendChild(h('button', { className: 'btn btn-block btn-out mt12', onclick: function () { loadAdsList(true); } }, 'Load More'));
    }
    ct.appendChild(h('div', { className: 'ta-c mt16' },
      h('button', { onclick: function () { showTutorial(); }, className: 'link-btn clr-pri fw600' }, 'How P2P works?')
    ));
  }

  function getPaymentMethodNameFromAd(ad) {
    if (!ad) return '';
    if (ad.payment_method_list && ad.payment_method_list.length) {
      var names = [];
      ad.payment_method_list.forEach(function (pm) {
        if (pm.admin_pamynt_method) {
          names.push(pm.admin_pamynt_method.name);
        }
      });
      if (names.length) return names;
    }
    if (ad.payment_method && S.adminPaymentMethods.length) {
      var found = S.adminPaymentMethods.find(function (apm) { return apm.uid === ad.payment_method; });
      if (found) return [found.name];
    }
    return [];
  }

  function renderAdCard(ad) {
    var c = h('div', { className: 'card' });
    var top = h('div', { className: 'row between mb8' });
    var uinfo = h('div', { className: 'row gap8', onclick: function () { go('profile', { userId: ad.user_id }); } });
    uinfo.appendChild(h('div', { className: 'avatar' }, (ad.user && (ad.user.nickname || ad.user.nick_name)) ? (ad.user.nickname || ad.user.nick_name).charAt(0).toUpperCase() : '?'));
    uinfo.appendChild(h('div', { className: 'col' },
      h('span', { className: 'fw600 fs13' }, (ad.user && (ad.user.nickname || ad.user.nick_name)) || 'User'),
      h('span', { className: 'fs11 clr-txt2' }, (ad.register_days || 0) + ' days')
    ));
    top.appendChild(uinfo);
    var btnLabel = (S.homeTxType === 1 ? 'Buy' : 'Sell') + ' ' + (ad.coin_type || '');
    var buyBtn = h('button', {
      className: 'btn btn-sm ' + (S.homeTxType === 1 ? 'btn-buy' : 'btn-pri'),
      onclick: function () {
        var args = { uid: ad.uid, adsType: S.homeTxType };
        if (!isLoggedIn()) requireLogin('adsDetails', args);
        else go('adsDetails', args);
      }
    }, btnLabel);
    top.appendChild(buyBtn);
    c.appendChild(top);
    c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Price:'), h('span', { className: 'fw600 fs13' }, fmtPrice(ad.price, ad.currency))));
    c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Available:'), h('span', { className: 'fs13' }, fmtCrypto(ad.available) + ' ' + (ad.coin_type || ''))));
    c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Limit:'), h('span', { className: 'fs13' }, fmt(ad.minimum_trade_size) + ' - ' + fmt(ad.maximum_trade_size) + ' ' + (ad.currency || ''))));
    var pmNames = getPaymentMethodNameFromAd(ad);
    if (pmNames.length) {
      var pRow = h('div', { className: 'row gap4', style: 'flex-wrap:wrap;margin-top:4px' });
      pmNames.forEach(function (nm) { pRow.appendChild(h('span', { className: 'payment-chip' }, nm)); });
      c.appendChild(h('div', { className: 'row gap8 mt4' }, h('span', { className: 'clr-txt2 fs12' }, 'Payment:'), pRow));
    }
    return c;
  }

  function showFilterModal() {
    var ol = h('div', { className: 'modal-overlay', onclick: function (e) { if (e.target === ol) ol.remove(); } });
    var sh = h('div', { className: 'modal-sheet' });
    sh.appendChild(h('h3', { className: 'fw600 mb16' }, 'Filter Ads'));
    sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Limit Amount'));
    var amtInput = h('input', { className: 'input mb12', type: 'number', placeholder: 'Write Amount', value: S.homeAmount || '' });
    amtInput.addEventListener('input', function () { S.homeAmount = this.value; });
    sh.appendChild(amtInput);
    if (S.settings && S.settings.currency) {
      sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Fiat'));
      var curSel = h('select', { className: 'input mb12' });
      curSel.appendChild(h('option', { value: 'all' }, 'All'));
      S.settings.currency.forEach(function (c) { curSel.appendChild(h('option', { value: c.currency_code || c.name }, c.name || c.currency_code)); });
      curSel.addEventListener('change', function () { S.homeCurrency = this.selectedIndex; });
      sh.appendChild(curSel);
    }
    if (S.settings && S.settings.payment_method) {
      sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Payment'));
      var paySel = h('select', { className: 'input mb12' });
      paySel.appendChild(h('option', { value: 'all' }, 'All'));
      S.settings.payment_method.forEach(function (p) { paySel.appendChild(h('option', { value: p.uid }, p.name)); });
      paySel.addEventListener('change', function () { S.homePayment = this.selectedIndex; });
      sh.appendChild(paySel);
    }
    if (S.settings && S.settings.country) {
      sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Available Regions'));
      var ctrySel = h('select', { className: 'input mb12' });
      ctrySel.appendChild(h('option', { value: 'all' }, 'All'));
      S.settings.country.forEach(function (c) { ctrySel.appendChild(h('option', { value: c.key }, c.value)); });
      ctrySel.addEventListener('change', function () { S.homeCountry = this.selectedIndex; });
      sh.appendChild(ctrySel);
    }
    sh.appendChild(h('button', { className: 'btn btn-pri btn-block mt8', onclick: function () { ol.remove(); loadAdsList(false); render(); } }, 'Apply'));
    ol.appendChild(sh);
    document.body.appendChild(ol);
  }

  function showTutorial() {
    var ol = h('div', { className: 'modal-overlay', onclick: function (e) { if (e.target === ol) ol.remove(); } });
    var sh = h('div', { className: 'modal-sheet' });
    sh.appendChild(h('h3', { className: 'fw600 mb16' }, 'How P2P works?'));
    var steps = [
      { t: 'Buy Step 1', d: 'Choose an ad and enter the amount you want to buy' },
      { t: 'Buy Step 2', d: 'Transfer the funds to the seller\'s payment method' },
      { t: 'Buy Step 3', d: 'Seller confirms payment and releases crypto to your wallet' }
    ];
    steps.forEach(function (s) {
      sh.appendChild(h('div', { className: 'card' }, h('div', { className: 'fw600 mb4' }, s.t), h('div', { className: 'clr-txt2 fs13' }, s.d)));
    });
    sh.appendChild(h('button', { className: 'btn btn-out btn-block mt12', onclick: function () { ol.remove(); } }, 'Close'));
    ol.appendChild(sh);
    document.body.appendChild(ol);
  }

  function renderAdsDetails(app) {
    if (!isLoggedIn()) {
      requireLogin('adsDetails', S.viewArgs);
      return;
    }
    var a = S.viewArgs;
    if (!S.adsDetails || S.adsDetails._uid !== a.uid) {
      loadAdsDetails(a.uid, a.adsType);
      renderHeader(app, 'Ads Details', true);
      var ct = h('div', { className: 'content' });
      ct.appendChild(renderLoading());
      app.appendChild(ct);
      return;
    }
    renderHeader(app, 'Ads Details', true);
    var ct = h('div', { className: 'content' });
    if (S.loading) { ct.appendChild(renderLoading()); app.appendChild(ct); return; }
    var d = S.adsDetails;
    if (!d || !d.ads) { ct.appendChild(renderEmpty('No details')); app.appendChild(ct); return; }
    var ad = d.ads;
    var topR = h('div', { className: 'row between mb12' });
    topR.appendChild(h('div', { className: 'row gap8', onclick: function () { go('profile', { userId: ad.user_id }); } },
      h('div', { className: 'avatar' }, ad.user ? (ad.user.nick_name || 'U').charAt(0).toUpperCase() : '?'),
      h('div', { className: 'col' },
        h('span', { className: 'fw600' }, ad.user ? ad.user.nick_name : 'User'),
        h('span', { className: 'fs11 clr-txt2' }, (ad.register_days || 0) + ' days')
      )
    ));
    topR.appendChild(h('div', { className: 'row gap12' },
      h('span', { className: 'fs12 clr-txt2' }, (d.orders || 0) + ' orders'),
      h('span', { className: 'fs12 clr-txt2' }, (d.completion || 0) + '% completion')
    ));
    ct.appendChild(topR);
    ct.appendChild(h('div', { className: 'row between mb4' }, h('span', {}, 'Price'), h('span', { className: 'fw600 clr-acc' }, fmtPrice(d.price, ad.currency))));
    ct.appendChild(h('div', { className: 'row between mb4' }, h('span', {}, 'Available'), h('span', {}, fmtCrypto(d.available) + ' ' + (ad.coin_type || ''))));
    ct.appendChild(h('div', { className: 'row between mb12' }, h('span', {}, 'Payment Time Limit'), h('span', {}, (d.payment_time || ad.payment_times || 0) + ' minutes')));
    var pmNames = [];
    if (d.payment_methods && d.payment_methods.length) {
      d.payment_methods.forEach(function (pm) {
        if (pm.admin_pamynt_method && pm.admin_pamynt_method.name) pmNames.push(pm.admin_pamynt_method.name);
      });
    }
    if (!pmNames.length && ad.admin_payment_method && S.adminPaymentMethods.length) {
      var found = S.adminPaymentMethods.find(function (apm) { return apm.uid === ad.admin_payment_method; });
      if (found) pmNames.push(found.name);
    }
    if (pmNames.length) {
      var pmRow = h('div', { className: 'row gap4 mb12', style: 'flex-wrap:wrap' });
      pmNames.forEach(function (nm) { pmRow.appendChild(h('span', { className: 'payment-chip' }, nm)); });
      ct.appendChild(pmRow);
    }
    if (d.termsAndCondition || ad.terms) {
      ct.appendChild(h('div', { className: 'card mb12' }, h('div', { className: 'fw600 mb4' }, 'Terms and Conditions'), h('div', { className: 'clr-txt2 fs13' }, d.termsAndCondition || ad.terms)));
    }
    ct.appendChild(h('div', { className: 'divider' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'I want to pay'));
    var priceInput = h('input', { className: 'input mb4', type: 'number', placeholder: 'Enter amount', id: 'adsPayInput' });
    ct.appendChild(priceInput);
    ct.appendChild(h('div', { className: 'form-hint mb12' }, 'Min price ' + (d.minimum_price || d.min_price || 0) + ' - Max price ' + (d.maximum_price || d.maximaum_price || d.max_price || 0) + ' ' + (ad.currency || '')));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'I will receive'));
    var amtInput = h('input', { className: 'input mb12', type: 'number', placeholder: 'Enter amount', id: 'adsRcvInput' });
    ct.appendChild(amtInput);
    var balBtn = h('button', { className: 'btn btn-sm btn-out', onclick: function () { loadAdsBalance(a.uid, ad.coin_type, a.adsType); } }, 'Get all balance');
    ct.appendChild(h('div', { className: 'balance-action-row' }, balBtn));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Select payment method'));
    var pmSel = h('select', { className: 'input mb12', id: 'adsPayMethodSel' });
    pmSel.appendChild(h('option', { value: '' }, 'Select'));
    S.paymentMethods.forEach(function (pm) {
      var pmName = getPaymentMethodDisplayName(pm);
      pmSel.appendChild(h('option', { value: pm.id }, pmName));
    });
    ct.appendChild(pmSel);
    var placeBtn = h('button', { className: 'btn ' + (a.adsType === 1 ? 'btn-buy' : 'btn-pri') + ' btn-block mt8' }, a.adsType === 1 ? 'Buy' : 'Sell');
    placeBtn.addEventListener('click', function () {
      var payVal = priceInput.value;
      var amtVal = amtInput.value;
      var payId = pmSel.value;
      if (!payVal && !amtVal) { toast('Enter amount', true); return; }
      if (!payId) { toast('Select payment method', true); return; }
      placeOrder(a.uid, a.adsType, payId, payVal, amtVal);
    });
    ct.appendChild(placeBtn);
    priceInput.addEventListener('input', function () {
      if (d.price && this.value) { amtInput.value = fmt(parseFloat(this.value) / d.price, 8); } else { amtInput.value = ''; }
    });
    amtInput.addEventListener('input', function () {
      if (d.price && this.value) { priceInput.value = fmt(parseFloat(this.value) * d.price, 2); } else { priceInput.value = ''; }
    });
    app.appendChild(ct);
  }

  function renderOrders(ct) {
    if (!isLoggedIn()) {
      ct.appendChild(h('div', { className: 'card ta-c p16' },
        h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }),
        h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'),
        h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your orders'),
        h('button', { className: 'btn btn-pri', onclick: function () { go('login'); } }, 'Login')
      ));
      return;
    }
    var subSeg = h('div', { className: 'seg mb12' });
    subSeg.appendChild(h('button', { className: S.ordersStatus === 'all' || S.ordersStatus === '' ? 'active' : '', onclick: function () { S.ordersStatus = 'all'; loadOrdersList(false); } }, 'All Orders'));
    subSeg.appendChild(h('button', { className: S.ordersStatus === '5' ? 'active' : '', onclick: function () { S.ordersStatus = '5'; loadDisputeList(false); } }, 'Disputed Orders'));
    ct.appendChild(subSeg);
    var list = S.ordersStatus === '5' ? S.disputeList : S.ordersList;
    if (list.length === 0 && !S.loading) {
      if (S.ordersStatus === '5') loadDisputeList(false);
      else loadOrdersList(false);
      return;
    }
    if (S.loading && list.length === 0) { ct.appendChild(renderLoading()); return; }
    if (list.length === 0) { ct.appendChild(renderEmpty('No orders')); return; }
    list.forEach(function (o) { ct.appendChild(renderOrderCard(o)); });
    if (S.ordersHasMore) {
      ct.appendChild(h('button', { className: 'btn btn-block btn-out mt12', onclick: function () { S.ordersStatus === '5' ? loadDisputeList(true) : loadOrdersList(true); } }, 'Load More'));
    }
  }

  function renderOrderCard(o) {
    var c = h('div', { className: 'card', onclick: function () { go('orderDetails', { uid: o.uid }); } });
    var isDispute = o.status === 5;
    c.appendChild(h('div', { className: 'row between mb4' },
      h('span', { className: 'fw600 fs13' }, o.coin_type || ''),
      h('span', { className: 'badge badge-' + statusColor(o.status, isDispute) }, statusText(o.status, isDispute))
    ));
    c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs12' }, 'Amount'), h('span', { className: 'fs13' }, fmt(o.amount) + ' ' + (o.coin_type || ''))));
    c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs12' }, 'Price'), h('span', { className: 'fs13' }, fmt(o.price) + ' ' + (o.currency || ''))));
    c.appendChild(h('div', { className: 'row between' }, h('span', { className: 'clr-txt2 fs11' }, '#' + (o.order_id || o.uid || '')), h('span', { className: 'fs11 clr-txt3' }, fmtDate(o.created_at))));
    return c;
  }

  function renderOrderDetails(app) {
    if (!isLoggedIn()) {
      requireLogin('orderDetails', S.viewArgs);
      return;
    }
    var a = S.viewArgs;
    if (!S.orderDetails || S.orderDetails._uid !== a.uid) {
      loadOrderDetails(a.uid);
      renderHeader(app, 'Order Details', true);
      var ct = h('div', { className: 'content' });
      ct.appendChild(renderLoading());
      app.appendChild(ct);
      return;
    }
    renderHeader(app, 'Order Details', true);
    var ct = h('div', { className: 'content' });
    if (S.loading) { ct.appendChild(renderLoading()); app.appendChild(ct); return; }
    var d = S.orderDetails;
    if (!d || !d.order) { ct.appendChild(renderEmpty('No details')); app.appendChild(ct); return; }
    var o = d.order;
    var isBuy = d.user_type === 1;
    var subSeg = h('div', { className: 'seg mb12' });
    var subTab = S.viewArgs.subTab || 0;
    subSeg.appendChild(h('button', { className: subTab === 0 ? 'active' : '', onclick: function () { S.viewArgs.subTab = 0; render(); } }, 'Details'));
    subSeg.appendChild(h('button', { className: subTab === 1 ? 'active' : '', onclick: function () { S.viewArgs.subTab = 1; render(); } }, 'Conversation'));
    ct.appendChild(subSeg);
    if (subTab === 1) { renderChat(ct, d); app.appendChild(ct); return; }
    ct.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Order number'), h('span', { className: 'fs13' }, o.order_id || o.uid || '')));
    ct.appendChild(h('div', { className: 'row between mb8' }, h('span', { className: 'clr-txt2 fs13' }, 'Time Created'), h('span', { className: 'fs13' }, fmtDate(o.created_at))));
    ct.appendChild(h('div', { className: 'card mb12' },
      h('div', { className: 'fw600 mb8' }, 'Confirm order info'),
      h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Amount'), h('span', { className: 'fw600' }, fmt(o.amount) + ' ' + (o.coin_type || ''))),
      h('div', { className: 'row between' }, h('span', { className: 'clr-txt2 fs13' }, 'Price'), h('span', { className: 'fw600 clr-acc' }, fmtPrice(o.price, o.currency)))
    ));
    if (o.status === 1 && d.due_minute > 0) {
      var endMs = Date.now() + d.due_minute * 1000;
      var cdDiv = h('div', { className: 'row between mb12' }, h('span', { className: 'clr-txt2 fs13' }, 'Time Left'), h('span', { className: 'countdown', id: 'orderCountdown' }, ''));
      ct.appendChild(cdDiv);
      var intv = setInterval(function () {
        var rem = Math.max(0, endMs - Date.now());
        var s = Math.floor(rem / 1000);
        var m = Math.floor(s / 60);
        var hr = Math.floor(m / 60);
        var dv = Math.floor(hr / 24);
        var el = document.getElementById('orderCountdown');
        if (el) el.textContent = twoDigit(dv) + ' : ' + twoDigit(hr % 24) + ' : ' + twoDigit(m % 60) + ' : ' + twoDigit(s % 60);
        if (rem <= 0) clearInterval(intv);
      }, 1000);
      S.countdownIntervals.push(intv);
    }
    if (d.dispute) {
      var dtxt = '';
      if (d.dispute.status === 1) { dtxt = o.status === 7 ? 'Released By Admin' : o.status === 6 ? 'Refunded By Admin' : 'Disputed'; }
      else { dtxt = (d.who_dispute === 'seller' ? 'Seller' : 'Buyer') + ' created dispute against order'; }
      ct.appendChild(h('div', { className: 'card mb12', style: 'border-color:var(--amber)' }, h('div', { className: 'fw600 clr-amber' }, dtxt)));
    } else {
      if ([0, 3, 4].indexOf(o.status) >= 0) {
        ct.appendChild(h('div', { className: 'card mb12', style: 'border-color:' + (o.status === 3 ? 'var(--green)' : 'var(--red)') },
          h('div', { className: 'fw600', style: 'color:' + (o.status === 3 ? 'var(--green)' : 'var(--red)') }, statusText(o.status))
        ));
      }
      if (o.status === 1 && isBuy && d.payment_method) {
        ct.appendChild(renderPaymentView(d.payment_method, o));
      }
      if (o.status === 2 && !isBuy) {
        if (d.payment_method) ct.appendChild(renderPaymentView(d.payment_method, o));
      }
      var btns = h('div', { className: 'row gap8 mt12', style: 'flex-wrap:wrap' });
      if (o.status === 1 && isBuy) { btns.appendChild(h('button', { className: 'btn btn-sm bg-red clr-white', onclick: function () { showCancelModal(o.uid); } }, 'Cancel')); }
      if (o.status === 2 && !isBuy) { btns.appendChild(h('button', { className: 'btn btn-sm bg-green clr-white', onclick: function () { releaseOrder(o.uid); } }, 'Release')); }
      if (o.status === 2) { btns.appendChild(h('button', { className: 'btn btn-sm bg-amber clr-white', onclick: function () { showDisputeModal(o.uid); } }, 'Dispute')); }
      if (btns.children.length) ct.appendChild(btns);
      if (o.status === 3) { ct.appendChild(renderReviewView(o, isBuy)); }
    }
    app.appendChild(ct);
  }

  function renderPaymentView(payInfo, order) {
    var c = h('div', { className: 'card mb12' });
    c.appendChild(h('div', { className: 'bg-pri clr-white p8 mb12', style: 'border-radius:6px;font-size:13px' }, 'Transfer the fund to the seller account provided below'));
    if (payInfo.bank_form) {
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Method Name'), h('span', { className: 'fs13 fw600' }, payInfo.bank_form.title || '')));
    }
    if (payInfo.bank) {
      for (var slug in payInfo.bank) {
        if (payInfo.bank.hasOwnProperty(slug)) {
          var fieldObj = payInfo.bank[slug];
          var title = fieldObj && fieldObj.title ? fieldObj.title : slug;
          var value = fieldObj && fieldObj.value !== undefined ? fieldObj.value : '';
          c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, title), h('span', { className: 'fs13' }, value)));
        }
      }
    }
    if (order.status === 1) {
      c.appendChild(h('button', { className: 'btn btn-pri btn-block mt8', onclick: function () { payOrder(order.uid); } }, 'Pay and notify seller'));
    }
    return c;
  }

  function renderReviewView(o, isBuy) {
    var c = h('div', { className: 'card mb12' });
    c.appendChild(h('div', { className: 'fw600 mb8' }, 'Submit Review'));
    c.appendChild(h('textarea', { className: 'input mb8', placeholder: 'Write your review', id: 'reviewText' }));
    var segDiv = h('div', { className: 'seg mb8' });
    segDiv.appendChild(h('button', { className: 'active', id: 'revPositive', onclick: function () { this.className = 'active'; document.getElementById('revNegative').className = ''; } }, 'Positive'));
    segDiv.appendChild(h('button', { id: 'revNegative', onclick: function () { this.className = 'active'; document.getElementById('revPositive').className = ''; } }, 'Negative'));
    c.appendChild(segDiv);
    c.appendChild(h('button', { className: 'btn btn-pri btn-block', onclick: function () {
      var txt = document.getElementById('reviewText').value;
      if (!txt) { toast('Write your review', true); return; }
      var isPos = document.getElementById('revPositive').className === 'active';
      submitFeedback(o.uid, txt, isPos ? 1 : 0);
    } }, 'Submit Review'));
    return c;
  }

  function renderChat(ct, d) {
    var msgs = d.chat_messages || [];
    var currentUserId = S.p2pUserId;
    if (msgs.length === 0) { ct.appendChild(renderEmpty('Your messages will appear here')); return; }
    var chatArea = h('div', { className: 'chat-area' });
    msgs.forEach(function (m) {
      var isSent = m.sender_id === currentUserId;
      var row = h('div', { className: 'chat-msg-row ' + (isSent ? 'sent' : 'received') });
      var bubbleContent = m.message || '';
      if (m.file) {
        bubbleContent += '\n[File]';
      }
      row.appendChild(h('div', { className: 'chat-bubble ' + (isSent ? 'sent' : 'received') }, bubbleContent));
      chatArea.appendChild(row);
    });
    ct.appendChild(chatArea);
    var msgRow = h('div', { className: 'row gap8' });
    var msgInput = h('input', { className: 'input flex1', placeholder: 'Write Message', id: 'chatMsgInput' });
    msgRow.appendChild(msgInput);
    msgRow.appendChild(h('button', { className: 'btn btn-sm btn-pri', onclick: function () {
      var txt = document.getElementById('chatMsgInput').value;
      if (!txt) { toast('Message can not be empty', true); return; }
      sendMessage(d.order ? d.order.uid : '', txt);
    } }, 'Send'));
    ct.appendChild(msgRow);
  }

  function renderLogin(app) {
    renderHeader(app, 'P2P Trading', false);
    var ct = h('div', { className: 'content' });
    ct.appendChild(h('div', { className: 'ta-c mb24' },
      h('img', { src: 'assets/mimilogo.png', alt: 'Mimi logo', style: 'width:72px;height:72px;object-fit:contain;margin-bottom:12px' }),
      h('div', { className: 'fw600 fs18' }, 'P2P Trading'),
      h('div', { className: 'clr-txt2 fs13 mt4' }, 'Sign in to continue')
    ));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Email'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'email', placeholder: 'Enter your email', id: 'loginEmail', autocomplete: 'email' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Password'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'password', placeholder: 'Enter your password', id: 'loginPassword', autocomplete: 'current-password' }));
    ct.appendChild(h('button', { className: 'btn btn-pri btn-block mb12', onclick: function () { doLogin(); } }, 'Sign In'));
    ct.appendChild(h('div', { className: 'ta-c' }, h('a', { onclick: function () { go('forgotPassword'); }, className: 'clr-pri fs13' }, 'Forgot Password?')));
    ct.appendChild(h('div', { className: 'divider mt12 mb12' }));
    ct.appendChild(h('div', { className: 'ta-c' },
      h('span', { className: 'clr-txt2 fs13' }, 'Don\'t have an account? '),
      h('a', { onclick: function () { go('register'); }, className: 'clr-pri' }, 'Sign Up')
    ));
    app.appendChild(ct);
  }

  function doLogin() {
    var email = document.getElementById('loginEmail').value.trim();
    var pass = document.getElementById('loginPassword').value;
    if (!email) { toast('Enter your email', true); return; }
    if (!pass) { toast('Enter your password', true); return; }
    S.loading = true;
    toast('Signing in...');
    apiPost('/api/sign-in', { email: email, password: pass }).then(function (r) {
      S.loading = false;
      var token = r.access_token || null;
      var user = r.user || null;
      if (r.success && token) {
        S.p2pToken = token;
        S.p2pUser = user;
        S.p2pUserId = user ? (user.id || user.uid || null) : null;
        S.isP2PLoggedIn = true;
        localStorage.setItem('p2p_token', token);
        if (user) localStorage.setItem('p2p_user', JSON.stringify(user));
        toast('Login successful');
        var pending = S.pendingAuthView;
        S.pendingAuthView = null;
        S.view = pending ? pending.view : 'main';
        S.viewArgs = pending ? pending.args : {};
        S.history = [];
        loadHomeData();
        connectWebSocket();
        render();
      } else {
        toast(r.message || 'Login failed', true);
      }
    }).catch(function (e) {
      S.loading = false;
      toast('Login failed: ' + e.message, true);
    });
  }

  function renderForgotPassword(app) {
    renderHeader(app, 'Forgot Password', true);
    var ct = h('div', { className: 'content' });
    ct.appendChild(h('div', { className: 'ta-c mb24' },
      h('img', { src: 'assets/mimilogo.png', alt: 'Mimi logo', style: 'width:72px;height:72px;object-fit:contain;margin-bottom:12px' }),
      h('div', { className: 'fw600 fs18' }, 'Reset Password'),
      h('div', { className: 'clr-txt2 fs13 mt4' }, 'Enter your email to reset password')
    ));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Email'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'email', placeholder: 'Enter your email', id: 'forgotEmail' }));
    ct.appendChild(h('button', { className: 'btn btn-pri btn-block mb12', onclick: function () { doForgotPassword(); } }, 'Send Reset Link'));
    ct.appendChild(h('div', { className: 'ta-c' }, h('a', { onclick: function () { go('login'); }, className: 'clr-pri fs13' }, 'Back to Sign In')));
    app.appendChild(ct);
  }

  function doForgotPassword() {
    var email = document.getElementById('forgotEmail').value.trim();
    if (!email) { toast('Enter your email', true); return; }
    S.loading = true;
    apiPost('/api/forgot-password', { email: email }).then(function (r) {
      S.loading = false;
      if (r.success || r.message) {
        toast(r.message || 'Reset link sent to your email');
        go('login');
        render();
      } else {
        toast(r.message || 'Failed to send reset link', true);
      }
    }).catch(function (e) {
      S.loading = false;
      toast('Request failed: ' + e.message, true);
    });
  }

  function renderRegister(app) {
    renderHeader(app, 'Create Account', true);
    var ct = h('div', { className: 'content' });
    ct.appendChild(h('div', { className: 'ta-c mb24' },
      h('img', { src: 'assets/mimilogo.png', alt: 'Mimi logo', style: 'width:72px;height:72px;object-fit:contain;margin-bottom:12px' }),
      h('div', { className: 'fw600 fs18' }, 'Create Account'),
      h('div', { className: 'clr-txt2 fs13 mt4' }, 'Sign up to start P2P trading')
    ));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'First Name'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'text', placeholder: 'Enter first name', id: 'regFirstName' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Last Name'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'text', placeholder: 'Enter last name', id: 'regLastName' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Email'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'email', placeholder: 'Enter your email', id: 'regEmail' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Password'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'password', placeholder: 'Create a password (min 6 chars)', id: 'regPassword' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Confirm Password'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'password', placeholder: 'Confirm your password', id: 'regPasswordConfirm' }));
    ct.appendChild(h('button', { className: 'btn btn-pri btn-block mb12', onclick: function () { doRegister(); } }, 'Sign Up'));
    ct.appendChild(h('div', { className: 'ta-c' },
      h('span', { className: 'clr-txt2 fs13' }, 'Already have an account? '),
      h('a', { onclick: function () { go('login'); }, className: 'clr-pri' }, 'Sign In')
    ));
    app.appendChild(ct);
  }

  function doRegister() {
    var fn = document.getElementById('regFirstName').value.trim();
    var ln = document.getElementById('regLastName').value.trim();
    var email = document.getElementById('regEmail').value.trim();
    var pass = document.getElementById('regPassword').value;
    var passConfirm = document.getElementById('regPasswordConfirm').value;
    if (!fn) { toast('Enter first name', true); return; }
    if (!email) { toast('Enter your email', true); return; }
    if (!pass) { toast('Create a password', true); return; }
    if (pass.length < 6) { toast('Password must be at least 6 characters', true); return; }
    if (pass !== passConfirm) { toast('Passwords do not match', true); return; }
    S.loading = true;
    toast('Creating account...');
    var body = { first_name: fn, last_name: ln, email: email, password: pass, password_confirmation: passConfirm };
    apiPost('/api/sign-up', body).then(function (r) {
      S.loading = false;
      if (r.success || (r.message && r.message.toLowerCase().indexOf('success') >= 0)) {
        toast('Registration successful! Please sign in.');
        S.view = 'login';
        render();
      } else {
        var errMsg = r.message || 'Registration failed';
        if (r.errors) {
          var errArr = [];
          for (var k in r.errors) { if (r.errors[k]) errArr = errArr.concat(r.errors[k]); }
          if (errArr.length) errMsg = errArr.join('. ');
        }
        toast(errMsg, true);
      }
    }).catch(function (e) {
      S.loading = false;
      toast('Registration failed: ' + e.message, true);
    });
  }

  function p2pLogout() {
    localStorage.removeItem('p2p_token');
    localStorage.removeItem('p2p_user');
    S.p2pToken = null;
    S.p2pUser = null;
    S.p2pUserId = null;
    S.isP2PLoggedIn = false;
    S.profile = null;
    S.settings = null;
    if (WS) { try { WS.close(); } catch (e) {} WS = null; }
    toast('Logged out');
    S.view = 'main';
    S.viewArgs = {};
    S.history = [];
    render();
    loadHomeData();
  }

  function checkP2PAuth() {
    var token = localStorage.getItem('p2p_token');
    if (token) {
      S.p2pToken = token;
      S.isP2PLoggedIn = true;
      var userStr = localStorage.getItem('p2p_user');
      if (userStr) {
        try {
          S.p2pUser = JSON.parse(userStr);
          S.p2pUserId = S.p2pUser ? (S.p2pUser.id || S.p2pUser.uid || null) : null;
        } catch (e) {}
      }
      return true;
    }
    return false;
  }

  function getPaymentMethodDisplayName(pm) {
    if (!pm) return 'Method';
    if (pm.bank_form && pm.bank_form.title) return pm.bank_form.title;
    var bankName = '';
    if (pm.bank) {
      for (var slug in pm.bank) {
        if (pm.bank.hasOwnProperty(slug)) {
          var f = pm.bank[slug];
          if (f && f.value && !bankName) bankName = f.value;
        }
      }
    }
    return bankName || 'Payment Method';
  }

  function renderUserCenter(ct) {
    if (!S.isP2PLoggedIn && !S.p2pToken) {
      var loginCard = h('div', { className: 'card ta-c p16' });
      loginCard.appendChild(h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }));
      loginCard.appendChild(h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'));
      loginCard.appendChild(h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your profile and P2P features'));
      loginCard.appendChild(h('button', { className: 'btn btn-pri mb8', onclick: function () { go('login'); } }, 'Login'));
      loginCard.appendChild(h('button', { className: 'btn btn-out', onclick: function () { go('register'); } }, 'Create Account'));
      ct.appendChild(loginCard);
      return;
    }
    if (!S.profile) { loadUserCenter(); ct.appendChild(renderLoading()); return; }
    var p = S.profile;
    var topCard = h('div', { className: 'card mb12' });
    var uRow = h('div', { className: 'row between mb12' });
    uRow.appendChild(h('div', { className: 'row gap8' },
      h('div', { className: 'avatar' }, p.user ? ((p.user.nick_name || p.user.first_name || 'U').charAt(0).toUpperCase()) : '?'),
      h('div', { className: 'col' },
        h('span', { className: 'fw600' }, p.user ? (p.user.nick_name || p.user.first_name) : 'User'),
        p.user && p.user.first_name ? h('span', { className: 'fs11 clr-txt2' }, p.user.first_name + ' ' + (p.user.last_name || '')) : null
      )
    ));
    uRow.appendChild(h('span', { className: 'fs12 clr-txt2' }, (p.user_register_at || 0) + ' days ago'));
    topCard.appendChild(uRow);
    var statGrid = h('div', { className: 'stat-grid' });
    [
      { v: p.total_trade || 0, l: 'Total Trades' },
      { v: (p.completion_rate_30d || 0) + '%', l: '30d Completion' },
      { v: p.positive || 0, l: 'Positive' },
      { v: (p.positive_feedback || 0) + '%', l: 'Feedback %' },
      { v: p.negative || 0, l: 'Negative' },
      { v: (p.first_order_at || 0) + 'd', l: 'First Order' }
    ].forEach(function (s) {
      statGrid.appendChild(h('div', { className: 'stat-item' },
        h('div', { className: 'stat-value' }, s.v),
        h('div', { className: 'stat-label' }, s.l)
      ));
    });
    topCard.appendChild(statGrid);
    topCard.appendChild(h('button', { className: 'btn btn-out btn-block', onclick: function () { p2pLogout(); } }, 'Logout'));
    ct.appendChild(topCard);
    var pmCard = h('div', { className: 'card mb12' });
    var pmHeader = h('div', { className: 'row between mb8' });
    pmHeader.appendChild(h('span', { className: 'fw600' }, 'P2P Payment Methods'));
    pmHeader.appendChild(h('button', { className: 'btn btn-sm btn-pri', onclick: function () { go('addPayment'); } }, 'Add'));
    pmCard.appendChild(pmHeader);
    if (S.paymentMethods.length === 0) { pmCard.appendChild(h('div', { className: 'clr-txt3 fs13' }, 'No payment methods')); }
    S.paymentMethods.forEach(function (pm) {
      var pmRow = h('div', { className: 'row between p8', style: 'background:var(--bg2);border-radius:8px;margin-bottom:6px' });
      var pmName = getPaymentMethodDisplayName(pm);
      pmRow.appendChild(h('span', { className: 'fs13' }, pmName));
      var actions = h('div', { className: 'row gap4' });
      actions.appendChild(h('button', { className: 'btn btn-sm', onclick: function () { go('addPayment', { edit: pm }); } }, '\u270E'));
      actions.appendChild(h('button', { className: 'btn btn-sm clr-red', onclick: function () { deletePaymentMethod(pm.id); } }, '\u2716'));
      pmRow.appendChild(actions);
      pmCard.appendChild(pmRow);
    });
    ct.appendChild(pmCard);
    var fbSeg = h('div', { className: 'seg mb12' });
    var fbFilter = S.feedBackList._fbFilter || 0;
    fbSeg.appendChild(h('button', { className: fbFilter === 0 ? 'active' : '', onclick: function () { S.feedBackList._fbFilter = 0; render(); } }, 'All'));
    fbSeg.appendChild(h('button', { className: fbFilter === 1 ? 'active' : '', onclick: function () { S.feedBackList._fbFilter = 1; render(); } }, 'Positive'));
    fbSeg.appendChild(h('button', { className: fbFilter === 2 ? 'active' : '', onclick: function () { S.feedBackList._fbFilter = 2; render(); } }, 'Negative'));
    ct.appendChild(fbSeg);
    var filtered = S.feedBackList;
    if (fbFilter === 1) filtered = S.feedBackList.filter(function (f) { return f.feedback_type === 1; });
    if (fbFilter === 2) filtered = S.feedBackList.filter(function (f) { return f.feedback_type !== 1; });
    filtered.forEach(function (fb) {
      ct.appendChild(h('div', { className: 'card mb8' },
        h('div', { className: 'fs13 mb4' }, fb.feedback || ''),
        h('div', { className: 'row between' },
          h('span', { className: 'fs11 clr-txt2' }, fb.user_name || ''),
          h('span', { className: 'badge badge-' + (fb.feedback_type === 1 ? 'green' : 'red') }, fb.feedback_type === 1 ? 'Positive' : 'Negative')
        )
      ));
    });
  }

  function renderAddPayment(app) {
    var isEdit = S.viewArgs.edit || null;
    renderHeader(app, isEdit ? 'Edit payment method' : 'Add payment method', true);
    var ct = h('div', { className: 'content' });
    if (S.adminPaymentMethods.length === 0) { loadAdminPaymentMethods(); ct.appendChild(renderLoading()); app.appendChild(ct); return; }
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Select Payment Method'));
    var pmSel = h('select', { className: 'input mb12', id: 'payMethodTypeSel' });
    pmSel.appendChild(h('option', { value: '' }, 'Select'));
    S.adminPaymentMethods.forEach(function (pm) { pmSel.appendChild(h('option', { value: pm.uid }, pm.name)); });
    ct.appendChild(pmSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Account Name'));
    var nameVal = '';
    if (isEdit && isEdit.bank) {
      for (var slug in isEdit.bank) {
        if (isEdit.bank.hasOwnProperty(slug)) {
          var f = isEdit.bank[slug];
          if (f && f.title && (f.title.toLowerCase().indexOf('name') >= 0 || f.title.toLowerCase().indexOf('account') >= 0)) {
            nameVal = f.value || '';
            break;
          }
        }
      }
    }
    ct.appendChild(h('input', { className: 'input mb12', placeholder: 'Enter account name', id: 'payNameInput', value: nameVal }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Access'));
    var accessSel = h('select', { className: 'input mb12', id: 'payAccessSel' });
    accessSel.appendChild(h('option', { value: '1' }, 'Public'));
    accessSel.appendChild(h('option', { value: '0' }, 'Private'));
    if (isEdit && isEdit.access !== undefined) accessSel.value = String(isEdit.access);
    ct.appendChild(accessSel);
    var dynamicDiv = h('div', { id: 'payDynamicFields' });
    ct.appendChild(dynamicDiv);
    pmSel.addEventListener('change', function () { renderPaymentFields(dynamicDiv, this.value, isEdit); });
    if (isEdit && isEdit.form_id) {
      var matchingAdmin = S.adminPaymentMethods.find(function (apm) { return apm.bank_form && apm.bank_form.id === isEdit.form_id; });
      if (matchingAdmin) {
        pmSel.value = matchingAdmin.uid;
        renderPaymentFields(dynamicDiv, matchingAdmin.uid, isEdit);
      }
    }
    ct.appendChild(h('button', { className: 'btn btn-pri btn-block mt16', onclick: function () { savePaymentMethod(isEdit); } }, isEdit ? 'Update' : 'Save'));
    app.appendChild(ct);
  }

  function renderPaymentFields(div, pmUid, isEdit) {
    div.innerHTML = '';
    var pm = S.adminPaymentMethods.find(function (p) { return p.uid === pmUid; });
    if (!pm || !pm.bank_form || !pm.bank_form.fields) return;
    pm.bank_form.fields.forEach(function (field) {
      if (field.slug === 'account_name' || field.slug === 'username') return;
      var val = '';
      if (isEdit && isEdit.bank && isEdit.bank[field.slug]) {
        val = isEdit.bank[field.slug].value || '';
      }
      div.appendChild(h('label', { className: 'fw600 fs13 mb4' }, field.title + (field.required ? ' *' : '')));
      var inp = h('input', { className: 'input mb12', placeholder: 'Enter ' + field.title, id: 'payField_' + field.slug, value: val });
      if (field.data_type === 'number') inp.setAttribute('type', 'number');
      div.appendChild(inp);
    });
  }

  function savePaymentMethod(editExisting) {
    var pmUid = document.getElementById('payMethodTypeSel').value;
    var username = document.getElementById('payNameInput').value;
    var access = document.getElementById('payAccessSel').value;
    if (!pmUid) { toast('Select payment method', true); return; }
    if (!username) { toast('Input account name', true); return; }
    var fd = new FormData();
    fd.append('payment_uid', pmUid);
    fd.append('username', username);
    fd.append('access', access);
    var pm = S.adminPaymentMethods.find(function (p) { return p.uid === pmUid; });
    if (pm && pm.bank_form && pm.bank_form.fields) {
      fd.append('form_id', pm.bank_form.id);
      pm.bank_form.fields.forEach(function (field) {
        if (field.slug === 'account_name' || field.slug === 'username') return;
        var el = document.getElementById('payField_' + field.slug);
        if (el) fd.append(field.slug, el.value);
      });
    }
    apiPostFD('/api/p2p/payment-method', fd).then(function (r) {
      if (r.success) {
        toast('Saved');
        goBack();
        loadPaymentMethods();
      } else {
        toast(r.message || 'Failed to save. The API may have changed.', true);
      }
    }).catch(function (e) {
      toast('Save failed: ' + e.message, true);
    });
  }

  function renderWallet(ct) {
    if (!S.isP2PLoggedIn) {
      ct.appendChild(h('div', { className: 'card ta-c p16' },
        h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }),
        h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'),
        h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your wallet'),
        h('button', { className: 'btn btn-pri', onclick: function () { go('login'); } }, 'Login')
      ));
      return;
    }
    if (S.wallets.length === 0) { loadWallets(); ct.appendChild(renderLoading()); return; }
    S.wallets.forEach(function (w) {
      var c = h('div', { className: 'card' });
      c.appendChild(h('div', { className: 'row between mb8' },
        h('div', { className: 'row gap8' },
          w.coin_icon ? h('img', { src: w.coin_icon, style: 'width:24px;height:24px;border-radius:50%' }) : null,
          h('span', { className: 'fw600 fs16' }, w.coin_type || w.name || '')
        ),
        h('span', { className: 'fs16 fw700' }, fmt(w.balance))
      ));
      c.appendChild(h('button', { className: 'btn btn-sm btn-out', onclick: function () { showTransferModal(w); } }, 'Transfer'));
      ct.appendChild(c);
    });
  }

  function showTransferModal(w) {
    var ol = h('div', { className: 'modal-overlay', onclick: function (e) { if (e.target === ol) ol.remove(); } });
    var sh = h('div', { className: 'modal-sheet' });
    sh.appendChild(h('h3', { className: 'fw600 mb16' }, 'Transfer - ' + w.coin_type));
    sh.appendChild(h('div', { className: 'fw600 mb4' }, 'Current Balance: ' + fmt(w.balance)));
    var segDiv = h('div', { className: 'seg mb12' });
    segDiv.appendChild(h('button', { className: 'active', id: 'trToP2P', onclick: function () { this.className = 'active'; document.getElementById('trFromP2P').className = ''; } }, 'Exchange \u2192 P2P'));
    segDiv.appendChild(h('button', { id: 'trFromP2P', onclick: function () { this.className = 'active'; document.getElementById('trToP2P').className = ''; } }, 'P2P \u2192 Exchange'));
    sh.appendChild(segDiv);
    sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Amount'));
    var amtInput = h('input', { className: 'input mb12', type: 'number', placeholder: 'Enter amount' });
    sh.appendChild(amtInput);
    sh.appendChild(h('button', { className: 'btn btn-pri btn-block', onclick: function () {
      var amt = parseFloat(amtInput.value);
      if (!amt || amt <= 0) { toast('Enter valid amount', true); return; }
      var type = document.getElementById('trToP2P').className === 'active' ? 1 : 2;
      transferBalance(w.coin_type, amt, type, function () { ol.remove(); loadWallets(); render(); });
    } }, 'Confirm'));
    ol.appendChild(sh);
    document.body.appendChild(ol);
  }

  function renderMyAds(ct) {
    if (!S.isP2PLoggedIn) {
      ct.appendChild(h('div', { className: 'card ta-c p16' },
        h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }),
        h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'),
        h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your Ads'),
        h('button', { className: 'btn btn-pri', onclick: function () { go('login'); } }, 'Login')
      ));
      return;
    }
    var seg = h('div', { className: 'seg mb12' });
    seg.appendChild(h('button', { className: 'buy-tab' + (S.myAdsTxType === 1 ? ' active' : ''), onclick: function () { S.myAdsTxType = 1; loadMyAdsList(false); render(); } }, 'Buy Ads'));
    seg.appendChild(h('button', { className: S.myAdsTxType === 2 ? 'active' : '', onclick: function () { S.myAdsTxType = 2; loadMyAdsList(false); render(); } }, 'Sell Ads'));
    var createBtn = h('button', { className: 'btn btn-sm btn-pri', onclick: function () { go('createAds', { isBuy: S.myAdsTxType === 1 }); } }, 'Create');
    var row = h('div', { className: 'row between mb12' });
    row.appendChild(seg);
    row.appendChild(createBtn);
    ct.appendChild(row);
    if (S.myAdsList.length === 0 && !S.loading) { loadMyAdsList(false); return; }
    if (S.loading && S.myAdsList.length === 0) { ct.appendChild(renderLoading()); return; }
    if (S.myAdsList.length === 0) { ct.appendChild(renderEmpty('No ads')); }
    S.myAdsList.forEach(function (ad) {
      var c = h('div', { className: 'card' });
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Price'), h('span', { className: 'fw600 fs13' }, fmtPrice(ad.price, ad.currency))));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Available'), h('span', { className: 'fs13' }, fmtCrypto(ad.available) + ' ' + (ad.coin_type || ''))));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Limit'), h('span', { className: 'fs13' }, fmt(ad.minimum_trade_size) + ' - ' + fmt(ad.maximum_trade_size) + ' ' + (ad.currency || ''))));
      c.appendChild(h('div', { className: 'row between mb8' }, h('span', { className: 'clr-txt2 fs11' }, fmtDate(ad.created_at)), h('span', { className: 'badge badge-' + (ad.status === 1 ? 'green' : 'red') }, ad.status === 1 ? 'Active' : 'Inactive')));
      var btnRow = h('div', { className: 'row gap8' });
      btnRow.appendChild(h('button', { className: 'btn btn-sm btn-out', onclick: function () { go('createAds', { isBuy: S.myAdsTxType === 1, edit: ad }); } }, 'Edit'));
      btnRow.appendChild(h('button', { className: 'btn btn-sm clr-red', onclick: function () { adsDelete(ad.uid, S.myAdsTxType); } }, 'Delete'));
      c.appendChild(btnRow);
      ct.appendChild(c);
    });
    if (S.myAdsHasMore) {
      ct.appendChild(h('button', { className: 'btn btn-block btn-out mt12', onclick: function () { loadMyAdsList(true); } }, 'Load More'));
    }
  }

  function renderCreateAds(app) {
    var isEdit = S.viewArgs.edit || null;
    var isBuy = S.viewArgs.isBuy !== false;
    renderHeader(app, isEdit ? 'Edit Advertisement' : 'Create Advertisement', true);
    var ct = h('div', { className: 'content' });
    var data = S.createAdsData;
    var step = data._step || 1;
    var steps = h('div', { className: 'step-indicator' });
    [1, 2, 3].forEach(function (s) {
      steps.appendChild(h('div', { className: 'step' + (s <= step ? ' active' : '') }));
    });
    ct.appendChild(steps);
    if (step === 1) renderCreateStep1(ct, isBuy, isEdit);
    else if (step === 2) renderCreateStep2(ct, isBuy, isEdit);
    else if (step === 3) renderCreateStep3(ct, isBuy, isEdit);
    app.appendChild(ct);
  }

  function renderCreateStep1(ct, isBuy, isEdit) {
    ct.appendChild(h('div', { className: 'seg mb12' },
      h('button', { className: 'buy-tab' + (isBuy ? ' active' : ''), onclick: function () { S.viewArgs.isBuy = true; render(); } }, 'I want to Buy'),
      h('button', { className: !isBuy ? 'active' : '', onclick: function () { S.viewArgs.isBuy = false; render(); } }, 'I want to Sell')
    ));
    if (!S.createAdsSettings) { loadCreateAdsSettings(); ct.appendChild(renderLoading()); return; }
    var cs = S.createAdsSettings;
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Select Asset'));
    var assetSel = h('select', { className: 'input mb12', id: 'createAssetSel' });
    if (cs.assets && cs.assets.length) { cs.assets.forEach(function (a) { assetSel.appendChild(h('option', { value: a.coin_type }, a.coin_type)); }); }
    if (S.createAdsData.coin_type) assetSel.value = S.createAdsData.coin_type;
    ct.appendChild(assetSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Select Fiat Currency'));
    var curSel = h('select', { className: 'input mb12', id: 'createCurrencySel' });
    if (cs.currency && cs.currency.length) { cs.currency.forEach(function (c) { curSel.appendChild(h('option', { value: c.currency_code || c.code || c.name }, c.name || c.currency_code)); }); }
    if (S.createAdsData.fiat_type) curSel.value = S.createAdsData.fiat_type;
    ct.appendChild(curSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Your Price'));
    var priceInput = h('input', { className: 'input mb4', type: 'number', placeholder: 'Enter price', id: 'createPriceInput', value: S.createAdsData.price || '' });
    ct.appendChild(priceInput);
    ct.appendChild(h('div', { className: 'form-hint mb12' }, 'Lowest Order Price will be shown'));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Price Type'));
    var ptFixed = S.createAdsData.price_type !== 2;
    var ptSeg = h('div', { className: 'seg mb12' },
      h('button', { className: ptFixed ? 'active' : '', id: 'ptFixed', onclick: function () { this.className = 'active'; document.getElementById('ptFloating').className = ''; } }, 'Fixed'),
      h('button', { className: !ptFixed ? 'active' : '', id: 'ptFloating', onclick: function () { this.className = 'active'; document.getElementById('ptFixed').className = ''; } }, 'Floating')
    );
    ct.appendChild(ptSeg);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Amount'));
    var amountInput = h('input', { className: 'input mb12', type: 'number', placeholder: 'Enter amount', id: 'createAmountInput', value: S.createAdsData.amount || '' });
    ct.appendChild(amountInput);
    ct.appendChild(h('button', { className: 'btn btn-pri btn-block', onclick: function () {
      S.createAdsData._step = 2;
      S.createAdsData.ads_type = isBuy ? 1 : 2;
      S.createAdsData.coin_type = assetSel.value;
      S.createAdsData.fiat_type = curSel.value;
      S.createAdsData.price = priceInput.value;
      S.createAdsData.price_type = document.getElementById('ptFixed').className === 'active' ? 1 : 2;
      S.createAdsData.amount = amountInput.value;
      render();
    } }, 'Next'));
  }

  function renderCreateStep2(ct, isBuy, isEdit) {
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Order Limit - Minimum'));
    var minInput = h('input', { className: 'input mb12', type: 'number', placeholder: 'Minimum', value: S.createAdsData.minimum_trade_size || '' });
    ct.appendChild(minInput);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Order Limit - Maximum'));
    var maxInput = h('input', { className: 'input mb12', type: 'number', placeholder: 'Maximum', value: S.createAdsData.maximum_trade_size || '' });
    ct.appendChild(maxInput);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Payment Time Limit'));
    var timeSel = h('select', { className: 'input mb12', id: 'createTimeLimitSel' });
    if (S.createAdsSettings && S.createAdsSettings.payment_time && S.createAdsSettings.payment_time.length) {
      S.createAdsSettings.payment_time.forEach(function (pt) { timeSel.appendChild(h('option', { value: pt.uid }, pt.time + ' minutes')); });
    } else {
      [15, 30, 45, 60].forEach(function (m) { timeSel.appendChild(h('option', { value: String(m) }, m + ' minutes')); });
    }
    if (S.createAdsData.time_limit) timeSel.value = S.createAdsData.time_limit;
    ct.appendChild(timeSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Select payment methods'));
    if (S.adminPaymentMethods.length === 0) loadAdminPaymentMethods();
    var pmCheckDiv = h('div', { className: 'mb12', id: 'createPmCheckboxes' });
    S.adminPaymentMethods.forEach(function (pm) {
      var lbl = h('label', { className: 'row gap8 mb4', style: 'cursor:pointer' });
      var chk = h('input', { type: 'checkbox', value: pm.uid, name: 'createPmCheck' });
      lbl.appendChild(chk);
      lbl.appendChild(h('span', { className: 'fs13' }, pm.name));
      pmCheckDiv.appendChild(lbl);
    });
    ct.appendChild(pmCheckDiv);
    ct.appendChild(h('div', { className: 'row gap8' },
      h('button', { className: 'btn btn-out flex1', onclick: function () { S.createAdsData._step = 1; render(); } }, 'Previous'),
      h('button', { className: 'btn btn-pri flex1', onclick: function () {
        S.createAdsData.minimum_trade_size = minInput.value;
        S.createAdsData.maximum_trade_size = maxInput.value;
        S.createAdsData.time_limit = timeSel.value;
        var sel = [];
        var checks = document.querySelectorAll('input[name="createPmCheck"]:checked');
        checks.forEach(function (c) { sel.push(c.value); });
        if (sel.length === 0) { toast('Select at least one payment method', true); return; }
        S.createAdsData.payment_methods = sel.join(',');
        S.createAdsData._step = 3;
        render();
      } }, 'Next')
    ));
  }

  function renderCreateStep3(ct, isBuy, isEdit) {
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Terms [Optional]'));
    ct.appendChild(h('textarea', { className: 'input mb12', placeholder: 'Terms will be displayed on the counterparty', id: 'createTermsInput' }, S.createAdsData.terms || ''));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Auto-reply [Optional]'));
    ct.appendChild(h('textarea', { className: 'input mb12', placeholder: 'Auto-reply will be displayed on the counterparty', id: 'createAutoInput' }, S.createAdsData.auto_reply || ''));
    ct.appendChild(h('div', { className: 'fw600 mb8' }, 'Counterparty Conditions'));
    ct.appendChild(h('label', { className: 'fs13 clr-txt2 mb4' }, 'Register days'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'number', placeholder: '0', id: 'createRegDays', value: S.createAdsData.register_days || '' }));
    ct.appendChild(h('label', { className: 'fs13 clr-txt2 mb4' }, 'Holding more than'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'number', placeholder: '0', id: 'createHolding', value: S.createAdsData.coin_holding || '' }));
    if (S.createAdsSettings && S.createAdsSettings.country && S.createAdsSettings.country.length) {
      ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Country'));
      var ctryDiv = h('div', { className: 'mb12', id: 'createCountryChecks' });
      S.createAdsSettings.country.forEach(function (c) {
        var lbl = h('label', { className: 'row gap8 mb4', style: 'cursor:pointer' });
        var chk = h('input', { type: 'checkbox', value: c.key, name: 'createCtryCheck' });
        lbl.appendChild(chk);
        lbl.appendChild(h('span', { className: 'fs13' }, c.value || c.key));
        ctryDiv.appendChild(lbl);
      });
      ct.appendChild(ctryDiv);
    }
    ct.appendChild(h('div', { className: 'row gap8 mt8' },
      h('button', { className: 'btn btn-out flex1', onclick: function () { S.createAdsData._step = 2; render(); } }, 'Previous'),
      h('button', { className: 'btn btn-pri flex1', onclick: function () {
        S.createAdsData.terms = document.getElementById('createTermsInput').value;
        S.createAdsData.auto_reply = document.getElementById('createAutoInput').value;
        S.createAdsData.register_days = document.getElementById('createRegDays').value;
        S.createAdsData.coin_holding = document.getElementById('createHolding').value;
        var ctryChecks = document.querySelectorAll('input[name="createCtryCheck"]:checked');
        var countries = [];
        ctryChecks.forEach(function (c) { countries.push(c.value); });
        S.createAdsData.countrys = countries.join(',');
        saveAds(isEdit);
      } }, isEdit ? 'Update' : 'Create')
    ));
  }

  function renderGiftCard(ct) {
    var subSeg = h('div', { className: 'seg mb12', style: 'flex-wrap:wrap' });
    ['Home', 'Orders', 'My Gift Cards', 'My Gift Card Ads'].forEach(function (name, i) {
      subSeg.appendChild(h('button', { className: S.gcSubTab === i ? 'active' : '', onclick: function () { S.gcSubTab = i; render(); } }, name));
    });
    ct.appendChild(subSeg);
    switch (S.gcSubTab) {
      case 0: renderGCHome(ct); break;
      case 1: renderGCOrders(ct); break;
      case 2: renderGCCardList(ct); break;
      case 3: renderGCUserAds(ct); break;
    }
  }

  function renderGCHome(ct) {
    if (S.gcAdsList.length === 0 && !S.loading) { loadGCAdsList(false); ct.appendChild(renderLoading()); return; }
    if (S.loading && S.gcAdsList.length === 0) { ct.appendChild(renderLoading()); return; }
    if (S.gcAdsList.length === 0) { ct.appendChild(renderEmpty('No gift card ads')); return; }
    S.gcAdsList.forEach(function (ad) {
      var c = h('div', { className: 'card' });
      c.appendChild(h('div', { className: 'row between mb4' },
        h('span', { className: 'fw600' }, ad.gift_card ? (ad.gift_card.name || ad.gift_card.coin_type || 'Gift Card') : 'Gift Card'),
        h('span', { className: 'badge badge-' + gcStatusColor(ad.status) }, gcStatusText(ad.status))
      ));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Price'), h('span', { className: 'fw600' }, fmtPrice(ad.price, ad.currency_type))));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Amount'), h('span', {}, ad.amount || '')));
      if (ad.user) {
        c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Seller'), h('span', { className: 'fs13' }, ad.user.nick_name || ad.user.first_name || 'User')));
      }
      c.appendChild(h('button', {
        className: 'btn btn-sm btn-buy',
        onclick: function () {
          var args = { uid: ad.uid };
          if (!isLoggedIn()) requireLogin('gcAdDetails', args);
          else go('gcAdDetails', args);
        }
      }, 'Buy Gift Card'));
      ct.appendChild(c);
    });
    if (S.gcAdsList.length > 0) {
      ct.appendChild(h('button', { className: 'btn btn-block btn-out mt12', onclick: function () { loadGCAdsList(true); } }, 'Load More'));
    }
  }

  function renderGCOrders(ct) {
    if (!isLoggedIn()) {
      ct.appendChild(h('div', { className: 'card ta-c p16' },
        h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }),
        h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'),
        h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your gift card orders'),
        h('button', { className: 'btn btn-pri', onclick: function () { go('login'); } }, 'Login')
      ));
      return;
    }
    if (S.gcOrdersList.length === 0 && !S.loading) { loadGCOrders(); }
    if (S.loading) { ct.appendChild(renderLoading()); return; }
    if (S.gcOrdersList.length === 0) { ct.appendChild(renderEmpty('No gift card orders')); return; }
    S.gcOrdersList.forEach(function (o) {
      var c = h('div', { className: 'card', onclick: function () { go('gcOrderDetails', { uid: o.uid || o.id }); } });
      c.appendChild(h('div', { className: 'row between mb4' },
        h('span', { className: 'fw600' }, '#' + (o.order_id || o.uid || o.id || '')),
        h('span', { className: 'badge badge-' + gcStatusColor(o.status) }, gcStatusText(o.status))
      ));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Price'), h('span', {}, fmtPrice(o.price, o.currency_type))));
      c.appendChild(h('div', { className: 'row between' }, h('span', { className: 'clr-txt2 fs13' }, 'Amount'), h('span', {}, fmt(o.amount))));
      ct.appendChild(c);
    });
  }

  function renderGCCardList(ct) {
    if (!isLoggedIn()) {
      ct.appendChild(h('div', { className: 'card ta-c p16' },
        h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }),
        h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'),
        h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your gift cards'),
        h('button', { className: 'btn btn-pri', onclick: function () { go('login'); } }, 'Login')
      ));
      return;
    }
    if (S.gcCardList.length === 0 && !S.loading) { loadGCCardList(); }
    if (S.loading) { ct.appendChild(renderLoading()); return; }
    if (S.gcCardList.length === 0) { ct.appendChild(renderEmpty('No gift cards')); return; }
    S.gcCardList.forEach(function (gc) {
      var c = h('div', { className: 'card' });
      var name = 'Gift Card';
      if (gc.banner) name = gc.banner.title || gc.banner.sub_title || name;
      else if (gc.gift_card) name = gc.gift_card.name || name;
      c.appendChild(h('div', { className: 'row between mb4' },
        h('span', { className: 'fw600' }, name),
        h('span', { className: 'badge badge-' + gcStatusColor(gc.status) }, gcStatusText(gc.status))
      ));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Amount'), h('span', {}, fmt(gc.amount) + ' ' + (gc.coin_type || ''))));
      if (gc.redeem_code) {
        c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Redeem Code'), h('span', { className: 'fs13 fw600' }, gc.redeem_code)));
      }
      ct.appendChild(c);
    });
  }

  function renderGCUserAds(ct) {
    if (!isLoggedIn()) {
      ct.appendChild(h('div', { className: 'card ta-c p16' },
        h('img', { className: 'login-required-logo', src: 'assets/mimilogo.png', alt: 'Mimi Money logo', width: '72', height: '72' }),
        h('div', { className: 'fw600 fs16 mb8' }, 'Login Required'),
        h('div', { className: 'clr-txt2 fs13 mb16' }, 'Please login to access your gift card ads'),
        h('button', { className: 'btn btn-pri', onclick: function () { go('login'); } }, 'Login')
      ));
      return;
    }
    ct.appendChild(h('button', { className: 'btn btn-sm btn-pri mb12', onclick: function () { go('gcCreateAd'); } }, 'Create Ad'));
    if (S.gcUserAdList.length === 0 && !S.loading) { loadGCUserAds(); }
    if (S.loading) { ct.appendChild(renderLoading()); return; }
    if (S.gcUserAdList.length === 0) { ct.appendChild(renderEmpty('No gift card ads')); return; }
    S.gcUserAdList.forEach(function (ad) {
      var c = h('div', { className: 'card' });
      var gcName = ad.gift_card ? (ad.gift_card.name || ad.gift_card.coin_type || 'Gift Card') : 'Ad';
      c.appendChild(h('div', { className: 'row between mb4' },
        h('span', { className: 'fw600' }, gcName),
        h('span', { className: 'badge badge-' + gcStatusColor(ad.status) }, gcStatusText(ad.status))
      ));
      c.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Price'), h('span', {}, fmtPrice(ad.price, ad.currency_type))));
      c.appendChild(h('div', { className: 'row gap8 mt8' },
        h('button', { className: 'btn btn-sm btn-out', onclick: function () { go('gcCreateAd', { edit: ad }); } }, 'Edit'),
        h('button', { className: 'btn btn-sm clr-red', onclick: function () { deleteGCAd(ad.id || ad.uid); } }, 'Delete')
      ));
      ct.appendChild(c);
    });
  }

  function renderGCAdDetails(app) {
    if (!isLoggedIn()) {
      requireLogin('gcAdDetails', S.viewArgs);
      return;
    }
    renderHeader(app, 'Gift Card Details', true);
    var ct = h('div', { className: 'content' });
    if (S.loading) { ct.appendChild(renderLoading()); app.appendChild(ct); return; }
    ct.appendChild(h('div', { className: 'card mb12' },
      h('div', { className: 'fw600 mb8' }, 'Gift Card Ad'),
      h('div', { className: 'clr-txt2 fs13' }, 'Click below to purchase this gift card')
    ));
    ct.appendChild(h('button', { className: 'btn btn-buy btn-block mt8', onclick: function () { placeGCOrder(S.viewArgs.uid); } }, 'Buy Gift Card'));
    app.appendChild(ct);
  }

  function renderGCOrderDetails(app) {
    if (!isLoggedIn()) {
      requireLogin('gcOrderDetails', S.viewArgs);
      return;
    }
    renderHeader(app, 'Gift Card Order Details', true);
    var ct = h('div', { className: 'content' });
    if (S.loading) { ct.appendChild(renderLoading()); app.appendChild(ct); return; }
    var d = S.gcOrderDetails;
    if (!d || !d.order) { ct.appendChild(renderEmpty('No details')); app.appendChild(ct); return; }
    var o = d.order;
    ct.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Order ID'), h('span', { className: 'fw600' }, o.order_id || o.uid || '')));
    ct.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Amount'), h('span', { className: 'fw600' }, fmt(o.amount))));
    ct.appendChild(h('div', { className: 'row between mb4' }, h('span', { className: 'clr-txt2 fs13' }, 'Price'), h('span', { className: 'fw600' }, fmtPrice(o.price, o.currency_type))));
    ct.appendChild(h('div', { className: 'row between mb8' }, h('span', { className: 'clr-txt2 fs13' }, 'Status'), h('span', { className: 'badge badge-' + gcStatusColor(o.status) }, gcStatusText(o.status))));
    var btns = h('div', { className: 'row gap8 mt8', style: 'flex-wrap:wrap' });
    if (o.status === 4) { btns.appendChild(h('button', { className: 'btn btn-sm bg-green clr-white', onclick: function () { gcOrderPayNow(o.id); } }, 'Pay & Notify')); }
    if (o.status === 4 && d.user_type === 2) { btns.appendChild(h('button', { className: 'btn btn-sm btn-pri', onclick: function () { gcOrderConfirm(o.id); } }, 'Confirm Payment')); }
    if (btns.children.length) ct.appendChild(btns);
    app.appendChild(ct);
  }

  function renderGCCreateAd(app) {
    if (!isLoggedIn()) {
      requireLogin('gcCreateAd', S.viewArgs);
      return;
    }
    var isEdit = S.viewArgs.edit || null;
    renderHeader(app, isEdit ? 'Edit Gift Card Ad' : 'Create Gift Card Ad', true);
    var ct = h('div', { className: 'content' });
    if (!S.gcPageData) { loadGCPageData(); ct.appendChild(renderLoading()); app.appendChild(ct); return; }
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Gift Card'));
    var gcSel = h('select', { className: 'input mb12', id: 'gcAdGiftCardSel' });
    gcSel.appendChild(h('option', { value: '' }, 'Select'));
    if (S.gcCardList.length === 0) {
      apiGet('/api/p2p/get-gift-card-p2p', { limit: 100, page: 1 }).then(function (r) {
        if (r.success && r.data && r.data.data) {
          r.data.data.forEach(function (gc) { gcSel.appendChild(h('option', { value: gc.id }, gc.coin_type + ' - ' + fmt(gc.amount))); });
        }
      });
    } else {
      S.gcCardList.forEach(function (gc) { gcSel.appendChild(h('option', { value: gc.id }, gc.coin_type + ' - ' + fmt(gc.amount))); });
    }
    if (isEdit && isEdit.gift_card_id) gcSel.value = String(isEdit.gift_card_id);
    ct.appendChild(gcSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Currency Type'));
    var curTypeSel = h('select', { className: 'input mb12', id: 'gcAdCurrencyType' });
    if (S.gcPageData.currency && S.gcPageData.currency.length) {
      S.gcPageData.currency.forEach(function (c) { curTypeSel.appendChild(h('option', { value: c.currency_code || c.code || c.name }, c.name || c.currency_code)); });
    } else {
      curTypeSel.appendChild(h('option', { value: 'USD' }, 'USD'));
    }
    ct.appendChild(curTypeSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Payment Currency Type'));
    var payCurSel = h('select', { className: 'input mb12', id: 'gcAdPayCurrencyType' });
    payCurSel.appendChild(h('option', { value: '1' }, 'Fiat'));
    payCurSel.appendChild(h('option', { value: '2' }, 'Crypto'));
    ct.appendChild(payCurSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Price'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'number', placeholder: 'Enter Price', id: 'gcAdPrice', value: isEdit ? isEdit.price || '' : '' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Status'));
    var statusSel = h('select', { className: 'input mb12', id: 'gcAdStatus' });
    statusSel.appendChild(h('option', { value: '1' }, 'Active'));
    statusSel.appendChild(h('option', { value: '0' }, 'Inactive'));
    ct.appendChild(statusSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Time Limit (minutes)'));
    ct.appendChild(h('input', { className: 'input mb12', type: 'number', placeholder: 'Time Limit', id: 'gcAdTimeLimit', value: isEdit ? isEdit.time_limit || '' : '' }));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Terms And Condition'));
    ct.appendChild(h('textarea', { className: 'input mb12', placeholder: 'Enter Terms And Condition', id: 'gcAdTerms' }, isEdit ? isEdit.terms_condition || '' : ''));
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Payment Method'));
    var pmSel = h('select', { className: 'input mb12', id: 'gcAdPayMethod' });
    pmSel.appendChild(h('option', { value: '' }, 'Select'));
    S.adminPaymentMethods.forEach(function (pm) { pmSel.appendChild(h('option', { value: pm.uid }, pm.name)); });
    ct.appendChild(pmSel);
    ct.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Country'));
    var ctrySel = h('select', { className: 'input mb12', id: 'gcAdCountry' });
    ctrySel.appendChild(h('option', { value: '' }, 'All'));
    if (S.gcPageData.country && S.gcPageData.country.length) {
      S.gcPageData.country.forEach(function (c) { ctrySel.appendChild(h('option', { value: c.key }, c.value || c.key)); });
    }
    ct.appendChild(ctrySel);
    ct.appendChild(h('button', { className: 'btn btn-pri btn-block', onclick: function () { saveGCAd(isEdit); } }, isEdit ? 'Update' : 'Create'));
    app.appendChild(ct);
  }

  function renderProfile(app) {
    var userId = S.viewArgs.userId;
    renderHeader(app, 'User Profile', true);
    var ct = h('div', { className: 'content' });
    if (userId) {
      if (!S.viewArgs.profileData) { loadProfileDetails(userId); ct.appendChild(renderLoading()); app.appendChild(ct); return; }
      var p = S.viewArgs.profileData;
      ct.appendChild(h('div', { className: 'card mb12' },
        h('div', { className: 'row gap8 mb12' },
          h('div', { className: 'avatar' }, p.user ? ((p.user.nick_name || p.user.first_name || 'U').charAt(0).toUpperCase()) : '?'),
          h('div', { className: 'col' },
            h('span', { className: 'fw600' }, p.user ? (p.user.nick_name || p.user.first_name) : 'User'),
            p.user && p.user.first_name ? h('span', { className: 'fs11 clr-txt2' }, p.user.first_name + ' ' + (p.user.last_name || '')) : null
          )
        )
      ));
      var statGrid = h('div', { className: 'stat-grid' });
      [
        { v: p.total_trade || 0, l: 'Total Trades' },
        { v: (p.completion_rate_30d || 0) + '%', l: '30d Completion' },
        { v: p.positive || 0, l: 'Positive' },
        { v: (p.positive_feedback || 0) + '%', l: 'Feedback %' },
        { v: p.negative || 0, l: 'Negative' },
        { v: (p.user_register_at || 0) + 'd', l: 'Registered' }
      ].forEach(function (s) {
        statGrid.appendChild(h('div', { className: 'stat-item' },
          h('div', { className: 'stat-value' }, s.v),
          h('div', { className: 'stat-label' }, s.l)
        ));
      });
      ct.appendChild(statGrid);
      if (p.feedback_list && p.feedback_list.length) {
        ct.appendChild(h('div', { className: 'fw600 mb8' }, 'Feedback'));
        p.feedback_list.forEach(function (fb) {
          ct.appendChild(h('div', { className: 'card mb8' },
            h('div', { className: 'fs13 mb4' }, fb.feedback || ''),
            h('div', { className: 'row between' },
              h('span', { className: 'fs11 clr-txt2' }, fb.user_name || ''),
              h('span', { className: 'badge badge-' + (fb.feedback_type === 1 ? 'green' : 'red') }, fb.feedback_type === 1 ? 'Positive' : 'Negative')
            )
          ));
        });
      }
    } else {
      ct.appendChild(renderEmpty('No profile data'));
    }
    app.appendChild(ct);
  }

  function loadProfileDetails(userId) {
    S.loading = true;
    apiGet('/api/p2p/user-profile', { id: userId }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.viewArgs.profileData = r.data;
      } else {
        toast(r.message || 'Failed to load profile', true);
      }
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function renderLoading() {
    return h('div', { className: 'loading-overlay' },
      h('div', { className: 'spinner' }),
      h('span', {}, 'Loading...')
    );
  }

  function renderEmpty(msg) {
    return h('div', { className: 'empty-state' }, msg);
  }

  function loadHomeData() {
    if (S.settings) { loadAdsList(false); render(); return; }
    if (S.loading) return;
    S.loading = true;
    render();
    apiGet('/api/p2p/ads-market-setting').then(function (r) {
      if (r.success && r.data) {
        S.settings = r.data;
        S.loading = false;
        loadAdsList(false);
        render();
      } else {
        S.loading = false;
        toast(r.message || 'Failed to load settings', true);
        render();
      }
    }).catch(function () { S.loading = false; render(); });
  }

  function loadAdsList(loadMore) {
    if (!loadMore) { S.homePage = 0; S.homeHasMore = true; S.adsList = []; }
    S.loading = true;
    S.homePage++;
    var currencyCode = 'all';
    if (S.homeCurrency > 0 && S.settings && S.settings.currency && S.settings.currency[S.homeCurrency - 1]) {
      currencyCode = S.settings.currency[S.homeCurrency - 1].currency_code || 'all';
    }
    var coinType = '';
    if (S.homeCoin > 0 && S.settings && S.settings.assets && S.settings.assets[S.homeCoin - 1]) {
      coinType = S.settings.assets[S.homeCoin - 1].coin_type || '';
    }
    if (!coinType && S.settings && S.settings.assets && S.settings.assets.length) {
      coinType = S.settings.assets[0].coin_type || '';
      S.homeCoin = 1;
    }
    var countryCode = 'all';
    if (S.homeCountry > 0 && S.settings && S.settings.country && S.settings.country[S.homeCountry - 1]) {
      countryCode = S.settings.country[S.homeCountry - 1].key || 'all';
    }
    var pmUid = 'all';
    if (S.homePayment > 0 && S.settings && S.settings.payment_method && S.settings.payment_method[S.homePayment - 1]) {
      pmUid = S.settings.payment_method[S.homePayment - 1].uid || 'all';
    }
    apiPost('/api/p2p/ads-filter-change', {
      per_page: 20, page: S.homePage, type: S.homeTxType, amount: S.homeAmount || 0,
      currency: currencyCode,
      coin: coinType,
      country: countryCode,
      payment_method: pmUid
    }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        var lr = r.data;
        S.homeHasMore = !!lr.next_page_url;
        if (lr.data) S.adsList = S.adsList.concat(lr.data);
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadAdsDetails(uid, adsType) {
    S.loading = true;
    S.adsDetails = null;
    apiGet('/api/p2p/ads-details', { uid: uid, ads_type: adsType }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.adsDetails = r.data;
        S.adsDetails._uid = uid;
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadAdsBalance(uid, coinType, adsType) {
    apiPost('/api/p2p/ads-available-balance', { uid: uid, coin_type: coinType, type: 3 - adsType }).then(function (r) {
      if (r.success && r.data) {
        var el = document.getElementById('adsRcvInput');
        if (el && r.data.balance) el.value = fmt(r.data.balance, 8);
        var el2 = document.getElementById('adsPayInput');
        if (el2 && S.adsDetails && S.adsDetails.price) el2.value = fmt(r.data.balance * S.adsDetails.price, 2);
      }
      else toast(r.message || 'Failed', true);
    });
  }

  function placeOrder(uid, adsType, payId, price, amount) {
    toast('Placing order...');
    apiPost('/api/p2p/place-p2p-order', {
      ads_type: adsType,
      ads_id: uid,
      payment_id: payId,
      price: price || null,
      amount: amount || null
    }).then(function (r) {
      if (r.success) {
        toast('Order placed');
        if (r.data && r.data.uid) go('orderDetails', { uid: r.data.uid });
        else goBack();
      } else toast(r.message || 'Failed', true);
    });
  }

  function loadOrdersList(loadMore) {
    if (!loadMore) { S.ordersPage = 0; S.ordersHasMore = true; S.ordersList = []; }
    S.loading = true;
    S.ordersPage++;
    apiGet('/api/p2p/my-p2p-order', { per_page: 20, page: S.ordersPage, ads_status: S.ordersStatus || 'all', coin: 'all', from_date: '', to_date: '', type: 'all' }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        var lr = r.data;
        S.ordersHasMore = !!lr.next_page_url;
        if (lr.data) S.ordersList = S.ordersList.concat(lr.data);
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadDisputeList(loadMore) {
    if (!loadMore) { S.ordersPage = 0; S.ordersHasMore = true; S.disputeList = []; }
    S.loading = true;
    S.ordersPage++;
    apiGet('/api/p2p/my-p2p-dispute', { per_page: 20, page: S.ordersPage, coin: 'all', type: 'all' }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        var lr = r.data;
        S.ordersHasMore = !!lr.next_page_url;
        if (lr.data) S.disputeList = S.disputeList.concat(lr.data);
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadOrderDetails(uid) {
    S.loading = true;
    S.orderDetails = null;
    apiPost('/api/p2p/get-p2p-order-details', { order_uid: uid }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.orderDetails = r.data;
        S.orderDetails._uid = uid;
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function sendMessage(orderUid, text) {
    apiPostFD('/api/p2p/send-message', { order_uid: orderUid, message: text }).then(function (r) {
      if (r.success) { toast('Sent'); loadOrderDetails(orderUid); }
      else toast(r.message || 'Failed', true);
    });
  }

  function payOrder(uid) {
    toast('Marking as paid...');
    apiPostFD('/api/p2p/payment-p2p-order', { trade_id: uid }).then(function (r) {
      if (r.success) { toast('Payment marked'); loadOrderDetails(uid); }
      else toast(r.message || 'Failed', true);
    });
  }

  function releaseOrder(uid) {
    if (!confirm('Do you want to release the escrow?')) return;
    apiPost('/api/p2p/release-p2p-order', { trade_id: uid }).then(function (r) {
      if (r.success) { toast('Released'); loadOrderDetails(uid); }
      else toast(r.message || 'Failed', true);
    });
  }

  function submitFeedback(uid, review, type) {
    apiPost('/api/p2p/order-feedback', { order_uid: uid, feedback: review, feedback_type: type }).then(function (r) {
      if (r.success) { toast('Review submitted'); loadOrderDetails(uid); }
      else toast(r.message || 'Failed', true);
    });
  }

  function showCancelModal(uid) {
    var ol = h('div', { className: 'modal-overlay', onclick: function (e) { if (e.target === ol) ol.remove(); } });
    var sh = h('div', { className: 'modal-sheet' });
    sh.appendChild(h('h3', { className: 'fw600 mb12' }, 'Cancel Order'));
    sh.appendChild(h('label', { className: 'fs13 mb4' }, 'Reason to cancel the order'));
    var reasonInput = h('textarea', { className: 'input mb12', placeholder: 'Write Your Reason' });
    sh.appendChild(reasonInput);
    sh.appendChild(h('button', { className: 'btn btn-pri btn-block', onclick: function () {
      var reason = reasonInput.value;
      if (!reason) { toast('Reason for the cancellation', true); return; }
      ol.remove();
      apiPost('/api/p2p/cancel-p2p-order', { order_uid: uid, reason: reason }).then(function (r) {
        if (r.success) { toast('Canceled'); loadOrderDetails(uid); } else toast(r.message || 'Failed', true);
      });
    } }, 'Confirm'));
    ol.appendChild(sh);
    document.body.appendChild(ol);
  }

  function showDisputeModal(uid) {
    var ol = h('div', { className: 'modal-overlay', onclick: function (e) { if (e.target === ol) ol.remove(); } });
    var sh = h('div', { className: 'modal-sheet' });
    sh.appendChild(h('h3', { className: 'fw600 mb12' }, 'Dispute Order'));
    sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Dispute Subject'));
    var subInput = h('input', { className: 'input mb12', placeholder: 'Write Subject' });
    sh.appendChild(subInput);
    sh.appendChild(h('label', { className: 'fw600 fs13 mb4' }, 'Dispute Description'));
    var descInput = h('textarea', { className: 'input mb12', placeholder: 'Write the Reason to dispute the order' });
    sh.appendChild(descInput);
    sh.appendChild(h('button', { className: 'btn btn-pri btn-block', onclick: function () {
      var sub = subInput.value;
      var desc = descInput.value;
      if (!sub) { toast('Write the dispute subject', true); return; }
      if (!desc) { toast('Write the dispute description', true); return; }
      ol.remove();
      apiPostFD('/api/p2p/dispute-process', { order_uid: uid, reason_subject: sub, reason_details: desc }).then(function (r) {
        if (r.success) { toast('Dispute filed'); loadOrderDetails(uid); } else toast(r.message || 'Failed', true);
      });
    } }, 'Confirm'));
    ol.appendChild(sh);
    document.body.appendChild(ol);
  }

  function loadUserCenter() {
    S.loading = true;
    apiGet('/api/p2p/user-center').then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.profile = r.data;
        S.feedBackList = r.data.feedback_list || [];
        S.feedBackList._fbFilter = 0;
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
    loadPaymentMethods();
  }

  function loadPaymentMethods() {
    apiGet('/api/p2p/payment-method', { per_page: 50, page: 1 }).then(function (r) {
      if (r.success && r.data && r.data.data) S.paymentMethods = r.data.data;
      else S.paymentMethods = [];
      render();
    });
  }

  function loadAdminPaymentMethods() {
    apiGet('/api/p2p/admin-payment-method').then(function (r) {
      if (r.success && r.data) S.adminPaymentMethods = r.data;
      else S.adminPaymentMethods = [];
      render();
    });
  }

  function deletePaymentMethod(id) {
    if (!confirm('Do you want to delete this payment info?')) return;
    apiPost('/api/p2p/payment-method', { delete: id }).then(function (r) {
      if (r.success) {
        toast('Deleted');
        loadPaymentMethods();
        render();
      } else {
        toast(r.message || 'Failed to delete. The API may have changed.', true);
      }
    });
  }

  function loadWallets() {
    apiGet('/api/p2p/wallets', { per_page: 50, page: 1 }).then(function (r) {
      if (r.success && r.data) { S.wallets = r.data.data || r.data.wallets || []; }
      else S.wallets = [];
      render();
    });
  }

  function transferBalance(coinType, amount, type, cb) {
    apiPost('/api/p2p/transfer-wallet-balance', { coin: coinType, amount: amount, type: type }).then(function (r) {
      if (r.success) { toast('Transfer successful'); if (cb) cb(); }
      else toast(r.message || 'Failed', true);
    });
  }

  function loadMyAdsList(loadMore) {
    if (!loadMore) { S.myAdsPage = 0; S.myAdsHasMore = true; S.myAdsList = []; }
    S.loading = true;
    S.myAdsPage++;
    apiPost('/api/p2p/user-ads-filter', { per_page: 20, page: S.myAdsPage, coin: 'all', ads_status: 'all', type: S.myAdsTxType }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        var lr = r.data;
        S.myAdsHasMore = !!lr.next_page_url;
        if (lr.data) S.myAdsList = S.myAdsList.concat(lr.data);
      } else toast(r.message || 'Failed', true);
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadCreateAdsSettings() {
    apiGet('/api/p2p/ads-create-setting').then(function (r) {
      if (r.success && r.data) {
        S.createAdsSettings = r.data;
        render();
      } else {
        toast(r.message || 'Failed to load create ads settings', true);
      }
    });
  }

  function saveAds(isEdit) {
    var d = S.createAdsData;
    var fd = new FormData();
    fd.append('ads_type', d.ads_type || (S.viewArgs.isBuy !== false ? 1 : 2));
    fd.append('coin_type', d.coin_type || '');
    fd.append('fiat_type', d.fiat_type || '');
    fd.append('price_type', d.price_type || 1);
    fd.append('price', d.price || '');
    fd.append('amount', d.amount || '');
    fd.append('min_limit', d.minimum_trade_size || '');
    fd.append('max_limit', d.maximum_trade_size || '');
    fd.append('time_limit', d.time_limit || '');
    fd.append('payment_methods', d.payment_methods || '');
    fd.append('terms', d.terms || '');
    fd.append('auto_reply', d.auto_reply || '');
    fd.append('register_days', d.register_days || 0);
    fd.append('coin_holding', d.coin_holding || 0);
    fd.append('countrys', d.countrys || '');
    if (isEdit) {
      fd.append('ads_uid', isEdit.uid);
      fd.append('ads_type', isEdit.ads_type || d.ads_type || 1);
      apiPostFD('/api/p2p/ads-edit', fd).then(function (r) {
        if (r.success) { toast('Updated'); S.createAdsData = {}; goBack(); } else toast(r.message || 'Failed', true);
      });
    } else {
      apiPostFD('/api/p2p/ads', fd).then(function (r) {
        if (r.success) { toast('Created'); S.createAdsData = {}; goBack(); } else toast(r.message || 'Failed', true);
      });
    }
  }

  function loadGCPageData() {
    S.loading = true;
    apiGet('/api/p2p/get-gift-card-page-data').then(function (r) {
      S.loading = false;
      if (r.success && r.data) { S.gcPageData = r.data; render(); }
      else toast(r.message || 'Failed', true);
    }).catch(function () { S.loading = false; render(); });
  }

  function loadGCAdsList(loadMore) {
    if (!loadMore) { S.gcListPage = 0; S.gcAdsList = []; }
    S.gcListPage++;
    S.loading = true;
    apiGet('/api/p2p/all-gift-card-ads-list', { limit: 20, page: S.gcListPage, price: S.gcFilterPrice, payment_currency_type: S.gcFilterPayType, currency_type: S.gcFilterCurrencyType, payment_method: S.gcFilterPayMethod, country: S.gcFilterCountry }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        var list = r.data.data || r.data;
        if (Array.isArray(list)) S.gcAdsList = S.gcAdsList.concat(list);
      }
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadGCOrders() {
    S.loading = true;
    apiGet('/api/p2p/get-gift-card-orders', { limit: 20, page: 1, status: S.gcOrdersStatus }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.gcOrdersList = r.data.data || (Array.isArray(r.data) ? r.data : []);
      }
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadGCCardList() {
    S.loading = true;
    apiGet('/api/p2p/get-gift-card-p2p', { limit: 20, page: 1 }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.gcCardList = r.data.data || (Array.isArray(r.data) ? r.data : []);
      }
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadGCUserAds() {
    S.loading = true;
    apiGet('/api/p2p/user-gift-card-ads-list', { limit: 20, page: 1, status: S.gcAdsStatus }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.gcUserAdList = r.data.data || (Array.isArray(r.data) ? r.data : []);
      }
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function placeGCOrder(uid) {
    toast('Placing order...');
    apiPost('/api/p2p/place-gift-card-order', { gift_card_id: uid }).then(function (r) {
      if (r.success) {
        toast('Order placed');
        if (r.data && r.data.uid) go('gcOrderDetails', { uid: r.data.uid });
        else goBack();
      } else toast(r.message || 'Failed', true);
    });
  }

  function gcOrderPayNow(id) {
    apiPostFD('/api/p2p/pay-now-gift-card-order', { gift_card_order_id: id }).then(function (r) {
      if (r.success) { toast('Paid'); loadGCOrderDetails(S.viewArgs.uid); } else toast(r.message || 'Failed', true);
    });
  }

  function gcOrderConfirm(id) {
    apiPost('/api/p2p/payment-confirm-gift-card-order', { gift_card_order_id: id }).then(function (r) {
      if (r.success) { toast('Confirmed'); loadGCOrderDetails(S.viewArgs.uid); } else toast(r.message || 'Failed', true);
    });
  }

  function loadGCOrderDetails(uid) {
    S.loading = true;
    S.gcOrderDetails = null;
    apiGet('/api/p2p/get-gift-card-order', { order_uid: uid }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) S.gcOrderDetails = r.data;
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function loadGCAdDetails(uid) {
    S.loading = true;
    apiGet('/api/p2p/get-gift-card-ads-details-p2p', { uid: uid }).then(function (r) {
      S.loading = false;
      if (r.success && r.data) {
        S.gcAdDetails = r.data;
      }
      render();
    }).catch(function () { S.loading = false; render(); });
  }

  function saveGCAd(isEdit) {
    var fd = new FormData();
    var gcId = document.getElementById('gcAdGiftCardSel').value;
    if (!gcId) { toast('Select a gift card', true); return; }
    fd.append('gift_card_id', gcId);
    var curType = document.getElementById('gcAdCurrencyType');
    if (curType) fd.append('currency_type', curType.value);
    fd.append('payment_currency_type', document.getElementById('gcAdPayCurrencyType').value);
    fd.append('price', document.getElementById('gcAdPrice').value);
    fd.append('status', document.getElementById('gcAdStatus').value);
    fd.append('time_limit', document.getElementById('gcAdTimeLimit').value);
    fd.append('terms_condition', document.getElementById('gcAdTerms').value);
    var pmSel = document.getElementById('gcAdPayMethod');
    if (pmSel && pmSel.value) {
      fd.append('payment_method[]', pmSel.value);
    }
    var ctrySel = document.getElementById('gcAdCountry');
    if (ctrySel && ctrySel.value) {
      fd.append('country[]', ctrySel.value);
    }
    if (isEdit) {
      fd.append('uid', isEdit.uid || isEdit.id);
      apiPostFD('/api/p2p/update-gift-card-adds', fd).then(function (r) {
        if (r.success) { toast('Updated'); goBack(); } else toast(r.message || 'Failed', true);
      });
    } else {
      apiPostFD('/api/p2p/store-gift-card-adds', fd).then(function (r) {
        if (r.success) { toast('Created'); goBack(); } else toast(r.message || 'Failed', true);
      });
    }
  }

  function deleteGCAd(id) {
    if (!confirm('Are you sure you want to proceed?')) return;
    apiPost('/api/p2p/gift-card-delete', { gift_card_id: id }).then(function (r) {
      if (r.success) { toast('Deleted'); loadGCUserAds(); } else toast(r.message || 'Failed', true);
    });
  }

  function adsStatusChange(id, type) {
    apiPost('/api/p2p/ads-status-change', { id: id, type: type }).then(function (r) {
      if (r.success) { toast('Status updated'); loadMyAdsList(false); render(); }
      else toast(r.message || 'Failed', true);
    });
  }

  function adsDelete(uid, type) {
    if (!confirm('Are you sure you want to delete this ad?')) return;
    apiPost('/api/p2p/ads-delete', { uid: uid, ads_type: type }).then(function (r) {
      if (r.success) { toast('Deleted'); loadMyAdsList(false); render(); }
      else toast(r.message || 'Failed', true);
    });
  }

  function getOrderRate(adsType, uid, price, amount) {
    var params = { ads_type: adsType, ads_id: uid };
    if (price) params.price = price;
    if (amount) params.amount = amount;
    return apiPost('/api/p2p/get-p2p-order-rate', params);
  }

  function gcOrderCancel(id, reason) {
    apiPost('/api/p2p/gift-card-order-cancel', { gift_card_order_id: id, reason: reason }).then(function (r) {
      if (r.success) { toast('Canceled'); loadGCOrderDetails(S.viewArgs.uid); } else toast(r.message || 'Failed', true);
    });
  }

  function gcOrderDispute(id, subject, details) {
    apiPostFD('/api/p2p/gift-card-order-dispute', { gift_card_order_id: id, reason_subject: subject, reason_details: details }).then(function (r) {
      if (r.success) { toast('Dispute filed'); loadGCOrderDetails(S.viewArgs.uid); } else toast(r.message || 'Failed', true);
    });
  }

  function connectWebSocket() {
    var wsUrl = window.P2P_WEBSOCKET_URL;
    if (!wsUrl || !S.p2pToken) return;
    var userId = S.p2pUserId;
    if (!userId && S.p2pUser) userId = S.p2pUser.id || S.p2pUser.uid;
    if (!userId) return;
    if (WS_RECONNECT_TIMER) { clearTimeout(WS_RECONNECT_TIMER); WS_RECONNECT_TIMER = null; }
    if (WS && (WS.readyState === WebSocket.OPEN || WS.readyState === WebSocket.CONNECTING)) return;
    if (WS) { try { WS.close(); } catch(e) {} WS = null; }
    try {
      WS = new WebSocket(wsUrl);
      WS.onopen = function () {
        WS_RECONNECT_ATTEMPT = 0;
        try {
          if (WS && WS.readyState === WebSocket.OPEN) {
            WS.send(JSON.stringify({ event: 'p2p-channel', channel: 'p2p-' + userId }));
          }
        } catch (e) {}
      };
      WS.onmessage = function (evt) {
        try {
          var data = JSON.parse(evt.data);
          if (data.event === 'order-updated' && data.order_uid) {
            if (S.view === 'orderDetails' && S.viewArgs.uid === data.order_uid) {
              loadOrderDetails(data.order_uid);
            }
          }
          if (data.event === 'new-message' && data.order_uid) {
            if (S.view === 'orderDetails' && S.viewArgs.uid === data.order_uid) {
              loadOrderDetails(data.order_uid);
            }
          }
        } catch (e) {}
      };
      WS.onclose = function () {
        WS = null;
        if (!S.isP2PLoggedIn || WS_RECONNECT_TIMER) return;
        var delay = Math.min(30000, 2000 * Math.pow(2, WS_RECONNECT_ATTEMPT++));
        WS_RECONNECT_TIMER = setTimeout(function () {
          WS_RECONNECT_TIMER = null;
          if (S.isP2PLoggedIn) connectWebSocket();
        }, delay);
      };
      WS.onerror = function () { if (WS) { try { WS.close(); } catch (e) {} } };
    } catch (e) {
      WS = null;
      if (S.isP2PLoggedIn && !WS_RECONNECT_TIMER) {
        WS_RECONNECT_TIMER = setTimeout(function () { WS_RECONNECT_TIMER = null; connectWebSocket(); }, 5000);
      }
    }
  }

  function init(base) {
    API_BASE = normalizeApiBase(base);
    applyTheme();
    if (window.location.hash) {
      var hash = window.location.hash.slice(1);
      if (hash.startsWith('ads/')) { var parts = hash.split('/'); go('adsDetails', { uid: parts[1], adsType: parseInt(parts[2]) || 1 }); }
      else if (hash.startsWith('order/')) { go('orderDetails', { uid: hash.split('/')[1] }); }
    }
  }

  function normalizeApiBase(base) {
    if (!base) return '';
    try {
      var parsed = new URL(String(base), window.location.origin);
      if (parsed.protocol !== 'https:' && parsed.origin !== window.location.origin) return '';
      return parsed.origin === window.location.origin ? '' : parsed.origin;
    } catch (e) {
      return '';
    }
  }

  function setBase(b) { API_BASE = normalizeApiBase(b); if (!S.settings) loadHomeData(); }
  function setWebSocketUrl(url) { window.P2P_WEBSOCKET_URL = url || ''; }

  window.addEventListener('hashchange', function () {
    var hash = window.location.hash.slice(1);
    if (hash.startsWith('ads/')) { var parts = hash.split('/'); loadAdsDetails(parts[1], parseInt(parts[2]) || 1); go('adsDetails', { uid: parts[1], adsType: parseInt(parts[2]) || 1 }); }
    else if (hash.startsWith('order/')) { loadOrderDetails(hash.split('/')[1]); go('orderDetails', { uid: hash.split('/')[1] }); }
  });

  document.addEventListener('DOMContentLoaded', function () {
    setTimeout(function () {
      var config = typeof P2P_CONFIG !== 'undefined' && P2P_CONFIG ? P2P_CONFIG : {};
      init(config.API_BASE_URL || '');
      if (config) {
        API_BASE = normalizeApiBase(config.API_BASE_URL);
        API_SECRET = '';
        window.P2P_WEBSOCKET_URL = typeof config.WEBSOCKET_URL === 'string' && /^wss:\/\//i.test(config.WEBSOCKET_URL) ? config.WEBSOCKET_URL : '';
      }
      checkP2PAuth();
      if (S.view !== 'adsDetails' && S.view !== 'orderDetails') S.view = 'main';
      render();
      loadHomeData();
      if (S.isP2PLoggedIn) connectWebSocket();
    }, 100);
  });

  return { init: init, render: render, setBase: setBase, setWebSocketUrl: setWebSocketUrl, goBack: goBack, toggleTheme: toggleTheme };
})();
