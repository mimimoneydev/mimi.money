<?php
class Password_reminder extends CP_Model {
    
    public function __construct() {
        parent::__construct();
        $this->table = TABLE_PASSWORD_REMINDERS;
    }
    
    public $validation_rules = array(
        // rules for forget password
        'forget_password' => array(
            array(
                'field' => 'email',
                'label' => 'Email',
                'rules' => 'required|valid_email'
            )
        ),
        
         //rules to update password of a user
        'update_password' => array(
            array(
                'field' => 'pass',
                'label' => 'Password',
                'rules' => 'required'
            ),
            array(
                'field' => 'confirm_pass',
                'label' => 'Confirm Password',
                'rules' => 'required|matches[pass]'
            )
        )
    );
    
    public function temp_reset_password($pass_token, $user_email) {
        $this->password_reminder->delete_where(array('email' => $user_email), TABLE_PASSWORD_REMINDERS);

        $data = array(
            'email' => $user_email,
            'token' => $pass_token,
            'created_at' =>  $_SERVER["REQUEST_TIME"]
        );

        if ($data) {
            $this->password_reminder->model_data = $data;
            $insert_id = $this->password_reminder->insert();
            return TRUE;
        } else {
            return FALSE;
        }
    }

    /*
     * This function will check pass token is valid or not
     * 
     * @param $pass_token
     * 
     * @return false or user object
     */
    public function is_temp_pass_valid($pass_token) {
        $this->db->where('token', $pass_token);
        $query = $this->db->get($this->table);
        return $query->row();
    }
    
     /*
     * To save user's data
     */

    public function insert() {
        $this->db->insert($this->table, $this->model_data);
        return $this->db->insert_id();
    }
}