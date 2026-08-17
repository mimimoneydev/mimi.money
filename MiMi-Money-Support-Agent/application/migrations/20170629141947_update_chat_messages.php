<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Update_chat_messages extends CI_Migration {

    public function up() {
        $fields = array(
            'message_type' => array(
                'type' => 'VARCHAR',
                'constraint' => 30,
                'default' => 'text'
            ),
            'message_meta' => array(
                'type' => 'TEXT',
                'null' => TRUE
            )
        );
        $this->dbforge->add_column('chat_messages', $fields, 'chat_message');
    }

    public function down() {
        $this->dbforge->drop_column('chat_messages', 'message_type');
        $this->dbforge->drop_column('chat_messages', 'message_meta');
    }

}
