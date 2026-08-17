<div id="login-modal" class="modal fade" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title"><?php echo $this->lang->line('text_login'); ?></h2>
            </div>
            <div class="modal-body">
                <div class="alert alert-success" ng-show="notification.showMessage">
                    <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                    <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
                </div>

                <div class="alert alert-danger" ng-show="notification.login_error">
                    <a href="#" class="close" aria-label="close" ng-click="notification.login_error = false">&times;</a>
                    <div ng-bind-html="notification.errors"></div>
                </div>
                
                <div class="alert alert-danger" ng-show="notification.showErrors">
                    <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                    <div ng-bind-html="notification.errors"></div>
                </div>

                <div class="clearfix">
                    <form name="loginForm" method="post" ng-submit="loginForm.$valid && login_user()" novalidate>
                        <div class="form-group">
                            <input type="email" name="email" ng-model="login.email" class="form-control" placeholder="<?php echo $this->lang->line('text_placeholder_email'); ?>" required>
                        </div>
                        <div class="form-group">
                            <input type="password" name="password" ng-model="login.password" class="form-control" placeholder="<?php echo $this->lang->line('text_placeholder_password'); ?>" required>
                        </div>
                        <div class="form-group clearfix">
                            <div class="checkbox pull-left">
                                <?php $remember_token = $this->input->cookie('remember_token'); ?>
                                <label>
                                    <input type="checkbox" name="remember_me" ng-model="login.remember_me" <?php if ($remember_token) echo ' checked="checked"'; ?> />
                                    <?php echo $this->lang->line('remember_me'); ?>
                                </label>
                            </div>
                            <div class="reset-password pull-right">
                                <a class="text-primary" href="<?php echo site_url('c=admin&m=forget_password') ?>" title="<?php echo $this->lang->line('reset_password'); ?>"><?php echo $this->lang->line('forgot_your_password'); ?></a>
                            </div>
                        </div>
                        <input type="submit" value="<?php echo $this->lang->line('login'); ?>" class="btn btn-x2 btn-primary btn-radius">
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>		
<!--End Add New Form-->