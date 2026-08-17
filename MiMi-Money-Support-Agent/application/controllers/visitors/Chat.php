<?php

/*
 * Controller to manage all caht related actions
 * Author: Pukhraj Prajapat (pukhraj.prajapat@g-axon.com)
 */

class Chat extends CP_VisitorController {
    /*
     * this function will return configuration.
     *  
     * @return all chat list or error if any.
     */

    function index() {
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            $response['result'] = 'success';
            $response['settings'] = $this->settings;
        } else {
            $response['error'] = $this->lang->line('invalid_token');
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
     * This function will return redirect url
     */

    private function __get_redirect_url() {
        $referer_url = $this->session->userdata('referer_url');
        if ($referer_url) {
            return $referer_url;
        }

        $chatbox = $this->session->userdata($this->access_token);
        if (isset($chatbox['browser_session'])) {
            $browser_session = $chatbox['browser_session'];
            return end($browser_session);
        }

        return false;
    }

    /*
     * To track visitor activity
     */

    private function __track_visitor() {
        $chatbox = $this->session->userdata($this->access_token);

        // tracking page and visitors
        $browser_session = isset($chatbox['browser_session']) ? $chatbox['browser_session'] : NULL;
        $pagevieData = $this->input->post(array('page_url', 'page_title'));
        $pagevieData['site_id'] = $this->current_site->id;
        if ($browser_session) {
            if (!in_array($this->input->post('page_url'), $browser_session)) {
                $this->user->increase_pagevies_counter($pagevieData);
                $browser_session[] = $this->input->post('page_url');
            }
        } else {
            // increasing visitor counter
            $this->user->increase_visitors_counter($this->current_site->id);
            $this->user->increase_pagevies_counter($pagevieData);

            $browser_session = array($this->input->post('page_url'));
        }

        $chatbox['browser_session'] = $browser_session;
        $this->session->set_userdata($this->access_token, $chatbox);

        return TRUE;
    }

    /*
     * Check any agent is online right now
     */

    private function __is_agents_online() {
        // getting online agents
        if ($this->settings->chat_mode == 'online') {
            $this->user->user_select_fields = " count(*) as total";
            $online_agents = $this->user->hasOnlineAgents($this->current_site->id);
            if ($online_agents->total > 0) {
                return TRUE;
            }
        }

        return FALSE;
    }

    /*
     * To update visitor last activity time
     */

    private function __update_last_activity() {
        if ($this->valid_token) {
            // updating last activity
            $chatbox = $this->session->userdata($this->access_token);
            $visitor = $chatbox['visitor'];

            if ($visitor) {
                // updating last activity time
                $this->user->model_data = array('last_activity_time' => date("Y-m-d H:i:s", now()));
                $this->user->update($visitor->id);
            }
        }
    }

    /*
     * To get current window agent
     */

    private function __get_window_agent() {
        // gettign user agent (browser, mobiel or robot)
        $this->load->library('user_agent');
        if ($this->agent->is_mobile()) {
            $user_agent = 'mobile';
        } elseif ($this->agent->is_robot()) {
            $user_agent = 'robot';
        } elseif ($this->agent->is_browser()) {
            $user_agent = 'browser';
        } else {
            $user_agent = 'Unidentified';
        }

        return $user_agent;
    }

    /*
     * this function will return online agent.
     * 
     * @return $online_agents (array of online agents)
     */

    public function get_online_agents() {
        $response = array('error' => '', 'result' => 'failed', 'is_agents_online' => FALSE);
        $this->forceToActivityUpdate();
        
        if ($this->valid_token) {
            // checking is agents online or not
            if ($this->settings->enable_specific_agent_request == 'yes') {
                $online_agents = $this->user->get_online_agents_list($this->current_site->id);
                $response['is_agents_online'] = (count($online_agents) > 0) ? TRUE : FALSE;
                $response['agents_list'] = $online_agents;
            } else {
                $response['is_agents_online'] = $this->__is_agents_online();
                $response['agents_list'] = array();
            }

            if ($this->settings->enable_agent_initiate_chats == 'yes') {
                $chatbox = $this->session->userdata($this->access_token);
                $anonymous_visitor = (isset($chatbox['anonymous_visitor'])) ? $chatbox['anonymous_visitor'] : '';

                if ($anonymous_visitor and $anonymous_visitor->id) {
                    $activity_time = date("Y-m-d H:i:s", now());
                    $this->temp_visitor->model_data = array('last_activity_time' => $activity_time);
                    $this->temp_visitor->update($anonymous_visitor->id);
                    $anonymous_visitor = $this->temp_visitor->get_visitor($anonymous_visitor->id);
                    $response['data'] = array('anonymous_visitor' => $anonymous_visitor);

                    // get all message of visitor
                    $messages = $this->temp_visitor->get_messages(array('temp_visitor_id' => $anonymous_visitor->id));
                    $response['data']['anonymous_messages'] = $messages;

                    $chatbox['anonymous_visitor'] = $anonymous_visitor;
                    $this->session->set_userdata($this->access_token, $chatbox);
                }
            }

            $response['result'] = 'success';
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function calls to check session exists or not
     * 
     * @return json object with result
     */

    public function get_session() {
        $response = array('error' => '', 'result' => 'failed', 'redirect_to' => '', 'settings' => '', 'data' => array());
        $this->forceToActivityUpdate();
        
        if ($this->valid_token) {
            // tracking visitor
            $this->__track_visitor();

            // closing closed chat sessions
            $this->user->closeClosedSessions();

            $chatbox = $this->session->userdata($this->access_token);
            $visited_pages = isset($chatbox['visited_pages']) ? $chatbox['visited_pages'] : array();
            $visited_pages = $this->__get_visited_pages($visited_pages, $this->input->post(array('page_title', 'page_url')));

            $response['tags'] = $this->configuration->get_site_departments(array($this->current_site->id));
            $response['settings'] = $this->settings;
            $response['user_agent'] = $this->__get_window_agent();
            $response['redirect_to'] = $this->__get_redirect_url();

            if ($this->settings->enable_specific_agent_request == 'yes') {
                $online_agents = $this->user->get_online_agents_list($this->current_site->id);
                $response['is_agents_online'] = (count($online_agents) > 0) ? TRUE : FALSE;
                $response['agents_list'] = $online_agents;
            } else {
                $response['is_agents_online'] = $this->__is_agents_online();
                $response['agents_list'] = array();
            }

            if (isset($chatbox['visitor_id']) and $chatbox['visitor_id']) {
                $chatHistory = $chatbox['chatHistory'];
                $chat_session = $chatbox['chat_session'];
                $visitor = $chatbox['visitor'];
                $agent = $chatbox['agent'];
                $show_feedback_form = isset($chatbox['show_feedback_form']) ? $chatbox['show_feedback_form'] : 'no';
                $minimized = isset($chatbox['minimized']) ? $chatbox['minimized'] : 'no';
                $message_stored = $chatbox['message_stored'];
                $last_id = $chatbox['last_id'];

                // will update visit info record
                $visit_data = $this->input->post(array('page_title', 'page_url'));
                $visit_data['meta_data'] = json_encode($visited_pages);
                $visit_data['ip_address'] = $this->input->ip_address();

                // where condition array
                $where = array();
                $where['user_id'] = $visitor->id;
                $where['request_id'] = $chat_session->id;
                $where['request_type'] = 'online';

                $this->chat_session->updateVisitInfo($visit_data, $where);

                if (empty($chatHistory)) {
                    $chatHistory = array();
                }

                $response['result'] = 'success';
                $response['data'] = array(
                    'visitor' => $visitor,
                    'agent' => $agent,
                    'chat_session' => $chat_session,
                    'minimized' => $minimized,
                    'show_feedback_form' => $show_feedback_form,
                    'chatHistory' => $chatHistory,
                    'last_id' => $last_id,
                    'message_stored' => $message_stored
                );

                $chatbox['visited_pages'] = $visited_pages;
                $this->session->set_userdata($this->access_token, $chatbox);
            } else {
                $response['result'] = 'no-session';
                $response['data'] = array('minimized' => (isset($chatbox['minimized']) and $chatbox['minimized']) ? $chatbox['minimized'] : 'no');

                if ($this->settings->enable_agent_initiate_chats == 'yes') {
                    $anonymous_visitor = (isset($chatbox['anonymous_visitor'])) ? $chatbox['anonymous_visitor'] : '';

                    $this->temp_visitor->model_data = $this->input->post(array('page_title', 'page_url'));
                    $this->temp_visitor->model_data['meta_data'] = json_encode($visited_pages);
                    $this->temp_visitor->model_data['status'] = 'present';
                    $this->temp_visitor->model_data['last_activity_time'] = date("Y-m-d H:i:s", now());

                    if ($anonymous_visitor and $anonymous_visitor->id) {
                        $this->temp_visitor->update($anonymous_visitor->id);
                        $anonymous_visitor_id = $anonymous_visitor->id;
                    } else {
                        $ip_address = ($this->input->ip_address() == '::1') ? '47.9.135.154' : $this->input->ip_address();
                        $geo_location = getLocationByIp($ip_address);
                        if ($geo_location and is_object($geo_location)) {
                            $siteuser = $this->input->post('siteuser');

                            $geocity = ($geo_location->geoplugin_city) ? $geo_location->geoplugin_city : $geo_location->geoplugin_countryName;
                            $this->temp_visitor->model_data['name'] = (isset($siteuser['name']) and $siteuser['name']) ? $siteuser['name'] : $geocity;
                            $this->temp_visitor->model_data['email'] = (isset($siteuser['email']) and $siteuser['email']) ? $siteuser['email'] : '';
                            $this->temp_visitor->model_data['message'] = (isset($siteuser['message']) and $siteuser['message']) ? $siteuser['message'] : 'Hi';

                            $this->temp_visitor->model_data['ip_address'] = $ip_address;
                            $this->temp_visitor->model_data['site_id'] = $this->current_site->id;
                            $this->temp_visitor->model_data['latitude'] = $geo_location->geoplugin_latitude;
                            $this->temp_visitor->model_data['longitude'] = $geo_location->geoplugin_longitude;
                            $this->temp_visitor->model_data['city'] = $geo_location->geoplugin_city;
                            $this->temp_visitor->model_data['state'] = $geo_location->geoplugin_regionName;
                            $this->temp_visitor->model_data['country'] = $geo_location->geoplugin_countryName;
                            $anonymous_visitor_id = $this->temp_visitor->insert();
                        }
                    }

                    if (isset($anonymous_visitor_id) and $anonymous_visitor_id) {
                        $anonymous_visitor = $this->temp_visitor->get_visitor($anonymous_visitor_id);
                        $response['data']['anonymous_visitor'] = $anonymous_visitor;

                        // get all message of visitor
                        $messages = $this->temp_visitor->get_messages(array('temp_visitor_id' => $anonymous_visitor_id));
                        $response['data']['anonymous_messages'] = $messages;

                        $chatbox['anonymous_visitor'] = $anonymous_visitor;
                        $chatbox['visited_pages'] = $visited_pages;
                        $this->session->set_userdata($this->access_token, $chatbox);
                    } else {
                        $chatbox['visited_pages'] = $visited_pages;
                        $this->session->set_userdata($this->access_token, $chatbox);
                    }
                }
            }
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Get visited pages 
     * 
     * @param Array $visited_pages
     * @param Array $new_page
     * 
     * @return Array $visited_pages
     */

    private function __get_visited_pages($visited_pages = array(), $new_page = array()) {
        if ($new_page and isset($new_page['page_url'])) {
            $page_exists = false;
            foreach ($visited_pages as $page) {
                if ($page['page_url'] == $new_page['page_url']) {
                    $page_exists = true;
                }
            }

            if ($page_exists === false) {
                $visited_pages[] = $new_page;
            }
        }

        return $visited_pages;
    }

    /*
     * This function create a chat request on server.
     * 
     * $param Post name,email,message
     * @return 
     */

    public function request() {
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {

            if ($this->settings->show_depaertment_selection_box == 'yes' and $this->settings->initiate_bypass_chat == 'no') {
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

                $this->session->unset_userdata($this->access_token);
                $chatbox = array();
                $chatbox['visited_pages'] = $visited_pages;

                $ip_address = ($this->input->ip_address() == '::1') ? '47.9.135.154' : $this->input->ip_address();
                $v_location = getLocationByIp($ip_address);

                // setup visitor data
                $this->user->model_data = $this->input->post(array('email', 'name'));
                $this->user->model_data['display_name'] = $this->input->post('name');
                if($this->input->post('contact_number')) {
                    $this->user->model_data['contact_number'] = $this->input->post('contact_number');
                }

                if ($v_location and is_object($v_location) and $this->input->post('initiate_bypass_chat') and $this->input->post('initiate_bypass_chat') == 'yes') {
                    $geocity = ($v_location->geoplugin_city) ? $v_location->geoplugin_city : $v_location->geoplugin_countryName;

                    $this->user->model_data['name'] = $geocity;
                    $this->user->model_data['display_name'] = $geocity;
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

                // storing visitor in session
                $chatbox['visitor_id'] = $visitor->id;
                $chatbox['visitor'] = $visitor;

                // creating user chat request.
                $this->chat_session->model_data['session_type'] = 'public';
                $this->chat_session->model_data['session_status'] = 'requested';
                $this->chat_session->model_data['site_id'] = $this->current_site->id;
                $this->chat_session->model_data['user_agent'] = $this->input->user_agent();

                $requested_tag = 0;
                if ($this->input->post('requested_tag') != NULL) {
                    $requested_tag = $this->chat_session->model_data['requested_tag'] = $this->input->post('requested_tag');
                }

                // creating session and visitor
                $visitorData = array('user_id' => $visitor->id);
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

                    $visitorData['address_id'] = $address_id;
                }

                $chat_session_id = $this->chat_session->create_session($visitorData);
                $chat_session = $this->chat_session->get_single(array('id' => $chat_session_id));

                // will create visit info record
                $visit_data = $this->input->post(array('page_title', 'page_url'));
                $visit_data['site_id'] = $this->current_site->id;
                $visit_data['user_id'] = $visitor->id;
                $visit_data['request_id'] = $chat_session_id;
                $visit_data['ip_address'] = $this->input->ip_address();
                $visit_data['created_at'] = date("Y-m-d H:i:s", now());
                $visit_data['meta_data'] = json_encode($visited_pages);
                $this->chat_session->createVisitInfo($visit_data);

                //insert message
                $user_message = array('chat_session_id' => $chat_session_id, 'sender_id' => $visitor->id, 'chat_message' => $this->input->post('message'));
                $local_id = 'vwb' . $visitor->id . $this->input->post('sort_order');
                $user_message['local_id'] = $local_id;
                $user_message['sort_order'] = $this->input->post('sort_order');
                $this->chat_message->model_data = $user_message;

                $message_id = $this->chat_message->add_message($local_id);
                $message_stored = array($message_id);

                // update agent information
                $agent = $this->chat_user->get_agent($chat_session->id);
                $chatbox['agent'] = $agent;

                // send chat requests
                $request_data = array('chat_session_id' => $chat_session_id, 'sender_id' => $visitor->id, 'message' => $this->input->post('message'));

                // check agent still online
                $agent_id = 0;
                if ($this->input->post('agent_id')) {
                    $online_agents = $this->user->get_online_agents_list($this->current_site->id);
                    foreach ($online_agents as $online_agent) {
                        if ($online_agent->id == $this->input->post('agent_id')) {
                            $agent_id = $this->input->post('agent_id');

                            // will leave the foreach loop and also the if statement
                            break;
                        }
                    }
                }

                $this->__send_requests($request_data, $requested_tag, $agent_id);
                $this->__send_notifications($visitor, $chat_session);

                // storing chat history in session
                $chatHistory = array(
                    array(
                        'id' => $message_id,
                        'chat_session_id' => $chat_session_id,
                        'chat_message' => $this->input->post('message'),
                        'sender_id' => $visitor->id,
                        'name' => $visitor->name,
                        'sort_order' => $this->input->post('sort_order')
                    )
                );

                // storing chat_session in session
                $chatbox['chatHistory'] = $chatHistory;
                $chatbox['last_id'] = $message_id;
                $chatbox['message_stored'] = $message_stored;
                $chatbox['chat_session'] = $chat_session;
                $this->session->set_userdata($this->access_token, $chatbox);

                $response['result'] = 'success';
                $response['data'] = array(
                    'visitor' => $visitor,
                    'agent' => $agent,
                    'chat_session' => $chat_session,
                    'chatHistory' => $chatHistory,
                    'last_id' => $message_id,
                    'message_stored' => $message_stored
                );
            } else {
                $response['error'] = validation_errors();
            }
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This proctect function will create chat requests.
     * 
     * @param $request_data array of data
     * @param $tag_id Id of department
     * @param $agent_id Id of requested agent
     */

    protected function __send_requests($request_data = array(), $tag_id = 0, $agent_id = 0) {
        if ($request_data) {
            $operators = array();
            $request_data['request_status'] = 'pending';
            $request_data['request_type'] = 'new';

            $insert_data = array();

            if ($tag_id > 0) {
                // fetching online agents from departments
                $online_agents = $this->user->get_online_agents_list($this->current_site->id, $tag_id);

                foreach ($online_agents as $key => $agent) {
                    if (!in_array($agent->id, $operators) and $agent->id != $request_data['sender_id']) {
                        $operators[] = $agent->id;
                        $insert_data[$key] = $request_data;
                        $insert_data[$key]['requested_to'] = $agent->id;
                        $insert_data[$key]['created_at'] = date("Y-m-d H:i:s", now());
                    }
                }
            } else {
                if ($agent_id) {
                    $operators[] = $agent_id;
                    $request_data['requested_to'] = $agent_id;
                    $request_data['created_at'] = date("Y-m-d H:i:s", now());
                    $insert_data[] = $request_data;
                } else {
                    // fetching all online agents.
                    $online_agents = $this->user->get_online_agents_list($this->current_site->id);
                    foreach ($online_agents as $key => $agent) {
                        if (!in_array($agent->id, $operators) and $agent->id != $request_data['sender_id']) {
                            $operators[] = $agent->id;
                            $insert_data[$key] = $request_data;
                            $insert_data[$key]['requested_to'] = $agent->id;
                            $insert_data[$key]['created_at'] = date("Y-m-d H:i:s", now());
                        }
                    }
                }
            }

            if (count($insert_data) > 0) {
                $this->chat_request->send_requests_batch($insert_data);
            }

            // sending request to admins if can notify
            if ($this->settings->notify_all_requests_admin == 'yes') {
                // fetching all online agents.
                $admins = $this->user->get_all(array('role' => 'admin', 'user_status' => 'active'), 'id');
                foreach ($admins as $key => $admin) {
                    if (!in_array($admin->id, $operators) and $admin->id != $request_data['sender_id']) {
                        $operators[] = $admin->id;
                        $insert_data = array();
                        $insert_data = $request_data;
                        $insert_data['requested_to'] = $admin->id;
                        $insert_data['created_at'] = date("Y-m-d H:i:s", now());

                        $this->chat_request->notify_to_admin($insert_data);
                    }
                }
            }
        }
    }

    /*
     * This proctect function will send notifications on mobile apps.
     * 
     * @param $visitor
     * @param $chat_session
     * 
     * @param $tag_id Id of department
     */

    protected function __send_notifications($visitor, $chat_session) {
        //get recent requests
        $requests = $this->chat_request->get_all(array('chat_session_id' => $chat_session->id));

        foreach ($requests as $request) {
            // insertin notification in database.
            $notification_data = array();
            $notification_data['notification_type'] = 'online_request';
            $notification_data['chat_session_id'] = $chat_session->id;
            $notification_data['request_id'] = $request->id;
            $notification_data['receiver_id'] = $request->requested_to;
            $notification_data['message'] = $request->message;
            $notification_data['display_message'] = $visitor->name . " sent you new request.";
            $notification_data['sender_id'] = $visitor->id;
            $notification_data['notification_status'] = 'unread';
            $this->chat_request->insert_notification($notification_data);

            // sending push notification
            $message = array();
            $notifications = $this->user->get_notifications($request->requested_to);
            $message['notificationsCounter'] = count($notifications);
            $message['unreadSession'] = $this->chat_session->get_running_session($request->requested_to);

            $message['type'] = 'online_request';
            $message['senderId'] = $visitor->id;
            $message['name'] = $visitor->name;
            $message['email'] = $visitor->email;
            $message['profile_color'] = $visitor->profile_color;
            $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
            $message['message'] = $request->message;
            $message['displayMessage'] = $this->lang->line('new_request_notification');
            $message['chatSessionId'] = $chat_session->id;
            $message['requestID'] = $request->id;

            push_notification($request->requested_to, $message);
        }
    }

    /*
     * This the chatHeartbeat of chattinh.
     * 
     * @param $last_id
     * @param $is_typing
     * 
     * @return chating data 
     */

    function chatHeartbeat() {
        $last_id = $this->input->get('last_id');
        $is_typing = $this->input->get('typing');
        $this->forceToActivityUpdate();

        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            $this->__update_last_activity();
            $chatbox = $this->session->userdata($this->access_token);

            $chatHistory = $chatbox['chatHistory'];
            $chat_session = $chatbox['chat_session'];
            $visitor = $chatbox['visitor'];
            $message_stored = $chatbox['message_stored'];

            // updating typing status.
            $this->chat_user->model_data = array('user_id' => $visitor->id, 'chat_session_id' => $chat_session->id);
            $this->chat_user->update_typing_status($is_typing);
            $messages = array();

            $messages_list = $this->chat_message->get_chat_messages($chat_session->id, $last_id);
            foreach ($messages_list as $row) {
                $chat_row = array();
                $last_id = $row->id;
                $chat_row['id'] = $row->id;
                $chat_row['name'] = $row->name;
                $chat_row['display_name'] = $row->display_name;
                $chat_row['profile_color'] = $row->profile_color;
                $chat_row['profile_pic'] = $row->profile_pic;
                $chat_row['profilePic'] = $this->media->get_thumbnail($row->profile_pic, PROFILE_PICS, $row->email);

                if ($row->message_type == 'file') {
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
                $messages[] = $chat_row;
            }

            // update session status
            $chatsession = $this->chat_session->get_single(array('id' => $chat_session->id), 'session_status');
            $chat_session->session_status = $chatsession->session_status;
            $chatbox['chat_session'] = $chat_session;
            
            // sending transcript to admin and operators if needed
            if($chat_session->session_status == 'closed') {
                $this->__send_chat_transcript($chatbox);
                $chatbox['sent_chatt_operators'] = 'yes';
            }

            // update agent information
            $agent = $this->chat_user->get_agent($chat_session->id);

            //set agent login status
            $online_users = $this->chat_user->get_online_users();
            $agent->status = (isset($online_users[$agent->id])) ? 'online' : 'offline';

            $chatbox['agent'] = $agent;

            // storing chat history in session
            $chatbox['chatHistory'] = $chatHistory;
            $chatbox['last_id'] = $last_id;
            $chatbox['message_stored'] = $message_stored;

            $this->session->set_userdata($this->access_token, $chatbox);

            $response['result'] = 'success';
            $response['last_id'] = $last_id;
            $response['chat_session'] = $chat_session;
            $response['agent'] = $agent;
            $response['chatMessagesData'] = $messages;
            $response['is_agents_online'] = $this->__is_agents_online();
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
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);

            //check if data is valid or not
            $this->form_validation->set_rules($this->chat_message->validation_rules['new_message']);

            if ($this->form_validation->run() === true) {
                $this->chat_message->model_data = $this->input->post(array('chat_message', 'chat_session_id', 'message_status', 'sender_id', 'sort_order'));
                $local_id = 'vwb' . $this->input->post('sender_id') . $this->input->post('sort_order');
                $this->chat_message->model_data['local_id'] = $local_id;

                $message_id = $this->chat_message->add_message($local_id);
                if ($message_id and is_object($message_id) === false) {
                    $chatHistory = $chatbox['chatHistory'];
                    $this->chat_message->model_data['id'] = $message_id;
                    $chatHistory[] = $this->chat_message->model_data;

                    $chat_session = $chatbox['chat_session'];
                    $visitor = $chatbox['visitor'];
                    $agent = $chatbox['agent'];

                    // insertin notification in database.
                    $notification_data = array();
                    $notification_data['notification_type'] = 'message';
                    $notification_data['chat_session_id'] = $chat_session->id;
                    $notification_data['receiver_id'] = $agent->id;
                    $notification_data['message'] = $this->input->post('chat_message');
                    $notification_data['display_message'] = sprintf($this->lang->line('visitor_sent_new_message'), $visitor->name);
                    $notification_data['sender_id'] = $visitor->id;
                    $notification_data['notification_status'] = 'unread';
                    $this->chat_request->insert_notification($notification_data);

                    // sending push notification
                    $message = array();
                    $notifications = $this->user->get_notifications($agent->id);
                    $message['notificationsCounter'] = count($notifications);
                    $message['unreadSession'] = $this->chat_session->get_running_session($agent->id);

                    $message['type'] = 'message';
                    $message['senderId'] = $visitor->id;
                    $message['name'] = $visitor->name;
                    $message['profile_color'] = $visitor->profile_color;
                    $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
                    $message['message'] = $this->input->post('chat_message');
                    $message['message_type'] = 'text';
                    $message['message_meta'] = '';
                    $message['displayMessage'] = sprintf($this->lang->line('get_new_message'), $visitor->name);
                    $message['chatSessionId'] = $chat_session->id;
                    $message['sortOrder'] = $this->input->post('sort_order');
                    $message['messageId'] = $message_id;

                    push_notification($agent->id, $message);

                    $response['result'] = 'success';
                    $response['message_row'] = $this->chat_message->model_data;
                } else {
                    //$response['error'] = $this->lang->line('message_enterd');
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
     * Upload user file and add in chat
     */

    function upload_file() {
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);

            //check if data is valid or not
            $this->form_validation->set_rules($this->chat_message->validation_rules['new_base64file']);

            if ($this->form_validation->run() === true) {
                $extension = strtolower(strrchr($this->input->post('filename'), '.'));
                $allowed_filetypes = explode("|", str_replace(" ", "", $this->settings->allowed_filetypes));

                if ($this->input->post('filesize') <= ($this->settings->file_upload_size * 1024) and in_array($extension, $allowed_filetypes)) {
                    $chat_session = $chatbox['chat_session'];
                    $visitor = $chatbox['visitor'];
                    $agent = $chatbox['agent'];

                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(CHATFILES_DIR);
                    $file_prefix = $chat_session->id . date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $uploaded_fileurl = upload_url(CHATFILES_DIR . $filedata['filename']);
                        $this->chat_message->model_data = $this->input->post(array('chat_session_id', 'message_status', 'sender_id', 'sort_order'));
                        $local_id = 'vwb' . $this->input->post('sender_id') . $this->input->post('sort_order');
                        $this->chat_message->model_data['local_id'] = $local_id;
                        $this->chat_message->model_data['message_type'] = 'file';
                        $chat_message = file_link($uploaded_fileurl, $base64_array['filesize'], $base64_array['filename']);
                        $this->chat_message->model_data['chat_message'] = $uploaded_fileurl;
                        $this->chat_message->model_data['message_meta'] = json_encode($this->input->post(array('filename', 'filetype', 'filesize')));

                        $message_id = $this->chat_message->add_message($local_id);
                        if ($message_id and is_object($message_id) === false) {
                            $this->chat_message->model_data['id'] = $message_id;
                            $this->chat_message->model_data['chat_message'] = $chat_message;
                            $response['message_data'] = $this->chat_message->model_data;

                            // insertin notification in database.
                            $notification_data = array();
                            $notification_data['notification_type'] = 'message';
                            $notification_data['chat_session_id'] = $chat_session->id;
                            $notification_data['receiver_id'] = $agent->id;
                            $notification_data['message'] = $uploaded_fileurl;
                            $notification_data['display_message'] = sprintf($this->lang->line('visitor_sent_new_message'), $visitor->name);
                            $notification_data['sender_id'] = $visitor->id;
                            $notification_data['notification_status'] = 'unread';
                            $this->chat_request->insert_notification($notification_data);

                            // sending push notification
                            $message = array();
                            $notifications = $this->user->get_notifications($agent->id);
                            $message['notificationsCounter'] = count($notifications);
                            $message['unreadSession'] = $this->chat_session->get_running_session($agent->id);

                            $message['type'] = 'message';
                            $message['senderId'] = $visitor->id;
                            $message['name'] = $visitor->name;
                            $message['profile_color'] = $visitor->profile_color;
                            $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
                            $message['message'] = $uploaded_fileurl;
                            $message['message_type'] = 'file';
                            $message['message_meta'] = $this->chat_message->model_data['message_meta'];
                            $message['displayMessage'] = sprintf($this->lang->line('get_new_message'), $visitor->name);
                            $message['chatSessionId'] = $chat_session->id;
                            $message['sortOrder'] = $this->input->post('sort_order');
                            $message['messageId'] = $message_id;

                            push_notification($agent->id, $message);

                            $response['result'] = 'success';
                        }
                    }
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
     * This function minimize chat window
     * 
     * 
     * @return $error or $message
     */

    function minimize() {
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);
            $chatbox['minimized'] = 'yes';
            $this->session->set_userdata($this->access_token, $chatbox);

            $response['result'] = 'success';
            $response['minimized'] = 'yes';
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function maximize chat window
     * 
     * 
     * @return $error or $message
     */

    function maximize() {
        $response = array('error' => '', 'result' => 'failed');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);
            $chatbox['minimized'] = 'no';
            $this->session->set_userdata($this->access_token, $chatbox);

            $response['result'] = 'success';
            $response['minimized'] = 'no';
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will end (close) chat.
     * 
     * 
     * @return $error or $message
     */

    function end() {
        $response = array('error' => '', 'result' => 'failed', 'redirect_to' => '');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);

            $chat_session = $chatbox['chat_session'];
            $this->chat_session->model_data['session_status'] = 'closed';
            if ($this->chat_session->update_chat_session($chat_session->id)) {                    
                $visitor = $chatbox['visitor'];
                $agent = $chatbox['agent'];

                // sending push notification
                $message = array();
                $notifications = $this->user->get_notifications($agent->id);
                $message['notificationsCounter'] = count($notifications);
                $message['unreadSession'] = $this->chat_session->get_running_session($agent->id);

                $message['type'] = 'chat_close';
                $message['senderId'] = $visitor->id;
                $message['name'] = $visitor->name;
                $message['profile_color'] = $visitor->profile_color;
                $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
                $message['message'] = '';
                $message['chatSessionId'] = $chat_session->id;

                push_notification($agent->id, $message);                    
                    
                    
                // sending transcript to admin and operators if needed
                $this->__send_chat_transcript($chatbox);
                $chatbox['sent_chatt_operators'] = 'yes';

                // sending transcript to visitor if needed
                if ($this->input->post('send_chat_transcript') == 'yes') {
                    $this->send_chat_transcript($this->input->post('visitor_email'));
                }

                $old_session_status = $chat_session->session_status;
                $chat_session->session_status = 'closed';
                $response['chat_session'] = $chat_session;

                if ($old_session_status == 'requested' or $old_session_status == 'forward') {
                    $this->close_requests($chat_session->id);
                }

                if ($this->settings->enable_feedback_form == 'yes' and $old_session_status != 'requested' and $old_session_status != 'forward') {
                    $response['show_feedback_form'] = 'yes';
                    $chatbox['show_feedback_form'] = 'yes';

                    $chatbox['chat_session'] = $chat_session;
                    $this->session->set_userdata($this->access_token, $chatbox);
                } else {
                    $response['redirect_to'] = $this->__get_redirect_url();
                    $this->__remove_session();
                }

                $response['result'] = 'success';
                $response['message'] = $this->lang->line('chat_closed');
            } else {
                $response['error'] = $this->lang->line('process_error');
            }
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will remove all session
     */

    private function __remove_session() {
        $chatbox = array('show_feedback_form' => false, 'last_id' => 0);
        $this->session->set_userdata($this->access_token, $chatbox);
    }

    /*
     * Function send_chat_transcript to visitor
     * 
     */

    function send_chat_transcript($visitor_email = '') {
        $chatbox = $this->session->userdata($this->access_token);

        $chat_session = $chatbox['chat_session'];
        $visitor = $chatbox['visitor'];
        $chat_messages = $this->chat_message->get_chat_transcript($chat_session->id);

        // send email to visitor
        $template_file = 'chat_transcript';
        $to = ($visitor_email) ? $visitor_email : $visitor->email;
        $subject = $this->lang->line('chat_transcript');

        $data = array(
            'visitor' => $visitor,
            'chatHistory' => $chat_messages,
            'from_email' => $this->current_site->site_email,
            'from_name' => $this->current_site->site_name,
            'site_name' => $this->current_site->site_name,
            'site_logo' => $this->current_site->site_logo
        );
        send_template_email($template_file, $to, $subject, $data);
    }

    /*
     * __send_chat_transcript
     *
     * Function will send chat transcript to admins or operators
     *
     * @type	function
     * @date	16-01-2018
     * @since	5.1.1
     *
     * @param	$chatbox Array
     * @return	N/A
     */

    private function __send_chat_transcript($chatbox) {
        $chat_session = $chatbox['chat_session'];
        $agent = $chatbox['agent'];
        $sent_chatt_operators = (isset($chatbox['sent_chatt_operators'])) ? $chatbox['sent_chatt_operators'] : 'no';
                        
        $operators = array();
        $chat_messages = array();

        if ($agent and $agent->id > 0 and $this->settings->send_chat_transcript_to_operators == 'yes' and $sent_chatt_operators == 'no') {
            $operators[] = $agent->id;
            $chat_messages = $this->chat_message->get_chat_transcript($chat_session->id);
            $operator = $this->user->get_single(array('id' => $agent->id), 'id, name, display_name, email');

            // send email to visitor
            $template_file = 'operators_chat_transcript';
            $to = $operator->email;
            $subject = $this->lang->line('chat_transcript');

            $data = array(
                'receiver' => $operator,
                'chatHistory' => $chat_messages
            );
            
            send_template_email($template_file, $to, $subject, $data);
        }

        if ($this->settings->send_chat_transcript_to_admin == 'yes' and $sent_chatt_operators == 'no') {
            if (empty($chat_messages)) {
                $chat_messages = $this->chat_message->get_chat_transcript($chat_session->id);
            }

            // fetching all online agents.
            $admins = $this->user->get_all(array('role' => 'admin', 'user_status' => 'active'), 'id, name, display_name, email');
            foreach ($admins as $key => $admin) {
                if (!in_array($admin->id, $operators)) {
                    // send email to visitor
                    $template_file = 'operators_chat_transcript';
                    $to = $admin->email;
                    $subject = $this->lang->line('chat_transcript');

                    $data = array(
                        'receiver' => $admin,
                        'chatHistory' => $chat_messages
                    );
                    send_template_email($template_file, $to, $subject, $data);
                }
            }
        }
    }
    
    /*
     * close_session
     *
     * Function will end currunt runing chat session.
     *
     * @type	function
     * @date	17-01-2018
     * @since	5.1.1
     *
     * @param	N/A
     * @return	N/A
     */
    
    function close_session() {
        $response = array('error' => '', 'result' => 'failed', 'redirect_to' => '');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);
            $response['result'] = 'success';
            $response['show_feedback_form'] = false;
            $response['redirect_to'] = $this->__get_redirect_url();

            $this->__remove_session();
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will send feedback to agent for chat convesation
     * 
     * @param Post feedback_text,rating,chat_session_id,feedback_by,feedback_to
     * 
     * @return $error or $message
     */

    public function send_feedback() {
        $response = array('error' => '', 'result' => 'failed', 'redirect_to' => '');
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);

            //check if data is valid or not
            $this->form_validation->set_rules($this->feedback->validation_rules['feedback']);

            if ($this->form_validation->run() === true) {
                $show_feedback_form = (isset($chatbox['show_feedback_form'])) ? $chatbox['show_feedback_form'] : 'no';
                if ($show_feedback_form == 'yes') {
                    $this->feedback->model_data = $this->input->post(array('rating', 'chat_session_id', 'feedback_by', 'feedback_to'));
                    $feedback_text = '';
                    if ($this->input->post('feedback_text')) {
                        $feedback_text = $this->input->post('feedback_text');
                        $this->feedback->model_data['feedback_text'] = $feedback_text;
                    }
                    $feedback_id = $this->feedback->insert_feedback();

                    if ($feedback_id) {
                        $rating_status = array(
                            1 => $this->lang->line('rating_one'),
                            2 => $this->lang->line('rating_two'),
                            3 => $this->lang->line('rating_three'),
                            4 => $this->lang->line('rating_four'),
                            5 => $this->lang->line('rating_five')
                        );

                        $chat_session = $chatbox['chat_session'];
                        $visitor = $chatbox['visitor'];
                        $agent = $chatbox['agent'];

                        //adding message in chat session.
                        $this->chat_message->model_data['chat_message'] = $feedback_text . $this->lang->line('and_your_rating_is') . $this->input->post('rating') . ' (' . $rating_status[$this->input->post('rating')] . ')';
                        $this->chat_message->model_data['chat_session_id'] = $chat_session->id;
                        $this->chat_message->model_data['message_status'] = 'unread';
                        $this->chat_message->model_data['sender_id'] = $visitor->id;
                        $this->chat_message->model_data['sort_order'] = $this->input->post('sort_order');
                        $local_id = 'awb' . $visitor->id . $this->input->post('sort_order');
                        $this->chat_message->model_data['local_id'] = $local_id;
                        $message_id = $this->chat_message->add_message($local_id);

                        // insertin notification in database.
                        $notification_data = array();
                        $notification_data['notification_type'] = 'message';
                        $notification_data['chat_session_id'] = $chat_session->id;
                        $notification_data['receiver_id'] = $agent->id;
                        $notification_data['message'] = $feedback_text . $this->lang->line('and_your_rating_is') . $this->input->post('rating') . ' (' . $rating_status[$this->input->post('rating')] . ')';
                        $notification_data['display_message'] = sprintf($this->lang->line('visitor_sent_feedback'), $visitor->name);
                        $notification_data['sender_id'] = $visitor->id;
                        $notification_data['notification_status'] = 'unread';
                        $this->chat_request->insert_notification($notification_data);

                        // sending push notification
                        $message = array();
                        $notifications = $this->user->get_notifications($agent->id);
                        $message['notificationsCounter'] = count($notifications);
                        $message['unreadSession'] = $this->chat_session->get_running_session($agent->id);

                        $message['type'] = 'message';
                        $message['senderId'] = $visitor->id;
                        $message['name'] = $visitor->name;
                        $message['profile_color'] = $visitor->profile_color;
                        $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
                        $message['message'] = $feedback_text . $this->lang->line('and_your_rating_is') . $this->input->post('rating') . ' (' . $rating_status[$this->input->post('rating')] . ')';
                        $message['rating'] = $this->input->post('rating');
                        $message['message_type'] = 'text';
                        $message['message_meta'] = '';
                        $message['displayMessage'] = sprintf($this->lang->line('visitor_sent_feedback'), $visitor->name);
                        $message['chatSessionId'] = $chat_session->id;
                        $message['sortOrder'] = $this->input->post('sort_order');
                        $message['messageId'] = $message_id;

                        push_notification($agent->id, $message);

                        $response['result'] = 'success';
                        $response['show_feedback_form'] = false;
                        $response['redirect_to'] = $this->__get_redirect_url();

                        $this->__remove_session();
                    } else {
                        $response['error'] = $this->lang->line('process_error');
                    }
                } else {
                    $response['show_feedback_form'] = false;
                    $response['error'] = $this->lang->line('process_error');
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
     * This function create a chat request on server.
     * 
     * $param Post name,email,message
     * @return 
     */

    public function connect_operator() {
        $response = array('error' => '', 'result' => 'failed');
        
        if ($this->valid_token) {
            $chatbox = $this->session->userdata($this->access_token);
            $visited_pages = isset($chatbox['visited_pages']) ? $chatbox['visited_pages'] : array();

            $this->session->unset_userdata($this->access_token);
            $chatbox = array();
            $chatbox['visited_pages'] = $visited_pages;

            $ip_address = ($this->input->ip_address() == '::1') ? '47.9.135.154' : $this->input->ip_address();
            $v_location = getLocationByIp($ip_address);

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

            // storing visitor in session
            $chatbox['visitor_id'] = $visitor->id;
            $chatbox['visitor'] = $visitor;

            // creating user chat request.
            $this->chat_session->model_data['session_type'] = 'public';
            $this->chat_session->model_data['session_status'] = 'open';
            $this->chat_session->model_data['site_id'] = $this->current_site->id;
            $this->chat_session->model_data['user_agent'] = $this->input->user_agent();

            // creating session and visitor
            $visitorData = array('user_id' => $visitor->id);
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

                $visitorData['address_id'] = $address_id;
            }

            $chat_session_id = $this->chat_session->create_session($visitorData);
            $chat_session = $this->chat_session->get_single(array('id' => $chat_session_id));
            $message_stored = array();

            // will create visit info record
            $visit_data = $this->input->post(array('page_title', 'page_url'));
            $visit_data['site_id'] = $this->current_site->id;
            $visit_data['user_id'] = $visitor->id;
            $visit_data['request_id'] = $chat_session_id;
            $visit_data['ip_address'] = $this->input->ip_address();
            $visit_data['created_at'] = date("Y-m-d H:i:s", now());
            $visit_data['meta_data'] = json_encode($visited_pages);
            $this->chat_session->createVisitInfo($visit_data);

            // check agent still online
            $operator_id = 0;
            if ($this->input->post('operator_id')) {
                $online_agents = $this->user->get_online_agents_list($this->current_site->id);
                foreach ($online_agents as $online_agent) {
                    if ($online_agent->id == $this->input->post('operator_id')) {
                        $operator_id = $this->input->post('operator_id');
                        $this->chat_request->model_data = array('chat_session_id' => $chat_session->id, 'user_id' => $operator_id);
                        $this->chat_request->add_operator();

                        // will leave the foreach loop and also the if statement
                        break;
                    }
                }
            }

            // update agent information
            $agent = $this->chat_user->get_agent($chat_session->id);
            $chatbox['agent'] = $agent;
            $chatHistory = array();

            if ($operator_id) {
                $temp_visitor_id = $this->input->post('id');
                // get all message of visitor
                $chat_messages = $this->temp_visitor->get_messages(array('temp_visitor_id' => $temp_visitor_id));
                foreach ($chat_messages as $key => $msg) {
                    $this->chat_message->model_data = array();
                    $this->chat_message->model_data['chat_session_id'] = $chat_session->id;
                    $this->chat_message->model_data['sender_id'] = ($operator_id == $msg->sender_id) ? $operator_id : $visitor->id;
                    $this->chat_message->model_data['chat_message'] = $msg->chat_message;
                    $this->chat_message->model_data['local_id'] = $msg->local_id;
                    $this->chat_message->model_data['sort_order'] = $msg->sort_order;
                    $this->chat_message->model_data['message_type'] = 'text';
                    $this->chat_message->model_data['message_status'] = 'read';
                    $this->chat_message->model_data['created_at'] = $msg->created_at;

                    $message_id = $this->chat_message->add_message($msg->local_id);
                    $this->chat_message->model_data['id'] = $message_id;
                    $this->chat_message->model_data['name'] = ($operator_id == $msg->sender_id) ? $agent->name : $visitor->name;
                    $this->chat_message->model_data['display_name'] = ($operator_id == $msg->sender_id) ? $agent->display_name : $visitor->display_name;
                    $this->chat_message->model_data['profile_color'] = ($operator_id == $msg->sender_id) ? $agent->profile_color : $visitor->profile_color;
                    $this->chat_message->model_data['profile_pic'] = $agent->profile_pic;
                    $this->chat_message->model_data['profilePic'] = $agent->profilePic;

                    $chatHistory[] = $this->chat_message->model_data;
                    $message_stored[] = $message_id;
                }

                $activity_time = date("Y-m-d H:i:s", now());
                $this->temp_visitor->model_data = array('last_activity_time' => $activity_time);
                $this->temp_visitor->model_data['chat_session_id'] = $chat_session->id;
                $this->temp_visitor->update($temp_visitor_id);

                //insert message
                $user_message = array('chat_session_id' => $chat_session_id, 'sender_id' => $visitor->id, 'chat_message' => $this->input->post('message'));
                $local_id = 'vwb' . $visitor->id . $this->input->post('sort_order');
                $user_message['local_id'] = $local_id;
                $user_message['sort_order'] = $this->input->post('sort_order');
                $this->chat_message->model_data = $user_message;

                $message_id = $this->chat_message->add_message($local_id);
                $message_stored[] = $message_id;

                // sending push notification
                $message = array();
                $notifications = $this->user->get_notifications($agent->id);
                $message['notificationsCounter'] = count($notifications);
                $message['unreadSession'] = $this->chat_session->get_running_session($agent->id);

                $message['type'] = 'chat_initiated';
                $message['temp_visitor_id'] = $temp_visitor_id;
                $message['senderId'] = $visitor->id;
                $message['name'] = $visitor->name;
                $message['profile_color'] = $visitor->profile_color;
                $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email);
                $message['message'] = $this->input->post('message');
                $message['message_type'] = 'text';
                $message['message_meta'] = '';
                $message['displayMessage'] = sprintf($this->lang->line('get_new_message'), $visitor->name);
                $message['chatSessionId'] = $chat_session->id;
                $message['sortOrder'] = $this->input->post('sort_order');
                $message['messageId'] = $message_id;
                
                if ($v_location and is_object($v_location)) {
                    $message['city'] = $v_location->geoplugin_city;
                    $message['state'] = $v_location->geoplugin_regionName;
                    $message['country'] = $v_location->geoplugin_countryName;
                }

                push_notification($agent->id, $message);
            } else {
                //insert message
                $user_message = array('chat_session_id' => $chat_session_id, 'sender_id' => $visitor->id, 'chat_message' => $this->input->post('message'));
                $local_id = 'vwb' . $visitor->id . $this->input->post('sort_order');
                $user_message['local_id'] = $local_id;
                $user_message['sort_order'] = $this->input->post('sort_order');
                $this->chat_message->model_data = $user_message;

                $message_id = $this->chat_message->add_message($local_id);
                $message_stored[] = $message_id;

                // send chat requests
                $requested_tag = 0;
                $request_data = array('chat_session_id' => $chat_session_id, 'sender_id' => $visitor->id, 'message' => $this->input->post('message'));

                $this->__send_requests($request_data, $requested_tag, $operator_id);
                $this->__send_notifications($visitor, $chat_session);
            }

            // storing chat history in session
            $chatHistory[] = array(
                'id' => $message_id,
                'chat_session_id' => $chat_session_id,
                'chat_message' => $this->input->post('message'),
                'sender_id' => $visitor->id,
                'name' => $visitor->name,
                'sort_order' => $this->input->post('sort_order')
            );

            // storing chat_session in session
            $chatbox['chatHistory'] = $chatHistory;
            $chatbox['last_id'] = $message_id;
            $chatbox['message_stored'] = $message_stored;
            $chatbox['chat_session'] = $chat_session;
            $this->session->set_userdata($this->access_token, $chatbox);

            $response['result'] = 'success';
            $response['data'] = array(
                'visitor' => $visitor,
                'agent' => $agent,
                'chat_session' => $chat_session,
                'chatHistory' => $chatHistory,
                'last_id' => $message_id,
                'message_stored' => $message_stored
            );
        } else {
            $response['error'] = $this->lang->line('invalid_token');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function return browser push message data
     */

    function browser_message() {
        $response = array();
        $response['title'] = 'Yay a message.';
        $response['message'] = 'We have received a push message.';
        $response['icon'] = $this->settings->admin_panel_logo;
        $response['data'] = array('url' => site_url('agents'));

        return $this->output->set_content_type('application/json')->set_output($this->return_json(array('notification' => $response)));
    }
    
    /*
     * Send Push notification to updated agent if is online
     */
    function forceToActivityUpdate() {
        $config = $this->configuration->get_single(array('config_option' => 'last_login_updated'));
        $newDate = strtotime($config->config_value . ' + 4 min');

        if (now() >= $newDate || $config->config_value === '') {
            $this->configuration->model_data = array('config_value' => date("Y-m-d H:i:s", now()));
            $this->configuration->update_where(array('config_option' => 'last_login_updated'));

            $this->db->select('device_id');
            $query = $this->db->get_where($this->gcm->table, array('user_status' => 1));
            $results = $query->result_array();
            $devices = array_column($results, "device_id");

            // sending push notification
            $message = array();
            $message['type'] = 'keep_login';

            sendPushNotification($devices, $message);
        }     
    }

}
