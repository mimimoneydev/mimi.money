<div class="modal-header">
    <button type="button" class="close" ng-click="back_to_files($event)">&times;</button>
    <h2 class="modal-title"><?php echo $this->lang->line('edit'); ?>: {{current_file}}</h2>
</div>
<div class="modal-body">
    <div class="alert alert-success" ng-show="notification.showMessage">
        <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
        <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
    </div>

    <div class="alert alert-danger" ng-show="notification.showErrors">
        <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
        <div ng-bind-html="notification.errors"></div>
    </div>

    <form name="translateForm" method="post" ng-submit="translateForm.$valid && translate_data($event)" novalidate>
        <div ng-repeat="(keyname, keyval) in filedata" class="form-group">
            <label for="{{keyname}}">{{copied_data[keyname]}}</label>
            <textarea rows="2" class="form-control" placeholder="Department Name" name="{{keyname}}" ng-model="filedata[keyname]" required></textarea>
        </div>

        <div class="form-footer">
            <button class="btn btn-primary" type="submit" ng-disabled="!translateForm.$valid"><?php echo $this->lang->line('submit_save'); ?></button>
            <button class="btn btn-default" type="button" ng-click="back_to_files($event)"><?php echo $this->lang->line('go_back'); ?></button>
        </div>
    </form>
</div>
