<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Create_anonymous_messages extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'temp_visitor_id' => array(
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
                'constraint' => 255
            ),
            'sort_order' => array(
                'type' => 'VARCHAR',
                'constraint' => 255
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
            'CONSTRAINT FOREIGN KEY (temp_visitor_id) REFERENCES ' . $this->db->dbprefix . 'temp_visitors(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('anonymous_messages', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('anonymous_messages', TRUE);
    }

}
