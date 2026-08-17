<!DOCTYPE html>
<html lang="en" ng-app="cmodule" class="login-page">
<head><?php include theme_path('partials/head.php'); ?></head>
<body ng-controller="BodyController" class="<?php echo body_classes(); ?>" ng-cloak>
    <?php //include theme_path('partials/header.php');?>
    <div class="page-container" ng-cloak>
        <div class="container">
            <?php include theme_path($this->data['view'] . '.php'); ?>
        </div>
    </div>
    <?php include theme_path('partials/end.php'); ?>
</body>
</html>