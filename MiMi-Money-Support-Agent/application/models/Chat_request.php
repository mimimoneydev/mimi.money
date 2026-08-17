<?php

class Chat_request extends CP_Model {

    //model db table
    var $table = "";
    public $validation_rules = array(
        'forward' => array(
            array(
                'field' => 'to',
                'label' => 'Forward To',
                'rules' => 'required'
            )
        ),
        // rules for chat forward
        'agent_forward' => array(
            array(
                'field' => 'agent_ids',
                'label' => 'Agent IDs',
                'rules' => 'required'
            ),
            array(
                'field' => 'csid',
                'label' => 'Chat Session ID',
                'rules' => 'required'
            )
        ),
        'deprtment_forward' => array(
            array(
                'field' => 'department_id',
                'label' => 'Departments ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'csid',
                'label' => 'Chat Session ID',
                'rules' => 'required'
            )
        ),
        // rules for visitor
        'chat_request' => array(
            array(
                'field' => 'name',
                'label' => 'Name',
                'rules' => 'required'
            ),
            array(
                'field' => 'email',
                'label' => 'Email',
                'rules' => 'required|valid_email'
            ),
            array(
                'field' => 'message',
                'label' => 'Message',
                'rules' => 'required'
            )
        )
    );

    /*
     * define construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_CHAT_REQUESTS;
    }

    /*
     * this function will forward chat to another agents
     * 
     * @param $user_id (ID of agent)
     * @param $visitor_id (ID of visitor)
     * @param $device_id
     * 
     * @return new insert id of chat agent
     */

    public function forward_to_agents($user_id, $visitor_id, $device_id = '') {
        $insert_data = array();
        $update_data = array();
        $requests = array();

        $agent_ids = explode(",", $this->model_data['agent_ids']);
        $query = $this->db->select('id, sender_id, requested_to')
                ->where('chat_session_id', $this->model_data['chat_session_id'])
                ->where('request_type', $this->model_data['request_type'])
                ->where('sender_id', $visitor_id)
                ->get($this->table);

        $requested_requests = $query->result();

        foreach ($requested_requests as $request) {
            $requests[$request->sender_id][$request->requested_to] = $request;
        }

        foreach ($agent_ids as $key => $agent_id) {
            if (isset($requests[$visitor_id]) and isset($requests[$visitor_id][$agent_id])) {
                $request = $requests[$visitor_id][$agent_id];
                $update_data[$key]['id'] = $request->id;
                $update_data[$key]['created_at'] = date("Y-m-d H:i:s", now());
                $update_data[$key]['chat_session_id'] = $this->model_data['chat_session_id'];
                $update_data[$key]['request_type'] = $this->model_data['request_type'];
                $update_data[$key]['message'] = $this->model_data['message'];
                $update_data[$key]['sender_id'] = $visitor_id;
                $update_data[$key]['requested_to'] = $agent_id;
                $update_data[$key]['requested_by'] = $user_id;
                $update_data[$key]['request_status'] = 'pending';
            } else {
                $insert_data[$key]['created_at'] = date("Y-m-d H:i:s", now());
                $insert_data[$key]['chat_session_id'] = $this->model_data['chat_session_id'];
                $insert_data[$key]['request_type'] = $this->model_data['request_type'];
                $insert_data[$key]['message'] = $this->model_data['message'];
                $insert_data[$key]['sender_id'] = $visitor_id;
                $insert_data[$key]['requested_to'] = $agent_id;
                $insert_data[$key]['requested_by'] = $user_id;
            }
        }

        if (count($update_data) > 0) {
            $this->db->update_batch($this->table, $update_data, 'id');
        }

        if (count($insert_data) > 0) {
            $this->db->insert_batch($this->table, $insert_data);
        }

        $this->db->update(TABLE_CHAT_USERS, array('user_state' => 'left'), array('user_id' => $user_id, 'chat_session_id' => $this->model_data['chat_session_id']));

        $this->db->update(TABLE_CHAT_SESSIONS, array('session_status' => 'forward'), array('id' => $this->model_data['chat_session_id']));

        // gettting visitor by $visitor_id
        $this->db->select('id, name, email, profile_pic');
        $query = $this->db->get_where(TABLE_USERS, array('id' => $visitor_id));
        $visitor = $query->row();

        // gettting agent by $user_id
        $this->db->select('id, name, email, profile_pic');
        $query = $this->db->get_where(TABLE_USERS, array('id' => $user_id));
        $agent = $query->row();

        // select fileds
        $select = TABLE_CHAT_REQUESTS . '.id, '
                . TABLE_CHAT_REQUESTS . '.sender_id, '
                . TABLE_CHAT_REQUESTS . '.requested_to, '
                . TABLE_USERS . '.id as uid, '
                . TABLE_USERS . '.name, '
                . TABLE_USERS . '.email, '
                . TABLE_USERS . '.profile_pic';

        $created_requests = $this->db->select($select)
                ->where_in(TABLE_CHAT_REQUESTS . '.chat_session_id', $this->model_data['chat_session_id'])
                ->where_in(TABLE_CHAT_REQUESTS . '.request_type', $this->model_data['request_type'])
                ->where_in(TABLE_CHAT_REQUESTS . '.sender_id', $visitor_id)
                ->from(TABLE_CHAT_REQUESTS)
                ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_CHAT_REQUESTS . '.requested_to', 'left')
                ->order_by(TABLE_CHAT_REQUESTS . ".id", 'asc')
                ->get()
                ->result();

