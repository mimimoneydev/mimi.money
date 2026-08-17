<div ng-controller="UserController as userctr">
    <div class="clearfix"> 
        <div class="header-panel clearfix">
            <h2 class="panel-title pull-left"><?php echo $this->lang->line('agents_and_visitors'); ?></h2>
            <div class="pull-right col-filter"><a href="#/" ng-click="show_filter($event)"><i class="fa fa-filter"></i></a></div>
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
                        <th>&nbsp;</th>
                        <th><?php echo $this->lang->line('name'); ?></th>
                        <th><?php echo $this->lang->line('email'); ?></th>
                        <th class="hidden-sm"><?php echo $this->lang->line('department'); ?></th>
                        <th class="text-center"><?php echo $this->lang->line('role'); ?></th>
                        <th class="text-center"><?php echo $this->lang->line('status'); ?></th>
                        <th class="text-center action-column"><?php echo $this->lang->line('action'); ?></th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="record in records| orderBy:'role'">
                        <td>
                            <img ng-show="record.profile_pic" class="img-circle avatar" ng-src="{{record.profilePic}}" alt="Profile Picture" title="" />
                            <span ng-style="{backgroundColor: record.profile_color}" ng-hide="record.profile_pic" class="user-avatar" title="{{record.name}}">{{record.name|oneCapLetter}}</span>
                        </td>
                        <td>{{record.name}}</td>
                        <td><a href="mailto:{{record.email}}">{{record.email}}</a></td>
                        <td class="hidden-sm">
                            <span ng-repeat="tag in record.tags" class="label label-radius">{{tag.tag_name}}</span>
                        </td>
                        <td class="text-center"><span class="badge badge-outline">{{record.role}}</span></td>
                        <td class="text-center">
                            <a ng-show="record.role == 'agent'" href="#/" ng-click="toogle_status($event, record)" class="item-status btn btn-link" ng-class="{'text-success':record.user_status == 'active'}">
                                <span ng-if="record.user_status == 'active'"><?php echo $this->lang->line('active'); ?></span>
                                <span ng-if="record.user_status != 'active'"><?php echo $this->lang->line('disabled'); ?></span>
                            </a>
                            
                            <span ng-show="record.role != 'agent'" class="item-status">
                                <span ng-if="record.user_status == 'active'"><?php echo $this->lang->line('active'); ?></span>
                                <span ng-if="record.user_status != 'active'"><?php echo $this->lang->line('disabled'); ?></span>
                            </span>
                        </td>
                        <td class="text-center action-column">
                            <ul class="list-inline">
                                <li ng-if="record.role != 'visitor'"><a href="#formblock" data-toggle="modal" ng-click="edit_user(record)" title="<?php echo $this->lang->line('edit'); ?>"> <i class="fa fa-pencil fa-lg"></i></a></li>
                                <li ng-if="record.role != 'visitor'"><a href="#passwordformblock" data-toggle="modal" ng-click="change_password(record)" title="<?php echo $this->lang->line('change_password'); ?>"> <i class="fa fa-unlock-alt fa-lg"></i></a></li>
                                <li ng-if="record.role != 'admin'"><a href="" ng-confirm-message="<?php echo $this->lang->line('confirm_deleted'); ?>" ng-confirm-click="delete_user(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a></li>
                            </ul>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="load-more-block text-center">
            <div ng-show="showNoRecordMessage" role="alert" class="alert alert-info"> <?php echo $this->lang->line('no_record_found'); ?></div>
            <button class="btn btn-primary text-center" ng-click="load_more()"><?php echo $this->lang->line('load_more'); ?> <span ng-show="loading"><i class="fa fa-refresh fa-spin"></i></span></button>
        </div>
        <a class="btn btn-circle btn-primary btn-fixed" href="#formblock" data-toggle="modal" ng-click="add($event)">+</a>
    </div>
    <?php include theme_path('users/form.php'); ?>
    <?php include theme_path('users/change_password.php'); ?>
</div>