<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<link href="<?php echo base_url('assets/cmodule-chat/css/chatbox-desktop.min.css'); ?>" rel="stylesheet">
<script type="text/javascript">
    var cmodule_site_url = '<?php echo site_url(); ?>';
    var cmodule_base_url = '<?php echo base_url(); ?>';
    var access_token = '<?php echo $this->access_token; ?>';

    var windowName = window.name;
    var strArray = windowName.split('[!]');
    var ptitle = strArray[0];
    var purl = strArray[1];
    var siteuser = '<?php echo json_encode($siteuser); ?>';
    var cbwindow = <?php echo json_encode($cbwindow); ?>;
</script>
<style>
.chat-cmodule-container .chat-cmodule-widget{border: 1px solid <?php echo hex2rgba($this->settings->background_color, 0.4);?>;border-bottom: 0 none;}
.chat-cmodule-footer{border-top: 1px solid <?php echo hex2rgba($this->settings->background_color, 0.4);?>;}
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
    <div ng-controller="BodyController" ng-cloak>  
        <?php include VIEWPATH . 'chatbox/minibox.php'; ?>
    </div>
    <script src="<?php echo base_url('assets/cmodule-chat/js/chatbox-desktop.min.js'); ?>"></script>
</body>
</html>