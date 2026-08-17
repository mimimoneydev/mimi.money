<?php

/**
 * Created by Abderrahim El imame.
 * Email : abderrahim.elimame@gmail.com
 * Date: 05/03/2016
 * Time: 23:28
 */
class GroupsController
{


    public $_GB;

    public function __construct($_GB)
    {
        $this->_GB = $_GB;
    }
    /**
     *  Function to get groupInfo
     * @param $groupID
     */
    public function getGroupInfo($groupID,$userID)
    {

        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = $this->_GB->_DB->select('groups', '*', "`id` = '{$groupID}' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);

            $fetch['id'] = (empty($fetch['id'])) ? null : $fetch['id'];
            $fetch['GroupName'] = (empty($fetch['name'])) ? null : $fetch['name'];
            $fetch['GroupImage'] = (empty($fetch['image'])) ? null : $fetch['image'];
            $fetch['CreatedDate'] = (empty($fetch['date'])) ? null : $fetch['date'];
            $fetch['CreatorID'] = $this->getUserIDByGroupID($fetch['id']);
            $fetch['Creator'] = $this->getCreatorName($fetch['id']);
           $fetch['isAdmin'] = $this->isAdmin($fetch['id'], $userID);
           $fetch['isDeleted'] = $this->isDeleted($fetch['id'], $userID);
           $fetch['isLeft'] = $this->isLeft($fetch['id'], $userID);
            $fetch['Members'] = $this->GetFirstGroupMembers($fetch['id']);
            unset ($fetch['date'], $fetch['userID'], $fetch['image'], $fetch['name'],$fetch['notification_key']);
            $this->_GB->Json($fetch);

        } else {
            $this->_GB->Json(null);
        }
        $this->_GB->_DB->free($query);
    }
    /**
     * Function to get  groups conversations
     * @param $userId
     */
    public function getGroups($userId)
    {
        $userId = $this->_GB->_DB->escapeString($userId);

        $query = "SELECT G.id , G.date AS CreatedDate,
                                  G.name AS GroupName ,
                                  G.image AS GroupImage
                          FROM prefix_users U,prefix_groups G,prefix_group_members GM
                          WHERE
                          CASE
                          WHEN GM.userID = U.id
                          THEN GM.userID = {$userId}
                          END
                          AND GM.groupID = G.id
                           AND U.is_activated = '1'
                           AND (GM.role = 'admin' OR GM.role = 'member')
                          GROUP BY G.id  ORDER BY G.id ASC ";
        $query = $this->_GB->_DB->MySQL_Query($query);
        $conversations = array();
        if ($this->_GB->_DB->numRows($query) != 0) {
            while ($fetch = $this->_GB->_DB->fetchAssoc($query)) {
                $fetch['id'] = (empty($fetch['id'])) ? null : $fetch['id'];
                $fetch['CreatorID'] = $this->getUserIDByGroupID($fetch['id']);
                $fetch['Creator'] = $this->getCreatorName($fetch['id']);
                $fetch['isDeleted'] = $this->isDeleted($fetch['id'], $userId);
                $fetch['isLeft'] = $this->isLeft($fetch['id'], $userId);
                $fetch['isAdmin'] = $this->isAdmin($fetch['id'], $userId);
                $fetch['Members'] = $this->GetFirstGroupMembers($fetch['id']);
                $conversations[] = $fetch;
            }
            $this->_GB->Json($conversations);
        } else {

            $this->_GB->Json($conversations);
        }
        $this->_GB->_DB->free($query);
    }



