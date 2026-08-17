<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_daily_pageviews extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'counter' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'page_title' => array(
                'type' => 'VARCHAR',
                'constraint' => 150,
                'default' => ''
            ),
            'page_url' => array(
                'type' => 'TEXT'
            ),
            'created_at' => array(
                'type' => 'date',
                'default' => '0000-00-00'
            )
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->create_table('daily_pageviews', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('daily_pageviews', TRUE);
    }

}
