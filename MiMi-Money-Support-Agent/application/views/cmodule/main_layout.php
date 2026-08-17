<!DOCTYPE html>
<html lang="en" ng-app="cmodule">
<head><?php include theme_path('partials/head.php'); ?></head>
<body ng-controller="BodyController" class="<?php echo body_classes(); ?>">
    <div class="page-container {{body_classes}}" ng-class="{'sidebar-collapsed':is_sidebar_collapsed}" ng-cloak>  
        <?php include theme_path('partials/header.php'); ?>
        <?php include theme_path('partials/navigation.php'); ?>
        <div class="container">
            <?php include theme_path('partials/notifications.php'); ?>
            <?php include theme_path($this->data['view'] . '.php'); ?>
        </div>
        <?php 
        if($this->data['filter_view']){
            include theme_path($this->data['filter_view']);
        }
        ?>
        <div ng-show="display_loader" class="cmodule-spinner-loader">
            <div class="cmodule-spinners">
                <div class="cmodule-spinner-bounce"></div>
                <div class="cmodule-spinner-bounce"></div>
                <div class="cmodule-spinner-bounce"></div>
            </div>
        </div>
    </div>
    <?php include theme_path('partials/end.php'); ?>
    <?php include theme_path('agents/login-modal.php'); ?>
</body>
</html>