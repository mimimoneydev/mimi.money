<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Update_daily_pageviews extends CI_Migration {

    public function up() {
        $fields = array(
            'site_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'default' => 0
            )
        );
        $this->dbforge->add_column('daily_pageviews', $fields, 'id');
    }

    public function down() {
        $this->dbforge->drop_column('daily_pageviews', 'site_id');
    }   

}
