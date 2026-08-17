<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_chat_sessions extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'port' => array(
                'type' => 'VARCHAR',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            ),
            'requested_tag' => array(
                'type' => 'INT',
                'constraint' => 11,
                'null' => TRUE,
                'default' => 0
            ),
            'supported_tags' => array(
                'type' => 'mediumtext',
                'null' => TRUE
            ),
            'session_status' => array(
                'type' => 'enum',
                'constraint' => array('requested','open','forward','closed','disconnected','on-hold'),
                'default' => 'requested'
            ),
            'session_type' => array(
                'type' => 'enum',
                'constraint' => array('public','private'),
                'default' => 'public'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('chat_sessions', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('chat_sessions', TRUE);
    }

}
