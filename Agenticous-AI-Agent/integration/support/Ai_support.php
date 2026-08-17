<?php
defined('BASEPATH') OR exit('No direct script access allowed');

class Ai_support extends CI_Controller {
    private $payload = array();

    public function __construct() {
        parent::__construct();
        $this->load->database();
        $this->config->load('openclaw');
        $this->config->load('agenticous');
        $this->output->set_content_type('application/json');
        if ($this->input->method(TRUE) === 'POST') {
            $decoded = json_decode($this->input->raw_input_stream, TRUE);
            $this->payload = is_array($decoded) ? $decoded : $this->input->post(NULL, TRUE);
        }
    }

    public function health() {
        $agenticous_status = $this->agenticous_status();
        return $this->respond(array(
            'status' => 'ok',
            'chatbull' => $this->db->conn_id ? 'connected' : 'unavailable',
            'openclaw' => $this->gateway_health() ? 'running' : 'unavailable',
            'agenticous' => $agenticous_status ? 'running' : 'unavailable',
            'support_wallet' => $agenticous_status && isset($agenticous_status['payerAddress']) ? $agenticous_status['payerAddress'] : NULL,
            'support_wallet_basescan_url' => $agenticous_status && isset($agenticous_status['payerAddress']) ? 'https://basescan.org/address/' . $agenticous_status['payerAddress'] : NULL,
            'model' => $this->config->item('openclaw_model')
        ));
    }

    public function chat() {
        if ($this->input->method(TRUE) !== 'POST') {
            return $this->respond(array('error' => 'POST required'), 405);
        }

        $attempts = $this->session->userdata('mimi_ai_attempts');
        $attempts = is_array($attempts) ? array_values(array_filter($attempts, function($time) { return $time > time() - 60; })) : array();
        if (count($attempts) >= 10) {
            return $this->respond(array('error' => 'Too many messages. Please wait a minute and try again.'), 429);
        }
        $attempts[] = time();
        $this->session->set_userdata('mimi_ai_attempts', $attempts);

        $message = trim((string) $this->value('message'));
        if ($message === '' || mb_strlen($message) > 2000) {
            return $this->respond(array('error' => 'Message must contain 1 to 2000 characters.'), 422);
        }

        $conversation = $this->find_or_create_conversation();
        if (!$conversation) {
            return $this->respond(array('error' => 'Unable to create the support conversation.'), 500);
        }

        $this->store_message($conversation->chat_session_id, $conversation->visitor_id, $message);
        $wallet_address = $this->extract_evm_address($message);
        $agenticous = FALSE;
        if ($wallet_address && !$this->has_active_agenticous_job($conversation->id)) {
            $agenticous = $this->request_agenticous_report($conversation, $wallet_address);
        }

        if (is_array($agenticous)) {
            $handoff = !$agenticous['ok'];
            $reply = $agenticous['ok']
                ? $this->format_agenticous_reply($agenticous['data'])
                : 'The Agenticous AI agent could not verify the wallet activity. I saved your request for a human support agent; no successful service result is being claimed.';
            $provider = $agenticous['ok'] ? 'agenticous-x402' : 'human';
        } else {
            $ai = $this->ask_openclaw($conversation, $message);
            $handoff = !$ai['ok'];
            $reply = $ai['ok']
                ? $ai['reply']
                : 'I have saved your message and notified the MiMi Money support team. A human agent will continue this conversation.';
            $provider = $ai['ok'] ? 'openclaw' : 'human';
        }

        $this->store_message($conversation->chat_session_id, 1, $reply);
        $this->db->where('id', $conversation->id)->update('mimi_ai_conversations', array(
            'state' => $handoff ? 'handoff' : 'ai',
            'updated_at' => gmdate('Y-m-d H:i:s')
        ));

        return $this->respond(array(
            'conversation_token' => $conversation->public_token,
            'reply' => $reply,
            'handoff' => $handoff,
            'provider' => $provider
        ));
    }

    public function handoff() {
        $token = (string) $this->value('conversation_token');
        $conversation = $this->conversation_by_token($token);
        if (!$conversation) {
            return $this->respond(array('error' => 'Conversation not found.'), 404);
        }
        $this->db->where('id', $conversation->chat_session_id)->update('chatbull_chat_sessions', array('session_status' => 'requested'));
        $this->db->where('id', $conversation->id)->update('mimi_ai_conversations', array('state' => 'handoff', 'updated_at' => gmdate('Y-m-d H:i:s')));
        return $this->respond(array('result' => 'success', 'message' => 'A human support agent has been notified.'));
    }

