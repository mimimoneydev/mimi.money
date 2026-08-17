<?php
class User_tag extends CP_Model {
    
    public function __construct() {
        parent::__construct();
        $this->table = TABLE_USER_TAGS;
    }
    
    /*
     * To save tags associated with a user
     * 
     * @param: $user_id (id of the user to associate the tags with)
     * 
     */
    public function save($user_id) {
        $this->db->delete($this->table, array('user_id' => $user_id));
        
        if($this->model_data['tags'] and is_array($this->model_data['tags'])){
            $insertData = array();
            foreach ($this->model_data['tags'] as $tag_id){
                if($tag_id){
                    $insertData[] = array('user_id' => $user_id, 'tag_id' => $tag_id);
                }
            }

            if(count($insertData) > 0){
                $this->db->insert_batch($this->table, $insertData);
            }
        }
    }
    
    /*
     * To pull all tags along with their tag name associated with a user
     * 
     * @param: $user_id (id of the user tags are associated with)
     * 
     * @return 
     *      array of objects (tag_id, tag_name)
     */
    public function get_user_tags($user_id) {
        $tags = $this->db->select('utags.*, tags.tag_name')
                    ->where('utags.user_id', $user_id)
                    ->from(TABLE_USER_TAGS . ' utags')
                    ->join(TABLE_TAGS . ' tags', 'tags.id = utags.tag_id')
                    ->get()
                    ->result();
        
        return $tags;
    }
}