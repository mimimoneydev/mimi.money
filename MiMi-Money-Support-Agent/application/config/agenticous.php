<?php
defined('BASEPATH') OR exit('No direct script access allowed');

$settings = array();
$settings_file = getenv('AGENTICOUS_PHP_INI') ?: '/etc/mimi-support-x402/php.ini';
if (is_readable($settings_file)) {
    $parsed = parse_ini_file($settings_file, FALSE, INI_SCANNER_RAW);
    if (is_array($parsed)) $settings = $parsed;
}

$config['agenticous_client_url'] = isset($settings['AGENTICOUS_CLIENT_URL'])
    ? $settings['AGENTICOUS_CLIENT_URL']
    : (getenv('AGENTICOUS_CLIENT_URL') ?: 'http://127.0.0.1:4411');
$config['agenticous_client_token'] = isset($settings['AGENTICOUS_CLIENT_TOKEN'])
    ? $settings['AGENTICOUS_CLIENT_TOKEN']
    : (getenv('AGENTICOUS_CLIENT_TOKEN') ?: '');
$config['agenticous_timeout_seconds'] = 35;
