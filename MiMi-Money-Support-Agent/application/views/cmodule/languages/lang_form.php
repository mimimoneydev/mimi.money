<div class="modal-header">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
    <h2 class="modal-title" ng-hide="record.id"><?php echo $this->lang->line('add_new_lang'); ?></h2>
    <h2 class="modal-title" ng-show="record.id"><?php echo $this->lang->line('edit_lang'); ?> : {{record.lang_name}}</h2>
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

    <form name="langForm" method="post" novalidate ng-submit="langForm.$valid && save_record($event, lang_image)">
        <div class="form-group">
            <input class="form-control" type="text" name="lang_name" placeholder="<?php echo $this->lang->line('lang_name'); ?>" ng-model="record.lang_name" required />
        </div>

        <div ng-if="!record.id" class="form-group">
            <input class="form-control" type="text" name="machine_name" placeholder="<?php echo $this->lang->line('machine_name'); ?>" ng-model="record.machine_name" required />
            <p class="help-block small"><?php echo $this->lang->line('machine_name_help'); ?></p>
        </div>

        <div class="form-group">
            <div class="input-group" ng-class="{'ng-invalid-border':(langForm.$submitted || langForm.$touched) && langForm.lang_image.$invalid}">
                <span class="input-group-btn">
                    <span class="btn btn-sn btn-default btn-file">
                        Browse&hellip; <input type="file" name="lang_image" ng-model="lang_image" accept="image/*" base-sixty-four-input>
                    </span>
                </span>
                <input type="text" ng-model="lang_image.filename" class="form-control" disabled="disabled">
                <span class="input-group-btn"><button ng-disabled="!lang_image.filename" class="btn btn-primary btn-sn btn-x2" ng-click="clear_file_input($event);lang_image = {}"><?php echo $this->lang->line('clear'); ?></button></span>
            </div>
            <p class="text-danger" ng-show="langForm.lang_image.$error.maxsize"><?php echo $this->lang->line('exceeded_filesize'); ?></p>
            <p class="text-danger" ng-show="langForm.lang_image.$error.accept"><?php echo $this->lang->line('invalid_filetype'); ?></p>
        </div>

        <div class="form-group-images form-group" ng-if="lang_image.filename || record.lang_flag">
            <div class="row">
                <div class="col-sm-4">
                    <img ng-if="lang_image.filename" class="img-responsive text-right" ng-src="{{'data:'+lang_image.filetype+';base64,'+lang_image.base64}}" alt="{{record.lang_flag}}">
                <img ng-if="record.lang_flag && !lang_image.filename" class="img-responsive" ng-src="{{record.lang_flag}}" alt="{{record.lang_flag}}">
                </div>
            </div>
        </div>

        <div class="form-footer">
            <button class="btn btn-primary" type="submit"><?php echo $this->lang->line('submit_save'); ?></button>
            <button class="btn btn-default" type="button" ng-click="close_modal($event)"><?php echo $this->lang->line('submit_cancel'); ?></button>
        </div>
    </form>
</div>
