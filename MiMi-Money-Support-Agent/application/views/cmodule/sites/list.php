<div ng-controller="SitesController as mainctr">
    <div class="header-panel clearfix">
        <h2><?php echo $this->lang->line('sites'); ?></h2> 
        <?php version_info(); ?>

        <?php if (PRODUCT_NAME == 'chatbull'): ?>                
            <div class="sourse-code">
                <h5><?php echo $this->lang->line('application_url'); ?></h5>
                <div class="code-string" select-on-click><?php highlight_string(rtrim(base_url(), '/')); ?></div>
            </div>
        <?php endif; ?>
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
            <div class="settings-inner-container">
                <div class="alert alert-info" ng-if="records.length == 0">
                    <p><?php echo $this->lang->line('no_sites_found'); ?></p>
                </div>

                <div ng-if="records.length > 0" class="table-responsive">
                    <table class="table default-table table-hover">
                        <thead>
                            <tr>
                                <th><?php echo $this->lang->line('site_logo'); ?></th>
                                <th><?php echo $this->lang->line('site_name'); ?></th>
                                <th><?php echo $this->lang->line('site_email'); ?></th>
                                <th><?php echo $this->lang->line('site_url'); ?></th>
                                <th class="text-center"><?php echo $this->lang->line('action'); ?></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr ng-repeat="record in records| orderBy:'site_name'">
                                <td>
                                    <img ng-if="record.site_logo" class="img-circle avatar" ng-src="{{record.site_logo}}" alt="{{record.site_name}}" title="{{record.site_name}}">
                                    <span ng-if="!record.site_logo" class="user-avatar" title="{{record.site_name}}">{{record.site_name|oneCapLetter}}</span>
                                </td>
                                <td>{{record.site_name}}</td>
                                <td>{{record.site_email}}</td>
                                <td>{{record.site_url}}</td>
                                <td class="text-center">
                                    <ul class="list-inline">
                                        <li><a href="<?php echo site_url('c=sites&m=site_setting&id={{record.id}}'); ?>" title="<?php echo $this->lang->line('settings'); ?>"> <i class="fa fa-cogs fa-lg"></i></a></li>
                                        <li><a href="#" ng-click="get_widget($event, record)" title="<?php echo $this->lang->line('chatbox_code'); ?>"> <i class="fa fa-file-code-o fa-lg"></i></a></li>
                                        <li><a href="#" ng-click="edit_record($event, record)" title="<?php echo $this->lang->line('edit'); ?>"> <i class="fa fa-pencil fa-lg"></i></a></li>
                                        <li><a href="#" ng-confirm-message="<?php echo $this->lang->line('confirm_deleted_site'); ?>" ng-confirm-click="delete_record(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a></li>
                                    </ul>					
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <a class="btn btn-circle btn-primary btn-fixed" href="#modal" ng-click="add_record($event)" title="<?php echo $this->lang->line('addnew'); ?>">+</a>
        </div>
    </div>
    
    <?php include theme_path('sites/modal.php'); ?>
</div>