    /**
     * Function to get groups members
     * @param $groupID
     * @return array
     *
     */
    public function GetFirstGroupMembers($groupID)
    {
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = " SELECT  GM.id ,
                           GM.role,
                           GM.groupID,
                           GM.isLeft,
                           GM.Deleted,
                           U.id AS userId,
                           U.username,
                           U.wallet_address,
                           U.image,
                           U.status,
                           U.status_date,
                           U.is_activated ,
                           U.registered_id
                             FROM prefix_users U,prefix_groups G,prefix_group_members GM
                             WHERE
                             CASE
                             WHEN GM.userID = U.id
                             THEN GM.groupID = G.id
                              END
                              AND
                              G.id = {$groupID}
                              AND
                              U.is_activated = 1
                             ORDER BY U.id ASC ";
        $query = $this->_GB->_DB->MySQL_Query($query);
        if ($this->_GB->_DB->numRows($query) != 0) {
            $GroupMembers = array();
            while ($fetch = $this->_GB->_DB->fetchAssoc($query)) {
                $fetch['username'] = (empty($fetch['username'])) ? null : $fetch['username'];
                $fetch['Linked'] = true;
                $fetch['Deleted'] = $this->isDeleted($fetch['groupID'], $fetch['userId']);
                $fetch['isLeft'] = $this->isLeft($fetch['groupID'], $fetch['userId']);
                $fetch['isAdmin'] = $this->isAdmin($fetch['groupID'], $fetch['userId']);
                $fetch['status_date'] = (empty($fetch['status_date'])) ? null : $this->_GB->Date($fetch['status_date']);
                unset ($fetch['created_at'], $fetch['auth_token'], $fetch['is_activated']);
                $GroupMembers[] = $fetch;
            }
            $this->_GB->_DB->free($query);
            return $GroupMembers;
        } else {
            $this->_GB->_DB->free($query);
            return $this->_GB->Json(array('groupsMembers' => null));
        }
    }


