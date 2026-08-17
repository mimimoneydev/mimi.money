<?php

class Canned_message extends CP_Model {

    //model db table
    var $table = "";
    public $validation_rules = array(
        array(
            'field' => 'title',
            'label' => 'lang:canned_title',
            'rules' => 'required'
        ),
        array(
            'field' => 'description',
            'label' => 'lang:canned_description',
            'rules' => 'required'
        )
    );

    /*
     * define construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_CANNED_MESSAGES;
    }

    /*
     * Get all enrties of agent and admins
     * 
     * @param Int $agent_id
     * @param String $role
     * 
     * @return Array $messages
     */

    function get_messages($agent_id, $role = 'agent') {
        $query = $this->db->select('cm.*');
        if ($role == 'agent') {
            $query->where('cm.user_id', $agent_id)
                ->or_where('role', 'admin');
        }
        
        $messages = $query->from($this->table . ' cm')
                ->join(TABLE_USERS . ' users', 'users.id = cm.user_id')
                ->order_by('title', 'asc')
                ->get()
                ->result();

        return $messages;
    }

    /*
     * Insert new entry in database
     * 
     * @return Object $message
     */

    public function insert() {
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['updated_at'] = date("Y-m-d H:i:s", now());

        $this->db->insert($this->table, $this->model_data);
        $message_id = $this->db->insert_id();
        $message = $this->db->get_where($this->table, array('id' => $message_id))->row();
        return $message;
    }

    /*
     * Updating existing entry     * 
     * 
     * @param Int $id
     * 
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update($id) {
        $this->model_data['updated_at'] = date("Y-m-d H:i:s", now());
        if ($this->db->update($this->table, $this->model_data, array("id" => $id))) {
            return true;
        }

        return false;
    }
    
    /*
     * Filter data and return all entries which match with filters
     * 
     * @param Array $filters
     * 
     * @return Array $chat_list (List of chat of user)
     */
    function filter_message ($filters = array()) {
        // select fileds
        $select = 'cm.*, '
                . 'users.name as userName, '
                . 'users.profile_color, '
                . 'users.profile_pic, '
                . "CASE WHEN users.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', users.profile_pic) END  as userProfilePic, ";
        
        $query = $this->db->select($select);
        
        if (isset($filters['keywords']) and $filters['keywords']) {
            $query->like('cm.title', $filters['keywords']);
            $query->or_like('cm.description', $filters['keywords']);
        }
        
        $messages = $query->from($this->table . ' cm')
                ->join(TABLE_USERS . ' users', 'users.id = cm.user_id')
                ->limit($this->item_per_page, $filters['offset'])
                ->order_by('title', 'asc')
                ->get()
                ->result();
        
        return $messages;
    }

}
