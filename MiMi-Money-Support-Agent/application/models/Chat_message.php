<?php

class Chat_message extends CP_Model {

    //model db table
    var $table = "";
    public $validation_rules = array(
        // rules for chat forward
        'new_message' => array(
            array(
                'field' => 'chat_message',
                'label' => 'Message',
                'rules' => 'required'
            ),
            array(
                'field' => 'sort_order',
                'label' => 'Sort Order',
                'rules' => 'required'
            ),
            array(
                'field' => 'chat_session_id',
                'label' => 'Chat Session ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'sender_id',
                'label' => 'Sender',
                'rules' => 'required'
            )
        ),
        'operator_message' => array(
            array(
                'field' => 'chat_message',
                'label' => 'Message',
                'rules' => 'required'
            ),
            array(
                'field' => 'sort_order',
                'label' => 'Sort Order',
                'rules' => 'required'
            ),
            array(
                'field' => 'csid',
                'label' => 'Chat Session ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'sender_id',
                'label' => 'Sender',
                'rules' => 'required'
            )
        ),
        'new_base64file' => array(
            array(
                'field' => 'filename',
                'label' => 'File',
                'rules' => 'required'
            )
        )
    );

    /*
     * define construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_CHAT_MESSAGES;
    }

    /*
     * This function will fetch and return all mesages in current chat session.
     * 
     * @param $chat_session_id
     * @return $messages (all message of current chat)
     */

    public function get_chat_messages($chat_session_id, $last_id = 0) {
        $messages_list = $this->db->select('msg.id, '
                        . 'msg.chat_session_id, '
                        . 'msg.sender_id, '
                        . 'msg.chat_message, '
                        . 'msg.message_meta, '
                        . 'msg.message_type, '
                        . 'msg.sort_order, '
                        . 'msg.message_status, '
                        . 'user.name, '
                        . 'user.display_name, '
                        . 'user.email, '
                        . 'user.role, '
                        . 'user.profile_color, '
                        . 'user.profile_pic, '
                        . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture"
                )->where('msg.chat_session_id', $chat_session_id)
                ->where('msg.id >', $last_id)
                ->from(TABLE_CHAT_MESSAGES . ' msg')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'msg.sender_id')
                ->order_by("msg.sort_order", 'asc')
                ->get()
                ->result();

        return $messages_list;
    }

    /*
     * This function will fetch and return all mesages.
     * 
     * @param $chat_session_id
     * @return $messages (all message of current chat)
     */

    public function get_chat_transcript($chat_session_id) {
        $messages_list = $this->db->select('msg.id, '
                        . 'msg.sender_id, '
                        . 'msg.chat_message, '
                        . 'msg.message_meta, '
                        . 'msg.message_type, '
                        . 'msg.sort_order, '
                        . 'msg.message_status, '
                        . 'user.name, '
                        . 'user.display_name, '
                        . 'user.email, '
                        . 'user.role, '
                        . 'user.profile_color, '
                        . 'user.profile_pic'
                )->where('msg.chat_session_id', $chat_session_id)
                ->from(TABLE_CHAT_MESSAGES . ' msg')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'msg.sender_id')
                ->order_by("msg.sort_order", 'asc')
                ->get()
                ->result();

        return $messages_list;
    }

    /*
     * This function will update message as read
     * 
     * @param $last_read_id
     * 
     * @return true or false
     */

    function mark_message_read($chat_session_id, $last_read_id = 0) {
        $this->db->update($this->table, array('message_status' => 'read'), array('chat_session_id' => $chat_session_id, 'id <=' => $last_read_id));
        return true;
    }
}
