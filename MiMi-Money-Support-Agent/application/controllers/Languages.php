<?php

/*
 * Controller to manage all users related actions
 * Author: Pukhraj Prajapat (pukhraj.prajapat@g-axon.com)
 */

class Languages extends CP_AdminController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();

        // check has admin access
        $this->has_admin_access();
    }

    /*
     * To load view for feedbacks management
     */

    function index() {
        // add users.js file
        $this->addjs(theme_url("js/app/directives/modal-confirm.js"), TRUE);
        $this->addjs(theme_url("js/app/languages.js"), TRUE);

        // get all language 
        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings));

        //write code here to load view
        $this->bulid_layout("languages/list");
    }

    /*
     * To list all records exists in DB
     * 
     * @return Json
     *      {records: [{id, name}..]}
     */

    public function all() {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $languages = $this->language->get_all();
            $response['languages'] = $languages;
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
                $this->language->validation_rules[] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->language->validation_rules);
            if ($this->form_validation->run() === true) {
                $langdir_path = APPPATH . 'language/' . strtolower($this->input->post('machine_name'));
                if (!is_dir($langdir_path)) {
                    mkdir($langdir_path, 0777, TRUE);
                }

                $files = get_filenames($langdir_path);
                if (empty($files)) {
                    $english_dir_path = APPPATH . 'language/english';
                    $lang_files = get_filenames($english_dir_path);

                    foreach ($lang_files as $file) {
                        $source_file = $english_dir_path . '/' . $file;
                        $target_file = $langdir_path . '/' . $file;

                        copy($source_file, $target_file);
                    }
                }

                $this->language->model_data = $this->input->post(array('lang_name', 'machine_name'));

                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(LANG_DIR);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $this->language->model_data['lang_flag'] = upload_url(LANG_DIR . $filedata['filename']);
                    }
                }


                $created = $this->language->insert();
                if ($created) {
                    $this->language->model_data['id'] = $created;
                    $response['result'] = 'success';
                    $response['new_record'] = $this->language->model_data;
                    $response['message'] = $this->lang->line('lang_added');
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

            $this->language->validation_rules['machine_name']['rules'] = $this->language->validation_rules['machine_name']['rules'] = 'trim|required|alpha_dash|callback_langname_check[' . $id . ']';

            if ($this->input->post('filename')) {
                $this->language->validation_rules[] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->language->validation_rules);
            if ($this->form_validation->run() === true) {
                $langdir_path = APPPATH . 'language/' . strtolower($this->input->post('machine_name'));

                if (strtolower($this->input->post('machine_name')) != 'english') {
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

                $this->language->model_data = $this->input->post(array('lang_name', 'machine_name'));

                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(LANG_DIR);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        if ($this->input->post('lang_flag') and $this->input->post('lang_flag') != 'india.png') {
                            $this->media->delete_media(basename($this->input->post('lang_flag')), LANG_DIR);
                        }

                        $response['lang_flag'] = $this->language->model_data['lang_flag'] = upload_url(LANG_DIR . $filedata['filename']);
                    }
                }

                $updated = $this->language->update($id);
                if ($updated) {
                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('lang_updated');
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
     * To activate record
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function activate() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $language = $this->language->get_single(array('id' => $id));
            if ($language) {
                // is config file writable? we need to be sure of this before start
                if (is_really_writable($this->config->config_path) === FALSE && !@chmod($this->config->config_path, FILE_WRITE_MODE)) {
                    $response['errors'] = $this->config->config_path . $this->lang->line('file_not_writable');
                } else {
                    $config_array = array(
                        'language' => $language->machine_name,
                        'language_version' => date("YmdHis", now())
                    );

                    if (!$this->config->config_update($config_array)) {
                        $response['errors'] = $this->lang->line('file_not_writable') . $this->config->config_path;
                    } else {
                        if ($this->language->deactivate_langs()) {
                            $this->language->model_data['lang_status'] = 'publish';
                            if ($this->language->update($id)) {
                                $response['result'] = 'success';
                                $response['message'] = $this->lang->line('lang_activated');
                            } else {
                                $response['errors'] = $this->lang->line('process_error');
                            }
                        } else {
                            $response['errors'] = $this->lang->line('process_error');
                        }
                    }
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

            $language = $this->language->get_single(array('id' => $id));
            if ($language) {
                if ($language->lang_status == 'publish') {
                    $response['errors'] = $this->lang->line('published_lang_cant_be_deleted');
                } else {
                    $langdir_path = APPPATH . 'language/' . $language->machine_name;
                    if ($language->lang_flag) {
                        $this->media->delete_media(basename($language->lang_flag), LANG_DIR);
                    }

                    if ($language->machine_name != 'english' and delete_files($langdir_path) and $this->language->delete_where(array('id' => $id))) {
                        rmdir($langdir_path);

                        $response['result'] = 'success';
                        $response['message'] = $this->lang->line('lang_deleted');
                    } else {
                        $response['errors'] = $this->lang->line('process_error');
                    }
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
     * Get all files of a language.
     * 
     * @return
     *      if(successful execution) then {result: "success", files: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function lang_files() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $language = $this->language->get_single(array('id' => $id));
            if ($language and $language->machine_name != 'english') {
                $files = get_filenames(APPPATH . 'language/' . $language->machine_name);
                $response['files'] = $files;
                $response['result'] = 'success';
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
     * Get file all data 
     * 
     * @return
     *      if(successful execution) then {result: "success", files: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function get_file() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');
            $language_file = $this->input->get('langfile');

            $language = $this->language->get_single(array('id' => $id));
            if ($language) {
                $filepath = APPPATH . 'language/' . $language->machine_name . '/' . $language_file;

                // is config file writable? we need to be sure of this before start
                if (is_really_writable($filepath) === FALSE && !@chmod($filepath, FILE_WRITE_MODE)) {
                    $response['errors'] = $filepath . $this->lang->line('file_not_writable');
                } else {
                    $language_file = str_replace(".php", "", $language_file);
                    $english_data = $this->lang->load($language_file, 'english', TRUE, FALSE);
                    $filedata = $this->lang->load($language_file, $language->machine_name, TRUE, FALSE);

                    foreach ($english_data as $langkey => $langval) {
                        if (!isset($filedata[$langkey])) {
                            $filedata[$langkey] = str_replace(site_url(), "'.site_url().'", $langval);
                        } else {
                            $lline_val = $filedata[$langkey];
                            $filedata[$langkey] = str_replace(site_url(), "'.site_url().'", $lline_val);
                        }
                    }

                    $response['filedata'] = $filedata;
                    $response['english_data'] = $english_data;
                    $response['result'] = 'success';
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
     * Update translated data in file
     * 
     * @return
     *      if(successful execution) then {result: "success", files: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function update_translate() {
        $response = array('result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');
            $language_file = $this->input->get('langfile');

            $language = $this->language->get_single(array('id' => $id));
            if ($language and $language->machine_name != 'english') {
                $filepath = APPPATH . 'language/' . $language->machine_name . '/' . $language_file;
                $data_array = $this->input->post();

                // is config file writable? we need to be sure of this before start
                if (is_really_writable($filepath) === FALSE && !@chmod($filepath, FILE_WRITE_MODE)) {
                    $response['errors'] = $filepath . $this->lang->line('file_not_writable');
                } elseif ($this->__update_file($data_array, $filepath)) {
                    $config_array = array(
                        'language_version' => date("YmdHis", now())
                    );

                    if (!$this->config->config_update($config_array)) {
                        $response['errors'] = $this->lang->line('file_not_writable') . $this->config->config_path;
                    } else {
                        $response['result'] = 'success';
                        $response['postdata'] = $this->input->post();
                        $response['message'] = $this->lang->line('lang_updated');
                    }
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

    /**
     * Update the lang file
     * 
     * @param Array $data_array
     * @param String Path $filepath
     * @param Array $filedata
     * @return Booleaun
     */
    function __update_file($data_array = array(), $filepath) {
        if (!is_array($data_array) && count($data_array) == 0) {
            return FALSE;
        }

        @chmod($filepath, FILE_WRITE_MODE);

        // Read the config file as PHP
        require $filepath;

        // load the file helper
        $this->load->helper('file');

        // Read the config data as a string
        $input_file = read_file($filepath);
        $input_file = "<?php";
        $input_file .= "\ndefined('BASEPATH') OR exit('No direct script access allowed');";
        $input_file .= "\n";

        // Do we need to add totally new items to the config file?
        if (is_array($data_array)) {
            foreach ($data_array as $key => $val) {
                if (strtolower($val) == 'true' or strtolower($val) == 'false') {
                    $input_file .= "\n\$lang['$key'] = " . $val . ";";
                } else {
                    $input_file .= "\n\$lang['$key'] = '" . addslashes(stripslashes($val)) . "';";
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
