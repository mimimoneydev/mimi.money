<div ng-controller="HeaderController">
    <div ng-show="visible_area == 'new-requests'" class="chat-section">
        <div id="new-requests-header" class="container-header-secondary">
            <div class="header-fixed clearfix">
                <a href="#" class="btn btn-action hidden-lg hidden-md hidden-sm pull-left" ng-click="toggleLeftSidebarDrawer($event)">
                    <i class="fa fa-bars"></i>
                </a>
                <div class="pull-left">                    
                    <h2 class="entry-title"><span class="icon-circle"><i class="fa fa-commenting-o fa-lg" aria-hidden="true"></i></span><?php echo $this->lang->line('new_requests'); ?></h2>
                </div>
                <div class="pull-right">
                    <ul class="request-list">
                        <li><a href="" ng-click="show_new_requests($event)" class="btn btn-link"><?php echo $this->lang->line('new_requests'); ?> <span ng-show="new_requests_counter > 0">({{new_requests_counter}})</span></a></li>
                        <li><a href="<?php echo site_url('d=agents&c=orequests'); ?>" class="btn btn-link"><?php echo $this->lang->line('offline_requests'); ?> <span ng-show="offlineRequestsCounter > 0">({{offlineRequestsCounter}})</span></a></li>
                    </ul>
                </div>
            </div>
        </div>

        <div class="new-requests-wrapper">
            <div class="alert alert-success" ng-show="notification.showMessage">
                <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
            </div>

            <div class="alert alert-danger" ng-show="notification.showErrors">
                <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                <div ng-bind-html="notification.errors"></div>
            </div>

            <div class="row">
                <div class="col-xs-12">
                    <div class="user-container">
                        <div class="user-entry" ng-repeat="row in new_requests">
                            <div class="user-head clearfix" ng-show="row.request_type == 'new'" ng-init="rand_color = getColor()">
                                <div class="user-photo">
                                    <img ng-show="row.profile_pic" class="img-circle avatar" ng-src="{{row.profile_picture}}" alt="{{row.name}}" title="{{row.name}}">
                                    <span ng-hide="row.profile_pic" ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                                </div>
                                <div class="user-details">
                                    <h5><span class="user-name"><a href="#/">{{row.name}}</a></span></h5>
                                    <p ng-show="row.tag_name"><span class="label label-default">{{row.tag_name}}</span></p>
                                </div>
                            </div>

                            <div class="user-head-block clearfix" ng-if="row.request_type == 'forward'" ng-init="rand_color = getColor()">
                                <div class="user-head clearfix pull-left pull-none-sm">
                                    <div class="user-photo">
                                        <img ng-show="row.agent_profile_pic" class="img-circle avatar" ng-src="{{row.agent_profile_picture}}" alt="{{row.agent_name}}" title="{{row.agent_name}}">
                                        <span ng-hide="row.agent_profile_pic" ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.agent_name}}">{{row.agent_name|oneCapLetter}}</span>
                                    </div>
                                    <div class="user-details">
                                        <h5><span class="user-name"><a href="#/">{{row.agent_name}}</a></span></h5>
                                        <p><span class="badge badge-outline"><?php echo $this->lang->line('agent'); ?></span></p>
                                    </div>
                                </div>

                                <div class="user-head clearfix forwarded-indecator pull-left pull-none-sm">
                                    <div class="forward-icon hidden-sm"><i class="fa fa-angle-double-right"></i></div>
                                    <div class="forward-icon visible-sm"><i class="fa fa-angle-double-down"></i></div>
                                    <p><?php echo $this->lang->line('forwarded'); ?></p>
                                </div>

                                <div class="user-head clearfix pull-left pull-none-sm" ng-init="rand_color = getColor()">
                                    <div class="user-photo">
                                        <img ng-show="row.profile_pic" class="img-circle avatar" ng-src="{{row.profile_picture}}" alt="{{row.name}}" title="{{row.name}}">
                                        <span ng-hide="row.profile_pic" ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                                    </div>
                                    <div class="user-details">
                                        <h5><span class="user-name"><a href="#/">{{row.name}}</a></span></h5>
                                        <p ng-show="row.tag_name"><span class="label label-default">{{row.tag_name}}</span></p>
                                    </div>
                                </div>
                            </div>
                            <div class="user-content clearfix">
                                <p ng-bind-html="row.message | newlines"></p>
                                <p><a href="#" class="btn btn-sm btn-primary" ng-click="accept_request($event, row)"><?php echo $this->lang->line('accept'); ?></a></p>
                            </div>
                        </div>

                        <div ng-hide="new_requests.length" role="alert" class="alert alert-info"> <?php echo $this->lang->line('no_record_found'); ?></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>