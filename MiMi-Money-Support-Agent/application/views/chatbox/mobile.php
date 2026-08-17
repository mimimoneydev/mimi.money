<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><?php echo $this->lang->line('visitor_panel'); ?> - <?php echo $this->current_site->site_name; ?></title>
<link href="<?php echo base_url('assets/cmodule-chat/css/chatbox-mobile.min.css'); ?>" rel="stylesheet">

<script>
    var site_url = '<?php echo site_url(); ?>';
    var cmodule_site_url = '<?php echo site_url();?>';
    var cmodule_base_url = '<?php echo base_url();?>';
    var access_token = '<?php echo $this->access_token;?>';
    var siteuser = '<?php echo json_encode($siteuser);?>';
    var cbwindow = <?php echo json_encode($cbwindow);?>;
    var pageinfo = <?php echo json_encode($pageinfo);?>;
</script>

<script src="<?php echo base_url('assets/cmodule-chat/js/chatbox-mobile.min.js'); ?>"></script>

<link rel="shortcut icon" href="<?php echo theme_url('icon/favicon.ico'); ?>" type="image/x-icon">
<link rel="icon" href="<?php echo theme_url('icon/favicon.ico'); ?>" type="image/x-icon">
<meta name="theme-color" content="#ffffff">
</head>
<body ng-app="cmodule" ng-controller="BodyController" ng-cloak>    
    <!--Chat Container-->
    <div ng-show="settings.chat_status == 'enable' && settings.plugin_validated == 'yes'" class="chat-cmodule" id="chat-cmodule-mainContainer" ng-init="show_chatbox()">
        <div class="chat-cmodule-section">
            <div class="chat-cmodule-container">
                <div id="chat-cmodule-header" class="chat-cmodule-header cmodule-clearfix">
                    <div class="cmodule-window-avatar">
                        <img ng-show="settings.default_avatar" ng-src="{{settings.default_avatar}}" height="50" width="50" alt="<?php echo $this->lang->line('operator'); ?>">
                        <span ng-hide="settings.default_avatar" ng-style="{backgroundColor: profile_color}" class="cmodule-user-avatar">{{form_title|oneCapLetter}}</span>
                    </div>
                    <div class="cmodule-window-title">{{form_title}}</div>
                    <div class="cmodule-window-controls">
                        <a ng-click="end_chat($event)" title="<?php echo $this->lang->line('close'); ?>" id="cmodule-chat-close" class="chat-cmodule-close cmodule-window-control" href="javascript:void(0)"></a>
                    </div>
                </div>

                <div class="cmodule-notification cmodule-error-message" ng-show="showError" ng-bind-html="errors" ng-class="{'in-cmodule': showError}"></div>
                <div class="cmodule-notification cmodule-success-message" ng-show="showMessage" ng-bind-html="success_message" ng-class="{'in-cmodule': showMessage}"></div>

                <div ng-show="visible_widget == 'initaiting-bypasschat-widget' && minimized != 'yes'" class="chat-cmodule-table-row cmodule-bypasschat-widget">
                    <div class="chat-cmodule-widget chat-cmodule-table">
                        <div class="chat-cmodule-table-row">
                            <div class="chat-cmodule-view chat-cmodule-table">
                                <div class="chat-cmodule-row chat-cmodule-table-cell">                                    
                                    <div class="chat-cmodule-widget-content">
                                        <div class="initaiting-progrss-container">
                                            <i class="cmodule-icon fa fa-circle-o-notch fa-spin fa-3x fa-fw" aria-hidden="true"></i> <span class="progress-text"><?php echo $this->lang->line('initiating_chat');?></span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div ng-show="visible_widget == 'offline-widget'" id="cmodule-offline-widget" class="chat-cmodule-table-row">
                    <div class="chat-cmodule-widget chat-cmodule-table">
                        <form class="chat-cmodule-table" name="offlineForm" id="offlineForm" action="" method="post" ng-submit="send_offline_request($event) && offlineForm.$valid">
                            <div class="chat-cmodule-table-row">
                                <div class="chat-cmodule-view chat-cmodule-table">
                                    <div class="chat-cmodule-row">
                                        <div ng-show="settings.offline_support_type == 'message'" class="chat-cmodule-widget-content">
                                            <div class="cmodule-help-message" ng-bind-html="settings.offline_message"></div>
                                        </div>
                                        
                                        <div ng-show="settings.offline_support_type == 'form'" class="chat-cmodule-widget-content">
                                            <div class="cmodule-help-message" ng-bind-html="settings.offline_heading_message"></div>
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="text" name="name" placeholder="<?php echo $this->lang->line('name'); ?>" ng-class="{'cmodule-error': offlineForm.name.$dirty && offlineForm.name.$invalid}" ng-model="visitor.name" required>
                                            </div>
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="email" name="email" placeholder="<?php echo $this->lang->line('email'); ?>" ng-class="{'cmodule-error': offlineForm.email.$dirty && offlineForm.email.$invalid}" ng-model="visitor.email" required ng-pattern="emailFormat">
                                            </div>
                                            <div class="cmodule-form-group" ng-if="settings.show_contact_number_input == 'yes'">
                                                <input class="cmodule-form-control" type="text" name="contact_number" placeholder="<?php echo $this->lang->line('placeholder_contact_number'); ?>" ng-model="visitor.contact_number">
                                            </div>
                                            <div class="cmodule-form-group" ng-show="settings.show_depaertment_selection_box == 'yes'">
                                                <select class="cmodule-form-control" ng-model="visitor.requested_tag" ng-required="settings.show_depaertment_selection_box == 'yes'" name="department" ng-class="{'cmodule-error': offlineForm.department.$dirty && offlineForm.department.$invalid}">
                                                    <option value=""><?php echo $this->lang->line('choose_department'); ?></option>
                                                    <option ng-repeat="option in tags" value="{{option.id}}">{{option.tag_name}}</option>
                                                </select>
                                            </div>
                                            <div class="cmodule-form-group cmodule-last-item">
                                                <textarea name="message" class="cmodule-form-control" cols="20" rows="4" placeholder="<?php echo $this->lang->line('your_question'); ?>" ng-class="{'cmodule-error': offlineForm.message.$dirty && offlineForm.message.$invalid}" ng-model="visitor.message" required></textarea>
                                            </div>
                                        </div>
                                    </div>
                                </div>	
                            </div>	
                            <div ng-show="settings.offline_support_type == 'form'" class="chat-cmodule-footer">
                                <button id="cmodule-offline-submit" class="chatnox-btn-default" type="submit"><?php echo $this->lang->line('send_now'); ?></button>
                            </div>
                        </form>
                    </div>
                </div>
                <div ng-show="visible_widget == 'online-widget'" id="cmodule-online-widget" class="chat-cmodule-table-row">
                    <div class="chat-cmodule-widget chat-cmodule-table">
                        <form class="chat-cmodule-table" name="onlineForm" id="onlineForm" action="" method="post" ng-submit="send_request($event) && onlineForm.$valid">
                            <div class="chat-cmodule-table-row">
                                <div class="chat-cmodule-view chat-cmodule-table">
                                    <div class="chat-cmodule-row">
                                        <div class="chat-cmodule-widget-content">
                                            <div class="cmodule-help-message" ng-bind-html="settings.welcome_message"></div>
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="text" name="name" placeholder="<?php echo $this->lang->line('name'); ?>" ng-model="visitor.name" ng-class="{'cmodule-error': onlineForm.name.$dirty && onlineForm.name.$invalid}" required>
                                            </div>
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="email" name="email" placeholder="<?php echo $this->lang->line('email'); ?>" ng-model="visitor.email" ng-class="{'cmodule-error': onlineForm.email.$dirty && onlineForm.email.$invalid}" required ng-pattern="emailFormat">
                                            </div>
                                            <div class="cmodule-form-group" ng-if="settings.show_contact_number_input == 'yes'">
                                                <input class="cmodule-form-control" type="text" name="contact_number" placeholder="<?php echo $this->lang->line('placeholder_contact_number'); ?>" ng-model="visitor.contact_number">
                                            </div>
                                            <div class="cmodule-form-group" ng-show="settings.show_depaertment_selection_box == 'yes'">
                                                <select class="cmodule-form-control" ng-model="visitor.requested_tag" ng-required="settings.show_depaertment_selection_box == 'yes'" name="department" ng-class="{'cmodule-error': onlineForm.department.$dirty && onlineForm.department.$invalid}">
                                                    <option value=""><?php echo $this->lang->line('choose_department'); ?></option>
                                                    <option ng-repeat="option in tags" value="{{option.id}}">{{option.tag_name}}</option>
                                                </select>
                                            </div>
                                            <div class="cmodule-form-group cmodule-last-item">
                                                <textarea name="message" class="cmodule-form-control" cols="20" rows="4" placeholder="<?php echo $this->lang->line('your_question'); ?>" ng-model="visitor.message" ng-class="{'cmodule-error': onlineForm.message.$dirty && onlineForm.message.$invalid}" required></textarea>
                                            </div>
                                        </div>
                                    </div>
                                </div>	
                            </div>		
                            <div class="chat-cmodule-footer">
                                <button id="cmodule-online-submit" class="chatnox-btn-default" type="submit"><?php echo $this->lang->line('chat_now'); ?></button>
                            </div>
                        </form>
                    </div>
                </div>
                
                <div ng-show="visible_widget == 'initiate-form'" class="chat-cmodule-table-row">
                    <div class="chat-cmodule-widget chat-cmodule-table">
                        <form class="chat-cmodule-table" name="anonymousForm" id="anonymousForm" action="" method="post" ng-submit="anonymousForm.$valid && submit_initiate_form($event)">
                            <div class="chat-cmodule-table-row">
                                <div class="chat-cmodule-view chat-cmodule-table">
                                    <div class="chat-cmodule-row">
                                        <div class="chat-cmodule-widget-content">
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="text" name="name" placeholder="<?php echo $this->lang->line('name'); ?>" ng-model="anonymous_user.name" ng-class="{'cmodule-error': anonymousForm.name.$dirty && anonymousForm.name.$invalid}" required>
                                            </div>
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="email" name="email" placeholder="<?php echo $this->lang->line('email'); ?>" ng-model="anonymous_user.email" ng-class="{'cmodule-error': anonymousForm.email.$dirty && anonymousForm.email.$invalid}" required ng-pattern="emailFormat">
                                            </div>
                                            <div class="cmodule-form-group" ng-if="settings.show_contact_number_input == 'yes'">
                                                <input class="cmodule-form-control" type="text" name="contact_number" placeholder="<?php echo $this->lang->line('placeholder_contact_number'); ?>" ng-model="anonymous_user.contact_number">
                                            </div>
                                            <div class="cmodule-form-group cmodule-last-item">
                                                <textarea name="message" class="cmodule-form-control" cols="20" rows="4" placeholder="<?php echo $this->lang->line('your_question'); ?>" ng-model="anonymous_user.message" ng-class="{'cmodule-error': anonymousForm.message.$dirty && anonymousForm.message.$invalid}" required></textarea>
                                            </div>
                                        </div>
                                    </div>
                                </div>	
                            </div>		
                            <div class="chat-cmodule-footer">
                                <button class="chatnox-btn-default" type="submit"><?php echo $this->lang->line('chat_now'); ?></button>
                            </div>
                        </form>
                    </div>
                </div>
                
                <div ng-show="visible_widget == 'feedback-widget'" id="cmodule-feedback-widget" class="chat-cmodule-table-row">
                    <div class="chat-cmodule-table chat-cmodule-widget">
                        <form class="chat-cmodule-table" name="feedbackForm" id="feedbackForm" action="" method="post" ng-submit="send_feedback($event) && feedbackForm.$valid">
                            <div class="chat-cmodule-table-row">
                                <div class="chat-cmodule-view">
                                    <div class="chat-cmodule-row">
                                        <div class="chat-cmodule-widget-content">
                                            <div class="cmodule-help-message" ng-bind-html="settings.feedback_heading_message"></div>
                                            <div class="cmodule-form-group">
                                                <p class="cmodule-rating"><?php echo $this->lang->line('rating'); ?>: <span class="cmodule-badge-success">{{feedback.rating}}</span> <strong class="cmodule-rating-string">{{rating_status[feedback.rating]}}</strong></p>
                                                <div data-range-slider data-floor="1" data-ceiling="5" data-step="1" data-precision="2" data-highlight="left" data-ng-model="feedback.rating"></div>
                                            </div>
                                            <div class="cmodule-form-group cmodule-last-item">
                                                <textarea name="message" class="cmodule-form-control" cols="20" rows="4" placeholder="<?php echo $this->lang->line('write_your_feedback'); ?>" ng-model="feedback.feedback_text" ng-class="{'cmodule-error': feedbackForm.message.$dirty && feedbackForm.message.$invalid}" ></textarea>
                                            </div>
                                        </div>
                                    </div>
                                </div>		
                            </div>
                            <div class="chat-cmodule-footer">
                                <button id="cmodule-online-submit" class="chatnox-btn-default chatnox-btn-normal" type="submit"><?php echo $this->lang->line('submit'); ?></button>
                                <button class="chatnox-btn-cancel chatnox-btn-normal" type="button" ng-click="close_session($event)"><?php echo $this->lang->line('close'); ?></button>
                            </div>
                        </form>
                    </div>
                </div>
                <div ng-show="visible_widget == 'chatting-widget'" id="live-chat-cmodule-widget" class="chat-cmodule-table-row  theme-{{settings.theme}}" ng-class="{'cmodule-avatar-removed':settings.theme == 'bubbles'}">
                    <div class="chat-cmodule-widget chat-cmodule-table">
                        <div ng-hide="settings.theme == 'classic'" class="chat-cmodule-table-row">
                            <div class="chat-cmodule-table chat-cmodule-view">
                                <div class="chat-cmodule-widget-content" id="message_box">
                                    <div ng-show="is_waiting" class="cmodule-help-message" ng-bind-html="settings.waiting_message"></div>
                                    <div ng-repeat="row in messages| orderBy:'sort_order'" id="chat-cmodule-chat-row-{{$index}}" class="{{row.class}}" ng-class="{'chat-cmodule-message-reply-row': row.sender_id == visitor.id, 'chat-cmodule-message-row': row.sender_id != visitor.id, 'chat-cmodule-last-chat-row': $index == (messages.length - 1)}" ng-mouseover="row.class = ''"  on-last-repeat>
                                        <div ng-if="settings.theme != 'bubbles' && row.sender_id != visitor.id" class="chat-row-avatar">
                                            <img ng-show="row.profile_pic && row.sender_id != visitor.id" title="{{row.display_name}}" alt="" ng-src="{{row.profilePic}}" class="cmodule-img-circle cmodule-avatar">
                                            <span ng-hide="row.profile_pic || row.sender_id == visitor.id" ng-style="{backgroundColor: row.profile_color}" class="cmodule-user-avatar">{{row.display_name|oneCapLetter}}</span>
                                        </div>
                                        <div class="chat-cmodule-message" ng-bind-html="row.chat_message | newlines | smilies"></div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div ng-show="settings.theme == 'classic'" class="chat-cmodule-table-row">
                            <div class="chat-cmodule-table chat-cmodule-view">
                                <div class="chat-cmodule-widget-content" id="classic_message_box">
                                    <div ng-show="is_waiting" class="cmodule-help-message" ng-bind-html="settings.waiting_message"></div>
                                    <ul class="cmodule-chat-list">
                                        <li ng-repeat="row in messages| orderBy:'sort_order'" class="cmodule-chat-item {{row.class}}" ng-class="{'cmodule-even': row.sender_id == visitor.id, 'cmodule-odd': row.sender_id != visitor.id}" ng-mouseover="row.class = ''"  on-last-repeat>
                                            <div ng-show="row.sender_id != visitor.id" class="chat-user-name">{{row.display_name}}</div>
                                            <div ng-show="row.sender_id == visitor.id" class="chat-user-name">You</div>
                                            <div class="cmodule-chat-message" ng-bind-html="row.chat_message | newlines | smilies"></div>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                        <div class="cmodule-modal" ng-show="ask_to_confirm == 'yes' && confirm_close_session == 'no'" ng-class="{'cmodule-in':ask_to_confirm == 'yes'}">
                            <div class="cmodule-modal-dialog">
                                <div class="cmodule-modal-content">
                                    <div class="cmodule-modal-body"><?php echo $this->lang->line('want_to_end_chat'); ?></div>
                                    <div class="cmodule-modal-footer">
                                        <a href="#" ng-click="confirm_close_session = 'yes';end_chat($event)"><?php echo $this->lang->line('yes'); ?></a>
                                        <a href="#" ng-click="ask_to_confirm = 'no'"><?php echo $this->lang->line('no'); ?></a>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="cmodule-modal" ng-show="ask_for_transcript == 'yes'" ng-class="{'cmodule-in':ask_for_transcript == 'yes'}">
                            <div class="cmodule-modal-dialog">
                                <div class="cmodule-modal-content">
                                    <div class="cmodule-modal-body"><?php echo $this->lang->line('want_transcript_in_email'); ?></div>
                                    <div class="cmodule-modal-footer">
                                        <a ng-show="settings.initiate_bypass_chat == 'yes'" href="#" ng-click="ask_to_visitor_email = 'yes';disable_click($event)"><?php echo $this->lang->line('yes'); ?></a>
                                        <a ng-show="settings.initiate_bypass_chat == 'no'" href="#" ng-click="settings.send_chat_transcript_to_visitor = 'yes';end_chat($event)"><?php echo $this->lang->line('yes'); ?></a>
                                        <a href="#" ng-click="settings.send_chat_transcript_to_visitor = 'no';end_chat($event)"><?php echo $this->lang->line('no'); ?></a>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="cmodule-modal" ng-show="ask_to_visitor_email == 'yes'" ng-class="{'cmodule-in':ask_to_visitor_email == 'yes'}">
                            <div class="cmodule-modal-dialog">
                                <div class="cmodule-modal-content">
                                    <div class="cmodule-modal-body"><?php echo $this->lang->line('text_email_for_transcript'); ?></div>
                                    <div class="cmodule-modal-footer">
                                        <form name="vemailForm" method="post">
                                            <div class="cmodule-form-group">
                                                <input class="cmodule-form-control" type="email" name="visitor_email" placeholder="<?php echo $this->lang->line('email'); ?>" ng-model="visitor_email" ng-class="{'cmodule-error': vemailForm.visitor_email.$dirty && vemailForm.visitor_email.$invalid}" required ng-pattern="emailFormat">
                                            </div>
                                            <button type="button" class="cmodule-button" ng-disabled="!vemailForm.visitor_email.$valid" ng-click="settings.send_chat_transcript_to_visitor = 'yes';end_chat($event)"><?php echo $this->lang->line('text_send_transcript'); ?></button>
                                            <button type="button" class="cmodule-button" ng-click="settings.send_chat_transcript_to_visitor = 'no';end_chat($event)"><?php echo $this->lang->line('cancel'); ?></button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </div>
                
                        <div class="cmodule-modal cmodule-operator-offline" ng-show="!is_agents_online" ng-class="{'cmodule-in':!is_agents_online}">
                            <div class="cmodule-modal-dialog">
                                <div class="cmodule-modal-content">
                                    <div class="cmodule-modal-body" ng-bind-html="settings.operator_gone_offline_message"></div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="chat-cmodule-typing-status"  ng-show="(is_typing || (new_msg_indecator && is_scroll_start)) && ask_for_transcript != 'yes'">
                            <div ng-show="is_typing" class="chat-cmodule-message chat-cmodule-typing"> 
                                <div class="chat-cmodule-typing-indicator"></div>
                                <div class="chat-cmodule-typing-indicator"></div>
                                <div class="chat-cmodule-typing-indicator"></div>
                            </div>
                            <div ng-show="new_msg_indecator && is_scroll_start" class="chat-cmodule-new-message-indecator"></div>
                        </div>

                        <div id="chat-cmodule-footer" class="chat-cmodule-footer">
                            <form name="chatForm" id="mobile chat-form" action="" method="post" ng-submit="send_message($event) && chatForm.$valid">
                                <div class="cmodule-message-box">
                                    <textarea focus-on-change="new_message" ng-focus="chatboxState = 'focus'" ng-blur="chatboxState = 'blur'" ng-model="new_message" ng-keypress="submit_message($event)" ng-disabled="chat_session.session_status == 'closed'" id="message" cols="20" rows="2" class="cmodule-form-control" placeholder="<?php echo $this->lang->line('placeholder_message'); ?>" required></textarea>
                                </div>
                                <div class="chat-cmodule-chat-toolbar">
                                    <?php if($this->settings->enable_file_sharing == 'yes' and $this->settings->allowed_filetypes != ''):?>
                                        <span class="chat-toolbar-item" ng-show="is_file_sending && chatForm.chatfiles.$valid"><?php echo $this->lang->line('sending_file');?></span>
                                        <span class="chat-toolbar-item chat-cmodule-validation-error" ng-show="chatForm.chatfiles.$error.maxsize"><?php echo $this->lang->line('exceeded_filesize');?></span>
                                        <span class="chat-toolbar-item chat-cmodule-validation-error" ng-show="chatForm.chatfiles.$error.accept"><?php echo $this->lang->line('invalid_filetype');?></span>
                                        <span class="chat-toolbar-item chat-cmodule-chat-toolbar-btn attachment-handle">
                                            <i aria-hidden="true" class="attachment-icon" role="button" title="<?php echo $this->lang->line('send_file'); ?>"></i>
                                            <input title="<?php echo $this->lang->line('send_file'); ?>" class="chat-cmodule-input-file" on-after-validate="upload_files" onerror="file_error_handler" type="file" role="button" ng-model="chatfiles" name="chatfiles" accept="<?php echo str_replace("|", ",", $this->settings->allowed_filetypes);?>" maxsize="<?php echo $this->settings->file_upload_size;?>" base-sixty-four-input>
                                        </span>
                                    <?php endif;?>
                                    <span class="chat-toolbar-item chat-cmodule-chat-toolbar-btn smilies-handle" smilies-selector="new_message" smilies-placement="top-right" smilies-title="<?php echo $this->lang->line('smilies'); ?>"></span>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
                
                <?php if ($this->settings->hide_poweredby == 'no'): ?>
                    <div class="chat-cmodule-poweredby"><p><?php echo $this->settings->poweredby_content; ?></p></div>
                <?php endif; ?>
                    
                <div ng-show="display_loader" class="cmodule-spinner-loader">
                    <div class="cmodule-spinners">
                        <div class="cmodule-spinner-bounce"></div>
                        <div class="cmodule-spinner-bounce"></div>
                        <div class="cmodule-spinner-bounce"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <audio id="audio1">
        <source src="<?php echo theme_url("audio/ping.mp3"); ?>"></source>
    </audio>
    <style ng-bind-html="custom_styles"></style>
</body>
</html>