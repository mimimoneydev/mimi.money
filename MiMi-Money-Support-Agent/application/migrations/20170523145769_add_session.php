<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_session extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'varchar',
                'constraint' => 40
            ),
            'ip_address' => array(
                'type' => 'VARCHAR',
                'constraint' => 45,
                'default' => ''
            ),
            'timestamp' => array(
                'type' => 'INT',
                'constraint' => 10,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'data' => array(
                'type' => 'blob'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('timestamp'), FALSE);
        $this->dbforge->create_table('session', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('session', TRUE);
    }

}
