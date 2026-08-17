# Peers P2P Trading

A dependency-free Node.js server and browser frontend for the Mimi Money P2P
trading experience. The Node server serves the static application and proxies
`/api/*` and `/app/*` requests to the configured upstream API, keeping the API
secret out of browser code.

## Requirements

- Node.js 18 or newer
- Access to the upstream Mimi Money API
- An API secret if the upstream environment requires one

No application database is included or required by this repository. Account,
market, and trade data come from the configured upstream API.

## Run locally

1. Clone the repository and enter it:

   ```bash
   git clone https://github.com/YOUR-ACCOUNT/YOUR-REPOSITORY.git
   cd YOUR-REPOSITORY
   ```

2. Create your local environment file:

   ```bash
   cp .env.example .env
   ```

3. Edit `.env` for your environment. Never commit this file. `API_SECRET` may
   remain empty only when the upstream API does not require it.

4. Load the environment and start the server:

   ```bash
   set -a
   . ./.env
   set +a
   npm start
   ```

5. Open `http://localhost:8080`.

There are no npm dependencies to install. `npm start` invokes the Node.js
runtime directly.

## Validate the checkout

Run the built-in source checks:

```bash
npm test
```

For a quick HTTP smoke test, start the application and request the home page:

```bash
curl --fail http://localhost:8080/
```

## Configuration

Server configuration is provided through environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | Local HTTP port |
| `API_BASE_URL` | `https://mimi.money` | Upstream API origin |
| `API_SECRET` | empty | Secret sent by the server to the upstream API |
| `ALLOWED_ORIGINS` | empty | Comma-separated additional browser origins |
| `MAX_BODY_BYTES` | `1048576` | Maximum proxied request-body size |

Browser-safe settings live in `config.js`. Do not place secrets in that file or
in any other frontend asset; everything served to a browser is public.

## Production deployment

The repository includes two optional deployment configurations:

- `.htaccess` applies Apache security rules and redirects traffic to
  `https://peers.mimi.money`. Change that hostname before deploying under a
  different domain. The file is not used by the local Node.js server.
- `peers-p2p.service` is a sample systemd unit. It assumes the repository is
  installed at `/var/www/peers-p2p-trading` and reads secrets from
  `/etc/peers-p2p.env`. Update the paths and user for your host before enabling
  it.

Example production environment file:

```bash
API_BASE_URL=https://mimi.money
API_SECRET=replace-with-a-real-secret
PORT=8080
ALLOWED_ORIGINS=https://peers.mimi.money
MAX_BODY_BYTES=1048576
```

Protect the environment file so it is readable only by the service account,
then run the application behind a TLS-terminating reverse proxy. Never commit
production credentials, user uploads, logs, database dumps, or runtime data.

## Project layout

```text
assets/              Static images
app.js               Browser application
config.js            Public browser configuration
index.html           Application entry point
server.js            Static server and upstream API proxy
styles.css           Application styles
theme-init.js        Early theme initialization
.htaccess            Optional Apache rules
peers-p2p.service    Optional systemd service template
```

