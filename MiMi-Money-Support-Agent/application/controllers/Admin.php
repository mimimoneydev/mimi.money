<?php

defined('BASEPATH') OR exit('No direct script access allowed');

class Admin extends CP_AdminController {

    /**
     * Index Page for this controller.
     *
     * Maps to the following URL
     * 		http://example.com/index.php/welcome
     * 	- or -
     * 		http://example.com/index.php/welcome/index
     * 	- or -
     * Since this controller is set as the default controller in
     * config/routes.php, it's displayed at http://example.com/
     *
     * So any other public methods not prefixed with an underscore will
     * map to /index.php/welcome/<method_name>
     * @see http://codeigniter.com/user_guide/general/urls.html
     */
    public function index() {
        // check has admin access
        $this->has_admin_access();
        $this->temp_visitor->del_older_anonymous_visitors();

        // add users.js file
        $this->addjs(base_url("assets/flot/jquery.flot.js"), TRUE);
        $this->addjsIe(base_url("assets/flot/excanvas.min.js"), 8);
        $this->addjs(base_url("assets/flot/jquery.flot.time.js"), TRUE);
        $this->addjs(base_url("assets/flot/jquery.flot.resize.js"), TRUE);
        $this->addjs(base_url("assets/flot/jquery.flot.selection.js"), TRUE);
        $this->addjs(base_url("assets/flot/jshashtable-2.1.js"), TRUE);
        $this->addjs(base_url("assets/flot/jquery.numberformatter-1.2.3.min.js"), TRUE);

        if ($this->settings->google_map_api_key) {
            $this->addjs("//maps.google.com/maps/api/js?key=" . $this->settings->google_map_api_key, TRUE);
        }

        $this->addjs(theme_url("js/app/dashboard.js"), TRUE);

        $data = array('user' => $this->current_user, 'settings' => $this->settings);

        $lived_date = explode("-", $this->settings->site_lived_year);
        $livedYear = (isset($lived_date[0])) ? intval($lived_date[0]) : date('Y', now());
        $livedMonth = (isset($lived_date[1])) ? intval($lived_date[1]) : 1;
        $current_year = date('Y', now());
        $data['current_year'] = array('year' => '');
        $months_labels = array(
            '1' => "Jan",
            '2' => "Feb",
            '3' => "Mar",
            '4' => "Apr",
            '5' => "May",
            '6' => "Jun",
            '7' => "Jul",
            '8' => "Aug",
            '9' => "Sep",
            '10' => "Oct",
            '11' => "Nov",
            '12' => "Dec"
        );

        $working_years = array();
        for ($y = $livedYear; $y <= $current_year; $y++) {
            $monthrange = 12;
            $month_start = 1;
            if ($current_year == $y) {
                $monthrange = date('m', now());
            }

            if ($livedYear == $y) {
                $month_start = $livedMonth;
            }

            $months = array();
            for ($m = $month_start; $m <= $monthrange; $m++) {
                $months[] = array('month' => sprintf('%02d', $m), 'name' => $months_labels[$m]);
            }

            if ($current_year == $y) {
                $data['current_year'] = array('year' => $y, 'months' => $months);
            }

            $working_years[] = array('year' => $y, 'months' => $months);
        }

        $data['working_years'] = $working_years;
        $data['label_chat_requests_per_day'] = $this->lang->line('dash_chat_requests_per_day');
        $data['label_visitors_per_day'] = $this->lang->line('dash_visitors_per_day');

        $this->add_js_inline($data);

        $this->bulid_layout("pages/dashboard");
    }

    /*
     * This will return dashboard data to view.
     * 
     * @return Json $response
     */

