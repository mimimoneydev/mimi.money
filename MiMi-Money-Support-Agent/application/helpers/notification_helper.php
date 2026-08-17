<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

/*
 * To verify and register lincese key and domain
 * 
 * @param String $license_key
 * 
 * @return json 
 */

function validate_license_key($license_key) {
    $CI = & get_instance();

    // sending request on server to validate lincense key.
    $url = CHATBULL_APIURL . 'notify_domain.php?action=register-domain';
    $fields = array('license_key' => $license_key, 'site_url' => base_url(), 'product_name' => PRODUCT_NAME);

    $CI->curl->create($url);
    $CI->curl->post($fields);
    $CI->curl->option(USERAGENT, $CI->input->user_agent());
    $result = $CI->curl->execute();

    if ($CI->curl->error_code) {
        $response = new stdClass();
        $response->result = 'failed';
        $response->error_code = $CI->curl->error_code;
        $response->error_string = $CI->curl->error_string;
        $response->errors = sprintf($CI->lang->line('processing_error_contact_author'), CHATBULL_SITEURL);
        $response->info = $CI->curl->info;

        return $response;
    } else {
        return json_decode($result);
    }
}

/*
 * To unregister lincese key and domain
 * 
 * @param String $license_key
 * 
 * @return json 
 */

function unregister_license_key($license_key) {
    $CI = & get_instance();

    // sending request on server to validate lincense key.
    $url = CHATBULL_APIURL . 'notify_domain.php?action=unregister-domain';
    $fields = array('license_key' => $license_key, 'site_url' => base_url());

    $CI->curl->create($url);
    $CI->curl->post($fields);
    $CI->curl->option(USERAGENT, $CI->input->user_agent());
    $result = $CI->curl->execute();

    if ($CI->curl->error_code) {
        $response = new stdClass();
        $response->result = 'failed';
        $response->error_code = $CI->curl->error_code;
        $response->error_string = $CI->curl->error_string;
        $response->errors = sprintf($CI->lang->line('processing_error_contact_author'), CHATBULL_SITEURL);
        $response->info = $CI->curl->info;

        return $response;
    } else {
        return json_decode($result);
    }
}

/*
 * This function will use to send push notification to user.
 * 
 * @param $devices ( ids of device which will get notification.)
 * @param $message (array of options)
 */

function sendPushNotification($devices = array(), $message = array()) {
    if(count($devices) > 0 and count($message) > 0) {
        $CI = & get_instance();
        $url = 'https://fcm.googleapis.com/fcm/send';
    
        if(isset($message['message_type'])) {
            $message_type = $message['message_type'];
            unset($message['message_type']);

            $message['content_type'] = $message_type;
        }


        // Set POST variables
        $fields = array(
            'priority' => 'high',
            'content_available' => true,
            'registration_ids' => $devices,
            'data' => $message
        );
        
        if(isset($message['name']) and $message['name'] and isset($message['message']) and $message['message']) {
            $fields['notification'] = array('title' => $message['name'], 'body' => $message['message'], 'sound' => 'default');
        }

        $CI->curl->create($url);
        $CI->curl->http_header('Authorization', 'key=' . ANDROID_NOTIFICATION_KEY);
        $CI->curl->http_header('Content-Type', 'application/json');
        $CI->curl->post(json_encode($fields));
        $response = $CI->curl->execute();


        if ($CI->curl->error_code) {
            $response = new stdClass();
            $response->result = 'failed';
            $response->error_code = $CI->curl->error_code;
            $response->error_string = $CI->curl->error_string;
            $response->errors = sprintf($CI->lang->line('processing_error_contact_author'), CHATBULL_SITEURL);
            $response->info = $CI->curl->info;

            return $response;
        } else {
            return json_decode($response);
        }
    }
}

/*
 * This function will use to send notifications.
 * 
 * @param $user_id
 * @param $message
 * @param $badge
 * @param $sedor_device_id
 * 
 * @return true
 */

function push_notification($user_id, $message, $sedor_device_id = '') {
    $CI = & get_instance();
    
    $conditions = array('user_id' => $user_id, 'user_status' => 1);
    
    if($sedor_device_id) $conditions['device_id !='] = $sedor_device_id;
    
    $CI->db->select('device_id');
    $query = $CI->db->get_where($CI->gcm->table, $conditions);
    $results = $query->result_array();
    $devices = array_column($results,"device_id");
    
    sendPushNotification($devices, $message);
}

/*
 * This function will be use to send template email
 * 
 * @param $template_file
 * @param $to
 * @param $data
 * 
 * @return true or false
 */

function send_template_email($template_file, $to, $subject, $data = array()) {
    if (empty($data)) {
        return false;
    }

    $CI = & get_instance();

    $settings = $CI->configuration->get_settings(array('site_id' => 0));
    if (empty($settings->admin_panel_logo)) {
        $settings->admin_panel_logo = base_url("assets/cmodule/images/logo.png");
    }

    $data['settings'] = $settings;
    $data['template_file'] = $template_file;

    if (!isset($data['from_email'])) {
        $data['from_email'] = $settings->admin_panel_email;
    }

    if (!isset($data['from_name'])) {
        $data['from_name'] = $settings->admin_panel_name;
    }

    if (!isset($data['site_name'])) {
        $data['site_name'] = $settings->admin_panel_name;
    }

    if (!isset($data['site_logo']) or empty($data['site_logo'])) {
        $data['site_logo'] = $settings->admin_panel_logo;
    }

    $CI->load->library('email');
    $config = array('priority' => 1, 'mailtype' => 'html');
    $CI->email->initialize($config);
    $CI->email->from($data['from_email'], $data['from_name']);
    $CI->email->to($to);

    if (isset($data['cc']) and $data['cc']) {
        $CI->email->cc($data['cc']);
    }

    if (isset($data['bcc']) and $data['bcc']) {
        $CI->email->cc($data['bcc']);
    }

    $CI->email->subject($subject . ' - ' . $data['site_name']);
    $message = $CI->load->view($CI->data['theme'] . '/mail_layout', $data, true);
    $CI->email->message($message);

    return $CI->email->send();
}