    private function find_or_create_conversation() {
        $token = (string) $this->value('conversation_token');
        if ($token !== '') {
            $existing = $this->conversation_by_token($token);
            if ($existing) return $existing;
        }

        $now = gmdate('Y-m-d H:i:s');
        $name = mb_substr(trim((string) $this->value('name')), 0, 100);
        $email = mb_substr(trim((string) $this->value('email')), 0, 100);
        if ($name === '') $name = 'MiMi Money Customer';

        $this->db->trans_start();
        $this->db->insert('chatbull_users', array(
            'name' => $name, 'email' => $email, 'display_name' => $name,
            'contact_number' => '', 'pass' => '', 'profile_pic' => '',
            'profile_color' => '#2093cd', 'user_status' => 'active', 'role' => 'visitor',
            'remember_token' => '', 'last_activity_time' => $now,
            'mobile_last_activity_time' => $now, 'desktop_last_activity_time' => $now,
            'last_login' => $now, 'created_at' => $now, 'modified_at' => $now
        ));
        $visitor_id = $this->db->insert_id();
        $this->db->insert('chatbull_chat_sessions', array(
            'site_id' => 0, 'user_agent' => $this->input->user_agent(),
            'port' => '0', 'session_status' => 'requested', 'session_type' => 'public'
        ));
        $session_id = $this->db->insert_id();
        $this->db->insert('chatbull_chat_users', array(
            'chat_session_id' => $session_id, 'user_id' => $visitor_id,
            'user_role' => 'visitor', 'started_at' => $now
        ));
        $public_token = bin2hex(random_bytes(32));
        $this->db->insert('mimi_ai_conversations', array(
            'public_token' => $public_token, 'chat_session_id' => $session_id,
            'visitor_id' => $visitor_id, 'state' => 'ai',
            'created_at' => $now, 'updated_at' => $now
        ));
        $conversation_id = $this->db->insert_id();
        $this->db->trans_complete();
        return $this->db->trans_status() ? $this->db->where('id', $conversation_id)->get('mimi_ai_conversations')->row() : FALSE;
    }

    private function conversation_by_token($token) {
        if (!preg_match('/^[a-f0-9]{64}$/', $token)) return FALSE;
        return $this->db->where('public_token', $token)->get('mimi_ai_conversations')->row();
    }

    private function store_message($session_id, $sender_id, $message) {
        $this->db->insert('chatbull_chat_messages', array(
            'chat_session_id' => $session_id, 'sender_id' => $sender_id,
            'local_id' => substr(bin2hex(random_bytes(12)), 0, 25),
            'sort_order' => sprintf('%.6f', microtime(TRUE)),
            'chat_message' => $message, 'message_type' => 'text',
            'message_status' => 'unread', 'created_at' => gmdate('Y-m-d H:i:s')
        ));
    }

    private function ask_openclaw($conversation, $latest_message) {
        $history = $this->db->select('sender_id, chat_message')->where('chat_session_id', $conversation->chat_session_id)
            ->order_by('id', 'DESC')->limit(20)->get('chatbull_chat_messages')->result_array();
        $messages = array(array('role' => 'system', 'content' => 'You are MiMi Money customer support. Use the knowledge base and approved tools. Never invent account, payment, or transaction facts. Ask for safe identifiers when needed and hand off to a human when a tool or verified answer is unavailable. Never request passwords, PINs, OTPs, seed phrases, or full card details.'));
        foreach (array_reverse($history) as $row) {
            $messages[] = array('role' => ((int) $row['sender_id'] === (int) $conversation->visitor_id) ? 'user' : 'assistant', 'content' => $row['chat_message']);
        }
        $body = json_encode(array('model' => 'openclaw', 'user' => 'mimi-support-' . $conversation->public_token, 'stream' => FALSE, 'messages' => $messages));
        $ch = curl_init($this->config->item('openclaw_url'));
        curl_setopt_array($ch, array(
            CURLOPT_POST => TRUE, CURLOPT_RETURNTRANSFER => TRUE, CURLOPT_CONNECTTIMEOUT => 2, CURLOPT_TIMEOUT => 45,
            CURLOPT_HTTPHEADER => array('Authorization: Bearer ' . $this->config->item('openclaw_token'), 'Content-Type: application/json', 'x-openclaw-model: ' . $this->config->item('openclaw_model')),
            CURLOPT_POSTFIELDS => $body
        ));
        $raw = curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        $decoded = json_decode($raw, TRUE);
        $reply = isset($decoded['choices'][0]['message']['content']) ? trim((string) $decoded['choices'][0]['message']['content']) : '';
        if ($status >= 200 && $status < 300 && $reply !== '') return array('ok' => TRUE, 'reply' => $reply);
        log_message('error', 'OpenClaw request failed: HTTP ' . $status . ' ' . $error);
        return array('ok' => FALSE, 'reply' => '');
    }

