<?php

/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/7/16
 * Time: 00:21
 */
include 'initializer.php';
?>


<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Admin Panel</title>
    <link rel="shortcut icon" type="image/x-icon" href="logo.png"/>
    <!-- Tell the browser to be responsive to screen width -->
    <meta content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" name="viewport">
    <!-- Bootstrap 3.3.6 -->
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.5.0/css/font-awesome.min.css">
    <!-- Ionicons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/ionicons/2.0.1/css/ionicons.min.css">
    <!-- Theme style -->
    <link rel="stylesheet" href="dist/css/AdminLTE.min.css">
    <!-- AdminLTE Skins. We have chosen the skin-blue for this starter
          page. However, you can choose any other skin. Make sure you
          apply the skin class to the body tag so the changes take effect.
    -->
    <link rel="stylesheet" href="dist/css/skins/skin-blue.min.css">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>

    <![endif]-->

    <!-- iCheck for checkboxes and radio inputs -->
    <link rel="stylesheet" href="plugins/iCheck/all.css">
</head>
<?php if ($_GB->getSession('admin') != false) {
?>

<body class="hold-transition  sidebar-mini ">
<div class="wrapper">


    <!-- Main Header -->
    <header class="main-header">

        <!-- Logo -->
        <a href="index.php" class="logo">
            <!-- mini logo for sidebar mini 50x50 pixels -->
            <span class="logo-mini"><b><?php
                    $app_name = $_GB->getSettings("app_name");
                    $rest = substr($app_name, 0, 2);
                    echo $rest; ?></b></span>
            <!-- logo for regular state and mobile devices -->
            <span class="logo-lg"><b><?php echo $_GB->getSettings("app_name") ?></b></span>
        </a>

        <!-- Header Navbar -->
        <nav class="navbar navbar-static-top" role="navigation">
            <!-- Sidebar toggle button-->
            <a href="#" class="sidebar-toggle" data-toggle="offcanvas" role="button">
                <span class="sr-only">Toggle navigation</span>
            </a>
            <!-- Navbar Right Menu -->
            <div class="navbar-custom-menu">
                <ul class="nav navbar-nav">

                    <!-- User Account Menu -->
                    <li class="dropdown user user-menu">
                        <?php
                        $userID = $_GB->getSession('admin');
                        $query = $_DB->select('admins', '*', '`id`=' . $userID);
                        $fetch = $_DB->fetchAssoc($query); ?>
                        <!-- Menu Toggle Button -->
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                            <!-- The user image in the navbar-->

                            <?php
                            $username = $fetch['username'];
                            if ($fetch['image'] != null) { ?>
                                <img src="../image/profile/<?php echo $fetch['image']; ?>" class="user-image"
                                     alt="User Avatar">
                                <!-- hidden-xs hides the username on small devices so only the image appears. -->
                                <span class="hidden-xs"><?php echo $username ?></span>
                                <?php
                            } else {
                                ?>

                                <img src="image_holder_ur_circle.png" class="user-image" alt="User Avatar">
                                <!-- hidden-xs hides the username on small devices so only the image appears. -->
                                <span class="hidden-xs"><?php echo $username ?></span>
                                <?php
                            }
                            ?>
                        </a>

                        <ul class="dropdown-menu">
                            <!-- The user image in the menu -->
                            <li class="user-header">
                                <?php
                                $username = $fetch['username'];
                                if ($fetch['image'] != null) {
                                    ?>
                                    <img src="../image/profile/<?php echo $fetch['image']; ?>" class="img-circle"
                                         alt="User Avatar">

                                    <p>
                                        <?php echo $username ?>
                                    </p>
                                    <?php
                                } else {
                                    ?>
                                    <img src="image_holder_ur_circle.png" class="img-circle"
                                         alt="User Avatar">

                                    <p>
                                        <?php echo $username ?>
                                    </p>
                                    <?php
                                }
                                ?>
                            </li>
                            <!-- Menu Footer-->
                            <li class="user-footer">
                                <div class="pull-left">
                                    <a href="editProfile.php"
                                       class="btn btn-block btn-success bg-blue-gradient">Profile</a>
                                </div>
                                <div class="pull-right">
                                    <a href="signOut.php" class="btn btn-block btn-danger  bg-red-gradient">Sign out</a>
                                </div>
                            </li>
                        </ul>
                    </li>
                </ul>
            </div>
        </nav>
    </header>
    <!-- Left side column. contains the logo and sidebar -->
    <aside class="main-sidebar ">
        <div class="box bg-gray-light "></div>

        <!-- sidebar: style can be found in sidebar.less -->
        <section class="sidebar ">


            <!-- Sidebar user panel (optional) -->
            <div class="user-panel">
                <?php
                $username = $fetch['username'];
                if ($fetch['image'] != null) { ?>
                    <div class="pull-left image">
                        <img src="../image/profile/<?php echo $fetch['image']; ?>" class="img-circle"
                             alt="User Avatar">
                    </div>
                    <div class="pull-left info">
                        <p><?php echo $username ?></p>
                    </div>
                    <?php
                } else {
                    ?>
                    <div class="pull-left image">
                        <img src="image_holder_ur_circle.png" class="img-circle" alt="User Avatar">
                    </div>
                    <div class="pull-left info">
                        <p><?php echo $username ?></p>
                    </div>
                    <?php
                }
                ?>
            </div>

            <!-- /.search form -->

            <!-- Sidebar Menu -->
            <ul class="sidebar-menu ">
                <li class="header">Main Menu</li>
                <!-- Optionally, you can add icons to the links -->
                <li class="active"><a href="index.php"><i class="fa fa-area-chart text-blue "></i> <span
                            class="text-blue ">Statistics data</span></a>
                </li>
                <li><a href="users.php?cmd=users"><i class="fa  fa-user text-blue"></i> <span
                            class="text-blue ">Users</span></a></li>
                <li><a href="groups.php?cmd=groups"><i class="fa  fa-users text-blue"></i> <span class="text-blue ">Groups</span></a>
                </li>
                <li><a href="messages.php?cmd=messages"><i class="fa  fa-envelope-o text-blue"></i> <span
                            class="text-blue ">Messages</span></a></li>

                <li><a href="calls.php?cmd=calls"><i class="fa  fa-phone text-blue"></i> <span
                            class="text-blue ">Calls</span></a></li>
                <li><a href="settings.php"><i class="fa  fa-cog text-blue"></i> <span class="text-blue ">Settings</span></a>
                </li>
            </ul>

            <!-- /.sidebar-menu -->
        </section>

        <!-- /.sidebar -->
    </aside>


    <!-- Content Wrapper. Contains page content -->
    <div class="content-wrapper">
        <?php } else { ?>

        <body class="hold-transition  sidebar-mini " style="background-color: #eee">
        <div class="wrapper">

            <!-- Main Header -->
            <header class="main-header">

                <!-- Logo -->
                <a href="index.php" class="logo">
                    <!-- mini logo for sidebar mini 50x50 pixels -->
                    <span class="logo-mini"><b><?php echo $_GB->getSettings("app_name") ?></b></span>
                    <!-- logo for regular state and mobile devices -->
                    <span class="logo-lg"><b><?php echo $_GB->getSettings("app_name") ?></b></span>
                </a>


                <!-- Header Navbar -->
                <nav class="navbar navbar-static-top" role="navigation">
                </nav>

            </header>
            <div class="content">
                <?php } ?>

