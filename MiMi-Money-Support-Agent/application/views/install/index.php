<div class="container" ng-controller="InstallController">
    <div class="logo-chatbull">
        <a href="<?php echo site_url(); ?>">
            <img src="<?php echo theme_url("images/logo.png"); ?>" alt="chatbull" title="chatbull"> 
        </a>
    </div>
    <div class="installer-content bs-card">
        <div class="installer-header">
            <h1 class="page-title"><?php echo $this->lang->line('install_page_title'); ?></h1>
            <ul class="nav nav-tabs nav-justified">
                <li>
                    <a href="#" ng-click="disable_click($event)"  ng-class="{'active':setup_db}">
                        <span class="text-uppercase"><?php echo $this->lang->line('install_step'); ?> 1</span>
                        <span class="text-capitalize"><?php echo $this->lang->line('install_setup_database'); ?></span>
                    </a>
                </li>
                <li>
                    <a href="#" ng-click="disable_click($event)"  ng-class="{'active':setup_user}">
                        <span class="text-uppercase"><?php echo $this->lang->line('install_step'); ?> 2</span>
                        <span class="text-capitalize"><?php echo $this->lang->line('install_setup_admin'); ?></span>
                    </a>
                </li>
                <li>
                    <a href="#" ng-click="disable_click($event)"  ng-class="{'active':setup_complete}">
                        <span class="text-uppercase"><?php echo $this->lang->line('install_step'); ?> 3</span>
                        <span class="text-capitalize"><?php echo $this->lang->line('install_setup_complete'); ?></span>
                    </a>
                </li>
            </ul>
        </div>

        <div class="tab-content">
            <div class="alert alert-success" ng-show="notification.showMessage">
                <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
            </div>
            <div class="alert alert-danger" ng-show="notification.showErrors">
                <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                <div ng-bind-html="notification.errors"></div>
            </div>

            <div class="clearfix" ng-show="visible_area == 'setup-db'">
                <form name="setupDb" method="post" ng-submit="setupDb.$valid && setup_database($event)" novalidate>
                    <fieldset>
                        <div class="clearfix">
                            <div class="row">
                                <div class="col-sm-7">
                                    <p class="text-instructions"><?php echo $this->lang->line('install_dbform_heading'); ?></p>

                                    <div class="form-group">
                                        <input type="text" name="host" id="host" ng-model="db.host" value="<?php echo $this->input->post('host'); ?>" class="form-control" placeholder="Host Name" required>
                                        <?php echo form_error('host'); ?>
                                    </div>
                                    <div class="form-group">
                                        <input type="text" name="db_name" id="db_name" ng-model="db.db_name" value="<?php echo $this->input->post('db_name'); ?>" class="form-control" placeholder="Database Name" required>
                                        <?php echo form_error('db_name'); ?>
                                    </div>
                                    <div class="form-group">
                                        <input type="text" name="db_user" id="db_user" ng-model="db.db_user" value="<?php echo $this->input->post('db_user'); ?>" class="form-control" placeholder="Database User" required>
                                        <?php echo form_error('db_user'); ?>
                                    </div>
                                    <div class="form-group">
                                        <input type="text" name="db_password" id="db_password" ng-model="db.db_password" value="<?php echo $this->input->post('db_password'); ?>" class="form-control" placeholder="Database Password">
                                        <?php echo form_error('db_password'); ?>
                                    </div>

                                    <div class="form-group">
                                        <button class="btn btn-primary" type="submit" ng-disabled="!setupDb.$valid"><?php echo $this->lang->line('install_btn_continue'); ?></button>
                                    </div>
                                </div>
                                <div class="col-sm-5">
                                    <div class="instructions-block">
                                        <p class="text-uppercase"><?php echo $this->lang->line('install_text_notes'); ?></p>
                                        <p><?php echo $this->lang->line('install_dbform_help1'); ?></p>
                                        <hr>
                                        <p><?php echo $this->lang->line('install_dbform_help2'); ?></p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                </form>
            </div>
            <div class="clearfix" ng-show="visible_area == 'setup-user'">
                <form name="setupUser" method="post" ng-submit="setupUser.$valid && setup_admin($event)" novalidate>
                    <fieldset>
                        <div class="row">
                            <div class="col-sm-7">
                                <p class="text-instructions"><?php echo $this->lang->line('install_userform_heading'); ?></p>
                                <div class="form-group">
                                    <input type="text" name="name" id="name" ng-model="user.name" value="<?php echo $this->input->post('name'); ?>" class="form-control" placeholder="Name" required>
                                    <?php echo form_error('name'); ?>
                                </div>

                                <div class="form-group">
                                    <input type="text" name="display_name" id="display_name" ng-model="user.display_name" value="<?php echo $this->input->post('display_name'); ?>" class="form-control" placeholder="Display Name" required>
                                    <?php echo form_error('display_name'); ?>
                                </div>

                                <div class="form-group">
                                    <input type="email" name="email" id="email" ng-model="user.email" value="<?php echo $this->input->post('email'); ?>" class="form-control" placeholder="Email" required>
                                    <?php echo form_error('email'); ?>
                                </div>
                                <div class="form-group">
                                    <input type="password" name="password" id="password" ng-model="user.password" class="form-control" placeholder="Password" required>
                                    <?php echo form_error('password'); ?>
                                </div>

                                <div class="form-group">
                                    <button class="btn btn-primary" type="submit" ng-disabled="!setupUser.$valid"><?php echo $this->lang->line('install_btn_continue'); ?></button>
                                </div>
                            </div>

                            <div class="col-sm-5">
                                <div class="instructions-block">
                                    <p class="text-uppercase"><?php echo $this->lang->line('install_text_notes'); ?></p>
                                    <p><?php echo $this->lang->line('install_userform_help1'); ?></p>
                                    <hr>
                                    <p><?php echo $this->lang->line('install_userform_help2'); ?></p>
                                    <hr>
                                    <p><?php echo $this->lang->line('install_userform_help3'); ?></p>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                </form>
            </div>

            <div class="clearfix" ng-show="visible_area == 'setup-complete'">
                <h3><?php echo $this->lang->line('install_congratulations'); ?></h3>
                
                <p><?php echo $this->lang->line('install_intalled_suucess'); ?></p>
                <div ng-bind-html="setup_completed_msg"></div>
                <p class="text-instructions"><?php echo $this->lang->line('install_next_todo'); ?></p>
                <p><a href="<?php echo site_url('c=admin'); ?>" target="_blank"><?php echo $this->lang->line('install_click_here'); ?></a> <?php echo $this->lang->line('install_to_login'); ?></p>
                
                <?php if (PRODUCT_NAME == 'chatbull'): ?>
                    <div id="installation" class="visitor-box-settings">  
                        <p class="text-instructions"><?php echo $this->lang->line('install_application_url'); ?></p>
                        <div class="bg-info"><?php highlight_string(rtrim(base_url(), '/')); ?></div>
                    </div>
                    <hr>
                <?php endif; ?>

                <ul class="list-unstyled list-setup-buttons">
                    <li><a href="<?php echo CHATBULL_SITEURL;?>verify-purchase-code" target="_new" class="btn btn-primary btn-outline"><?php echo $this->lang->line('install_btn_purchase_code'); ?></a></li>
                    <li><a href="<?php echo CHATBULL_SITEURL;?>create-apply-chatbox-widget" target="_new" class="btn btn-primary btn-outline"><?php echo $this->lang->line('install_btn_chatbox_widget'); ?></a></li>
                    <?php if (PRODUCT_NAME == 'chatbull'): ?>
                        <li><a href="<?php echo CHATBULL_SITEURL;?>install-mac-application" target="_new" class="btn btn-primary btn-outline"><?php echo $this->lang->line('install_btn_mac_app'); ?></a></li>
                        <li><a href="<?php echo CHATBULL_SITEURL;?>install-windows-application" target="_new" class="btn btn-primary btn-outline"><?php echo $this->lang->line('install_btn_windows_app'); ?></a></li>
                        <li><a href="<?php echo CHATBULL_SITEURL;?>install-android-app" target="_new" class="btn btn-primary btn-outline"><?php echo $this->lang->line('install_btn_android_app'); ?></a></li>
                    <?php endif; ?>
                </ul>
            </div>
        </div>
    </div>
</div>