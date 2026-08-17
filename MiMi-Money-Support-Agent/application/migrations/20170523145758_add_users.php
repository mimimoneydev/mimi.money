<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_users extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'name' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'email' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'display_name' => array(
                'type' => 'VARCHAR',
                'constraint' => 50,
                'default' => ''
            ),
            'contact_number' => array(
                'type' => 'VARCHAR',
                'constraint' => 25,
                'default' => ''
            ),
            'pass' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'profile_pic' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'user_status' => array(
                'type' => 'enum',
                'constraint' => array('blocked', 'active', 'deleted'),
                'default' => 'active'
            ),
            'role' => array(
                'type' => 'enum',
                'constraint' => array('admin','agent','visitor'),
                'default' => 'visitor'
            ),
            'remember_token' => array(
                'type' => 'VARCHAR',
                'constraint' => 25,
                'default' => ''
            ),
            'last_activity_time' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'mobile_last_activity_time' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'desktop_last_activity_time' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'last_login' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'modified_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('users', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('users', TRUE);
    }

}