    private function extract_evm_address($message) {
        if (!preg_match('/(?:^|[^a-fA-F0-9])(0x[a-fA-F0-9]{40})(?:$|[^a-fA-F0-9])/', (string) $message, $matches)) {
            return FALSE;
        }
        return $matches[1];
    }

    private function has_active_agenticous_job($conversation_id) {
        if (!$this->db->table_exists('mimi_agenticous_jobs')) return FALSE;
        return $this->db->where('conversation_id', (int) $conversation_id)
            ->where_in('status', array('pending', 'settled'))
            ->count_all_results('mimi_agenticous_jobs') > 0;
    }

    private function request_agenticous_report($conversation, $wallet_address) {
        if (!$this->db->table_exists('mimi_agenticous_jobs')) {
            log_message('error', 'Agenticous AI agent jobs table is unavailable.');
            return array('ok' => FALSE);
        }

        $token = (string) $this->config->item('agenticous_client_token');
        $url = rtrim((string) $this->config->item('agenticous_client_url'), '/') . '/v1/reports';
        if ($token === '' || !preg_match('#^http://127\.0\.0\.1:\d+$#', rtrim((string) $this->config->item('agenticous_client_url'), '/'))) {
            log_message('error', 'Agenticous AI agent client configuration is unavailable or unsafe.');
            return array('ok' => FALSE);
        }

        $now = gmdate('Y-m-d H:i:s');
        $this->db->insert('mimi_agenticous_jobs', array(
            'conversation_id' => (int) $conversation->id,
            'wallet_address' => $wallet_address,
            'status' => 'pending',
            'created_at' => $now,
            'updated_at' => $now
        ));
        $job_id = $this->db->insert_id();

        $ch = curl_init($url);
        curl_setopt_array($ch, array(
            CURLOPT_POST => TRUE,
            CURLOPT_RETURNTRANSFER => TRUE,
            CURLOPT_CONNECTTIMEOUT => 2,
            CURLOPT_TIMEOUT => (int) $this->config->item('agenticous_timeout_seconds'),
            CURLOPT_HTTPHEADER => array('Authorization: Bearer ' . $token, 'Content-Type: application/json'),
            CURLOPT_POSTFIELDS => json_encode(array('address' => $wallet_address))
        ));
        $raw = curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        $decoded = json_decode($raw, TRUE);
        $valid = $status >= 200 && $status < 300
            && is_array($decoded)
            && isset($decoded['report']) && is_array($decoded['report'])
            && isset($decoded['payment']['success']) && $decoded['payment']['success'] === TRUE;

        if (!$valid) {
            $this->db->where('id', $job_id)->update('mimi_agenticous_jobs', array(
                'status' => 'failed', 'updated_at' => gmdate('Y-m-d H:i:s')
            ));
            log_message('error', 'Agenticous AI agent request failed: HTTP ' . $status . ($error ? ' transport error' : ''));
            return array('ok' => FALSE);
        }

        $payment = $decoded['payment'];
        $report = $decoded['report'];
        $this->db->where('id', $job_id)->update('mimi_agenticous_jobs', array(
            'agent_request_id' => isset($report['requestId']) ? substr((string) $report['requestId'], 0, 64) : NULL,
            'status' => 'settled',
            'payment_network' => isset($payment['network']) ? substr((string) $payment['network'], 0, 64) : NULL,
            'payment_transaction' => isset($payment['transaction']) ? substr((string) $payment['transaction'], 0, 128) : NULL,
            'payment_payer' => isset($payment['payer']) ? substr((string) $payment['payer'], 0, 64) : NULL,
            'payment_recipient' => isset($payment['recipient']) ? substr((string) $payment['recipient'], 0, 64) : NULL,
            'payment_amount_usd' => isset($payment['amountUsd']) ? (float) $payment['amountUsd'] : 0.01,
            'updated_at' => gmdate('Y-m-d H:i:s')
        ));
        return array('ok' => TRUE, 'data' => $decoded);
    }

