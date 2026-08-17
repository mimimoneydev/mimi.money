<?php

class CP_Model extends CI_Model {

    public $model_data = array();
    public $item_per_page = 10;
    public $user_select_fields = '';

    public function __construct() {
        parent::__construct();

        $ut = TABLE_USERS;
        $this->user_select_fields = $ut . '.id, ' . $ut . '.name, ' . $ut . '.email, ' . $ut . '.display_name, ' . $ut . '.profile_pic, ' . $ut . '.profile_color, ' . $ut . '.role, ' . $ut . '.last_activity_time, ' . $ut . '.last_login, ' . $ut . '.contact_number';
        $this->user_select_fields .= ", CASE WHEN " . $ut . ".profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', " . $ut . ".profile_pic) END  as profile_picture";
    }

    /*
     * This function will return agents list.
     * If you pass department id as argument it will return on agent within that department
     * 
     * @param $site_id
     * @param $department_id (optional)
     * @param $select (optional)
     * 
     * @return $agents;
     */

    function get_agents($site_id, $department_id = '', $select = '') {
        if (empty($select)) {
            $select = $this->user_select_fields;
        }

        // select fields
        $this->db->select($select);

        // if department id 
        if ($department_id) {
            $this->db->where('user_tag.tag_id', $department_id);
        }
        
        $this->db->where('user_site.site_id', $site_id);

        $query = $this->db->where(TABLE_USERS . '.role !=', 'visitor')
                ->from(TABLE_USERS);

        // if department id 
        if ($department_id) {
            $query->join(TABLE_USER_TAGS . ' user_tag', 'user_tag.user_id = ' . TABLE_USERS . '.id', 'left');
        }
        
        $query->join(TABLE_USERS_TO_SITES . ' user_site', 'user_site.user_id = ' . TABLE_USERS . '.id');

        $agents = $query->order_by(TABLE_USERS . ".id", 'desc')
                ->group_by(TABLE_USERS . ".id")
                ->get()
                ->result();

        return $agents;
    }

    /*
     * This function will return agents list of a chat session.
     * If you pass department id as argument it will return on agent within that department
     * 
     * @param $session_id
     * 
     * @return $agents;
     */

    function session_agents($session_id) {
        $query = $this->db->select('user_id')
                ->where('chat_session_id', $session_id)
                ->where('user_role', 'agent')
                ->get(TABLE_CHAT_USERS);

        $agents = $query->result();

        return $agents;
    }

    /*
     * This function will be call to fetch a single record from database.
     * 
     * @param $conditions array of conditions
     * @param $select fields of table
     * 
     * @return result as single object
     */

    public function get_single($conditions = array(), $select = '*') {
        $this->db->select($select);
        $query = $this->db->get_where($this->table, $conditions);

        return $query->row();
    }

    /*
     * This function will be call to fetch a single record from database.
     * 
     * @param $conditions array of conditions
     * @param $select fields of table
     * 
     * @return result as single object
     */

    public function get_all($conditions = array(), $select = '*') {
        $this->db->select($select);
        $query = $this->db->get_where($this->table, $conditions);

        return $query->result();
    }

    /*
     * This function will be call to update record from database.
     * 
     * @param $where array of conditions
     * @param $table (name of db table) optional
     * @return boolean (true or false)
     */

    public function update_where($where = array(), $table = '') {
        if (empty($table))
            $table = $this->table;
        if (empty($where) === false) {
            return $this->db->update($table, $this->model_data, $where);
        }

        return false;
    }

    /*
     * This function will be call to delete record from database.
     * 
     * @param $where array of conditions
     * @param $table (name of db table) optional
     * @return boolean (true or false)
     */

    function delete_where($where = array(), $table = '') {
        if (empty($table))
            $table = $this->table;
        if (empty($where) === false) {
            return $this->db->delete($table, $where);
        }

        return false;
    }

    /*
     * This function will return online users
     * 
     * @return $online_users array of online users
     */

