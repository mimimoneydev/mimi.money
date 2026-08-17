<?php

class Tags extends CP_AdminController {

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
        $this->addjs(theme_url("js/app/tags.js"), TRUE);
        $tags = $this->tag->get_all();
        $this->add_js_inline(array('tags' => $tags, 'user' => $this->current_user, 'settings' => $this->settings));

        //write code here to load view
        $this->bulid_layout("tags/list");
    }

    /*
     * To list all tags exists in DB
     * 
     * @return
     *      {result: "success", tags: [{id, name}..]}
     */

    public function get_tags() {
        $response = array('result' => 'success');
        if ($this->current_user) {
            $response['departments'] = $this->tag->get_tags($this->input->post('offset'));
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }

        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To add a tag in db
     * 
     * Input will be given in $_POST (tag_name)
     * 
     * @return 
     *      if(successful validtaion) then {result: "success", id: "tag's id", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else    {result: "failed", errors: [{error messages}]}
     */

    public function add_tag() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            //check if data is valid or not
            $this->form_validation->set_rules($this->tag->validation_rules['insert']);

            if ($this->form_validation->run() === true) {
                $this->tag->model_data = $this->input->post(array('tag_name'));
                $insert_id = $this->tag->insert();

                if ($insert_id) {

                    $data = $this->input->post();
                    $data['id'] = $insert_id;

                    $response['result'] = 'success';
                    $response['tag'] = $data;
                    $response['message'] = $this->lang->line('tag_added');
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
     * To update a tag
     * 
     * GET Input given $id (id of the tag)
     * 
     * Input will be given in $_POST (tag_name)
     * 
     * @return 
     *      if(successful validtaion) then {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     *      else    {result: "failed", errors: [{error messages}]}
     */

    public function update_tag() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            //check if data is valid or not
            $this->form_validation->set_rules($this->tag->validation_rules['update']);

            if ($this->form_validation->run() === true) {
                $id = $this->input->get('id');

                $this->tag->model_data = $this->input->post(array('tag_name'));
                if ($this->tag->update($id)) {
                    $response['result'] = 'success';
                    $response['message'] = $this->lang->line('tag_updated');
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
        $id = $this->input->get('id');

        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $this->tag->model_data['tag_status'] = $this->input->post('tag_status');
            if ($this->tag->update($id)) {
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
     * To delete a tag
     * 
     * GET input given $id (id of the tag)
     * 
     * @return
     *      {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     */

    public function delete_tag() {
        $id = $this->input->get('id');

        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $this->user_tag->delete_where(array('tag_id' => $id));
            $this->tag->delete_where(array('tag_id' => $id), TABLE_OFFLINE_REQUEST_TAGS);
            $this->tag->delete_where(array('tag_id' => $id), TABLE_USER_TAGS);

            $this->chat_session->model_data['requested_tag'] = 0;
            $this->chat_session->update_where(array('requested_tag' => $id));

            if ($this->tag->delete_where(array('id' => $id))) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('tag_deleted');
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
