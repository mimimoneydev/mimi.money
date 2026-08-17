<div class="login-section">
    <div class="login-container">
        <div class="login-content">
            <div class="login-header">
                <a class="logo" href="<?php echo site_url(); ?>" title="{{settings.admin_panel_name}}">
                    <img class="img-responsive" ng-src="{{settings.admin_panel_logo}}" alt="{{settings.admin_panel_name}}" title="{{settings.admin_panel_name}}">
                </a>
            </div>
            
            <?php include theme_path('partials/notifications.php'); ?>

            <div class="login-form">
                <form accept-charset="utf-8" action="<?php echo site_url('c=admin&m=login'); ?>" method="post">
                    <fieldset>
                        <div class="form-group">
                            <input type="email" name="email" id="email" value="<?php echo $this->input->post('email'); ?>" class="form-control" placeholder="<?php echo $this->lang->line('text_placeholder_email'); ?>">
                            <?php echo form_error('email'); ?>
                        </div>
                        <div class="form-group">
                            <input type="password" name="password" id="password" class="form-control" placeholder="<?php echo $this->lang->line('text_placeholder_password'); ?>">
                            <?php echo form_error('password'); ?>
                        </div>
                        <div class="form-group clearfix">
                            <div class="checkbox pull-left">
                                <?php $remember_token = $this->input->cookie('remember_token'); ?>
                                <label>
                                    <input type="checkbox" name="remember_me" <?php if ($remember_token) echo ' checked="checked"'; ?> />
                                    <?php echo $this->lang->line('remember_me'); ?>
                                </label>
                            </div>
                            <div class="reset-password pull-right">
                                <a class="text-primary" href="<?php echo site_url('c=admin&m=forget_password') ?>" title="<?php echo $this->lang->line('reset_password'); ?>"><?php echo $this->lang->line('forgot_your_password'); ?></a>
                            </div>
                        </div>
                        <input type="submit" value="<?php echo $this->lang->line('login'); ?>" class="btn btn-x2 btn-primary btn-radius">
                    </fieldset>
                </form>
            </div>
        </div>
    </div>
</div>
