<div ng-controller="SiteSettingController as ssctr">
    <div class="header-panel clearfix">
        <h2>{{site.site_name}}: <?php echo $this->lang->line('settings'); ?> <a class="pull-right small" href="<?php echo site_url('c=sites'); ?>"><i class="fa fa-arrow-circle-o-left" aria-hidden="true"></i> <?php echo $this->lang->line('back'); ?></a></h2>   
        <?php version_info(); ?>
    </div>

    <div class="alert alert-success" ng-show="notification.showMessage">
        <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
        <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
    </div>

    <div class="alert alert-danger" ng-show="notification.showErrors">
        <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
        <div ng-bind-html="notification.errors"></div>
    </div>

    <div class="row" ng-init="show_preview = true; show_form = true">
        <div class="" ng-class="{'col-md-8':show_preview,'col-md-12': show_preview == false}">
            <div class="bs-card">
                <form name="settingForm" class="choose-theme" action="" method="post" ng-submit="settingForm.$valid">
                    <div class="theme-settings-container">
                        <ul class="nav nav-tabs" role="tablist">
                            <li class="active"><a ng-click="show_preview = true; show_form = true"  class="active"data-toggle="tab" role="tab" aria-controls="general-settings" href="#general-settings"><?php echo $this->lang->line('general'); ?></a></li>
                            <li><a ng-click="show_preview = true; show_form = true" data-toggle="tab" role="tab" aria-controls="content-info" href="#content-info"><?php echo $this->lang->line('content_and_info'); ?></a></li>
                            <li><a ng-click="show_preview = true; show_form = true" data-toggle="tab" role="tab" aria-controls="choose-theme" href="#choose-theme"><?php echo $this->lang->line('choose_a_theme'); ?></a></li>
                            <li><a ng-click="show_preview = true; show_form = true" data-toggle="tab" role="tab" aria-controls="site-departments" href="#site-departments"><?php echo $this->lang->line('departments'); ?></a></li>
                            <li><a href="#save" ng-click="update_settings($event)"><?php echo $this->lang->line('btn_save_settings'); ?></a></li>
                        </ul>
                        <div class="tab-content">
                            <div id="general-settings" class="active tab-pane fade in">
                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('enable_disable_chat'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_status-enable" name="chat_status" ng-model="settings.chat_status" value="enable">
                                            <label for="chat_status-enable"><?php echo $this->lang->line('enable'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_status-disable" name="chat_status" ng-model="settings.chat_status" value="disable">
                                            <label for="chat_status-disable"><?php echo $this->lang->line('disable'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <h5>
                                        <?php echo $this->lang->line('time_interwal'); ?> <span class="small">( <?php echo $this->lang->line('in_seconds'); ?> )</span>
                                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" uib-tooltip="<?php echo $this->lang->line('time_interwal_help'); ?>"><i class="fa fa-question-circle"></i></a>
                                    </h5>
                                    <input class="form-control" type="number" min="3" max="20" ng-min="3" ng-max="20" ng-model="settings.time_interwal" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('chat_mode'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_mode-online" name="chat_mode" ng-model="settings.chat_mode" value="online">
                                            <label for="chat_mode-online"><?php echo $this->lang->line('online'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_mode-offline" name="chat_mode" ng-model="settings.chat_mode" value="offline">
                                            <label for="chat_mode-offline"><?php echo $this->lang->line('offline'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <h5>
                                        <?php echo $this->lang->line('initiate_bypass_chat'); ?>
                                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" uib-tooltip="<?php echo $this->lang->line('initiate_bypass_chat_help'); ?>"><i class="fa fa-question-circle"></i></a>
                                    </h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="initiate_bypass_chat-yes" name="initiate_bypass_chat" ng-model="settings.initiate_bypass_chat" value="yes">
                                            <label for="initiate_bypass_chat-yes" ng-click="settings.send_chat_transcript_to_visitor = 'ask_to_visiter';settings.open_chatbox_automatically = 'no';"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="initiate_bypass_chat-no" name="initiate_bypass_chat" ng-model="settings.initiate_bypass_chat" value="no">
                                            <label for="initiate_bypass_chat-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group animated">
                                    <h5><?php echo $this->lang->line('send_chat_transcript_to_visitor'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input ng-disabled="settings.initiate_bypass_chat == 'yes'" type="radio" id="send_chat_transcript_to_visitor-yes" name="send_chat_transcript_to_visitor" ng-model="settings.send_chat_transcript_to_visitor" value="yes">
                                            <label for="send_chat_transcript_to_visitor-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="send_chat_transcript_to_visitor-no" name="send_chat_transcript_to_visitor" ng-model="settings.send_chat_transcript_to_visitor" value="no">
                                            <label for="send_chat_transcript_to_visitor-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="send_chat_transcript_to_visitor-ask_to_visiter" name="send_chat_transcript_to_visitor" ng-model="settings.send_chat_transcript_to_visitor" value="ask_to_visiter">
                                            <label for="send_chat_transcript_to_visitor-ask_to_visiter"><?php echo $this->lang->line('ask_to_visiter'); ?></label>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="form-group animated">
                                    <h5><?php echo $this->lang->line('send_chat_transcript_to_admin'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="send_chat_transcript_to_admin-yes" name="send_chat_transcript_to_admin" ng-model="settings.send_chat_transcript_to_admin" value="yes">
                                            <label for="send_chat_transcript_to_admin-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="send_chat_transcript_to_admin-no" name="send_chat_transcript_to_admin" ng-model="settings.send_chat_transcript_to_admin" value="no">
                                            <label for="send_chat_transcript_to_admin-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="form-group animated">
                                    <h5><?php echo $this->lang->line('send_chat_transcript_to_operators'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="send_chat_transcript_to_operators-yes" name="send_chat_transcript_to_operators" ng-model="settings.send_chat_transcript_to_operators" value="yes">
                                            <label for="send_chat_transcript_to_operators-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="send_chat_transcript_to_operators-no" name="send_chat_transcript_to_operators" ng-model="settings.send_chat_transcript_to_operators" value="no">
                                            <label for="send_chat_transcript_to_operators-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-hide="settings.initiate_bypass_chat == 'yes'" class="form-group animated" ng-class="{'bounceInLeft':settings.initiate_bypass_chat == 'no','bounceOutRight':settings.initiate_bypass_chat == 'yes'}">
                                    <h5>
                                        <?php echo $this->lang->line('open_chatbox_automatically'); ?>
                                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" tooltip-append-to-body="true" uib-tooltip-html="'<?php echo $this->lang->line('open_chatbox_automatically_help'); ?>'"><i class="fa fa-question-circle"></i></a>
                                    </h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="open_chatbox_automatically-yes" name="open_chatbox_automatically" ng-model="settings.open_chatbox_automatically" value="yes">
                                            <label for="open_chatbox_automatically-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="open_chatbox_automatically-no" name="open_chatbox_automatically" ng-model="settings.open_chatbox_automatically" value="no">
                                            <label for="open_chatbox_automatically-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-show="settings.open_chatbox_automatically == 'yes'" class="form-group animated" ng-class="{'bounceInLeft':settings.open_chatbox_automatically == 'yes','bounceOutRight':settings.open_chatbox_automatically == 'no'}">
                                    <h5>
                                        <?php echo $this->lang->line('time_automatically_open_chatbox'); ?> <span class="small">( <?php echo $this->lang->line('in_seconds'); ?> )</span>
                                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" tooltip-append-to-body="true" uib-tooltip="<?php echo $this->lang->line('time_automatically_open_chatbox_help'); ?>"><i class="fa fa-question-circle"></i></a>
                                    </h5>
                                    <input class="form-control" type="number" min="5" ng-min="5" ng-model="settings.time_automatically_open_chatbox" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('enable_feedback_form'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_feedback_form-yes" name="enable_feedback_form" ng-model="settings.enable_feedback_form" value="yes">
                                            <label for="enable_feedback_form-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_feedback_form-no" name="enable_feedback_form" ng-model="settings.enable_feedback_form" value="no">
                                            <label for="enable_feedback_form-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('show_contact_number_input'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="show_contact_number_input-yes" name="show_contact_number_input" ng-model="settings.show_contact_number_input" value="yes">
                                            <label for="show_contact_number_input-yes" ng-click="settings.enable_specific_agent_request = 'no'"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="show_contact_number_input-no" name="show_contact_number_input" ng-model="settings.show_contact_number_input" value="no">
                                            <label for="show_contact_number_input-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('show_depaertment_selection_box'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="show_depaertment_selection_box-yes" name="show_depaertment_selection_box" ng-model="settings.show_depaertment_selection_box" value="yes">
                                            <label for="show_depaertment_selection_box-yes" ng-click="settings.enable_specific_agent_request = 'no'"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="show_depaertment_selection_box-no" name="show_depaertment_selection_box" ng-model="settings.show_depaertment_selection_box" value="no">
                                            <label for="show_depaertment_selection_box-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-hide="settings.show_depaertment_selection_box == 'yes' || settings.show_contact_number_input == 'yes'" class="form-group animated" ng-class="{'bounceInLeft':(settings.show_depaertment_selection_box == 'no' && settings.show_contact_number_input == 'no'),'bounceOutRight':(settings.show_depaertment_selection_box == 'yes' || settings.show_contact_number_input == 'yes')}">
                                    <h5><?php echo $this->lang->line('enable_specific_agent_request'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_specific_agent_request-yes" name="enable_specific_agent_request" ng-model="settings.enable_specific_agent_request" value="yes">
                                            <label for="enable_specific_agent_request-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_specific_agent_request-no" name="enable_specific_agent_request" ng-model="settings.enable_specific_agent_request" value="no">
                                            <label for="enable_specific_agent_request-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('enable_file_sharing'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_file_sharing-yes" name="enable_file_sharing" ng-model="settings.enable_file_sharing" value="yes">
                                            <label for="enable_file_sharing-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_file_sharing-no" name="enable_file_sharing" ng-model="settings.enable_file_sharing" value="no">
                                            <label for="enable_file_sharing-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-hide="settings.enable_file_sharing == 'no'" class="form-group animated" ng-class="{'bounceInLeft':settings.enable_file_sharing == 'yes','bounceOutRight':settings.enable_file_sharing == 'no'}">
                                    <h5>
                                        <?php echo $this->lang->line('allowed_filetypes'); ?>
                                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" tooltip-append-to-body="true" uib-tooltip-html="'<?php echo $this->lang->line('allowed_filetypes_help'); ?>'"><i class="fa fa-question-circle"></i></a>
                                    </h5>
                                    <input class="form-control" type="text" ng-model="settings.allowed_filetypes" required />
                                    <p class="help-block"><?php echo $this->lang->line('allowed_filetypes_example'); ?></p>
                                </div>

                                <div ng-hide="settings.enable_file_sharing == 'no'" class="form-group animated" ng-class="{'bounceInLeft':settings.enable_file_sharing == 'yes','bounceOutRight':settings.enable_file_sharing == 'no'}">
                                    <h5>
                                        <?php echo $this->lang->line('file_upload_size'); ?> <span class="small">( <?php echo $this->lang->line('filesize_unit'); ?> )</span>
                                    </h5>
                                    <input class="form-control" type="number" min="500" ng-min="500" ng-model="settings.file_upload_size" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('hide_poweredby'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="hide_poweredby-yes" name="hide_poweredby" ng-model="settings.hide_poweredby" value="yes">
                                            <label for="hide_poweredby-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="hide_poweredby-no" name="hide_poweredby" ng-model="settings.hide_poweredby" value="no">
                                            <label for="hide_poweredby-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-hide="settings.hide_poweredby == 'yes'" class="form-group animated" ng-class="{'bounceInLeft':settings.hide_poweredby == 'no','bounceOutRight':settings.hide_poweredby == 'yes'}">
                                    <h5><?php echo $this->lang->line('poweredby_content'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" class="form-control" cols="40" rows="3" name="poweredby_content" ng-model="settings.poweredby_content" required></textarea>
                                </div>
                            </div>
                            <div id="content-info" class="tab-pane fade">
                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('default_avatar'); ?></h5>
                                    <div class="input-group">
                                        <span class="input-group-btn">
                                            <span class="btn btn-sn btn-default btn-file">
                                                Browse&hellip; <input type="file" name="entry_avatar" ng-model="ssctr.entry_avatar" accept="image/*" base-sixty-four-input>
                                            </span>
                                        </span>
                                        <input type="text" ng-model="ssctr.entry_avatar.filename" class="form-control" disabled="disabled">
                                        <span class="input-group-btn"><button class="btn btn-primary btn-sn btn-x2" ng-disabled="!ssctr.entry_avatar.filename" ng-click="clear_file_input($event)"><?php echo $this->lang->line('clear'); ?></button></span>
                                    </div>
                                </div>

                                <div class="form-group-images form-group" ng-if="ssctr.entry_avatar.filename || settings.default_avatar">
                                    <div class="row">
                                        <div class="col-sm-4">
                                            <img ng-if="ssctr.entry_avatar.filename" class="img-responsive" ng-src="{{'data:'+ssctr.entry_avatar.filetype+';base64,'+ssctr.entry_avatar.base64}}" alt="{{site.site_name}}">
                                            <img ng-if="settings.default_avatar && !ssctr.entry_avatar.filename" class="img-responsive" ng-src="{{settings.default_avatar}}" alt="{{site.site_name}}">
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('chat_start_title'); ?></h5>
                                    <input class="form-control" type="text" ng-model="settings.chat_start_title" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('online_form_title'); ?></h5>
                                    <input class="form-control" type="text" ng-model="settings.online_form_title" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('set_welcome_message'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" class="form-control" cols="40" rows="4" ng-model="settings.welcome_message" required><?php echo $this->lang->line('welcome_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('welcome_message_help'); ?></em></p>
                                </div>

                                <div class="form-group">
                                    <h5>
                                        <?php echo $this->lang->line('chat_waiting_title'); ?>
                                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" tooltip-append-to-body="true" uib-tooltip-html="'<?php echo $this->lang->line('waiting_title_help'); ?>'"><i class="fa fa-question-circle"></i></a>
                                    </h5>
                                    <input class="form-control" type="text" ng-model="settings.chat_waiting_title" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('set_waiting_message'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" class="form-control" cols="40" rows="4" ng-model="settings.waiting_message" required><?php echo $this->lang->line('waiting_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('waiting_message_help'); ?></em></p>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('operator_gone_offline_message'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" name="operator_gone_offline_message" class="form-control" cols="40" rows="4" ng-model="settings.operator_gone_offline_message" required></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('operator_gone_offline_message_help'); ?></em></p>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('offline_form_title'); ?></h5>
                                    <input class="form-control" type="text" ng-model="settings.offline_form_title" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('on_offline_show'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="offline_support_type-form" name="offline_support_type" ng-model="settings.offline_support_type" value="form">
                                            <label for="offline_support_type-form"><?php echo $this->lang->line('conf_form'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="offline_support_type-message" name="offline_support_type" ng-model="settings.offline_support_type" value="message">
                                            <label for="offline_support_type-message"><?php echo $this->lang->line('conf_message'); ?></label>
                                        </div>
                                    </div>
                                </div>
                                
                                <div ng-show="settings.offline_support_type == 'message'" class="form-group animated" ng-class="{'bounceInLeft':settings.offline_support_type == 'message','bounceOutRight':settings.offline_support_type == 'form'}">
                                    <h5><?php echo $this->lang->line('offline_message'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" class="form-control" cols="40" rows="4" ng-model="settings.offline_message" required><?php echo $this->lang->line('offline_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('offline_message_help'); ?></em></p>
                                </div>

                                <div ng-show="settings.offline_support_type == 'form'" class="form-group animated" ng-class="{'bounceInLeft':settings.offline_support_type == 'form','bounceOutRight':settings.offline_support_type == 'message'}">
                                    <h5><?php echo $this->lang->line('set_offline_heading_message'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" class="form-control" cols="40" rows="4" ng-model="settings.offline_heading_message" required><?php echo $this->lang->line('offline_heading_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('offline_heading_message_help'); ?></em></p>
                                </div>

                                <div ng-show="settings.offline_support_type == 'form'" class="form-group animated" ng-class="{'bounceInLeft':settings.offline_support_type == 'form','bounceOutRight':settings.offline_support_type == 'message'}">
                                    <h5><?php echo $this->lang->line('set_offline_submission_message'); ?></h5>
                                    <textarea class="form-control" cols="40" rows="4" ng-model="settings.offline_submission_message" required><?php echo $this->lang->line('offline_submission_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('offline_submission_message_help'); ?></em></p>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('set_feedback_heading_message'); ?></h5>
                                    <textarea ui-tinymce="tinymceOptions" class="form-control" cols="40" rows="4" ng-model="settings.feedback_heading_message" required><?php echo $this->lang->line('feedback_heading_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('feedback_heading_message_help'); ?></em></p>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('set_feedback_submission_message'); ?></h5>
                                    <textarea class="form-control" cols="40" rows="4" ng-model="settings.feedback_submission_message" required><?php echo $this->lang->line('feedback_submission_message'); ?></textarea>
                                    <p class="help-block"><em><?php echo $this->lang->line('feedback_submission_message_help'); ?></em></p>
                                </div>
                            </div>
                            <div id="choose-theme" class="tab-pane fade">
                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('choose_theme'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="visotor-theme-classic" name="theme" ng-model="settings.theme" value="classic">
                                            <label for="visotor-theme-classic"><?php echo $this->lang->line('theme_classic'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="visotor-theme-bubbles" name="theme" ng-model="settings.theme" value="bubbles">
                                            <label for="visotor-theme-bubbles"><?php echo $this->lang->line('theme_bubbles'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="visotor-theme-bubbles_with_avatar" name="theme" ng-model="settings.theme" value="bubbles_with_avatar">
                                            <label for="visotor-theme-bubbles_with_avatar"><?php echo $this->lang->line('theme_bubbles_with_avatar'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="visotor-theme-bubbles_boom" name="theme" ng-model="settings.theme" value="bubbles_boom">
                                            <label for="visotor-theme-bubbles_boom"><?php echo $this->lang->line('theme_bubbles_boom'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('visitor_widget_type'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="visitor_widget_type-chaticon" name="visitor_widget_type" ng-model="settings.visitor_widget_type" value="chaticon">
                                            <label for="visitor_widget_type-chaticon"><?php echo $this->lang->line('chaticon'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="visitor_widget_type-chatbar" name="visitor_widget_type" ng-model="settings.visitor_widget_type" value="chatbar">
                                            <label for="visitor_widget_type-chatbar" ng-click="settings.enable_online_animation = 'no';"><?php echo $this->lang->line('chatbar'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-show="settings.visitor_widget_type == 'chaticon'" class="form-group animated" ng-class="{'bounceInLeft':settings.visitor_widget_type == 'chaticon','bounceOutRight':settings.visitor_widget_type == 'chatbar'}">
                                    <h5><?php echo $this->lang->line('chat_icon_size'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_icon_size-large" name="chat_icon_size" ng-model="settings.chat_icon_size" value="large-size">
                                            <label for="chat_icon_size-large"><?php echo $this->lang->line('large'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_icon_size-medium" name="chat_icon_size" ng-model="settings.chat_icon_size" value="medium-size">
                                            <label for="chat_icon_size-medium"><?php echo $this->lang->line('medium'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="chat_icon_size-small" name="chat_icon_size" ng-model="settings.chat_icon_size" value="small-size">
                                            <label for="chat_icon_size-small"><?php echo $this->lang->line('small'); ?></label>
                                        </div>
                                    </div>
                                </div>

                                <div ng-show="settings.visitor_widget_type == 'chaticon'" class="form-group animated" ng-class="{'bounceInLeft':settings.visitor_widget_type == 'chaticon','bounceOutRight':settings.visitor_widget_type == 'chatbar'}">
                                    <h5><?php echo $this->lang->line('enable_online_animation'); ?></h5>
                                    <div class="radio-list">
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_online_animation-yes" name="enable_online_animation" ng-model="settings.enable_online_animation" value="yes">
                                            <label for="enable_online_animation-yes"><?php echo $this->lang->line('yes'); ?></label>
                                        </div>
                                        
                                        <div class="radio radio-replace radio-inline">
                                            <input type="radio" id="enable_online_animation-no" name="enable_online_animation" ng-model="settings.enable_online_animation" value="no">
                                            <label for="enable_online_animation-no"><?php echo $this->lang->line('no'); ?></label>
                                        </div>
                                    </div>
                               </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('background_color'); ?></h5>
                                    <input ng-change="update_theme()" class="form-control" colorpicker colorpicker-with-input="true" colorpicker-position="bottom" colorpicker-close-on-select type="text" ng-model="settings.background_color" ng-style="{'background-color':settings.background_color, 'color':settings.title_color}"  ng-readonly="settings.background_color" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('title_color'); ?></h5>
                                    <input ng-change="update_theme()" class="form-control" colorpicker colorpicker-with-input="true" colorpicker-position="bottom" colorpicker-close-on-select type="text" ng-model="settings.title_color" ng-style="{'background-color':settings.title_color}"  ng-readonly="settings.title_color" required />
                                </div>

                                <div class="form-group">
                                    <h5><?php echo $this->lang->line('set_position'); ?></h5>
                                    <div class="radio">
                                        <label class="radio-inline"><input type="radio" name="window_position" ng-model="settings.window_position" value="left"> <?php echo $this->lang->line('left'); ?></label>
                                        <label class="radio-inline"><input type="radio" name="window_position" ng-model="settings.window_position" value="right"> <?php echo $this->lang->line('right'); ?></label>
                                    </div>
                                </div>
                            </div>

                            <div id="site-departments" class="tab-pane fade">
                                <div class="form-group">
                                    <div class="category-block">
                                        <h5><?php echo $this->lang->line('departments'); ?></h5>
                                        <div ng-repeat="tag in tags" class="checkbox checkbox-replace">
                                            <input id="filter-row-{{$index}}" type="checkbox" checklist-model="settings.departments" checklist-value="tag.id">
                                            <label for="filter-row-{{$index}}">{{tag.tag_name}}</label>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="form-group form-action">
                        <button class="btn btn-primary" ng-disabled="!settingForm.$valid" ng-click="update_settings($event)"><?php echo $this->lang->line('submit_save'); ?></button>
                    </div>
                </form>
            </div>
        </div>

        <div class="col-md-4 hidden-sm hidden-xs" ng-show="show_preview">
            <?php include theme_path('sites/chat_windows.php'); ?>
        </div>
    </div>
</div>