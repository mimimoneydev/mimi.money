<?php
/**
 * Created by Abderrahim El imame.
 * Email : abderrahim.elimame@gmail.com
 * Date: 19/02/2016
 * Time: 23:28
 */


// Runtime error reporting is controlled by APP_DEBUG.
$appDebug = filter_var(getenv('APP_DEBUG') ?: 'false', FILTER_VALIDATE_BOOLEAN);
ini_set('display_errors', $appDebug ? '1' : '0');
ini_set('log_errors', '1');
error_reporting($appDebug ? E_ALL : 0);
$appKeySecret = getenv('APP_KEY_SECRET') ?: 'change-me-local-only';

// include the database connection class
include 'config/DataBase.php';
// include the config file
include 'config/Config.php';
// include the SessionsController class
include 'application/controllers/SessionsController.php';
// include the UsersController class
include 'application/controllers/UsersController.php';
// include the MessagesController class
include 'application/controllers/MessagesController.php';
// include the GroupsController class
include 'application/controllers/GroupsController.php';
// include the ProfileController class
include 'application/controllers/ProfileController.php';
// include the Pagination class
include 'application/helpers/Pagination.php';
// include the Helper class
include 'application/helpers/Helper.php';
// include the Security class
include 'application/helpers/Security.php';


$_DB = new DataBase($_Config);
$_DB->connect();
$_DB->selectDB();
$Security = new Security($_DB);
$_GB = new Helper($_DB);
$Groups = new GroupsController($_GB);
$Users = new UsersController($_GB, $Groups);
$Messages = new MessagesController($_GB, $Users);
$Users->setMessagesController($Messages);
$Profile = new ProfileController($_GB);

function parseJsonInput()
{
    $raw = file_get_contents('php://input');
    if (empty($raw)) {
        return;
    }
    $json = json_decode($raw, true);
    if (is_array($json)) {
        $_POST = $json;
    }
}

$cmd = isset($_GET['cmd']) ? $_GET['cmd'] : null;

