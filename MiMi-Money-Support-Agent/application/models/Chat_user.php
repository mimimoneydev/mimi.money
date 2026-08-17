<?php

class Chat_user extends CP_Model {

    //model db table
    var $table = "";

    /*
     * define construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_CHAT_USERS;
    }

    /*
     * This function will return current agent of chat session
     * 
     * @param $chat_session_id
     * @return $agent;
     */

    public function get_agent($chat_session_id) {
        $agent = new stdClass();
        $agent->id = 0;
        $agent->name = 0;
        $agent->profilePic = '';

        $user = $this->db->select(TABLE_CHAT_USERS . '.is_typing, ' . $this->user_select_fields)
                ->where(TABLE_CHAT_USERS . '.chat_session_id', $chat_session_id)
                ->where(TABLE_CHAT_USERS . '.user_state', 'in-chat')
                ->where(TABLE_CHAT_USERS . '.user_role', 'agent')
                ->from(TABLE_CHAT_USERS)
                ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_CHAT_USERS . '.user_id')
                ->get()
                ->row();

        if ($user) {
            $agent = $user;
            $agent->profilePic = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $user->email);
            
            $user->email = '';
        }

        return $agent;
    }

    /*
     * This function will return current visitor of chat session
     * 
     * @param $chat_session_id
     * @return $visitor;
     */

    public function get_visitor($chat_session_id) {
        $visitor = new stdClass();
        $visitor->id = 0;
        $visitor->name = 0;
        $visitor->profilePic = '';

        $user = $this->db->select(TABLE_CHAT_USERS . '.is_typing, '
                        . $this->user_select_fields . ', '
                        . TABLE_USER_VISIT_INFO . '.ip_address, '
                        . TABLE_USER_VISIT_INFO . '.page_title, '
                        . TABLE_USER_VISIT_INFO . '.page_url, '
                        . TABLE_USER_VISIT_INFO . '.meta_data, '
                        . TABLE_CHAT_USERS . '.started_at, '
                        . 'site.site_name, '
                        . 'site.site_url, '
                        . 'user_address.city, '
                        . 'user_address.state, '
                        . 'user_address.country'
                )
                ->where(TABLE_CHAT_USERS . '.chat_session_id', $chat_session_id)
                ->where(TABLE_CHAT_USERS . '.user_state', 'in-chat')
                ->where(TABLE_CHAT_USERS . '.user_role', 'visitor')
                ->from(TABLE_CHAT_USERS)
                ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_CHAT_USERS . '.user_id')
                ->join(TABLE_USER_ADDRESS . ' user_address', TABLE_CHAT_USERS . '.address_id = ' . 'user_address.id', 'left')
                ->join(TABLE_CHAT_SESSIONS . ' ch_sess', 'ch_sess.id = ' . TABLE_CHAT_USERS . ".chat_session_id")
                ->join(TABLE_SITES . ' site', 'site.id = ch_sess.site_id', 'left')
                ->join(TABLE_USER_VISIT_INFO, TABLE_USER_VISIT_INFO . ".request_id = " . TABLE_CHAT_USERS . ".chat_session_id and " . TABLE_USER_VISIT_INFO . ".request_type = 'online'", "left")
                ->get()
                ->row();

        if ($user) {
            $visitor = $user;
            $visitor->profilePic = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $user->email);
        }

        return $visitor;
    }

    /*
     * Function will update user typing status.
     * 
     * @param $is_typing;
     * @return true or false
     */

    function update_typing_status($is_typing = 0) {
        if ($this->db->update($this->table, array("is_typing" => $is_typing), $this->model_data)) {
            return true;
        }

        return false;
    }

}
