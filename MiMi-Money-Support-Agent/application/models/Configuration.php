<?php

class Configuration extends CP_Model {

    //model db table
    var $table = "";
    public $validation_rules = array(
        // rules for chat forward
        'site-update' => array(
            array(
                'field' => 'chat_status',
                'label' => 'Chat status',
                'rules' => 'required'
            ),
            array(
                'field' => 'chat_mode',
                'label' => 'Chat mode',
                'rules' => 'required'
            ),
            array(
                'field' => 'show_depaertment_selection_box',
                'label' => 'lang:show_depaertment_selection_box',
                'rules' => 'required'
            ),
            array(
                'field' => 'enable_feedback_form',
                'label' => 'Enable feedback form',
                'rules' => 'required'
            ),
            array(
                'field' => 'theme',
                'label' => 'Theme',
                'rules' => 'required'
            ),
            array(
                'field' => 'title_color',
                'label' => 'Title color',
                'rules' => 'required'
            ),
            array(
                'field' => 'background_color',
                'label' => 'Background color',
                'rules' => 'required'
            ),
            array(
                'field' => 'welcome_message',
                'label' => 'Welcome message',
                'rules' => 'required'
            ),
            array(
                'field' => 'waiting_message',
                'label' => 'Waiting message',
                'rules' => 'required'
            ),
            array(
                'field' => 'offline_heading_message',
                'label' => 'Offline Form introduction message',
                'rules' => 'required'
            ),
            array(
                'field' => 'offline_submission_message',
                'label' => 'Offline Form submission message',
                'rules' => 'required'
            ),
            array(
                'field' => 'feedback_heading_message',
                'label' => 'Feedback Form introduction message',
                'rules' => 'required'
            ),
            array(
                'field' => 'feedback_submission_message',
                'label' => 'Feedback Form submission message',
                'rules' => 'required'
            ),
            array(
                'field' => 'offline_form_title',
                'label' => 'Offline form title',
                'rules' => 'required'
            ),
            array(
                'field' => 'online_form_title',
                'label' => 'Online form title',
                'rules' => 'required'
            ),
            array(
                'field' => 'chat_start_title',
                'label' => 'Start chat title',
                'rules' => 'required'
            )
        ),
        'update' => array(
            array(
                'field' => 'admin_panel_name',
                'label' => 'lang:admin_panel_name',
                'rules' => 'required'
            ),
            array(
                'field' => 'enable_agent_file_sharing',
                'label' => 'lang:enable_agent_file_sharing',
                'rules' => 'required'
            ),
            array(
                'field' => 'agent_allowed_filetypes',
                'label' => 'lang:agent_allowed_filetypes',
                'rules' => 'required'
            ),
            array(
                'field' => 'agent_file_upload_size',
                'label' => 'lang:agent_file_upload_size',
                'rules' => 'required'
            )
        )
    );

    /*
     * define construct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_CONFIGURATION;
    }

    /*
     * This function will be use to fetch visitor settings.
     * 
     * @param Array $conditions
     * @return $settings
     */

    function get_settings($conditions = array()) {
        $settings = new stdClass();
        $query = $this->db->get_where($this->table, $conditions);

        foreach ($query->result() as $option) {
            $settings->{$option->config_option} = $option->config_value;
        }

        return $settings;
    }

    /*
     * Gget sites departments 
     * 
     * @param Array $sites_ids
     * @return Array $departments
     */

    function get_site_departments($sites_ids) {
        $departments = array();

        $query = $this->db->select('*');
        $query->where_in('site_id', $sites_ids);
        $query->where('config_option', 'departments');

        $sites_departments = $query->from($this->table)
                ->get()
                ->result();

        $deparsments_ids = array();
        foreach ($sites_departments as $departments) {
            $departments_array = explode(",", $departments->config_value);
            $deparsments_ids = array_merge($deparsments_ids, $departments_array);
        }

        if (count($deparsments_ids) > 0) {
            $departments = $this->db->select('*')
                    ->where_in('id', $deparsments_ids)
                    ->from(TABLE_TAGS)
                    ->get()
                    ->result();
        }

        return $departments;
    }

