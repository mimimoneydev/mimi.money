<?php

/**
 * Created by PhpStorm.
 * User: gaxon
 * Date: 30-09-2015
 * Time: 12:01
 */
class Orequest extends CP_Model {

    public $table = "";
    public $validation_rules = array(
        "insert" => array(
            array(
                'field' => 'visitor_id',
                'label' => 'Visitor ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'visitor_note',
                'label' => 'Visitor Note',
                'rules' => 'required'
            )
        ),
        "update" => array(
            array(
                "field" => "request_status",
                "label" => "Request Status",
                "rules" => "required"
            )
        ),
        "reply" => array(
            array(
                "field" => "sender_id",
                "label" => "Sender Is",
                "rules" => "required"
            ),
            array(
                "field" => "message",
                "label" => "Message",
                "rules" => "required"
            )
        )
    );

    /*
     * construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_OFFLINE_REQUESTS;
    }

    /*
     * This function will create new offline request.
     * It will also create an entry in conversation table.
     * 
     * @return $request_id
     */

    function create_request() {
        // inserting request
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->db->insert($this->table, $this->model_data);
        $request_id = $this->db->insert_id();

        // setting conversation data
        $conversations = array();
        $conversations['request_id'] = $request_id;
        $conversations['sender_id'] = $this->model_data['visitor_id'];
        $conversations['message'] = $this->model_data['visitor_note'];
        $conversations['message_status'] = 'unread';
        $conversations['conversation_type'] = 'query';
        $conversations['created_at'] = $this->model_data['created_at'];

        // inserting conversation query
        $this->db->insert(TABLE_OFFLINE_REQUEST_CONVERSATIONS, $conversations);

        return $request_id;
    }

    /*
     * This function will insert tag entry for request
     * 
     * @return $insert_id
     */

    function insert_request_tag() {
        $this->db->insert(TABLE_OFFLINE_REQUEST_TAGS, $this->model_data);
        $insert_id = $this->db->insert_id();
        
        return $insert_id;
    }

    /*
     * This function will return offline requests
     * we can pass $user_id for any particular user, By default its return all requests
     * 
     * @param $user_id (optional)
     * 
     * @return $requests (array of requests)
     */

    function pending_offline_requests($user_id = '') {
        $department_ids = array();
        $sites_ids = array();
        if ($user_id) {
            /*$tags_users = $this->db->select('tag_id')->where('user_id', $user_id)->get(TABLE_USER_TAGS)->result();
            foreach ($tags_users as $tag) {
                $department_ids[] = $tag->tag_id;
            }*/
            
            $users_to_sites = $this->db->select('site_id')->where('user_id', $user_id)->get(TABLE_USERS_TO_SITES)->result();
            foreach ($users_to_sites as $site) {
                $sites_ids[] = $site->site_id;
            }
        }

        // select fileds
        $select = 'oreqs.id, '
                . 'oreqs.visitor_note, '
                . 'oreqs.request_status, '
                . 'oreqs.created_at, '
                . 'user.id as sender_id, '
                . 'user.name, '
                . 'user.email, '
                . 'user.profile_color, '
                . 'user.profile_pic, '
                . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture";

        $this->db->select($select);

        /*if (count($department_ids) > 0) {
            $this->db->where_in('ortag.tag_id', $department_ids);
        }*/
        
        if (count($sites_ids) > 0) {
            $this->db->where_in('oreqs.site_id', $sites_ids);
        }

        $this->db->where('oreqs.request_status', 'pending');

        $query = $this->db->from(TABLE_OFFLINE_REQUESTS . ' oreqs')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'oreqs.visitor_id');

        if (count($department_ids) > 0) {
            $query->join(TABLE_OFFLINE_REQUEST_TAGS . ' ortag', 'ortag.orequest_id = ' . 'oreqs.id', 'left');
        }

        $requests = $query->order_by("oreqs.id", 'desc')
                ->group_by("oreqs.id")
                ->get()
                ->result();

