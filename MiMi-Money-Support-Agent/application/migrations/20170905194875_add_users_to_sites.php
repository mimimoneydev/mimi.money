<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Migration_Add_users_to_sites extends CI_Migration {

    public function up() {
        $this->dbforge->add_field(array(
            'id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'auto_increment' => TRUE
            ),
            'user_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE
            ),
            'site_id' => array(
                'type' => 'INT',
                'constraint' => 11,
                'unsigned' => TRUE,
                'null' => TRUE,
            ),
            'CONSTRAINT FOREIGN KEY (user_id) REFERENCES ' . $this->db->dbprefix . 'users(id) ON DELETE CASCADE ON UPDATE CASCADE',
            'CONSTRAINT FOREIGN KEY (site_id) REFERENCES ' . $this->db->dbprefix . 'sites(id) ON DELETE CASCADE ON UPDATE CASCADE'
        ));

        $this->dbforge->add_key('id', TRUE);
        $this->dbforge->add_key(array('user_id', 'site_id'), FALSE);
        
        $this->dbforge->create_table('users_to_sites', TRUE);
    }

    public function down() {
        $this->dbforge->drop_table('users_to_sites', TRUE);        
    }

}
