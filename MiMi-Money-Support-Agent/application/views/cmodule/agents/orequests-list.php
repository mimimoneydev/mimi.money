<div ng-controller="RequestController">
    <div ng-show="visible_area == 'main-content'" class="row">        
        <div class="col-xs-12">
            <div id="offline-requests-header" class="container-header-secondary">
                <div class="header-fixed clearfix">
                    <a href="#" class="btn btn-action hidden-lg hidden-md hidden-sm pull-left" ng-click="toggleLeftSidebarDrawer($event)">
                        <i class="fa fa-bars"></i>
                    </a>
                    <div class="pull-left">
                        <h2 class="entry-title"><span class="icon-circle"><i class="fa fa-envelope-o fa-lg" aria-hidden="true"></i></span><?php echo $this->lang->line('offline_requests'); ?></h2>
                    </div>
                    <div class="pull-right">
                        <ul class="request-list">
                            <li><a href="" ng-click="show_new_requests($event)" class="btn btn-link"><?php echo $this->lang->line('new_requests'); ?> <span ng-show="new_requests_counter > 0">({{new_requests_counter}})</span></a></li>
                            <li><a href="<?php echo site_url('d=agents&c=orequests'); ?>" class="btn btn-link"><?php echo $this->lang->line('offline_requests'); ?>  <span ng-show="offlineRequestsCounter > 0">({{offlineRequestsCounter}})</span></a></li>
                        </ul>
                    </div>
                </div>
            </div>

            <div class="offline-requests-wrapper">
                <div class="alert alert-success" ng-show="notification.showMessage">
                    <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                    <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
                </div>

                <div class="alert alert-danger" ng-show="notification.showErrors">
                    <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                    <div ng-bind-html="notification.errors"></div>
                </div>

                <div class="user-container">
                    <div class="user-entry" ng-repeat="record in records">
                        <div class="user-head clearfix">
                            <div class="user-photo" ng-init="rand_color = getColor()">
                                <img ng-show="record.profile_pic" class="img-circle avatar" ng-src="{{record.profile_picture}}" alt="Profile Picture" title="{{record.name}}">
                                <span ng-hide="record.profile_pic" ng-style="{backgroundColor: record.profile_color}" class="user-avatar" title="{{record.name}}">{{record.name|oneCapLetter}}</span>
                            </div>
                            <div class="user-details">
                                <h5><span class="user-name"><a href="#/">{{record.name}}</a></span></h5>
                                <p ng-show="record.email">{{record.email}}</p>
                            </div>
                        </div>
                        <div class="user-content clearfix">
                            <p ng-bind-html="record.message | newlines"></p>
                            <p><a href="mailto:{{record.email}}" class="btn btn-sm btn-primary" ng-click="get_conversations(record)">Send Mail</a></p>
                        </div>
                    </div>

                    <div ng-hide="records.length" role="alert" class="alert alert-info"> <?php echo $this->lang->line('no_record_found'); ?></div>
                </div>

                <div class="load-more-block text-center">
                    <div ng-show="showNoRecordMessage && records.length" role="alert" class="alert alert-info"> <?php echo $this->lang->line('no_record_found'); ?></div>
                    <button class="btn btn-success btn-primary text-center" ng-click="load_more()"><?php echo $this->lang->line('load_more'); ?> <span ng-show="loading"><i class="fa fa-refresh fa-spin"></i></span></button>
                </div>
            </div>

            <div id="offlineformblock" class="additional-section modal fade">
                <div class="modal-table">
                    <div class="modal-cell">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
                                <h2 class="modal-title"><?php echo $this->lang->line('request_conversation'); ?></h2>
                            </div>
                            <div class="modal-body">
                                <div class="row">        
                                    <div class="col-xs-12">
                                        <div class="chat-container" id="offline_message_box">
                                            <div class="chat-row" ng-repeat="row in conversations" ng-class="{'reply': row.senderId == user.id}" ng-mouseover="row.class = ''" on-last-repeat>
                                                <img ng-show="row.senderId != user.id" class="img-circle avatar" ng-src="{{row.profilePic}}" alt="{{row.name}}" title="{{row.name}}">
                                                <div class="chat-message" ng-bind-html="'<p>' + row.message + '</p>'"></div>
                                            </div>
                                        </div>

                                        <form name="offlineReplyForm" id="offline-reply-form" action="" method="post" ng-submit="send_message($event)">
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
        </div>
    </div>
</div>