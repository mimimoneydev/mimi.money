<?php

/*
 * Controller to manage all users related actions
 * Author: Pukhraj Prajapat (pukhraj.prajapat@g-axon.com)
 */

class Feedbacks extends CP_AdminController {
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
        $this->addjs(theme_url("js/app/feedback.js"), TRUE);

        // get all agents with admin
        $agents = $this->user->get_all(array('role !=' => 'visitor'), "id,name");
        $lang_labels = $this->lang->load('feedback', '', TRUE);
        
        $this->add_js_inline(array('agents' => $agents, 'lang_labels' => $lang_labels, 'user' => $this->current_user, 'settings' => $this->settings));

        // add filter file.
        $this->data['filter_view'] = 'feedback/filters.php';

        //write code here to load view
        $this->bulid_layout("feedback/list");
    }

    /*
     * This function will load feedback.
     * 
     * @return $feddbacks json array
     */

    public function get_feddback_list() {
        $response = array('result' => 'success');
        
        if ($this->current_user) {
            $feddbacks = $this->feedback->get_feddbacks($this->input->post());
            $response['feddbacks'] = $feddbacks;
        } else {
            $response['result'] = 'failed';
            $response['error_type'] = 'login_session_expired';
            $response['errors'] = $this->lang->line('login_session_expired');
        }
        
        return $this->output->set_content_type('application/json')->set_output($this->return_json($response));
    }

    /*
     * To delete entry
     * 
     * @return
     *      {result: "success", message: "DYNAMIC MESSAGE AS PER LANG"}
     */

    public function delete() {
        $response = array('errors' => '', 'result' => 'failed');

        if ($this->current_user) {
            $fid = $this->input->get('id');

            if ($this->feedback->delete_where(array('id' => $fid))) {
                $response['result'] = 'success';
                $response['message'] = $this->lang->line('feedback_deleted');
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
