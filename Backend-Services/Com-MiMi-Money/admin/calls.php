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

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);
?>

<div class="box  bg-gray-light">
    <?php
    if (isset($_GET['cmd']) && $_GET['cmd'] == 'calls') {
        ?>

        <center>
            <div class="box-header">
                <label class="btn btn-block btn-info "><strong>Calls List</strong></label>
            </div>
        </center>

        <div class="box-body ">
            <table class="table table-bordered table-striped table-responsive">
                <thead>
                <tr>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Type</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">From</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">To</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Date</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Duration</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Call</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <?php
                $rows = $_DB->CountRows('calls');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'calls.php?cmd=calls&page=#i#');
                $query = $_DB->select('calls', '*', '', "`id` DESC", $_PAG->limit);
                while ($fetch = $_DB->fetchAssoc($query)) {

                    $recipient = $fetch['to_id'];
                    $sender = $fetch['from_id'];

                    $duration = $fetch['duration'];

                    $isAccepted = false;
                    $isReceived = false;
                    $isEmitted = false;

                    if ($fetch['accepted'] != 0) {
                        $isAccepted = true;
                    }
                    if ($fetch['received'] != 0) {
                        $isReceived = true;
                    }
                    if ($fetch['emitted'] != 0) {
                        $isEmitted = true;
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

                    $date = $fetch['date'];
                    echo '<tr>';
                    echo '<td>';
                    echo '<center><label class="label label-success" style="font-size: medium">' . $fetch['type'] . '</label></center>';
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
                    echo '<center><label class=" label label-success" style="font-size: medium">' . $duration . '</label></center>';
                    echo '</td>';
                    echo '<td>';
                    if ($isAccepted) {
                        echo '<center> <label class="label label-success" style="font-size: medium">
                     Accepted</label></center>';
                    } else if ($isEmitted) {
                        echo '<center> <label class="label label-warning" style="font-size: medium">
                      Emitted</label></center>';
                    } else if ($isReceived) {
                        echo '<center> <label class="label label-danger" style="font-size: medium">
                      Received</label></center>';
                    }
                    echo '</td>';
                    echo '<td>';
                    echo '<a type="button"  href="calls.php?cmd=deleteCall&id=' . $fetch['id'] . '" onclick="return checkDelete()"  class="btn btn-block btn-danger"> Delete </a>';
                    echo '</td>';
                    echo '</tr>';
                } ?>
                </tbody>
            </table>
        </div>
        <?php

        echo $_PAG->urls;
    } else if (isset($_GET['cmd'], $_GET['id']) && $_GET['cmd'] == 'deleteCall') {
        $id = $_DB->escapeString($_GET['id']);
        $delete = $_DB->delete('calls', '`id` = ' . $id);
        if ($delete) {
            echo $_GB->ErrorDisplay('The calls Deleted successfully', 'yes');
            echo $_GB->refreshPage('calls.php?cmd=calls', 2);
        } else {
            echo $_GB->ErrorDisplay('Failed to delete this message ,please try again later');
            echo $_GB->refreshPage('calls.php?cmd=calls', 2);
        }
    } else if (isset($_GET['cmd'], $_GET['id']) && $_GET['cmd'] == 'userCalls') {
        $id = $_DB->escapeString($_GET['id']);
        ?>
        <div class="box-body ">
            <table class="table table-bordered table-striped table-responsive">
                <thead>
                <tr>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Type</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">From</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">To</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Date</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Duration</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Call</th>
                    <th style="text-align:center;   color: #0073b7 !important; font-size: 15px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <?php
                $rows = $_DB->CountRows('calls');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'calls.php?cmd=calls&page=#i#');
                $query = $_DB->select('calls', '*', " `from_id` = '{$id}' OR (`to_id` = '{$id}' AND `emitted` = 0)", "`id` DESC", $_PAG->limit);
                while ($fetch = $_DB->fetchAssoc($query)) {

                    $recipient = $fetch['to_id'];
                    $sender = $fetch['from_id'];

                    $duration = $fetch['duration'];

                    $isAccepted = false;
                    $isReceived = false;
                    $isEmitted = false;

                    if ($fetch['accepted'] != 0) {
                        $isAccepted = true;
                    }
                    if ($fetch['received'] != 0) {
                        $isReceived = true;
                    }
                    if ($fetch['emitted'] != 0) {
                        $isEmitted = true;
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

                    $date = $fetch['date'];
                    echo '<tr>';
                    echo '<td>';
                    echo '<center><label class="label label-success" style="font-size: medium">' . $fetch['type'] . '</label></center>';
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
                    echo '<center><label class=" label label-success" style="font-size: medium">' . $duration . '</label></center>';
                    echo '</td>';
                    echo '<td>';
                    if ($isAccepted) {
                        echo '<center> <label class="label label-success" style="font-size: medium">
                     Accepted</label></center>';
                    } else if ($isEmitted) {
                        echo '<center> <label class="label label-warning" style="font-size: medium">
                      Emitted</label></center>';
                    } else if ($isReceived) {
                        echo '<center> <label class="label label-danger" style="font-size: medium">
                      Received</label></center>';
                    }
                    echo '</td>';
                    echo '<td>';
                    echo '<a type="button"  href="calls.php?cmd=deleteCall&id=' . $fetch['id'] . '" onclick="return checkDelete()"  class="btn btn-block btn-danger"> Delete </a>';
                    echo '</td>';
                    echo '</tr>';
                } ?>
                </tbody>
            </table>
        </div>
        <?php

        echo $_PAG->urls;
    } ?>
</div>

<?php

include 'footer.php'
?>
