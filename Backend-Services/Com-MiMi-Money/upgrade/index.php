<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/10/16
 * Time: 17:13
 */
?>


<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Admin Panel</title>
    <link rel="shortcut icon" type="image/x-icon" href="/admin/logo.png"/>
    <!-- Tell the browser to be responsive to screen width -->
    <meta content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" name="viewport">
    <!-- Bootstrap 3.3.6 -->
    <link rel="stylesheet" href="../install/bootstrap/css/bootstrap.min.css">
    <!-- Font Awesome -->
    <link rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.5.0/css/font-awesome.min.css">
    <!-- Ionicons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/ionicons/2.0.1/css/ionicons.min.css">
    <!-- Theme style -->
    <link rel="stylesheet" href="../install/dist/css/AdminLTE.min.css">
    <!-- AdminLTE Skins. We have chosen the skin-blue for this starter
          page. However, you can choose any other skin. Make sure you
          apply the skin class to the body tag so the changes take effect.
    -->
    <link rel="stylesheet" href="../install/dist/css/skins/skin-blue.min.css">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>

    <![endif]-->

    <!-- iCheck for checkboxes and radio inputs -->
    <link rel="stylesheet" href="../install/plugins/iCheck/all.css">
</head>
<body class="hold-transition  sidebar-mini " style="background-color: #eee">
<div class="wrapper">

    <!-- Main Header -->
    <header class="main-header">

        <!-- Logo -->
        <a href="index.php" class="logo">
            <!-- mini logo for sidebar mini 50x50 pixels -->
            <span class="logo-mini"><b>Upgrade Database Process</b></span>
            <!-- logo for regular state and mobile devices -->
            <span class="logo-lg"><b>Upgrade Database Process</b></span>
        </a>


        <!-- Header Navbar -->
        <nav class="navbar navbar-static-top" role="navigation">
        </nav>

    </header>
    <div class="content">
        <?php

        if (isset($_POST['db_server'], $_POST['db_name'], $_POST['db_user_name'], $_POST['db_user_password'], $_POST['db_prefix'])) {
            $connect = mysqli_connect($_POST['db_server'], $_POST['db_user_name'], $_POST['db_user_password']);
            if ($connect) {
                $selectDB = mysqli_select_db($connect, $_POST['db_name']);
                if ($selectDB) {

                    $lines = file('dataBase.sql');
                    // Loop through each line
                    foreach ($lines as $line) {
                        // Add this line to the current segment
                        if (substr($line, 0, 2) == '--' || $line == '') {
                            continue;
                        }
                        // Add this line to the current segment
                        $templine .= $line;
                        // If it has a semicolon at the end, it's the end of the query
                        if (substr(trim($line), -1, 1) == ';') {
                            // Perform the query
                            mysqli_query($connect, str_replace('wa_', rtrim($_POST['db_prefix'], '_') . '_', $templine)) or print('<center><div class="form-group has-error">  <label class="control-label" for="inputError"><i class="fa fa-times-circle-o"></i> Error performing query \'<strong>' . $templine . '\': ' . mysqli_error($connect) . ' </label></div></center>');
                            // Reset temp variable to empty
                            $templine = '';
                        }
                    }
                    echo '<center><div class="form-group has-success"> <label class="control-label" for="inputSuccess"><i class="fa fa-check"></i> Your script has been upgraded "If u have any issue contact me " </label></div></center>';
                } else {
                    echo '<center><div class="form-group has-error">  <label class="control-label" for="inputError"><i class="fa fa-times-circle-o"></i> Can\'t Connect To Database (' . $_POST['db_name'] . ') </label></div></center>';
                }
            } else {
                echo '<center><div class="form-group has-error">  <label class="control-label" for="inputError"><i class="fa fa-times-circle-o"></i>Can\'t Connect The Server </label></div></center>';
            }
        }
        ?>

        <div class="box box-primary  lockscreen-wrapper" style="max-width: 800px ;margin: 0 auto; margin-top: 5%">
            <div class="box-header with-border">
                <center><h2 class="box-title info">Install Process</h2></center>
            </div>
            <!-- /.box-header -->
            <!-- form start -->

            <form class="form-horizontal" action="" method="POST">

                <div class="box-body">

                    <div class="form-group">
                        <div class="col-lg-12">
                            <input class="form-control" type="text" name="db_server" id="db_server"
                                   placeholder="Host Name"
                                   value="<?php if (isset($_POST['db_server'])) {
                                       echo $_POST['db_server'];
                                   } ?>">
                        </div>


                    </div>
                    <div class="form-group">

                        <div class="col-sm-12">
                            <input class="form-control" type="text" value="<?php if (isset($_POST['db_name'])) {
                                echo $_POST['db_name'];
                            } ?>"
                                   name="db_name" id="db_name" placeholder="Database Name">
                        </div>

                    </div>
                    <div class="form-group">

                        <div class="col-sm-12">
                            <input class="form-control" type="text"
                                   value="<?php if (isset($_POST['db_user_name'])) {
                                       echo $_POST['db_user_name'];
                                   } ?>" name="db_user_name"
                                   id="db_user_name" placeholder="Database Username">
                        </div>

                    </div>
                    <div class="form-group">
                        <div class="col-sm-12">
                            <input class="form-control" type="password"
                                   value="<?php if (isset($_POST['db_user_password'])) {
                                       echo $_POST['db_user_password'];
                                   } ?>"
                                   name="db_user_password" id="db_user_password" placeholder="Database Password">
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-sm-12">
                            <input class="form-control" type="text"
                                   value="<?php if (isset($_POST['db_prefix'])) {
                                       echo $_POST['db_prefix'];
                                   } ?>"
                                   name="db_prefix" id="db_prefix" placeholder="Prefix of tables Ex: wa_">
                        </div>
                    </div>
                </div>


                <!-- /.box-body -->
                <div class="box-footer">
                    <center>
                        <button type="submit" class="btn btn-success  ">Upgrade</button>
                    </center>
                </div>
                <!-- /.box-footer -->
            </form>

        </div>
    </div>


</div>
<!-- /.content-wrapper -->

<!-- ./wrapper -->

<!-- REQUIRED JS SCRIPTS -->

<!-- jQuery 2.2.3 -->
<script src="../install/plugins/jQuery/jquery-2.2.3.min.js"></script>
<!-- Bootstrap 3.3.6 -->
<script src="../install/bootstrap/js/bootstrap.min.js"></script>
<!-- AdminLTE App -->
<script src="../install/dist/js/app.min.js"></script>

<!-- iCheck 1.0.1 -->
<script src="../install/plugins/iCheck/icheck.min.js"></script>
<!-- Optionally, you can add Slimscroll and FastClick plugins.
     Both of these plugins are recommended to enhance the
     user experience. Slimscroll is required when using the
     fixed layout. -->

</body>
</html>
