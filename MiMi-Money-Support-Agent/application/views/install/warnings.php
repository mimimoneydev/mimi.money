<div class="container" ng-controller="InstallController">
    <div class="logo-chatbull">
        <a href="<?php echo site_url(); ?>">
            <img src="<?php echo theme_url("images/logo.png"); ?>" alt="chatbull" title="chatbull"> 
        </a>
    </div>
    <div class="installer-content bs-card">
        <h1 class="page-title">Three <strong>easy steps</strong> to <strong>setup</strong> <?php echo plugin_name();?> <strong class="text-danger"><?php echo CHATBULL_VERSION;?></strong></h1>
        <ul class="nav nav-pills">
            <li><a href="#" ng-click="disable_click($event)"  ng-class="{'active':setup_db}"><span class="title">1. Setup Database</span></a></li>
            <li><a href="#" ng-click="disable_click($event)"  ng-class="{'active':setup_user}"><span class="title">2. Admin Account Setup</span></a></li>
            <li><a href="#" ng-click="disable_click($event)"  ng-class="{'active':setup_complete}"><span class="title">3. Configuration Info</span></a></li>
        </ul>
        
        <div class="clearfix" ng-show="visible_area == 'warnings'">
            <form name="setupDb" action="" method="post" ng-submit="setup_database($event) && setupDb.$valid">
                <fieldset>
                    <div class="clearfix">
                        <h3><?php echo $this->data['pagetitle']; ?></h3>
                        <div class="text-danger">
                            <?php foreach ($install_warnings as $warning): ?>
                                <p><?php echo $warning ?></p>
                            <?php endforeach; ?>
                        </div>
                        
                        <div class="alert alert-info" role="alert"><?php echo $this->lang->line('change_write_perm_text'); ?></div>
                    </div>
                </fieldset>
            </form>
        </div>
    </div>
</div>

