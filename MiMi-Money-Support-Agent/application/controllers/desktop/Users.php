<?php

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
     * @return user object with error key.
     */

    function authentication() {
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        //check if data is valid or not
        $this->form_validation->set_rules($this->user->validation_rules['login']);

        if ($this->form_validation->run() === true) {
            $user = $this->authentication->login($this->input->post('email'), $this->input->post('password'));
            if ($user and is_object($user) and in_array($user->role, array('agent', 'admin'))) {
                // updating last activity time of logged in user
                $this->user->model_data = array('desktop_last_activity_time' => date("Y-m-d H:i:s", now()));
                $this->user->update($user->id);

                $response['id'] = $user->id;
                $response['email'] = $user->email;
                $response['profile_color'] = $user->profile_color;
                $response['display_name'] = $user->display_name;
                $response['profile_picture'] = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $user->email);

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
        $user_id = $this->input->get('user_id');

        if ($this->valid_token) {
            // updating last activity time of logged in user
            $this->user->model_data = array('desktop_last_activity_time' => date("Y-m-d H:i:s", (now() - (5 * 60))));
            $this->user->update($user_id);

            $response['result'] = 'success';
            $response['message'] = $this->lang->line('logged_out');
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

}