        return $requests;
    }
    
    /*
     * This function will return offline requests
     * we can pass $user_id for any particular user, By default its return all requests
     * 
     * @param $user_id (optional)
     * @param $offset (optional)
     * @param $set_limit (optional)
     * 
     * @return $requests (array of requests)
     */

    function get_offline_requests($user_id = '', $offset = 0, $set_limit = false) {
        $department_ids = array();
        $sites_ids = array();
        if ($user_id) {
            /*$tags_users = $this->db->select('tag_id')->where('user_id', $user_id)->get(TABLE_USER_TAGS)->result();
            foreach ($tags_users as $tag) {
                $department_ids[] = $tag->tag_id;
            }*/
            
            $users_to_sites = $this->db->select('site_id')->where('user_id', $user_id)->get(TABLE_USERS_TO_SITES)->result();
            foreach ($users_to_sites as $site) {
                $sites_ids[] = $site->site_id;
            }
        }

        // select fileds
        $select = 'oreqs.id, '
                . 'oreqs.visitor_note as message, '
                . 'oreqs.request_status, '
                . 'oreqs.created_at, '
                . 'tag.tag_name as department, '
                . 'user.id as sender_id, '
                . 'user.name, '
                . 'user.email, '
                . 'user.profile_color, '
                . 'user.profile_pic, '
                . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture";

        $this->db->select($select);

        /*if (count($department_ids) > 0) {
            $this->db->where_in('ortag.tag_id', $department_ids);
        }*/
        
        if (count($sites_ids) > 0) {
            $this->db->where_in('oreqs.site_id', $sites_ids);
        }

        $this->db->where('oreqs.request_status', 'pending');

        $query = $this->db->from(TABLE_OFFLINE_REQUESTS . ' oreqs')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'oreqs.visitor_id');
        
        $query->join(TABLE_OFFLINE_REQUEST_TAGS . ' ortag', 'ortag.orequest_id = ' . 'oreqs.id', 'left');
        $query->join(TABLE_TAGS . ' tag', 'tag.id = ' . 'ortag.tag_id', 'left');

        if ($set_limit) {
            $query->limit($this->item_per_page, $offset);
        }

        $requests = $query->order_by("oreqs.id", 'desc')
                ->group_by("oreqs.id")
                ->get()
                ->result();

        return $requests;
    }

    /*
     * This function will return offline requests
     * 
     * @param $offset
     * 
     * @return $requests (array of requests)
     */

    function get_admin_requests($offset) {
        // select fileds
        $select = 'oreqs.id, '
                . 'oreqs.visitor_note as message, '
                . 'oreqs.request_status, '
                . 'oreqs.created_at, '
                . 'user.id as sender_id, '
                . 'user.name, '
                . 'user.email, '
                . 'user.profile_color, '
                . 'user.profile_pic, '
                . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture";

        $requests = $this->db->select($select)
                ->from(TABLE_OFFLINE_REQUESTS . ' oreqs')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'oreqs.visitor_id')
                ->limit($this->item_per_page, $offset)
                ->order_by("oreqs.id", 'desc')
                ->get()
                ->result();

        return $requests;
    }

    /*
     * This function will return offline request and visitor data
     * 
     * @param $request_id
     * 
     * @return $request (object of request)
     */

    function get_request_data($request_id) {
        // select fileds
        $select = 'oreqs.id, '
                . 'oreqs.visitor_note, '
                . 'oreqs.request_status, '
                . 'oreqs.created_at, '
                . 'user.id as uid, '
                . 'user.name, '
                . 'user.email, '
                . 'user.profile_color, '
                . 'user.profile_pic, '
                . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture";

        $request = $this->db->select($select)
                ->where('oreqs.id', $request_id)
                ->from(TABLE_OFFLINE_REQUESTS . ' oreqs')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'oreqs.visitor_id')
                ->get()
                ->row();

        return $request;
    }

    /*
     * This function will be use to fetch offline request conversation
     * 
     * $param $request_id
     * @return $conversations
     */

    public function get_conversation($request_id) {
        $this->db->update($this->table, array('request_status' => 'open'), array('id' => $request_id));
        
        // select fileds
        $select = 'oreqs_cav.id, '
                . 'oreqs_cav.request_id, '
                . 'oreqs_cav.message, '
                . 'oreqs_cav.message_status, '
                . 'oreqs_cav.created_at, '
                . 'user.id as sender_id, '
                . 'user.name, '
                . 'user.email, '
                . 'user.profile_color, '
                . 'user.profile_pic, '
                . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as profile_picture";

        $conversations = $this->db->select($select)
                ->where_in('oreqs_cav.request_id', $request_id)
                ->from(TABLE_OFFLINE_REQUEST_CONVERSATIONS . ' oreqs_cav')
                ->join(TABLE_USERS . ' user', 'user.id = ' . 'oreqs_cav.sender_id')
                ->order_by("oreqs_cav.id", 'asc')
                ->get()
                ->result();

        return $conversations;
    }
    
    /*
     * This function will return agents of request
     * 
     * @param $request
     * 
     * @return $agents
     */
    
    function get_request_agents($request) {

        $agents = $this->db->select('sender_id')
                ->where('request_id', $request->id)
                ->where('sender_id !=', $request->uid)
                ->from(TABLE_OFFLINE_REQUEST_CONVERSATIONS)
                ->group_by("sender_id")
                ->get()
                ->result();

        return $agents;
    }

    /*
     * This function will insert reply for request
     * 
     * @param $request_id
     * 
     * @return $reply_id
     */

    function reply_request($request_id) {
        // updating offline request status open
        $this->db->update($this->table, array('request_status' => 'open'), array('id' => $request_id));

        // insert request reply
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->db->insert(TABLE_OFFLINE_REQUEST_CONVERSATIONS, $this->model_data);

        return $this->db->insert_id();
    }

}
