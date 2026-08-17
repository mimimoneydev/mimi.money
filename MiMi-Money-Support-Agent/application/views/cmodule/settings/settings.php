<div ng-controller="SettingController as mainctr">
    <div class="header-panel clearfix">
        <h2><?php echo $this->lang->line('global_settings'); ?></h2>   
        <?php version_info(); ?>
    </div>
    
    
    <div class="alert alert-success" ng-show="notification.showMessage">
        <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
        <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
    </div>

    <div class="alert alert-danger" ng-show="notification.showErrors">
        <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
        <div ng-bind-html="notification.errors"></div>
    </div>
    
    <div class="bs-card">
        <div class="settings-container">
            <?php include theme_path('settings/global-settings.php'); ?>
        </div>
    </div>
</div>