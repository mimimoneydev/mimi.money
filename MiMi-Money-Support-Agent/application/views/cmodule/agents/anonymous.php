<div class="chat-section" ng-controller="AnonymousController">
    <div ng-show="visible_area == 'anonymous'" class="clearfix" ng-class="{'active-right-sidebar':operatorPanel.right_sidebar}">
        <div class="chat-area">
            <div id="anonymous-header" class="container-header-secondary request-list-wrapper">
                <div class="clearfix text-center">
                    <a href="#" class="btn btn-action hidden-lg hidden-md hidden-sm pull-left" ng-click="toggleLeftSidebarDrawer($event)">
                        <i class="fa fa-bars"></i>
                    </a>
                    <ul class="request-list">
                        <li><a href="" ng-click="show_new_requests($event)" class="btn btn-link"><?php echo $this->lang->line('new_requests'); ?> <span ng-show="new_requests_counter > 0">({{new_requests_counter}})</span></a></li>
                        <li><a href="<?php echo site_url('d=agents&c=orequests'); ?>" class="btn btn-link"><?php echo $this->lang->line('offline_requests'); ?>  <span ng-show="offlineRequestsCounter > 0">({{offlineRequestsCounter}})</span></a></li>
                    </ul>
                    <a href="#" class="btn btn-action hidden-lg hidden-md pull-right" ng-click="toggleSidebarDrawer($event)">
                        <i class="fa fa-user-circle"></i>
                    </a>
                </div>
            </div>

            <div class="chat-container {{settings.operator_chat_theme}}">
                <div class="chat-container-frame" id="list-message">
                    <div ng-repeat="row in messages| orderBy:'sort_order'" id="message-row-{{$index}}" class="chat-row reply {{row.class}}" ng-class="{'last-chat-row': $index == (messages.length - 1)}" ng-mouseover="row.class = ''" on-last-repeat>
                        <div class="chat-message"><p ng-bind-html="row.chat_message | newlines | smilies"></p></div>
                    </div>
                </div>
            </div>

            <form id="anonymous-chat-form" name="anonymousChatForm" action="" method="post" ng-submit="send_message($event)">
                <div class="message-chat-panel">
                    <div class="form-group chat-message-box">
                        <textarea focus-on-change="new_message" cols="40" name="message" ng-model="new_message" placeholder="<?php echo $this->lang->line('placeholder_message'); ?>" class="form-control" ng-keydown="submit_message($event)" ng-disabled="anonymous_user.status == 'offline'"></textarea>
                    </div>
                    <button class="btn btn-link" ng-disabled="!new_message || anonymous_user.status == 'offline'"><i class="fa fa-paper-plane"></i></button>
                </div>
                <div class="chat-toolbar">
                    <span class="chat-toolbar-item smilies-handle" smilies-selector="new_message" smilies-placement="top-right" smilies-title="<?php echo $this->lang->line('smilies'); ?>"></span>
                    <?php if ($this->settings->enable_canned_messages == 'yes'): ?>
                        <span class="chat-toolbar-item canned-message canned-message-handle" message-data="new_message" canned-options="cannedOptions"></span>
                    <?php endif; ?>
                </div>
            </form>
        </div>

        <div id="anonymous-visitor-panel" class="active-user-panel" ng-class="{'ct-drawer position-right':windowWidth < 992,'open':sidebarDrawerOpen}">
            <div class="visitor-avatar">
                <span ng-style="{'background-color':anonymous_user.profile_color}" class="user-avatar" title="{{anonymous_user.name}}">{{anonymous_user.name|oneCapLetter}}</span>
            </div>

            <span ng-show="anonymous_user.name" class="profile-info entry-title">
                <span class="user-name">{{anonymous_user.name}}</span>
            </span>

            <span class="online-status" title="online"></span>

            <h3 class="title text-uppercase"><?php echo $this->lang->line('other_detail'); ?></h3>
            <ul class="list-unstyled list-action">
                <li ng-if="anonymous_user.email">
                    <i class="fa fa-envelope fa-fw" aria-hidden="true"></i>
                    <a href="mailto:{{anonymous_user.email}}">{{anonymous_user.email}}</a>
                </li>

                <li ng-if="anonymous_user.contact_number">
                    <i class="fa fa-phone fa-fw" aria-hidden="true"></i>
                    <a href="tel:{{anonymous_user.contact_number}}">{{anonymous_user.contact_number}}</a>
                </li>

                <li ng-if="anonymous_user.country">
                    <i class="fa fa-map-marker fa-fw" aria-hidden="true"></i>
                    <span>
                        <span ng-if="anonymous_user.city" class="d-inline" ng-bind-html="anonymous_user.city|do_seprate:', '"></span>
                        <span ng-if="anonymous_user.state" class="d-inline" ng-bind-html="anonymous_user.state|do_seprate:', '"></span>
                        <span class="d-inline" ng-bind-html="anonymous_user.country"></span>
                    </span>
                </li>

                <li ng-if="anonymous_user.site_name">
                    <i class="fa fa-globe fa-fw" aria-hidden="true"></i>
                    <a target="_blank" ng-href="{{anonymous_user.site_url|prep_url}}" title="{{anonymous_user.site_name}}"><span>{{anonymous_user.site_name}}</span></a>
                </li>

                <li>
                    <i class="fa fa-clock-o fa-fw" aria-hidden="true"></i>
                    <span ng-if="anonymous_user.created_at">{{anonymous_user.created_at| datetimeToTimestamp | date:'medium'}}</span>
                </li>
            </ul>

            <div ng-if="anonymous_user.ip_address" title="{{anonymous_user.ip_address}}" class="ip-address"><?php echo $this->lang->line('ip'); ?> - {{anonymous_user.ip_address}}</div>

            <h3 class="title"><?php echo $this->lang->line('visited_pages'); ?></h3>

            <ul class="list-unstyled list-pages">
                <li ng-repeat="page in anonymous_user.visited_pages|orderBy:'-'">
                    <a target="_blank" ng-href="{{page.page_url}}" title="{{page.page_title}}">{{page.page_title||'<?php echo $this->lang->line('no_page_title'); ?>'}} </a>
                    <i ng-if="anonymous_user.page_url == page.page_url" class="fa fa-circle" aria-hidden="true"></i>
                </li>
            </ul>
        </div>

        <div ng-if="sidebarDrawerOpen" class="ct-backdrop" ng-click="toggleSidebarDrawer($event)"></div>
    </div>
</div>