if (isset($cmd)) {
    if (isset($_SERVER['HTTP_TOKEN'])) {
        $token = $_SERVER['HTTP_TOKEN'];
        $userID = $Users->getUserIdByToken($token);
        $isValidToken = $Users->getSessionToken($token);

    } else {
        $token = null;
        $userID = 0;
        $isValidToken = false;
    }


    if (isset($_SERVER['HTTP_ACCEPT'])) {
        $Accept = $_SERVER['HTTP_ACCEPT'];
    } else {
        $Accept = null;
    }
    switch ($cmd) {


        case 'CheckNetwork':
            if ($isValidToken) {
                $array = array(
                    'connected' => true,
                    'status' => 'Connected'
                );
                $_GB->Json($array);
            } else {
                $array = array(
                    'connected' => false,
                    'status' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'DeleteUserAccount':
            if ($isValidToken) {
                if (isset($_POST['walletAddress'])) {
                    $walletAddress = $_POST['walletAddress'];
                    $country = $_POST['country'];
                    $array = $Users->initDeleteAccount($userID, $walletAddress, $country);
                    $_GB->Json($array);
                } else {
                    // failed to insert row
                    $array = array(
                        'success' => false,
                        'message' => 'Oops! some params are missing.',
                        'walletAddress' => null,
                        'smsVerification' => false,
                        'code' => null
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'DeleteUserAccountConfirmation':
            if ($isValidToken) {
                if (isset($_POST['code'])) {
                    $Users->deleteAccountConfirmation($_POST['code']);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Oops! some params are missing.'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'Join':
            if (isset($_POST)) {
                parseJsonInput();
                $resp = $Users->SignIn($_POST);
                $_GB->Json($resp);
            } else {
                // failed to insert row
                $array = array(
                    'success' => false,
                    'message' => 'Oops! some params are missing.',
                    'walletAddress' => null,
                    'smsVerification' => false,
                    'code' => null,
                    'hasBackup' => false
                );
                $_GB->Json($array);
            }
            break;

        case 'verifyUser':
            if (isset($_POST['code'])) {
                $Users->activateUser($_POST['code']);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Oops! some params are missing.',
                    'userID' => null,
                    'token' => null,
                    'hasBackup' => false,
                    'backup_hash' => null,
                    'hasProfile' => false
                );
                $_GB->Json($array);
            }
            break;

        // Backwards/forwards compatibility with Android endpoint casing.
        case 'VerifyUser':
            $array = array(
                'success' => true,
                'message' => 'Verification disabled (wallet auth).',
                'userID' => $userID,
                'token' => $token,
                'hasBackup' => false,
                'backup_hash' => null,
                'hasProfile' => true
            );
            $_GB->Json($array);
            break;


        case 'resend':
            if (isset($_POST['walletAddress'])) {
                $Users->ResendCode($_POST['walletAddress']);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Oops! some params are missing.'
                );
                $_GB->Json($array);
            }
            break;

        case 'Resend':
            $array = array(
                'success' => true,
                'message' => 'SMS resend disabled (wallet auth).'
            );
            $_GB->Json($array);
            break;

        case 'SendContacts':
            if ($isValidToken) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $Users->compareWalletAddresses($_POST, $userID);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Oops! some params are missing.'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;

        case 'updateRegisteredId':
            if ($isValidToken) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $registeredId = isset($_POST['registeredId']) ? $_POST['registeredId'] : null;
                    if ($registeredId !== null) {
                        $Users->updateRegisteredId($userID, $registeredId);
                    } else {
                        $array = array(
                            'success' => false,
                            'message' => 'registeredId parameter is required',
                            'registered_id' => null
                        );
                        $_GB->Json($array);
                    }
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized',
                    'registered_id' => null
                );
                $_GB->Json($array);
            }

            break;
        case 'GetContact':
            $userId = $_GET['userID'];
            if ($isValidToken) {
                $Users->getContactInfo($userId, $userID);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;

        case 'blockUser':
            $userId = $_GET['userId'];
            if ($isValidToken) {
                $Users->blockUser($userID, $userId);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;

        case 'saveAcceptedCall':
            if ($isValidToken) {

                if (isset($_POST)) {
                    parseJsonInput();
                    $Users->saveAcceptedCall($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Oops! some params are missing.'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'saveEmittedCall':
            if ($isValidToken) {
                if (isset($_POST)) {
                    parseJsonInput();

                    $Users->saveEmittedCall($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Oops! some params are missing.');

                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'saveReceivedCall':
            if ($isValidToken) {
                if (isset($_POST)) {
                    parseJsonInput();

                    $Users->saveReceivedCall($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Oops! some params are missing.');

                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'unBlockUser':
            $userId = $_GET['userId'];
            if ($isValidToken) {
                $Users->unBlockUser($userID, $userId);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'GetGroup':
            $groupID = $_GET['groupID'];

            if ($isValidToken) {
                $Groups->getGroupInfo($groupID,$userID);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'GetGroupMembers':
            $groupID = $_GET['groupID'];
            $groupID = $_DB->escapeString($groupID);


            if ($isValidToken) {
                if ($userID != 0) {
                    $query = " SELECT GM.id ,GM.role,GM.groupID,GM.Deleted,U.id AS userId,U.username,U.wallet_address,U.image,U.status,U.status_date,U.is_activated
                             FROM prefix_users U,prefix_groups G,prefix_group_members GM
                             WHERE
                             CASE
                             WHEN GM.userID = U.id
                             THEN GM.groupID = G.id
                              END
                              AND
                              G.id = {$groupID}
                              AND
                              U.is_activated = 1  ORDER BY GM.id ASC";
                    $query = $_DB->MySQL_Query($query);
                    $Groups->GetGroupMembers($query);

                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;

        case 'EditName':
            if (isset($_POST)) {


                if ($isValidToken) {
                    parseJsonInput();
                    $newstatus = $_POST['newStatus'];
                    $statusID = isset($_POST['statusID']) ? $_POST['statusID'] : $userID;
                    $Users->editName($newstatus, $userID, $statusID);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }

            break;

        case 'EditGroupName':
            if (isset($_POST)) {

                if ($isValidToken) {
                    parseJsonInput();
                    $newstatus = $_POST['newStatus'];
                    $groupID = $_POST['statusID'];
                    $Groups->EditGroupName($newstatus, $groupID);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }

            break;

        case 'ExitGroup':

            $groupID = $_GET['groupID'];

            if ($isValidToken) {
                $Groups->exitGroup($userID, $groupID);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;

        case 'DeleteGroup':

            $groupID = $_GET['groupID'];

            if ($isValidToken) {
                $Groups->deleteGroup($userID, $groupID);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;

        case 'GetStatus':

            $userID = $_DB->escapeString($userID);


            if ($isValidToken) {
                if ($userID != 0) {
                    $query = "
                            SELECT S.*
                           FROM prefix_users U,prefix_status S
                           WHERE
                           S.userID = {$userID}
                           GROUP BY S.id   ORDER BY S.id DESC ";
                    $query = $_DB->MySQL_Query($query);
                    $rows = $_DB->numRows($query);
                    $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                    $_PAG = new Pagination($page,
                        $rows
                        , 6,
                        'routes.php?page=#i#');
                    if ($page > $_PAG->pages) {
                        $_GB->Json(array());
                    } else {
                        $Users->getStatus($query);
                    }
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'EditStatus':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if ($userID != 0) {
                        parseJsonInput();
                        $newstatus = $_POST['newStatus'];
                        $Users->insertStatus($userID, $newstatus);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }

            break;
        case 'UpdateStatus':
            $statusID = $_GET['statusID'];

            if ($isValidToken) {
                $Users->updateStatus($userID, $statusID);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'DeleteStatus':
            if ($isValidToken) {
                $Users->DeleteStatus($userID, $_GET['statusId']);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'DeleteAllStatus':

            $userID = $_DB->escapeString($userID);


            if ($isValidToken) {
                $query = "DELETE S.* FROM  prefix_status S
                           JOIN prefix_users U ON   S.userID = U.id
                           WHERE
                           CASE
                           WHEN S.userID = {$userID}
                           THEN U.id = {$userID}
                            END
                            AND
                            S.status != U.status";
                $query = $_DB->MySQL_Query($query);

                $Users->DeleteAllStatus($query);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }
            break;
        case 'saveMessageGroup':
            if ($isValidToken) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $Messages->saveMessageGroup($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Some Params are Missing'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }

            break;
        case 'sendMessage':

            if ($isValidToken) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $Messages->sendMessage($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Some Params are Missing'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }

            break;

        case 'CreateMessageReceipt':
            // Create a message receipt for offline message tracking
            // This is called by the socket server when a message needs receipt tracking
            $internalSecret = isset($_SERVER['HTTP_X_INTERNAL_SECRET']) ? $_SERVER['HTTP_X_INTERNAL_SECRET'] : null;
            if ($internalSecret === $appKeySecret) {
                if (isset($_POST['messageId']) && isset($_POST['recipientId'])) {
                    parseJsonInput();
                    $messageId = intval($_POST['messageId']);
                    $recipientId = intval($_POST['recipientId']);
                    $status = isset($_POST['status']) ? intval($_POST['status']) : 0;
                    
                    // Check if receipt exists
                    $existing = $_DB->select('message_receipts', '*', "`message_id`='{$messageId}' AND `recipient_id`='{$recipientId}'");
                    if ($_DB->numRows($existing) > 0) {
                        $_DB->free($existing);
                        // Update if new status is higher
                        $_DB->update('message_receipts', "`status` = GREATEST(status, {$status})", "`message_id`='{$messageId}' AND `recipient_id`='{$recipientId}'");
                    } else {
                        $_DB->free($existing);
                        // Insert new receipt
                        $receiptData = array(
                            'message_id' => $messageId,
                            'recipient_id' => $recipientId,
                            'status' => $status
                        );
                        $_DB->insert('message_receipts', $receiptData);
                    }
                    
                    $_GB->Json(array('success' => true, 'message' => 'Receipt created/updated'));
                } else {
                    $_GB->Json(array('success' => false, 'message' => 'messageId and recipientId required'));
                }
            } else {
                $_GB->Json(array('success' => false, 'message' => 'Unauthorized'));
            }
            break;

        case 'updateMessageStatus':

            if ($isValidToken || (isset($_SERVER['HTTP_X_INTERNAL_SECRET']) && $_SERVER['HTTP_X_INTERNAL_SECRET'] === $appKeySecret)) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $Messages->updateMessageStatus($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Some Params are Missing'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }

            break;

        case 'updateMessageStatusForUser':

            if ($isValidToken || (isset($_SERVER['HTTP_X_INTERNAL_SECRET']) && $_SERVER['HTTP_X_INTERNAL_SECRET'] === $appKeySecret)) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $Messages->updateMessageStatusForUser($_POST);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Some Params are Missing'
                    );
                    $_GB->Json($array);
                }
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }

            break;

        case 'createGroup':
            if (isset($_POST)) {
                $userID = $_POST['userID'];

                if ($isValidToken) {
                    if ($userID != 0) {
                        $groupName = $_POST['name'];
                        if (isset($_FILES['image'])) {
                            $imageID = $_GB->uploadImage($_FILES['image']);
                        } else {
                            $imageID = null;
                        }
                        $ids = $_POST['ids'];
                        $date = $_POST['date'];
                        $notificationKey = isset($_POST["notification_key"]) && trim($_POST["notification_key"]) !== ""
                            ? $_POST["notification_key"]
                            : null;
                        $Groups->createGroup($groupName, $imageID, $userID, $ids, substr($date, 1, -1), $notificationKey);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }

            break;


        case 'addMembersToGroup':
            if (isset($_POST)) {


                if ($isValidToken) {
                    if ($userID != 0) {
                        $groupID = $_POST['groupID'];
                        $ids = $_POST['ids'];
                        $Groups->addMembersToGroup($groupID, $ids);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }


            break;

        case 'makeMemberAdmin':
            if (isset($_POST)) {


                if ($isValidToken) {
                    if ($userID != 0) {
                        $groupID = $_POST['groupID'];
                        $id = $_POST['id'];
                        $Groups->makeMemberAdmin($groupID, $id);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }


            break;
        case 'makeAdminMember':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if ($userID != 0) {
                        $groupID = $_POST['groupID'];
                        $id = $_POST['id'];
                        $Groups->makeAdminMember($groupID, $id);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }


            break;
        case 'removeMemberFromGroup':
            if (isset($_POST)) {


                if ($isValidToken) {
                    if ($userID != 0) {
                        $groupID = $_POST['groupID'];
                        $id = $_POST['id'];
                        $Groups->removeMemberFromGroup($groupID, $id);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }


            break;
        case 'getGroups':
            if ($isValidToken) {
                $Groups->getGroups($userID);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }

            break;

        case 'uploadImage':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if ($userID != 0) {
                        if (isset($_FILES['image'])) {
                            $imageHash = $_GB->uploadImage($_FILES['image']);
                        } else {
                            $imageHash = null;
                        }

                        $Profile->uploadProfileImage($imageHash, $userID);
                    } else {
                        $array = array(
                            'success' => false,
                            'userImage' => null,
                            'message' => 'Oops! Something went wrong'
                        );
                        $_GB->Json($array);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }

            break;


        case 'uploadGroupImage':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if (isset($_FILES['image'])) {
                        $imageHash = $_GB->uploadImage($_FILES['image']);
                    } else {
                        $imageHash = null;
                    }
                    $groupID = $_POST['groupID'];
                    $Profile->uploadProfileGroupImage($imageHash, $groupID);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Unauthorized'
                    );
                    $_GB->Json($array);
                }
            }

            break;

        case 'uploadMessagesImage':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if (isset($_FILES['image'])) {
                        $imageHash = $_GB->uploadImage($_FILES['image']);
                    } else {
                        $imageHash = null;
                    }

                    if ($imageHash != null) {
                        $array = array(
                            'success' => true,
                            'url' => $imageHash,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);

                    } else {
                        $array = array(
                            'success' => false,
                            'url' => null,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'url' => null,
                        'videoThumbnail' => null
                    );
                    $_GB->Json($array);
                }
            }

            break;
        case 'uploadMessagesAudio':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if (isset($_FILES['audio'])) {
                        $audioHash = $_GB->uploadAudio($_FILES['audio']);
                    } else {
                        $audioHash = null;
                    }

                    if ($audioHash != null) {

                        $array = array(
                            'success' => true,
                            'url' => $audioHash,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);

                    } else {
                        $array = array(
                            'success' => false,
                            'url' => null,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'url' => null,
                        'videoThumbnail' => null
                    );
                    $_GB->Json($array);
                }
            }

            break;

        case 'uploadMessagesDocument':
            if (isset($_POST)) {

                if ($isValidToken) {
                    if (isset($_FILES['document'])) {
                        $documentHash = $_GB->uploadDocument($_FILES['document']);
                    } else {
                        $documentHash = null;
                    }

                    if ($documentHash != null) {
                        $array = array(
                            'success' => true,
                            'url' => $documentHash,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);

                    } else {
                        $array = array(
                            'success' => false,
                            'url' => null,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'url' => null,
                        'videoThumbnail' => null
                    );
                    $_GB->Json($array);
                }
            }

            break;
        case 'uploadMessagesVideo':
            if (isset($_POST)) {

                if ($isValidToken) {

                    if (isset($_FILES['video'])) {
                        $videoHash = $_GB->uploadVideo($_FILES['video']);
                        // Get auto-generated thumbnail hash from database
                        $autoThumbnailHash = $_GB->getVideoThumbnailHash($videoHash);
                    } else {
                        $videoHash = null;
                        $autoThumbnailHash = null;
                    }

                    if (isset($_FILES['thumbnail'])) {
                        $VideoThumbnailHash = $_GB->uploadVideoThumbnail($_FILES['thumbnail']);
                    } else {
                        $VideoThumbnailHash = $autoThumbnailHash;
                    }

                    if ($videoHash != null) {
                        $url = $_GB->getVideoFileUrl($videoHash);
                        $urlThumbnail = $_GB->getVideoThumbnailFileUrl($VideoThumbnailHash);

                        $array = array(
                            'success' => true,
                            'url' => $videoHash,
                            'videoThumbnail' => $VideoThumbnailHash
                        );
                        $_GB->Json($array);

                    } else {
                        $array = array(
                            'success' => false,
                            'url' => null,
                            'videoThumbnail' => null
                        );
                        $_GB->Json($array);
                    }
                } else {
                    $array = array(
                        'success' => false,
                        'url' => null,
                        'videoThumbnail' => null
                    );
                    $_GB->Json($array);
                }
            }

            break;

        case 'userHasBackup':
            if (isset($_POST)) {
                if ($isValidToken) {
                    $hasBackup = $_POST['hasBackup'];
                    $Users->updateBackup($hasBackup, $userID);
                } else {
                    $array = array(
                        'success' => false,
                        'message' => 'Ops !! something went wrong '
                    );
                    $this->_GB->Json($array);
                }
            }

            break;

        case 'TestFcm':
            $fcmResult = $_GB->testFcmConnection();
            $_GB->Json($fcmResult);
            break;


        case 'GetAppSettings':

            if ($isValidToken) {
                $unitBannerID = $_GB->getSettings('banner_ads_unit_id');
                $adsBannerStatus = $_GB->getSettings('banner_ads_status');
                $appVersion = $_GB->getSettings('app_version');
                if ($adsBannerStatus == 1) {
                    $adsBannerStatus = true;
                } else {
                    $adsBannerStatus = false;
                }


                $unitVideoID = $_GB->getSettings('video_ads_unit_id');
                $appID = $_GB->getSettings('video_ads_app_id');
                $adsVideoStatus = $_GB->getSettings('video_ads_status');
                if ($adsVideoStatus == 1) {
                    $adsVideoStatus = true;
                } else {
                    $adsVideoStatus = false;
                }

                $unitInterstitialID = $_GB->getSettings('interstitial_ads_unit_id');
                $adsInterstitialStatus = $_GB->getSettings('interstitial_ads_status');
                if ($adsInterstitialStatus == 1) {
                    $adsInterstitialStatus = true;
                } else {
                    $adsInterstitialStatus = false;
                }

                $unitWalletBannerID = $_GB->getSettings('wallet_banner_ads_unit_id');
                $adsWalletBannerStatus = $_GB->getSettings('wallet_banner_ads_status');
                if ($adsWalletBannerStatus == 1) {
                    $adsWalletBannerStatus = true;
                } else {
                    $adsWalletBannerStatus = false;
                }

                $unitMoneyBannerID = $_GB->getSettings('money_banner_ads_unit_id');
                $adsMoneyBannerStatus = $_GB->getSettings('money_banner_ads_status');
                if ($adsMoneyBannerStatus == 1) {
                    $adsMoneyBannerStatus = true;
                } else {
                    $adsMoneyBannerStatus = false;
                }

                $unitSpaceBannerID = $_GB->getSettings('space_banner_ads_unit_id');
                $adsSpaceBannerStatus = $_GB->getSettings('space_banner_ads_status');
                if ($adsSpaceBannerStatus == 1) {
                    $adsSpaceBannerStatus = true;
                } else {
                    $adsSpaceBannerStatus = false;
                }

                $array = array(
                    'adsVideoStatus' => $adsVideoStatus,
                    'adsBannerStatus' => $adsBannerStatus,
                    'adsInterstitialStatus' => $adsInterstitialStatus,
                    'unitBannerID' => $unitBannerID,
                    'unitVideoID' => $unitVideoID,
                    'unitInterstitialID' => $unitInterstitialID,
                    'appVersion' => $appVersion,
                    'appID' => $appID,
                    'unitWalletBannerID' => $unitWalletBannerID,
                    'adsWalletBannerStatus' => $adsWalletBannerStatus,
                    'unitMoneyBannerID' => $unitMoneyBannerID,
                    'adsMoneyBannerStatus' => $adsMoneyBannerStatus,
                    'unitSpaceBannerID' => $unitSpaceBannerID,
                    'adsSpaceBannerStatus' => $adsSpaceBannerStatus
                );
                $_GB->Json($array);
            } else {
                $array = array(
                    'success' => false,
                    'message' => 'Unauthorized'
                );
                $_GB->Json($array);
            }

            break;
        case "GetApplicationPrivacy":

            $app_version = $_GB->getSettings("privacy_policy");
            if ($app_version != null) {
                $array = array(
                    "success" => true,
                    "message" => $app_version
                );
                $_GB->Json($array);
            } else {
                $array = array(
                    "success" => false,
                    "message" => "Oops ! Something went wrong"
                );
                $_GB->Json($array);
            }

            break;

        case 'SendCallPush':
            $isInternal = (isset($_SERVER['HTTP_X_INTERNAL_SECRET']) && $_SERVER['HTTP_X_INTERNAL_SECRET'] === $appKeySecret);
            if ($isValidToken || $isInternal) {
                if (isset($_POST)) {
                    parseJsonInput();
                    $recipientId = isset($_POST['recipientId']) ? $_DB->escapeString($_POST['recipientId']) : null;
                    if (empty($recipientId)) {
                        $_GB->Json(array('success' => false, 'message' => 'recipientId is required'));
                        break;
                    }
                    $callData = array(
                        'callerId' => isset($_POST['callerId']) ? $_POST['callerId'] : '',
                        'callerName' => isset($_POST['callerName']) ? $_POST['callerName'] : '',
                        'callerImage' => isset($_POST['callerImage']) ? $_POST['callerImage'] : '',
                        'callType' => isset($_POST['callType']) ? $_POST['callType'] : 'voice',
                        'callId' => isset($_POST['callId']) ? $_POST['callId'] : '',
                        'roomName' => isset($_POST['roomName']) ? $_POST['roomName'] : ''
                    );
                    $result = $_GB->sendCallPush($recipientId, $callData);
                    if ($result !== false) {
                        $_GB->Json(array('success' => true, 'message' => 'call push sent'));
                    } else {
                        $_GB->Json(array('success' => false, 'message' => 'failed to send call push'));
                    }
                } else {
                    $_GB->Json(array('success' => false, 'message' => 'missing params'));
                }
            } else {
                $_GB->Json(array('success' => false, 'message' => 'Unauthorized'));
            }
            break;

        case 'GetIceServers':
            if ($isValidToken) {
                $turnHost = getenv('TURN_HOST') ?: '';
                $turnSecret = getenv('TURN_SECRET') ?: '';
                $turnPort = intval(getenv('TURN_PORT') ?: 3478);
                $turnsPort = intval(getenv('TURNS_PORT') ?: 5349);
                $iceServers = array(
                    array('urls' => 'stun:stun.l.google.com:19302'),
                    array('urls' => 'stun:stun1.l.google.com:19302'),
                    array('urls' => 'stun:stun2.l.google.com:19302')
                );

                if ($turnHost !== '' && $turnSecret !== '') {
                    $ttl = 86400;
                    $username = strval(time() + $ttl);
                    $credential = base64_encode(hash_hmac('sha1', $username, $turnSecret, true));
                    $iceServers[] = array('urls' => 'turn:' . $turnHost . ':' . $turnPort . '?transport=udp', 'username' => $username, 'credential' => $credential);
                    $iceServers[] = array('urls' => 'turn:' . $turnHost . ':' . $turnPort . '?transport=tcp', 'username' => $username, 'credential' => $credential);
                    $iceServers[] = array('urls' => 'turns:' . $turnHost . ':' . $turnsPort . '?transport=tcp', 'username' => $username, 'credential' => $credential);
                }

                $_GB->Json(array(
                    'success' => true,
                    'iceServers' => $iceServers,
                    'message' => ($turnHost !== '' && $turnSecret !== '') ? 'ICE servers with TURN relay' : 'ICE servers with STUN only'
                ));
            } else {
                $_GB->Json(array('success' => false, 'message' => 'Unauthorized'));
            }
            break;


    }

} else {

    $array = array(
        'success' => false,
        'message' => ' Required field(s) is missing'
    );
    $_GB->Json($array);

}