    /**
     *  Function to check if member is left
     * @param $groupID
     * @param $userID
     * @return bool
     */
    public function isLeft($groupID, $userID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = $this->_GB->_DB->select('group_members', 'isLeft', "  `groupID`= '{$groupID}' AND `userID`= '{$userID}' AND `isLeft`= '1' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }

    }

    /**
     *  Function to check if member is deleted
     * @param $groupID
     * @param $userID
     * @return bool
     */
    public function isDeleted($groupID, $userID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = $this->_GB->_DB->select('group_members', 'Deleted', "  `groupID`= '{$groupID}' AND `userID`= '{$userID}' AND `Deleted`= '1' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }

    }

    /**
     *  Function to check if a member is an admin
     * @param $groupID
     * @param $userID
     * @return bool
     */
    public function isAdmin($groupID, $userID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);
        $groupID = $this->_GB->_DB->escapeString($groupID);
        $query = $this->_GB->_DB->select('group_members', 'userID', "  `groupID`= '{$groupID}' AND `userID`= '{$userID}' AND `role`= 'admin' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }

    }


    /**
     *  Function to get user id by group id
     * @param $groupID
     * @return int
     */
    public function getUserIDByGroupID($groupID)
    {
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = $this->_GB->_DB->select('groups', 'userID', "`id`= '{$groupID}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['userID'];
        } else {
            $this->_GB->_DB->free($query);
            return 0;
        }
    }

    /**
     *  Function to get creator name
     * @param $groupID
     * @return null
     */
    public function getCreatorName($groupID)
    {
        $groupID = $this->_GB->_DB->escapeString($groupID);
        $id = $this->getUserIDByGroupID($groupID);
        $query = $this->_GB->_DB->select('users', '*', "`id`= '{$id}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['wallet_address'];
        } else {
            $this->_GB->_DB->free($query);
            return null;
        }
    }

    /**
     *  Function to create a group
     * @param $name
     * @param $image
     * @param $userId
     * @param $ids
     * @param $date
     */
    public function createGroup($name, $image, $userId, $ids, $date, $notificationKey = null)
    {

        // $userId = $this->_GB->_DB->escapeString($userId);
        $date = $this->_GB->_DB->escapeString($date);
        $name = $this->_GB->_DB->escapeString($name);


        if (strpos($name, '\'') !== false) {
            $name = str_replace('\'', "\\'", $name);
        }

        $notificationKey = $notificationKey ? $this->_GB->_DB->escapeString($notificationKey) : $this->randomString();


        $createGroup = array(
            'name' => $name,
            'image' => $image,
            'userID' => $userId,
            'notification_key' => $notificationKey,
            'date' => $date
        );
        $insert = $this->_GB->_DB->insert('groups', $createGroup);
        $groupID = $this->_GB->_DB->last_Id();
        if ($insert) {
            substr($ids, 0, 0);
            $array = explode(",", $ids);
            $in = true;
            foreach ($array as $id) {

                if ($userId == $id) {
                    $addMembersToGroup = array(
                        'groupID' => $groupID,
                        'userID' => $id,
                        'role' => "admin",
                        'Deleted' => 0

                    );
                } else {
                    $addMembersToGroup = array(
                        'groupID' => $groupID,
                        'userID' => $id,
                        'role' => "member",
                        'Deleted' => 0

                    );
                }

                if (!$this->UserExist($id, $groupID)) {
                    $insertMembers = $this->_GB->_DB->insert('group_members', $addMembersToGroup);
                    if (!$insertMembers) {
                        $in = false;
                        break;
                    }
                }

            }
            $membersGroupModels = $this->GetFirstGroupMembers($groupID);

            if ($in) {
                $this->_GB->Json(array(
                  'success' => true,
                  'message' => 'group has been created successfully',
                  'groupID' => $groupID,
                  'membersGroupModels' => $membersGroupModels,
                  'groupImage' => $image));

            } else {
                $this->_GB->Json(array('success' => false,
                    'message' => 'Oops  something went wrong',
                    'groupID' => null,
                    'membersGroupModels' => null,
                    'groupImage' => null));
            }
        } else {
            $this->_GB->Json(array('success' => false,
                'message' => 'Failed to create this group',
                'groupID' => null,
                'membersGroupModels' => null,
                'groupImage' => null));
        }
    }

  public function randomString($length = 30) {
	$str = "";
	$characters = array_merge(range('A','Z'), range('a','z'), range('0','9'));
	$max = count($characters) - 1;
	for ($i = 0; $i < $length; $i++) {
		$rand = mt_rand(0, $max);
		$str .= $characters[$rand];
	}
	return $str;
}

    /**
     *  Function to check if member exist
     * @param $userID
     * @param $groupID
     * @return bool
     */
    public function UserExist($userID, $groupID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = $this->_GB->_DB->select('group_members', '`userID`', "`userID` = '{$userID}' AND `groupID` = '{$groupID}' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }
    }

    /**
     *  Function to check if a group exist
     * @param $groupID
     * @return bool
     */
    public function GroupExist($groupID)
    {
        $groupID = $this->_GB->_DB->escapeString($groupID);
        $query = $this->_GB->_DB->select('groups', '`id`', "`id` = '{$groupID}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }
    }


    /**
     *  Function to add new member to group
     * @param $groupID
     * @param $id
     */
    public function addMembersToGroup($groupID, $id)
    {

        $id = $this->_GB->_DB->escapeString($id);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        if ($this->UserExist($id, $groupID)) {
            if ($this->isDeleted($groupID, $id) || $this->isLeft($groupID, $id)) {
                $deleted = 0;
                $left = 0;
                $role = "member";
                $fields = "`Deleted` = '" . $deleted . "'";
                $fields .= ",`isLeft` = '" . $left . "'";
                $fields .= ",`role` = '" . $role . "'";
                $update = $this->_GB->_DB->update('group_members', $fields, " `userID`= '{$id}' AND `groupID`= '{$groupID}'");
                if ($update) {
                    $query = $this->_GB->_DB->select('groups', '*', "`id` = '{$groupID}' ");
                    if ($this->_GB->_DB->numRows($query) != 0) {
                        $fetch = $this->_GB->_DB->fetchAssoc($query);
                        $this->_GB->pushToSocket('socket_group_member_added', array(
                            'groupID' => $groupID,
                            'userID' => $id
                        ), null, $groupID);


                        $this->_GB->Json(array('success' => true,
                            'message' => 'Member(s) has  been added successfully',
                            'groupID' => $groupID,
                            'groupImage' => null));
                    }
                    $this->_GB->_DB->free($query);
                } else {
                    $this->_GB->Json(array('success' => false,
                        'message' => 'Failed Something went wrong',
                        'groupID' => null,
                        'groupImage' => null));
                }

            } else {
                $this->_GB->Json(array('success' => false,
                    'message' => 'This member is already exist',
                    'groupID' => null,
                    'groupImage' => null));
            }
        } else {
            $addMembersToGroup = array(
                'groupID' => $groupID,
                'userID' => $id,
                'role' => "member"

            );
            $insert = $this->_GB->_DB->insert('group_members', $addMembersToGroup);
            if (!$insert) {
                $this->_GB->Json(array('success' => false,
                    'message' => 'Failed Something went wrong',
                    'groupID' => null,
                    'groupImage' => null));
            } else {

                $query = $this->_GB->_DB->select('groups', '*', "`id` = '{$groupID}' ");
                if ($this->_GB->_DB->numRows($query) != 0) {
                    $fetch = $this->_GB->_DB->fetchAssoc($query);
                    $this->_GB->pushToSocket('socket_group_member_added', array(
                        'groupID' => $groupID,
                        'userID' => $id
                    ), null, $groupID);
                }
                $this->_GB->_DB->free($query);

                $this->_GB->Json(array('success' => true,
                    'message' => 'Member(s) has  been added successfully',
                    'groupID' => $groupID,
                    'groupImage' => null));

            }
        }
    }

    /**
     *  Function to user registerId
     * @param $userID
     * @return null
     */
    public function getUserRegisterID($userID)
    {
        $query = $this->_GB->_DB->select('users', '`registered_id`', "`id` = '{$userID}' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['registered_id'];
        } else {
            $this->_GB->_DB->free($query);
            return null;
        }
    }


    /**
     *  Function to remove member from group
     * @param $groupID
     * @param $id
     */
    public function removeMemberFromGroup($groupID, $id)
    {

        $id = $this->_GB->_DB->escapeString($id);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $fields = "`Deleted` = '" . 1 . "'";
        $fields .= ",`isLeft` = '" . 1 . "'";
        $delete = $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$id}");
        if ($delete) {

            $query = $this->_GB->_DB->select('groups', '*', "`id` = '{$groupID}' ");
            if ($this->_GB->_DB->numRows($query) != 0) {
                $fetch = $this->_GB->_DB->fetchAssoc($query);
                $this->_GB->pushToSocket('socket_group_member_removed', array(
                    'groupID' => $groupID,
                    'userID' => $id
                ), null, $groupID);

                $this->_GB->Json(array('success' => true,
                    'message' => 'The member has  been deleted successfully',
                    'groupID' => $groupID,
                    'groupImage' => null));
            }
            $this->_GB->_DB->free($query);
        } else {
            $this->_GB->Json(array('success' => false,
                'message' => 'Failed to delete this member please try again later',
                'groupID' => null,
                'groupImage' => null));
        }

    }

    /**
     *  Function to make a member as an admin
     * @param $groupID
     * @param $id
     */
    public function makeMemberAdmin($groupID, $id)
    {

        $id = $this->_GB->_DB->escapeString($id);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $role = "admin";
        $fields = "`role` = '" . $role . "'";
        $update = $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$id}");
        if (!$update) {
            $this->_GB->Json(array('success' => false,
                'message' => 'Failed Something went wrong',
                'groupID' => null,
                'groupImage' => null));
        } else {
            $this->_GB->Json(array('success' => true,
                'message' => 'Member has  been making as admin successfully',
                'groupID' => $groupID,
                'groupImage' => null));

        }

    }

    /**
     *  Function to make an admin as a member
     * @param $groupID
     * @param $id
     */
    public function makeAdminMember($groupID, $id)
    {

        $id = $this->_GB->_DB->escapeString($id);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $role = "member";
        $fields = "`role` = '" . $role . "'";
        $update = $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$id}");
        if (!$update) {
            $this->_GB->Json(array('success' => false,
                'message' => 'Failed Something went wrong',
                'groupID' => null,
                'groupImage' => null));
        } else {
            $this->_GB->Json(array('success' => true,
                'message' => 'Admin has  been making as member successfully',
                'groupID' => $groupID,
                'groupImage' => null));

        }

    }




    /**
     *  Function to get group members list
     * @param $query
     */
    public function GetGroupMembers($query)
    {
        if ($this->_GB->_DB->numRows($query) != 0) {
            $GroupMembers = array();
            while ($fetch = $this->_GB->_DB->fetchAssoc($query)) {
                $fetch['username'] = (empty($fetch['username'])) ? null : $fetch['username'];
                $fetch['Linked'] = true;
                $fetch['Deleted'] = $this->isDeleted($fetch['groupID'], $fetch['userId']);
                $fetch['isLeft'] = $this->isLeft($fetch['groupID'], $fetch['userId']);
                $fetch['isAdmin'] = $this->isAdmin($fetch['groupID'], $fetch['userId']);
                $fetch['status_date'] = (empty($fetch['status_date'])) ? null : $this->_GB->Date($fetch['status_date']);
                unset ($fetch['created_at'], $fetch['auth_token']);
                $GroupMembers[] = $fetch;
            }
            $this->_GB->Json($GroupMembers);
        } else {
            $this->_GB->Json(null);
        }
        $this->_GB->_DB->free($query);
    }

    /**
     *  Function to exit a group
     * @param $userID
     * @param $groupID
     */
    public function exitGroup($userID, $groupID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $query = $this->_GB->_DB->select('group_members', '`userID`', "`groupID` = '{$groupID}' ");
        $GroupMembers = array();
        if ($this->_GB->_DB->numRows($query) != 0) {
            while ($fetch = $this->_GB->_DB->fetchAssoc($query)) {
                $GroupMembers[] = $fetch;
            }
        }
        $this->_GB->_DB->free($query);
        $generatedID = $this->generateRandomID($GroupMembers);

        if (!$this->isLeft($groupID, $userID)) {
          if ($this->isAdmin($groupID, $userID)) {
            if ($generatedID != 0) {
                $role1 = "member";
                $fields = "`isLeft` = '" . 1 . "'";
                $fields .= ",`role` = '" . $role1 . "'";
                $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$userID}");

                $role = "admin";
                $fields = "`role` = '" . $role . "'";
                $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$generatedID}");

                $query = $this->_GB->_DB->select('groups', '*', "`id` = '{$groupID}' ");
                if ($this->_GB->_DB->numRows($query) != 0) {
                    $fetch = $this->_GB->_DB->fetchAssoc($query);
                    $this->_GB->pushToSocket('socket_group_member_left', array(
                        'groupID' => $groupID,
                        'userID' => $userID
                    ), null, $groupID);
                }
                $array = array(
                    'success' => true,
                    'message' => 'You left  this group successfully'
                );
                $this->_GB->Json($array);

            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Failed to left this group try again later'
                );
                $this->_GB->Json($array);
            }
          }else{
            $role1 = "member";
            $fields = "`isLeft` = '" . 1 . "'";
            $fields .= ",`role` = '" . $role1 . "'";
            $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$userID}");


