<div class="filter-container row-container clearfix" ng-controller="ProfileController">
    <div ng-show="show_pass_form">
        <div class="col-lg-12">
            <h2><?php echo $this->lang->line('change_password'); ?></h2>

            <div class="alert alert-success" ng-show="notification.showMessage">
                <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
            </div>

            <div class="alert alert-danger" ng-show="notification.showErrors">
                <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                <div ng-bind-html="notification.errors"></div>
            </div>

            <div class="bs-card">
                <form name="changePassForm" method="post" ng-submit="update_password($event) && changePassForm.$valid" novalidate>
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