<?php

/**
 * Created by Abderrahim El imame.
 * Email : abderrahim.elimame@gmail.com
 * Date: 27/02/2016
 * Time: 22:07
 */
class MessagesController
{


    public $_GB;
    public $Users;
    public $_Users;

    public $_Waiting, $_Sent, $_Delivered, $_Seen, $_Downloaded;

    public function __construct($_GB, $Users)
    {
        $this->_GB = $_GB;
        $this->_Users = $Users;
        $this->_Waiting = 0;
        $this->_Sent = 1;
        $this->_Delivered = 2;
        $this->_Seen = 3;
        $this->_Downloaded = 4;
    }


    /**
     * Function to send a new message
     * @param $array
     */
    public function sendMessage($array)
    {

        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }
        $userId = $array['senderId'];
        $recipientId = isset($array['recipientId']) ? (int)$array['recipientId'] : 0;
        $recipientWalletAddress = isset($array['recipientWalletAddress']) ? $array['recipientWalletAddress'] : null;

        if ($recipientId <= 0 && !empty($recipientWalletAddress)) {
            $recipientId = $this->getUserIdByWalletAddress($recipientWalletAddress);
            if ($recipientId > 0) {
                $array['recipientId'] = $recipientId;
            }
        }

        if ($recipientId <= 0 && !empty($recipientWalletAddress)) {
            $this->queueMessageForWalletAddress($array, $recipientWalletAddress);
            return;
        }

