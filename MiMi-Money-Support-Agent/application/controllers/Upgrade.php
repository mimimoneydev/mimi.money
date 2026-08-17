<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Upgrade extends CP_AdminController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();
    }

    /*
     * To load upgrade links view
     */

    function index() {
        // add users.js file
        $this->addjs(theme_url("js/app/upgrade.js"), TRUE);
        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings));

        //write code here to load view
        $this->bulid_layout("pages/upgrade");
    }

    /*
     * To load upgrade view
     */

    function upgrade() {
        // Update config/database.php file
        $dbdata = array('dbprefix' => DB_PRIFIX);
        $this->config->db_config_update($dbdata);

        // add css and js file
        $this->body_classes[] = 'intallation-process';
        $this->addcss(base_url("assets/cmodule/css/cmodule-installer.css"), TRUE);
        $this->addjs(theme_url("js/app/install/main.js"), TRUE);
        $this->addjs(theme_url("js/app/install/upgrade.js"), TRUE);

        $files_updated = 'no';
        $action_type = 'update';
        if ($this->settings->current_version != CHATBULL_VERSION or ! isset($this->settings->current_product_name) or $this->settings->current_product_name != PRODUCT_NAME) {
            $files_updated = 'yes';
        }

        if (isset($this->settings->current_product_name) and $this->settings->current_product_name != PRODUCT_NAME) {
            $action_type = 'upgrade';
        }

        // get all agents with admin
        $upgrade_text = $this->lang->load('upgrade', '', TRUE);
        $data = array('upgrade_text' => $upgrade_text, 'settings' => $this->settings);
        $data['files_updated'] = $files_updated;
        $data['action_type'] = $action_type;
        $this->add_js_inline($data);

        //write code here to load view
        $data['content'] = $this->load->view('install/upgrade', $data, TRUE);
        $this->load->view('install/template', $data);
    }

    /*
     * To send request to plugin domain and return response
     * 
     * @return {Json) $response
     */

    function get_server() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');
        $action = ($this->input->get('action')) ? $this->input->get('action') : '';

        // sending request on server to check version.
        $url = CHATBULL_APIURL . 'api-info.php';
        $fields = array('product_name' => PRODUCT_NAME, 'action' => $action);

        $this->curl->create($url);
        $this->curl->post($fields);
        $result = json_decode($this->curl->execute());

        if ($result->result == 'success') {
            $response = $result;
        } else {
            $response['errors'] = $result->errors;
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Verify license key (purchase code) from plugin domain and return response
     * 
     * @return {Json) $response
     */

    function verify_license_key() {
        $response = array('errors' => '', 'result' => 'failed');

        $license_key = $this->input->post('license_key');
        if ($license_key) {
            $result = validate_license_key($license_key);

            if ($result->result == 'success') {
                $response['result'] = 'success';
                $response['message'] = $result->message;

                $this->configuration->model_data = array('config_value' => $license_key);
                $this->configuration->update_where(array('config_option' => 'licence_key'));
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
            $response['errors'] = sprintf($this->lang->line('reuired_field'), $this->lang->line('licence_key'));
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function downoad latest version of chatbull plugin
     */

    function download() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');

        // load the file helper
        $this->load->helper('string');
        $download_url = CHATBULL_APIURL . 'api-upgrade.php?action=download';
        $filename = "chatbull-" . random_string('alnum') . ".zip";
        $zipFile = UPDATE_DIR . $filename; // Local Zip File Path

        if (!is_dir(UPDATE_DIR)) {
            mkdir(UPDATE_DIR, 0777, TRUE);
        }

        if (is_really_writable(UPDATE_DIR) === FALSE) {
            if (!chmod(UPDATE_DIR, DIR_WRITE_ALL_MODE)) {
                $response['errors'] = sprintf($this->lang->line('dir_not_writable'), 'Update');
            }
        }

        if ($response['errors'] == '') {
            // setting header
            $headers = array(
                'purchasecode: ' . $this->settings->licence_key,
                'registerurl: ' . base_url()
            );

            $zipResource = @fopen($zipFile, FOPEN_READ_WRITE_CREATE_DESTRUCTIVE);
            $ch = curl_init(str_replace(" ", "%20", $download_url)); //Here is the file we are downloading, replace spaces with %20
            curl_setopt($ch, CURLOPT_TIMEOUT, 5000);
            curl_setopt($ch, CURLOPT_FILE, $zipResource); // write curl response to file
            curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
            curl_setopt($ch, CURLOPT_AUTOREFERER, true);
            curl_setopt($ch, CURLOPT_BINARYTRANSFER, true);
            curl_setopt($ch, CURLOPT_FAILONERROR, true);
            curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);
            curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
            $result = curl_exec($ch); // get curl response
            curl_close($ch);
            fclose($zipResource);

            if ($result) {
                $response['downloaded_filename'] = $filename;
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('plugin_name') . " " . $this->lang->line('files_downloaded');
                $response['response'] = $result;
            } else {
                
            }
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function Extracting downloaded latest version of chatbull plugin
     */

    function extract() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');
        $downloaded_filename = ($this->input->get('downloaded_filename')) ? $this->input->get('downloaded_filename') : '';
        $zipFile = UPDATE_DIR . $downloaded_filename; // Local Zip File Path

        if (file_exists($zipFile)) {
            $output = extractZip($zipFile, FCPATH);
            if ($output['result'] == 'failed') {
                $response['errors'] = $output['errors'];
            } else {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('files_extracted');
            }
        } else {
            $response['errors'] = $this->lang->line('zip_not_found');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will be use to make changes in database on made in new version.
     * #method Get
     * @return json object
     */

    function update_db() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');
        $response['message'] = $this->lang->line('db_updated');
        $response['result'] = 'success';

        $this->load->library('migration');

        if ($this->migration->latest() === FALSE) {
            $response['errors'] = $this->migration->error_string();
        } else {
            $sites = $this->site_model->get_sites();
            $this->configuration->merge_settings_options($sites);

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

            // changing site lived year value.
            $lived_date = explode("-", $this->settings->site_lived_year);
            if (count($lived_date) < 2) {
                $admin = $this->user->get_single(array('role' => 'admin'));

                // updating site_lived_year in database.
                $this->configuration->model_data = array('site_lived_year' => date("Y-m-d", strtotime($admin->created_at)));
                $this->configuration->update();
            }

            $this->_update_languages();

            $files = array('desktop_lang', 'chat_lang', 'conf_lang', 'user_lang');
            $this->sync_lanuages($files);
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

    /*
     * To update version to database.
     */

    function upgrade_complete() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');
        $action_type = $this->input->get('action_type');

        $response['result'] = 'success';

        // sending request on server to check version.
        if ($action_type == 'upgrade') {
            $response['message'] = $this->lang->line('upgrade_completed');
            $url = CHATBULL_APIURL . 'api-upgrade.php?action=add-history';
            $fields = array('current_version' => $this->settings->current_version, 'current_product_name' => $this->settings->current_product_name, 'new_version' => CHATBULL_VERSION);
        } else {
            $response['message'] = $this->lang->line('update_completed');
            $url = CHATBULL_APIURL . 'api-update.php?action=add-history';
            $fields = array('current_version' => $this->settings->current_version, 'new_version' => CHATBULL_VERSION);
        }

        $this->curl->create($url);
        $this->curl->http_header('purchasecode', $this->settings->licence_key);
        $this->curl->http_header('registerurl', base_url());
        $this->curl->post($fields);
        $result = $this->curl->execute();

        // updating version in database.
        $this->configuration->model_data = array('current_version' => CHATBULL_VERSION, 'current_product_name' => PRODUCT_NAME);

        if ($action_type == 'upgrade') {
            $this->configuration->model_data['licence_key'] = '';

            $update_config_data = array();
            $update_config_data['validated_code'] = 'no';
            $this->config->config_update($update_config_data);
        }

        $this->configuration->update();

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
