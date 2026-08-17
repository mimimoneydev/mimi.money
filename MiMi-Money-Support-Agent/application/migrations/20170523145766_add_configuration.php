<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_configuration extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'config_option' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'config_value' => array(
                'type' => 'mediumtext'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('configuration', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('configuration', TRUE);
    }

}
