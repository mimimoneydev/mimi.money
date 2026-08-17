<div ng-controller="TagController">
    <div class="header-panel clearfix">
        <h2><?php echo $this->lang->line('departments'); ?></h2>
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
                    <th width="200"><?php echo $this->lang->line('department_name'); ?></th>
                    <th class="col-xs-7"><?php echo $this->lang->line('agents'); ?></th>
                    <th class="text-center"><?php echo $this->lang->line('status'); ?></th>
                    <th class="text-center action-column"><?php echo $this->lang->line('action'); ?></th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="record in records| orderBy:'tag_name'">
                    <td>{{record.tag_name}}</td>
                    <td class="col-xs-7"><span ng-repeat="agent in record.agents" class="label label-radius">{{agent.name}}</span></td>
                    <td class="text-center">
                        <a href="#/" ng-click="toogle_status($event, record)" class="item-status btn btn-link" ng-class="{'text-success':record.tag_status == 'publish'}">
                            <span ng-if="record.tag_status == 'publish'"><?php echo $this->lang->line('active'); ?></span>
                            <span ng-if="record.tag_status != 'publish'"><?php echo $this->lang->line('disabled'); ?></span>
                        </a>
                    </td>
                    <td class="text-center action-column">
                        <ul class="list-inline">
                            <li><a href="#formblock" data-toggle="modal" ng-click="edit(record)" title="<?php echo $this->lang->line('edit'); ?>"> <i class="fa fa-pencil fa-lg"></i></a></li>
                            <li><a href="" ng-confirm-message="<?php echo $this->lang->line('confirm_delete_tag'); ?>" ng-confirm-click="remove(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a></li>
                        </ul>					
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <div class="load-more-block text-center">
        <div ng-show="showNoRecordMessage" role="alert" class="alert alert-info"> <?php echo $this->lang->line('no_record_found'); ?></div>
        <button class="btn btn-success btn-primary text-center" ng-click="load_more()"><?php echo $this->lang->line('load_more'); ?> <span ng-show="loading"><i class="fa fa-refresh fa-spin"></i></span></button>
    </div>

    <a href="#formblock" data-toggle="modal" class="btn btn-circle btn-primary btn-fixed">+</a>
    
    <!--Add New Form-->
    <div id="formblock" class="modal fade" data-backdrop="static" data-keyboard="false">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" ng-click="reset_tag()" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
                    <h2 class="modal-title"><?php echo $this->lang->line('add_new_tag'); ?></h2>
                </div>
                <div class="modal-body">
                    <form name="tagForm" method="post" ng-submit="tagForm.$valid && save_tag($event)" novalidate>
                        <div class="form-group">
                            <input class="form-control" type="text" placeholder="Department Name" ng-model="tag.tag_name" required />
                        </div>

                        <div class="form-footer">
                            <button class="btn btn-primary" type="submit" ng-disabled="!tagForm.$valid"><?php echo $this->lang->line('submit_save'); ?></button>
                            <button class="btn btn-default" type="button" ng-click="reset_tag()"  data-dismiss="modal"><?php echo $this->lang->line('submit_cancel'); ?></button>
                        </div>
                    </form>
                </div>	
            </div>
        </div>
    </div>		
    <!--End Add New Form-->	
</div>