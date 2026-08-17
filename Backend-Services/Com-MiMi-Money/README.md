# MiMi Money server

MiMi Money is a PHP/MySQL API and administration panel with a Node.js/Socket.IO realtime service. This repository package contains application source and database structure only. Production database records, uploaded media, installed Node modules, private keys, and Firebase credentials are intentionally excluded.

## Requirements

- PHP 8.1 or newer with `mysqli`, `curl`, `openssl`, `gd`, `fileinfo`, and `mbstring`
- MySQL 5.7+/8.x or a compatible MariaDB release
- Node.js 18+ and npm
- Apache with `mod_rewrite` for an Apache deployment; PHP's built-in server can be used locally

## Clone and configure

```bash
git clone https://github.com/YOUR-ACCOUNT/YOUR-REPOSITORY.git mimi-money
cd mimi-money
cp .env.example .env
```

Edit `.env` and replace every `replace-with-...` value. Use a unique local database password and generate the socket secret, for example:

```bash
openssl rand -hex 32
```

The application reads environment variables directly; it does not parse `.env` itself. Load the values in each terminal before starting PHP, Node, or database commands:

```bash
set -a
. ./.env
set +a
```

## Create the empty database

Create a local database and least-privileged user. Substitute your own password in both this SQL and `.env`:

```sql
CREATE DATABASE mimi_money CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mimi_money'@'localhost' IDENTIFIED BY 'replace-with-a-local-password';
GRANT ALL PRIVILEGES ON mimi_money.* TO 'mimi_money'@'localhost';
FLUSH PRIVILEGES;
```

Import the schema-only dump:

```bash
mysql -u "$DB_USER" -p "$DB_NAME" < install/dataBase.sql
```

The dump deliberately contains no records. To use the admin panel, create a local administrator after import (replace the example values):

```sql
INSERT INTO wa_admins (username, password, image)
VALUES ('local-admin', MD5('replace-with-a-strong-local-password'), NULL);
```

The legacy application uses MD5 for admin-password compatibility. Keep the local instance private and plan a password-hashing upgrade before exposing a new deployment publicly.

Application settings can be entered through the admin panel or inserted into `wa_settings` for local development. Do not copy production rows into this repository.

## Install and run locally

Install the locked Node dependencies:

```bash
cd node_app
npm ci
cd ..
```

Ensure the runtime folders are writable by the local web process:

```bash
chmod -R u+rwX uploads
```

Start the PHP API from the repository root:

```bash
set -a
. ./.env
set +a
php -S 127.0.0.1:8080 router.php
```

In a second terminal, start Socket.IO:

```bash
set -a
. ./.env
set +a
cd node_app
npm start
```

Open `http://127.0.0.1:8080/admin/login.php`. The Socket.IO health endpoint is `http://127.0.0.1:9001/health` unless `PORT` is changed.

## Optional Firebase notifications

Copy `config/firebase-service-account.example.json` to `config/firebase-service-account.json` and replace every placeholder with credentials from a development-only Firebase project. The real file is ignored by Git. Never commit a service-account JSON file.

## Validation

Run syntax checks before pushing changes:

```bash
find . -path './node_app/node_modules' -prune -o -name '*.php' -print0 | xargs -0 -n1 php -l
node --check node_app/app.js
node --check node_app/db.js
node --check node_app/socketHandler.js
node --check node_app/users.js
```

`node_app/test_features.js` is an integration helper. It requires three disposable test users configured with the `TEST_USER_*` environment variables and must never be run against production.

## Production notes

- Point the web root at this repository and enable Apache rewrite/header modules, or translate `.htaccess` rules for your web server.
- Set `APP_DEBUG=false` and `SOCKET_DEBUG=false`.
- Set explicit `SSL_KEY_PATH` and `SSL_CERT_PATH` values if Node terminates TLS; otherwise terminate TLS at a reverse proxy.
- Set `INTERNAL_API_PROTOCOL`, `INTERNAL_API_HOST`, and `INTERNAL_API_PORT` to the private PHP endpoint reachable by the Node service.
- Configure `TURN_HOST` and `TURN_SECRET` only for a TURN server you control. With both unset, clients receive public STUN entries only.
- Restrict access to `/install` after setup and keep `config/Config.php` and credential files unreachable from the web.
- Back up the database and `uploads/` outside Git. Neither contains data in this package.
- Review the legacy authentication, CORS, and dependency surface before exposing a fresh deployment to the public internet.

## Repository hygiene

The committed upload folders contain only `.gitkeep` placeholders. `node_modules`, logs, local environment files, database files, TLS keys, and Firebase service-account credentials are ignored. `install/dataBase.sql` contains table/index structure only.
