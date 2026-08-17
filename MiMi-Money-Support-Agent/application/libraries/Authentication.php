<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

class Authentication {

    private $error = array();

    function __construct() {
        $this->ci = & get_instance();
    }

    /**
     * A generic funtion to check user as a authenticated or not, and check if a session is open or closed and show the messages.
     * @param ($email , $password) 
     */
    function login($user_login, $password) {
        if ($user = $this->ci->user->get_single(array('email' => $user_login, 'role !=' => 'visitor'))) {

            if ($user->user_status == 'blocked') {
                $this->ci->session->set_flashdata('error', $this->ci->lang->line('blocked_account'));
                return false;
                exit;
            }

            if ($user->user_status == 'pending') {
                $this->ci->session->set_flashdata('error', sprintf($this->ci->lang->line('pending_account'), $this->ci->site_name));
                return false;
            }

            if ($user->pass == md5($password) and $user->user_status == 'active') {
                $this->store_session($user);
                
                return $user;
            } else {
                $this->ci->session->set_flashdata('error', $this->ci->lang->line('wrong_password'));
                return false;
            }
        } else {
            $this->ci->session->set_flashdata('error', $this->ci->lang->line('wrong_login_id'));
            return false;
        }
    }
    
    /**
     * Login request by ajax
     * A generic funtion to check user as a authenticated or not, and check if a session is open or closed and show the messages.
     * @param ($email , $password) 
     */
    function logiin_ajax($user_login, $password) {
        $response = array('result' => 'failed', 'error' => '');
        if ($user = $this->ci->user->get_single(array('email' => $user_login, 'role !=' => 'visitor'))) {

            if ($user->user_status == 'blocked') {
                $response['error'] = $this->ci->lang->line('blocked_account');
            } elseif ($user->user_status == 'pending') {
                $response['error'] = sprintf($this->ci->lang->line('pending_account'), $this->ci->site_name);
            } elseif ($user->pass == md5($password) and $user->user_status == 'active') {
                $this->store_session($user);

                $response['result'] = 'success';
                $response['user'] = $user;
            } else {
                $response['error'] = $this->ci->lang->line('wrong_password');
            }
        } else {
            $response['error'] = $this->ci->lang->line('wrong_login_id');
        }
        
        return $response;
    }
    
    /*
     * Store userdata in session
     */
    function store_session ($user) {
        $this->ci->session->set_userdata('current_user_id', $user->id);
        $this->ci->session->set_userdata('dismis_update_alert', 'no');
        $remember_token = random_string('alnum', 20);
        if (empty($user->remember_token)) {
            $user->remember_token = $remember_token;
        }
        $settings = $this->ci->configuration->get_settings();
        $data = array('settings' => $settings, 'user' => $user);
        $this->ci->session->set_userdata($data);

        $this->ci->db->update($this->ci->user->table, array('last_login' => date("Y-m-d H:i:s", now()), 'remember_token' => $user->remember_token), array("id" => $user->id));
    }

}
