<?php

class Canned_messages extends CP_AgentController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();

        // check has admin access
        $this->has_admin_access();
        $this->load->model('canned_message');
    }

    /*
     * Load view of canned-messages
     */

    function index() {
        $cannet_messages = $this->lang->load('canned-messages', '', TRUE);

        // add users.js file
        $this->addjs(theme_url("js/app/directives/modal-confirm.js"), TRUE);
        $this->addjs(theme_url("js/app/canned-messages.js"), TRUE);

        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings, 'canned' => $cannet_messages));

        // add filter file.
        $this->data['filter_view'] = 'canned-messages/filters.php';

        //write code here to load view
        $this->bulid_layout("canned-messages/list");
    }

    /*
     * Get record from database
     */

    function get_messages() {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $canned_messages = $this->canned_message->filter_message($this->input->post());
            $response['canned_messages'] = $canned_messages;
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To save entry in database.
     * 
     * @method post
     * @return Json $response
     */

    function save() {
        $response = array('result' => 'failed', 'description' => '');

        if ($this->current_user) {
            //checking validations
            $this->form_validation->set_rules($this->canned_message->validation_rules);

            if ($this->form_validation->run() === true) {
                $saved = false;
                $this->canned_message->model_data = $this->input->post(array('title', 'description'));
                $message_id = $this->input->post('id');
                if ($message_id) {
                    $saved = $this->canned_message->update($message_id);
                    $response['is_new'] = FALSE;
                } else {
                    $this->canned_message->model_data['user_id'] = $this->current_user->id;
                    $saved = $this->canned_message->insert();
                    $response['is_new'] = TRUE;
                    $saved->profile_pic = $this->current_user->profile_pic;
                    $saved->userName = $this->current_user->name;
                    $saved->userProfilePic = $this->current_user->profile_picture;

                    $response['created'] = $saved;
                }

                if ($saved) {
                    $response['result'] = 'success';
                    $response['description'] = $this->lang->line('canned_saved_success');
                } else {
                    $response['description'] = $this->lang->line('canned_saved_error');
                }
            } else {
                $response['validation_errors'] = validation_errors();
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To delete entry from database
     * 
     * @return Json $response
     */

    function delete() {
        $response = array('result' => 'failed', 'description' => '');

        if ($this->current_user) {
            $message_id = $this->input->get('id');

            if ($this->canned_message->delete_where(array("id" => $message_id))) {
                $response['result'] = 'success';
                $response['description'] = $this->lang->line('canned_deleted_success');
            } else {
                $response['description'] = $this->lang->line('canned_deleted_error');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
