<?php

/*
 * Main Base Controller
 */

class CP_Controller extends CI_Controller {

    var $settings;
    var $body_classes = array();
    var $data = array('theme' => 'cmodule');

    public function __construct() {
        parent::__construct();
    }

    /*
     * Returns the data in json format
     * @param : data if anything else than controller's data attribute.
     * returns: either data passed in argument or controller's data attribute in JSON format.
     * Author: Pukhraj Prajapat, pukhraj.prajapat@g-axon.com
     */

    public function return_json($data = "") {
        if (!empty($data)) {
            return json_encode($data);
        }
    }

    /*
     * This function will cancel all requests.
     * 
     * @param $chat_session_id
     * 
     * @return true or false
     */

    function close_requests($chat_session_id) {
        return $this->chat_request->close_requests($chat_session_id);
    }

    /*
     * This function will return cureent server time in miliseconds.
     * 
     * @return $milliseconds
     */

    function get_time_miliseconds() {
        $micro_time = microtime(true);
        $milliseconds = round($micro_time * 1000);

        return $milliseconds;
    }

}

/*
 * Main Admin Base Controller
 */

class CP_AdminController extends CP_Controller {

    var $files = array('css_header' => '', 'css_footer' => '', 'js_header' => '', 'js_footer' => '');
    var $item_per_page = 10;
    var $current_user = '';

    public function __construct() {
        parent::__construct();

        $this->form_validation->set_error_delimiters('', '');
        if ($this->config->item('installed') != 'no') {
            if ($this->config->item('application_token')) {
                if ($this->session->userdata('current_user_id')) {
                    $current_user_id = $this->session->userdata('current_user_id');
                    $this->current_user = $this->user->get_where(array('id' => $current_user_id));
                    $this->current_user->profile_picture = $this->media->get_thumbnail($this->current_user->profile_pic, PROFILE_PICS, $this->current_user->email);

                    if ($this->router->class == 'admin' and ( $this->router->method == 'login')) {
                        redirect("c=admin");
                    }
                } else {
                    $remember_token = $this->input->cookie('remember_token');
                    $this->current_user = $this->user->get_where(array('remember_token' => $remember_token));
                    if ($this->current_user) {
                        // closing closable sessions
                        $this->user->closeClosedSessions();
                        $this->authentication->store_session($this->current_user);

                        if ($this->current_user->role == 'agent') {
                            redirect("d=agents&c=agents");
                        } elseif ($this->router->class == 'admin' and $this->router->method == 'login') {
                            redirect("c=admin");
                        }
                    }
                }

                $this->settings = $this->configuration->get_settings(array('site_id' => 0));
                if (empty($this->settings->admin_panel_logo)) {
                    $this->settings->admin_panel_logo = theme_url("images/logo.png");
                }
            } else {
                $this->settings = $this->configuration->get_settings();
            }

            $this->settings->plugin_validated = 'no';
            if ($this->settings->licence_key) {
                $this->settings->plugin_validated = $this->config->item('validated_code');
            }

            if ($this->settings->current_version != CHATBULL_VERSION or ! isset($this->settings->current_product_name) or $this->settings->current_product_name != PRODUCT_NAME) {
                if ($this->router->class != 'upgrade') {
                    redirect("c=upgrade&m=upgrade");
                }
            }
        
            if (!$this->session->userdata('current_user_id') and $this->router->class != 'upgrade') {
                unset($this->settings->licence_key);
            }
        } elseif ($this->config->item('installed') != 'yes' and $this->router->class != 'install') {
            redirect("c=install");
        }

        $this->_setDefaultData();
    }

    /*
     * This function will be use to add js in view.
     * 
     * @pparam $filepath (js file path)
     * @param $in_footer (If true file will be include before body end
     */

    function addjs($filepath, $in_footer = false) {
        if ($in_footer) {
            $this->files['js_footer'] .= '<script src="' . $filepath . '"></script>' . "\n";
        } else {
            $this->files['js_header'] .= '<script type="text/javascript" src="' . $filepath . '"></script>' . "\n";
        }
    }

    /*
     * This function will be use to add js for ie.
     * 
     * @pparam $filepath (js file path)
     * @param $ie_version
     */

    function addjsIe($filepath, $ie_version = '') {
        if ($ie_version) {
            $this->files['js_header'] .= '<!--[if lte IE ' . $ie_version . ']><script type="text/javascript" src="' . $filepath . '"></script><![endif]-->' . "\n";
        }
    }

    /*
     * This function will be use to add inline js in view.
     * 
     * @pparam $filepath (js file path)
     * @param $in_footer (If true file will be include before body end
     */

    function add_js_inline($data, $in_footer = false) {
        if ($in_footer) {
            $this->files['js_footer'] .= '<script> var cmodule = ' . $this->return_json($data) . '</script>' . "\n";
        } else {
            $this->files['js_header'] .= '<script> var cmodule = ' . $this->return_json($data) . '</script>' . "\n";
        }
    }

