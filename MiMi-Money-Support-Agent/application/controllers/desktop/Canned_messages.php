<?php

class Canned_messages extends CP_AppController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();
        $this->load->model('canned_message');
    }

    /*
     * Get all entries from database
     * 
     * @return Json list
     */

    function index() {
        $user_id = $this->input->get('user_id');
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');
        
        if ($this->valid_token) {
            $user = $this->user->get_single(array('id' => $user_id));
            $canned_messages = $this->canned_message->get_messages($user->id, $user->role);
            
            $response['result'] = 'success';
            $response['canned_messages'] = $canned_messages;
        } else {
            $response['error'] = $this->lang->line('invalid_token');
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
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');
        
        if ($this->valid_token) {
            $this->canned_message->validation_rules[] = array(
                'field' => 'user_id',
                'label' => 'lang:canned_user_id',
                'rules' => 'required'
            );

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
                    $this->canned_message->model_data['user_id'] = $this->input->post('user_id');
                    $saved = $this->canned_message->insert();
                    $response['is_new'] = TRUE;
                    $response['created'] = $saved;
                }

                if ($saved) {
                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('canned_saved_success');
                } else {
                    $response['error'] = $this->lang->line('canned_saved_error');
                }
            } else {
                $response['error'] = validation_errors();
            }
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To delete entry from database
     * 
     * @return Json $response
     */

    function delete() {
        $message_id = $this->input->get('id');        
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');
        
        if ($this->valid_token) {
            if ($this->canned_message->delete_where(array("id" => $message_id))) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('canned_deleted_success');
            } else {
                $response['error'] = $this->lang->line('canned_deleted_error');
            }
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
