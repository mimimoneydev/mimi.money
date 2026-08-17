<?php

class Chat extends CP_AgentController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();

        $this->data['title'] = $this->lang->line('agent_panel');
        $this->data['layout'] = 'agent_layout';
        $this->body_classes[] = 'agent-logged-in';

        $this->addjs(theme_url("js/app/agents/header.js"), TRUE);
        $this->addjs(theme_url("js/app/agents/chat_list.js"), TRUE);
        $this->addjs(theme_url("js/app/agents/workroom.js"), TRUE);
        $this->add_js_inline(array('user' => $this->current_user));
    }

    /*
     * This function will use to get all recents chats.
     * 
     * @return json data of requests
     */

    function index() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            // getting recent chat list
            $chat_list = $this->chat_session->get_recent_chats($this->current_user->id);

            $response['result'] = 'success';
            $response['data'] = array('chatListData' => $chat_list);
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will use to get all closed chats.
     * 
     * @return json data of requests
     */

    function closed() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $excepts = $this->input->post('excepts');
            // getting closed chat list
            $closed_chats = $this->chat_session->get_closed_chats($this->current_user->id, $excepts, 15);

            $response['result'] = 'success';
            $response['data'] = array('closed_chats' => $closed_chats);
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will return cureent server time.
     * 
     * @return milliseconds
     */

    function get_server_time() {
        $millitime = $this->get_time_miliseconds();
        $response = array('result' => 'success', 'milliseconds' => $millitime);

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will call to accept a request
     * 
     * @return result and status
     */

    function accept_request() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $request_id = $this->input->get('request_id');
            $chat_session_id = $this->input->get('csid');

            // mark notification as read
            $this->user->mark_notification_read(array('chat_session_id' => $chat_session_id, 'notification_type' => 'online_request'));

            // get chat session
            $chat_session = $this->chat_session->get_single(array('id' => $chat_session_id));
            if ($chat_session->session_status == 'requested' or $chat_session->session_status == 'forward') {
                $request = $this->chat_request->get_single(array('id' => $request_id));
                if ($request->request_status == 'pending') {
                    $this->chat_request->accept_request($request);

                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('request_accepted');
                } else {
                    $response['errors'] = $this->lang->line('request_expired');
                }
            } else {
                $response['errors'] = $this->lang->line('request_expired');
                $this->close_requests($chat_session_id);
            }
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function calls to check session exists or not
     * 
     * @return json object with result
     */

    public function get_session() {
        $response = array( 'result' => 'failed');
        
        if ($this->current_user) {
            $chat_session_id = $this->input->get('csid');

            $messages = array();
            $visitor = '';
            $chat_session = '';

            $workroomHistory = $this->session->userdata('workroomHistory');
            if ($workroomHistory and isset($workroomHistory[$chat_session_id]) and isset($workroomHistory[$chat_session_id]['chatHistory']) and $workroomHistory[$chat_session_id]['chatHistory']) {
                $messages = $workroomHistory[$chat_session_id]['chatHistory'];
                $visitor = $workroomHistory[$chat_session_id]['visitor'];
                $chat_session = $workroomHistory[$chat_session_id]['chat_session'];
                $last_id = $workroomHistory[$chat_session_id]['last_id'];
                $message_stored = $workroomHistory[$chat_session_id]['message_stored'];
            } else {
                //if (!$workroomHistory) {
                $workroomHistory = array();
                //}

                $chatHistory = $message_stored = array();
                $last_id = 0;

                $messages_list = $this->chat_message->get_chat_messages($chat_session_id);
                foreach ($messages_list as $key => $row) {
                    $chat_row = array();
                    $last_id = $row->id;
                    $chat_row['id'] = $row->id;
                    $chat_row['name'] = $row->name;
                    $chat_row['profile_color'] = $row->profile_color;
                    $chat_row['profile_pic'] = $row->profile_pic;
                    $chat_row['profilePic'] = $this->media->get_thumbnail($row->profile_pic, PROFILE_PICS, $row->email);

                    if($row->message_type == 'file') {
                        $file_object = json_decode($row->message_meta);
                        $chat_row['chat_message'] = file_link($row->chat_message, $file_object->filesize, $file_object->filename);
                    } else {
                        $chat_row['chat_message'] = $row->chat_message;
                    }

                    $chat_row['sort_order'] = $row->sort_order;
                    $chat_row['message_status'] = $row->message_status;
                    $chat_row['sender_id'] = $row->sender_id;
                    $chat_row['class'] = '';

                    if (!in_array($row->id, $message_stored)) {
                        $message_stored[] = $row->id;
                        $chatHistory[] = $chat_row;
                    }

                    $chat_row['class'] = 'new-message';
                    $messages[$key] = $chat_row;
                }

                if ($last_id > 0) {
                    $this->chat_message->mark_message_read($chat_session_id, $last_id);
                }

                $visitor = $this->chat_user->get_visitor($chat_session_id);

                $chat_session = $this->chat_session->get_single(array('id' => $chat_session_id));

                // storing chat history in session
                $workroomHistory[$chat_session_id] = array(
                    'chatHistory' => $chatHistory,
                    'last_id' => $last_id,
                    'visitor' => $visitor,
                    'chat_session' => $chat_session,
                    'message_stored' => $message_stored
                );
                $this->session->set_userdata('workroomHistory', $workroomHistory);
            }

            //set visiter login status
            $online_users = $this->chat_user->get_online_users();
            $visitor->status = (isset($online_users[$visitor->id])) ? 'online' : 'offline';

            $lang_labels = array(
                'visitor_left_chat' => $this->lang->line('visitor_left_chat')
            );

            $response['result'] = 'success';
            $response['data'] = array(
                'chatHistory' => $messages,
                'visitor' => $visitor,
                'chat_session' => $chat_session,
                'last_id' => $last_id,
                'message_stored' => $message_stored,
                'labels' => $lang_labels
            );
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This the chatHeartbeat of chatting.
     * 
     * @return chating data 
     */

    function chat_heartbeat() {
        $response = array('result' => 'failed');
        
        if ($this->current_user) {
            $chat_session_id = $this->input->get('csid');
            $last_id = $this->input->get('last_message_id');
            $is_typing = $this->input->get('typing');

            $workroomHistory = $this->session->userdata('workroomHistory');
            $chatHistory = $workroomHistory[$chat_session_id]['chatHistory'];
            $visitor = $workroomHistory[$chat_session_id]['visitor'];
            $chat_session = $workroomHistory[$chat_session_id]['chat_session'];
            $message_stored = $workroomHistory[$chat_session_id]['message_stored'];

            // updating typing status.
            $this->chat_user->model_data = array('user_id' => $this->current_user->id, 'chat_session_id' => $chat_session->id);
            $this->chat_user->update_typing_status($is_typing);

            // mark notification as read
            $this->user->mark_notification_read(array('chat_session_id' => $chat_session_id, 'notification_type' => 'message'));

            $messages = array();

            $messages_list = $this->chat_message->get_chat_messages($chat_session_id, $last_id);
            foreach ($messages_list as $key => $row) {
                $chat_row = array();
                $last_id = $row->id;
                $chat_row['id'] = $row->id;
                $chat_row['name'] = $row->name;
                $chat_row['profile_color'] = $row->profile_color;
                $chat_row['profile_pic'] = $row->profile_pic;
                $chat_row['profilePic'] = $this->media->get_thumbnail($row->profile_pic, PROFILE_PICS, $row->email);
                if($row->message_type == 'file') {
                    $file_object = json_decode($row->message_meta);
                    $chat_row['chat_message'] = file_link($row->chat_message, $file_object->filesize, $file_object->filename);
                } else {
                    $chat_row['chat_message'] = $row->chat_message;
                }

                $chat_row['sort_order'] = $row->sort_order;
                $chat_row['message_status'] = $row->message_status;
                $chat_row['sender_id'] = $row->sender_id;
                $chat_row['class'] = '';

                if (!in_array($row->id, $message_stored)) {
                    $message_stored[] = $row->id;
                    $chatHistory[] = $chat_row;
                }

                $chat_row['class'] = 'new-message';
                $messages[$key] = $chat_row;
            }

            if ($last_id > 0) {
                $this->chat_message->mark_message_read($chat_session_id, $last_id);
            }

            $chat_session = $this->chat_session->get_single(array('id' => $chat_session_id));
            $visitor = $this->chat_user->get_visitor($chat_session_id);

            //set visiter login status
            $online_users = $this->chat_user->get_online_users();
            $visitor->status = (isset($online_users[$visitor->id])) ? 'online' : 'offline';

            // storing chat history in session
            $workroomHistory[$chat_session_id] = array(
                'chatHistory' => $chatHistory,
                'last_id' => $last_id,
                'visitor' => $visitor,
                'chat_session' => $chat_session,
                'message_stored' => $message_stored
            );
            $this->session->set_userdata('workroomHistory', $workroomHistory);

            $response['result'] = 'success';
            $response['last_id'] = $last_id;
            $response['visitor'] = $visitor;
            $response['chat_session'] = $chat_session;
            $response['chatMessagesData'] = $messages;
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
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
        
        if ($this->current_user) {
            //check if data is valid or not
            $this->form_validation->set_rules($this->chat_message->validation_rules['operator_message']);

            if ($this->form_validation->run() === true) {
                $chat_session_id = $this->input->post('csid');
                
                $this->chat_message->model_data = $this->input->post(array('chat_message', 'message_status', 'sender_id', 'sort_order'));
                $local_id = 'awb' . $this->input->post('sender_id') . $this->input->post('sort_order');
                $this->chat_message->model_data['local_id'] = $local_id;
                $this->chat_message->model_data['chat_session_id'] = $chat_session_id;

                $message_id = $this->chat_message->add_message($local_id);
                if ($message_id and is_object($message_id) === false) {
                    $workroomHistory = $this->session->userdata('workroomHistory');
                    $chatHistory = $workroomHistory[$chat_session_id]['chatHistory'];
                    $visitor = $workroomHistory[$chat_session_id]['visitor'];
                    $chat_session = $workroomHistory[$chat_session_id]['chat_session'];
                    $last_id = $workroomHistory[$chat_session_id]['last_id'];

                    $this->chat_message->model_data['id'] = $message_id;
                    $this->chat_message->model_data['name'] = $this->current_user->name;
                    $this->chat_message->model_data['profile_color'] = $this->current_user->profile_color;
                    $this->chat_message->model_data['profile_pic'] = $this->current_user->profile_pic;
                    $this->chat_message->model_data['profilePic'] = $this->current_user->profile_picture;

                    $chatHistory[] = $this->chat_message->model_data;

                    $sender = $this->user->get_single(array('id' => $this->input->post('sender_id')));
                    // sending push notification
                    $message = array();
                    $notifications = $this->user->get_notifications($sender->id);
                    $message['notificationsCounter'] = count($notifications);
                    $message['unreadSession'] = $this->chat_session->get_running_session($sender->id);

                    $message['type'] = 'message';
                    $message['senderId'] = $sender->id;
                    $message['name'] = $visitor->name;
                    $message['profile_color'] = $visitor->profile_color;
                    $message['profilePic'] = $this->media->get_thumbnail($sender->profile_pic, PROFILE_PICS, $sender->email, '404');
                    $message['message'] = $this->input->post('chat_message');
                    $message['message_type'] = 'text';
                    $message['message_meta'] = '';
                    $message['displayMessage'] = sprintf($this->lang->line('get_new_message'), $sender->name);
                    $message['chatSessionId'] = $chat_session_id;
                    $message['sortOrder'] = $this->input->post('sort_order');
                    $message['messageId'] = $message_id;

                    push_notification($sender->id, $message);

                    $response['result'] = 'success';
                    $response['message_row'] = $this->chat_message->model_data;
                } else {
                    //$response['errors'] = $this->lang->line('message_enterd');
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
     * Upload user file and add in chat
     */

    function upload_file() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $chatbox = $this->session->userdata($this->access_token);

            //check if data is valid or not
            $this->form_validation->set_rules($this->chat_message->validation_rules['new_base64file']);

            if ($this->form_validation->run() === true) {
                $extension = strtolower(strrchr($this->input->post('filename'), '.'));
                $allowed_filetypes = explode("|", str_replace(" ", "", $this->settings->agent_allowed_filetypes));

                if($this->input->post('filesize') <= ($this->settings->agent_file_upload_size * 1024) and in_array($extension, $allowed_filetypes)) {
                    $chat_session_id = $this->input->post('csid');

                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(CHATFILES_DIR);
                    $file_prefix = $chat_session_id . date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $uploaded_fileurl = upload_url(CHATFILES_DIR . $filedata['filename']);
                        $this->chat_message->model_data = $this->input->post(array('message_status', 'sender_id', 'sort_order'));
                        $local_id = 'awb' . $this->input->post('sender_id') . $this->input->post('sort_order');
                        $this->chat_message->model_data['local_id'] = $local_id;
                        $this->chat_message->model_data['chat_session_id'] = $chat_session_id;
                        $this->chat_message->model_data['message_type'] = 'file';

                        $chat_message = file_link($uploaded_fileurl, $base64_array['filesize'], $base64_array['filename']);
                        $this->chat_message->model_data['chat_message'] = $uploaded_fileurl;
                        $this->chat_message->model_data['message_meta'] = json_encode($this->input->post(array('filename', 'filetype', 'filesize')));

                        $message_id = $this->chat_message->add_message($local_id);
                        if ($message_id and is_object($message_id) === false) {
                            $workroomHistory = $this->session->userdata('workroomHistory');
                            $chatHistory = $workroomHistory[$chat_session_id]['chatHistory'];
                            $visitor = $workroomHistory[$chat_session_id]['visitor'];
                            $chat_session = $workroomHistory[$chat_session_id]['chat_session'];
                            $last_id = $workroomHistory[$chat_session_id]['last_id'];

                            $this->chat_message->model_data['id'] = $message_id;
                            $this->chat_message->model_data['chat_message'] = $chat_message;
                            $this->chat_message->model_data['name'] = $this->current_user->name;
                            $this->chat_message->model_data['profile_color'] = $this->current_user->profile_color;
                            $this->chat_message->model_data['profile_pic'] = $this->current_user->profile_pic;
                            $this->chat_message->model_data['profilePic'] = $this->current_user->profile_picture;

                            $chatHistory[] = $this->chat_message->model_data;

                            $sender = $this->user->get_single(array('id' => $this->input->post('sender_id')));
                            // sending push notification
                            $message = array();
                            $notifications = $this->user->get_notifications($sender->id);
                            $message['notificationsCounter'] = count($notifications);
                            $message['unreadSession'] = $this->chat_session->get_running_session($sender->id);

                            $message['type'] = 'message';
                            $message['senderId'] = $sender->id;
                            $message['name'] = $visitor->name;
                            $message['profile_color'] = $visitor->profile_color;
                            $message['profilePic'] = $this->media->get_thumbnail($sender->profile_pic, PROFILE_PICS, $sender->email, '404');
                            $message['message'] = $uploaded_fileurl;
                            $message['message_type'] = 'file';
                            $message['message_meta'] = $this->chat_message->model_data['message_meta'];
                            $message['displayMessage'] = sprintf($this->lang->line('get_new_message'), $sender->name);
                            $message['chatSessionId'] = $chat_session_id;
                            $message['sortOrder'] = $this->input->post('sort_order');
                            $message['messageId'] = $message_id;

                            push_notification($sender->id, $message);

                            $response['result'] = 'success';
                            $response['message_row'] = $this->chat_message->model_data;
                        }
                    }
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
     * This function will return date for forward chat.
     * Data will be departments list and agents list.
     * 
     * @return $data (lists of agent and departments)
     */

    public function get_forward_data() {
        $response = array('error' => '', 'result' => 'failed');

        if ($this->current_user) {
            $chat_session_id = $this->input->get('csid');
            $chat_session = $this->chat_session->get_single(array('id' => $chat_session_id));
            $agents_list = $this->chat_session->get_available_agents($chat_session);
            $departments_list = $this->chat_session->get_available_departments($chat_session);

            $response['data'] = array('agents_list' => $agents_list, 'departments_list' => $departments_list);
            $response['result'] = 'success';
        } else {
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Function for forward_chat
     * 
     * @param Post = to, agents, department
     * 
     * @return $result type and status
     */

    public function forward_chat() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $chat_session_id = $this->input->get('csid');

            if (isset($_POST['to'])) {
                if ($_POST['to'] == 'agents') {
                    $this->chat_request->validation_rules['forward'][] = array(
                        'field' => 'agents[]',
                        'label' => 'Agents',
                        'rules' => 'required'
                    );
                } elseif ($_POST['to'] == 'departments') {
                    $this->chat_request->validation_rules['forward'][] = array(
                        'field' => 'department',
                        'label' => 'Department',
                        'rules' => 'required'
                    );
                }
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->chat_request->validation_rules['forward']);
            if ($this->form_validation->run() === true and $this->session->userdata('current_user_id')) {
                $workroomHistory = $this->session->userdata('workroomHistory');
                $visitor = $workroomHistory[$chat_session_id]['visitor'];
                $this->chat_request->model_data['chat_session_id'] = $chat_session_id;
                $this->chat_request->model_data['request_type'] = 'forward';
                $this->chat_request->model_data['message'] = sprintf($this->lang->line('get_forwarded_request_from'), $this->current_user->name);

                if ($_POST['to'] == 'agents') {
                    $this->chat_request->model_data['agent_ids'] = implode(",", $this->input->post('agents'));
                    $output = $this->chat_request->forward_to_agents($this->current_user->id, $visitor->id);
                    if ($output) {
                        $response['result'] = 'success';
                        $response['message'] = $this->lang->line('chat_forwarded');
                    } else {
                        $response['error'] = $this->lang->line('process_error');
                    }
                } elseif ($_POST['to'] == 'departments') {
                    $this->chat_request->model_data['department_id'] = $this->input->post('department');
                    $output = $this->chat_request->forward_to_deprtments($this->current_user->id, $visitor->id);

                    if ($output) {
                        $response['result'] = 'success';
                        $response['message'] = $this->lang->line('chat_forwarded');
                    } else {
                        $response['error'] = $this->lang->line('process_error');
                    }
                }

                if ($response['result'] == 'success') {
                    $chatHistory = $workroomHistory[$chat_session_id]['chatHistory'];
                    $visitor = $workroomHistory[$chat_session_id]['visitor'];
                    $chat_session = $workroomHistory[$chat_session_id]['chat_session'];
                    $last_id = $workroomHistory[$chat_session_id]['last_id'];

                    $chat_session->session_status = 'forward';
                    $response['chat_session'] = $chat_session;

                    // storing chat history in session
                    $workroomHistory[$chat_session_id] = array('chatHistory' => $chatHistory, 'last_id' => $last_id, 'visitor' => $visitor, 'chat_session' => $chat_session);
                    $this->session->set_userdata('workroomHistory', $workroomHistory);
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
     * This function will end (close) chat.
     * 
     * @param Post params = chat_session_id
     * 
     * @return $error or $message
     */

    function end() {
        $response = array('result' => 'failed');
        
        if ($this->current_user) {
            $chat_session_id = $this->input->get('csid');

            $this->chat_session->model_data['session_status'] = 'closed';
            if ($this->chat_session->update_chat_session($chat_session_id)) {
                $workroomHistory = $this->session->userdata('workroomHistory');
                $chatHistory = $workroomHistory[$chat_session_id]['chatHistory'];
                $visitor = $workroomHistory[$chat_session_id]['visitor'];
                $chat_session = $workroomHistory[$chat_session_id]['chat_session'];
                $last_id = $workroomHistory[$chat_session_id]['last_id'];

                $chat_session->session_status = 'closed';

                // storing chat history in session
                $workroomHistory[$chat_session_id] = array('chatHistory' => $chatHistory, 'last_id' => $last_id, 'visitor' => $visitor, 'chat_session' => $chat_session);
                $this->session->set_userdata('workroomHistory', $workroomHistory);

                $response['result'] = 'success';
                $response['chat_session'] = $chat_session;
                $response['message'] = $this->lang->line('chat_closed');
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