    /*
     * This function will be use to add css in view.
     * 
     * @pparam $filepath (css file path)
     * @param $in_footer (If true file will be include before body end
     */

    function addcss($filepath, $in_footer = false) {
        if ($in_footer) {
            $this->files['css_footer'] .= '<link rel="stylesheet" type="text/css" href="' . $filepath . '" media="all" />' . "\n";
        } else {
            $this->files['css_header'] .= '<link rel="stylesheet" type="text/css" href="' . $filepath . '" media="all" />' . "\n";
        }
    }

    /*
     * Set lanuage label for client side scripts
     * 
     * @return Array $lables
     */

    private function _set_lang_labels() {
        $this->data['lang_lables'] = array();

        $this->data['lang_lables']['yes'] = $this->lang->line('yes');
        $this->data['lang_lables']['no'] = $this->lang->line('no');
    }

    /*
     * Function will be call to set Default data
     */

    protected function _setDefaultData() {
        $this->data['title'] = $this->lang->line('admin_panel');
        $this->data['pagetitle'] = '';
        $this->data['description'] = '';
        $this->data['tags'] = '';
        $this->data['theme'] = 'cmodule';
        $this->data['layout'] = 'main_layout';
        $this->data['filter_view'] = '';

        $this->roles = array(
            array('name' => 'admin', 'label' => $this->lang->line('admins')),
            array('name' => 'agent', 'label' => $this->lang->line('agents')),
            array('name' => 'visitor', 'label' => $this->lang->line('visitors'))
        );

        $this->_set_lang_labels();
    }

    /*
     * This function will be call to build view.
     * 
     * @param string $view file path
     * @param string $layout (optional)
     * @param string $theme (optional)
     */

    function bulid_layout($view, $layout = '', $theme = '') {
        if ($layout)
            $this->data['layout'] = $layout;
        if ($theme)
            $this->data['theme'] = $theme;
        $this->data['view'] = $view;

        // checking user session
        if (empty($this->current_user) and $this->router->directory != 'visitors/' and $this->router->method != 'forget_password' and $this->router->method != 'reset_password' and $this->router->class != 'cmodule') {
            // set login view and layout
            $this->data['layout'] = 'simple_layout';
            $this->data['view'] = 'pages/login';
            $this->data['pagetitle'] = 'Login';
        }

        if ($this->data['pagetitle'])
            $this->data['pagetitle'] .= ' - ';

        // load main vies files.
        $this->load->view($this->data['theme'] . '/' . $this->data['layout']);
    }

    /*
     * This function check user can access admin panel or not.
     * If can access then ok otherwise redirect them to his accessible area.
     */

    function has_admin_access() {
        if ($this->session->userdata('current_user_id')) {
            if ($this->current_user->role == 'agent') {
                redirect('d=agents&c=agents');
            }
        }
    }

    /*
     * This will add uniqe validation ruke in validation
     */

    function setEditUniqueRule($rules, $unique_field, $id) {
        $update_rules = array();
        foreach ($rules as $rule) {
            if ($rule['field'] == $unique_field) {
                $rule['rules'] = $rule['rules'] . '|callback_email_check[' . $id . ']';
            }

            $update_rules[] = $rule;
        }

        return $update_rules;
    }

    /*
     * This function will check email of user
     * This is validation callback function.
     */

    public function email_check($email, $user_id = '') {
        $unique_email_message = $this->lang->line('mustbe_unique');
        if ($user_id) {
            $user = $this->user->get_single(array('email' => $email, 'id !=' => $user_id));
            if ($user and $user->id) {
                if ($user->user_status == 'deleted') {
                    $this->user->model_data = array('user_status' => 'active');
                    $this->user->update($user->id);
                }

                $this->form_validation->set_message('email_check', $unique_email_message);
                return FALSE;
            } else {
                return TRUE;
            }
        } else {
            $user = $this->user->get_single(array('email' => $email, 'user_status !=' => 'deleted'));
            if ($user and $user->id and in_array($user->role, array('admin', 'agent'))) {
                $this->form_validation->set_message('email_check', $unique_email_message);
                return FALSE;
            } else {
                return TRUE;
            }
        }
    }

    /*
     * This function will check machine_name of language
     * This is validation callback function.
     */

    public function langname_check($langname, $id = '') {
        $uniqu_message = $this->lang->line('mustbe_unique');

        if ($id) {
            $record = $this->language->get_single(array('machine_name' => $langname, 'id !=' => $id));
            if ($record and $record->id) {
                $this->form_validation->set_message('langname_check', $uniqu_message);
                return FALSE;
            } else {
                return TRUE;
            }
        } else {
            $record = $this->language->get_single(array('machine_name' => $langname));
            if ($record and $record->id) {
                $this->form_validation->set_message('langname_check', $uniqu_message);
                return FALSE;
            } else {
                return TRUE;
            }
        }
    }

