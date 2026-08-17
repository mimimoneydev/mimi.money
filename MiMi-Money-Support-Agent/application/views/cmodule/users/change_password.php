<div id="passwordformblock" class="modal fade" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-passform-model">&times;</button>
                <h2 class="modal-title"><?php echo $this->lang->line('change_password'); ?></h2>
            </div>
            <div class="modal-body">
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
                        <div class="clearfix">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="form-group">
                                        <input class="form-control" type="password" placeholder="Password" ng-model="user.pass" required />
                                    </div>
                                </div>
                                
                                <div class="col-md-6">
                                    <div class="form-group">
                                        <input class="form-control" type="password" placeholder="Confirm Password" ng-model="user.confirm_pass" required />
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="form-footer">
                            <button class="btn btn-primary" type="submit" ng-disabled="!changePassForm.$valid"><?php echo $this->lang->line('submit'); ?></button>
                            <button class="btn btn-default" type="button" ng-click="toggle_password_form(true)" data-dismiss="modal"><?php echo $this->lang->line('submit_cancel'); ?></button>
                        </div>
                    </form>
                </div>
            </div>	
        </div>
    </div>
</div>		
<!--End Add New Form-->