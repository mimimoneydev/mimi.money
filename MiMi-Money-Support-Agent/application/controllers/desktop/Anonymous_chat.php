<?php

class Anonymous_chat extends CP_AppController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();
    }

    /*
     * This function will use to get all messages
     * 
     * @return json data of messages
     */

    function get_messages() {
        $response = array('error' => '', 'result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        if ($this->valid_token) {
            $temp_visitor_id = $this->input->get('visitor_id');

            // get all message of visitor
            $messages = $this->temp_visitor->get_messages(array('temp_visitor_id' => $temp_visitor_id));

            $response['result'] = 'success';
            $response['messages_list'] = $messages;
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will create message entry
     * 
     * @return $result faild or success
     */

    public function send() {
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        if ($this->valid_token) {
            //check if data is valid or not
            $this->form_validation->set_rules($this->temp_visitor->validation_rules['new_message']);

            if ($this->form_validation->run() === true) {
                $temp_visitor_id = $this->input->post('temp_visitor_id');
                $operator_id = $this->input->post('sender_id');

                $this->temp_visitor->model_data = $this->input->post(array('chat_message', 'temp_visitor_id', 'message_status', 'sender_id', 'sort_order'));
                $local_id = 'ad' . $this->input->post('sender_id') . $this->input->post('sort_order');
                $this->temp_visitor->model_data['local_id'] = $local_id;
                $this->temp_visitor->model_data['created_at'] = date("Y-m-d H:i:s", now());

                $message_id = $this->temp_visitor->add_message($local_id);
                if ($message_id and is_object($message_id) === false) {
                    $response['initiated'] = $this->temp_visitor->initiate_chat_request($temp_visitor_id, $operator_id);

                    $response['result'] = 'success';
                    $response['message_row'] = $this->temp_visitor->model_data;
                } elseif ($message_id and is_object($message_id)) {
                    $response['error'] = $this->lang->line('message_enterd');
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
     * Close anonymous initiated request
     */

    function close() {
        $response = array('result' => 'failed');
        $response['language_version'] = $this->config->item('language_version');

        if ($this->valid_token) {
            $temp_visitor_id = $this->input->get('visitor_id');
            $this->temp_visitor->model_data = array('status' => 'leaved');
            $this->temp_visitor->update($temp_visitor_id);
            $response['result'] = 'success';
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }
        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
