<div ng-controller="LanguagesController">
    <div class="header-panel clearfix">
        <h2 class="panel-title pull-left"><?php echo $this->lang->line('languages_settings'); ?></h2>
    </div>
    <div class="alert alert-success" ng-show="notification.showMessage">
        <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
        <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
    </div>

    <div class="alert alert-danger" ng-show="notification.showErrors">
        <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
        <div ng-bind-html="notification.errors"></div>
    </div>

    <div class="table-responsive bs-card">
        <table class="table default-table table-hover">
            <thead>
                <tr>
                    <th></th>
                    <th><?php echo $this->lang->line('lang_name'); ?></th>
                    <th><?php echo $this->lang->line('machine_name'); ?></th>
                    <th class="text-center"><?php echo $this->lang->line('status'); ?></th>
                    <th class="text-center action-column"><?php echo $this->lang->line('action'); ?></th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="record in records | orderBy:'lang_name'">
                    <td>
                        <a href="#modal" ng-click="list_files($event, record)" title="{{record.lang_name}}">
                            <img ng-if="record.lang_flag" class="img-circle avatar" ng-src="{{record.lang_flag}}" alt="{{record.lang_flag}}">
                            <span ng-if="!record.lang_flag" class="user-avatar">{{record.lang_name|oneCapLetter}}</span>
                        </a>
                    </td>
                    <td><a href="#modal" ng-click="list_files($event, record)">{{record.lang_name}} <span ng-if="record.machine_name == 'english'" class="tag label"><?php echo $this->lang->line('default'); ?></span></a></td>
                    <td>{{record.machine_name}}</td>
                    <td class="text-center"><a href="#/" ng-click="toogle_status($event, record)" class="status {{record.lang_status| statusClass:'publish'}}"><i class="fa fa-power-off"></i></a></td>
                    <td class="text-center action-column">
                        <ul class="list-inline" ng-if="record.machine_name != 'english'">
                            <li><a href="#" ng-click="list_files($event, record)" title="<?php echo $this->lang->line('translate'); ?>"> <i class="fa fa-language fa-lg"></i></a></li>
                            <li><a href="#" ng-click="edit_record($event, record)" title="<?php echo $this->lang->line('edit'); ?>"> <i class="fa fa-pencil fa-lg"></i></a></li>
                            <li ng-if="record.lang_status != 'publish'"><a href="#" ng-confirm-message="<?php echo $this->lang->line('confirm_deleted_lang'); ?>" ng-confirm-click="delete_record(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a></li>
                        </ul>					
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    
    <a class="btn btn-circle btn-primary btn-fixed" href="#formblock" data-toggle="modal" ng-click="add_record($event)" title="<?php echo $this->lang->line('addnew'); ?>">+</a>

    <?php include theme_path('languages/form.php'); ?>
</div>