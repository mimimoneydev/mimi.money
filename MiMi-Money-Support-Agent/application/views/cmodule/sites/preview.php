<div class="chat-preview-panel bs-card">
    <ul class="nav nav-tabs" role="tablist">
        <li class="active"><a data-toggle="tab" role="tab" aria-controls="chat_window" href="#chat_window"><?php echo $this->lang->line('chat_window'); ?></a></li>
        <li><a data-toggle="tab" role="tab" aria-controls="offline_form" href="#offline_form"><?php echo $this->lang->line('chat_form'); ?></a></li>
        <li><a data-toggle="tab" role="tab" aria-controls="visitor_widget" href="#visitor_widget"><?php echo $this->lang->line('visitor_widget'); ?></a></li>
    </ul>

    <div class="tab-content">
        <div id="chat_window" class="active tab-pane">
            <?php include theme_path('settings/sites/chat_windows.php'); ?>
        </div>
        <div id="offline_form" class="tab-pane">
            <?php include theme_path('settings/sites/offline_forms.php'); ?>
        </div>
        
        <div id="visitor_widget" class="tab-pane" style="position: relative; min-height: 70px;">
            <div class="chat-cmodule visitor-widget-box">
                <div ng-if="settings.visitor_widget_type == 'chaticon'" class="chat-cmodule-btn-wrap cmodule-clearfix {{settings.chat_icon_size}}" id="chat-cmodule-widget-bar" style="margin: auto;">
                    <div class="cmodule-chat-btn" style="position: absolute;">
                        <a href="javascript:void(0)" title="{{settings.chat_start_title}}">
                            <i aria-hidden="true" class="cmodule-chat-icon fa fa-comments-o"></i>
                        </a>
                    </div>
                </div>

                <div ng-if="settings.visitor_widget_type == 'chatbar'" class="chat-cmodule-section" style="position: inherit;">
                    <div class="chat-cmodule-widget-head cmodule-clearfix widget-bar" id="chat-cmodule-widget-bar" style="left: 0; right: 0; margin: auto; bottom: 0; position: absolute; max-width: 100%;">
                        <div class="cmodule-window-widget-title">{{settings.chat_start_title}}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>