            $query = $this->_GB->_DB->select('groups', '*', "`id` = '{$groupID}' ");
            if ($this->_GB->_DB->numRows($query) != 0) {
                $fetch = $this->_GB->_DB->fetchAssoc($query);
                $this->_GB->pushToSocket('socket_group_member_left', array(
                    'groupID' => $groupID,
                    'userID' => $userID
                ), null, $groupID);
            }
            $array = array(
                'success' => true,
                'message' => 'You left  this group successfully'
            );
            $this->_GB->Json($array);
          }
        } else {
            $array = array(
                'success' => false,
                'message' => 'You are already left this group'
            );
            $this->_GB->Json($array);
        }

    }

    /**
     *  Function to edit group name
     * @param $name
     * @param $groupID
     */
    public function EditGroupName($name, $groupID)
    {

        $name = $this->_GB->_DB->escapeString($name);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        if (strpos($name, '\'') !== false) {
            $name = str_replace('\'', "\\'", $name);
        }
        $fields = "`name` = '" . $name . "'";
        $result = $this->_GB->_DB->update('groups', $fields, "`id` = '{$groupID}' ");

        // check if row inserted or not
        if ($result) {
            $array = array(
                'success' => true,
                'message' => 'group Name  is updated successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to update the group name '
            );
            $this->_GB->Json($array);
        }
    }

    /**
     *  Function to get user name by id
     * @param $userID
     * @return null
     */
    public function getUserNameByID($userID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);

        $query = $this->_GB->_DB->select('users', '`username`,`wallet_address`', "`id`= '{$userID} '");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            if ($fetch['username'] != null) {
                $this->_GB->_DB->free($query);
                return $fetch['username'];
            } else {
                $this->_GB->_DB->free($query);
                return $fetch['wallet_address'];
            }

        } else {
            return null;
        }
    }

    /**
     *  Function to get user phone (wallet_address) by id
     * @param $userID
     * @return null
     */
    public function getUserPhoneByID($userID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);

        $query = $this->_GB->_DB->select('users', '`wallet_address`', "`id`= '{$userID} '");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['wallet_address'];
        } else {
            $this->_GB->_DB->free($query);
            return null;
        }
    }

    /**
     *  Function to get user wallet address by id
     * @param $userID
     * @return null
     */
    public function getUserWalletAddressByID($userID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);

        $query = $this->_GB->_DB->select('users', '`wallet_address`', "`id`= '{$userID} '");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['wallet_address'];
        } else {
            $this->_GB->_DB->free($query);
            return null;
        }
    }

    /**
     *  Function to delete group
     * @param $userID
     * @param $groupID
     */
    public function deleteGroup($userID, $groupID)
    {

        $userID = $this->_GB->_DB->escapeString($userID);
        $groupID = $this->_GB->_DB->escapeString($groupID);

        $fields = "`Deleted` = '" . 1 . "'";
        $delete = $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$groupID}' AND `userID` = {$userID}");
        if ($delete) {
            $array = array(
                'success' => true,
                'message' => 'This group is deleted successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to delete this group '
            );
            $this->_GB->Json($array);
        }

    }

    /**
     *  Function to generate a random id
     * @param $GroupMembers
     * @return mixed
     */
    public function generateRandomID($GroupMembers)
    {
        $randIndex = array_rand($GroupMembers);
        $current = current($GroupMembers[$randIndex]);
        return $current;

    }

        public function addSingleMemberLogin($userId)
        {
            $userId = $this->_GB->_DB->escapeString($userId);
            $isleft = 0;
            $isdeleted = 0;
            $IDResult = $this->_GB->_DB->select('group_members', '`groupID`', "  `userID` = '{$userId}' AND `isLeft` = '{$isleft}' AND `Deleted` = '{$isdeleted}'");
            if ($this->_GB->_DB->numRows($IDResult) > 0) {
                while ($fetch = $this->_GB->_DB->fetchAssoc($IDResult)) {
                    $this->_GB->pushToSocket('socket_group_rejoin', array(
                        'groupID' => $fetch['groupID'],
                        'userID' => $userId
                    ), null, $fetch['groupID']);
                }
            }
        }

}
