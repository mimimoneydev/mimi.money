<?php

class Language extends CP_Model {

    public $table = "";
    public $validation_rules = array(
        'lang_name' => array(
            'field' => 'lang_name',
            'label' => 'lang:lang_name',
            'rules' => 'trim|required'
        ),
        'machine_name' => array(
            'field' => 'machine_name',
            'label' => 'lang:machine_name',
            'rules' => 'trim|required|alpha_dash|callback_langname_check'
        )
    );

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_LANGUAGES;
    }

    /*
     * To save tag's data
     */

    public function insert() {
        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['machine_name'] = strtolower($this->model_data['machine_name']);

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
        $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
        if (isset($this->model_data['machine_name'])) {
            $this->model_data['machine_name'] = strtolower($this->model_data['machine_name']);
        }

        if ($this->db->update($this->table, $this->model_data, array("id" => $id))) {
            return true;
        }

        return false;
    }

    /*
     * Deactivate all languages
     */

    function deactivate_langs() {
        if ($this->db->update($this->table, array("lang_status" => 'unpublish'), array("lang_status" => 'publish'))) {
            return true;
        }

        return false;
    }

}
