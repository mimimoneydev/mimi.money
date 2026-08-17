<?php

class Gcm extends CP_Model {

    //model db table
    var $table = "";
    public $validation_rules = array(
        // rules for user login
        'login' => array(
            array(
                'field' => 'email',
                'label' => 'Email',
                'rules' => 'required|valid_email'
            ),
            array(
                'field' => 'password',
                'label' => 'Password',
                'rules' => 'required'
            ),
            array(
                'field' => 'mac_address',
                'label' => 'Mac Address',
                'rules' => 'required'
            ),
            array(
                'field' => 'device_id',
                'label' => 'Device ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'device_type',
                'label' => 'Device Type',
                'rules' => 'required'
            )
        ),
        // rules for user login
        'logout' => array(
            array(
                'field' => 'user_id',
                'label' => 'User ID',
                'rules' => 'required'
            ),
            array(
                'field' => 'mac_address',
                'label' => 'Mac Address',
                'rules' => 'required'
            )
        )
    );

    /*
     * define construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_GCM_USERS;
    }

    /*
     * This function will add or update gsm user by their mac address.
     * 
     */

    public function sysc_gsm_user() {
        $mac_address = $this->model_data['mac_address'];

        $query = $this->db->get_where($this->table, array('mac_address' => $mac_address));
        $gsm_user = $query->row();
        if ($gsm_user) {
            if ($this->db->update($this->table, $this->model_data, array('mac_address' => $mac_address))) {
                return true;
            }
        } else {
            $this->model_data['created_at'] = date("Y-m-d H:i:s", now());

            $this->db->insert($this->table, $this->model_data);
            return $this->db->insert_id();
        }
    }
    
    /*
     * This function will change user status as logout
     * 
     * @param $mac_address
     * 
     * @return true or false
     */
    
    function logout($mac_address) {
        return $this->db->update($this->table, array('user_status' => 0), array('mac_address' => $mac_address));
    }

}