    function get_online_users() {
        $online_users = array();
        $online_activity_time = date("Y-m-d H:i:s", (now() - (15)));
        $mobile_online_activity_time = date("Y-m-d H:i:s", (now() - (5 * 60)));
        $desktop_online_activity_time = date("Y-m-d H:i:s", (now() - (5 * 60)));

        $users = $this->db->select($this->user_select_fields)
                ->or_where('last_activity_time > ', $online_activity_time)
                ->or_where('mobile_last_activity_time > ', $mobile_online_activity_time)
                ->or_where('desktop_last_activity_time > ', $desktop_online_activity_time)
                ->from(TABLE_USERS)
                ->get()
                ->result();

        foreach ($users as $user) {
            $user->profilePic = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $user->email);
            $online_users[$user->id] = $user;
        }
        return $online_users;
    }

    /*
     * This function check has online agents
     * 
     * @param $site_id
     * @param $department_id
     * @return $online_users array of online users
     */

    function hasOnlineAgents($site_id, $department_id = '') {
        $result = array();
        $online_activity_time = date("Y-m-d H:i:s", (now() - (15)));
        $mobile_online_activity_time = date("Y-m-d H:i:s", (now() - (5 * 60)));
        $desktop_online_activity_time = date("Y-m-d H:i:s", (now() - (5 * 60)));

        if ($department_id) {
            $where = "(" . TABLE_USERS . ".last_activity_time > '" . $online_activity_time . "' or " . TABLE_USERS . ".mobile_last_activity_time > '" . $mobile_online_activity_time . "' or " . TABLE_USERS . ".desktop_last_activity_time > '" . $desktop_online_activity_time . "')";
            $result = $this->db->select($this->user_select_fields)
                    ->where_in(TABLE_USERS . '.role', array('agent', 'admin'))
                    ->where(TABLE_USER_TAGS . '.tag_id', $department_id)
                    ->where('us.site_id', $site_id)
                    ->where($where)
                    ->from(TABLE_USERS)
                    ->join(TABLE_USER_TAGS, TABLE_USER_TAGS . '.user_id = ' . TABLE_USERS . '.id')
                    ->join(TABLE_USERS_TO_SITES . ' us', 'us.user_id = ' . TABLE_USERS . '.id')
                    ->get()
                    ->row();
        } else {
            $where = "(last_activity_time > '" . $online_activity_time . "' or mobile_last_activity_time > '" . $mobile_online_activity_time . "' or desktop_last_activity_time > '" . $desktop_online_activity_time . "')";
            $result = $this->db->select($this->user_select_fields)
                    ->where_in(TABLE_USERS . '.role', array('agent', 'admin'))
                    ->where('us.site_id', $site_id)
                    ->where($where)
                    ->from(TABLE_USERS)
                    ->join(TABLE_USERS_TO_SITES . ' us', 'us.user_id = ' . TABLE_USERS . '.id')
                    ->get()
                    ->row();
        }

        return $result;
    }

    /*
     * This function will return online agents list in particular department
     * 
     * @param $site_id
     * @param $department_id
     * @return $online_users array of online users
     */

    function get_online_agents_list($site_id, $department_id = '') {
        $online_users = array();
        $online_activity_time = date("Y-m-d H:i:s", (now() - (15)));
        $mobile_online_activity_time = date("Y-m-d H:i:s", (now() - (5 * 60)));
        $desktop_online_activity_time = date("Y-m-d H:i:s", (now() - (5 * 60)));

        $this->user_select_fields = str_replace(TABLE_USERS . '.email, ', "", $this->user_select_fields);

        if ($department_id) {
            $where = "(" . TABLE_USERS . ".last_activity_time > '" . $online_activity_time . "' or " . TABLE_USERS . ".mobile_last_activity_time > '" . $mobile_online_activity_time . "' or " . TABLE_USERS . ".desktop_last_activity_time > '" . $desktop_online_activity_time . "')";
            $users = $this->db->select($this->user_select_fields)
                    ->where_in(TABLE_USERS . '.role', array('agent', 'admin'))
                    ->where(TABLE_USER_TAGS . '.tag_id', $department_id)
                    ->where('us.site_id', $site_id)
                    ->where($where)
                    ->from(TABLE_USERS)
                    ->join(TABLE_USER_TAGS, TABLE_USER_TAGS . '.user_id = ' . TABLE_USERS . '.id')
                    ->join(TABLE_USERS_TO_SITES . ' us', 'us.user_id = ' . TABLE_USERS . '.id')
                    ->get()
                    ->result();
        } else {
            $where = "(last_activity_time > '" . $online_activity_time . "' or mobile_last_activity_time > '" . $mobile_online_activity_time . "' or desktop_last_activity_time > '" . $desktop_online_activity_time . "')";
            $users = $this->db->select($this->user_select_fields)
                    ->where_in(TABLE_USERS . '.role', array('agent', 'admin'))
                    ->where('us.site_id', $site_id)
                    ->where($where)
                    ->from(TABLE_USERS)
                    ->join(TABLE_USERS_TO_SITES . ' us', 'us.user_id = ' . TABLE_USERS . '.id')
                    ->get()
                    ->result();
        }

        return $users;
    }

    /*
     * This function will insert new notification in database.
     * 
     * @param $notification_data
     * 
     * @return insert_id
     */

    function insert_notification($notification_data) {
        if (isset($notification_data['chat_session_id']) and $notification_data['chat_session_id'] > 0 and $notification_data['notification_type'] == 'message') {
            $query = $this->db->get_where(TABLE_NOTIFICATIONS, array('notification_type' => 'message', 'chat_session_id' => $notification_data['chat_session_id'], 'notification_status' => 'unread'));
            if ($query->row()) {
                return true;
            }
        }

        $notification_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->db->insert(TABLE_NOTIFICATIONS, $notification_data);
        $insert_id = $this->db->insert_id();

        return $insert_id;
    }

    /*
     * This function mark notification as read.
     * 
     * @param $where
     * 
     * @return true or false
     */

    function mark_notification_read($where = array()) {
        if (is_array($where) and empty($where) === false) {
            $this->db->update(TABLE_NOTIFICATIONS, array('notification_status' => 'read'), $where);
            return true;
        }

        return false;
    }

    /*
     * This function will reatun all unread notifications of an agent.
     * 
     * @param $agetn_id
     * 
     * @return $notifications
     */

    function get_notifications($agetn_id) {
        $time_before_3day = date("Y-m-d H:i:s", strtotime(date("Y-m-d H:i:s", now()) . ' - 3 day'));

        $notifications = $this->db->select(TABLE_NOTIFICATIONS . '.id, '
                        . TABLE_NOTIFICATIONS . '.chat_session_id as chatSessionId, '
                        . TABLE_NOTIFICATIONS . '.request_id as requestId, '
                        . TABLE_NOTIFICATIONS . '.message, '
                        . TABLE_NOTIFICATIONS . '.display_message as displayMessage, '
                        . TABLE_NOTIFICATIONS . '.sender_id as senderId, '
                        . TABLE_NOTIFICATIONS . '.notification_type as notificationType, '
                        . 'sender.name, '
                        . 'sender.profile_color, '
                        . 'sender.email, '
                        . " CASE WHEN sender.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . '/' . PROFILE_PICS) . "/thumb/', sender.profile_pic) END  as profilePic"
                )->where(TABLE_NOTIFICATIONS . '.receiver_id', $agetn_id)
                ->where(TABLE_NOTIFICATIONS . '.notification_status', 'unread')
                ->where(TABLE_NOTIFICATIONS . '.created_at >=', $time_before_3day)
                ->from(TABLE_NOTIFICATIONS)
                ->join(TABLE_USERS . ' sender', 'sender.id = ' . TABLE_NOTIFICATIONS . '.sender_id')
                ->order_by(TABLE_NOTIFICATIONS . ".id", 'desc')
                ->get()
                ->result();

        return $notifications;
    }

    /*
     * This function return chat sessions which have unread message.
     * 
     * @param $user_id
     * 
     * @return array of session ids
     */

    function get_running_session($user_id) {
        $sessions_list = $this->db->select(TABLE_CHAT_USERS . ".chat_session_id, ( select count(*) from " . TABLE_CHAT_MESSAGES . " cm where cm.sender_id != " . $user_id . " and cm.chat_session_id = " . TABLE_CHAT_USERS . ".chat_session_id and cm.message_status = 'unread' ) as unread_messages")
                ->where(TABLE_CHAT_USERS . '.user_id', $user_id)
                ->where(TABLE_CHAT_USERS . '.user_state', 'in-chat')
                ->where(TABLE_CHAT_SESSIONS . '.session_status', 'open')
                ->from(TABLE_CHAT_USERS)
                ->join(TABLE_CHAT_SESSIONS, TABLE_CHAT_SESSIONS . '.id = ' . TABLE_CHAT_USERS . '.chat_session_id')
                ->order_by(TABLE_CHAT_SESSIONS . ".id", 'asc')
                ->get()
                ->result();

        $running_sessions = array();
        foreach ($sessions_list as $session) {
            if ($session->unread_messages > 0) {
                $running_sessions[] = $session->chat_session_id;
            }
        }
        return implode(",", $running_sessions);
    }

    /*
     * This function will enter new message in database.
     * 
     * @param $local_id
     * 
     * @return $message_id or false
     */

    public function add_message($local_id) {
        // fetch message by local id
        $query = $this->db->get_where(TABLE_CHAT_MESSAGES, array('local_id' => $local_id));
        $message = $query->row();
        if ($message) {
            return $message;
        }

        // creating session entry
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->db->insert(TABLE_CHAT_MESSAGES, $this->model_data);
        $message_id = $this->db->insert_id();

        return $message_id;
    }

    /*
     * This function will close chat whic are closed but status is not changed yet.
     */

    function closeClosedSessions() {
        $online_users = $this->get_online_users();

        $sessions = $this->db->select(TABLE_CHAT_SESSIONS . '.id, MAX(' . TABLE_CHAT_MESSAGES . '.created_at) as created_at')
                ->where_in(TABLE_CHAT_SESSIONS . '.session_status', array('open', 'requested', 'forward', 'disconnected', 'on-hold'))
                ->from(TABLE_CHAT_MESSAGES)
                ->join(TABLE_CHAT_SESSIONS, TABLE_CHAT_SESSIONS . '.id = ' . TABLE_CHAT_MESSAGES . '.chat_session_id')
                ->group_by(TABLE_CHAT_SESSIONS . '.id')
                ->order_by('created_at', 'desc')
                ->limit(10)
                ->get()
                ->result();

        $time_before_10mins = date("Y-m-d H:i:s", (now() - (15 * 60)));
        $update_data = array();
        foreach ($sessions as $key => $session) {
            if ($time_before_10mins > $session->created_at) {
                $visitor = $this->db->select(TABLE_CHAT_USERS . '.id')
                        ->where(TABLE_CHAT_USERS . '.chat_session_id', $session->id)
                        ->where(TABLE_CHAT_USERS . '.user_state', 'in-chat')
                        ->where(TABLE_CHAT_USERS . '.user_role', 'visitor')
                        ->from(TABLE_CHAT_USERS)
                        ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_CHAT_USERS . '.user_id')
                        ->get()
                        ->row();

                if (!$visitor || !isset($online_users[$visitor->id])) {
                    $update_data[$key] = array('id' => $session->id, 'session_status' => 'closed');
                }
            }
        }

        if (count($update_data) > 0) {
            $this->db->update_batch(TABLE_CHAT_SESSIONS, $update_data, 'id');
        }

        return $sessions;
    }

    /*
     * This function will enter new address in database.
     * 
     * @param $address
     * 
     * @return $message_id or false
     */

    public function add_address($address) {
        // creating address entry
        $address['created_at'] = date("Y-m-d H:i:s", now());
        $this->db->insert(TABLE_USER_ADDRESS, $address);
        $address_id = $this->db->insert_id();

        return $address_id;
    }

    /*
     * This function will increate visitor counter per day
     * 
     * @param: Int $site_id
     * @return Mixed
     */

    function increase_visitors_counter($site_id) {
        $created_at = date("Y-m-d", now());
        // fetch message by local id
        $query = $this->db->get_where(TABLE_DAILY_VISITORS, array('created_at' => $created_at, 'site_id' => $site_id));
        $record = $query->row();
        if ($record) {
            $visitors = $record->visitors + 1;
            $this->db->update(TABLE_DAILY_VISITORS, array('visitors' => $visitors), array('id' => $record->id));
            return true;
        } else {
            $inserData = array('visitors' => 1, 'created_at' => $created_at, 'site_id' => $site_id);
            $this->db->insert(TABLE_DAILY_VISITORS, $inserData);
            return $this->db->insert_id();
        }
    }

    /*
     * This function will increate pagevies counter per day
     * 
     * @param $pagevieData
     */

    function increase_pagevies_counter($pagevieData) {
        $created_at = date("Y-m-d", now());
        // fetch message by local id
        $query = $this->db->get_where(TABLE_DAILY_PAGEVIEWS, array('created_at' => $created_at, 'page_url' => $pagevieData['page_url'], 'site_id' => $pagevieData['site_id']));
        $record = $query->row();
        if ($record) {
            $counter = $record->counter + 1;
            $this->db->update(TABLE_DAILY_PAGEVIEWS, array('counter' => $counter), array('id' => $record->id));
            return true;
        } else {
            $pagevieData['created_at'] = $created_at;
            $pagevieData['counter'] = 1;

            $this->db->insert(TABLE_DAILY_PAGEVIEWS, $pagevieData);
            return $this->db->insert_id();
        }
    }

    /*
     * To deleted older i day entries from data of anonymous visitors
     */
    function del_older_anonymous_visitors() {
        $time_before_day = date("Y-m-d H:i:s", strtotime(date("Y-m-d H:i:s", now()) . ' - 1 day'));
        $where = " last_activity_time < '" . $time_before_day . "'";
        
        return $this->db->delete(TABLE_TEMP_VISITORS, $where);
    }

}
