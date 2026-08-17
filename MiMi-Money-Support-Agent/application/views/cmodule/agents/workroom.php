<div class="chat-section" ng-controller="WorkroomController">
    <div ng-show="visible_area == 'workroom'" class="clearfix" ng-class="{'active-right-sidebar':operatorPanel.right_sidebar}">
        <div class="chat-area">
            <div id="workroom-header" class="container-header-secondary request-list-wrapper">
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
                <div class="chat-container-frame" ng-class="{'disabled-chatting':chat_session.session_status != 'open' && chat_session.session_status != 'on-hold' && chat_session.session_status != 'disconnected'}" id="message_box">
                    <div ng-repeat="row in messages| orderBy:'sort_order'" id="chat-row-{{$index}}" class="chat-row {{row.class}}" ng-class="{'reply': row.sender_id != visitor.id, 'last-chat-row': $index == (messages.length - 1)}" ng-mouseover="row.class = ''"  on-last-repeat>
                        <div class="chat-row-avatar">
                            <img ng-show="row.profile_pic && row.sender_id == visitor.id" class="img-circle avatar" ng-src="{{row.profilePic}}" alt="" title="{{row.name}}">
                            <span ng-hide="row.profile_pic || row.sender_id != visitor.id" ng-style="{'background-color':row.profile_color}" class="user-avatar" title="{{row.name}}">{{row.name|oneCapLetter}}</span>
                        </div>
                        <div title="{{row.name}}" class="chat-message"><p ng-bind-html="row.chat_message | newlines | smilies"></p></div>
                    </div>

                    <div class="alert alert-success" ng-show="notification.showMessage">
                        <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                        <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>
                    </div>

                    <div class="alert alert-danger" ng-show="notification.showErrors">
                        <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                        <div ng-bind-html="notification.errors"></div>
                    </div>
                </div>
            </div>

            <div class="status-typing-indecator" ng-show="chat_session.session_status == 'open' || chat_session.session_status == 'on-hold' || chat_session.session_status == 'disconnected'">
                <div class="message-status-typing" ng-show="is_typing && chat_session.session_status == 'open'">
                    <i class="fa fa-pencil">&nbsp;</i> {{visitor.name}} <?php echo $this->lang->line('is_typing'); ?>
                </div>
                <div ng-show="new_msg_indecator && is_scroll_start && chat_session.session_status == 'open'" class="chat-cmodule-new-message-indecator"></div>
            </div>

            <form name="chatForm" id="chatForm" action="" method="post" ng-submit="send_message($event)" ng-show="chat_session.session_status == 'open' || chat_session.session_status == 'on-hold' || chat_session.session_status == 'disconnected'">
                <div class="message-chat-panel">
                    <div class="form-group chat-message-box">
                        <textarea ng-focus="chatboxState = 'focus'" focus-on-change="new_message" ng-blur="chatboxState = 'blur'" cols="40" name="message" ng-model="new_message" id="message" placeholder="<?php echo $this->lang->line('placeholder_message'); ?>" class="form-control" ng-keydown="submit_message($event)" ng-disabled="visitor.status == 'offline'"></textarea>
                    </div>
                    <button class="btn btn-link" ng-disabled="!new_message || visitor.status == 'offline'"><i class="fa fa-paper-plane"></i></button>
                </div>
                <div class="chat-toolbar">
                    <span class="chat-toolbar-item smilies-handle" smilies-selector="new_message" smilies-placement="top-right" smilies-title="<?php echo $this->lang->line('smilies'); ?>"></span>
                    <?php if ($this->settings->enable_canned_messages == 'yes'): ?>
                        <span class="chat-toolbar-item canned-message canned-message-handle" message-data="new_message" canned-options="cannedOptions"></span>
                    <?php endif; ?>
                    <?php if ($this->settings->enable_agent_file_sharing == 'yes' and $this->settings->agent_allowed_filetypes != ''): ?>
                        <span class="chat-toolbar-item attachment-handle">
                            <i aria-hidden="true" class="chat-toolbar-btn" role="button" title="<?php echo $this->lang->line('send_file'); ?>"></i>
                            <input title="<?php echo $this->lang->line('send_file'); ?>" class="chat-input-file" on-after-validate="upload_files" onerror="file_error_handler" type="file" role="button" ng-model="chatfiles" name="chatfiles" accept="<?php echo str_replace("|", ",", $this->settings->agent_allowed_filetypes); ?>" maxsize="<?php echo $this->settings->agent_file_upload_size; ?>" base-sixty-four-input>
                        </span>
                        <span class="chat-toolbar-item" ng-show="is_file_sending && chatForm.chatfiles.$valid"><?php echo $this->lang->line('sending_file'); ?></span>
                        <span class="chat-toolbar-item text-danger" ng-show="chatForm.chatfiles.$error.maxsize"><?php echo $this->lang->line('exceeded_filesize'); ?></span>
                        <span class="chat-toolbar-item text-danger" ng-show="chatForm.chatfiles.$error.accept"><?php echo $this->lang->line('invalid_filetype'); ?></span>
                    <?php endif; ?>
                </div>
            </form>
        </div>

        <div id="workroom-visitor-panel" class="active-user-panel" ng-class="{'ct-drawer position-right':windowWidth < 992,'open':sidebarDrawerOpen}">
            <div class="visitor-avatar">
                <img ng-show="visitor.profile_pic" class="img-circle avatar" ng-src="{{visitor.profilePic}}" alt="" title="{{visitor.name}}">
                <span ng-show="!visitor.profile_pic" ng-style="{'background-color':visitor.profile_color}" class="user-avatar" title="{{visitor.name}}">{{visitor.name|oneCapLetter}}</span>
            </div>

            <span ng-show="visitor.name" class="profile-info entry-title">
                <span class="user-name" ng-show="user.id != visitor.id">{{visitor.name}}</span>
                <span class="user-name" ng-show="user.id == visitor.id"><?php echo $this->lang->line('text_you'); ?></span>
            </span>

            <span class="{{visitor.status}}-status" title="{{visitor.status}}"></span>

            <ul class="list-inline action-button" ng-if="chat_session.session_status != 'closed'">
                <li ng-if="chat_session.session_status == 'open'">
                    <a href="#formblock" data-toggle="modal" ng-click="display_forward($event)" class="btn btn-default btn-chat-action" title="<?php echo $this->lang->line('forword'); ?>"><i class="fa fa-share fa-fw" aria-hidden="true"></i><?php echo $this->lang->line('forword'); ?></a>
                </li>
                <li ng-if="chat_session.session_status == 'open' || chat_session.session_status == 'on-hold' || chat_session.session_status == 'disconnected'">
                    <a href="#/" ng-click="end_chat($event)" class="btn btn-default btn-chat-action" title="<?php echo $this->lang->line('close'); ?>"><i class="fa fa-close fa-fw" aria-hidden="true"></i><?php echo $this->lang->line('close'); ?></a>
                </li>
            </ul>

            <h3 class="title text-uppercase"><?php echo $this->lang->line('other_detail'); ?></h3>
            <ul class="list-unstyled list-action">
                <li ng-if="visitor.email">
                    <i class="fa fa-envelope fa-fw" aria-hidden="true"></i>
                    <a href="mailto:{{visitor.email}}">{{visitor.email}}</a>
                </li>

                <li ng-if="visitor.contact_number">
                    <i class="fa fa-phone fa-fw" aria-hidden="true"></i>
                    <a href="tel:{{visitor.contact_number}}">{{visitor.contact_number}}</a>
                </li>

                <li ng-if="visitor.country">
                    <i class="fa fa-map-marker fa-fw" aria-hidden="true"></i>
                    <span>
                        <span ng-if="visitor.city" class="d-inline" ng-bind-html="visitor.city|do_seprate:', '"></span>
                        <span ng-if="visitor.state" class="d-inline" ng-bind-html="visitor.state|do_seprate:', '"></span>
                        <span class="d-inline" ng-bind-html="visitor.country"></span>
                    </span>
                </li>

                <li ng-if="visitor.site_name">
                    <i class="fa fa-globe fa-fw" aria-hidden="true"></i>
                    <a target="_blank" ng-href="{{visitor.site_url|prep_url}}" title="{{visitor.site_name}}"><span>{{visitor.site_name}}</span></a>
                </li>

                <li>
                    <i class="fa fa-clock-o fa-fw" aria-hidden="true"></i>
                    <span ng-if="visitor.started_at">{{visitor.started_at| datetimeToTimestamp | date:'medium'}}</span>
                </li>
            </ul>

            <div ng-if="visitor.ip_address" title="{{visitor.ip_address}}" class="ip-address"><?php echo $this->lang->line('ip'); ?> - {{visitor.ip_address}}</div>

            <h3 class="title"><?php echo $this->lang->line('visited_pages'); ?></h3>

            <ul class="list-unstyled list-pages">
                <li ng-repeat="page in visitor.visited_pages|orderBy:'-'">
                    <a target="_blank" ng-href="{{page.page_url}}" title="{{page.page_title}}">{{page.page_title||'<?php echo $this->lang->line('no_page_title'); ?>'}}</a>
                    <i ng-if="visitor.page_url == page.page_url && chat_session.session_status != 'closed'" class="fa fa-circle" aria-hidden="true"></i>
                </li>
            </ul>
        </div>
        <div ng-if="sidebarDrawerOpen" class="ct-backdrop" ng-click="toggleSidebarDrawer($event)"></div>

        <div id="formblock" class="modal fade" data-keyboard="true">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
                        <h2 class="modal-title"><?php echo $this->lang->line('forward_chat'); ?></h2>
                    </div>
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-xs-12">
                                <div class="alert alert-success" ng-show="notification.showMessage">
                                    <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                                    <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>
                                </div>

                                <div class="alert alert-danger" ng-show="notification.showErrors">
                                    <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                                    <div ng-bind-html="notification.errors"></div>
                                </div>

                                <form name="forwardForm" method="post" ng-submit="forwardForm.$valid && forward_chat($event, forward)" novalidate>
                                    <div class="form-group">
                                        <h5><?php echo $this->lang->line('forward_to'); ?></h5>
                                        <div class="radio-list">
                                            <div class="radio radio-replace radio-inline">
                                                <input type="radio" id="forward-to-agents" name="forward_to" ng-model="forward.to" value="agents" required>
                                                <label for="forward-to-agents"><?php echo $this->lang->line('agents'); ?></label>
                                            </div>

                                            <div class="radio radio-replace radio-inline">
                                                <input type="radio" id="forward-to-departments" name="forward_to" ng-model="forward.to" value="departments" required>
                                                <label for="forward-to-departments"><?php echo $this->lang->line('departments'); ?></label>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="form-group" ng-show="forward.to == 'agents'" ng-init="forward.agents = []">
                                        <h5><?php echo $this->lang->line('agents'); ?></h5>
                                        <div ng-if="agents_list.length" ng-repeat="agent in agents_list" class="checkbox checkbox-replace">
                                            <input type="checkbox" id="agent-row-{{$index}}" name="forward_agents" checklist-model="forward.agents" checklist-value="agent.id" ng-required="!forward.agents.length && forward.to == 'agents'">
                                            <label for="agent-row-{{$index}}">{{agent.name}}</label>
                                        </div>

                                        <div ng-if="!agents_list.length">
                                            <p><?php echo $this->lang->line('no_operators_online'); ?></p>
                                        </div>
                                    </div>

                                    <div class="form-group" ng-show="forward.to == 'departments'">
                                        <h5><?php echo $this->lang->line('departments'); ?></h5>
                                        <div ng-if="departments_list.length" class="radio-list">
                                            <div ng-repeat="department in departments_list" class="radio radio-replace">
                                                <input type="radio" id="department-row-{{$index}}" name="forward_department" ng-model="forward.department" value="{{department.id}}" ng-required="!forward.department.length && forward.to == 'departments'">
                                                <label for="department-row-{{$index}}">{{department.department}}</label>
                                            </div>
                                        </div>

                                        <div ng-if="!departments_list.length">
                                            <p><?php echo $this->lang->line('no_operators_online'); ?></p>
                                        </div>
                                    </div>

                                    <div class="panel">
                                        <button id="send-btn" class="btn btn-primary" ng-disabled="!forwardForm.$valid || (!forward.agents.length && !forward.department)"><?php echo $this->lang->line('forward'); ?></button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <audio id="audio1">
        <source src="<?php echo theme_url("audio/ping.mp3"); ?>"></source>
    </audio>
</div>
