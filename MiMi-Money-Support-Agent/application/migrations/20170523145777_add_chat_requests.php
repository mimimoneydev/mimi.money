<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_chat_requests extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'chat_session_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'sender_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'requested_to' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'requested_by' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
                'default' => 0
            ),
            'request_type' => array(
                'type' => 'enum',
                'constraint' => array('new','forward'),
                'default' => 'new'
            ),
            'message' => array(
                'type' => 'mediumtext'
            ),
            'request_status' => array(
                'type' => 'enum',
                'constraint' => array('pending','accepted','cancelled','closed'),
                'default' => 'pending'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (chat_session_id) REFERENCES ' . $this->db->dbprefix . 'chat_sessions(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (sender_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (requested_to) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('chat_session_id', 'sender_id'), FALSE);
        $this->dbforge->add_key(array('requested_to', 'requested_by'), FALSE);
        
        $this->dbforge->create_table('chat_requests', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('chat_requests', TRUE);
    }

}
