<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_sites extends CI_Migration {

    public function up() {
        $this->dbforge->rename_table('access_tokens', 'sites');
        
        $fields = array(
            'site_logo' => array(
                'type' => 'VARCHAR',
                'constraint' => 255,
                'default' => ''
            ),
            'site_email' => array(
                'type' => 'VARCHAR',
                'constraint' => 100,
                'default' => ''
            ),
            'site_name' => array(
                'type' => 'VARCHAR',
                'constraint' => 150,
                'default' => ''
            )
        );
        $this->dbforge->add_column('sites', $fields, 'id');
        
        $fields = array(
            'modified_at' => array(
                'type' => 'timestamp',
                'default' => '0000-00-00 00:00:00'
            )
        );
        $this->dbforge->add_column('sites', $fields, 'created_at');
    }

    public function down() {
        $this->dbforge->drop_column('sites', 'site_name');
        $this->dbforge->drop_column('sites', 'site_logo');
        $this->dbforge->drop_column('sites', 'site_email');
        $this->dbforge->drop_column('sites', 'modified_at');
        $this->dbforge->rename_table('sites', 'access_tokens');        
    }

}
