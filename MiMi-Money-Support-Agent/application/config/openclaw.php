<?php
defined('BASEPATH') OR exit('No direct script access allowed');

$config['openclaw_url'] = getenv('OPENCLAW_URL') ?: 'http://127.0.0.1:18789/v1/chat/completions';
$config['openclaw_token'] = getenv('OPENCLAW_TOKEN') ?: '';
$config['openclaw_model'] = getenv('OPENCLAW_MODEL') ?: 'openrouter/google/gemini-3.6-flash';
