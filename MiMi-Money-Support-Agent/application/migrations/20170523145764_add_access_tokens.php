<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_access_tokens extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'site_url' => array(
                'type' => 'text'
            ),
            'token' => array(
                'type' => 'TEXT',
                'constraint' => 20,
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('access_tokens', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('access_tokens', TRUE);
    }

}
