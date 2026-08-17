<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/11/16
 * Time: 03:08
 */
include 'header.php';

if ($_GB->getSession('admin') == false) {
    header("location:login.php");
}
?>
<?php

$userID = $_GB->getSession('admin');
$query = $_DB->select('admins', '*', '`id`=' . $userID);
$fetch = $_DB->fetchAssoc($query);
$admin_name = $fetch['username'];
$admin_image = $fetch['image'];
$oldPassword = $fetch['password'];


if (isset($_POST['admin_name'])) {

    foreach ($_POST as $key => $value) {
        $_POST[$key] = $_DB->escapeString(trim($value));
    }

    if (md5($_POST['old_admin_password']) != $oldPassword) {
        echo $_GB->ErrorDisplay('Your old password is not correct');
    } else {

        if (isset($_FILES['input_admin_image'])) {
            $imageHash = $_GB->uploadAdminImage($_FILES['input_admin_image']);
        } else {
            $imageHash = null;
        }

        $fields = "`username` = '" . $_POST['admin_name'] . "'";
        if (!empty($_POST['admin_password'])) {
            $fields .= ",`password` = '" . md5($_POST['admin_password']) . "'";
        }
        $fields .= ",`image` ='" . $imageHash . "'";
        $update = $_DB->update('admins', $fields, "`id` = {$userID}");
        if ($update) {
            echo $_GB->ErrorDisplay('Your information are updated successfully', 'yes');
            echo $_GB->refreshPage('editProfile.php', 1);
        } else {
            echo $_GB->ErrorDisplay('Failed to update your information', 'no');
        }
    }
}

?>
<?php
if ($admin_image != null) {
    ?>


    <div class="box  " style="max-width: 600px ;margin: 0 auto; margin-top: 10%">
    <center>
        <div class="box-header with-border">
            <h3 class="box-title">Admin Profile</h3>
        </div>
    </center>
    <!-- /.box-header -->

    <center>
    <div class="box-body ">
    <!-- form start -->
    <form role="form" action="" method="POST" enctype="multipart/form-data">
    <center>
        <div class="mdl-textfield mdl-js-textfield mdl-textfield--floating-label input-card-settings">
            <img style="width: 100px ;height: 100px" alt="User Image" class="img-circle"
                 src="../image/profile/<?php echo $admin_image; ?>">
            <i><input class="mdl-textfield__input" type='file' id="input_admin_image"
                      name="input_admin_image"/></i>
        </div>
    </center>

    <?php
} else {
    ?>

    <div class="box  " style="max-width: 600px ;margin: 0 auto; margin-top: 10%">
    <center>
        <div class="box-header with-border">
            <h3 class="box-title">Admin Profile</h3>
        </div>
    </center>
    <!-- /.box-header -->

    <center>
    <div class="box-body " >
    <form role="form" action="" method="POST" enctype="multipart/form-data">
    <center>
        <div
            class="mdl-textfield mdl-js-textfield mdl-textfield--floating-label input-card-settings">
            <img style="width: 100px ;height: 100px" alt="User Image" class="img-circle"
                 src="image_holder_ur_circle.png">
            <input class="mdl-textfield__input" type='file' id="input_admin_image"
                   name="input_admin_image"/>

        </div>
    </center>
    <?php
}
?>


    <div class="form-group">
        <label for="admin_name">Admin name</label>
        <input type="text" class="form-control" name="admin_name" id="admin_name"
               value="<?php echo $admin_name ?>" placeholder="Admin name" required>

    </div>


    <div class="form-group">

        <label for="old_admin_password">Old Admin password</label>
        <input type="text" class="form-control"
               name="old_admin_password" id="old_admin_password" placeholder="Old password"
               required>
    </div>
    <div class="form-group">

        <label for="admin_password">New password</label>
        <input type="text" class="form-control"
               name="admin_password" id="admin_password" placeholder="New Password"
               required>
    </div>


    <center>
        <button type="submit"
                class="btn  btn-success btn-lg">
            <i>Save Changes</i></button>
    </center>
    </form>
    </div>
    </center>
    </div>

<?php
include "footer.php";
?>