<?php
declare(strict_types=1);

define('BASEPATH', '/var/www/support.mimi.money/system/');
define('ENVIRONMENT', 'production');
$db = array();
require '/var/www/support.mimi.money/application/config/database.php';

if (!isset($db['default']) || !is_array($db['default'])) {
    fwrite(STDERR, "Support database configuration is unavailable.\n");
    exit(1);
}

$settings = $db['default'];
$connection = new mysqli(
    (string) $settings['hostname'],
    (string) $settings['username'],
    (string) $settings['password'],
    (string) $settings['database'],
    isset($settings['port']) ? (int) $settings['port'] : 3306,
);
if ($connection->connect_errno) {
    fwrite(STDERR, "Unable to connect to the support database.\n");
    exit(1);
}
$connection->set_charset('utf8mb4');
$sql = file_get_contents(__DIR__ . '/../integration/support/mimi-agenticous-jobs.sql');
if ($sql === false || !$connection->query($sql)) {
    fwrite(STDERR, "Unable to apply the Agenticous AI agent support migration.\n");
    exit(1);
}
echo "Agenticous AI agent support migration is present.\n";
