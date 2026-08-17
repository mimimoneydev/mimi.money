<?php

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

class Site_model extends CP_Model {

    //model db table
    var $table = "";
    var $validation_rules = array(
        'site_name' => array(
            'field' => 'site_name',
            'label' => 'lang:site_name',
            'rules' => 'trim|required'
        ),
        'site_email' => array(
            'field' => 'site_email',
            'label' => 'lang:site_email',
            'rules' => 'trim|required|valid_email'
        ),
        'site_url' => array(
            'field' => 'site_url',
            'label' => 'lang:site_url',
            'rules' => 'trim|required|callback__domain_check'
        )
    );

    /*
     * construct function 
     * defins model table name
     */

    public function __construct() {
        // Call the CI_Model constructor
        parent::__construct();
        $this->table = TABLE_SITES;
    }

    /*
     * To create new entry of resource
     * 
     * @return new insert id 
     */

    public function create() {
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());

        $this->db->insert($this->table, $this->model_data);
        return $this->db->insert_id();
    }

    /*
     * To update entry data     * 
     * 
     * @param: $id
     * 
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update($id) {
        $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
        if ($this->db->update($this->table, $this->model_data, array("id" => $id))) {
            return true;
        }

        return false;
    }

    /*
     * To return all record from database
     * 
     * @param Array $conditions
     */

    function get_sites($conditions = array()) {
        $query = $this->db->get_where($this->table, $conditions);

        return $query->result();
    }

    /*
     * Get all where conditions in
     * 
     * @param Array $conditions
     */

    function get_sites_in($conditions = array(), $select = '*') {
        $query = $this->db->select($select);
        foreach ($conditions as $field_name => $values) {
            $query->where_in($field_name, $values);
        }

        $sites = $query->from($this->table)
                ->get()
                ->result();

        return $sites;
    }

    /*
     * To validate token in databse
     * 
     * @param (string) $token
     * @return $response
     */

    function is_valid($token) {
        $query = $this->db->get_where($this->table, array('token' => $token));
        $response = $query->row();

        return $response;
    }

    /*
     * Get chat sessions by site id
     * 
     * @param Int $site_id
     * @return Array $chat_sessions
     */
    function get_chat_sessions($site_id) {
        $query = $this->db->where('site_id', $site_id)->get(TABLE_CHAT_SESSIONS);
        $chat_sessions = $query->result();
        
        return $chat_sessions;
    }

}
