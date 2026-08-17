<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_password_reminders extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'email' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'token' => array(
                'type' => 'VARCHAR',
                'constraint' => 25,
                'default' => ''
            ),
            'created_at' => array(
                'type' => 'INT',
                'constraint' => 10,
                'unsigned' => TRUE
            )
        ));

        $this->dbforge->add_key(array('email'), FALSE);
        $this->dbforge->add_key(array('token'), FALSE);
        $this->dbforge->create_table('password_reminders', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('password_reminders', TRUE);
    }

}
