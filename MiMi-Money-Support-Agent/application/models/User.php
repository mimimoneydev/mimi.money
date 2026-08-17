<?php

class User extends CP_Model {

    public $validation_rules = array(
        // rules for user login
        'login' => array(
            array(
                'field' => 'email',
                'label' => 'Email',
                'rules' => 'required|valid_email'
            ),
            array(
                'field' => 'password',
                'label' => 'Password',
                'rules' => 'required'
            )
        ),
        // rules for insert new user
        'insert' => array(
            array(
                'field' => 'name',
                'label' => 'Name',
                'rules' => 'required'
            ),
            array(
                'field' => 'display_name',
                'label' => 'Display Name',
                'rules' => 'required'
            ),
            array(
                'field' => 'role',
                'label' => 'Role',
                'rules' => 'required'
            ),
            array(
                'field' => 'email',
                'label' => 'Email',
                'rules' => 'required|valid_email|callback_email_check'
            ),
            array(
                'field' => 'pass',
                'label' => 'Password',
                'rules' => 'required'
            ),
            array(
                'field' => 'confirm_pass',
                'label' => 'Confirm Password',
                'rules' => 'required|matches[pass]'
            )
        ),
        //rules to update user info
        'update' => array(
            array(
                'field' => 'name',
                'label' => 'Name',
                'rules' => 'required'
            ),
            array(
                'field' => 'display_name',
                'label' => 'Display Name',
                'rules' => 'required'
            ),
            array(
                'field' => 'email',
                'label' => 'Email',
                'rules' => 'required|valid_email'
            )
        ),
        //rules to update password of a user
        'update_password' => array(
            array(
                'field' => 'pass',
                'label' => 'Password',
                'rules' => 'required'
            ),
            array(
                'field' => 'confirm_pass',
                'label' => 'Confirm Password',
                'rules' => 'required|matches[pass]'
            )
        )
    );

    /*
     * Profile colors options
     */
    private $colors = array('#f16364', '#f58559', '#f9a43e', '#e4c62e', '#67bf74', '#59a2be', '#2093cd', '#ad62a7', '#805781', '#e57373', '#f06292', '#a1887f', '#ba68c8', '#9575cd', '#7986cb', '#64b5f6', '#4fc3f7', '#4dd0e1', '#4db6ac', '#81c784', '#aed581', '#ff8a65', '#d4e157', '#ffd54f', '#ffb74d', '#90a4ae');

