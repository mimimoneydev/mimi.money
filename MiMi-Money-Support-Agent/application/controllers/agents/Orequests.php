<?php

class Orequests extends CP_AgentController {

    public function __construct() {
        parent::__construct();

        $this->data['title'] = $this->lang->line('agent_panel');
        $this->data['layout'] = 'agent_layout';
        $this->body_classes[] = 'agent-logged-in';
        $this->load->library('user_agent');
        $this->data['browser_name'] = strtolower($this->agent->browser());

        $this->addjs(theme_url("js/app/agents/header.js"), TRUE);
        $this->addjs(theme_url("js/app/agents/chat_list.js"), TRUE);
        $this->addjs(theme_url("js/app/agents/workroom.js"), TRUE);
        $this->addjs(theme_url("js/app/agents/anonymous.js"), TRUE);

        if ($this->data['browser_name'] == 'chrome') {
            $this->addjs(theme_url("js/push-notification.js"), TRUE);
        }

        $cannet_messages = $this->lang->load('canned-messages', 'english', TRUE);
        $this->settings->is_https = is_https();
        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings, 'canned' => $cannet_messages));
    }

    /*
     * To load view for chat history management
     */

    function index() {
        $cannet_messages = $this->lang->load('canned-messages', 'english', TRUE);

        // add users.js file
        $this->addjs(theme_url("js/app/agents/orequest.js"), TRUE);
        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings, 'canned' => $cannet_messages));

        //write code here to load view
        $this->bulid_layout("agents/orequests-list");
    }

    /*
     * this function will use to get offline request.
     * 
     * @return $requests (json array)
     */

    function get_requests() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $response['offline_requests'] = $this->orequest->get_offline_requests($this->current_user->id, $this->input->post('offset'), true);
            $response['result'] = 'success';
        } else {
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
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $request_id = $this->input->get('request_id');

            // mark notification as read
            $this->user->mark_notification_read(array('request_id' => $request_id, 'notification_type' => 'offline_request'));
            $conversations = $this->orequest->get_conversation($request_id);
            $response['conversations'] = $conversations;
            $response['result'] = 'success';
        } else {
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
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $request_id = $this->input->get('request_id');

            //check if data is valid or not
            $this->form_validation->set_rules($this->orequest->validation_rules['reply']);
            if ($this->form_validation->run() === true) {
                $this->orequest->model_data = $this->input->post(array('sender_id', 'message'));
                $this->orequest->model_data['request_id'] = $request_id;
                $this->orequest->model_data['message_status'] = 'unread';
                $this->orequest->model_data['conversation_type'] = 'reply';

                $request = $this->orequest->get_request_data($request_id);
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
                    $link = site_url('d=visitors&c=orequests&m=index&rid=' . convert_to_hash($request->id));
                    $data = array('link' => $link, 'name' => $request->name, 'sender' => $this->current_user, 'from_email' => $this->current_user->email, 'from_name' => $this->current_user->name, 'message' => $this->input->post('message'));
                    send_template_email($template_file, $to, $subject, $data);
                } else {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors();
            }
        } else {
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

    public function delete_request() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $request_id = $this->input->get('request_id');

            $this->orequest->delete_where(array('request_id' => $request_id), TABLE_OFFLINE_REQUEST_CONVERSATIONS);
            $this->orequest->delete_where(array('orequest_id' => $request_id), TABLE_OFFLINE_REQUEST_TAGS);

            if ($this->orequest->delete_where(array('id' => $request_id))) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('request_deleted');
            } else {
                $response['errors'] = $this->lang->line('process_error');
            }
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
