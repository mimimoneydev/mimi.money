<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_tags extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'tag_name' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'tag_status' => array(
                'type' => 'enum',
                'constraint' => array('publish','unpublish'),
                'default' => 'publish'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('tags', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('tags', TRUE);
    }

}