    /*
     * Cunstruct function
     */

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_USERS;
    }

    /*
     * To save user's data
     */

    public function insert() {
        $user = $this->user->get_single(array('email' => $this->model_data['email']));
        if ($user and $user->id) {
            $this->model_data['user_status'] = 'active';
            if (!in_array($user->role, array('admin', 'agent'))) {
                $this->model_data['role'] = 'agent';
            }

            $this->update($user->id);

            return $user->id;
        }

        $this->model_data['created_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
        $this->model_data['name'] = ucfirst($this->model_data['name']);
        $this->model_data['display_name'] = ucfirst($this->model_data['display_name']);

        if (is_null($this->model_data['profile_color'])) {
            $this->model_data['profile_color'] = $this->colors[rand(0, 25)];
        }

        $this->db->insert($this->table, $this->model_data);
        return $this->db->insert_id();
    }

    /*
     * To update user's data     * 
     * 
     * @param: $id (id of the user)
     * 
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update($id) {
        $this->model_data['modified_at'] = date("Y-m-d H:i:s", now());
        if (isset($this->model_data['name'])) {
            $this->model_data['name'] = ucfirst($this->model_data['name']);
        }

        if (isset($this->model_data['display_name'])) {
            $this->model_data['display_name'] = ucfirst($this->model_data['display_name']);
        }

        if ($this->db->update($this->table, $this->model_data, array("id" => $id))) {
            return true;
        }

        return false;
    }

    /*
     * To delete user from database.
     * 
     * @param: $id (id of the user)
     * @return Boolean
     */
    function delete($id) {
        $query = $this->db->get_where($this->table, array('id' => $id));
        $user = $query->row();
        
        // deleting profile picture
        if($user->profile_pic) {
            $this->media->delete_media($user->profile_pic, PROFILE_PICS);
        }
        
        $tables = array(TABLE_USER_VISIT_INFO, TABLE_USER_ADDRESS, TABLE_USER_TAGS, TABLE_USERS_TO_SITES);
        $this->db->where('user_id', $user->id);
        $this->db->delete($tables);
        
        // deleting chat sessions
        $chat_sessions = $this->db->select('ch_sess.id')
                ->where('ch_user.user_id', $user->id)
                ->from(TABLE_CHAT_SESSIONS . ' ch_sess')
                ->join(TABLE_CHAT_USERS . ' ch_user', "ch_user.chat_session_id = ch_sess.id")
                ->group_by('ch_sess.id')
                ->get()
                ->result();
        
        foreach ($chat_sessions as $session) {
            $tables = array(TABLE_CHAT_MESSAGES, TABLE_FEEDBACK, TABLE_CHAT_USERS, TABLE_CHAT_REQUESTS);
            $this->db->where('chat_session_id', $session->id);
            $this->db->delete($tables);

            // deleting notifications
            $this->db->where('chat_session_id', $session->id);
            $this->db->where_in('notification_type', array('message', 'online_request'));
            $this->db->delete(TABLE_NOTIFICATIONS);
        
            $this->db->where('id', $session->id);
            $this->db->delete(TABLE_CHAT_SESSIONS);
        }
        
        // deleting offilne requests
        $this->db->where('visitor_id', $user->id);
        $this->db->delete(TABLE_OFFLINE_REQUESTS);
        
        $this->db->where('sender_id', $user->id);
        $this->db->delete(TABLE_OFFLINE_REQUEST_CONVERSATIONS);
        
        // deleting main user entry
        $this->db->where('id', $user->id);
        if ($this->db->delete($this->table)) {
            return true;
        }

        return false;
    }

    /*
     * To get user's data where condition match
     * 
     * @param: Array $where
     * @return Mixed $user or false;     * 
     */

    public function get_where($where = array()) {
        //pull data from users table and store in $user
        //pull data from user_tags table and store in $this->user->model_data['tags']
        $query = $this->db->select('id, name, display_name, email, profile_pic, profile_color, role, last_login, user_status, contact_number')
                ->get_where($this->table, $where);

        $user = $query->row();
        if ($user) {
            $user->tags = array();
            $user->sites = array();

            $tags_users = $this->db->select('tag_id')->where('user_id', $user->id)->get(TABLE_USER_TAGS)->result();
            foreach ($tags_users as $tag) {
                $user->tags[] = $tag->tag_id;
            }

            $users_to_sites = $this->db->select('site_id')->where('user_id', $user->id)->get(TABLE_USERS_TO_SITES)->result();
            foreach ($users_to_sites as $site) {
                $user->sites[] = $site->site_id;
            }

            return $user;
        }

        return false;
    }

    /*
     * To get user's values and will assign in model_data
     * 
     * @param: $id (id of the user)
     * 
     */

    public function get($id) {
        //pull data from users table and store in $this->user->model_data
        //pull data from user_tags table and store in $this->user->model_data['tags']
        $query = $this->db->select('id, name, display_name, email, profile_pic, profile_color, role, last_login, user_status, contact_number')
                ->where('id', $id)
                ->get($this->table);

        $this->model_data = $query->row_array();
        $this->model_data['tags'] = array();
        $this->model_data['sites'] = array();

        $tags_users = $this->db->select('tag_id')->where('user_id', $id)->get(TABLE_USER_TAGS)->result();
        foreach ($tags_users as $tag) {
            $this->model_data['tags'][] = $tag->tag_id;
        }

        $users_to_sites = $this->db->select('site_id')->where('user_id', $id)->get(TABLE_USERS_TO_SITES)->result();
        foreach ($users_to_sites as $site) {
            $this->model_data['sites'][] = $site->site_id;
        }
    }

    /*
     * To get all user's list with filter options applied
     * 
     * @param: array of filter options (role, keywords, tags (array)
     * 
     * @return:
     *      array of user objects (with tags)
     */

    public function get_users($filter_values = array()) {
        $user_ids = array();
        $tag_uids = array();
        $users = array();

        if (isset($filter_values['tags']) and $filter_values['tags']) {
            $tags_users = $this->db->select('user_id')->where_in('tag_id', $filter_values['tags'])->get('chatbull_user_tags')->result();
            foreach ($tags_users as $user)
                $tag_uids[$user->user_id] = $user->user_id;
        }

        $sql = "select id, name, email, profile_pic, profile_color, role, last_login, user_status from " . $this->table . " where user_status != 'deleted'";

        if (isset($filter_values['tags']) and $filter_values['tags']) {
            if (empty($tag_uids))
                return $users;

            $sql .= " and id in (" . implode(",", $tag_uids) . ") ";
        }

        if (isset($filter_values['keywords']) and $filter_values['keywords']) {
            $sql .= " and (email like '%" . $filter_values['keywords'] . "%' or name like '%" . $filter_values['keywords'] . "%' ";
            $sql .= " or display_name like '%" . $filter_values['keywords'] . "%' or contact_number like '%" . $filter_values['keywords'] . "%' )";
        }

        if (isset($filter_values['roles']) and $filter_values['roles']) {
            $sql .= " and role in ('" . implode("','", $filter_values['roles']) . "') ";
        }

        $sql .= " order by role asc limit " . $filter_values['offset'] . ", " . $this->item_per_page;
        $users = $this->db->query($sql)->result();

        if (count($users) > 0) {

            foreach ($users as $user) {
                $user->last_login_string = strtotime($user->last_login) * 1000;
                $user->profilePic = $this->media->get_thumbnail($user->profile_pic, PROFILE_PICS, $user->email);

                $user_ids[] = $user->id;
            }

            $tags = $this->db->select('utags.*, tags.tag_name')
                    ->where_in('utags.user_id', $user_ids)
                    ->from(TABLE_USER_TAGS . ' utags')
                    ->join(TABLE_TAGS . ' tags', 'tags.id = utags.tag_id')
                    ->get()
                    ->result();

            $user_tags = array();
            foreach ($tags as $tag) {
                $user_tags[$tag->user_id][] = $tag;
            }

            $sites = $this->db->select('usite.*, site.site_name')
                    ->where_in('usite.user_id', $user_ids)
                    ->from(TABLE_USERS_TO_SITES . ' usite')
                    ->join(TABLE_SITES . ' site', 'site.id = usite.site_id')
                    ->get()
                    ->result();

            $users_sites = array();
            foreach ($sites as $site) {
                $users_sites[$site->user_id][] = $site;
            }

            foreach ($users as $user) {
                $user->tags = array();
                if (isset($user_tags[$user->id])) {
                    $user->tags = $user_tags[$user->id];
                }

                $user->sites = array();
                if (isset($user_tags[$user->id])) {
                    $user->sites = $user_tags[$user->id];
                }
            }
        }

        return $users;
    }

    /*
     * This function will return all agents of a department.
     * 
     * @param $tag_id (department id)
     * 
     * @return $agents ;
     */

    function get_department_agents($tag_id) {
        $agents = $this->db->select(TABLE_USER_TAGS . '.user_id')
                ->where(TABLE_USER_TAGS . '.tag_id', $tag_id)
                ->where_in(TABLE_USERS . '.role', array('agent', 'admin'))
                ->where(TABLE_USERS . '.user_status', 'active')
                ->from(TABLE_USER_TAGS)
                ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_USER_TAGS . '.user_id')
                ->get()
                ->result();

        return $agents;
    }

    /*
     * Get monthly users
     * 
     * @param $month
     * @param $year
     * 
     * @return $totla_users;
     */

    function getUsersData() {
        $usersData = array();

        //$where = " MONTH(created_at) = '" . $month . "' and YEAR(created_at) = '" . $year . "' and role = 'visitor'";
        $results = $this->db->select("count(*) as total, DATE_FORMAT(`created_at`, '%Y-%m-%d') AS date_formatted")
                ->where('role', 'visitor')
                ->group_by('date_formatted')
                ->order_by('created_at', 'asc')
                ->get(TABLE_USERS)
                ->result();

        foreach ($results as $row) {
            $usersData[$row->date_formatted] = $row->total;
        }

        return $usersData;
    }

    /*
     * Get monthly users
     * 
     * @param Int $site_id Default 0
     * @param Query $where Default ''
     * 
     * @return $totla_users;
     */

    function getRequestsData($site_id = 0, $where = '') {
        $requestsData = array();

        $this->db->select("count(*) as total, DATE_FORMAT(`started_at`, '%Y-%m-%d') AS date_formatted");

        if ($site_id) {
            $this->db->where('ch_sess.site_id', $site_id);
        }

        if ($where) {
            $this->db->where($where);
        }

        $results = $this->db->where('user_role', 'visitor')
                //->where(" DATE_FORMAT(`started_at`, '%Y-%m') = '2017-08' ")
                ->from(TABLE_CHAT_USERS . ' ch_user')
                ->join(TABLE_CHAT_SESSIONS . ' ch_sess', 'ch_sess.id = ch_user.chat_session_id')
                ->group_by('date_formatted')
                ->order_by('started_at', 'asc')
                ->get()
                ->result();

        foreach ($results as $row) {
            $requestsData[$row->date_formatted] = $row->total;
        }

        return $requestsData;
    }

    /*
     * This function will return visitors address data to populate map on dashboard.
     * 
     * @param Int $site_id Default 0
     * @return $users;
     */

    function getVisitorsAddress($site_id = 0) {
        $select = TABLE_USER_ADDRESS . '.id, '
                . TABLE_USER_ADDRESS . '.user_id, '
                . TABLE_USER_ADDRESS . '.city, '
                . TABLE_USER_ADDRESS . '.state, '
                . TABLE_USER_ADDRESS . '.country, '
                . TABLE_USER_ADDRESS . '.latitude, '
                . TABLE_USER_ADDRESS . '.longitude, '
                . 'user.name, '
                . 'user.email, '
                . 'user.pass, '
                . 'user.profile_pic, '
                . 'user.profile_color, '
                . "CASE WHEN user.profile_pic != '' THEN CONCAT('" . base_url(UPLOAD_DIR . PROFILE_PICS) . "thumb/', user.profile_pic) END  as userProfilePic ";

        $this->db->select($select);

        if ($site_id) {
            $this->db->where('site_id', $site_id);
        }

        $users = $this->db->from(TABLE_USER_ADDRESS)
                ->join(TABLE_USERS . ' user', 'user.id = ' . TABLE_USER_ADDRESS . '.user_id')
                ->get()
                ->result();
        return $users;
    }

    /*
     * This function will return hits of visitors
     * 
     * @param Int $site_id Default 0
     * @param Query $where Default ''
     */

    function get_visitors($site_id = 0, $where = '') {
        $visitorsData = array();
        $this->db->select("sum(visitors) as total_visitors, created_at");

        if ($site_id) {
            $this->db->where('site_id', $site_id);
        }

        if ($where) {
            $this->db->where($where);
        }

        $results = $this->db->from(TABLE_DAILY_VISITORS)
                ->group_by('created_at')
                ->order_by('created_at', 'asc')
                ->get()
                ->result();

        foreach ($results as $row) {
            $visitorsData[$row->created_at] = $row->total_visitors;
        }

        return $visitorsData;
    }

    /*
     * This function will return highes page views
     * 
     * @param Int $site_id Default 0
     * @param $limit (dfault 5)
     */

    function get_pagevies($site_id = 0, $limit = 5) {
        $this->db->select("sum(counter) as total, page_url, page_title");

        if ($site_id) {
            $this->db->where('site_id', $site_id);
        }

        $results = $this->db->from(TABLE_DAILY_PAGEVIEWS)
                ->group_by('page_url')
                ->order_by('total', 'desc')
                ->limit($limit)
                ->get()
                ->result();

        return $results;
    }

}