    /*
     * To update config data
     * 
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update() {
        $config_data = array();

        foreach ($this->model_data as $opt_name => $opt_val) {
            $config_data[] = array('config_option' => $opt_name, 'config_value' => $opt_val);
        }

        if (count($config_data) > 0) {
            $this->db->update_batch($this->table, $config_data, 'config_option');
            return true;
        }

        return false;
    }

    /*
     * To update site config data
     * 
     * @paran Int $site_id
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update_site_settings($site_id) {
        foreach ($this->model_data as $opt_name => $opt_val) {
            $config_data = array('config_value' => $opt_val);
            $where = array('config_option' => $opt_name, 'site_id' => $site_id);
            $this->db->update($this->table, $config_data, $where);
        }

        return true;
    }

    /*
     * Return settings for visitors panel as required
     * 
     * @param Int $site_id
     * @param Array $conditions
     * @return $settings
     */

    function get_visitor_settings($site_id, $conditions = array()) {
        $settings = new stdClass();
        $config_options = array(
            'theme',
            'visitor_widget_type',
            'chat_icon_size',
            'enable_online_animation',
            'enable_agent_initiate_chats',
            'enable_specific_agent_request',
            'enable_file_sharing',
            'file_upload_size',
            'allowed_filetypes',
            'chat_mode',
            'time_interwal',
            'chat_start_title',
            'chat_waiting_title',
            'offline_form_title',
            'online_form_title',
            'default_avatar',
            'text_color',
            'title_color',
            'background_color',
            'enable_feedback_form',
            'offline_submission_message',
            'initiate_bypass_chat',
            'feedback_submission_message',
            'send_chat_transcript_to_visitor',
            'send_chat_transcript_to_admin',
            'send_chat_transcript_to_operators',
            'operator_gone_offline_message',
            'open_chatbox_automatically',
            'time_automatically_open_chatbox',
            'chat_status',
            'window_position',
            'offline_heading_message',
            'show_depaertment_selection_box',
            'show_contact_number_input',
            'welcome_message',
            'offline_support_type',
            'offline_message',
            'feedback_heading_message',
            'waiting_message',
            'current_product_name',
            'current_version',
            'hide_poweredby',
            'poweredby_content',
            'notify_all_requests_admin'
        );

        $this->db->where_in('config_option', $config_options);
        $this->db->where_in('site_id', array(0, $site_id));
        $query = $this->db->get_where($this->table, $conditions);

        foreach ($query->result() as $option) {
            $settings->{$option->config_option} = $option->config_value;
        }

        return $settings;
    }

    /*
     * Return settings for visitors panel as required
     * 
     * @param Array $conditions
     * @return $settings
     */

    function get_agent_settings() {
        $config_options = array(
            'enable_canned_messages',
            'enable_agent_initiate_chats',
            'enable_agent_file_sharing',
            'agent_allowed_filetypes',
            'agent_time_interwal',
            'agent_file_upload_size',
            'admin_panel_logo',
            'admin_panel_name',
            'admin_panel_email',
            'operator_chat_theme'
        );

        $settings = new stdClass();
        $this->db->where_in('config_option', $config_options);
        $this->db->where('site_id', 0);
        $query = $this->db->get_where($this->table);

        foreach ($query->result() as $option) {
            $settings->{$option->config_option} = $option->config_value;
        }
        return $settings;
    }

    /*
     * Default all options for site configuration
     * 
     * @return array $options
     */

