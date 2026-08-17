<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Chatbox extends CP_VisitorController {
    /*
     * This function will load chatbox for mobile
     */

    public function index() {;
        $siteuser = $this->input->get(array('name', 'email', 'message'));
        $cbwindow = $this->input->get(array('is_mobile', 'innerHeight', 'outerHeight', 'innerWidth', 'outerWidth'));
        $pageinfo = $this->input->get(array('page_title', 'page_url'));

        if (isset($_SERVER['HTTP_REFERER'])) {
            $this->session->set_userdata('referer_url', $_SERVER['HTTP_REFERER']);
        }

        if ($this->current_site and get_domain($this->current_site->site_url) == get_domain(referer_url())) {
            $this->load->view('chatbox/mobile', array('siteuser' => $siteuser, 'cbwindow' => $cbwindow, 'pageinfo' => $pageinfo));
        } else {
            return $this->output->set_content_type('application/json')->set_output($this->return_json($this->settings));
        }
    }

    /*
     * This function retrurns chatbox mini html.
     */

    function button() {
        $siteuser = $this->input->get(array('name', 'email', 'message'));
        $cbwindow = $this->input->get(array('is_mobile', 'innerHeight', 'outerHeight', 'innerWidth', 'outerWidth'));

        if ($this->current_site and get_domain($this->current_site->site_url) == get_domain(referer_url())) {
            $this->load->view('chatbox/chatbox-btn', array('siteuser' => $siteuser, 'cbwindow' => $cbwindow));
        } else {
            return $this->output->set_content_type('application/json')->set_output($this->return_json($this->settings));
        }
    }

    /*
     * This function retrurns chatbox mini html.
     */

    function chatpanel() {
        $siteuser = $this->input->get(array('name', 'email', 'message'));
        $cbwindow = $this->input->get(array('is_mobile', 'innerHeight', 'outerHeight', 'innerWidth', 'outerWidth'));

        if ($this->current_site and get_domain($this->current_site->site_url) == get_domain(referer_url())) {
            $this->load->view('chatbox/chatpanel', array('siteuser' => $siteuser, 'cbwindow' => $cbwindow));
        } else {
            return $this->output->set_content_type('application/json')->set_output($this->return_json($this->settings));
        }
    }

    /*
     * This function retrurns chatbox settings as a json.
     */

    function settings_json() {      
        header('Access-Control-Allow-Origin: *');
        return $this->output->set_content_type('application/json')->set_output($this->return_json($this->settings));
    }    

    /*
     * This function retrurns chatbox settings as a json.
     */

    function settings() {
        return $this->output->set_content_type('application/json')->set_output('var settings = ' . $this->return_json($this->settings));
    }

    /*
     * This function will send notification to ios server
     * 
     * @param $appkey
     */

    function send_notifications($appkey = '') {
        $response = array('error' => '', 'result' => 'failed');
        if ($appkey == APPKEY) {
            $ios_notifications = $this->input->post('ios_notifications');
            $domain_url = $this->input->post('domain_url');
            $licence_key = $this->input->post('licence_key');
            if ($ios_notifications and $domain_url and $licence_key) {
                foreach ($ios_notifications as $notification) {
                    send_ios_alert($notification['device_id'], $notification['badge'], $notification['message']);
                }
            }

            $response['result'] = 'success';
        } else {
            $response['error'] = $this->lang->line('invalid_appkey');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To check plugin installed and validated.
     */

    function is_installed() {
        $this->lang->load('wp');
        $response = array('result' => 'failed');
        $wp_url = $this->input->get('wp_url');
        $domain_name = get_domain($wp_url);
        $return_token = ($this->input->get('token')) ? $this->input->get('token') : 'no';

        if ($this->config->item('installed') == 'yes') {
            if ($this->config->item('validated_code') == 'yes') {
                if (empty($domain_name)) {
                    $response['errors']['widget-not-created'] = $this->lang->line('domain_missing');
                } else {
                    $this->db->like('site_url', $domain_name);
                    $query = $this->db->get(TABLE_SITES);
                    $token = $query->row();

                    if ($token) {
                        $response['result'] = 'success';
                        $response['message'] = $this->lang->line('plugin_is_ready');
                        if ($return_token == 'yes') {
                            $response['token'] = $token;
                        }
                    } else {
                        $response['errors']['widget-not-created'] = sprintf($this->lang->line('widget_not_created'), site_url('c=settings'));
                    }
                }
            } else {
                $response['errors']['not-validated'] = sprintf($this->lang->line('plugin_not_validated'), site_url('c=settings'));
            }
        } else {
            $response['errors']['not-installed'] = sprintf($this->lang->line('plugin_not_installed'), site_url());
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