        foreach ($created_requests as $key => $request) {
            // insertin notification in database.
            $notification_data = array();
            $notification_data['notification_type'] = 'online_request';
            $notification_data['chat_session_id'] = $this->model_data['chat_session_id'];
            $notification_data['request_id'] = $request->id;
            $notification_data['receiver_id'] = $request->requested_to;
            $notification_data['message'] = sprintf($this->lang->line('get_forwarded_request_from'), $agent->name);
            $notification_data['display_message'] = sprintf($this->lang->line('get_forwarded_request_from'), $agent->name);
            $notification_data['sender_id'] = $visitor->id;
            $notification_data['notification_status'] = 'unread';
            $this->chat_request->insert_notification($notification_data);

            // sending push notification
            $message = array();
            $notifications = $this->get_notifications($request->requested_to);
            $message['notificationsCounter'] = count($notifications);
            $message['unreadSession'] = $this->get_running_session($request->requested_to);

            $message['type'] = 'online_request';
            $message['senderId'] = $visitor->id;
            $message['name'] = $visitor->name;
            $message['email'] = $visitor->email;
            $message['profilePic'] = $this->media->get_thumbnail($visitor->profile_pic, PROFILE_PICS, $visitor->email, '404');
            $message['message'] = sprintf($this->lang->line('get_forwarded_request_from'), $agent->name);
            $message['displayMessage'] = sprintf($this->lang->line('get_forwarded_request_from'), $agent->name);
            $message['chatSessionId'] = $this->model_data['chat_session_id'];
            $message['requestID'] = $request->id;

            push_notification($request->requested_to, $message);
        }

        $user_message = array('chat_session_id' => $this->model_data['chat_session_id'], 'sender_id' => $agent->id, 'chat_message' => $this->lang->line('im_forwardeding_request'));
        $millitime = round(microtime(true) * 1000);
        $local_id = 'awb' . $agent_id . $millitime;
        $user_message['local_id'] = $local_id;
        $user_message['sort_order'] = $millitime;
        $chat_session_id = $this->model_data['chat_session_id'];
        $this->model_data = $user_message;

        $message_id = $this->add_message($local_id);
        if ($message_id and is_object($message_id) === false) {
            // sending push notification
            $message = array();
            $notifications = $this->get_notifications($agent->id);
            $message['notificationsCounter'] = count($notifications);
            $message['unreadSession'] = $this->get_running_session($agent->id);

            $message['type'] = 'message';
            $message['senderId'] = $agent->id;
            $message['name'] = $visitor->name;
            $message['profilePic'] = $this->media->get_thumbnail($agent->profile_pic, PROFILE_PICS, $agent->email, '404');
            $message['message'] = $this->lang->line('im_forwardeding_request');
            $message['displayMessage'] = sprintf($this->lang->line('agent_forwarded_request_to_other'), $agent->name);
            $message['chatSessionId'] = $chat_session_id;
            $message['sortOrder'] = $millitime;
            $message['messageId'] = $message_id;

            push_notification($agent->id, $message, $device_id);

            return true;
        } else if ($message_id and is_object($message_id)) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * this function will forward chat to another agents
     * 
     * @param $agent_id (ID of agent)
     * @param $visitor_id (ID of visitor)
     * @param $device_id
     * 
     * @return new insert id of chat agent
     */

    public function forward_to_deprtments($agent_id, $visitor_id, $device_id = '') {
        $deprtment_id = $this->model_data['department_id'];
        $session_id = $this->model_data['chat_session_id'];

        $agents = $this->session_agents($session_id);
        $agents_ids = array();
        foreach ($agents as $row) {
            $agents_ids[] = $row->user_id;
        }

        $query = $this->db->select(TABLE_USER_TAGS . '.user_id')
                ->where(TABLE_USER_TAGS . '.tag_id', $deprtment_id)
                ->where(TABLE_USER_TAGS . '.user_id !=', $agent_id)
                ->where(TABLE_USERS . '.role !=', 'visitor')
                ->where(TABLE_USERS . '.user_status', 'active')
                ->where_not_in(TABLE_USER_TAGS . '.user_id', $agents_ids)
                ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_USER_TAGS . '.user_id')
                ->get(TABLE_USER_TAGS);

        $users = $query->result();

