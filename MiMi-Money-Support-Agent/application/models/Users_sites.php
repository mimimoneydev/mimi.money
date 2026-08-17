<?php

class Users_sites extends CP_Model {

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_USERS_TO_SITES;
    }

    /*
     * Save entries in database
     * 
     * @param: $user_id (id of the user to associate the tags with)
     * 
     */

    public function save($user_id) {
        $this->db->delete($this->table, array('user_id' => $user_id));

        if ($this->model_data['sites'] and is_array($this->model_data['sites'])) {
            $insertData = array();
            foreach ($this->model_data['sites'] as $site_id) {
                if ($site_id) {
                    $insertData[] = array('user_id' => $user_id, 'site_id' => $site_id);
                }
            }

            if (count($insertData) > 0) {
                $this->db->insert_batch($this->table, $insertData);
            }
        }
    }

    /*
     * Add new visitor site
     * 
     * @param: Int $user_id
     * @param: Int $site_id
     * @return Mixed
     */
    function add_visitor_site($user_id, $site_id) {
        $conditions = array('user_id' => $user_id, 'site_id' => $site_id);
        $query = $this->db->get_where($this->table, $conditions);
        
        if($entry = $query->row()) {
            return $entry;
        } else {
            $insertData = array('user_id' => $user_id, 'site_id' => $site_id);
            $this->db->insert($this->table, $insertData);
            return $this->db->insert_id();
        }
    }

    /*
     * Get sites on user
     * 
     * @param Int $user_id
     * @return Array $sites
     */
    public function get_sites($user_id) {
        $sites = $this->db->select('usite.*, site.site_name')
                ->where('usite.user_id', $user_id)
                ->from(TABLE_USERS_TO_SITES . ' usite')
                ->join(TABLE_SITES . ' site', 'site.id = usite.site_id')
                ->get()
                ->result();

        return $sites;
    }

}
