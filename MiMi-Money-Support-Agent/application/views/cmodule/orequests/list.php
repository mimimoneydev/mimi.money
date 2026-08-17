<div ng-controller="RequestController">
    <div class="header-panel clearfix">
        <h2><?php echo $this->lang->line('offline_requests'); ?></h2>
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
                    <th><?php echo $this->lang->line('status'); ?></th>
                    <th><?php echo $this->lang->line('created_at'); ?></th>
                    <th class="text-center"><?php echo $this->lang->line('action'); ?></th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="record in records">
                    <td>
                        <img ng-show="record.profile_pic" class="img-circle avatar" ng-src="{{record.profile_picture}}" alt="{{record.name}}" title="{{record.name}}" />
                        <span ng-hide="record.profile_pic" ng-style="{backgroundColor: record.profile_color}" class="user-avatar" title="{{record.name}}">{{record.name|oneCapLetter}}</span>
                    </td>
                    <td>{{record.name}}</td>
                    <td><p ng-bind-html="record.message | newlines"></p></td>
                    <td>{{record.request_status| capitalize}}</td>
                    <td>{{record.created_at| datetimeToTimestamp | date:'mediumDate'}}</td>
                    <td class="text-center action-column">
                        <ul class="list-inline">
                            <li><a href="mailto:{{record.email}}?subject=<?php echo $this->lang->line('reply_of_request'); ?>" ng-click="get_conversations(record)"><i class="fa fa-envelope-o"></i></a></li>
                            <li><a href="" ng-confirm-message="<?php echo $this->lang->line('confirm_deleted_orequest'); ?>" ng-confirm-click="delete_record(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a> </li>
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
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
                    <h2 class="modal-title"><?php echo $this->lang->line('request_conversation'); ?></h2>
                </div>
                <div class="modal-body">
                    <div class="row">        
                        <div class="col-xs-12">
                            <div class="chat-container" id="message_box">
                                <div class="chat-row" ng-repeat="row in conversations" ng-class="{'reply': row.senderId == user.id}" ng-mouseover="row.class = ''" on-last-repeat>
                                    <img ng-show="row.senderId != user.id" class="img-circle avatar" ng-src="{{row.profilePic}}" alt="{{row.name}}" title="{{row.name}}">
                                    <div title="{{row.name}}" class="chat-message"><p ng-bind-html="row.chat_message | newlines"></p></div>
                                </div>
                            </div>

                            <form name="offlineForm" id="offlineForm" action="" method="post" ng-submit="send_message($event)">
                                <div class="message-chat-panel">
                                    <div class="form-group chat-message-box">
                                        <textarea cols="40" rows="1" name="message" ng-model="new_message" id="message" placeholder="Message" class="form-control"></textarea>
                                    </div>	
                                    <button id="send-btn" class="btn btn-link btn-lg" ng-disabled="!new_message"><i class="fa fa-paper-plane"></i></button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>