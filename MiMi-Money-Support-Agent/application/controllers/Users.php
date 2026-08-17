<?php

/*
 * Controller to manage all users related actions
 */

class Users extends CP_AdminController {
    /*
     * Calling parent constructor
     */

    public function __construct() {
        parent::__construct();

        // check has admin access
        $this->has_admin_access();
    }

    /*
     * To load view for users management
     */

    function index() {
        // add users.js file
        $this->addjs(theme_url("js/app/directives/modal-confirm.js"), TRUE);
        $this->addcss(base_url("assets/ng-img-crop/compile/minified/ng-img-crop.css"), TRUE);
        $this->addjs(base_url("assets/ng-img-crop/compile/minified/ng-img-crop.js"), TRUE);
        $this->addjs(theme_url("js/app/users.js"), TRUE);

        $tags = $this->tag->get_all();

        $script_data = array(
            'sites' => $this->site_model->get_sites(),
            'tags' => $tags,
            'roles' => $this->roles,
            'settings' => $this->settings,
            'user' => $this->current_user
        );
        $this->add_js_inline($script_data);

        // add filter file.
        $this->data['filter_view'] = 'users/filters.php';

        //write code here to load view
        $this->bulid_layout("users/list");
    }

    /*
     * To pull users list with filter options applied if any given
     * Possible filter variables in POST (role, keywords, tags (array))
     * 
     */

