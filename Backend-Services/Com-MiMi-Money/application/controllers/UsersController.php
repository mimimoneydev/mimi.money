<?php

/**
 * Created by Abderrahim El imame.
 * Email : abderrahim.elimame@gmail.com
 * Date: 19/02/2016
 * Time: 22:47
 */

class UsersController
{

    public $_GB;
    public $Groups;
    public $_Group;
    public $_Groups;
    public $_Messages;

    public function __construct($_GB, $Groups)
    {
        $this->_GB = $_GB;
        $this->_Group = $Groups;
        $this->_Groups = $Groups;
        $this->_Messages = null;
    }

    public function setMessagesController($Messages)
    {
        $this->_Messages = $Messages;
    }


    public function SignIn($array)
    {
        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }

        $walletAddress = !empty($array['walletAddress']) ? $array['walletAddress'] : (isset($array['mobile']) ? $array['mobile'] : null);
        $countryName = isset($array['country']) ? $array['country'] : null;

        $code = rand(100000, 999999);
        $res = $this->createUser($walletAddress, $code, $countryName);
        return $res;
    }


    public function createUser($walletAddress, $code, $countryName)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        $countryName = $this->_GB->_DB->escapeString($countryName);
        $isWalletUser = $this->isWalletCountry($countryName);

        $app_name = $this->_GB->getSettings('app_name');
        $smsVerification = $this->_GB->getSettings('sms_verification');
        $smsVerification = false;
        if (!$this->UserExist($walletAddress)) {
            $auth_token = $this->generateApiKey($walletAddress);

            $effectiveCountry = $isWalletUser ? 'WALLET' : $countryName;

            $tempUsername = 'User' . substr($walletAddress, 2, 8);

            $arrayData = array(
                'wallet_address' => $walletAddress,
                'auth_token' => $auth_token,
                'status' => 'Hey, I am using ' . $app_name . '. Enjoy it.',
                'status_date' => time(),
                'country' => $effectiveCountry,
                'is_activated' => 1,
                'has_backup' => 0,
                'backup_hash' => null,
                'username' => $tempUsername,
                'image' => null,
                'registered_id' => null

            );
            $result = $this->_GB->_DB->insert('users', $arrayData);
            $newUserID = $this->_GB->_DB->last_Id();
            $this->insertDefaultStatus($newUserID);
            if ($result) {
                $IDResult = $this->_GB->_DB->select('users', '*', "  `wallet_address` = '{$walletAddress}'");
                if ($this->_GB->_DB->numRows($IDResult) > 0) {
                    $fetch = $this->_GB->_DB->fetchAssoc($IDResult);
                    $this->_GB->_DB->free($IDResult);
                    $needsProfile = empty($fetch['username']) || strpos($fetch['username'], 'User') === 0;
                    $this->resolvePendingWalletMessagesForUser($fetch);
                    return $this->buildWalletJoinResponse($fetch, 'Wallet account has been created successfully.', $needsProfile);
                }

            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Sorry! Error occurred in registration .',
                    'walletAddress' => null,
                    'smsVerification' => $smsVerification,
                    'code' => null,
                    'hasBackup' => false
                );
                return $array;

            }
        } else if ($this->UserExist($walletAddress)) {
            $auth_token = $this->generateApiKey($walletAddress);

            $fields = "`auth_token` = '" . $auth_token . "'";
            $fields .= ",`is_activated` = 1";
            if (!$isWalletUser) {
                $fields .= ",`country` = '" . $countryName . "'";
            }

            $result = $this->_GB->_DB->update('users', $fields, "`wallet_address` = '{$walletAddress}'");

            if ($result) {
                $IDResult = $this->_GB->_DB->select('users', '*', "  `wallet_address` = '{$walletAddress}'");
                if ($this->_GB->_DB->numRows($IDResult) > 0) {
                    $fetch = $this->_GB->_DB->fetchAssoc($IDResult);
                    $this->_GB->_DB->free($IDResult);
                    $needsProfile = empty($fetch['username']) || empty($fetch['image']) || strpos($fetch['username'], 'User') === 0;
                    $this->resolvePendingWalletMessagesForUser($fetch);
                    return $this->buildWalletJoinResponse($fetch, 'Wallet login successful.', $needsProfile);
                }

            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Sorry! Error occurred in registration.',
                    'walletAddress' => null,
                    'smsVerification' => $smsVerification,
                    'code' => null,
                    'hasBackup' => false
                );
                return $array;

            }
        } else {
            $array = array(
                'success' => false,
                'message' => 'Sorry! wallet address is not valid or missing.',
                'walletAddress' => null,
                'smsVerification' => $smsVerification,
                'code' => null,
                'hasBackup' => false
            );
            return $array;
        }
    }

    private function isWalletCountry($countryName)
    {
        return strtoupper((string)$countryName) === 'WALLET';
    }

    private function getUserByIdentity($walletAddress, $country = null)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        $where = "`wallet_address` = '{$walletAddress}'";

        if ($country !== null) {
            $country = $this->_GB->_DB->escapeString($country);
            $where .= " AND `country` = '{$country}'";
        }

        $query = $this->_GB->_DB->select('users', '*', $where, '', '1');
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch;
        }

        $this->_GB->_DB->free($query);
        return null;
    }

    private function getStoredLabel($ownerUserID, $targetUserID)
    {
        $ownerUserID = $this->_GB->_DB->escapeString($ownerUserID);
        $targetUserID = $this->_GB->_DB->escapeString($targetUserID);

        if (empty($ownerUserID) || empty($targetUserID)) {
            return null;
        }

        $query = $this->_GB->_DB->select('user_labels', 'label', "`owner_user_id` = '{$ownerUserID}' AND `target_user_id` = '{$targetUserID}'", '', '1');
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return empty($fetch['label']) ? null : $fetch['label'];
        }

        $this->_GB->_DB->free($query);
        return null;
    }

    private function upsertStoredLabel($ownerUserID, $targetUserID, $label)
    {
        $ownerUserID = $this->_GB->_DB->escapeString($ownerUserID);
        $targetUserID = $this->_GB->_DB->escapeString($targetUserID);
        $label = $this->_GB->_DB->escapeString(trim($label));

        $query = $this->_GB->_DB->select('user_labels', 'id', "`owner_user_id` = '{$ownerUserID}' AND `target_user_id` = '{$targetUserID}'", '', '1');
        $existingLabelID = null;
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $existingLabelID = $fetch['id'];
        }
        $this->_GB->_DB->free($query);

        if ($label === '') {
            if ($existingLabelID === null) {
                return true;
            }

            return $this->_GB->_DB->delete('user_labels', "`id` = '{$existingLabelID}'");
        }

        if ($existingLabelID !== null) {
            $fields = "`label` = '" . $label . "'";
            $fields .= ",`updated_at` = '" . time() . "'";
            return $this->_GB->_DB->update('user_labels', $fields, "`id` = '{$existingLabelID}'");
        }

        return $this->_GB->_DB->insert('user_labels', array(
            'owner_user_id' => $ownerUserID,
            'target_user_id' => $targetUserID,
            'label' => $label,
            'updated_at' => time()
        ));
    }

    private function resolvePendingWalletMessagesForUser($fetch)
    {
        if ($this->_Messages === null || empty($fetch['wallet_address']) || empty($fetch['id'])) {
            return 0;
        }

        return $this->_Messages->resolvePendingWalletMessages($fetch['wallet_address'], $fetch['id']);
    }

    private function buildWalletJoinResponse($fetch, $message, $needsProfile = false)
    {
        $needsProfileCheck = $needsProfile || empty($fetch['image']);
        return array(
            'success' => true,
            'message' => $message,
            'walletAddress' => empty($fetch['wallet_address']) ? null : $fetch['wallet_address'],
            'smsVerification' => false,
            'code' => null,
            'userID' => $fetch['id'],
            'token' => $fetch['auth_token'],
            'hasBackup' => $fetch['has_backup'] == 1 ? true : false,
            'backup_hash' => empty($fetch['backup_hash']) ? null : $fetch['backup_hash'],
            'hasProfile' => !$needsProfileCheck && $this->userProfileExist($fetch['id']) ? true : false
        );
    }

    private function formatContactPayload($fetch, $ownerUserID, $contactID = 0, $fallbackUsername = null, $fallbackWalletAddress = null, $fallbackImage = null)
    {
        $label = $this->getStoredLabel($ownerUserID, $fetch['id']);
        $fetch['id'] = empty($fetch['id']) ? null : (int)$fetch['id'];
        $fetch['contactID'] = empty($contactID) ? 0 : (int)$contactID;
        $fetch['username'] = !empty($label) ? $label : (empty($fetch['username']) ? $fallbackUsername : $fetch['username']);
        $fetch['walletAddress'] = empty($fetch['wallet_address']) ? $fallbackWalletAddress : $fetch['wallet_address'];
        $fetch['image'] = empty($fetch['image']) ? $fallbackImage : $fetch['image'];
        $fetch['Linked'] = true;
        $fetch['Activate'] = !empty($fetch['is_activated']) && (int)$fetch['is_activated'] === 1;
        $fetch['Exist'] = true;
        $fetch['status'] = empty($fetch['status']) ? null : $fetch['status'];
        $fetch['status_date'] = empty($fetch['status_date']) ? null : $this->_GB->Date($fetch['status_date']);
        $fetch['registered_id'] = empty($fetch['registered_id']) ? null : $fetch['registered_id'];
        unset($fetch['auth_token'], $fetch['created_at'], $fetch['country'], $fetch['user_id'], $fetch['is_activated'], $fetch['wallet_address']);
        return $fetch;
    }

    /**
     * Function to  check if th user is already exist.
     * @param $walletAddress
     * @param $country
     * @return bool
     * @internal param $UserName
     */
    public function UserExist($walletAddress, $country = null)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        $query = $this->_GB->_DB->select('users', '`id`', "`wallet_address` = '{$walletAddress}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }
    }

    public function initDeleteAccount($userID, $walletAddress, $country)
    {
        $code = rand(100000, 999999);
        $res = $this->DeleteAccount($userID, $walletAddress, $code, $country);
        return $res;
    }

    public function DeleteAccount($userID, $walletAddress, $code, $country)
    {

        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        $userID = $this->_GB->_DB->escapeString($userID);

        if ($this->UserExist($walletAddress)) {
            $res = $this->createCode($userID, $code);
            if ($res) {
                $array = array(
                    'success' => true,
                    'message' => 'Delete confirmation code generated.',
                    'walletAddress' => $walletAddress,
                    'smsVerification' => false,
                    'code' => $code
                );
                return $array;
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Sorry! Error occurred while delete your account.',
                    'walletAddress' => null,
                    'smsVerification' => false,
                    'code' => null
                );
                return $array;
            }
        } else {
            $array = array(
                'success' => false,
                'message' => 'Sorry! Account not found.',
                'walletAddress' => null,
                'smsVerification' => false,
                'code' => null
            );
            return $array;
        }
    }

    public function deleteAccountConfirmation($code)
    {
        $code = $this->_GB->_DB->escapeString($code);

        $query = ("SELECT  U.id,
                           U.username,
                           U.wallet_address,
                           U.auth_token,
                           U.is_activated,
                           U.has_backup,
                           U.backup_hash
                           FROM prefix_users U, prefix_sms_codes S
                           WHERE S.code = {$code}
                           AND S.UserID = U.id ");
        $query = $this->_GB->_DB->MySQL_Query($query);
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $queryGroup = " SELECT G.id , G.date AS CreatedDate,
                                  G.name AS GroupName ,
                                  G.image AS GroupImage
                          FROM prefix_users U,prefix_groups G,prefix_group_members GM
                          WHERE
                          CASE
                          WHEN GM.userID = U.id
                          THEN GM.userID = {$fetch['id']}
                          END
                          AND GM.groupID = G.id
                           AND U.is_activated = '1'
                           AND (GM.role = 'admin' OR GM.role = 'member')
                          GROUP BY G.id  ORDER BY G.id ASC ";
            $queryGroup = $this->_GB->_DB->MySQL_Query($queryGroup);

            if ($this->_GB->_DB->numRows($queryGroup) != 0) {
                while ($fetchGroup = $this->_GB->_DB->fetchAssoc($queryGroup)) {
                    $this->_Group->exitGroup($fetch['id'], $fetchGroup['id']);
                    $fields = "`Deleted` = '" . 1 . "'";
                    $this->_GB->_DB->update('group_members', $fields, " `groupID` = '{$fetchGroup['id']}' AND `userID` = {$fetch['id']}");
                }
            }
            $this->_GB->_DB->delete('sms_codes', " `UserID` =  {$fetch['id']} ");
            $is_activated = 0;

            $fields = "`is_activated` = '" . $is_activated . "'";
            $fields .= ",`image` = NULL";
            $fields .= ",`registered_id` = NULL";
            $delete = $this->_GB->_DB->update('users', $fields, "`id`= {$fetch['id']}");
            if ($delete) {
                $array = array(
                    'success' => true,
                    'message' => 'Your account is deleted successfully'
                );
                $this->_GB->Json($array);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Failed to delete your account'
                );
                $this->_GB->Json($array);
            }
            $this->_GB->_DB->free($query);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to delete your account'
            );
            $this->_GB->Json($array);
        }

    }


    public function insertDefaultStatus($userID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $app_name = $this->_GB->getSettings('app_name');

        $arrayStatus = array("Only Emergency calls", "Busy", "At work", "in a meeting", "Available", "Playing football", "Hey, I am using $app_name. Enjoy it.");
        $lastElement = end($arrayStatus);
        foreach ($arrayStatus as $status) {
            if ($status == $lastElement) {
                $addDefaultStatus = array(
                    'status' => $status,
                    'userID' => $userID,
                    'current' => 1
                );
            } else {
                $addDefaultStatus = array(
                    'status' => $status,
                    'userID' => $userID,
                    'current' => 0
                );
            }
            $this->_GB->_DB->insert('status', $addDefaultStatus);
        }
    }

    /**
     * Function to  check if th user is already exist.
     * @param $walletAddress
     * @return bool
     * @internal param $UserName
     */
    public function UserLinked($walletAddress)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);

        $query = $this->_GB->_DB->select('users', '`id`', "`wallet_address` = '{$walletAddress}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }
    }

    /**
     * Function to  check if th user is activate.
     * @param $walletAddress
     * @return bool
     * @internal param $UserName
     */
    public function UserActivate($walletAddress)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);

        $activated = 1;
        $query = $this->_GB->_DB->select('users', '`id`', "`wallet_address` = '{$walletAddress}' AND `is_activated` = '{$activated}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }
    }

    public function ResendCode($walletAddress)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        $code = rand(100000, 999999);

        $IDResult = $this->_GB->_DB->select('users', '*', "  `wallet_address` = '{$walletAddress}'");
        if ($this->_GB->_DB->numRows($IDResult) > 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($IDResult);
            $res = $this->createCode($fetch['id'], $code);
            if ($res) {
                $array = array(
                    'success' => true,
                    'message' => 'Verification code regenerated. SMS disabled.',
                );
                $this->_GB->Json($array);
            } else {

                $array = array(
                    'success' => false,
                    'message' => 'Sorry! Error occurred .',
                );
                $this->_GB->Json($array);
            }
            $this->_GB->_DB->free($IDResult);
        }


    }

    public function createCode($UserID, $code)
    {
        $UserID = $this->_GB->_DB->escapeString($UserID);

        // delete the old otp if exists
        $this->_GB->_DB->delete('sms_codes', "`UserID`= {$UserID}");
        $array = array(
            'UserID' => $UserID,
            'code' => $code,
            'status' => 0
        );
        $result = $this->_GB->_DB->insert('sms_codes', $array);
        return $result;
    }


    public function activateUser($code)
    {
        $code = $this->_GB->_DB->escapeString($code);

        $query = (" SELECT  U.id,
                           U.username,
                           U.phone,
                           U.auth_token,
                           U.is_activated,
                           U.has_backup,
                           U.backup_hash
                           FROM prefix_users U, prefix_sms_codes S
                           WHERE S.code = {$code}
                           AND S.UserID = U.id ");
        $query = $this->_GB->_DB->MySQL_Query($query);
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $is_activated = 1;
            $this->_GB->_DB->update('users', "`is_activated` = '{$is_activated}' ", "`id`='{$fetch['id']}'");
            $this->_GB->_DB->update('sms_codes', "`status` = '{$is_activated}' ", "`UserID`='{$fetch['id']}'");

            $array = array(
                'success' => true,
                'message' => 'Your account has been created successfully.',
                'userID' => $fetch['id'],
                'token' => $fetch['auth_token'],
                'hasBackup' => $fetch['has_backup'] == 1 ? true : false,
                'backup_hash' =>   (empty($fetch['backup_hash'])) ? null : $fetch['backup_hash'],
                'hasProfile' => $this->userProfileExist($fetch['id']) ? true : false
            );
            $this->_GB->Json($array);

        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to activate your account try again or resend sms to get new code.',
                'userID' => null,
                'token' => null,
                'hasBackup' => false,
                'backup_hash' => null,
                'hasProfile' => false
            );
            $this->_GB->Json($array);
        }
        $this->_GB->_DB->free($query);

    }

    private function userProfileExist($userID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);

        $query = $this->_GB->_DB->select('users', 'id', "(( `username` <> ''   AND `username` IS NOT NULL)
                                                           OR (`image` <> '' AND `image` IS NOT NULL))
                                                           AND `id`= '{$userID}' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }

    }

    /**
     * Generating random Unique MD5 String for user Api key
     * @param $walletAddress
     * @return string
     */
    private function generateApiKey($walletAddress)
    {
        return md5(uniqid(rand(), true) . $walletAddress);
    }


    public function compareWalletAddresses($array, $ownerUserID)
    {
        $ownerUserID = $this->_GB->_DB->escapeString($ownerUserID);
        $contactsModelList = isset($array['contactsModelList']) ? $array['contactsModelList'] : array();
        $resultFinal = array();
        for ($i = 0; $i < count($contactsModelList); $i++) {
            $walletAddress = $this->_GB->_DB->escapeString($contactsModelList[$i]['walletAddress']);
            $walletAddressTmp = $this->_GB->_DB->escapeString($contactsModelList[$i]['walletAddressTmp']);
            $username = $this->_GB->_DB->escapeString($contactsModelList[$i]['username']);
            $image = $this->_GB->_DB->escapeString($contactsModelList[$i]['image']);
            $contactID = $this->_GB->_DB->escapeString($contactsModelList[$i]['contactID']);
            $identity = empty($walletAddressTmp) ? $walletAddress : $walletAddressTmp;

            $fetch = $this->getUserByIdentity($identity);
            if ($fetch != null) {
                $resultFinal [] = $this->formatContactPayload($fetch, $ownerUserID, $contactID, $username, $walletAddress, $image);
            } else {
                $fetch = array(
                    'id' => $contactID,
                    'contactID' => $contactID,
                    'Linked' => false,
                    'Activate' => false,
                    'Exist' => true,
                    'status' => $walletAddress,
                    'walletAddress' => $walletAddress,
                    'image' => $image,
                    'username' => empty($username) ? null : $username,
                    'registered_id' => null,
                    'status_date' => null);
                $resultFinal [] = $fetch;
            }
        }
        $this->_GB->Json($resultFinal);
        return;
        $resultFinal = array();
        for ($i = 0; $i < count($contactsModelList); $i++) {
            $walletAddress = $this->_GB->_DB->escapeString($contactsModelList[$i]['walletAddress']);
            $walletAddressTmp = $this->_GB->_DB->escapeString($contactsModelList[$i]['walletAddressTmp']);
            $username = $this->_GB->_DB->escapeString($contactsModelList[$i]['username']);
            $image = $this->_GB->_DB->escapeString($contactsModelList[$i]['image']);
            $contactID = $this->_GB->_DB->escapeString($contactsModelList[$i]['contactID']);

            if ($this->UserLinked($walletAddressTmp)) {
                $result = $this->_GB->_DB->select('users', '*', "  `wallet_address` LIKE '%{$walletAddressTmp}%'");
                if ($this->_GB->_DB->numRows($result) != 0) {
                    $fetch = $this->_GB->_DB->fetchAssoc($result);
                    if ($this->UserActivate($fetch['wallet_address'])) {
                        $fetch['contactID'] = $contactID;
                        $fetch['username'] = (empty($fetch['username'])) ? $username : $fetch['username'];
                        $fetch['Linked'] = true;
                        $fetch['Activate'] = true;
                        $fetch['Exist'] = true;
                        $fetch['walletAddress'] = (empty($fetch['wallet_address'])) ? $walletAddress : $walletAddress;
                        $fetch['image'] = (empty($fetch['image'])) ? null : $fetch['image'];
                        $fetch['status_date'] = (empty($fetch['status_date'])) ? null : $this->_GB->Date($fetch['status_date']);
                        $fetch['registered_id'] = (empty($fetch['registered_id'])) ? null : $fetch['registered_id'];
                        unset ($fetch['auth_token']);


                        $resultFinal [] = $fetch;
                    } else {
                        $fetch['contactID'] = $contactID;
                        $fetch['username'] = (empty($fetch['username'])) ? $username : $fetch['username'];
                        $fetch['Linked'] = true;
                        $fetch['Activate'] = false;
                        $fetch['Exist'] = true;
                        $fetch['walletAddress'] = (empty($fetch['wallet_address'])) ? $walletAddress : $walletAddress;
                        $fetch['image'] = (empty($fetch['image'])) ? null : $fetch['image'];
                        $fetch['registered_id'] = (empty($fetch['registered_id'])) ? null : $fetch['registered_id'];
                        $fetch['status_date'] = (empty($fetch['status_date'])) ? null : $this->_GB->Date($fetch['status_date']);
                        unset ($fetch['auth_token']);


                        $resultFinal [] = $fetch;
                    }
                }

                $this->_GB->_DB->free($result);

            } else {
                $fetch = array(
                    'id' => $contactID,
                    'contactID' => $contactID,
                    'Linked' => false,
                    'Activate' => false,
                    'Exist' => true,
                    'status' => $walletAddress,
                    'walletAddress' => $walletAddress,
                    'image' => $image,
                    'username' => $username,
                    'registered_id' => null);
                $resultFinal [] = $fetch;

            }
        }
        $this->_GB->Json($resultFinal);

    }


    public function getSessionToken($auth_token)
    {
        $auth_token = $this->_GB->_DB->escapeString($auth_token);
        $query = $this->_GB->_DB->select('users', 'auth_token', "`auth_token`= '{$auth_token}'  ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }
    }

    public function getUserIdByToken($auth_token)
    {
        $auth_token = $this->_GB->_DB->escapeString($auth_token);
        $query = $this->_GB->_DB->select('users', 'id', "`auth_token`= '{$auth_token}'");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['id'];
        } else {
            $this->_GB->_DB->free($query);
            return 0;
        }
    }

    public function getContactInfo($userID, $ownerUserID = 0)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $ownerUserID = $this->_GB->_DB->escapeString($ownerUserID);
        $query = $this->_GB->_DB->select('users', '*', "`id`= '{$userID}'  ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $fetch = $this->formatContactPayload($fetch, $ownerUserID, 0, null, $fetch['wallet_address'], $fetch['image']);
            $this->_GB->Json($fetch);
        } else {
            $this->_GB->Json(null);
        }
        $this->_GB->_DB->free($query);
        return;
        $query = $this->_GB->_DB->select('users', '*', "`id`= '{$userID}'  ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $fetch['id'] = (empty($fetch['id'])) ? null : $fetch['id'];
            $fetch['username'] = (empty($fetch['username'])) ? null : $fetch['username'];
            $fetch['walletAddress'] = (empty($fetch['wallet_address'])) ? null : $fetch['wallet_address'];
            $fetch['image'] = (empty($fetch['image'])) ? null : $fetch['image'];
            $fetch['Linked'] = $this->UserLinked($fetch['wallet_address']);
            $fetch['Activate'] = $this->UserActivate($fetch['wallet_address']);
            $fetch['status'] = (empty($fetch['status'])) ? null : $fetch['status'];
            $fetch['status_date'] = (empty($fetch['status_date'])) ? null : $this->_GB->Date($fetch['status_date']);

            unset($fetch['auth_token'], $fetch['is_activated'], $fetch['created_at'], $fetch['country'], $fetch['user_id']);
            $this->_GB->Json($fetch);

        } else {
            $this->_GB->Json(null);
        }
        $this->_GB->_DB->free($query);
    }


    /**
     * Function to un block user
     * @param $userID
     * @param $to
     */
    public function unBlockUser($userID, $to)
    {
        $to = $this->_GB->_DB->escapeString($to);
        $delete = $this->_GB->_DB->delete('users_blocked', "`from_id` = {$userID} AND `to_id` = {$to}");
        if ($delete) {
            $this->_GB->Json(array('success' => true, 'message' => 'User has been UnBlocked successfully'));
        } else {
            $this->_GB->Json(array('success' => false, 'message' => 'try again, something went wrong'));
        }
    }

    /**
     * Function to block user
     * @param $userID
     * @param $to
     */
    public function blockUser($userID, $to)
    {
        $to = $this->_GB->_DB->escapeString($to);
        $query = $this->_GB->_DB->select('users', '`id`', "`id` = {$to}");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            if ($fetch['id'] != $userID) {
                if ($this->isBlocking($userID, $fetch['id']) != true) {
                    $insert = $this->_GB->_DB->insert('users_blocked', array('from_id' => $userID, 'to_id' => $fetch['id'], 'date' => time()));
                    if ($insert) {
                        $this->_GB->Json(array('success' => true, 'message' => 'User has been blocked successfully'));
                    } else {
                        $this->_GB->Json(array('success' => false, 'message' => 'Can\'t block the user Try Again'));
                    }
                } else {
                    $this->unBlockUser($userID, $to);
                }
            }
        } else {
            $this->_GB->Json(array('success' => false, 'message' => 'User doesn\'t Exists anymore'));
        }
        $this->_GB->_DB->free($query);
    }

    /**
     * Function to check if user is blocking
     * @param $from
     * @param $to
     * @return bool
     */
    public function isBlocking($from, $to)
    {
        $query = $this->_GB->_DB->select('users_blocked', '`id`', "`from_id` = {$from} AND `to_id` = {$to}");
        $count = $this->_GB->_DB->numRows($query);
        if ($count != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }

    }

    public function saveAcceptedCall($array)
    {
        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }

        $fromId = $array['fromId'];
        $toId = $array['toId'];
        $date = $array['date'];
        $duration = $array['duration'];
        $isVideo = $array['isVideo'];

        $arrayData = array(
            'from_id' => $fromId,
            'to_id' => $toId,
            'date' => $date,
            'duration' => $duration,
            'accepted' => 1,
            'received' => 0,
            'emitted' => 0,
            'type' => $isVideo
        );
        $insert = $this->_GB->_DB->insert('calls', $arrayData);
        if ($insert) {
            $this->_GB->Json(array('success' => true, 'message' => 'accepted call has been saved successfully'));
        } else {
            $this->_GB->Json(array('success' => false, 'message' => 'Can\'t block the user Try Again'));
        }

    }


    public function saveEmittedCall($array)
    {
        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }

        $fromId = $array['fromId'];
        $toId = $array['toId'];
        $date = $array['date'];
        $duration = $array['duration'];
        $isVideo = $array['isVideo'];

        $arrayData = array(
            'from_id' => $fromId,
            'to_id' => $toId,
            'date' => $date,
            'duration' => $duration,
            'accepted' => 0,
            'received' => 0,
            'emitted' => 1,
            'type' => $isVideo
        );
        $insert = $this->_GB->_DB->insert('calls', $arrayData);
        if ($insert) {
            $this->_GB->Json(array('success' => true, 'message' => 'accepted call has been saved successfully'));
        } else {
            $this->_GB->Json(array('success' => false, 'message' => 'Can\'t block the user Try Again'));
        }
    }

    public function saveReceivedCall($array)
    {
        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }

        $fromId = $array['fromId'];
        $toId = $array['toId'];
        $date = $array['date'];
        $duration = $array['duration'];
        $isVideo = $array['isVideo'];

        $arrayData = array(
            'from_id' => $fromId,
            'to_id' => $toId,
            'date' => $date,
            'duration' => $duration,
            'accepted' => 0,
            'received' => 1,
            'emitted' => 0,
            'type' => $isVideo
        );
        $insert = $this->_GB->_DB->insert('calls', $arrayData);
        if ($insert) {
            $this->_GB->Json(array('success' => true, 'message' => 'accepted call has been saved successfully'));
        } else {
            $this->_GB->Json(array('success' => false, 'message' => 'Can\'t block the user Try Again'));
        }
    }

    /**
     *  Function to user registerId
     * @param $registeredId
     * @param $userID
     * @return null
     */
    public function getUserRegisterID($registeredId, $userID)
    {
        $registeredId = $this->_GB->_DB->escapeString($registeredId);
        $userID = $this->_GB->_DB->escapeString($userID);
        $query = $this->_GB->_DB->select('users', '`id`', "`id` = '{$userID}' AND `registered_id` = '{$registeredId}' ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Function to update the register id  GCM of a specific user
     * @param $userID
     * @param $registeredId
     */
    public function updateRegisteredId($userID, $registeredId)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $registeredId = $this->_GB->_DB->escapeString($registeredId);

        if (empty($registeredId)) {
            $array = array(
                'success' => false,
                'message' => 'Failed to update Registered Id: empty token',
                'registered_id' => null
            );
            $this->_GB->Json($array);
            return;
        }

        $fields = "`registered_id` = '" . $registeredId . "'";
        $result = $this->_GB->_DB->update('users', $fields, "`id` = {$userID} ");
        if ($result) {
            $this->_Group->addSingleMemberLogin($userID);
            $userQuery = $this->_GB->_DB->select('users', '`id`, `wallet_address`', "`id` = {$userID}", '', '1');
            if ($userFetch = $this->_GB->_DB->fetchAssoc($userQuery)) {
                $this->resolvePendingWalletMessagesForUser($userFetch);
            }
            $this->_GB->_DB->free($userQuery);
            $array = array(
                'success' => true,
                'message' => ' Registered Id is updated successfully ',
                'registered_id' => $registeredId
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to update Registered Id ',
                'registered_id' => null
            );
            $this->_GB->Json($array);
        }
    }

    public function getStatus($query)
    {
        if ($this->_GB->_DB->numRows($query) != 0) {
            $status = array();
            while ($fetch = $this->_GB->_DB->fetchAssoc($query)) {

                $fetch['current'] = $fetch['current'] == 1 ? true : false;
                $status[] = $fetch;

            }
            $this->_GB->Json($status);
        } else {
            $this->_GB->Json(null);
        }
        $this->_GB->_DB->free($query);
    }

    public function editStatus($newStatus, $userID, $statusID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $statusID = $this->_GB->_DB->escapeString($statusID);
        $newStatus = $this->_GB->_DB->escapeString($newStatus);

        $fields = "`status` = '" . $newStatus . "'";
        $result = $this->_GB->_DB->update('status', $fields, "`id` = {$statusID} AND `userID` = {$userID}");


        // check if row inserted or not
        if ($result) {

            $fields .= ",`status_date` = '" . time() . "'";
            $this->_GB->_DB->update('users', $fields, "`id` = {$userID}");
            $array = array(
                'success' => true,
                'message' => 'Status is updated successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to update status '
            );
            $this->_GB->Json($array);
        }
    }

    public function existStatus($userID, $status)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $status = $this->_GB->_DB->escapeString($status);
        $query = $this->_GB->_DB->select('status', '*', "`status` = '{$status}' AND `userID` = {$userID}");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $this->_GB->_DB->free($query);
            return true;
        } else {
            $this->_GB->_DB->free($query);
            return false;
        }

    }

    public function insertStatus($userID, $status)
    {
        $status = $this->_GB->_DB->escapeString($status);
        $userID = $this->_GB->_DB->escapeString($userID);


        if (strpos($status, '\'') !== false) {
            $status = str_replace('\'', "\\'", $status);
        }

        if ($this->existStatus($userID, $status)) {
            $array = array(
                'success' => true,
                'message' => 'Status already exist '
            );
            $this->_GB->Json($array);
        } else {
            $fields = "`current` = '" . 0 . "'";
            $this->_GB->_DB->update('status', $fields, "`userID` = {$userID}");

            $addNewStatus = array(
                'status' => $status,
                'userID' => $userID,
                'current' => 1
            );

            $insert = $this->_GB->_DB->insert('status', $addNewStatus);


            if ($insert) {
                $fields = "`status` = '" . $status . "'";
                $fields .= ",`status_date` = '" . time() . "'";
                $result = $this->_GB->_DB->update('users', $fields, "`id` = {$userID}");

                // check if row inserted or not
                if ($result) {
                    $array = array(
                        'success' => true,
                        'message' => 'Status is updated successfully '
                    );
                    $this->_GB->Json($array);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Failed to update status '
                    );
                    $this->_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Failed to insert status '
                );
                $this->_GB->Json($array);
            }
        }
    }

    public function updateStatus($userID, $statusID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $statusID = $this->_GB->_DB->escapeString($statusID);


        $status = null;
        $query = $this->_GB->_DB->select('status', '*', "`id` = {$statusID} AND `userID` = {$userID}");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $status = $this->_GB->_DB->escapeString($fetch['status']);
            $field1 = "`current` = '" . 0 . "'";
            $field2 = "`current` = '" . 1 . "'";
            $this->_GB->_DB->update('status', $field1, "`userID` = {$userID}");
            $this->_GB->_DB->update('status', $field2, "`id` = {$statusID} AND `userID` = {$userID}");

            if (strpos($status, '\'') !== false) {
                $status = str_replace('\'', "\\'", $status);
            }
            $fields = "`status` = '" . $status . "'";
            $fields .= ",`status_date` = '" . time() . "'";
            $result = $this->_GB->_DB->update('users', $fields, "`id` = {$userID}");

            // check if row inserted or not
            if ($result) {
                $array = array(
                    'success' => true,
                    'message' => 'Status is updated successfully '
                );
                $this->_GB->Json($array);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Failed to update status '
                );
                $this->_GB->Json($array);
            }
        }
        $this->_GB->_DB->free($query);
    }

    public function DeleteStatus($userID, $statusID)
    {
        $userID = $this->_GB->_DB->escapeString($userID);
        $statusID = $this->_GB->_DB->escapeString($statusID);

        $delete = $this->_GB->_DB->delete('status', "`id`= '{$statusID}' AND `userID`= {$userID}");
        if ($delete) {
            $array = array(
                'success' => true,
                'message' => 'Status is deleted successfully'
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to delete status '
            );
            $this->_GB->Json($array);
        }
    }


    public function DeleteAllStatus($delete)
    {
        if ($delete) {
            $array = array(
                'success' => true,
                'message' => 'All Status are deleted successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to delete status '
            );
            $this->_GB->Json($array);
        }
    }


    public function editName($name, $userID, $targetUserID = 0)
    {
        $name = trim($name);
        
        if (empty($name)) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => 'Username is required'
            ));
            return;
        }
        
        if (strlen($name) < 2) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => 'Username must be at least 2 characters'
            ));
            return;
        }
        
        if (strlen($name) > 50) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => 'Username must be less than 50 characters'
            ));
            return;
        }
        
        $name = $this->_GB->_DB->escapeString($name);
        $userID = $this->_GB->_DB->escapeString($userID);
        $targetUserID = empty($targetUserID) ? $userID : $this->_GB->_DB->escapeString($targetUserID);

        if ((int)$targetUserID === (int)$userID) {
            $fields = "`username` = '" . $name . "'";
            $result = $this->_GB->_DB->update('users', $fields, "`id` = {$userID} ");
        } else {
            $targetUser = $this->getUser($targetUserID);
            if ($targetUser == null) {
                $this->_GB->Json(array(
                    'success' => false,
                    'message' => 'Failed to update name '
                ));
                return;
            }

            $result = $this->upsertStoredLabel($userID, $targetUserID, $name);
        }

        if ($result) {
            $array = array(
                'success' => true,
                'message' => 'Name  is updated successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to update name '
            );
            $this->_GB->Json($array);
        }
        return;

        $fields = "`username` = '" . $name . "'";
        $result = $this->_GB->_DB->update('users', $fields, "`id` = {$userID} ");

        // check if row inserted or not
        if ($result) {
            $array = array(
                'success' => true,
                'message' => 'Name  is updated successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to update name '
            );
            $this->_GB->Json($array);
        }
    }

    public function updateBackup($hasBackupFolder, $userID)
    {
        $hasBackup = 1;
        $fields = "`has_backup` = '" . $hasBackup . "'";
        $fields .= ",`backup_hash` = '" . $hasBackupFolder . "'";
        $result = $this->_GB->_DB->update('users', $fields, "`id` = {$userID}");
        if ($result) {
            $array = array(
                'success' => true,
                'message' => 'the backup stored successfully '
            );
            $this->_GB->Json($array);
        } else {
            $array = array(
                'success' => false,
                'message' => 'Failed to store the backup '
            );
            $this->_GB->Json($array);
        }
    }


    /**
     * Function to get  user
     * @param $id
     * @return null
     */
    public function getUser($id)
    {
        $id = $this->_GB->_DB->escapeString($id);

        $query = $this->_GB->_DB->select('users', '*', "`id` = {$id}");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            unset($fetch['backup_hash'], $fetch['registered_id'], $fetch['auth_token']);
            $this->_GB->_DB->free($query);
            return $fetch;
        } else {
            $this->_GB->_DB->free($query);
            return null;
        }
    }
    /****************************
     * functions for admins
     ****************************/


    /**
     * Function for admin login
     * @param $username
     * @param $password
     */
    public
    function adminLogin($username, $password)
    {

        $username = trim($this->_GB->_DB->escapeString($username));
        $password = trim($this->_GB->_DB->escapeString($password));
        $adminPassword = md5($password);
        $query = $this->_GB->_DB->select('admins', '*', "`username` = '{$username}' AND `password` = '{$adminPassword}'");
        $fetch = $this->_GB->_DB->fetchAssoc($query);
        if (empty($username) || empty($password)) {
            echo $this->_GB->ErrorDisplay('All fields are required');
        } else if ($this->_GB->_DB->numRows($query) <= 0) {
            echo $this->_GB->ErrorDisplay('Login failed please try again later');
        } else {
            $this->_GB->setSession('admin', $fetch['id']);
            $this->_GB->setSession('adminName', $fetch['username']);
            header("Refresh: 1; url=index.php");
            echo $this->_GB->ErrorDisplay('Logged in successfully.', 'yes');
        }
        $this->_GB->_DB->free($query);
    }

}
