<?php

/*
 * Controller to manage all users related actions
 * Author: Pukhraj Prajapat (pukhraj.prajapat@g-axon.com)
 */

class Sites extends CP_AdminController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();

        // check has admin access
        $this->has_admin_access();
    }

    /*
     * To load view
     */

    public function index() {
        // add users.js file
        $this->addjs(theme_url("js/app/directives/modal-confirm.js"), TRUE);
        $this->addjs(theme_url("js/app/settings/sites.js"), TRUE);

        $this->add_js_inline(array('settings' => $this->settings, 'user' => $this->current_user));

        //write code here to load view
        $this->bulid_layout("sites/list");
    }

    /*
     * Get all sites from database
     * 
     * @return $response
     */

    function get_sites() {
        $response = array('result' => 'success');
        if ($this->current_user) {
            $response['sites'] = $this->site_model->get_sites();
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }
        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Store new record in databse
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function store() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            if ($this->input->post('filename')) {
                $this->site_model->validation_rules[] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->site_model->validation_rules);
            if ($this->form_validation->run() === true) {
                $this->site_model->model_data = $this->input->post(array('site_name', 'site_url', 'site_email'));
                $this->site_model->model_data['token'] = random_string('alnum', 20);

                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(LOGOS);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $this->site_model->model_data['site_logo'] = upload_url(LOGOS . $filedata['filename']);
                    }
                } else {
                    $this->site_model->model_data['site_logo'] = '';
                }

                $created = $this->site_model->create();
                if ($created) {
                    $new_options = array();
                    $site_options = $this->configuration->get_site_default();
                    foreach ($site_options as $opt) {
                        $opt['site_id'] = $created;
                        $new_options[] = $opt;
                    }

                    if (count($new_options) > 0) {
                        $this->configuration->insert_options($new_options, TRUE);
                    }

                    $this->site_model->model_data['id'] = $created;
                    $response['result'] = 'success';
                    $response['new_record'] = $this->site_model->model_data;
                    $response['message'] = $this->lang->line('site_added');
                } else {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors('<p>', '</p>');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Update record in databse
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function update() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $this->site_model->validation_rules['site_url']['rules'] = $this->site_model->validation_rules['site_url']['rules'] = 'trim|required|callback__domain_check[' . $id . ']';

            if ($this->input->post('filename')) {
                $this->site_model->validation_rules[] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->site_model->validation_rules);
            if ($this->form_validation->run() === true) {
                $this->site_model->model_data = $this->input->post(array('site_name', 'site_url', 'site_email'));

                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(LOGOS);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        if ($this->input->post('site_logo') and $this->input->post('site_logo') != theme_url("images/logo.png")) {
                            $this->media->delete_media(basename($this->input->post('site_logo')), LOGOS);
                        }

                        $response['site_logo'] = $this->site_model->model_data['site_logo'] = upload_url(LOGOS . $filedata['filename']);
                    }
                }

                $updated = $this->site_model->update($id);
                if ($updated) {
                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('site_updated');
                } else {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors('<p>', '</p>');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Delete record from database
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function destroy() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $site = $this->site_model->get_single(array('id' => $id));
            if ($site) {
                $chat_sessions = $this->site_model->get_chat_sessions($site->id);
                foreach ($chat_sessions as $session) {
                    $this->chat_session->delete_chat($session->id);
                }

                if ($site->site_logo and $site->site_logo != theme_url("images/logo.png")) {
                    $this->media->delete_media(basename($site->site_logo), LOGOS);
                }

                $this->users_sites->delete_where(array('site_id' => $id));

                if ($this->site_model->delete_where(array('id' => $id))) {
                    $this->configuration->delete_where(array('site_id' => $id));

                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('site_deleted');
                } else {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = $this->lang->line('invalid_id');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Check domain name is already exists or not.
     * 
     * @param {String} $url
     * @param {Int} $id
     * 
     * @return Boolean
     */

    function _domain_check($url, $id = 0) {
        $domain_exists = $this->lang->line('domain_exists');
        $domain_name = get_domain($url);

        if ($id) {
            $this->db->where('id !=', $id);
            $this->db->like('site_url', $domain_name);
            $query = $this->db->get($this->site_model->table);
            $entry = $query->row();
            if ($entry) {
                $this->form_validation->set_message('_domain_check', $domain_exists);
                return FALSE;
            } else {
                return TRUE;
            }
        } else {
            $this->db->like('site_url', $domain_name);
            $query = $this->db->get($this->site_model->table);
            $entry = $query->row();
            if ($entry) {
                $this->form_validation->set_message('_domain_check', $domain_exists);
                return FALSE;
            } else {
                return TRUE;
            }
        }
    }

    /*
     * Load site setting view
     */

    function site_setting() {
        $site_id = $this->input->get('id');

        // add users.js file
        $this->addcss(base_url("assets/cmodule-chat/css/cmodule-chat.css"), TRUE);
        $this->addjs(base_url("assets/angular-ui-tinymce/tinymce/tinymce.js"), TRUE);
        $this->addjs(base_url("assets/angular-ui-tinymce/src/tinymce.js"), TRUE);
        $this->addjs(theme_url("js/app/settings/site-settings.js"), TRUE);
        $tags = $this->tag->get_all();
        $site = $this->site_model->get_single(array('id' => $site_id));
        $site_settings = $this->configuration->get_settings(array('site_id' => $site_id));

        $script_data = array(
            'site' => $site,
            'tags' => $tags,
            'site_settings' => $site_settings,
            'settings' => $this->settings,
            'user' => $this->current_user
        );

        $this->add_js_inline($script_data);

        //write code here to load view
        $this->bulid_layout("sites/settings");
    }

    /*
     * To update a tag
     * 
     * Input will be given in $_POST (tag_name)
     * 
     * @return 
     *      if(successful validtaion) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else    {result: "failed", errors: [{error messages}]}
     */

    function update_site_settings() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $site_id = $this->input->get('site_id');

            //check if data is valid or not
            $this->form_validation->set_rules($this->configuration->validation_rules['site-update']);



            if ($this->form_validation->run() === true) {
                $this->configuration->model_data = $this->input->post();
                if ($this->input->post('departments')) {
                    $departments = array();
                    $tags = $this->tag->get_all();
                    foreach ($tags as $tag) {
                        if (($key = array_search($tag->id, $this->configuration->model_data['departments'])) !== false) {
                            $departments[] = $this->configuration->model_data['departments'][$key];
                        }
                    }
                    $this->configuration->model_data['departments'] = implode(",", $departments);
                } else {
                    $this->configuration->model_data['departments'] = '';
                }

                if ($this->input->post('entry_avatar')) {
                    $base64_array = $this->input->post('entry_avatar');
                    $upload_path = upload_dir(AVATAR);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $thumbnail_config = array('create_thumb' => true, 'width' => '120', 'height' => '120');
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array, $thumbnail_config);

                    if ($filedata['uploaded']) {
                        if ($this->input->post('default_avatar') and $this->input->post('default_avatar') != theme_url("images/logo.png")) {
                            $this->media->delete_media(basename($this->input->post('default_avatar')), AVATAR);
                        }

                        $response['default_avatar'] = $this->configuration->model_data['default_avatar'] = upload_url(AVATAR . 'thumb/' . $filedata['filename']);
                        unset($this->configuration->model_data['entry_avatar']);
                    }
                }

                if ($this->configuration->update_site_settings($site_id)) {
                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('setting_updated');
                } else {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors('<p>', '</p>');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
