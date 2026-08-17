<?php

class Tag extends CP_Model {

    public $table = "";
    public $validation_rules = array(
        "insert" => array(
            array(
                'field' => 'tag_name',
                'label' => 'Tag',
                'rules' => 'required'
            )
        ),
        "update" => array(
            array(
                "field" => "tag_name",
                "label" => "Tag Name",
                "rules" => "required"
            )
        )
    );

    public function __construct() {
        parent::__construct();
        $this->table = TABLE_TAGS;
    }

    /*
     * To save tag's data
     */

    public function insert() {
        $this->db->insert($this->table, $this->model_data);
        return $this->db->insert_id();
    }

    /*
     * To update tag's data     * 
     * 
     * @param: $id (id of the tag)
     * 
     * @return
     *      if (successful entry done) then true
     *      else false
     */

    public function update($id) {
        if ($this->db->update($this->table, $this->model_data, array("id" => $id))) {
            return true;
        }

        return false;
    }

    /*
     * This function will be call to fetch tags.
     * 
     * $param $offset
     * $return $tags array of objects
     */

    public function get_tags($offset) {
        $tags_ids = array();
        $tag_agents = array();

        $query = $this->db->select('*')
                ->limit($this->item_per_page, $offset)
                ->order_by('tag_name')
                ->get($this->table);

        $tags = $query->result();

        if (count($tags) > 0) {
            foreach ($tags as $tag) {
                $tags_ids[] = $tag->id;
            }

            $agents = $this->db->select(TABLE_USER_TAGS . '.tag_id, ' . TABLE_USERS . '.name, ' . TABLE_USERS . '.profile_color')
                    ->where_in(TABLE_USER_TAGS . '.tag_id', $tags_ids)
                    ->from(TABLE_USER_TAGS)
                    ->join(TABLE_USERS, TABLE_USERS . '.id = ' . TABLE_USER_TAGS . '.user_id')
                    ->get()
                    ->result();

            foreach ($agents as $agent) {
                $tag_agents[$agent->tag_id][] = $agent;
            }

            foreach ($tags as $tag) {
                $tag->agents = array();
                if (isset($tag_agents[$tag->id])) {
                    $tag->agents = $tag_agents[$tag->id];
                }
            }
        }

        return $tags;
    }

}
