<div class="login-section" ng-controller="RequestController">
    <div class="request-container">
        <div class="login-content">
            <div class="login-header">
                <h2><?php echo $this->lang->line('conversation'); ?></h2>
            </div>
            <div class="login-form">
                <div class="chat-container" id="message_box">
                    <div class="chat-row" ng-repeat="row in conversations" ng-class="{'reply': row.senderId == request.uid}" ng-mouseover="row.class = ''" on-last-repeat>
                        <img ng-show="row.senderId != request.uid" class="img-circle avatar" ng-src="{{row.profilePic}}" alt="{{row.name}}" title="{{row.name}}">
                        <div class="chat-message" ng-bind-html="'<p>' + row.message + '</p>'"></div>
                    </div>
                </div>

                <form name="offlineReplyForm" id="offline-reply-form" action="" method="post" ng-submit="send_message($event)">
                    <div class="message-chat-panel">
                        <div class="form-group chat-message-box">
                            <textarea cols="40" rows="1" name="message" ng-model="new_message" id="message" placeholder="Message" class="form-control"></textarea>
                        </div>	
                        <button id="send-btn" class="btn btn-link btn-lg" ng-disabled="!new_message"><i class="fa fa-paper-plane"></i></button>
                    </div>
                </form>
            </div>	
        </div>
    </div>
</div>
