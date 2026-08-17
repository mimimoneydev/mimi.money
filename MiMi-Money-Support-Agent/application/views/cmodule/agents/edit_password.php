<div class="row-container clearfix" ng-controller="ProfileController">
    <div ng-show="visible_area == 'main-content'" class="col-lg-12">
        <div id="operator-profile-header" class="container-header-secondary">
            <div class="header-fixed clearfix">
                <a href="#" class="btn btn-action hidden-lg hidden-md hidden-sm pull-left" ng-click="toggleLeftSidebarDrawer($event)">
                    <i class="fa fa-bars"></i>
                </a>
                <div class="pull-left user-info-section">
                    <img ng-show="user.profile_pic" class="img-circle avatar" ng-src="{{user.profile_picture}}" alt="Profile Picture" title="" />
                    <span ng-hide="user.profile_pic" ng-style="{backgroundColor: user.profile_color}" class="user-avatar" title="{{user.name}}">{{user.name|oneCapLetter}}</span>
                    <span class="profile-info entry-title">
                        <span class="user-name">{{user.name}}</span>
                        <span class="title-2"><?php echo $this->lang->line('change_password'); ?></span>
                    </span>                    
                </div>
                <div class="pull-right">
                    <ul class="request-list">
                        <li><a href="" ng-click="show_new_requests($event)" class="btn btn-link"><?php echo $this->lang->line('new_requests'); ?> <span ng-show="new_requests_counter > 0">({{new_requests_counter}})</span></a></li>
                        <li><a href="<?php echo site_url('d=agents&c=orequests'); ?>" class="btn btn-link"><?php echo $this->lang->line('offline_requests'); ?>  <span ng-show="offlineRequestsCounter > 0">({{offlineRequestsCounter}})</span></a></li>
                    </ul>
                </div>
            </div>
        </div>

        <div class="operator-profile-wrapper">
            <div class="alert alert-success" ng-show="notification.showMessage">
                <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
            </div>

            <div class="alert alert-danger" ng-show="notification.showErrors">
                <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                <div ng-bind-html="notification.errors"></div>
            </div>

            <div class="clearfix">
                <form name="changePassForm" action="" method="post" ng-submit="update_password($event) && changePassForm.$valid">
                    <div class="form-group input-max">
                        <input class="form-control" type="password" placeholder="Password" ng-model="user.pass" required />
                    </div>

                    <div class="form-group input-max">
                        <input class="form-control" type="password" placeholder="Confirm Password" ng-model="user.confirm_pass" required />
                    </div>

                    <div class="form-footer">
                        <button class="btn btn-primary" type="submit" ng-disabled="!changePassForm.$valid"><?php echo $this->lang->line('submit'); ?></button>
                        <button class="btn btn-default" type="button" ng-click="toggle_password_form(true)"><?php echo $this->lang->line('submit_cancel'); ?></button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>