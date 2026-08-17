<div class="page-sidebar" ng-class="{'ct-drawer position-left':windowWidth < 768,'open':leftSidebarDrawerOpen}">
    <div class="user-info-block">
        <div class="dropdown">
            <a href="<?php echo site_url('d=agents&c=agents'); ?>">
                <img ng-show="user.profile_pic" class="img-circle avatar" ng-src="{{user.profile_picture}}" alt="Profile Picture" title="" />
                <span ng-hide="user.profile_pic" ng-style="{backgroundColor: user.profile_color}" class="user-avatar" title="{{user.name}}">{{user.name|oneCapLetter}}</span>
            </a>
            <span class="profile-info">
                <a class="dropdown-toggle" href="<?php echo site_url('d=agents&c=agents'); ?>" data-toggle="dropdown">
                    <span class="user-name"><?php echo $this->current_user->name; ?> <span class="caret"></span></span>
                </a>
                <a class="user-logout" href="<?php echo site_url('c=admin&m=logout'); ?>"><i class="fa fa-sign-out"></i> <?php echo $this->lang->line('logout'); ?></a>
            
                <ul class="dropdown-menu">
                    <li><a href="<?php echo site_url('d=agents&c=agents'); ?>"><?php echo $this->lang->line('edit_profile'); ?></a></li>
                    <li><a href="<?php echo site_url('d=agents&c=agents&m=change_password'); ?>"><?php echo $this->lang->line('change_password'); ?></a></li>
                    <?php if ($this->data['browser_name'] == 'chrome'): ?>
                        <li><a href="#" ng-click="disable_click($event)" class="js-push-button"><?php echo $this->lang->line('enable_push_notification'); ?></a></li>
                    <?php endif; ?>
                    <li><a href="<?php echo site_url('c=admin&m=logout'); ?>"><?php echo $this->lang->line('logout'); ?></a></li>
                </ul>
            </span>
        </div>
    </div>

    <div class="user-panel" ng-controller="ChatlistController">
        <ul role="tablist" class="nav nav-tabs">
            <li class="active"><a href="#recent-chats" aria-controls="recent-chats" role="tab" data-toggle="tab" id="tab-recent-chats"><?php echo $this->lang->line('recent_chats'); ?></a></li>
            <li class=""><a href="#history" aria-controls="history" role="tab" data-toggle="tab" ng-click="get_closed_chats(true)" id="tab-chat-history"><?php echo $this->lang->line('chat_history'); ?></a></li>
        </ul>

        <div class="tab-content">
            <div class="tab-pane active chat-sidebar sidebar-recent-chats" id="recent-chats">
                <h4 class="user-list-title"><?php echo $this->lang->line('open_chats'); ?> ({{recent_chats.length}})</h4>
                <ul class="user-list">
                    <li ng-show="show_sidebar_alert">
                        <div class="alert {{sidebar_alert_class}}">{{sidebar_message}}</div>
                    </li>
                    <li ng-repeat="row in recent_chats" ng-init="canPlaySound(chat_session_id, row.id, row.unread)">
                        <span class="chat-item" id="chat-session-{{row.id}}" ng-click="get_chat_session(row)" ng-class="{'active': chat_session_id == row.id}">
                            <img ng-show="row.profile_pic" class="img-circle avatar" ng-src="{{row.profile_picture}}" alt="{{row.name}}" title="{{row.name}}">
                            <span ng-hide="row.profile_pic" ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                            <span class="profile-info">
                                <span class="user-name" ng-class="{'strong':row.unread > 0}">{{row.name}}</span>
                                <span ng-if="row.country" class="user-location">
                                    <span ng-if="row.city" class="d-inline" ng-bind-html="row.city|do_seprate:', '"></span>
                                    <span class="d-inline" ng-bind-html="row.country"></span>
                                </span>
                            </span>
                            <span ng-show="row.unread > 0" class="message-counter badge badge-warning">{{row.unread}}</span>
                        </span>
                    </li>
                    <li ng-hide="recent_chats.length">
                        <span class="chat-item"><span class="user-name"><?php echo $this->lang->line('no_request'); ?></span></span>
                    </li>
                </ul>

                <div ng-if="settings.enable_agent_initiate_chats == 'yes'">
                    <h4 class="user-list-title" ng-if="anonymous_users.length > 0"><?php echo $this->lang->line('other_anonymous_users'); ?> ({{anonymous_users.length}})</h4>
                    <ul class="user-list">
                        <li ng-repeat="row in anonymous_users" ng-init="is_visitor_connected(row)">
                            <span class="chat-item" ng-click="getAnonymousUser(row)" ng-class="{'active': anonymous_user_id == row.id}">
                                <span ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                                <span class="profile-info">
                                    <span class="user-name">{{row.name}}</span>
                                    <span ng-if="row.country" class="user-location">
                                        <span ng-if="row.city" class="d-inline" ng-bind-html="row.city|do_seprate:', '"></span>
                                        <span class="d-inline" ng-bind-html="row.country"></span>
                                    </span>
                                </span>
                                <!--<span class="message-counter badge badge-warning">12</span>-->
                            </span>
                        </li>
                    </ul>
                </div>
            </div>
            <div class="tab-pane chat-sidebar sidebar-chat-history" id="history">
                <ul class="user-list">
                    <li ng-show="show_sidebar_alert">
                        <div class="alert {{sidebar_alert_class}}">{{sidebar_message}}</div>
                    </li>

                    <li ng-repeat="row in chat_history| orderBy:'-message_id'">
                        <span class="chat-item" ng-click="get_chat_session(row)" ng-class="{'active': chat_session_id == row.id}">
                            <img ng-show="row.profile_pic" class="img-circle avatar" ng-src="{{row.profile_picture}}" alt="{{row.name}}" title="{{row.name}}">
                            <span ng-hide="row.profile_pic" ng-style="{backgroundColor: row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                            <span class="profile-info">
                                <span class="user-name">{{row.name}}</span>
                                <span ng-if="row.country" class="user-location">
                                    <span ng-if="row.city" class="d-inline" ng-bind-html="row.city|do_seprate:', '"></span>
                                    <span class="d-inline" ng-bind-html="row.country"></span>
                                </span>
                            </span>
                        </span>
                    </li>

                    <li ng-show="in_process" class="processing-loader-block">
                        <p class="processing-loader"><i class="fa fa-circle-o-notch fa-spin fa-2x fa-fw"></i> <span><?php echo $this->lang->line('loading'); ?></span></p>
                    </li>

                    <li ng-hide="chat_history.length && !in_process">
                        <span class="chat-item" ng-click="disable_click($event)"><span class="user-name"><?php echo $this->lang->line('no_request'); ?></span></span>
                    </li>                    
                </ul>
            </div>
        </div>
    </div>
</div>
<div ng-if="leftSidebarDrawerOpen" class="ct-backdrop" ng-click="toggleLeftSidebarDrawer($event)"></div>