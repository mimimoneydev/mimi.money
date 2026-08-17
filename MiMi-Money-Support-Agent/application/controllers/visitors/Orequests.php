<?php

class Orequests extends CP_VisitorController {

    public function __construct() {
        parent::__construct();

        $this->data['layout'] = 'simple_layout';
    }

    /*
     * To load view for chat history management
     */

    function index() {
        $identifier = $this->input->get('rid');
        $request_id = convert_to_string($identifier);
        $request = $this->orequest->get_request_data($request_id);

        if ($request) {
            $this->addjs(theme_url("js/app/visitors/orequest.js"), TRUE);
            $this->add_js_inline(array('request' => $request));

            $this->bulid_layout("visitors/reply");
        } else {
            $this->bulid_layout("no-permission");
        }
    }

    /*
     * this function will use to get offline request.
     * 
     * @return $requests (json array)
     */

    function request() {
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            if ($this->settings->show_depaertment_selection_box == 'yes') {
                $this->chat_request->validation_rules['chat_request'][] = array(
                    'field' => 'requested_tag',
                    'label' => 'Department',
                    'rules' => 'required'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->chat_request->validation_rules['chat_request']);
            if ($this->form_validation->run() === true) {
                $chatbox = $this->session->userdata($this->access_token);
                $visited_pages = isset($chatbox['visited_pages']) ? $chatbox['visited_pages'] : array();
            
                // setup visitor data
                $this->user->model_data = $this->input->post(array('email', 'name')); 
                $this->user->model_data['display_name'] = $this->input->post('name');
                if($this->input->post('contact_number')) {
                    $this->user->model_data['contact_number'] = $this->input->post('contact_number');
                }             

                // check i visitor exists
                $visitor = $this->user->get_single(array('email' => $this->input->post('email')));
                if ($visitor) {
                    if (in_array($visitor->role, array('admin', 'agent'))) {
                        
                    } else {
                        $visitor->name = $this->input->post('name');
                        $visitor->display_name = $this->input->post('name');
                        
                        //updating visitor
                        $this->user->update($visitor->id);
                    }
                } else {
                    // inserting visitor
                    $this->user->model_data['role'] = 'visitor';
                    $visiter_id = $this->user->insert();
                    $visitor = $this->user->get_single(array('id' => $visiter_id));
                }
                
                // saving visitor site
                $this->users_sites->add_visitor_site($visitor->id, $this->current_site->id);

                $visitor->profilePic = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email);

                // setup request data
                $this->orequest->model_data['visitor_id'] = $visitor->id;
                $this->orequest->model_data['visitor_note'] = $this->input->post('message');
                $this->orequest->model_data['request_status'] = 'pending';
                $this->orequest->model_data['site_id'] = $this->current_site->id;
                
                // inserting visitor address in database
                $v_location = getLocationByIp($this->input->ip_address());
                if ($v_location and is_object($v_location)) {
                    $address = array();
                    $address['site_id'] = $this->current_site->id;
                    $address['user_id'] = $visitor->id;
                    $address['latitude'] = $v_location->geoplugin_latitude;
                    $address['longitude'] = $v_location->geoplugin_longitude;
                    $address['city'] = $v_location->geoplugin_city;
                    $address['state'] = $v_location->geoplugin_regionName;
                    $address['country'] = $v_location->geoplugin_countryName;
                    $address_id = $this->user->add_address($address);
                    
                    $this->orequest->model_data['address_id'] = $address_id;
                }
                
                $request_id = $this->orequest->create_request();

                if ($request_id) {
                    // will create visit info record
                    $visit_data = $this->input->post(array('page_title', 'page_url'));
                    $visit_data['meta_data'] = json_encode($visited_pages);
                    $visit_data['site_id'] = $this->current_site->id;
                    $visit_data['user_id'] = $visitor->id;
                    $visit_data['request_id'] = $request_id;
                    $visit_data['request_type'] = 'offline';
                    $visit_data['ip_address'] = $this->input->ip_address();
                    $visit_data['created_at'] = date("Y-m-d H:i:s", now());
                    $this->chat_session->createVisitInfo($visit_data);
                
                
                    $visitor->request_id = $request_id;
                    if ($this->input->post('requested_tag') != NULL) {
                        $this->orequest->model_data = array('orequest_id' => $request_id, 'tag_id' => $this->input->post('requested_tag'));
                        $this->orequest->insert_request_tag();
                    }

                    $this->__send_notifications($visitor, $this->input->post());
                    $response['result'] = 'success';
                } else {
                    $response['error'] = $this->lang->line('process_error');
                }
            } else {
                $response['error'] = validation_errors();
            }
        } else {
            $response['error'] = $this->lang->line('invalid_appkey');
        }
        
        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This proctect function will send notifications on mobile apps.
     * 
     * @param $visitor
     * @param $chat_session
     * 
     * @param $tag_id Id of department
     */

