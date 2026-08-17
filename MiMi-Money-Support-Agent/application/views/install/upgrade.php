<div class="container" ng-controller="UpgradeController">
    <div class="logo-chatbull">
        <a href="<?php echo site_url(); ?>">
            <img src="<?php echo theme_url("images/logo.png"); ?>" alt="chatbull" title="chatbull"> 
        </a>
    </div>

    <div class="update-content bs-card">
        <div class="tab-content">
            <h1 class="page-title"><strong><?php echo $this->lang->line('upgrade'); ?></strong> <span class="small pull-right"><a href="<?php echo site_url(); ?>"><i class="fa fa-angle-double-left" area-hidden="true"> <?php echo $this->lang->line('back_to_home'); ?> </i></a></span></h1>

            <div class="clearfix">
                <p><?php echo $this->lang->line('update_introduction_text'); ?></p>
                <p><strong><?php echo $this->lang->line('take_backup_first'); ?></strong></p>
            </div>
            
            <div class="alert alert-success" ng-show="notification.showMessage">
                <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
            </div>
            <div class="alert alert-danger" ng-show="notification.showErrors">
                <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                <div ng-bind-html="notification.errors"></div>
            </div>

            <form name="upgradeForm" novalidate ng-submit="upgradeForm.$valid">
                <div class="form-container">
                    <?php if (PRODUCT_NAME != 'chatbull'): ?>
                        <div ng-hide="verified_license_key || files_updated == 'yes'" class="form-group form-divider animate-slide">
                            <div class="radio">
                                <label class="radio-inline"><input type="radio" name="has_license_key" ng-model="has_license_key" value="yes" ng-change="showOptions()"> <?php echo $this->lang->line('i_have_lkey'); ?></label>
                                <label class="radio-inline"><input type="radio" name="has_license_key" ng-model="has_license_key" value="no" ng-change="showOptions()"> <?php echo $this->lang->line('i_dont_have_lkey'); ?></label>
                            </div>
                        </div>

                        <div class="form-upgrade-fields animate-slide" ng-show="has_license_key == 'yes'">
                            <div ng-hide="verified_license_key" class="form-group animate-slide">
                                <div class="input-group">
                                    <input class="form-control" type="text" ng-model="record.license_key" required />
                                    <span class="input-group-btn"><button class="btn btn-success btn-sn btn-x2 group-btn-middle" ng-click="verify_license_key($event)" ng-disabled="!record.license_key"><?php echo $this->lang->line('verify'); ?></button></span>
                                </div>
                            </div>

                            <div class="clearfix animate-slide" ng-show="verified_license_key && !is_upgraded">
                                <div ng-show="processing"><i class="fa fa-spinner fa-pulse"></i> {{processing_text}}</div>
                                <button class="btn btn-primary" type="submit" ng-click="start_upgrade($event)" ng-disabled="processing"><?php echo $this->lang->line('start_upgrade'); ?></button>
                            </div>
                        </div>

                        <div class="form-upgrade-plugins animate-slide" ng-show="has_license_key == 'no'">
                            <div class="row">
                                <div ng-repeat="row in plugins" class="col-md-3 col-xs-4">
                                    <div class="thumbnail">
                                        <a target="_new" ng-href="{{row.link}}" title="{{row.title}}"><img ng-src="{{row.logo_thumb}}" alt="{{row.title}}" class="img-responsive"></a>

                                        <div class="caption text-center">
                                            <h3><a target="_new" ng-href="{{row.link}}" title="{{row.title}}">{{row.title}}</a></h3>
                                            <h5><strong>{{row.version}}</strong></h5>
                                            <p><span><span><?php echo $this->lang->line('price'); ?>: </span> {{row.price|currency}}</span></p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    <?php endif; ?>

                    <div class="clearfix animate-slide" ng-show="files_updated == 'yes' && !is_upgraded">
                        <div ng-show="processing"><i class="fa fa-spinner fa-pulse"></i> {{processing_text}}</div>
                        <button class="btn btn-primary" type="submit" ng-click="start_update_db($event)" ng-disabled="processing"><?php echo $this->lang->line('update_db'); ?></button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>