    function get_dashboard_data() {
        $response = $this->get_chart_data('result');

        if ($this->current_user) {
            $site_id = $this->input->get('site_id');

            $response['pageviews'] = $this->user->get_pagevies($site_id);

            $where = array();
            if ($site_id) {
                $where['site_id'] = $site_id;
            }
            $response['online_requests'] = $this->chat_session->get_single($where, ' count(*) as total ');
            $response['offline_requests'] = $this->orequest->get_single($where, ' count(*) as total ');
            $response['most_rated_operators'] = $this->feedback->get_most_rated_operators($site_id);

            $response['recent_chats'] = $this->chat_session->get_opened_chats($site_id);
            $response['operators_n_requests'] = $this->chat_session->operators_n_requests($site_id);

            $response['anonymous_users'] = array();
            $response['visitors'] = array();

            if ($this->settings->google_map_api_key) {
                $site_id = $this->input->get('site_id');

                $anonymous_users = $this->temp_visitor->get_online_visitors($site_id);
                $visitors = $this->user->getVisitorsAddress($site_id);
                $response['anonymous_users'] = $anonymous_users;
                $response['visitors'] = $visitors;
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Get chart data
     * 
     * @param String $return_type Default 
     */

    function get_chart_data($return_type = 'json') {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $settings = $this->settings;
            $site_id = $this->input->get('site_id');
            $year = $this->input->get('year');
            $month = $this->input->get('month');

            $lived_date = explode("-", $settings->site_lived_year);
            $livedYear = (isset($lived_date[0])) ? intval($lived_date[0]) : date('Y', now());
            $livedMonth = (isset($lived_date[1])) ? intval($lived_date[1]) : 1;
            $livedDay = (isset($lived_date[2])) ? intval($lived_date[2]) : 1;

            $endTime = date('Y', now());
            $today_data = date('Y-m', now());

            $users_record = array();
            $users_counter = array();
            $visitors_counter = array();
            $monthly_users = 0;
            $monthly_visitors = 0;
            $total_users = 0;

            $where = " DATE_FORMAT(`started_at`, '%Y-%m') = '" . $year . "-" . sprintf('%02d', $month) . "' ";
            $requestsData = $this->user->getRequestsData($site_id, $where);
            $where = " DATE_FORMAT(`created_at`, '%Y-%m') = '" . $year . "-" . sprintf('%02d', $month) . "' ";
            $visitorsData = $this->user->get_visitors($site_id, $where);

            if (function_exists('cal_days_in_month')) {
                $days_in_month = cal_days_in_month(CAL_GREGORIAN, $month, $year);
            } else {
                $days_in_month = date('t', mktime(0, 0, 0, $month, 1, $year));
            }

            for ($d = 1; $d <= $days_in_month; $d++) {
                $datestr = $year . "-" . sprintf('%02d', $month) . "-" . sprintf('%02d', $d);
                $daily_visitors = (isset($visitorsData[$datestr])) ? intval($visitorsData[$datestr]) : 0;
                $visitors_counter[] = array($year, $month, $d, $daily_visitors);

                $daily_users = (isset($requestsData[$datestr])) ? intval($requestsData[$datestr]) : 0;
                $users_record[] = array($year, $month, $d, $daily_users);
            }

            $response['users_per_day'] = array('total' => $users_record);
            $response['visitors_counter'] = array('total' => $visitors_counter);
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        if ($return_type == 'json') {
            return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
        }

        return $response;
    }

    /*
     * To set admin data
     */

    function set_data() {
        $output = array('result' => 'success', 'message' => $this->lang->line('information_saved'));

        if ($this->settings->licence_key and $this->config->item('validated_code') == 'yes') {
            $url = CHATBULL_APIURL . 'notify_domain.php?action=is-registered';
            $fields = array('license_key' => $this->settings->licence_key, 'site_url' => base_url());

            $this->curl->create($url);
            $this->curl->post($fields);
            $result = $this->curl->execute();
            $response = json_decode($result);
            if ($response and $response->result == 'failed') {
                // load the file helper
                $this->load->helper('string');
                if ($this->config->config_update(array('validated_code' => "no"))) {
                    $this->configuration->model_data = array('config_value' => '');
                    $this->configuration->update_where(array('config_option' => 'licence_key'));
                }
            }
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($output));
    }

    /*
     * This function will dismiss update availabel notification
     * 
     * @return json object
     */

    function dismis_message() {
        $this->session->set_userdata('dismis_update_alert', 'yes');
    }

    /*
     * To check is upgrades are available for current project.
     */

    function upgrade_available() {
        $response = array('result' => 'failed', 'errors' => '', 'message' => '');

        // sending request on server to check version.
        $url = CHATBULL_APIURL . 'api-info.php';
        $fields = array('product_name' => PRODUCT_NAME, 'action' => 'has-upgrade');

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
     * This function display login form.
     */

    function login() {
        $this->form_validation->set_error_delimiters('<div class="input-error">', '</div>');
        //check if data is valid or not
        $this->form_validation->set_rules($this->user->validation_rules['login']);

        if ($this->form_validation->run() === true) {
            $user = $this->authentication->login($this->input->post('email'), $this->input->post('password'));
            if ($user) {
                if ($this->input->post('remember_me') != NULL) {
                    $cookie = array(
                        'name' => 'remember_token',
                        'value' => $user->remember_token,
                        'expire' => (86500 * 30)
                    );
                } else {
                    $cookie = array(
                        'name' => 'remember_token',
                        'value' => $user->remember_token,
                        'expire' => ''
                    );
                }

                $this->input->set_cookie($cookie);

                // closing closable sessions
                $this->user->closeClosedSessions();

                if ($user->role == 'agent') {
                    redirect("d=agents&c=agents");
                } else {
                    redirect("c=admin");
                }
            }

            redirect("c=admin&m=login");
        }

        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings));
        $this->bulid_layout("pages/login");
    }

    /*
     * Login request by ajax
     */

    function logiin_ajax() {
        $response = array('result' => 'failed', 'description' => '');

        $this->form_validation->set_error_delimiters('<div class="input-error">', '</div>');
        //check if data is valid or not
        $this->form_validation->set_rules($this->user->validation_rules['login']);

        if ($this->form_validation->run() === true) {
            $output = $this->authentication->logiin_ajax($this->input->post('email'), $this->input->post('password'));
            if ($output['result'] == 'success') {
                $user = $output['user'];
                if ($this->input->post('remember_me') != NULL) {
                    $cookie = array(
                        'name' => 'remember_token',
                        'value' => $user->remember_token,
                        'expire' => (86500 * 30)
                    );
                } else {
                    $cookie = array(
                        'name' => 'remember_token',
                        'value' => $user->remember_token,
                        'expire' => ''
                    );
                }

                $this->input->set_cookie($cookie);

                $response['result'] = 'success';
            } else {
                $response['errors'] = $output['error'];
            }
        } else {
            $response['errors'] = validation_errors();
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This funtion destroy session
     */

    public function logout() {
        //$this->session->sess_destroy();
        $this->session->unset_userdata('workroomHistory');
        $this->session->unset_userdata('current_user_id');
        $this->session->unset_userdata('dismis_update_alert');
        $this->session->unset_userdata('is_update_checked');
        $this->session->unset_userdata('new-version');

        $remember_token = $this->input->cookie('remember_token');
        $cookie = array(
            'name' => 'remember_token',
            'value' => $remember_token,
            'expire' => ''
        );

        $this->input->set_cookie($cookie);

        $this->session->set_flashdata('success', $this->lang->line('logged_out'));
        redirect("");
    }

    /*
     * This function display forget password form and process.
     */

    public function forget_password() {
        //check if data is valid or not
        $this->form_validation->set_rules($this->password_reminder->validation_rules['forget_password']);
        if ($this->form_validation->run() === true) {
            $user = $this->user->get_single(array('email' => $this->input->post('email'), 'role !=' => 'visitor'));
            if ($user) {
                $pass_token = random_string('alnum', 24);
                if ($this->password_reminder->temp_reset_password($pass_token, $this->input->post('email'))) {

                    // send email to visitor
                    $template_file = 'forget_password';
                    $to = $this->input->post('email');
                    $subject = $this->lang->line('reset_your_password');
                    $link = site_url('c=admin&m=reset_password&token=' . $pass_token);
                    $message = $this->lang->line('email_change_pass_description');

                    $data = array(
                        'name' => $user->name,
                        'link' => $link,
                        'message' => $message
                    );

                    $response = send_template_email($template_file, $to, $subject, $data);
                    if ($response) {
                        $this->session->set_flashdata('success', $this->lang->line('reset_link_sent'));
                        redirect("c=admin&m=login");
                    } else {
                        $this->session->set_flashdata('error', $this->lang->line('reset_link_not_sent'));
                        redirect("c=admin&m=forget_password");
                    }
                } else {
                    $this->session->set_flashdata('error', $this->lang->line('process_error'));
                    redirect("c=admin&m=forget_password");
                }
            }
            $this->session->set_flashdata('error', $this->lang->line('email_not_found'));
            redirect("c=admin&m=forget_password");
        }

        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings));
        $this->form_validation->set_error_delimiters('<div class="input-error">', '</div>');
        $this->bulid_layout("pages/forget_password", 'simple_layout');
    }

    /*
     * This function display reset password form and process.
     * 
     * @param  $pass_token
     */

    public function reset_password() {
        $pass_token = $this->input->get('token');
        $user = $this->password_reminder->is_temp_pass_valid($pass_token);
        if (!$user) {
            $this->session->set_flashdata('error', $this->lang->line('invalid_pass_token'));
            redirect("c=admin&m=forget_password");
        } else {
            $delta = 86400;
            if ($_SERVER["REQUEST_TIME"] - $user->created_at > $delta) {
                $this->session->set_flashdata('error', $this->lang->line('token_expired'));
                redirect("c=admin&m=forget_password");
            } else {
                //check if data is valid or not
                $this->form_validation->set_rules($this->password_reminder->validation_rules['update_password']);
                if ($this->form_validation->run() === true) {
                    $this->user->model_data = array('pass' => hashpassword($this->input->post('pass')));
                    $updated = $this->user->update_where(array('email' => $user->email));
                    if ($updated) {
                        $this->password_reminder->delete_where(array('email' => $user->email));
                        $this->session->set_flashdata('success', $this->lang->line('password_updated'));
                        redirect("c=admin&m=login");
                    } else {
                        $this->session->set_flashdata('error', $this->lang->line('process_error'));
                        redirect("c=admin&m=reset_password/" . $pass_token);
                    }
                }
            }
        }

        $this->add_js_inline(array('user' => $this->current_user, 'settings' => $this->settings));
        $this->data['token'] = $pass_token;
        $this->form_validation->set_error_delimiters('<div class="input-error">', '</div>');
        $this->bulid_layout("pages/reset_password", 'simple_layout');
    }

    /*
     * To data checker
     */

    function checker() {
        show_404();
    }

    /*
     * For mvapp
     */

    function mvapp() {
        show_404();
    }

    /*
     * For mivaap
     */

    function mivaap() {
        show_404();
    }

}