    function users_list() {
        $response = array('result' => 'success');
        
        if ($this->current_user) {
            $response['users'] = $this->user->get_users($this->input->post());
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To add a new user into db
     * will have POST values for variables (name, display_name, email, pass, confirm_pass, contact_number, role)
     * $_FILES variable profile_pic 
     * 
     * @return 
     *      if(successful data entry) then {result: "success", id: user_id, message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{error_data}]}
     */

    function add_user() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            if ($this->input->post('filename')) {
                $this->user->validation_rules['insert'][] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->user->validation_rules['insert']);

            if ($this->form_validation->run() === true) {
                $this->user->model_data = $this->input->post(array('name', 'display_name', 'contact_number', 'email', 'pass', 'role', 'profile_color'));
                if (is_null($this->user->model_data['contact_number'])) {
                    $this->user->model_data['contact_number'] = '';
                }

                $this->user->model_data['pass'] = hashpassword($this->user->model_data['pass']);

                $this->user->model_data['profile_pic'] = '';
                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(PROFILE_PICS);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $upload_path = $upload_path . 'thumb/';
                        $base64_array['base64'] = substr($this->input->post('base64WithData'), strpos($this->input->post('base64WithData'), ",") + 1);
                        $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);
                        if ($filedata['uploaded']) {
                            $this->user->model_data['profile_pic'] = $filedata['filename'];
                        }
                    }
                }

                if (empty($response['errors']) and $user_id = $this->user->insert()) {
                    // saving user sites
                    $this->users_sites->model_data['sites'] = $this->input->post('sites');
                    $this->users_sites->save($user_id);

                    // saving user departments
                    $this->user_tag->model_data['tags'] = $this->input->post('tags');
                    $this->user_tag->save($user_id);

                    $user = $this->user->get_single(array('id' => $user_id), array('profile_pic', 'profile_color'));

                    $userdata = $this->input->post();
                    $userdata['id'] = $user_id;
                    $userdata['profile_color'] = $user->profile_color;
                    $userdata['profile_pic'] = $user->profile_pic;
                    $userdata['profile_picture'] = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $this->input->post('email'));
                    $userdata['tags'] = $this->user_tag->get_user_tags($user_id);
                    $userdata['sites'] = $this->users_sites->get_sites($user_id);

                    $response['result'] = 'success';
                    $response['user'] = $userdata;
                    $response['message'] = $this->lang->line('user_added');
                } elseif (empty($response['errors'])) {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors();
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To update an existing user
     * Input will be given in $_POST name, display_name, contact_number
     * Input in $_FILES profile_pic
     * 
     * @return
     *      if (successful exectution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed"}
     */

    function update_user() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $this->user->validation_rules['update'] = $this->setEditUniqueRule($this->user->validation_rules['update'], 'email', $id);

            if ($this->input->post('filename')) {
                $this->user->validation_rules['update'][] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->user->validation_rules['update']);

            if ($this->form_validation->run() === true) {
                $this->user->model_data = $this->input->post(array('name', 'display_name', 'contact_number', 'email', 'profile_color'));

                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(PROFILE_PICS);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $this->media->delete_media($this->input->post('profile_pic'), PROFILE_PICS);

                        $upload_path = $upload_path . 'thumb/';
                        $base64_array['base64'] = substr($this->input->post('base64WithData'), strpos($this->input->post('base64WithData'), ",") + 1);
                        $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);
                        if ($filedata['uploaded']) {
                            $this->user->model_data['profile_pic'] = $filedata['filename'];
                        }
                    }
                }

                if (empty($response['errors']) and $this->user->update($id)) {
                    // saving user sites
                    $this->users_sites->model_data['sites'] = $this->input->post('sites');
                    $this->users_sites->save($id);

                    // saving user departments
                    $this->user_tag->model_data['tags'] = $this->input->post('tags');
                    $this->user_tag->save($id);

                    $user = $this->user->get_single(array('id' => $id), array('profile_pic'));

                    $userdata = $this->input->post();
                    $userdata['profile_pic'] = $user->profile_pic;
                    $userdata['profile_picture'] = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $this->input->post('email'));
                    $userdata['last_login_string'] = strtotime($userdata['last_login']) * 1000;
                    $userdata['tags'] = $this->user_tag->get_user_tags($id);
                    $userdata['sites'] = $this->users_sites->get_sites($id);

                    $response['result'] = 'success';
                    $response['user'] = $userdata;
                    $response['message'] = $this->lang->line('user_updated');
                } elseif (empty($response['errors'])) {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors();
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will show edit profile page.
     */

    function edit_profile() {
        // add users.js file
        $this->addcss(base_url("assets/ng-img-crop/compile/minified/ng-img-crop.css"), TRUE);
        $this->addjs(base_url("assets/ng-img-crop/compile/minified/ng-img-crop.js"), TRUE);
        $this->addjs(theme_url("js/app/profile.js"), TRUE);

        $script_data = array(
            'sites' => $this->site_model->get_sites(),
            'settings' => $this->settings,
            'user' => $this->current_user
        );
        $this->add_js_inline($script_data);

        //write code here to load view
        $this->bulid_layout("users/profile");
    }

    /*
     * To update an existing user
     * Input will be given in $_POST name, display_name, contact_number
     * Input in $_FILES profile_pic
     * 
     * @return
     *      if (successful exectution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed"}
     */

    function update_profile() {
        $id = $this->input->get('id');
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $this->user->validation_rules['update'] = $this->setEditUniqueRule($this->user->validation_rules['update'], 'email', $id);

            if ($this->input->post('filename')) {
                $this->user->validation_rules['update'][] = array(
                    'field' => 'base64',
                    'label' => 'lang:base64',
                    'rules' => 'trim|required|valid_base64'
                );
            }

            //check if data is valid or not
            $this->form_validation->set_rules($this->user->validation_rules['update']);

            if ($this->form_validation->run() === true) {
                $this->user->model_data = $this->input->post(array('name', 'display_name', 'contact_number', 'email', 'profile_color'));

                if ($this->input->post('filename')) {
                    $base64_array = $this->input->post(array('base64', 'filename', 'filetype', 'filesize'));
                    $upload_path = upload_dir(PROFILE_PICS);
                    $file_prefix = date("YmdHis", now()) . '_';
                    $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);

                    if ($filedata['uploaded']) {
                        $this->media->delete_media($this->input->post('profile_pic'), PROFILE_PICS);

                        $upload_path = $upload_path . 'thumb/';
                        $base64_array['base64'] = substr($this->input->post('base64WithData'), strpos($this->input->post('base64WithData'), ",") + 1);
                        $filedata = $this->media->upload_base64_file($upload_path, $file_prefix, $base64_array);
                        if ($filedata['uploaded']) {
                            $this->user->model_data['profile_pic'] = $filedata['filename'];
                        }
                    }
                }

                if (empty($response['errors']) and $this->user->update($id)) {
                    // saving user sites
                    $this->users_sites->model_data['sites'] = $this->input->post('sites');
                    $this->users_sites->save($id);

                    // saving user departments
                    $this->user_tag->model_data['tags'] = $this->input->post('tags');
                    $this->user_tag->save($id);

                    $user = $this->user->get_single(array('id' => $id), array('profile_pic'));

                    $response['result'] = 'success';
                    $response['profile_pic'] = $user->profile_pic;
                    $response['profile_picture'] = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS);
                    $response['message'] = $this->lang->line('profile_updated');
                } elseif (empty($response['errors'])) {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors();
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * This function will show change password page.
     */

    function change_password() {
        // add users.js file
        $this->addcss(base_url("assets/ng-img-crop/compile/minified/ng-img-crop.css"), TRUE);
        $this->addjs(base_url("assets/ng-img-crop/compile/minified/ng-img-crop.js"), TRUE);
        $this->addjs(theme_url("js/app/profile.js"), TRUE);

        $script_data = array(
            'sites' => $this->site_model->get_sites(),
            'settings' => $this->settings,
            'user' => $this->current_user
        );
        $this->add_js_inline($script_data);

        //write code here to load view
        $this->bulid_layout("users/edit_password");
    }

    /*
     * To pull single user's complete data
     * 
     * Input GET $id (id of user)
     * 
     * @return
     *      {user complete object with tags}
     */

    function get() {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $this->user->get($this->input->get('id'));
            $this->user->model_data['profile_picture'] = $this->media->get_thumbnail($this->user->model_data['profile_pic'], PROFILE_PICS, $this->user->model_data['email']);
            if (count($this->user->model_data['sites']) > 0) {
                $sites_departments = $this->configuration->get_site_departments($this->user->model_data['sites']);
                $response['departments'] = $sites_departments;
            }

            $response['user'] = $this->user->model_data;
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * Get site departments 
     * 
     * @return Json $response
     */

    function get_departments() {
        $response = array('result' => 'success');

        if ($this->current_user) {
            $seleted_sites = $this->input->post('sites');

            $sites_departments = $this->configuration->get_site_departments($seleted_sites);
            $response['departments'] = $sites_departments;
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To change password
     * 
     * Input in GET $id
     * 
     * Input in POST pass, confirm_pass
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function update_password() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            //check if data is valid or not
            $this->form_validation->set_rules($this->user->validation_rules['update_password']);
            if ($this->form_validation->run() === true) {
                $this->user->model_data['pass'] = hashpassword($this->input->post('pass'));

                if ($this->user->update($id)) {
                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('password_updated');
                } else {
                    $response['errors'] = $this->lang->line('process_error');
                }
            } else {
                $response['errors'] = validation_errors();
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To change status
     * 
     * Input in POST user_status
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function update_status() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $this->user->model_data['user_status'] = $this->input->post('user_status');
            if ($this->user->update($id)) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('user_status_changed');
            } else {
                $response['errors'] = $this->lang->line('process_error');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To delete user
     *      This doesn't mean we will delete user permanently. It will just mark that user's user_status as 
     * 
     * @return
     *      if(successful execution) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else {result: "failed", errors: [{all error messages}]}
     */

    function delete_user() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            if ($this->user->delete($id)) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('user_deleted');
            } else {
                $response['errors'] = $this->lang->line('process_error');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To delete user profile picture from db and folder
     * 
     * @return response 
     */

    function remove_picture() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $id = $this->input->get('id');

            $user = $this->user->get_single(array('id' => $id), array('profile_pic', 'email'));
            $this->user->model_data['profile_pic'] = '';
            if ($this->user->update($id)) {
                $this->media->delete_media($user->profile_pic, PROFILE_PICS);
                $src = $this->media->get_thumbnail($this->user->model_data['profile_pic'], PROFILE_PICS, $user->email);

                $response['result'] = 'success';
                $response['src'] = $src;
                $response['message'] = $this->lang->line('profile_pic_deleted');
            } else {
                $response['errors'] = $this->lang->line('process_error');
            }
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

}
