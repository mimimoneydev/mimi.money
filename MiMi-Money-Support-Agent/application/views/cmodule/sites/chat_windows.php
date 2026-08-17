<div class="chat-cmodule">
    <div class="chat-cmodule-container">
        <div id="chat-cmodule-header" class="chat-cmodule-header cmodule-clearfix">
            <div class="cmodule-window-avatar" ng-init="rand_color = getColor()">
                <img ng-show="settings.default_avatar" ng-src="{{settings.default_avatar}}" height="50" width="50" alt="{{settings.online_form_title}}" title="{{settings.online_form_title}}">
                <span ng-hide="settings.default_avatar" ng-style="{backgroundColor: rand_color}" class="cmodule-user-avatar" title="{{settings.online_form_title}}">{{settings.online_form_title|oneCapLetter}}</span>
            </div>
            <div class="cmodule-window-title">{{settings.online_form_title}}</div>
            <div class="cmodule-window-controls">
                <a title="Minimize window" id="cmodule-chat-minimize" class="chat-cmodule-minimize cmodule-window-control" href="javascript:void(0)"></a> 
                <a title="End Chat" id="cmodule-chat-close" class="chat-cmodule-close cmodule-window-control" href="javascript:void(0)"></a>
            </div>
        </div>

        <!-- theme bubbles with avatar -->
        <div id="live-chat-cmodule-widget" class="chat-cmodule-widget theme-{{settings.theme}}" ng-class="{'cmodule-avatar-removed':settings.theme == 'bubbles'}">
            <div class="chat-cmodule-view">
                <div class="chat-cmodule-row">
                    <div class="chat-cmodule-widget-content mCustomScrollbar" ng-show="settings.theme != 'classic'">
                        <div class="chat-cmodule-message-reply-row single">
                            <div class="chat-cmodule-message">
                                <?php echo $this->lang->line('dummy_text_row1'); ?>
                            </div>
                        </div>
                        <div class="chat-cmodule-message-row single">
                            <div ng-if="settings.theme != 'bubbles'" class="chat-row-avatar">
                                <img ng-show="settings.default_avatar" title="setlla-johnson" alt="" ng-src="{{settings.default_avatar}}" class="cmodule-img-circle cmodule-avatar">
                                <span ng-hide="settings.default_avatar" ng-style="{backgroundColor: rand_color}" class="cmodule-user-avatar" title="{{settings.online_form_title}}">{{settings.online_form_title|oneCapLetter}}</span>
                            </div>
                            <div class="chat-cmodule-message">
                                <?php echo $this->lang->line('dummy_text_row2'); ?>
                            </div>
                        </div>
                        <div class="chat-cmodule-message-reply-row top">
                            <div class="chat-cmodule-message">
                                <?php echo $this->lang->line('dummy_text_row3'); ?>
                            </div>
                        </div>
                        <div class="chat-cmodule-message-reply-row bottom">
                            <div class="chat-cmodule-message">
                                <?php echo $this->lang->line('dummy_text_row4'); ?>
                            </div>
                        </div>
                        <div class="chat-cmodule-message-row top">
                            <div ng-if="settings.theme == 'bubbles_with_avatar'" class="chat-row-avatar">
                                <img ng-show="settings.default_avatar" title="setlla-johnson" alt="" ng-src="{{settings.default_avatar}}" class="cmodule-img-circle cmodule-avatar">
                                <span ng-hide="settings.default_avatar" ng-style="{backgroundColor: rand_color}" class="cmodule-user-avatar" title="{{settings.online_form_title}}">{{settings.online_form_title|oneCapLetter}}</span>
                            </div>
                            <div class="chat-cmodule-message">
                                <?php echo $this->lang->line('dummy_text_row5'); ?>
                            </div>
                        </div>
                        <div class="chat-cmodule-message-row bottom chat-cmodule-last-chat-row">
                            <div ng-if="settings.theme != 'bubbles'" class="chat-row-avatar">
                                <img ng-show="settings.default_avatar" title="setlla-johnson" alt="" ng-src="{{settings.default_avatar}}" class="cmodule-img-circle cmodule-avatar">
                                <span ng-hide="settings.default_avatar" ng-style="{backgroundColor: rand_color}" class="cmodule-user-avatar" title="{{settings.online_form_title}}">{{settings.online_form_title|oneCapLetter}}</span>
                            </div>
                            <div class="chat-cmodule-message">
                                <?php echo $this->lang->line('dummy_text_row6'); ?>
                            </div>
                        </div>
                    </div>
                    
                    <div class="chat-cmodule-widget-content mCustomScrollbar" ng-show="settings.theme == 'classic'">
                        <ul class="cmodule-chat-list">
                            <li class="cmodule-chat-item cmodule-even">
                                <div class="chat-user-name"><?php echo $this->lang->line('text_you'); ?></div>
                                <div class="cmodule-chat-message">
                                    <?php echo $this->lang->line('dummy_text_row1'); ?>
                                </div>
                            </li>
                            <li class="cmodule-chat-item cmodule-odd">
                                <div class="chat-user-name"><?php echo $this->lang->line('text_operator'); ?></div>
                                <div class="cmodule-chat-message">
                                    <?php echo $this->lang->line('dummy_text_row2'); ?>
                                </div>
                            </li>
                            <li class="cmodule-chat-item cmodule-even">
                                <div class="chat-user-name"><?php echo $this->lang->line('text_you'); ?></div>
                                <div class="cmodule-chat-message">
                                    <?php echo $this->lang->line('dummy_text_row3'); ?>
                                </div>
                            </li>
                             <li class="cmodule-chat-item cmodule-even">
                                <div class="chat-user-name"><?php echo $this->lang->line('text_you'); ?></div>
                                <div class="cmodule-chat-message">
                                    <?php echo $this->lang->line('dummy_text_row4'); ?>
                                </div>
                            </li>
                            <li class="cmodule-chat-item cmodule-odd">
                                <div class="chat-user-name"><?php echo $this->lang->line('text_operator'); ?></div>
                                <div class="cmodule-chat-message">
                                    <?php echo $this->lang->line('dummy_text_row5'); ?>
                                </div>
                            </li>
                            <li class="cmodule-chat-item cmodule-odd">
                                <div class="chat-user-name"><?php echo $this->lang->line('text_operator'); ?></div>
                                <div class="cmodule-chat-message">
                                    <?php echo $this->lang->line('dummy_text_row6'); ?>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
            <div class="chat-cmodule-footer">
                <div class="cmodule-message-box">
                    <textarea cols="20" rows="2" class="cmodule-form-control" placeholder="<?php echo $this->lang->line('placeholder_message'); ?>"></textarea>
                </div>
                <div class="chat-cmodule-chat-toolbar">
                    <span ng-if="settings.enable_file_sharing == 'yes'" class="chat-toolbar-item chat-cmodule-chat-toolbar-btn attachment-handle">
                        <i aria-hidden="true" class="attachment-icon" role="button" title="<?php echo $this->lang->line('send_file'); ?>"></i>
                    </span>
                    <span class="chat-toolbar-item chat-cmodule-chat-toolbar-btn smilies-handle" smilies-selector="new_message" smilies-placement="top-right" smilies-title="Smilies"></span>
                </div>
            </div>
        </div>
    </div>
</div>