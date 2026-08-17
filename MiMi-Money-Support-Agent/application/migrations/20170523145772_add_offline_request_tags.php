<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_offline_request_tags extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'orequest_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'tag_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
                'default' => 0
            ),
            'CONSTRAINT FOREIGN KEY (orequest_id) REFERENCES ' . $this->db->dbprefix . 'offline_requests(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (tag_id) REFERENCES ' . $this->db->dbprefix . 'tags(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('orequest_id', 'tag_id'), FALSE);
        
        $this->dbforge->create_table('offline_request_tags', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('offline_request_tags', TRUE);
    }

}
