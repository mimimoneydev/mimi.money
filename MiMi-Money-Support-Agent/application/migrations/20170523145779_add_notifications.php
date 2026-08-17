<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_notifications extends CI_Migration {

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
            'receiver_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
                'default' => 0
            ),
            'request_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
                'default' => 0
            ),
            'message' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'unsigned' => TRUE
            ),
            'display_message' => array(
                'type' => 'TEXT'
            ),
            'notification_type' => array(
                'type' => 'enum',
                'constraint' => array('message','online_request','offline_request','offline_reply'),
                'default' => 'message'
            ),
            'notification_status' => array(
                'type' => 'enum',
                'constraint' => array('read','unread'),
                'default' => 'unread'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (chat_session_id) REFERENCES ' . $this->db->dbprefix . 'chat_sessions(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (sender_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (receiver_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('chat_session_id', 'sender_id'), FALSE);
        $this->dbforge->add_key(array('receiver_id', 'request_id'), FALSE);
        
        $this->dbforge->create_table('notifications', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('notifications', TRUE);
    }

}
