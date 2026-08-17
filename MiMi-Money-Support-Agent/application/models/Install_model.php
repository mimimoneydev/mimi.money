<?php

class Install_model extends CP_Model {
    /*
     * Profile colors options
     */

    private $colors = array('#f16364', '#f58559', '#f9a43e', '#e4c62e', '#67bf74', '#59a2be', '#2093cd', '#ad62a7', '#805781', '#e57373', '#f06292', '#a1887f'
        , '#ba68c8', '#9575cd', '#7986cb', '#64b5f6', '#4fc3f7', '#4dd0e1', '#4db6ac', '#81c784', '#aed581', '#ff8a65', '#d4e157', '#ffd54f', '#ffb74d', '#90a4ae');

    /**
     * Constructor
     * 
     * @access	public
     * @return	void
     */
    function __construct() {
        parent::__construct();

        $this->sql_path = APPPATH . 'models/installer.sql';

        // load the file helper
        $this->load->helper('file');
    }

    /*
     * This function run database sql file
     * 
     * @return true or false
     */

    public function use_sql_string() {
        $sql = read_file($this->sql_path);
        // Trim it
        $sql = trim($sql);
        return mysqli_multi_query($this->db->conn_id, $sql);
    }

    /*
     * To save user's data
     */

    public function save_admin() {
        $query = $this->db->get_where(TABLE_USERS, array('email' => $this->model_data['email']));
        $admin = $query->row();
        
        $this->model_data['name'] = ucfirst($this->model_data['name']);
        $this->model_data['display_name'] = ucfirst($this->model_data['display_name']);

        if ($admin) {
            $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
            $this->model_data['profile_color'] = $this->colors[rand(0, 25)];
            $this->db->update(TABLE_USERS, $this->model_data, array("id" => $admin->id));
            return true;
        } else {
            $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
            $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
            $this->model_data['profile_color'] = $this->colors[rand(0, 25)];

            $this->db->insert(TABLE_USERS, $this->model_data);
            return $this->db->insert_id();
        }
    }

}
