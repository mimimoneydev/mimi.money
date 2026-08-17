<?php
/** Runtime database configuration. Keep credentials in environment variables. */
ob_start();
if (session_status() !== PHP_SESSION_ACTIVE) {
    session_start();
}
$isDebug = filter_var(getenv('APP_DEBUG') ?: 'false', FILTER_VALIDATE_BOOLEAN);
error_reporting($isDebug ? E_ALL : 0);
ini_set('display_errors', $isDebug ? '1' : '0');
$dbHost = getenv('DB_HOST') ?: '127.0.0.1';
$dbPort = getenv('DB_PORT') ?: '3306';

return $_Config = array(
    'DB_SERVER' => $dbHost . ':' . $dbPort,
    'DB_USER' => getenv('DB_USER') ?: 'mimi_money',
    'DB_PASSWORD' => getenv('DB_PASSWORD') ?: '',
    'DB_NAME' => getenv('DB_NAME') ?: 'mimi_money',
    'DB_TABLE_PREFIX' => getenv('DB_TABLE_PREFIX') ?: 'wa_',
);