        if (count($users) > 0) {
            $agent_ids = array();
            foreach ($users as $user) {
                $agent_ids[] = $user->user_id;
            }

            $this->model_data['agent_ids'] = implode(",", $agent_ids);
            return $this->forward_to_agents($agent_id, $visitor_id, $device_id);
        } else {
            return 'no-agents';
        }
    }

    /*
     * this function will create new chat
     * 
     * @param $batch_data (array of data)
     * @return true or false
     */

    function send_requests_batch($batch_data = array()) {
        if (empty($batch_data))
            return false;

        $this->db->insert_batch($this->table, $batch_data);

        return true;
    }

    /*
     * NOtify admin to new online request
     * 
     * @param Array $request_data
     * @return Mixed insert_id or false
     */

    function notify_to_admin($request_data = array()) {
        if (is_array($request_data) and count($request_data) > 0) {
            $this->db->insert($this->table, $request_data);

            return $this->db->insert_id();
        }

        return FALSE;
    }

    /*
     * This function will cancel all requests.
     * 
     * @param $chat_session_id
     * 
     * @return true or false
     */

    function close_requests($chat_session_id) {
        return $this->db->update($this->table, array('request_status' => 'closed'), array('chat_session_id' => $chat_session_id));
    }

    /*
     * This function will return all pending new requests of an angent
     * 
     * @param $agent_id Id for Agent
     * @param $last_request_id
     * 
     * @return $new_requests array of new requests
     */

    function new_requests($agent_id, $last_request_id = 0) {
        $requests = $this->db->select('creqs.id, '
                        . 'creqs.chat_session_id, '
                        . 'creqs.message, '
                        . 'creqs.request_type, '
                        . 'tag.tag_name, '
                        . 'user.id as sender_id, '
                        . 'user.name, '
                        . 'user.email, '
                        . 'user.profile_color, '
                        . 'user.profile_pic,'
                        . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture, "
                        . 'agent.id as agent_id,'
                        . 'agent.name as agent_name,'
                        . 'agent.email as agent_email,'
                        . 'agent.profile_color as agent_profile_color, '
                        . 'agent.profile_pic as agent_profile_pic, '
                        . " CASE WHEN agent.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', agent.profile_pic) END  as agent_profile_picture, "
                        . 'visit_info.ip_address, '
                        . 'visit_info.page_title, '
                        . 'visit_info.page_url'
                )
                ->where('creqs.id > ', $last_request_id)
                ->where('creqs.requested_to', $agent_id)
                ->where('creqs.request_status', 'pending')
                ->from(TABLE_CHAT_REQUESTS . ' creqs')
                ->join(TABLE_CHAT_SESSIONS . ' sess', 'sess.id = ' . 'creqs.chat_session_id')
                ->join(TABLE_TAGS . ' tag', 'tag.id = ' . 'sess.requested_tag', 'left')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'creqs.sender_id')
                ->join(TABLE_USERS . ' agent', 'agent.id = ' . 'creqs.requested_by', 'left')
                ->join(TABLE_USER_VISIT_INFO . ' visit_info', "visit_info.request_id = " . "sess.id and " . "visit_info.request_type = 'online' and " . "visit_info.user_id = " . "user.id ", "left")
                ->order_by("creqs.id", "asc")
                ->get()
                ->result();

        return $requests;
    }

    /*
     * This function will return request details
     * 
     * @param $request_id
     * @return $request 
     */

    function get_request($request_id) {
        $request = $this->db->select('creqs.id, '
                        . 'creqs.chat_session_id, '
                        . 'creqs.message, '
                        . 'user.id as sender_id, '
                        . 'user.name, '
                        . 'user.email, '
                        . 'user.profile_color, '
                        . 'user.profile_pic, '
                        . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture, "
                )
                ->where('creqs.id', $request_id)
                ->from(TABLE_CHAT_REQUESTS . ' creqs')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'creqs.sender_id')
                ->get()
                ->row();

        return $request;
    }

    /*
     * Accept request
     * 
     * @param $request
     * @return true or false
     */

    function accept_request($request) {
        // updating chat_requests table
        $this->db->update($this->table, array('request_status' => 'closed'), array('chat_session_id' => $request->chat_session_id));
        $this->db->update($this->table, array('request_status' => 'accepted'), array('id' => $request->id));

        // updating chat_session table
        $this->db->update(TABLE_CHAT_SESSIONS, array('session_status' => 'open'), array('id' => $request->chat_session_id));

        //insert chat user entry
        $user_data = array('chat_session_id' => $request->chat_session_id, 'user_id' => $request->requested_to, 'user_role' => 'agent', 'user_state' => 'in-chat', 'started_at' => date("Y-m-d H:i:s"));
        $this->db->insert(TABLE_CHAT_USERS, $user_data);
    }

    /*
     * Add operator in chat
     */
    function add_operator() {
        //insert chat user entry
        $this->model_data['user_role'] = 'agent';
        $this->model_data['user_state'] = 'in-chat';
        $this->model_data['started_at'] = date("Y-m-d H:i:s", now());
        
        $this->db->insert(TABLE_CHAT_USERS, $this->model_data);
    }

}
