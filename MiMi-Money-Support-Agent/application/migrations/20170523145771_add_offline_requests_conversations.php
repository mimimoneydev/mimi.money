<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_offline_requests_conversations extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'request_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'sender_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'message' => array(
                'type' => 'mediumtext'
            ),
            'message_status' => array(
                'type' => 'enum',
                'constraint' => array('read','unread'),
                'default' => 'unread'
            ),
            'conversation_type' => array(
                'type' => 'enum',
                'constraint' => array('query','reply'),
                'default' => 'query'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (request_id) REFERENCES ' . $this->db->dbprefix . 'offline_requests(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('request_id', 'sender_id'), FALSE);
        $this->dbforge->create_table('offline_requests_conversations', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('offline_requests_conversations', TRUE);
    }

}
