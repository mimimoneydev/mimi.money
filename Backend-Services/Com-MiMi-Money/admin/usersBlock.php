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
    if (isset($_GET['cmd'], $_GET['id']) && $_GET['cmd'] == 'usersBlock') {
        $id = $_DB->escapeString($_GET['id']);
        ?>


        <center>
            <div class="box-header">
                <label class="btn btn-block btn-info "><strong>Users blocked List</strong></label>
            </div>
        </center>
        <div class="table-responsive ">
            <!-- USERS LIST -->
            <!--/.box -->

            <ul class="users-list ">

                <?php
                $rows = $_DB->CountRows('users_blocked');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'usersBlock.php?cmd=usersBlock&page=#i#');
                $query = $_DB->select('users_blocked', '`to_id`', " `from_id` = '{$id}'", "`id` DESC", $_PAG->limit);
                while ($fetch = $_DB->fetchAssoc($query)) {

                    $user = $Users->getUser($fetch['to_id']);
                    $username = $user['username'];
                    $userImage = $user['image'];


                    echo '<li>';


                    echo '<div class="box box-widget widget-user">';
                    echo '<div class="widget-user-header bg-blue-gradient">';
                    echo '<h3 class="widget-user-username"> <strong>';
                    if ($username == null) {
                        echo 'No username';
                    } else {
                        echo $user['username'];
                    }

                    echo '</strong></h3>';
                    echo '</div>';
                    echo '<div class="widget-user-image">';
                    if ($userImage != null) { ?>
                        <img class="img-circle" alt="User Avatar"
                             src="../image/settings/<?php echo $userImage ?>"
                             onerror="this.src='image_holder_ur_circle.png'">
                    <?php } else { ?>
                        <img class="img-circle" alt="User Avatar" src="image_holder_ur_circle.png">
                        <?php
                    }

                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo $user['country'];
                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo $user['wallet_address'];
                    echo '<h6 class="widget-user-desc">';
                    echo $user['status'];
                    echo '</h6>';
                    echo '</div>';
                    echo '</div>';
                    echo '</li>';
                } ?>
            </ul>
        </div>
        <?php
    } ?>
</div>

<?php

echo $_PAG->urls;
include 'footer.php'
?>
