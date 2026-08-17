var http = require('http');
var https = require('https');
var fs = require('fs');
var path = require('path');

var API_BASE = (process.env.API_BASE_URL || 'https://mimi.money').replace(/\/+$/, '');
var API_SECRET = process.env.API_SECRET || '';
var PORT = process.env.PORT || 8080;
var WWW = path.resolve(__dirname);
var MAX_BODY_BYTES = parseInt(process.env.MAX_BODY_BYTES || '1048576', 10);
var ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || '')
  .split(',')
  .map(function (origin) { return origin.trim(); })
  .filter(Boolean);
var BLOCKED_STATIC = {
  '/mimilogo.png': true,
  '/server.js': true,
  '/peers-p2p.service': true
};

var MIME = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

if (!API_SECRET) {
  console.warn('API_SECRET is not configured; proxying without userapisecret.');
}

function sameOrigin(req) {
  var origin = req.headers.origin;
  if (!origin) return true;
  var host = req.headers.host;
  if (!host) return false;
  try {
    var parsed = new URL(origin);
    return parsed.host === host || ALLOWED_ORIGINS.indexOf(origin) >= 0;
  } catch (e) {
    return false;
  }
}

function corsHeaders(req) {
  var headers = { vary: 'Origin' };
  var origin = req.headers.origin;
  if (origin && sameOrigin(req)) {
    headers['access-control-allow-origin'] = origin;
    headers['access-control-allow-credentials'] = 'true';
  }
  headers['access-control-allow-methods'] = 'GET,POST,PUT,DELETE,OPTIONS';
  headers['access-control-allow-headers'] = 'Content-Type,Authorization,token,Accept';
  headers['access-control-max-age'] = '86400';
  return headers;
}

function securityHeaders(extra) {
  var headers = {
    'x-content-type-options': 'nosniff',
    'x-frame-options': 'DENY',
    'referrer-policy': 'same-origin',
    'permissions-policy': 'camera=(), microphone=(), geolocation=(), payment=()',
    'content-security-policy': [
      "default-src 'self'",
      "script-src 'self'",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' https: data:",
      "connect-src 'self' wss://api.tradexpro.org",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'"
    ].join('; ')
  };
  if (extra) {
    for (var k in extra) headers[k] = extra[k];
  }
  return headers;
}

function getBody(req) {
  return new Promise(function (resolve, reject) {
    var chunks = [];
    var total = 0;
    req.on('data', function (c) {
      total += c.length;
      if (total > MAX_BODY_BYTES) {
        reject(new Error('Request body too large'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', function () { resolve(Buffer.concat(chunks)); });
    req.on('error', reject);
  });
}

function requestPath(requestUrl) {
  try {
    return new URL(requestUrl, 'http://localhost').pathname;
  } catch (e) {
    return null;
  }
}

function proxy(req, res) {
  if (!sameOrigin(req)) {
    res.writeHead(403, securityHeaders(corsHeaders(req)));
    res.end(JSON.stringify({ success: false, message: 'Origin not allowed' }));
    return;
  }

  var target = API_BASE + req.url;
  var parsed = new URL(target);
  var fwdHeaders = {};
  for (var k in req.headers) {
    var lk = k.toLowerCase();
    if (lk === 'host' || lk === 'origin' || lk === 'referer' || lk === 'connection') continue;
    fwdHeaders[k] = req.headers[k];
  }
  fwdHeaders['host'] = parsed.hostname;
  fwdHeaders['origin'] = API_BASE;
  fwdHeaders['referer'] = API_BASE + '/';
  if (API_SECRET) fwdHeaders['userapisecret'] = API_SECRET;
  fwdHeaders['accept'] = 'application/json';

  getBody(req).then(function (body) {
    var opts = {
      hostname: parsed.hostname,
      port: 443,
      path: parsed.pathname + parsed.search,
      method: req.method,
      headers: fwdHeaders
    };
    if (body.length) opts.headers['content-length'] = body.length;

    var proxyReq = https.request(opts, function (proxyRes) {
      var hdrs = securityHeaders(corsHeaders(req));
      for (var k in proxyRes.headers) {
        var lk = k.toLowerCase();
        if (lk === 'access-control-allow-origin' || lk === 'access-control-allow-headers' || lk === 'access-control-allow-methods' || lk === 'access-control-allow-credentials') continue;
        hdrs[k] = proxyRes.headers[k];
      }
      res.writeHead(proxyRes.statusCode, hdrs);
      proxyRes.pipe(res);
    });
    proxyReq.on('error', function (e) {
      res.writeHead(502, securityHeaders(Object.assign({ 'content-type': 'application/json' }, corsHeaders(req))));
      res.end(JSON.stringify({ success: false, message: 'Proxy error' }));
    });
    if (body.length) proxyReq.write(body);
    proxyReq.end();
  }).catch(function (e) {
    var status = e.message === 'Request body too large' ? 413 : 400;
    res.writeHead(status, securityHeaders(Object.assign({ 'content-type': 'application/json' }, corsHeaders(req))));
    res.end(JSON.stringify({ success: false, message: e.message }));
  });
}

function serveStatic(req, res) {
  var pathname = requestPath(req.url);
  if (pathname === null) { res.writeHead(400, securityHeaders()); res.end('Bad request'); return; }
  if (pathname === '/') pathname = '/index.html';
  if (BLOCKED_STATIC[pathname]) { res.writeHead(404, securityHeaders()); res.end('Not found'); return; }
  var fp = path.resolve(WWW, '.' + pathname);
  if (fp !== WWW && !fp.startsWith(WWW + path.sep)) { res.writeHead(403, securityHeaders()); res.end(); return; }
  if (path.basename(fp).charAt(0) === '.') { res.writeHead(404, securityHeaders()); res.end('Not found'); return; }
  fs.readFile(fp, function (err, data) {
    if (err) { res.writeHead(404, securityHeaders()); res.end('Not found'); return; }
    var ext = path.extname(fp).toLowerCase();
    res.writeHead(200, securityHeaders({ 'content-type': MIME[ext] || 'application/octet-stream' }));
    res.end(data);
  });
}

var server = http.createServer(function (req, res) {
  var pathname = requestPath(req.url);

  if (pathname === null) {
    res.writeHead(400, securityHeaders());
    res.end('Bad request');
    return;
  }

  if (req.method === 'OPTIONS') {
    if (!sameOrigin(req)) {
      res.writeHead(403, securityHeaders(corsHeaders(req)));
      res.end();
      return;
    }
    res.writeHead(204, securityHeaders(corsHeaders(req)));
    res.end();
    return;
  }

  if (pathname.startsWith('/api/') || pathname.startsWith('/app/')) {
    proxy(req, res);
    return;
  }

  serveStatic(req, res);
});

server.listen(PORT, function () {
  console.log('P2P Trading server running at http://localhost:' + PORT);
  console.log('API proxy: /api/* -> ' + API_BASE + '/api/*');
});
