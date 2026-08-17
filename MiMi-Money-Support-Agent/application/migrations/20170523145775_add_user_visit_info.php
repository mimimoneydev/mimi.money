<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_user_visit_info extends CI_Migration {

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
            'request_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'request_type' => array(
                'type' => 'enum',
                'constraint' => array('online','offline'),
                'default' => 'online'
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
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (user_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('user_id', 'request_id'), FALSE);
        
        $this->dbforge->create_table('user_visit_info', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('user_visit_info', TRUE);
    }

}
