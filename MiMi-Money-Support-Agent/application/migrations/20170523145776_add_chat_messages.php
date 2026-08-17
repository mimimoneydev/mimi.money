<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_chat_messages extends CI_Migration {

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
            'local_id' => array(
                'type' => 'VARCHAR',
                'constraint' => 25
            ),
            'sort_order' => array(
                'type' => 'VARCHAR',
                'constraint' => 20
            ),
            'chat_message' => array(
                'type' => 'mediumtext'
            ),
            'message_status' => array(
                'type' => 'enum',
                'constraint' => array('read','unread'),
                'default' => 'unread'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (chat_session_id) REFERENCES ' . $this->db->dbprefix . 'chat_sessions(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (sender_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('chat_session_id', 'sender_id'), FALSE);
        $this->dbforge->add_key(array('local_id'), FALSE);
        
        $this->dbforge->create_table('chat_messages', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('chat_messages', TRUE);
    }

}
