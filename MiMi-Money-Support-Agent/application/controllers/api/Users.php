<?php

/*
 * Controller to manage all users related actions
 * Author: Pukhraj Prajapat (pukhraj.prajapat@g-axon.com)
 */

class Users extends CP_AppController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();
    }

    /*
     * Authinticate user 
     * 
     * @param @appkey
     * @return user object with error key.
     */

    function authentication() {
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        //check if data is valid or not
        $this->form_validation->set_rules($this->gcm->validation_rules['login']);

        if ($this->form_validation->run() === true) {
            $user = $this->authentication->login($this->input->post('email'), $this->input->post('password'));
            if ($user and is_object($user) and in_array($user->role, array('agent', 'admin'))) {
                // updating last activity time of logged in user
                $this->user->model_data = array('mobile_last_activity_time' => date("Y-m-d H:i:s", now()));
                $this->user->update($user->id);

                $response['id'] = $user->id;
                $response['email'] = $user->email;
                $response['profile_color'] = $user->profile_color;
                $response['display_name'] = $user->display_name;
                $response['profile_picture'] = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $user->email, '404');

                $this->gcm->model_data = $this->input->post(array('device_id', 'device_type', 'mac_address'));
                $this->gcm->model_data['user_id'] = $user->id;
                $this->gcm->model_data['name'] = $user->name;
                $this->gcm->model_data['user_status'] = 1;
                $this->gcm->sysc_gsm_user();

                $response['token'] = $this->config->item('application_token');
                $response['settings'] = $this->settings;
                $response['result'] = 'success';
            } elseif ($user and is_object($user) and $user->role != 'agent') {
                $response['error'] = $this->lang->line('agent_access_only');
            } else {
                $response['error'] = $this->session->flashdata('error');
            }
        } else {
            $response['error'] = validation_errors();
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will set gsm user status 0;
     * 
     * @return error or message
     */

    function logout() {
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        if ($this->valid_token) {
            //check if data is valid or not
            $this->form_validation->set_rules($this->gcm->validation_rules['logout']);

            if ($this->form_validation->run() === true) {
                //check if data is valid or not
                $this->gcm->logout($this->input->post('mac_address'));

                // updating last activity time of logged in user
                $this->user->model_data = array('mobile_last_activity_time' => date("Y-m-d H:i:s", (now() - (5 * 60))));
                $this->user->update($this->input->post('user_id'));

                $response['result'] = 'success';
                $response['message'] = $this->lang->line('logged_out');
            } else {
                $response['error'] = validation_errors();
            }
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function fetch all notifications of a user
     * And return them.
     * 
     * @param $user_id (Id of User)
     * 
     * @return $notificationsData (list of notification) or error
     */

    public function notifications() {
        $user_id = $this->input->get('user_id');
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        if ($this->valid_token) {
            $notifications = $this->user->get_notifications($user_id);

            $response['result'] = 'success';
            $response['notifications'] = $notifications;
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will send user a link to change passwod in email
     * 
     * @param @appkey
     * @param post email
     * 
     * @return error or message
     */

    function forgot_pass() {
        $response = array('result' => 'failed');

        //check if data is valid or not
        $this->form_validation->set_rules($this->password_reminder->validation_rules['forget_password']);
        if ($this->form_validation->run() === true) {
            $user = $this->user->get_single(array('email' => $this->input->post('email'), 'role !=' => 'visitor'));
            if ($user) {
                $pass_token = md5(uniqid());
                if ($this->password_reminder->temp_reset_password($pass_token, $this->input->post('email'))) {

                    // send email to visitor
                    $template_file = 'forget_password';
                    $to = $this->input->post('email');
                    $subject = $this->lang->line('reset_your_password');
                    $link = site_url('c=admin&m=reset_password&token=' . $pass_token);
                    $message = $this->lang->line('email_change_pass_description');
                    
                    $data = array(
                        'name' => $user->name, 
                        'link' => $link, 
                        'message' => $message
                    );
                    
                    $mailsent = send_template_email($template_file, $to, $subject, $data);
                    if ($mailsent) {
                        $response['result'] = 'success';
                        $response['message'] = $this->lang->line('reset_link_sent');
                    } else {
                        $response['error'] = $this->lang->line('reset_link_not_sent');
                    }
                } else {
                    $response['error'] = $this->lang->line('process_error');
                }
            } else {
                $response['error'] = $this->lang->line('email_not_exits');
            }
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
