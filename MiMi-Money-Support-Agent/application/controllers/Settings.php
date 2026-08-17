<?php

class Settings extends CP_AdminController {

    public function __construct() {
        parent::__construct();

        // check has admin access
        $this->has_admin_access();
    }

    /*
     * To load view for tags listing page in angular
     */

    public function index() {
        // add users.js file
        $this->addjs(theme_url("js/app/directives/modal-confirm.js"), TRUE);
        $this->addjs(theme_url("js/app/settings/settings.js"), TRUE);

        $this->add_js_inline(array('settings' => $this->settings, 'user' => $this->current_user));

        //write code here to load view
        $this->bulid_layout("settings/settings");
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

    public function update_settings() {

        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            //check if data is valid or not
            $this->form_validation->set_rules($this->configuration->validation_rules['update']);

            if ($this->form_validation->run() === true) {
                $this->configuration->model_data = $this->input->post();
                if ($this->input->post('admin_logo')) {
                    $base64_array = $this->input->post('admin_logo');
                    $upload_path = upload_dir(LOGOS);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        if ($this->input->post('admin_panel_logo') and $this->input->post('admin_panel_logo') != theme_url("images/logo.png")) {
                            $this->media->delete_media(basename($this->input->post('admin_panel_logo')), LOGOS);
                        }

                        $response['admin_panel_logo'] = $this->configuration->model_data['admin_panel_logo'] = upload_url(LOGOS . $filedata['filename']);
                        unset($this->configuration->model_data['admin_logo']);
                    }
                }

                if ($this->configuration->update()) {
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

    /*
     * Verify license key (purchase code)
     */

    function verify_license_key() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $license_key = $this->input->post('license_key');
            if ($license_key) {
                $result = validate_license_key($license_key);

                if ($result->result == 'success') {
                    // load the file helper
                    $this->load->helper('string');
                    if (!$this->config->config_update(array('validated_code' => "yes"))) {
                        $response['errors'] = $this->lang->line('licence_key_not_validating');
                    } else {
                        $response['result'] = 'success';
                        $response['message'] = $result->message;

                        $this->configuration->model_data = array('config_value' => $license_key);
                        $this->configuration->update_where(array('config_option' => 'licence_key'));
                    }
                } else {
                    if (is_object($result->errors)) {
                        foreach ($result->errors as $errorType => $error) {
                            $error_type = '';
                            if (is_string($errorType)) {
                                $error_type = '<strong>' . str_replace("_", " ", $errorType) . '</strong> - ';
                            }
                            $response['errors'] .= $error_type . $error;
                        }
                    } else {
                        $response['errors'] = $result->errors;
                    }
                }
            } else {
                $response['errors'] = sprintf($this->lang->line('error_required'), $this->lang->line('licence_key'));
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To unregister domain and license key
     */

    function unregister() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $license_key = $this->input->post('license_key');
            if ($license_key) {
                $result = unregister_license_key($license_key);
                if ($result->result == 'success') {
                    // load the file helper
                    $this->load->helper('string');
                    if (!$this->config->config_update(array('validated_code' => "no"))) {
                        $response['errors'] = $this->lang->line('licence_key_not_unregistering');
                    } else {
                        $response['result'] = 'success';
                        $response['message'] = $result->message;

                        $this->configuration->model_data = array('config_value' => '');
                        $this->configuration->update_where(array('config_option' => 'licence_key'));
                    }
                } else {
                    $response['errors'] = $result->errors;
                }
            } else {
                $response['errors'] = sprintf($this->lang->line('error_required'), $this->lang->line('licence_key'));
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
