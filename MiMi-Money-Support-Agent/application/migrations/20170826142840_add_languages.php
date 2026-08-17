<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_languages extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'lang_name' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'machine_name' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'lang_flag' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'lang_status' => array(
                'type' => 'enum',
                'constraint' => array('publish','unpublish'),
                'default' => 'unpublish'
            ),
            'created_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            ),
            'modified_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('languages', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('languages', TRUE);
    }

}
