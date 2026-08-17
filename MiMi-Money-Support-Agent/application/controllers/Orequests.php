<?php

class Orequests extends CP_AdminController {

    public function __construct() {
        parent::__construct();

        // check has admin access
        $this->has_admin_access();
    }

    /*
     * To load view for chat history management
     */

    function index() {
        // add users.js file
        $this->addjs(theme_url("js/app/directives/modal-confirm.js"), TRUE);
        $this->addjs(theme_url("js/app/orequest.js"), TRUE);
        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings));

        //write code here to load view
        $this->bulid_layout("orequests/list");
    }

    /*
     * this function will use to get offline request.
     * 
     * @return $requests (json array)
     */

    function get_requests() {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $offline_requests = $this->orequest->get_admin_requests($this->input->post('offset'));
            $response['offline_requests'] = $offline_requests;
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will be use to get conversation of request
     * 
     * @return $conversations (json array)
     */

    public function get_conversations() {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $request_id = $this->input->get('id');
            $conversations = $this->orequest->get_conversation($request_id);
            $response['conversations'] = $conversations;
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will use to reply of online request
     * 
     * @param Post `sender_id`, `message`
     * 
     * @return $reply_id or error
     */

    function reply_request() {
        $response = array('error' => '', 'result' => 'failed');

        if ($this->current_user) {
            $request_id = $this->input->get('id');

            //check if data is valid or not
            $this->form_validation->set_rules($this->orequest->validation_rules['reply']);
            if ($this->form_validation->run() === true) {
                $this->orequest->model_data = $this->input->post(array('sender_id', 'message'));
                $this->orequest->model_data['request_id'] = $request_id;
                $this->orequest->model_data['message_status'] = 'unread';
                $this->orequest->model_data['conversation_type'] = 'reply';

                $request = $this->orequest->get_gequest_data($request_id);
                $reply_id = $this->orequest->reply_request($request_id);

                if ($request_id) {
                    $message_row = array();
                    $message_row['id'] = $request->id;
                    $message_row['senderId'] = $this->current_user->id;
                    $message_row['name'] = $this->current_user->name;
                    $message_row['email'] = $this->current_user->email;
                    $message_row['message'] = $this->input->post('message');
                    $message_row['profilePic'] = $this->media->get_thumbnail($this->current_user->profile_pic, PROFILE_PICS, $this->current_user->email);

                    $response['result'] = 'success';
                    $response['message_row'] = $message_row;

                    // send email to visitor
                    $template_file = 'request_reply';
                    $to = $request->email;
                    $subject = 'Reply your request';
                    $link = site_url('request/reply/' . $request->id);
                    $data = array('link' => $link, 'name' => $request->name, 'sender' => $this->current_user, 'from_email' => $this->current_user->email, 'from_name' => $this->current_user->name, 'message' => $this->input->post('message'));
                    send_template_email($template_file, $to, $subject, $data);
                } else {
                    $response['error'] = $this->lang->line('process_error');
                }
            } else {
                $response['error'] = validation_errors();
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To delete a request and its conversations
     * 
     * @return
     *      {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     */

    public function delete() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $request_id = $this->input->get('id');

            $this->orequest->delete_where(array('request_id' => $request_id), TABLE_OFFLINE_REQUEST_CONVERSATIONS);
            $this->orequest->delete_where(array('orequest_id' => $request_id), TABLE_OFFLINE_REQUEST_TAGS);

            if ($this->orequest->delete_where(array('id' => $request_id))) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('request_deleted');
            } else {
                $response['errors'] = $this->lang->line('process_error');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
