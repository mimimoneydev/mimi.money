<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 7/8/16
 * Time: 02:00
 */


include 'header.php';

if ($_GB->getSession('admin') != false) {
    header("location:index.php");
}
?>
<?php
if (isset($_POST['username'], $_POST['password'])) {
    $Users->adminLogin($_POST['username'], $_POST['password']);
}
?>
<div class="box box-info lockscreen-wrapper">
    <div class="box-header with-border">
        <h3 class="box-title">Admin Login</h3>
    </div>
    <!-- /.box-header -->
    <!-- form start -->
    <form class="form-horizontal" action="" method="POST">
        <div class="box-body">
            <div class="form-group">
                <label  for="username" class="col-sm-2 control-label">Username</label>

                <div class="col-sm-10">
                    <input type="text" class="form-control" name="username" id="username" placeholder="Username">
                </div>
            </div>
            <div class="form-group">
                <label for="password" class="col-sm-2 control-label">Password</label>

                <div class="col-sm-10">
                    <input type="password" class="form-control" name="password" id="password" placeholder="Password">
                </div>
            </div>
        </div>
        <!-- /.box-body -->
        <div class="box-footer">
            <button type="submit" class="btn btn-info pull-right">Sign in</button>
        </div>
        <!-- /.box-footer -->
    </form>
</div>