    private function format_agenticous_reply($data) {
        $report = isset($data['report']) && is_array($data['report']) ? $data['report'] : array();
        $payment = isset($data['payment']) && is_array($data['payment']) ? $data['payment'] : array();
        $summary = isset($report['summary']) && is_array($report['summary']) ? $report['summary'] : array();
        $transactions = isset($report['transactions']) && is_array($report['transactions']) ? array_slice($report['transactions'], 0, 7) : array();
        $searched = isset($summary['networksSearched']) ? (int) $summary['networksSearched'] : 0;
        $unavailable = isset($summary['networksUnavailable']) ? (int) $summary['networksUnavailable'] : 0;
        $lines = array();
        $lines[] = 'The Agenticous AI agent completed the 7-day wallet search for ' . (isset($report['address']) ? $report['address'] : 'the supplied address') . '.';
        $lines[] = 'Explorer coverage: ' . $searched . ' searched, ' . $unavailable . ' unavailable. Up to 7 transactions are shown.';
        $intelligence = isset($report['intelligence']) && is_array($report['intelligence']) ? $report['intelligence'] : array();
        if (isset($intelligence['status']) && $intelligence['status'] === 'generated' && !empty($intelligence['overview'])) {
            $model = isset($intelligence['model']) ? (string) $intelligence['model'] : 'Gemini';
            $route = isset($intelligence['upstreamProvider']) ? (string) $intelligence['upstreamProvider'] : 'configured provider';
            $lines[] = $model . ' summary (' . $route . '): ' . trim((string) $intelligence['overview']);
            if (isset($intelligence['notableActivity']) && is_array($intelligence['notableActivity'])) {
                foreach (array_slice($intelligence['notableActivity'], 0, 3) as $item) {
                    if (is_string($item) && trim($item) !== '') $lines[] = '• ' . trim($item);
                }
            }
        }

        if (count($transactions) === 0) {
            $lines[] = 'No recent transactions were found by the explorers that responded. This does not prove the address had no activity.';
        } else {
            $lines[] = 'Recent transactions:';
            foreach ($transactions as $index => $transaction) {
                $chain = isset($transaction['chain']) ? $transaction['chain'] : 'Unknown network';
                $direction = isset($transaction['direction']) ? strtoupper($transaction['direction']) : 'UNKNOWN';
                $amount = isset($transaction['amount']) && $transaction['amount'] !== '' ? $transaction['amount'] . ' ' : '';
                $asset = isset($transaction['asset']) ? $transaction['asset'] : '';
                $timestamp = isset($transaction['timestamp']) ? $transaction['timestamp'] : 'unknown time';
                $status = isset($transaction['status']) ? $transaction['status'] : 'unknown';
                $hash = isset($transaction['hash']) ? $transaction['hash'] : '';
                $short_hash = strlen($hash) > 18 ? substr($hash, 0, 10) . '…' . substr($hash, -6) : $hash;
                $url = isset($transaction['explorerUrl']) ? $transaction['explorerUrl'] : '';
                $lines[] = ($index + 1) . '. [' . $chain . '] ' . $direction . ' ' . trim($amount . $asset)
                    . ' — ' . $timestamp . ' — ' . $status . ' — ' . $short_hash
                    . ($url ? ' — ' . $url : '');
            }
        }

        $payment_hash = isset($payment['transaction']) ? (string) $payment['transaction'] : '';
        $lines[] = 'Service payment: $0.01 via x402 on ' . (isset($payment['network']) ? $payment['network'] : 'Base')
            . ($payment_hash ? '; settlement transaction ' . $payment_hash . '.' : '.');
        if (!empty($payment['explorerUrl'])) $lines[] = 'Basescan payment receipt: ' . $payment['explorerUrl'];
        $lines[] = 'Explorer results can be delayed or incomplete. Verify important activity using the supplied transaction links.';
        return implode("\n", $lines);
    }

    private function agenticous_health() {
        return $this->agenticous_status() !== FALSE;
    }

    private function agenticous_status() {
        $token = (string) $this->config->item('agenticous_client_token');
        $base = rtrim((string) $this->config->item('agenticous_client_url'), '/');
        if ($token === '' || !preg_match('#^http://127\.0\.0\.1:\d+$#', $base)) return FALSE;
        $ch = curl_init($base . '/healthz');
        curl_setopt_array($ch, array(
            CURLOPT_RETURNTRANSFER => TRUE,
            CURLOPT_CONNECTTIMEOUT => 1,
            CURLOPT_TIMEOUT => 2,
            CURLOPT_HTTPHEADER => array('Authorization: Bearer ' . $token)
        ));
        $raw = curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        $decoded = json_decode($raw, TRUE);
        return $status >= 200 && $status < 300 && is_array($decoded) ? $decoded : FALSE;
    }

    private function gateway_health() {
        $ch = curl_init('http://127.0.0.1:18789/health');
        curl_setopt_array($ch, array(CURLOPT_RETURNTRANSFER => TRUE, CURLOPT_CONNECTTIMEOUT => 1, CURLOPT_TIMEOUT => 2));
        curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        return $status >= 200 && $status < 500;
    }

    private function value($key) { return isset($this->payload[$key]) ? $this->payload[$key] : ''; }
    private function respond($data, $status = 200) { return $this->output->set_status_header($status)->set_output(json_encode($data)); }
}
