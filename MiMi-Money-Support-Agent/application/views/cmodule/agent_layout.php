<!DOCTYPE html>
<html lang="en" ng-app="cmodule">
<head><?php include theme_path('partials/head.php'); ?></head>
<body ng-controller="BodyController" class="operator-panel <?php echo body_classes(); ?>" ng-cloak>
    <div class="page-container {{body_classes}}">        
        <?php include theme_path('partials/agent_sidebar.php'); ?>
        <div class="main-container">
            <?php include theme_path('partials/agent_header.php'); ?>
            <?php include theme_path('partials/notifications.php'); ?>
            <?php include theme_path($this->data['view'] . '.php'); ?>
            <?php include theme_path('agents/workroom.php'); ?>
            <?php include theme_path('agents/anonymous.php'); ?>
            <div ng-show="display_loader" class="cmodule-spinner-loader">
                <div class="cmodule-spinners">
                    <div class="cmodule-spinner-bounce"></div>
                    <div class="cmodule-spinner-bounce"></div>
                    <div class="cmodule-spinner-bounce"></div>
                </div>
            </div>
        </div>
    </div>
    <?php include theme_path('partials/end.php'); ?>
    <?php include theme_path('agents/login-modal.php'); ?>
</body>
</html>