    /*
     * sync_lanuages
     * 
     * Function will syns lanuages files
     * 
     * @type    function
     * @date    13-12-2017
     * @since   5.1.0
     * 
     * @param   Array $files Name of languages files
     * @param   Array $languages Optional
     * @return  n/a
     */

    function sync_lanuages($files = array(), $languages = array()) {
        if (!is_array($files) or empty($files))
            return;

        // fetch languages if not exists
        if (empty($languages)) {
            $languages = $this->language->get_all();
        }

        foreach ($languages as $lang) {
            if ($lang->machine_name != 'english') {
                foreach ($files as $file) {
                    $this->sync_langfile($file, $lang);
                }
            }
        }
    }

    /*
     * sync_langfile
     * 
     * Function will syns lanuage file
     * 
     * @type    function
     * @date    13-12-2017
     * @since   5.1.0
     * 
     * @param   String $language_file Name of language file
     * @param   Object $language     * 
     * @return  Boolean
     */

    function sync_langfile($language_file, $language) {
        // remove php extention
        $language_file = str_replace(".php", "", $language_file);

        $english_data = $this->lang->load($language_file, 'english', TRUE, FALSE);
        $filedata = $this->lang->load($language_file, $language->machine_name, TRUE, FALSE);
        $filepath = APPPATH . 'language/' . $language->machine_name . '/' . $language_file . '.php';

        @chmod($filepath, FILE_WRITE_MODE);

        // Read the config file as PHP
        require $filepath;

        // Read the config data as a string
        $input_file = read_file($filepath);

        // Trim it
        $input_file = trim($input_file);

        foreach ($english_data as $key => $val) {
            if (!isset($filedata[$key])) {
                if (strtolower($val) == 'true' or strtolower($val) == 'false') {
                    $input_file .= "\n\$lang['$key'] = $val;";
                } else {
                    $input_file .= "\n\$lang['$key'] = '" . addslashes($val) . "';";
                }
            }
        }

        if (!$fp = fopen($filepath, FOPEN_WRITE_CREATE_DESTRUCTIVE)) {
            return FALSE;
        }

        flock($fp, LOCK_EX);
        fwrite($fp, $input_file, strlen($input_file));
        flock($fp, LOCK_UN);
        fclose($fp);

        @chmod($filepath, FILE_READ_MODE);

        return TRUE;
    }

}

/*
 * Main Agent Base Controller
 */

class CP_AgentController extends CP_AdminController {
    /*
     * construct function
     */

    public function __construct() {
        parent::__construct();
    }

}

/*
 * Main Visitor Base Controller
 */

class CP_VisitorController extends CP_Controller {

    /**
     * Access token for authorization
     *
     * @var string
     */
    var $access_token = '';

    /**
     * Token Object if valid token
     *
     * @var object or FALSE
     */
    protected $valid_token;

    /*
     * Current site url
     * 
     * @access Public
     */
    var $current_site;

    /*
     * construct function
     */

    public function __construct() {
        parent::__construct();
        $this->form_validation->set_error_delimiters('', '');

        $this->access_token = ($this->input->get('token')) ? $this->input->get('token') : $this->input->get_request_header('Accesstoken', TRUE);
        $this->current_site = $this->site_model->get_single(array('token' => $this->access_token));

        if ($this->current_site and $this->config->item('validated_code') == 'yes' and $this->config->item('installed') == 'yes') {
            $this->settings = $this->configuration->get_visitor_settings($this->current_site->id);
            $this->settings->plugin_validated = $this->config->item('validated_code');

            $this->settings->label_give_feedback = $this->lang->line('give_feedback');
            $this->settings->label_session_closed = $this->lang->line('session_closed');
            $this->settings->label_feedback = $this->lang->load('feedback', '', TRUE);

            // gettting access token
            $this->input->request_headers();
            $this->valid_token = $this->current_site;
        } else {
            $this->settings = new stdClass();
            $this->settings->result = 'failed';
            $this->settings->error = $this->lang->line('invalid_token');
            $this->settings->plugin_validated = $this->config->item('validated_code');
        }
    }

}

/*
 * Main App Base Controller
 */

class CP_AppController extends CP_Controller {

    /**
     * Access token for authorization
     *
     * @var string
     */
    protected $access_token = '';

    /**
     * Token Object if valid token
     *
     * @var object or FALSE
     */
    protected $valid_token;

    /*
     * construct function
     */

    public function __construct() {
        parent::__construct();
        $this->form_validation->set_error_delimiters('', '');

        $this->settings = $this->configuration->get_agent_settings();
        $this->settings->plugin_validated = $this->config->item('validated_code');

        if ($this->config->item('validated_code') == 'yes' and $this->config->item('installed') == 'yes') {
            // gettting access token
            $this->input->request_headers();
            $this->access_token = ($this->input->get('token')) ? $this->input->get('token') : $this->input->get_request_header('Accesstoken', TRUE);
            $this->valid_token = ($this->access_token == $this->config->item('application_token')) ? TRUE : FALSE;
        }
    }

}
