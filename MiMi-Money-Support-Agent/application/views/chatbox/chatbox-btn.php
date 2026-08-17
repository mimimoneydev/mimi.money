<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<link href="<?php echo base_url('assets/cmodule-chat/css/chatbox-desktop.min.css'); ?>" rel="stylesheet">
<script type="text/javascript">
    var cmodule_site_url = '<?php echo site_url();?>';
    var cmodule_base_url = '<?php echo base_url();?>';
    var access_token = '<?php echo $this->access_token;?>';

    var windowName = window.name;
    var strArray = windowName.split('[!]');
    var ptitle = strArray[0];
    var purl = strArray[1];
    var siteuser = '<?php echo json_encode($siteuser);?>';
    var cbwindow = <?php echo json_encode($cbwindow);?>;
</script>

<style>
.chat-cmodule .cmodule-chat-icon, .chat-cmodule-widget-head, .chat-cmodule .widget-bar.cmodule-agent-avatar .cmodule-window-title, .cmodule-triangle-box p {
    color: <?php echo $this->settings->title_color;?> !important;
}
.chat-cmodule .cmodule-chat-btn, .chat-cmodule-widget-head, .cmodule-triangle-box{
    background-color: <?php echo $this->settings->background_color;?> !important;
}

.cmodule-triangle-box:after {
    border-color: <?php echo $this->settings->background_color;?> transparent;
}
</style>

<?php if($this->settings->visitor_widget_type == 'chaticon' and $this->settings->enable_online_animation == 'yes'):?>
<style>
iframe.chat-cmodule-iframe {width: 112px;min-width: 112px;height: 125px;}
.chat-cmodule .cmodule-chat-btn.status-online {right: 15px; bottom: 28px;animation: pulse 2s linear infinite;}
.chat-cmodule .cmodule-chat-btn.status-online .cmodule-chat-handle {}
.chat-cmodule .large-size {width: 112px; height: 125px;}
.chat-cmodule .medium-size {width: 92px; height: 105px;}
.chat-cmodule .small-size {width: 79px; height: 92px;}

@-webkit-keyframes pulse {
    0% {
        -webkit-box-shadow: 0 0 0 0 rgba(204,169,44, 0.4);
    }
    70% {
        -webkit-box-shadow: 0 0 0 15px <?php echo hex2rgba($this->settings->background_color, 0.7);?>;
    }
    100% {
        -webkit-box-shadow: 0 0 0 0 rgba(204,169,44, 0);
    }
}
@keyframes pulse {
    0% {
        -moz-box-shadow: 0 0 0 0 rgba(204,169,44, 0.4);
        box-shadow: 0 0 0 0 rgba(204,169,44, 0.4);
    }
    65% {
        -moz-box-shadow: 0 0 0 15px <?php echo hex2rgba($this->settings->background_color, 0.7);?>;
        box-shadow: 0 0 0 15px <?php echo hex2rgba($this->settings->background_color, 0.7);?>;
    }
    100% {
        -moz-box-shadow: 0 0 0 0 rgba(204,169,44, 0);
        box-shadow: 0 0 0 0 rgba(204,169,44, 0);
    }
}
</style>
<?php endif;?>
</head>
<body ng-app="cmodule">
    <div ng-controller="BodyController" class="chat-cmodule visitor-widget-box mobile-view widget-box-<?php echo $this->settings->visitor_widget_type;?>">
        <div ng-show="form_title && settings.chat_status == 'enable' && settings.plugin_validated == 'yes'" class="chat-cmodule" id="chat-cmodule-mainContainer" ng-init="show_chatbox()">
            <div class="chat-cmodule-section" ng-class="{'chat-cmodule-minimized chat-cmodule-closed':visible_widget == 'start'}">
                <div ng-show="visible_widget == 'operator-request' && minimized != 'yes'" class="cmodule-triangle-box cmodule-widget-type-<?php echo $this->settings->visitor_widget_type;?> triangle-box-<?php echo $this->settings->chat_icon_size;?>" ng-class="{'operator-ping':visible_widget == 'operator-request' && minimized != 'yes', 'pulse-animation': settings.enable_online_animation == 'yes'}">
                    <a ng-click="minimize_chat($event)" title="<?php echo $this->lang->line('close'); ?>" class="cmodule-triangle-close" href="javascript:void(0)"></a>

                    <div class="cmodule-triangle-content">
                        <p ng-repeat="row in anonymous_messages| orderBy:'sort_order'" ng-bind-html="row.chat_message | newlines | smilies"></p>
                    </div>
                </div>

                <?php if($this->settings->visitor_widget_type == 'chatbar'):?>
                    <div ng-show="visible_widget == 'start' || visible_widget == 'operator-request'" id="chat-cmodule-widget-bar" class="chat-cmodule-widget-head cmodule-clearfix animate-show widget-bar" ng-class="{'status-online':is_agents_online,'cmodule-agent-avatar':online_agent && online_agent.id}">
                        <div ng-if="!online_agent" onclick="if ('parentIFrame' in window) window.parentIFrame.close()" class="cmodule-window-widget-title">{{form_title}}</div>
                        <div ng-if="online_agent && online_agent.id" onclick="if ('parentIFrame' in window) window.parentIFrame.close()" class="cmodule-window-widget-title">
                            <div class="cmodule-window-avatar">
                                <img ng-if="online_agent.profile_pic" class="cmodule-user-avatar cmodule-chat-handle" ng-src="{{online_agent.profile_picture}}" alt="{{online_agent.display_name}}" title="{{online_agent.display_name}}">
                                <span ng-if="!online_agent.profile_pic" ng-style="{backgroundColor: online_agent.profile_color}" class="cmodule-user-avatar">{{online_agent.display_name|oneCapLetter}}</span>
                            </div>
                            <div class="cmodule-window-title">{{online_agent.display_name}}</div>
                        </div>
                    </div>
                <?php endif;?>

                <?php if($this->settings->visitor_widget_type == 'chaticon'):?>
                    <div class="chat-cmodule-btn-wrap cmodule-clearfix <?php echo $this->settings->chat_icon_size;?>" id="chat-cmodule-widget-chaticon">
                        <div class="cmodule-chat-btn" ng-class="{'status-online':is_agents_online,'cmodule-agent-avatar':online_agent && online_agent.profile_picture}">
                            <a href="javascript:void(0)" title="{{form_title}}" onclick="if ('parentIFrame' in window) window.parentIFrame.close()">
                                <i ng-if="!online_agent || !online_agent.profile_pic" aria-hidden="true" class="cmodule-chat-icon fa fa-comments-o cmodule-chat-handle"></i>
                                <img ng-if="online_agent && online_agent.profile_pic" class="cmodule-user-avatar cmodule-chat-handle" ng-src="{{online_agent.profile_picture}}" alt="{{online_agent.display_name}}" title="{{online_agent.display_name}}">
                            </a>
                        </div>
                    </div>
                <?php endif;?>
            </div>
        </div>
    </div>
    <script src="<?php echo base_url('assets/cmodule-chat/js/chatbox-desktop.min.js'); ?>"></script>
</body>
</html>