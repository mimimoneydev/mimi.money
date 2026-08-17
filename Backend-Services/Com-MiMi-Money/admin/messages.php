<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/8/16
 * Time: 00:25
 */

include 'header.php';
if ($_GB->getSession('admin') == false) {
    header("location:login.php");
}
?>

<div class="box  bg-gray-light">
    <?php
    if (isset($_GET['cmd']) && $_GET['cmd'] == 'messages') {
        ?>

        <center>
            <div class="box-header">
                <label class="btn btn-block btn-info "><strong>Messages List</strong></label>
            </div>
        </center>

        <div class="box-body ">
            <table class="table table-bordered table-striped table-responsive">
                <thead>
                <tr>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Message</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Media</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Media actions</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">isGroup</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">From</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">To</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Date</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <?php
                $rows = $_DB->CountRows('messages');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'messages.php?cmd=messages&page=#i#');
                $query = "SELECT M.id,
                            M.Date AS date,
                            M.message ,
                            M.image AS imageFile,
                            M.video AS videoFile,
                            M.thumbnail AS thumbnailFile,
                            M.document AS documentFile,
                            M.audio AS audioFile,
                            M.ConversationID AS conversationID  ,
                           C.recipient  AS recipientID,
                           C.sender AS senderID,
                            M.UserID AS sender,
                           M.groupID 
                  FROM  prefix_messages M

                  LEFT JOIN  prefix_users AS U
                  ON U.id = M.UserID

                  LEFT JOIN  prefix_conversations C
                  ON C.id = M.ConversationID
                  
                    GROUP BY M.id ORDER BY M.id DESC LIMIT  {$_PAG->limit}";

                $query = $_DB->MySQL_Query($query);
                while ($fetch = $_DB->fetchAssoc($query)) {
                    $message = $fetch['message'];

                    if ($fetch['groupID'] != 0) {
                        $isGroup = true;
                    } else {
                        $isGroup = false;
                    }

                    if ($isGroup) {

                        $recipient = 0;
                        if ($fetch['sender'] != 0) {
                            if ($Groups->getUserNameByID($fetch['sender']) == null) {
                                $from = $Groups->getUserPhoneByID($fetch['sender']);
                            } else {
                                $from = $Groups->getUserNameByID($fetch['sender']);
                            }
                        }

                        if ($recipient != 0) {
                            if ($Groups->getUserNameByID($recipient) == null) {
                                $to = $Groups->getUserPhoneByID($recipient);
                            } else {
                                $to = $Groups->getUserNameByID($recipient);
                            }
                        }
                    } else {

                        $recipient = 0;
                        $sender = 0;

                        if ($fetch['senderID'] == $fetch['sender']) {
                            $recipient = $fetch['recipientID'];
                            $sender = $fetch['senderID'];
                        } else {

                            $recipient = $fetch['senderID'];
                            $sender = $fetch['recipientID'];
                        }

                        if ($sender != 0) {
                            if ($Groups->getUserNameByID($sender) == null) {
                                $from = $Groups->getUserPhoneByID($sender);
                            } else {
                                $from = $Groups->getUserNameByID($sender);
                            }
                        }
                        if ($recipient != 0) {
                            if ($Groups->getUserNameByID($recipient) == null) {
                                $to = $Groups->getUserPhoneByID($recipient);
                            } else {
                                $to = $Groups->getUserNameByID($recipient);
                            }
                        }
                    }
                    $date = $fetch['date'];


                    echo '<tr>';
                    echo '<td class="mdl-data-table__cell--non-numeric" style="word-break:break-all;">';
                    if ($message != null) {
                        if ($message == '0x0i3del0x0') {
                            echo '<center><label class="label label-success" style="font-size: medium">A user is created this group </label></center>';
                        } else if ($message == '0x0i5ela0x0') {
                            echo
                            '<center><label class="label label-danger" style="font-size: medium">A user is left this group </label></center>';
                        } else {
                            echo '<center><div >' . $message . '</div></center>';
                        }
                    } else
                        echo '<center> <label class="label label-warning" style="font-size: medium"> Empty message </label></center>';
                    echo '</td>';
                    echo '<td>';
                    if ($fetch['imageFile'] != null && $fetch['imageFile'] != "null") {
                        ?>
                        <center><img style="width: 100px ;height: 100px" alt="User Image"
                                     src="../image/messageImage/<?php echo $fetch['imageFile']; ?>">
                        </center>
                        <?php
                    } else if ($fetch['videoFile'] != null && $fetch['videoFile'] != "null") {
                        ?>
                        <video style="width: 100px ;height: 100px"
                               src="../video/messageVideo/<?php echo $fetch['videoFile']; ?>" controls
                               poster="../video/messageVideoThumbnail/<?php echo $fetch['thumbnailFile']; ?>">
                        </video>

                    <?php } else
                        if ($fetch['documentFile'] != null && $fetch['documentFile'] != "null") {
                            ?>
                            <center>
                                <object class="ben-cherif-data--image ">
                                    <embed
                                        src="../document/messageDocument/<?php echo $fetch['documentFile']; ?>"></embed>
                                </object>

                            </center>
                            <?php
                        } else if ($fetch['audioFile'] != null && $fetch['audioFile'] != "null") {
                            ?>
                            <center>
                                <audio controls>
                                    <source src="../audio/messageAudio/<?php echo $fetch['audioFile']; ?>"
                                            type="audio/mpeg">

                                </audio>
                            </center>
                            <?php
                        } else {
                            echo '<center> <label class="label label-warning" style="font-size: medium">
                 No media </label></center>';
                        }
                    echo '</td>';
                    echo '<td>';
                    if ($fetch['imageFile'] != null && $fetch['imageFile'] != "null") {
                        echo '<center><a type="button"  href="../image/messageImage/' . $fetch['imageFile'] . '"  class="btn btn-block btn-success" download> Download </a> </center>';
                    } else if ($fetch['videoFile'] != null && $fetch['videoFile'] != "null") {
                        echo '<center><a type="button"  href="../video/messageVideo/' . $fetch['videoFile'] . '"  class="btn btn-block btn-success" download> Download </a> </center>';
                    } else if ($fetch['documentFile'] != null && $fetch['documentFile'] != "null") {
                        echo '<center><a type="button"  href="../document/messageDocument/' . $fetch['documentFile'] . '"  class="btn btn-block btn-success" download> Download </a> </center>';
                    } else if ($fetch['audioFile'] != null && $fetch['audioFile'] != "null") {
                        echo '<center><a type="button"  href="../audio/messageAudio/' . $fetch['audioFile'] . '"  class="btn btn-block btn-success" download> Download </a> </center>';
                    }
                    echo '</td>';
                    echo '<td>';
                    if ($isGroup) {
                        echo '<center> <label class="label label-success" style="font-size: medium"> True</label></center>';
                    } else {
                        echo '<center> <label class="label label-danger" style="font-size: medium">
                False</label></center>';
                    }
                    echo '</td>';
                    echo '<td style="word-break:break-all;">';
                    echo '<center><div >' . $from . '</div></center>';
                    echo '</td>';
                    echo '<td style="word-break:break-all;">';
                    echo '<center><div >' . $to . '</div></center>';
                    echo '</td>';
                    echo '<td  style="word-break:break-all;">';
                    echo '<center><div >' . date("F d , Y , g:i a", strtotime($date)) . '</div></center>';
                    echo '</td>';
                    echo '<td>';
                    echo '<a type="button"  href="messages.php?cmd=deleteMessage&id=' . $fetch['id'] . '" onclick="return checkDelete()"  class="btn btn-block btn-danger"> Delete </a>';
                    echo '</td>';
                    echo '</tr>';
                } ?>
                </tbody>
            </table>
        </div>

        <?php
    } else
        if (isset($_GET['cmd'], $_GET['id']) && $_GET['cmd'] == 'deleteMessage') {
            $id = $_DB->escapeString($_GET['id']);
            $delete = $_DB->delete('messages', '`id` = ' . $id);
            if ($delete) {
                echo $_GB->ErrorDisplay('The message Deleted successfully', 'yes');
                echo $_GB->refreshPage('messages.php?cmd=messages', 1);
            } else {
                echo $_GB->ErrorDisplay('Failed to delete this message ,please try again later');
                echo $_GB->refreshPage('messages.php?cmd=messages', 1);
            }
        } ?>
</div>

<?php

echo $_PAG->urls;
include 'footer.php'
?>
