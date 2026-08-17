<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Update_user_visit_info extends CI_Migration {

    public function up() {
        $fields = array(
            'meta_data' => array(
                'type' => 'TEXT',
                'null' => TRUE
            ),
            'site_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            )
        );
        $this->dbforge->add_column('user_visit_info', $fields, 'id');
    }

    public function down() {
        $this->dbforge->drop_column('user_visit_info', 'site_id');
        $this->dbforge->drop_column('user_visit_info', 'meta_data');
    }   

}
