<!doctype html>
<html class="no-js" lang="en" ng-app="cmodule">
<head><?php include theme_path('partials/head.php'); ?></head>
<body ng-controller="MainController" class="installation-page <?php echo body_classes(); ?>">
    <div class="installer-container" ng-cloak>
        <div class="main-installer">
            <?php echo $content; ?>
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
</body>
</html>	
