<div class="chat-cmodule">
    <div class="chat-cmodule-container">
        <div id="chat-cmodule-header" class="chat-cmodule-header cmodule-clearfix">
            <div class="cmodule-window-avatar" ng-init="rand_color = getColor()">
                <img ng-show="settings.default_avatar" ng-src="{{settings.default_avatar}}" height="50" width="50" alt="Chatbull Avatar" title="Chatbull Avatar">
                <span ng-hide="settings.default_avatar" ng-style="{backgroundColor: rand_color}" class="cmodule-user-avatar" title="Chatbull Avatar">A</span>
            </div>
            <div class="cmodule-window-title">{{settings.offline_form_title}}</div>
            <div class="cmodule-window-controls">
                <a title="Minimize window" id="cmodule-chat-minimize" class="chat-cmodule-minimize cmodule-window-control" href="javascript:void(0)"></a> 
                <a title="End Chat" id="cmodule-chat-close" class="chat-cmodule-close cmodule-window-control" href="javascript:void(0)"></a>
            </div>
        </div>
        <div id="cmodule-online-widget" class="chat-cmodule-widget">
            <div class="chat-cmodule-view">
                <div class="chat-cmodule-row">
                    <div class="chat-cmodule-widget-content">
                        <div class="cmodule-help-message">{{settings.offline_heading_message}}</div>
                        <div class="cmodule-form-group">
                            <input class="cmodule-form-control" type="text" name="name" placeholder="Name">
                        </div>
                        <div class="cmodule-form-group">
                            <input class="cmodule-form-control" type="email" name="email" placeholder="Email">
                        </div>
                        <div class="cmodule-form-group">
                            <textarea class="cmodule-form-control" cols="20" rows="4" placeholder="Your Question...."></textarea>
                        </div>
                    </div>
                </div>
            </div>		
            <div class="chat-cmodule-footer">
                <button id="cmodule-online-submit" class="chatnox-btn-default" type="submit">Chat Now</button>
            </div>
        </div>
    </div>
</div>