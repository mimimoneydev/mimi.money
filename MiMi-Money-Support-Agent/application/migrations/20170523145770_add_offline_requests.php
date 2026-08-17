<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_offline_requests extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'visitor_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'address_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
                'default' => 0
            ),
            'visitor_note' => array(
                'type' => 'mediumtext'
            ),
            'request_status' => array(
                'type' => 'enum',
                'constraint' => array('pending','open','closed'),
                'default' => 'pending'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('visitor_id', 'address_id'), FALSE);
        $this->dbforge->create_table('offline_requests', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('offline_requests', TRUE);
    }

}
