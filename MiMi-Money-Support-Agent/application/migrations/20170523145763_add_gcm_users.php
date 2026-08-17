<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_gcm_users extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'user_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'mac_address' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'name' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'user_status' => array(
                'type' => 'char',
                'constraint' => 1,
                'default' => 1
            ),
            'device_type' => array(
                'type' => 'enum',
                'constraint' => array('iOS','android'),
                'default' => 'android'
            ),
            'device_id' => array(
                'type' => 'longtext'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (user_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('user_id'), FALSE);
        $this->dbforge->create_table('gcm_users', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('gcm_users', TRUE);
    }

}
