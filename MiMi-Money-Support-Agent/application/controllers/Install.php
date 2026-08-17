<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

class Install extends CP_AdminController {

    // Email and Password gets displayed at the end of the installer
    var $admin_password = '';
    var $admin_email = '';
    // Some database defaults and information that needs tracking throughout the process
    var $db_driver = 'mysqli';
    var $db_prefix = 'chatbull_';
    var $db_char_set = 'utf8';
    var $db_dbcollat = 'utf8_general_ci';
    // Figured out in the constructor and needed for the install process
    var $base_url = '';

    /**
     * Constructor
     * 
     * @access	public
     * @return	void
     */
    function __construct() {
        parent::__construct();

        $this->body_classes[] = 'intallation-process';
        $this->addcss(base_url("assets/cmodule/css/cmodule-installer.css"), TRUE);

        // Only show install page if there is no lock
        if ($this->config->item('installed') != 'no') {
            redirect('c=admin');
        }
    }

    /**
     * First Step
     *
     * We check if the server can be used to install NAB
     *
     * @access	public
     * @param	bool
     * @return	mixed
     */
    function index() {
        $data['install_warnings'] = array();

        // is PHP version ok?
        if (!is_php('5.1.6')) {
            $data['install_warnings'][] = $this->lang->line('old_php');
        }

        // is config file writable? we need to be sure of this before start
        if (is_really_writable($this->config->config_path) === FALSE && !@chmod($this->config->config_path, FILE_WRITE_MODE)) {
            $data['install_warnings'][] = $this->config->config_path . $this->lang->line('file_not_writable');
        }

        // is autoload file writable? we need to be sure of this before start
        if (is_really_writable($this->config->autoload_path) === FALSE && !@chmod($this->config->autoload_path, FILE_WRITE_MODE)) {
            $data['install_warnings'][] = $this->config->autoload_path . $this->lang->line('file_not_writable');
        }

        // is database file writable? we need to be sure of this before start
        if (is_really_writable($this->config->database_path) === FALSE && !@chmod($this->config->database_path, FILE_WRITE_MODE)) {
            $data['install_warnings'][] = $this->config->database_path . $this->lang->line('file_not_writable');
        }

        // No errors? let's move to the next step
        if (count($data['install_warnings']) == 0) {
            redirect('c=install&m=start');
        } else {
            $this->add_js_inline(array('intallation_step' => 'warnings'));
            $this->addjs(theme_url("js/app/install/main.js"), TRUE);
            $this->addjs(theme_url("js/app/install/install.js"), TRUE);
            $this->data['pagetitle'] = $this->lang->line('installation_problem');
            $data['content'] = $this->load->view('install/warnings', $data, TRUE);
            $this->load->view('install/template', $data);
        }
    }

    /**
     * Start (firts and only step)
     *
     * The install process unique step
     *
     * @access	public
     * @return	string
     */
    function start() {
        $data = array();

        $intallation_step = 'setup-db';

        if (@include($this->config->database_path)) {
            // load based on custom passed information
            if ($db[$active_group]['username'] and $db[$active_group]['database']) {
                $tables = $this->db->list_tables();
                if (count($tables) > 0 and $this->db->table_exists(TABLE_USERS)) {
                    $intallation_step = 'setup-user';
                }
            }
        }

        $this->add_js_inline(array('intallation_step' => $intallation_step));

        $this->addjs(theme_url("js/app/install/main.js"), TRUE);
        $this->addjs(theme_url("js/app/install/install.js"), TRUE);
        $this->data['pagetitle'] = $this->lang->line('welocme_installer');
        $data['content'] = $this->load->view('install/index', $data, TRUE);
        $this->load->view('install/template', $data);
    }

    /*
     * This function will check all tables are installed.
     * 
     * @return json object
     */

    function all_tables_created() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');
        $this->load->library('migration');

