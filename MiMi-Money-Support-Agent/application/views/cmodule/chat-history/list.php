<div ng-controller="ChathistoryController">
    <div class="header-panel clearfix">
        <h2 class="panel-title pull-left"><?php echo $this->lang->line('chat_history'); ?></h2>
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
                    <th><?php echo $this->lang->line('visitor_name'); ?></th>
                    <th><?php echo $this->lang->line('message'); ?></th>
                    <th class="text-center"><?php echo $this->lang->line('status'); ?></th>
                    <th class="text-center action-column"><?php echo $this->lang->line('action'); ?></th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="record in records">
                    <td>
                        <img ng-show="record.profile_pic" class="img-circle avatar" ng-src="{{record.visitorProfilePic}}" alt="Profile Picture" title="" />
                        <span ng-hide="record.profile_pic" ng-style="{backgroundColor: record.profile_color}" class="user-avatar" title="{{record.visitorName}}">{{record.visitorName|oneCapLetter}}</span>
                    </td>
                    <td><a href="#formblock" data-toggle="modal" ng-click="get_conversations(record)">{{record.visitorName}}</a></td>
                    <td><p ng-bind-html="record.chat_message | cut:true:50:' ...'"></p></td>
                    <td class="text-center">{{record.session_status| capitalize}}</td>
                    <td class="text-center action-column">
                        <ul class="list-inline">
                            <li><a href="#formblock" data-toggle="modal" ng-click="get_conversations(record)" title="<?php echo $this->lang->line('conversation'); ?>"> <i class="fa fa-eye fa-lg"></i></a></li>
                            <li><a href="" ng-confirm-message="<?php echo $this->lang->line('confirm_deleted_chat'); ?>" ng-confirm-click="delete_record(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a> </li>
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

    <div id="formblock" class="modal fade" data-backdrop="static" data-keyboard="false">
        <div class="modal-dialog modal-lg" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
                    <h2 class="modal-title"><?php echo $this->lang->line('chat_conversation'); ?></h2>
                    <div class="list-action">
                        <a ng-if="visitor.email" href="mailto:{{visitor.email}}"><span class="label label-radius">{{visitor.email}}</span></a>
                        <span ng-if="visitor.ip_address" title="{{visitor.ip_address}}" class="label label-radius"><?php echo $this->lang->line('ip'); ?> - {{visitor.ip_address}}</span>
                        <span ng-if="visitor.started_at" class="label label-radius">{{visitor.started_at| datetimeToTimestamp | date:'medium'}}</span>
                        <a ng-if="visitor.page_title" target="_blank" ng-href="{{visitor.page_url}}" title="{{visitor.page_title}}"><span class="label label-radius">{{visitor.page_title}}</span></a>
                    </div>
                </div>
                <div class="modal-body">
                    <div class="row">        
                        <div class="col-xs-12">
                            <div class="chat-container {{settings.operator_chat_theme}}" id="message_box">
                                <div ng-repeat="row in conversations" id="chat-row-{{$index}}" class="chat-row" ng-class="{'reply': row.sender_id != visitor.id}" on-last-repeat>
                                    <div class="chat-row-avatar">
                                        <img ng-show="row.profile_pic && row.sender_id == visitor.id" class="img-circle avatar" ng-src="{{row.profile_picture}}" alt="{{row.name}}" title="{{row.name}}">
                                        <span ng-hide="row.profile_pic || row.sender_id != visitor.id" ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                                    </div>
                                    <div title="{{row.name}}" class="chat-message"><p ng-bind-html="row.chat_message | newlines | smilies"></p></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>	
            </div>
        </div>
    </div>
</div>