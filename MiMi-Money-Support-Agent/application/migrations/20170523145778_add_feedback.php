<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_feedback extends CI_Migration {

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
            'feedback_by' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'feedback_to' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'rating' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'feedback_text' => array(
                'type' => 'mediumtext'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'CONSTRAINT FOREIGN KEY (chat_session_id) REFERENCES ' . $this->db->dbprefix . 'chat_sessions(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (feedback_by) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (feedback_to) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('chat_session_id', 'feedback_by'), FALSE);
        $this->dbforge->add_key(array('feedback_to'), FALSE);
        
        $this->dbforge->create_table('feedback', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('feedback', TRUE);
    }

}