    protected function __send_notifications($visitor, $post_data) {
        $requested_tag = '';
        if (isset($post_data['requested_tag']) and $post_data['requested_tag']) {
            $requested_tag = $post_data['requested_tag'];
        }
        
        $operators = array();

        //get agents
        $agents = $this->orequest->get_agents($this->current_site->id, $requested_tag);

        foreach ($agents as $agent) { 
            $operators[] = $agent->id;
            
            // sending push notification.
            $message = array();
            $notifications = $this->user->get_notifications($agent->id);
            $message['notificationsCounter'] = count($notifications);
            $message['unreadSession'] = $this->chat_session->get_running_session($agent->id);
                    
            $message['type'] = 'offline_request';
            $message['senderId'] = $visitor->id;
            $message['name'] = $visitor->name;
            $message['email'] = $visitor->email;
            $message['profile_color'] = $visitor->profile_color;
            $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
            $message['message'] = $post_data['message'];
            $message['displayMessage'] = $this->lang->line('text_new_offline_request_notification');
            $message['requestID'] = $visitor->request_id;

            push_notification($agent->id, $message);
            
            // sending email to operators
            $template_file = 'new_offline_request';
            $to = $agent->email;
            $subject = $this->lang->line('text_new_offline_request_email_subject');

            $data = array(
                'operator' => $agent,
                'from_email' => $visitor->email,
                'from_name' => $visitor->name . ' - ' . $this->current_site->site_name,
                'site_name' => $this->current_site->site_name,
                'site_logo' => $this->current_site->site_logo
            );
            
            send_template_email($template_file, $to, $subject, $data);
        }
        
        // sending request to admins if can notify
        if ($this->settings->notify_all_requests_admin == 'yes') {
            // fetching all online agents.
            $admins = $this->user->get_all(array('role' => 'admin', 'user_status' => 'active'), 'id, name, email');
            foreach ($admins as $key => $admin) {
                if (!in_array($admin->id, $operators)) {
                    // sending email to operators
                    $template_file = 'new_offline_request';
                    $to = $admin->email;
                    $subject = $this->lang->line('text_new_offline_request_email_subject');

                    $data = array(
                        'operator' => $admin,
                        'from_email' => $visitor->email,
                        'from_name' => $visitor->name . ' - ' . $this->current_site->site_name,
                        'site_name' => $this->current_site->site_name,
                        'site_logo' => $this->current_site->site_logo
                    );

                    send_template_email($template_file, $to, $subject, $data);
                }
            }
        }
    }

    /*
     * This function will be use to get conversation of request
     * 
     * @param $request_id
     * 
     * @return $conversations (json array)
     */

    public function get_conversations($request_id) {
        $conversations = $this->orequest->get_conversation($request_id);

        return $this->output->set_content_type('application/json')->set_output($this->return_json($conversations));
    }

    /*
     * This function will use to reply of online request
     * 
     * @param $request_id
     * 
     * @param Post `sender_id`, `message`
     * 
     * @return $reply_id or error
     */

    function reply_request($request_id) {
        $response = array('error' => '', 'result' => 'failed');
        
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
                $message_row['senderId'] = $request->uid;
                $message_row['name'] = $request->name;
                $message_row['email'] = $request->email;
                $message_row['message'] = $this->input->post('message');
                $message_row['profilePic'] = $this->media->get_thumbnail($request->profile_pic, PROFILE_PICS, $request->email);

                $response['result'] = 'success';
                $response['message_row'] = $message_row;

                $agents = $this->orequest->get_request_agents($request);
                foreach ($agents as $agent) {
                    // insertin notification in database.
                    $notification_data = array();
                    $notification_data['notification_type'] = 'offline_reply';
                    $notification_data['request_id'] = $request->id;
                    $notification_data['receiver_id'] = $agent->sender_id;
                    $notification_data['message'] = $this->input->post('message');
                    $notification_data['display_message'] = $request->name . " replied you.";
                    $notification_data['sender_id'] = $request->uid;
                    $notification_data['notification_status'] = 'unread';
                    $this->chat_request->insert_notification($notification_data);
                    
                    // sending push notification
                    $message = array();
                    $notifications = $this->user->get_notifications($agent->sender_id);
                    $message['notificationsCounter'] = count($notifications);
                    $message['unreadSession'] = $this->chat_session->get_running_session($agent->sender_id);

                    $message['type'] = 'offline_reply';
                    $message['senderId'] = $request->uid;
                    $message['name'] = $request->name;
                    $message['email'] = $request->email;
                    $message['profilePic'] = $this->media->get_thumbnail($request->profile_pic, PROFILE_PICS, $request->email, '404');
                    $message['message'] = $this->input->post('message');
                    $message['displayMessage'] = $request->name . $this->lang->line('request_reply');
                    $message['requestID'] = $request->id;
                    $message['replyID'] = $reply_id;

                    push_notification($agent->sender_id, $message);
                }
            } else {
                $response['error'] = $this->lang->line('process_error');
            }
        } else {
            $response['error'] = validation_errors();
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
