<?php
defined('BASEPATH') OR exit('No direct script access allowed');

$active_group = 'default';
$query_builder = TRUE;

$db = array();
$db['default'] = array(
    'dsn' => '',
    'hostname' => getenv('MIMI_DB_HOST') ?: '127.0.0.1',
    'username' => getenv('MIMI_DB_USER') ?: 'mimi_support',
    'password' => getenv('MIMI_DB_PASSWORD') ?: '',
    'database' => getenv('MIMI_DB_NAME') ?: 'mimi_support',
    'dbdriver' => 'mysqli',
    'dbprefix' => 'chatbull_',
    'pconnect' => FALSE,
    'db_debug' => (ENVIRONMENT !== 'production'),
    'cache_on' => FALSE,
    'cachedir' => '',
    'char_set' => 'utf8mb4',
    'dbcollat' => 'utf8mb4_general_ci',
    'swap_pre' => '',
    'encrypt' => FALSE,
    'compress' => FALSE,
    'stricton' => FALSE,
    'failover' => array(),
    'save_queries' => TRUE,
);