    function get_site_default() {
        $options = array(
            // general
            array('config_option' => 'chat_status', 'config_value' => 'enable'),
            array('config_option' => 'time_interwal', 'config_value' => '3'),
            array('config_option' => 'chat_mode', 'config_value' => 'online'),
            array('config_option' => 'initiate_bypass_chat', 'config_value' => 'no'),
            array('config_option' => 'send_chat_transcript_to_visitor', 'config_value' => 'ask_to_visiter'),
            array('config_option' => 'send_chat_transcript_to_admin', 'config_value' => 'yes'),
            array('config_option' => 'send_chat_transcript_to_operators', 'config_value' => 'yes'),
            array('config_option' => 'open_chatbox_automatically', 'config_value' => 'no'),
            array('config_option' => 'time_automatically_open_chatbox', 'config_value' => '5'),
            array('config_option' => 'enable_feedback_form', 'config_value' => 'yes'),
            array('config_option' => 'show_depaertment_selection_box', 'config_value' => 'no'),
            array('config_option' => 'show_contact_number_input', 'config_value' => 'no'),
            array('config_option' => 'enable_specific_agent_request', 'config_value' => 'no'),
            array('config_option' => 'enable_file_sharing', 'config_value' => 'yes'),
            array('config_option' => 'file_upload_size', 'config_value' => '5000'),
            array('config_option' => 'allowed_filetypes', 'config_value' => '.docx|.txt|.gif|.jpg|.png'),
            array('config_option' => 'hide_poweredby', 'config_value' => 'no'),
            array('config_option' => 'poweredby_content', 'config_value' => 'Powered By <a href="' . POWERED_BY_URL . '" target="_new">' . POWERED_BY . '</a>'),
            //theme settings
            array('config_option' => 'theme', 'config_value' => 'bubbles_boom'),
            array('config_option' => 'visitor_widget_type', 'config_value' => 'chatbar'),
            array('config_option' => 'chat_icon_size', 'config_value' => 'large-size'),
            array('config_option' => 'enable_online_animation', 'config_value' => 'yes'),
            array('config_option' => 'background_color', 'config_value' => '#3367d6'),
            array('config_option' => 'title_color', 'config_value' => '#ffffff'),
            array('config_option' => 'window_position', 'config_value' => 'right'),
            //content & info
            array('config_option' => 'default_avatar', 'config_value' => ''),
            array('config_option' => 'chat_start_title', 'config_value' => 'Start Chat'),
            array('config_option' => 'chat_waiting_title', 'config_value' => 'Live Chat'),
            array('config_option' => 'online_form_title', 'config_value' => 'Live Chat'),
            array('config_option' => 'welcome_message', 'config_value' => 'Hello Guest! How can I help you today?'),
            array('config_option' => 'waiting_message', 'config_value' => 'Our agents are already engaged with trying to help out some customers. Please be patient. Someone will be here in a jiffy!'),
            array('config_option' => 'operator_gone_offline_message', 'config_value' => 'Seems like agent is offline...'),
            array('config_option' => 'offline_support_type', 'config_value' => 'form'),
            array('config_option' => 'offline_message', 'config_value' => 'Sorry, no agent currently online. Please leave a message & we will contact you soon.'),
            array('config_option' => 'offline_form_title', 'config_value' => 'We are offline'),
            array('config_option' => 'offline_heading_message', 'config_value' => 'Sorry, no agent currently online. Please leave a message & we will contact you soon.'),
            array('config_option' => 'offline_submission_message', 'config_value' => 'Thank you for contacting us! we will get back to you within 24 hours.'),
            array('config_option' => 'feedback_heading_message', 'config_value' => 'Your opinion matters a lot. Kindly leave your feedback.'),
            array('config_option' => 'feedback_submission_message', 'config_value' => 'Your Feedback has been sent successfully.'),
            //departments
            array('config_option' => 'departments', 'config_value' => ''),
        );

        return $options;
    }

    /*
     * Default all options for global configuration
     * 
     * @return array $options
     */

