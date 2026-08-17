# MiMi Money Support

MiMi Money Support is a CodeIgniter 3/PHP customer-support application with an optional OpenClaw gateway and a separate Node.js x402 client for paid Agenticous reports.

This repository contains application code and database structure only. Production database records, uploaded customer files, logs, caches, credentials, installed dependencies, build output, and historical installer/backup archives are intentionally excluded.

## Requirements

- PHP 8.1 or newer with `mysqli`, `mbstring`, `curl`, `json`, `openssl`, and `fileinfo`
- MySQL 5.7+/MariaDB 10.3+
- Apache 2.4 with `mod_rewrite` for an Apache setup, or PHP's built-in server for local development
- Node.js 18.18+ and npm, only when using the optional x402 client

## Local PHP setup

1. Clone the repository and enter it:

   ```bash
   git clone https://github.com/YOUR-ORG/YOUR-REPOSITORY.git
   cd YOUR-REPOSITORY
   ```

2. Create local configuration. Replace all placeholder secrets; never commit `.env.local`.

   ```bash
   cp .env.example .env.local
   set -a
   . ./.env.local
   set +a
   ```

   Generate local random values with `openssl rand -hex 32`. Environment variables must be loaded again in each new shell, or configured in your local web server.

3. Create an empty database and a least-privilege local user. Substitute the same password used in `.env.local`:

   ```sql
   CREATE DATABASE mimi_support CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   CREATE USER 'mimi_support'@'localhost' IDENTIFIED BY 'YOUR_LOCAL_PASSWORD';
   GRANT ALL PRIVILEGES ON mimi_support.* TO 'mimi_support'@'localhost';
   FLUSH PRIVILEGES;
   ```

4. Ensure the runtime directories are writable by your local web-server user:

   ```bash
   chmod -R u+rwX application/cache application/logs uploads
   ```

5. Start the application from the repository root:

   ```bash
   php -S 127.0.0.1:8080
   ```

6. Open <http://127.0.0.1:8080/index.php>. With `MIMI_INSTALLED=no`, the application routes to its installer. Use the database values from `.env.local`, let the migrations create the empty schema, and create a local administrator account.

7. After installation, change `MIMI_INSTALLED=yes`, reload the variables, and restart PHP. Add the MiMi AI conversation table (schema only):

   ```bash
   mysql -h "$MIMI_DB_HOST" -u "$MIMI_DB_USER" -p "$MIMI_DB_NAME" < deployment/mimi-ai-conversations.sql
   ```

The installer writes local configuration values while it runs. Review `git status` before committing and do not commit locally generated secrets.

## Optional x402 client

The x402 service uses a dedicated EVM payer key and makes real paid requests. Use a development wallet with limited funds; never reuse a treasury or personal wallet.

```bash
cd x402-client
cp .env.example .env
npm ci
npm run typecheck
npm test
npm run build
npm start
```

Set `AGENTICOUS_CLIENT_TOKEN` in the PHP environment to the same value as the x402 client's `INTERNAL_TOKEN`. The default service address is `http://127.0.0.1:4411`.

## Optional OpenClaw gateway

The PHP application expects the gateway at `OPENCLAW_URL`. Set `OPENCLAW_TOKEN` and `OPENCLAW_MODEL` in the environment. Files in `openclaw/` provide the support-agent instructions and knowledge-base location. The examples in `deployment/` are production templates and must be adapted to local paths and users before use.

## Validation before pushing

```bash
find application system -name '*.php' -print0 | xargs -0 -n1 php -l
cd x402-client && npm ci && npm run typecheck && npm test && npm run build
```

Before every push, confirm that `git status` contains no `.env`, database dump, upload, log, cache, private key, API token, or generated dependency/build directory.

## Repository layout

- `application/` — CodeIgniter application code, configuration, and migrations
- `system/` — bundled CodeIgniter framework
- `assets/`, `customer/` — browser assets and customer-facing client
- `uploads/` — empty runtime upload structure (customer data is ignored)
- `openclaw/` — support-agent policy and knowledge-base files
- `x402-client/` — optional Node.js/TypeScript paid-request service
- `deployment/` — schema and Apache/systemd examples

See `license.txt` and third-party asset license files for applicable licensing terms.
