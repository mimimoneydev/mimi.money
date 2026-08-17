<div class="" ng-controller="FeedbackController">
    <div class="header-panel clearfix">
        <h2 class="panel-title pull-left"><?php echo $this->lang->line('feedbacks'); ?></h2>
        <div class="pull-right col-filter"><a href="#/" ng-click="show_filter($event)"><i class="fa fa-filter"></i></a></div>
    </div>
    
    <div class="alert alert-success" ng-show="notification.showMessage">
        <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
        <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
    </div>

    <div class="alert alert-danger" ng-show="notification.showErrors">
        <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
        <div ng-bind-html="notification.errors"></div>
    </div>
    
    <div class="feedback-container">
        <div class="feedback-list feedback-even clearfix bs-card" ng-repeat="record in records">
            <div class="feedback-user">
                <img ng-show="record.receiver_profile_pic" class="img-circle avatar" ng-src="{{record.receiverProfilePic}}" alt="{{record.receiverName}}" title="<?php echo $this->lang->line('operator'); ?>: {{record.receiverName}}" />
                <span ng-hide="record.receiver_profile_pic" ng-style="{backgroundColor: record.receiver_profile_color}" class="user-avatar" title="<?php echo $this->lang->line('operator'); ?>: {{record.receiverName}}">{{record.receiverName|oneCapLetter}}</span>
                <h6 class="user-name">{{record.receiverName}}</h6>
            </div>
            <div class="feedback">
                <p ng-bind-html="record.message | newlines"></p>
                <p class="feedback-footer clearfix">
                    <span class="rating"><?php echo $this->lang->line('rating'); ?>: {{record.rating}}/5 <span>({{rating_status[record.rating]}})</span></span>
                    <a href="" ng-confirm-message="<?php echo $this->lang->line('confirm_deleted_feedback'); ?>" ng-confirm-click="delete_record(record)" title="<?php echo $this->lang->line('delete'); ?>"> <i class="fa fa-trash fa-lg"></i></a>
                    <cite class="feedback-by hidden-xs">
                        <img ng-show="record.sender_profile_pic" class="img-circle avatar" ng-src="{{record.senderProfilePic}}" alt="{{record.senderName}}" title="<?php echo $this->lang->line('visitor'); ?> {{record.senderName}}" />
                        <span ng-hide="record.sender_profile_pic" ng-style="{backgroundColor: record.sender_profile_color}" class="user-avatar" title="<?php echo $this->lang->line('visitor'); ?> {{record.senderName}}">{{record.senderName|oneCapLetter}}</span>
                        <span>{{record.senderName}} (<?php echo $this->lang->line('visitor'); ?>)</span>
                    </cite>
                </p>
            </div>
        </div>
    </div>
    <div class="load-more-block text-center">
        <div ng-show="showNoRecordMessage" role="alert" class="alert alert-info"> <?php echo $this->lang->line('no_record_found'); ?></div>
        <button class="btn btn-success btn-primary text-center" ng-click="load_more()"><?php echo $this->lang->line('load_more'); ?> <span ng-show="loading"><i class="fa fa-refresh fa-spin"></i></span></button>
    </div>
</div>