<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/8/16
 * Time: 00:28
 */

include 'header.php';
if ($_GB->getSession('admin') == false) {
    header("location:login.php");
}
?>
<div class="box  bg-gray-light ">
    <?php
    if (isset($_GET['cmd']) && $_GET['cmd'] == 'groups') {
        ?>

        <center>
            <div class="box-header">
                <label class="btn btn-block btn-info "><strong>Groups List</strong></label>
            </div>
        </center>


        <div class="table-responsive ">
            <!-- USERS LIST -->
            <!--/.box -->

            <ul class="users-list ">

                <?php
                $rows = $_DB->CountRows('groups');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'groups.php?cmd=groups&page=#i#');
                $query = $_DB->select('groups', '*', '', '`id` DESC', $_PAG->limit);
                while ($fetch = $_DB->fetchAssoc($query)) {
                    $groupName = $fetch['name'];
                    $groupImage = $fetch['image'];
                    $id = $fetch['id'];


                    echo '<li>';


                    echo '<div class="box box-widget widget-user">';
                    echo '<div class="widget-user-header bg-blue-gradient">';
                    echo '<h3 class="widget-user-username" style="word-break:break-all;"> <strong> ';
                    if ($groupName == null) {
                        echo 'No name';
                    } else {
                        echo $fetch['name'];
                    }

                    echo ' </strong></h3>';
                    echo '</div>';
                    echo '<div class="widget-user-image">';
                    if ($groupImage != null) { ?>
                        <img class="img-circle" alt="User Avatar"
                             src="../image/settings/<?php echo $groupImage ?>" onerror="this.src='image_holder_ur_circle.png'">
                    <?php } else { ?>
                        <img class="img-circle" alt="User Avatar"  src="image_holder_gr_circle.png">
                        <?php
                    }

                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo '<h5 class="widget-user-desc">';
                    echo '<strong>Number of members : </strong> ' . $_DB->CountRows('group_members', "`groupID` = {$id}");
                    echo '</h5>';
                    echo '<a  type="button" href="groups.php?cmd=groupMembers&groupID=' . $id . '" class="btn btn-block btn-success">View</a>';
                    echo '<a type="button" onclick="return checkDelete()"  href="groups.php?cmd=deleteGroup&groupID=' . $id . '" class="btn btn-block btn-danger"> Delete </a>';
                    echo '</div>';
                    echo '</div>';
                    echo '</li>';
                } ?>
            </ul>
        </div>
        <?php
    } else if (isset($_GET['cmd'], $_GET['groupID']) && $_GET['cmd'] == 'groupMembers') {
        $groupID = $_DB->escapeString($_GET['groupID']);
        ?>

        <center>
            <div class="box-header">
                <h3 class="box-title">Members List</h3>
            </div>
        </center>
        <div class="table-responsive ">
            <!-- USERS LIST -->
            <!--/.box -->

            <ul class="users-list ">

                <?php
                $rows = $_DB->CountRows('groups');
                $page = (isset($_GET['page']) && !empty($_GET['page'])) ? $Security->MA_INT($_GET['page']) : 1;
                $_PAG = new Pagination($page, $rows, 20, 'groups.php?cmd=groupMembers&page=#i#');
                $query = " SELECT  GM.id ,GM.role,GM.groupID,U.id AS userId,U.username,U.country,U.phone,U.image

                             FROM prefix_users U,prefix_groups G,prefix_group_members GM
                             WHERE
                             CASE
                             WHEN GM.userID = U.id
                             THEN GM.groupID = G.id
                              END
                              AND
                              G.id = {$groupID}
                             GROUP BY U.id   ORDER BY U.id DESC LIMIT {$_PAG->limit}";
                $query = $_DB->MySQL_Query($query);
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

                    echo ' </strong></h3>';
                    echo '</div>';
                    echo '<div class="widget-user-image">';
                    if ($userImage != null) { ?>
                        <img class="img-circle" alt="User Avatar"
                             src="../image/settings/<?php echo $userImage ?>" onerror="this.src='image_holder_ur_circle.png'">
                    <?php } else { ?>
                        <img class="img-circle" alt="User Avatar"  src="image_holder_ur_circle.png">
                        <?php
                    }

                    echo '</div>';
                    echo '<div class="box-footer">';
                    echo $fetch['country'];
                    echo '</div>';
                    echo '<strong>Role : </strong>' . $fetch['role'];
                    echo '<div class="box-footer">';
                    echo $fetch['wallet_address'];
                    echo '<h6 class="widget-user-desc">';
                    echo $fetch['status'];
                    echo '</h6>';
                    echo '</div>';
                    echo '</div>';

                    echo '</li>';
                } ?>
            </ul>
        </div>


    <?php } else if (isset($_GET['cmd'], $_GET['groupID']) && $_GET['cmd'] == 'deleteGroup') {
        $id = $_DB->escapeString($_GET['groupID']);
        $delete = $_DB->delete('groups', '`id` = ' . $id);
        if ($delete) {
            echo $_GB->ErrorDisplay('The group Deleted successfully', 'yes');
            echo $_GB->refreshPage('groups.php?cmd=groups', 1);
        } else {
            echo $_GB->ErrorDisplay('Failed to delete this group please try again later');
            echo $_GB->refreshPage('groups.php?cmd=groups', 1);
        }
    } ?>
</div>

<?php
echo $_PAG->urls;
include 'footer.php'
?>
