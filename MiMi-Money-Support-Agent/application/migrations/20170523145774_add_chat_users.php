<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_chat_users extends CI_Migration {

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
            'user_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'address_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
                'default' => 0
            ),
            'user_role' => array(
                'type' => 'enum',
                'constraint' => array('agent','visitor'),
                'default' => 'visitor'
            ),
            'is_typing' => array(
                'type' => 'char',
                'default' => 0
            ),
            'user_state' => array(
                'type' => 'enum',
                'constraint' => array('in-chat','left'),
                'default' => 'in-chat'
            ),
            'started_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (chat_session_id) REFERENCES ' . $this->db->dbprefix . 'chat_sessions(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (user_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('chat_session_id', 'user_id'), FALSE);
        $this->dbforge->add_key(array('address_id'), FALSE);
        
        $this->dbforge->create_table('chat_users', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('chat_users', TRUE);
    }

}