    function get_default() {
        $options = array(
            array('config_option' => 'licence_key', 'config_value' => ''),
            array('config_option' => 'google_map_api_key', 'config_value' => ''),
            array('config_option' => 'google_map_zoom', 'config_value' => '2'),
            array('config_option' => 'notify_all_requests_admin', 'config_value' => 'no'),
            array('config_option' => 'enable_agent_initiate_chats', 'config_value' => 'yes'),
            array('config_option' => 'enable_canned_messages', 'config_value' => 'yes'),
            array('config_option' => 'enable_agent_file_sharing', 'config_value' => 'yes'),
            array('config_option' => 'agent_time_interwal', 'config_value' => '3'),
            array('config_option' => 'agent_file_upload_size', 'config_value' => '5000'),
            array('config_option' => 'agent_allowed_filetypes', 'config_value' => '.docx|.txt|.gif|.jpg|.png'),
            array('config_option' => 'admin_panel_logo', 'config_value' => ''),
            array('config_option' => 'admin_panel_name', 'config_value' => ucfirst(str_replace('chatbull', 'ChatBull', PRODUCT_NAME))),
            array('config_option' => 'admin_panel_email', 'config_value' => ''),
            array('config_option' => 'operator_chat_theme', 'config_value' => 'bubbles-boom'),
            array('config_option' => 'current_version', 'config_value' => CHATBULL_VERSION),
            array('config_option' => 'current_product_name', 'config_value' => PRODUCT_NAME),
            array('config_option' => 'site_lived_year', 'config_value' => date("Y-m-d", now())),
            array('config_option' => 'last_login_updated', 'config_value' => date("Y-m-d H:i:s", now()))
        );

        return $options;
    }

    /*
     * Insert new record in database
     * 
     * @param Array $data
     * @param Boolean $batch Default False
     * 
     * @return boolean or insert id
     */

    function insert_options($data = array(), $batch = FALSE) {
        if (is_array($data) and count($data) > 0) {
            if ($batch) {
                $this->db->insert_batch($this->table, $data);
                return TRUE;
            } else {
                $this->db->insert($this->table, $data);
                return $this->db->insert_id();
            }
        }

        return FALSE;
    }

    /*
     * Upgrade site settings ( Will not be usable after 5.0.0 )
     * 
     * @param Array $sites
     * @param Object $settings
     */

    function upgrade_site_settings($sites, $settings) {
        $this->db->truncate($this->table);

        $new_options = array(
            array('config_option' => 'licence_key', 'config_value' => $settings->licence_key),
            array('config_option' => 'current_version', 'config_value' => $settings->current_version),
            array('config_option' => 'current_product_name', 'config_value' => $settings->current_product_name),
            array('config_option' => 'site_lived_year', 'config_value' => $settings->site_lived_year)
        );
        $this->insert_options($new_options, TRUE);

        $options = $this->get_site_default();
        $new_options = array();

        foreach ($sites as $site) {
            $data = array('modified_at' => date("Y-m-d H:i:s", now()));
            $this->db->update(TABLE_SITES, $data, array("id" => $site->id));

            foreach ($options as $opt) {
                if (isset($settings->{$opt['config_option']})) {
                    $opt['config_value'] = $settings->{$opt['config_option']};
                }

                if ($opt['config_option'] == 'theme') {
                    $opt['config_value'] = 'bubbles_boom';
                }

                $opt['site_id'] = $site->id;
                $new_options[] = $opt;
            }
        }

        if (count($new_options) > 0) {
            $this->configuration->insert_options($new_options, TRUE);
        }
    }

    /*
     * merge_settings_options
     *
     * Merge Site settings options if are missing in datbase
     *
     * @type	function
     * @date	15-01-2018
     * @since	5.1.1
     *
     * @param	$sites Array
     * @return	N/A
     */

    function merge_settings_options($sites) {
        $options = $this->get_site_default();
        $new_options = array();

        foreach ($sites as $site) {
            $settings = $this->get_settings(array('site_id' => $site->id));
            foreach ($options as $opt) {
                if (!isset($settings->{$opt['config_option']})) {
                    $opt['site_id'] = $site->id;
                    $new_options[] = $opt;
                }
            }
        }

        if (count($new_options) > 0) {
            $this->configuration->insert_options($new_options, TRUE);
        }
    }

}