        if ($recipientId <= 0) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => "recipientId or recipientWalletAddress is required"
            ));
            return;
        }

        if ($userId != $recipientId) {
            $conversationID = $this->getConversation($userId, $recipientId);

            if ($conversationID != 0) {
                $array['conversationId'] = $conversationID;
            } else {
                $data = array(
                    'sender' => $userId,
                    'recipient' => $recipientId,
                    'Date' => $array['date']);

                $insert = $this->_GB->_DB->insert('conversations', $data);
                if ($insert) {
                    $array['conversationId'] = $this->_GB->_DB->last_Id();
                }
            }



            $arrayData = array(
                'userID' => $userId,
                'message' => $array['messageBody'],
                'image' => $array['image'],
                'video' => $array['video'],
                'audio' => $array['audio'],
                'duration' => $array['duration'],
                'fileSize' => $array['fileSize'],
                'thumbnail' => $array['thumbnail'],
                'document' => $array['document'],
                'Date' => $array['date'],
                'groupID' => 0,
                'ConversationID' => $array['conversationId'],
                'status' => $this->_Sent
            );

            $insert = $this->_GB->_DB->insert('messages', $arrayData);
            if ($insert) {
                $serverMessageId = $this->_GB->_DB->last_Id();
                $this->tryUpdateStatus($serverMessageId, $this->_Sent);
                
                // Create message receipt for delivery tracking
                $receiptData = array(
                    'message_id' => $serverMessageId,
                    'recipient_id' => $recipientId,
                    'status' => 0
                );
                $this->_GB->_DB->insert('message_receipts', $receiptData);
                
                $arrayMessageData = array(
                    'actionType' => 'socket_new_message_server',
                    'recipientId' => $recipientId,
                    'messageId' => isset($array['messageId']) ? $array['messageId'] : $serverMessageId,
                    'messageBody' => $array['messageBody'],
                    'senderId' => $array['senderId'],
                    'walletAddress' => $array['walletAddress'],
                    'senderName' => $array['senderName'],
                    'date' => $array['date'],
                    'isGroup' => $array['isGroup'],
                    'image' => $array['image'],
                    'video' => $array['video'],
                    'audio' => $array['audio'],
                    'document' => $array['document'],
                    'thumbnail' => $array['thumbnail'],
                    'duration' => $array['duration'],
                    'fileSize' => $array['fileSize'],
                    'senderImage' => isset($array['senderImage']) ? $array['senderImage'] : null
                );


                $senderRegisteredId = isset($array['registered_id']) ? $array['registered_id'] : null;
                $recipientRegisteredId = null;
                $getUser = $this->_GB->_DB->select('users', '`registered_id`', '`id`=' . $recipientId);
                $fetchUser = $this->_GB->_DB->fetchAssoc($getUser);
                if ($fetchUser && !empty($fetchUser['registered_id'])) {
                    $recipientRegisteredId = $fetchUser['registered_id'];
                }

                if (!empty($senderRegisteredId) && $senderRegisteredId === $recipientRegisteredId) {
                    $this->_GB->sendMessageThroughFCM($recipientRegisteredId, $arrayMessageData);
                } elseif (!empty($recipientRegisteredId)) {
                    $this->_GB->sendMessageThroughFCM($recipientRegisteredId, $arrayMessageData);
                }

                $this->_GB->pushToSocket('socket_new_message_server', $arrayMessageData, $recipientId);


                $arrayMessage = array(
                    'success' => true,
                    'message' => "message sent successfully",
                    'messageId' => $serverMessageId
                );
                $this->_GB->Json($arrayMessage);

            } else {

                $arrayMessageData = array(
                    'success' => false,
                    'message' => "failed to send message"
                );
                $this->_GB->Json($arrayMessageData);
            }
        }

        // }
    }


    public function resolvePendingWalletMessages($walletAddress, $recipientId)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        $recipientId = (int)$recipientId;

        if (empty($walletAddress) || $recipientId <= 0) {
            return 0;
        }

        $this->ensurePendingWalletMessagesTable();

        $query = $this->_GB->_DB->select(
            'pending_wallet_messages',
            '*',
            "`recipient_wallet_address` = '{$walletAddress}' AND `status` = 0",
            '`id` ASC'
        );

        $resolvedCount = 0;
        $pendingRows = $this->_GB->_DB->numRows($query);
        for ($i = 0; $i < $pendingRows; $i++) {
            $pending = $this->_GB->_DB->fetchAssoc($query);
            $messageId = (int)$pending['message_id'];
            $senderId = (int)$pending['sender_id'];
            if ($messageId <= 0 || $senderId <= 0 || $senderId === $recipientId) {
                continue;
            }

            $conversationID = $this->getConversation($senderId, $recipientId);
            if ($conversationID == 0) {
                $messageQuery = $this->_GB->_DB->select('messages', '`Date`', "`id` = '{$messageId}'", '', '1');
                $messageDate = time();
                if ($messageFetch = $this->_GB->_DB->fetchAssoc($messageQuery)) {
                    $messageDate = $messageFetch['Date'];
                }
                $this->_GB->_DB->free($messageQuery);

                $insert = $this->_GB->_DB->insert('conversations', array(
                    'sender' => $senderId,
                    'recipient' => $recipientId,
                    'Date' => $messageDate
                ));
                if ($insert) {
                    $conversationID = $this->_GB->_DB->last_Id();
                }
            }

            if ($conversationID != 0) {
                $this->_GB->_DB->update('messages', "`ConversationID` = '{$conversationID}'", "`id` = '{$messageId}'");
            }

            $this->upsertMessageReceipt($messageId, $recipientId, 0);
            $this->_GB->_DB->update('pending_wallet_messages', "`status` = 1, `resolved_recipient_id` = '{$recipientId}'", "`id` = '{$pending['id']}'");
            $resolvedCount++;
        }
        $this->_GB->_DB->free($query);

        return $resolvedCount;
    }

    private function queueMessageForWalletAddress($array, $recipientWalletAddress)
    {
        $this->ensurePendingWalletMessagesTable();

        $arrayData = array(
            'userID' => $array['senderId'],
            'message' => $array['messageBody'],
            'image' => $array['image'],
            'video' => $array['video'],
            'audio' => $array['audio'],
            'duration' => $array['duration'],
            'fileSize' => $array['fileSize'],
            'thumbnail' => $array['thumbnail'],
            'document' => $array['document'],
            'Date' => $array['date'],
            'groupID' => 0,
            'ConversationID' => 0,
            'status' => $this->_Sent
        );

        $insert = $this->_GB->_DB->insert('messages', $arrayData);
        if (!$insert) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => "failed to queue message"
            ));
            return;
        }

        $serverMessageId = $this->_GB->_DB->last_Id();
        $this->tryUpdateStatus($serverMessageId, $this->_Sent);
        $this->_GB->_DB->insert('pending_wallet_messages', array(
            'message_id' => $serverMessageId,
            'sender_id' => $array['senderId'],
            'recipient_wallet_address' => $recipientWalletAddress,
            'status' => 0
        ));

        $this->_GB->Json(array(
            'success' => true,
            'message' => "message queued for wallet registration",
            'messageId' => $serverMessageId,
            'queuedByWalletAddress' => true
        ));
    }

    private function getUserIdByWalletAddress($walletAddress)
    {
        $walletAddress = $this->_GB->_DB->escapeString($walletAddress);
        if (empty($walletAddress)) {
            return 0;
        }

        $query = $this->_GB->_DB->select('users', '`id`', "`wallet_address` = '{$walletAddress}'", '', '1');
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return (int)$fetch['id'];
        }

        $this->_GB->_DB->free($query);
        return 0;
    }

    private function upsertMessageReceipt($messageId, $recipientId, $status)
    {
        $messageId = (int)$messageId;
        $recipientId = (int)$recipientId;
        $status = (int)$status;

        $existing = $this->_GB->_DB->select('message_receipts', '*', "`message_id`='{$messageId}' AND `recipient_id`='{$recipientId}'");
        if ($this->_GB->_DB->numRows($existing) > 0) {
            $this->_GB->_DB->free($existing);
            return $this->_GB->_DB->update('message_receipts', "`status` = GREATEST(status, {$status})", "`message_id`='{$messageId}' AND `recipient_id`='{$recipientId}'");
        }

        $this->_GB->_DB->free($existing);
        return $this->_GB->_DB->insert('message_receipts', array(
            'message_id' => $messageId,
            'recipient_id' => $recipientId,
            'status' => $status
        ));
    }

    private function ensurePendingWalletMessagesTable()
    {
        $this->_GB->_DB->MySQL_Query(
            "CREATE TABLE IF NOT EXISTS `prefix_pending_wallet_messages` (" .
            "`id` int(11) NOT NULL AUTO_INCREMENT," .
            "`message_id` int(11) NOT NULL," .
            "`sender_id` int(11) NOT NULL," .
            "`recipient_wallet_address` varchar(255) NOT NULL," .
            "`resolved_recipient_id` int(11) DEFAULT NULL," .
            "`status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=pending, 1=resolved'," .
            "`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," .
            "`updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," .
            "PRIMARY KEY (`id`)," .
            "UNIQUE KEY `uniq_message_wallet` (`message_id`, `recipient_wallet_address`)," .
            "KEY `idx_wallet_status` (`recipient_wallet_address`, `status`)," .
            "KEY `idx_resolved_recipient` (`resolved_recipient_id`)" .
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    public function getConversation($senderID, $recipientID)
    {
        $senderID = $this->_GB->_DB->escapeString($senderID);
        $recipientID = $this->_GB->_DB->escapeString($recipientID);

        $query = $this->_GB->_DB->select('conversations', 'id', "(`sender`= '{$senderID}' AND `recipient`= '{$recipientID}') OR (`sender`= '{$recipientID}' AND `recipient`= '{$senderID}') ");
        if ($this->_GB->_DB->numRows($query) != 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($query);
            $this->_GB->_DB->free($query);
            return $fetch['id'];
        } else {
            $this->_GB->_DB->free($query);
            return 0;
        }
    }


    /******************************************** Groups methods ********************************************
     *
     ******************************************* Groups methods *********************************************/

    /**
     *  save the new group messages
     * @param $array
     */
    public function saveMessageGroup($array)
    {

        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }
        $groupID = $array['groupID'];


        $arrayData = array(
            'groupID' => $groupID,
            'message' => $array['messageBody'],
            'image' => $array['image'],
            'video' => $array['video'],
            'audio' => $array['audio'],
            'duration' => $array['duration'],
            'fileSize' => $array['fileSize'],
            'thumbnail' => $array['thumbnail'],
            'document' => $array['document'],
            'Date' => $array['date'],
            'UserID' => $array['senderId'],
            'ConversationID' => 0
        );
        $insert = $this->_GB->_DB->insert('messages', $arrayData);
        if ($insert) {

            $arrayMessageData = array(
                'actionType' => 'socket_new_group_message_server',
                'senderId' => $array['senderId'],
                'recipientId' => $array['recipientId'],
                'messageBody' => $array['messageBody'],
                'senderName' => $array['senderName'],
                'walletAddress' => $array['walletAddress'],
                'GroupImage' => $array['GroupImage'],
                'GroupName' => $array['GroupName'],
                'isGroup' => $array['isGroup'],
                'date' => $array['date'],
                'video' => $array['video'],
                'thumbnail' => $array['thumbnail'],
                'image' => $array['image'],
                'audio' => $array['audio'],
                'document' => $array['document'],
                'duration' => $array['duration'],
                'fileSize' => $array['fileSize'],
                'groupID' => $array['groupID']
            );


            $getGroup = $this->_GB->_DB->select('groups', '`notification_key`', "`id`='{$groupID}'");
            $fetchGroup = $this->_GB->_DB->fetchAssoc($getGroup);
            if ($fetchGroup['notification_key'] != null) {
                $this->_GB->sendGroupMessageThroughFCM($fetchGroup['notification_key'], $arrayMessageData);
            }

            $this->_GB->pushToSocket('socket_new_message_group_server', $arrayMessageData);

            $arrayMessage = array(
                'success' => true,
                'message' => "message sent successfully"
            );
            $this->_GB->Json($arrayMessage);
            $this->_GB->_DB->free($getGroup);
        } else {
            $arrayMessageData = array(
                'success' => false,
                'message' => "failed to send message"
            );
            $this->_GB->Json($arrayMessageData);
        }
    }


    public function updateMessageStatus($array)
    {
        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }

        $messageId = isset($array['messageId']) ? $array['messageId'] : null;
        $status = isset($array['status']) ? intval($array['status']) : null;

        if ($messageId === null || $status === null) {
            $arrayMessage = array(
                'success' => false,
                'message' => "messageId and status are required"
            );
            $this->_GB->Json($arrayMessage);
            return;
        }

        if (!in_array($status, array($this->_Sent, $this->_Delivered, $this->_Seen, $this->_Downloaded))) {
            $arrayMessage = array(
                'success' => false,
                'message' => "invalid status value"
            );
            $this->_GB->Json($arrayMessage);
            return;
        }

        $result = $this->tryUpdateStatus($messageId, $status);

        if ($result) {
            $arrayMessage = array(
                'success' => true,
                'message' => "status updated successfully"
            );
        } else {
            $arrayMessage = array(
                'success' => false,
                'message' => "failed to update status"
            );
        }
        $this->_GB->Json($arrayMessage);
    }

    private function tryUpdateStatus($messageId, $status)
    {
        try {
            $safeStatus = $this->_GB->_DB->escapeString($status);
            $update = $this->_GB->_DB->update('messages', "`status` = '$safeStatus'", "`id`='{$messageId}'");
            return $update ? true : false;
        } catch (Exception $e) {
            return false;
        }
    }

    public function updateMessageStatusForUser($array)
    {
        foreach ($array as $key => $value) {
            $array[$key] = $this->_GB->_DB->escapeString(trim($value));
        }

        $messageId = isset($array['messageId']) ? intval($array['messageId']) : null;
        $userId = isset($array['userId']) ? intval($array['userId']) : null;
        $status = isset($array['status']) ? intval($array['status']) : null;

        if ($messageId === null || $userId === null || $status === null) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => "messageId, userId and status are required"
            ));
            return;
        }

        if (!in_array($status, array($this->_Waiting, $this->_Sent, $this->_Delivered, $this->_Seen, $this->_Downloaded))) {
            $this->_GB->Json(array(
                'success' => false,
                'message' => "invalid status value. Use: 0=pending, 1=sent, 2=delivered, 3=seen, 4=downloaded"
            ));
            return;
        }

        $result = $this->upsertMessageStatus($messageId, $userId, $status);

        $this->_GB->Json(array(
            'success' => $result ? true : false,
            'message' => $result ? "status updated successfully" : "failed to update status"
        ));
    }

    private function upsertMessageStatus($messageId, $userId, $status)
    {
        try {
            $existing = $this->_GB->_DB->select('message_status', '*', "`message_id`='{$messageId}' AND `user_id`='{$userId}'");
            
            if ($this->_GB->_DB->numRows($existing) > 0) {
                $this->_GB->_DB->free($existing);
                return $this->_GB->_DB->update('message_status', "`status` = '{$status}'", "`message_id`='{$messageId}' AND `user_id`='{$userId}'");
            } else {
                $this->_GB->_DB->free($existing);
                $data = array(
                    'message_id' => $messageId,
                    'user_id' => $userId,
                    'status' => $status
                );
                return $this->_GB->_DB->insert('message_status', $data);
            }
        } catch (Exception $e) {
            return false;
        }
    }

    public function getMessageStatusForUser($messageId, $userId)
    {
        $messageId = $this->_GB->_DB->escapeString($messageId);
        $userId = $this->_GB->_DB->escapeString($userId);
        
        $result = $this->_GB->_DB->select('message_status', '*', "`message_id`='{$messageId}' AND `user_id`='{$userId}'");
        
        if ($this->_GB->_DB->numRows($result) > 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($result);
            $this->_GB->_DB->free($result);
            return intval($fetch['status']);
        }
        
        $this->_GB->_DB->free($result);
        
        $msgResult = $this->_GB->_DB->select('messages', 'status', "`id`='{$messageId}'");
        if ($this->_GB->_DB->numRows($msgResult) > 0) {
            $fetch = $this->_GB->_DB->fetchAssoc($msgResult);
            $this->_GB->_DB->free($msgResult);
            return intval($fetch['status']);
        }
        
        $this->_GB->_DB->free($msgResult);
        return $this->_Waiting;
    }

    public function getMessageStatuses($messageId)
    {
        $messageId = $this->_GB->_DB->escapeString($messageId);
        $statuses = array();
        
        $result = $this->_GB->_DB->select('message_status', '*', "`message_id`='{$messageId}'");
        
        while ($fetch = $this->_GB->_DB->fetchAssoc($result)) {
            $statuses[] = array(
                'user_id' => intval($fetch['user_id']),
                'status' => intval($fetch['status']),
                'updated_at' => $fetch['updated_at']
            );
        }
        
        $this->_GB->_DB->free($result);
        return $statuses;
    }


}
