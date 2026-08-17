<?php

class Temp_visitor extends CP_Model {

    public $table = "";

    /*
     * validations riles
     */
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
                'field' => 'temp_visitor_id',
                'label' => 'Visitor ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'sender_id',
                'label' => 'Sender',
                'rules' => 'required'
            )
        )
    );

    /*
     * Profile colors options
     */
    private $colors = array('#f16364', '#f58559', '#f9a43e', '#e4c62e', '#67bf74', '#59a2be', '#2093cd', '#ad62a7', '#805781', '#e57373', '#f06292', '#a1887f'
        , '#ba68c8', '#9575cd', '#7986cb', '#64b5f6', '#4fc3f7', '#4dd0e1', '#4db6ac', '#81c784', '#aed581', '#ff8a65', '#d4e157', '#ffd54f', '#ffb74d', '#90a4ae');

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_TEMP_VISITORS;
    }

    /*
     * To save tag's data
     */

    public function insert() {
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['profile_color'] = $this->colors[rand(0, 25)];
        $this->db->insert($this->table, $this->model_data);
        return $this->db->insert_id();
    }

    /*
     * To update tag's data     * 
     * 
     * @param: $id (id of the tag)
     * 
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update($id) {
        if ($this->db->update($this->table, $this->model_data, array("id" => $id))) {
            return true;
        }

        return false;
    }

    /*
     * Get visitor where condition mathc
     * 
     * @param Array $where
     * @return Object $visitor
     */

    function get_visitor($temp_visitor_id) {
        $visitor = $this->db->select('visitor.*, '
                        . 'site.site_name, '
                        . 'site.site_url, '
                        . 'user.display_name as operator_name, '
                        . 'user.profile_color as operator_profile_color, '
                        . 'user.profile_pic as operator_profile_pic, '
                        . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END as operator_profile_picture "
                )
                ->where('visitor.id', $temp_visitor_id)
                ->from($this->table . ' visitor')
                ->join(TABLE_USERS . ' user', 'user.id = visitor.operator_id', 'left')
                ->join(TABLE_SITES . ' site', 'site.id = visitor.site_id', 'left')
                ->get()
                ->row();

        return $visitor;
    }

    /*
     * Get online available visitors
     * 
     * @param Object $user
     * @return Mixed $anonymous_users or False
     */

    function available_visitors($user) {
        if ($user and $user->id and count($user->sites) > 0) {
            $online_activity_time = date("Y-m-d H:i:s", (now() - (45)));
            $where = " last_activity_time > '" . $online_activity_time . "'";

            $this->db->select('site.site_name, '
                        . 'site.site_url, '
                        . 'visitor.*'
                );
                    
            $query = $this->db->where('visitor.status', 'present')
                    ->where($where);

            $query->where_in('visitor.site_id', $user->sites);
            $query->where('visitor.email !=', $user->email);

            $anonymous_users = $query->where_in('operator_id', array(0, $user->id))
                    ->from($this->table . ' visitor')
                    ->join(TABLE_SITES . ' site', 'site.id = visitor.site_id', 'left')
                    ->get()
                    ->result();

            return $anonymous_users;
        }

        return FALSE;
    }

    /*
     * Get all online visitord 
     * 
     * @param Int $site_id optional
     * @return Array $anonymous_users
     */
    function get_online_visitors($site_id = 0) {
        $online_activity_time = date("Y-m-d H:i:s", (now() - (45)));
        $where = " last_activity_time > '" . $online_activity_time . "'";

        $query = $this->db->where('status', 'present')
                ->where($where);

        if ($site_id) {
            $query->where('site_id', $site_id);
        }

        $anonymous_users = $query->from($this->table)
                ->get()
                ->result();

        return $anonymous_users;
    }

    /*
     * get all messages
     * 
     * @param Array $where
     */

    function get_messages($where = array()) {
        $messages = $this->db->get_where(TABLE_ANONUMOUS_MESSAGES, $where)->result();

        return $messages;
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
        $query = $this->db->get_where(TABLE_ANONUMOUS_MESSAGES, array('local_id' => $local_id));
        $message = $query->row();
        if ($message) {
            return $message;
        }

        // creating session entry
        $this->db->insert(TABLE_ANONUMOUS_MESSAGES, $this->model_data);
        $message_id = $this->db->insert_id();

        return $message_id;
    }

    /*
     * Iniciate a chat request for onlinne visitor
     * 
     * @param Int $temp_visitor_id
     * @param Int $operator_id
     * @return Boolean
     */

    function initiate_chat_request($temp_visitor_id, $operator_id) {
        $temp_visitor = $this->db->get_where($this->table, array('id' => $temp_visitor_id))->row();

        if ($temp_visitor and $temp_visitor->operator_id) {
            return TRUE;
        } elseif ($temp_visitor and $this->db->update($this->table, array('operator_id' => $operator_id), array("id" => $temp_visitor_id))) {
            return TRUE;
        }

        return FALSE;
    }

}
