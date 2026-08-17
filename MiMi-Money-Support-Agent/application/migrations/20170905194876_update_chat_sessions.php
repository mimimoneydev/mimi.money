<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Update_chat_sessions extends CI_Migration {

    public function up() {
        $fields = array(
            'user_agent' => array(
                'type' => 'TEXT',
                'null' => TRUE
            ),
            'site_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            )
        );
        $this->dbforge->add_column('chat_sessions', $fields, 'id');
    }

    public function down() {
        $this->dbforge->drop_column('chat_sessions', 'site_id');
        $this->dbforge->drop_column('chat_sessions', 'user_agent');
    }   

}
