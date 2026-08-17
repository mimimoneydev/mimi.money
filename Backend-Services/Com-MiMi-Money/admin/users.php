<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/7/16
 * Time: 23:38
 */
include 'header.php';
if ($_GB->getSession('admin') == false) {
    header("location:login.php");
}
?>

<div class="box  bg-gray-light ">
    <?php
    if (isset($_GET['cmd']) && $_GET['cmd'] == 'users') {
        ?>
        <center>
            <div class="box-header">
                <label class="btn btn-block btn-info "><strong>Users List</strong></label>
            </div>
        </center>
        <div class="table-responsive ">
            <!-- USERS LIST -->
            <!--/.box -->

            <ul class="users-list ">

                <?php
                $rows = $_DB->CountRows('users');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'users.php?cmd=users&page=#i#');
                $query = $_DB->select('users', '*', '', '`id` DESC', $_PAG->limit);
                while ($fetch = $_DB->fetchAssoc($query)) {
                    $username = $fetch['username'];
                    $userImage = $fetch['image'];


                    echo '<li>';


                    echo '<div class="box box-widget widget-user">';
                    echo '<div class="widget-user-header bg-blue-gradient">';
                    echo '<h3 class="widget-user-username"> <strong>';
                    if ($username == null) {
                        echo 'No username';
                    } else {
                        echo $fetch['username'];
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
                    echo $fetch['country'];
                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo $fetch['wallet_address'];
                    echo '<h6 class="widget-user-desc">';
                    echo $fetch['status'];
                    echo '</h6>';
                    echo '<a type="button"  href="users.php?cmd=deleteUser&id=' . $fetch['id'] . '" onclick="return checkDelete()"  class="btn btn-block btn-danger"> Delete </a>';

                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo '<div class="col-md-6">';
                    echo '<a type="button"  href="calls.php?cmd=userCalls&id=' . $fetch['id'] . '"   class="btn btn-block btn-info"> Calls </a>';
                    echo '</div>';
                    echo '<div class="col-md-6">';
                    echo '<a type="button"  href="usersBlock.php?cmd=usersBlock&id=' . $fetch['id'] . '"   class="btn btn-block btn-success"> Block List </a>';
                    echo '</div>';
                    echo '</div>';
                    echo '</div>';

                    echo '</li>';
                } ?>
            </ul>
        </div>
        <?php
    } else if (isset($_GET['cmd'], $_GET['id']) && $_GET['cmd'] == 'deleteUser') {
        $id = $_DB->escapeString($_GET['id']);
        $delete = $_DB->delete('users', '`id` = ' . $id);
        if ($delete) {
            echo $_GB->ErrorDisplay('The user Deleted successfully', 'yes');
            echo $_GB->refreshPage('users.php?cmd=users', 1);
        } else {
            echo $_GB->ErrorDisplay('Failed to delete this user ,please try again later');
            echo $_GB->refreshPage('users.php?cmd=users', 1);
        }
    } ?>

</div>
<?php
echo $_PAG->urls;
include 'footer.php'
?>
