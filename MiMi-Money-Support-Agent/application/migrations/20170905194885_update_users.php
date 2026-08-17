<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Update_users extends CI_Migration {

    public function up() {
        $fields = array(
            'profile_color' => array(
                'type' => 'VARCHAR',
                'constraint' => 10,
                'default' => ''
            )
        );
        $this->dbforge->add_column('users', $fields, 'profile_pic');
    }

    public function down() {
        $this->dbforge->drop_column('users', 'site_id');
    }   

}