        if ($this->migration->latest() === FALSE) {
            $response['errors'] = $this->migration->error_string();
        } else {
            $this->settings = $this->configuration->get_settings(array('site_id' => 0));

            // upgrade site settings for version 5.x
            if ($this->settings and $this->settings->current_version and $this->settings->current_version < 5) {
                $sites = $this->site_model->get_sites();
                $this->configuration->upgrade_site_settings($sites, $this->settings);
            } else {
                $sites = $this->site_model->get_sites();
                $this->configuration->merge_settings_options($sites);
            }

            $options = $this->configuration->get_default();
            $new_options = array();

            foreach ($options as $opt) {
                if (!isset($this->settings->{$opt['config_option']})) {
                    $new_options[] = $opt;
                }
            }

            if (count($new_options) > 0) {
                $this->configuration->insert_options($new_options, TRUE);
            }

            if ($this->settings and $this->settings->current_version and $this->settings->current_version < 5) {
                // update for version 5.x
                $this->configuration->model_data = array('admin_panel_logo' => $this->settings->site_logo);
                $this->configuration->model_data['admin_panel_name'] = $this->settings->site_name;
                $this->configuration->model_data['admin_panel_email'] = $this->settings->site_email;
                $this->configuration->model_data['enable_agent_file_sharing'] = $this->settings->enable_file_sharing;
                $this->configuration->model_data['agent_file_upload_size'] = $this->settings->file_upload_size;
                $this->configuration->model_data['agent_allowed_filetypes'] = $this->settings->allowed_filetypes;
                $this->configuration->model_data['agent_time_interwal'] = $this->settings->time_interwal;
                $this->configuration->update();

                $users = $this->user->get_all();
                $profile_colors = array('#f16364', '#f58559', '#f9a43e', '#e4c62e', '#67bf74', '#59a2be', '#2093cd', '#ad62a7', '#805781', '#e57373', '#f06292', '#a1887f', '#ba68c8', '#9575cd', '#7986cb', '#64b5f6', '#4fc3f7', '#4dd0e1', '#4db6ac', '#81c784', '#aed581', '#ff8a65', '#d4e157', '#ffd54f', '#ffb74d', '#90a4ae');
                foreach ($users as $user) {
                    $this->user->model_data = array('profile_color' => $profile_colors[rand(0, 25)]);
                    $this->user->model_data['name'] = $user->name;
                    $this->user->model_data['display_name'] = $user->display_name;
                    $this->user->update($user->id);
                }
            }

            $response['db_installed'] = 'yes';
            $response['result'] = 'success';
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will setup database.
     * 
     * @return json response
     */

    function setup_db() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');

        $this->form_validation->set_rules('host', 'Host Name', 'required');
        $this->form_validation->set_rules('db_name', 'Database Name', 'required|callback__check_mysql_connection');
        $this->form_validation->set_rules('db_user', 'Database user', 'required');
        if ($this->form_validation->run() == TRUE) {

            // setup database config file
            if (!$this->_setup_database()) {
                $response['errors'] = $this->config->database_path . $this->lang->line('file_not_writable');
            } else {

                $update_autoload_data = array();
                $update_autoload_data['libraries'] = "'database', 'form_validation', 'curl', 'email',  'authentication', 'media'";
                if (!$this->config->autoload_update($update_autoload_data)) {
                    $response['errors'] = $this->lang->line('file_not_writable') . $this->config->autoload_update;
                } else {
                    $response['message'] = $this->lang->line('database_installed');
                    $response['result'] = 'success';
                }
            }
        } else {
            $response['errors'] = validation_errors();
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will setup user.
     * 
     * @return json response
     */

    function setup_user() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');

        $this->form_validation->set_message('required', $this->lang->line('reuired_field'));

        $this->form_validation->set_rules('name', 'Name', 'required');
        $this->form_validation->set_rules('email', 'Email', 'required|valid_email');
        $this->form_validation->set_rules('password', 'Password', 'required');

        if ($this->form_validation->run() == TRUE) {
            $this->load->model('install_model');

            $this->install_model->model_data = $this->input->post(array('name', 'email', 'display_name'));
            $this->install_model->model_data['pass'] = hashpassword($this->input->post('password'));
            $this->install_model->model_data['role'] = 'admin';
            $user_id = $this->install_model->save_admin();

            if ($user_id) {
                $main_settings = $this->configuration->get_settings(array('site_id' => 0));

                // load the file helper
                $this->load->helper('string');

                // updating config options
                $update_config_data = array();

                $this->configuration->model_data = array('current_version' => CHATBULL_VERSION, 'current_product_name' => PRODUCT_NAME);
                $this->configuration->model_data['admin_panel_email'] = $this->input->post('email');
                $this->configuration->model_data['site_lived_year'] = date("Y-m-d", now());

                if ($main_settings and $main_settings->current_product_name and $main_settings->current_product_name != PRODUCT_NAME) {
                    $this->configuration->model_data['licence_key'] = '';

                    $update_config_data['validated_code'] = 'no';
                }

                $this->configuration->update();

                $update_config_data['encryption_key'] = 'chatbull_' . random_string('alnum');
                $update_config_data['sess_driver'] = 'database';
                $update_config_data['sess_cookie_name'] = random_string('alnum') . '_session';
                $update_config_data['sess_save_path'] = 'session';
                $update_config_data['time_reference'] = 'UTC';
                $update_config_data['language_version'] = date("YmdHis", now());
                $update_config_data['application_token'] = random_string('alnum', 20);

                if (!$this->config->config_update($update_config_data)) {
                    $response['errors'] = $this->lang->line('file_not_writable') . $this->config->config_path;
                } else {
                    $update_autoload_data = array();
                    $update_autoload_data['libraries'] = "'database', 'session', 'form_validation', 'curl', 'email', 'authentication', 'media'";
                    if (!$this->config->autoload_update($update_autoload_data)) {
                        $response['errors'] = $this->lang->line('file_not_writable') . $this->config->autoload_update;
                    } else {
                        if (!$this->config->config_update(array('installed' => "yes"))) {
                            $response['errors'] = $this->lang->line('installed_not_locked');
                        } else {
                            @chmod($this->config->config_path, FILE_READ_MODE);
                            @chmod($this->config->database_path, FILE_READ_MODE);

                            $this->_update_languages();
                            $files = array('desktop_lang', 'chat_lang', 'conf_lang', 'user_lang');
                            $this->sync_lanuages($files);

                            $response['result'] = 'success';
                            $response['page_title'] = $this->lang->line('plugin_installed');
                            $response['message'] = $this->lang->line('installation_completed');
                            $response['completed_message'] = '<p>Your email: (<strong> ' . $this->input->post('email') . ' </strong>) and password: (<strong> ' . $this->input->post('password') . ' </strong>)</p>';
                        }
                    }
                }
            } else {
                $response['errors'] = $this->lang->line('user_not_created');
            }
        } else {
            $response['errors'] = validation_errors();
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Update languages record and files.
     */

    private function _update_languages() {
        $entered_languages = $this->language->get_all();
        if (empty($entered_languages)) {
            // inserting english language in database
            $this->language->model_data = array('lang_name' => 'English', 'machine_name' => 'english', 'lang_status' => 'unpublish');
            if ($this->config->item('language') == 'english') {
                $this->language->model_data['lang_status'] = 'publish';
            }

            $this->language->insert();

            $this->load->helper('directory');
            $languages_path = APPPATH . 'language';
            $languages = directory_map($languages_path, 1);

            foreach ($languages as $language) {
                $machine_name = str_replace('/', '', stripslashes($language));
                if (!in_array($machine_name, array('english', 'index.html'))) {
                    $langdir_path = APPPATH . 'language/' . $machine_name;
                    $english_dir_path = APPPATH . 'language/english';
                    $lang_files = get_filenames($english_dir_path);

                    foreach ($lang_files as $file) {
                        $source_file = $english_dir_path . '/' . $file;
                        $target_file = $langdir_path . '/' . $file;

                        if (file_exists($target_file) === false) {
                            copy($source_file, $target_file);
                        }
                    }

                    $this->language->model_data = array('lang_name' => ucfirst($machine_name), 'machine_name' => $machine_name, 'lang_status' => 'unpublish');
                    if ($this->config->item('language') == $machine_name) {
                        $this->language->model_data['lang_status'] = 'publish';
                    }
                    $this->language->insert();
                }
            }
        } else {
            foreach ($entered_languages as $language) {
                if ($language->machine_name != 'english') {
                    $langdir_path = APPPATH . 'language/' . $language->machine_name;
                    $english_dir_path = APPPATH . 'language/english';
                    $lang_files = get_filenames($english_dir_path);

                    foreach ($lang_files as $file) {
                        $source_file = $english_dir_path . '/' . $file;
                        $target_file = $langdir_path . '/' . $file;

                        if (file_exists($target_file) === false) {
                            copy($source_file, $target_file);
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------

    /**
     * DB Driver Check
     *
     * Test a given driver to ensure the server can use it. We'll also create the
     * database here if we need to.
     *
     * @access	private
     * @param	array
     * @return	bool
     */
    function _check_mysql_connection() {
        if ($this->input->post('db_user')) {
            $config_db = array();
            $config_db['hostname'] = $this->input->post('host');
            $config_db['username'] = $this->input->post('db_user');
            $config_db['password'] = $this->input->post('db_password');
            $config_db['database'] = '';
            $config_db['dbdriver'] = $this->db_driver;
            $config_db['dbprefix'] = $this->db_prefix;
            $config_db['char_set'] = $this->db_char_set;
            $config_db['dbcollat'] = $this->db_dbcollat;

            // load based on custom passed information
            $this->load->database($config_db);
            $database_error = $this->db->error();

            if (is_resource($this->db->conn_id) OR is_object($this->db->conn_id)) {
                $server_ver = $this->db->version();
                if (version_compare($server_ver, '5.5.3', '>')) {
                    // php version is high enough
                    $config_db['char_set'] = $this->db_char_set = 'utf8mb4';
                    $config_db['dbcollat'] = $this->db_dbcollat = 'utf8mb4_general_ci';

                    $this->db->close();
                    $this->load->database($config_db);
                }

                // There is a connection
                $this->load->dbutil();

                // Now then, does the DB exist?
                if ($this->dbutil->database_exists($this->input->post('db_name'))) {
                    // Connected and found the db. Happy days are here again!
                    return TRUE;
                } else {
                    $this->load->dbforge();

                    // creating database 
                    if ($this->dbforge->create_database($this->input->post('db_name'))) {
                        return TRUE;
                    } else {
                        $this->form_validation->set_message('_check_mysql_connection', $this->lang->line('installation_completed'));
                        return FALSE;
                    }
                }
            } else {
                $this->form_validation->set_message('_check_mysql_connection', $this->lang->line('db_not_exists') . $database_error['message']);
                return FALSE;
            }
        } else {
            $this->form_validation->set_message('_check_mysql_connection', $this->lang->line('dbname_required'));
            return FALSE;
        }
    }

    /**
     * Setup Database (only MySQL supported now)
     *
     *
     * @access	private
     * @return	bool
     */
    function _setup_database() {
        $_db['hostname'] = $this->input->post('host');
        $_db['username'] = $this->input->post('db_user');
        $_db['password'] = $this->input->post('db_password');
        $_db['database'] = $this->input->post('db_name');
        $_db['dbdriver'] = $this->db_driver;
        $_db['dbprefix'] = $this->db_prefix;
        $_db['char_set'] = $this->db_char_set;
        $_db['dbcollat'] = $this->db_dbcollat;

        // Update config/database.php file
        return $this->config->db_config_update($_db);
    }

}

/* End of file install.php */
/* Location: ./application/controllers/install.php */