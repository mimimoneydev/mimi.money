<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Create_temp_visitors extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'site_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'operator_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'chat_session_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'name' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'email' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'message' => array(
                'type' => 'mediumtext'
            ),
            'profile_color' => array(
                'type' => 'VARCHAR',
                'constraint' => 10,
                'default' => ''
            ),
            'ip_address' => array(
                'type' => 'VARCHAR',
                'constraint' => 70,
                'default' => ''
            ),
            'page_url' => array(
                'type' => 'TEXT'
            ),
            'page_title' => array(
                'type' => 'VARCHAR',
                'constraint' => 150,
                'default' => ''
            ),
            'meta_data' => array(
                'type' => 'TEXT',
                'null' => TRUE
            ),
            'latitude' => array(
                'type' => 'decimal',
                'constraint' => '10,7'
            ),
            'longitude' => array(
                'type' => 'decimal',
                'constraint' => '10,7'
            ),
            'city' => array(
                'type' => 'VARCHAR',
                'constraint' => 150,
                'default' => ''
            ),
            'state' => array(
                'type' => 'VARCHAR',
                'constraint' => 150,
                'default' => ''
            ),
            'country' => array(
                'type' => 'VARCHAR',
                'constraint' => 150,
                'default' => ''
            ),
            'status' => array(
                'type' => 'enum',
                'constraint' => array('present','leaved'),
                'default' => 'leaved'
            ),
            'last_activity_time' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('temp_visitors', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('temp_visitors', TRUE);
    }

}
