<div ng-controller="CannedMessagesController">
    <div class="header-panel clearfix">
        <h2 class="panel-title pull-left"><?php echo $this->lang->line('canned_messages'); ?></h2>
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
                    <th width="60">&nbsp;</th>
                    <th><?php echo $this->lang->line('name'); ?></th>
                    <th><?php echo $this->lang->line('canned_title'); ?></th>
                    <th><?php echo $this->lang->line('canned_description'); ?></th>
                    <th class="text-center action-column"><?php echo $this->lang->line('action'); ?></th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="record in records | orderBy:'title'">
                    <td>
                        <img ng-show="record.profile_pic" class="img-circle avatar" ng-src="{{record.userProfilePic}}" alt="Profile Picture" title="{{record.userName}}" />
                        <span ng-hide="record.profile_pic" ng-style="{backgroundColor: record.profile_color}" class="user-avatar" title="{{record.userName}}">{{record.userName|oneCapLetter}}</span>
                    </td>
                    <td><a href="#" uib-popover-html="record.description | newlines" popover-trigger="'outsideClick'" popover-title="{{record.title}}" ng-click="$event.preventDefault()">{{record.userName}}</a></td>
                    <td><p ng-bind-html="record.title"></p></td>
                    <td><p ng-bind-html="record.description | cut:true:50"></p></td>
                    <td class="text-center action-column">
                        <ul class="list-inline">
                            <li><a href="#formblock" data-toggle="modal" ng-click="editMessage($event, record)" title="<?php echo $this->lang->line('canned_edit'); ?>"><i class="fa fa-pencil fa-lg"></i></a></li>
                            <li><a href="#" ng-confirm-message="<?php echo $this->lang->line('canned_confirm_del'); ?>" ng-confirm-click="deleteMessage(record)" title="<?php echo $this->lang->line('canned_delete'); ?>"><i class="fa fa-trash fa-lg"></i></a></li>
                        </ul>					
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <div class="load-more-block text-center">
        <div ng-show="showNoRecordMessage" role="alert" class="alert alert-info"> <?php echo $this->lang->line('canned_no_result'); ?></div>
        <button class="btn btn-success btn-primary text-center" ng-click="load_more()"><?php echo $this->lang->line('load_more'); ?> <span ng-show="loading"><i class="fa fa-refresh fa-spin"></i></span></button>
    </div>
    
    <a class="btn btn-circle btn-primary btn-fixed" href="#formblock" data-toggle="modal" ng-click="addMessage($event)">+</a>

    <!--Add New Form-->
    <div id="formblock" class="modal fade" data-backdrop="static" data-keyboard="false">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
                    <h2 class="modal-title">{{form_title}}</h2>
                </div>
                <div class="modal-body">
                    <form name="entryForm" method="post" novalidate ng-submit="entryForm.$valid && saveMessage()">
                        <div class="form-group">
                            <input class="form-control" name="title" ng-model="record.title" placeholder="<?php echo $this->lang->line('canned_title'); ?>" required>
                            <span style="color:red" ng-show="entryForm.$submitted || entryForm.title.$dirty">
                                <span ng-show="entryForm.title.$error.required"><?php echo $this->lang->line('canned_validation_required'); ?></span>
                            </span>
                        </div>

                        <div class="form-group">
                            <textarea class="form-control" cols="40" rows="4" name="description" ng-model="record.description" placeholder="<?php echo $this->lang->line('canned_description'); ?>" required></textarea>
                            <span style="color:red" ng-show="entryForm.$submitted || entryForm.description.$dirty">
                                <span ng-show="entryForm.description.$error.required"><?php echo $this->lang->line('canned_validation_required'); ?></span>
                            </span>
                        </div>

                        <div class="form-footer">
                            <button class="btn btn-primary" type="submit"><?php echo $this->lang->line('canned_save'); ?></button>
                            <button class="btn btn-default" type="button" ng-click="resetForm()"  data-dismiss="modal"><?php echo $this->lang->line('canned_cancel'); ?></button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>		
    <!--End Add New Form-->
</div>