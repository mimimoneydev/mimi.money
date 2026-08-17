#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo "Run this installer as root." >&2
  exit 1
fi

plugin_source=/var/www/agenticous.mimi.money/integration/openclaw-plugin
plugin_target=/opt/openclaw/plugins/agenticous-blockchain
client_env=/etc/mimi-support-x402/client.env
openclaw_cli=/opt/openclaw/bin/openclaw
openclaw_home=/var/lib/openclaw
openclaw_secret_dir=/etc/openclaw
openclaw_client_token_file=/etc/openclaw/agenticous-client.token

if [[ ! -x "$openclaw_cli" ]] || [[ ! -f "$plugin_source/openclaw.plugin.json" ]] || [[ ! -d "$plugin_source/dist" ]] || [[ ! -d "$plugin_source/node_modules/typebox" ]] || [[ ! -r "$client_env" ]]; then
  echo "OpenClaw, the built plugin, and the support sidecar environment must exist before installation." >&2
  exit 1
fi

internal_token=$(sed -n 's/^INTERNAL_TOKEN=//p' "$client_env" | tail -n 1)
if [[ ${#internal_token} -lt 32 ]]; then
  echo "The support sidecar token is unavailable." >&2
  exit 1
fi

config_patch=$(mktemp /tmp/agenticous-openclaw-config.XXXXXX)
cleanup() {
  rm -f "$config_patch"
}
trap cleanup EXIT
umask 077
install -d -m 0750 -o root -g openclaw "$openclaw_secret_dir"
printf '%s\n' "$internal_token" > "$openclaw_client_token_file"
chown root:openclaw "$openclaw_client_token_file"
chmod 0640 "$openclaw_client_token_file"

install -d -m 0755 -o root -g root "$plugin_target"
cp -a "$plugin_source/dist" "$plugin_target/"
install -m 0644 "$plugin_source/package.json" "$plugin_source/package-lock.json" "$plugin_source/openclaw.plugin.json" "$plugin_source/README.md" "$plugin_target/"
install -d -m 0755 "$plugin_target/node_modules"
cp -a "$plugin_source/node_modules/typebox" "$plugin_target/node_modules/"
chown -R root:root "$plugin_target"
find "$plugin_target" -type d -exec chmod 0755 {} +
find "$plugin_target" -type f -exec chmod 0644 {} +

if ! sudo -u openclaw env HOME="$openclaw_home" "$openclaw_cli" plugins info agenticous-blockchain >/dev/null 2>&1; then
  sudo -u openclaw env HOME="$openclaw_home" "$openclaw_cli" plugins install --link "$plugin_target"
fi
printf '%s\n' "{ plugins: { entries: { 'agenticous-blockchain': { enabled: true, config: { supportClientUrl: 'http://127.0.0.1:4411', supportClientTokenFile: '$openclaw_client_token_file', explorerTimeoutMs: 10000 } } } } }" > "$config_patch"
chown openclaw:openclaw "$config_patch"
sudo -u openclaw env HOME="$openclaw_home" "$openclaw_cli" config patch --file "$config_patch"
sudo -u openclaw env HOME="$openclaw_home" "$openclaw_cli" config validate

echo "Agenticous AI agent OpenClaw plugin installed and configured. Restart mimi-openclaw only after reviewing its effective tool policy."
