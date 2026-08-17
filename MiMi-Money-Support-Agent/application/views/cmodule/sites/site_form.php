<div class="modal-header">
    <button type="button" class="close" ng-click="close_modal($event)">&times;</button>
    <h2 class="modal-title" ng-hide="record.id"><?php echo $this->lang->line('add_new_site'); ?></h2>
    <h2 class="modal-title" ng-show="record.id"><?php echo $this->lang->line('edit_site'); ?> : {{record.lang_name}}</h2>
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

    <form name="entryForm" method="post" novalidate ng-submit="entryForm.$valid && save_record($event)">
        <div class="clearfix">
            <div class="form-group">
                <input class="form-control" type="text" name="site_name" placeholder="<?php echo $this->lang->line('site_name'); ?>" ng-model="record.site_name" required />
            </div>

            <div class="form-group">
                <input class="form-control" type="text" name="site_url" placeholder="<?php echo $this->lang->line('site_url'); ?>" ng-model="record.site_url" required />
            </div>

            <div class="form-group">
                <input class="form-control" type="email" name="site_email" placeholder="<?php echo $this->lang->line('site_email'); ?>" ng-model="record.site_email" required />
            </div>

            <div class="form-group">
                <div class="input-group" ng-class="{'ng-invalid-border':(entryForm.$submitted || entryForm.$touched) && entryForm.entry_image.$invalid}">
                    <span class="input-group-btn">
                        <span class="btn btn-sn btn-default btn-file">
                            Browse&hellip; <input type="file" name="entry_image" ng-model="mainctr.entry_image" accept="image/*" base-sixty-four-input>
                        </span>
                    </span>
                    <input type="text" ng-model="mainctr.entry_image.filename" placeholder="<?php echo $this->lang->line('site_logo'); ?>" class="form-control" disabled="disabled">
                    <span class="input-group-btn"><button ng-disabled="!mainctr.entry_image.filename" class="btn btn-primary btn-sn btn-x2" ng-click="clear_file_input($event);"><?php echo $this->lang->line('clear'); ?></button></span>
                </div>
                <p class="text-danger" ng-show="entryForm.entry_image.$error.maxsize"><?php echo $this->lang->line('exceeded_filesize'); ?></p>
                <p class="text-danger" ng-show="entryForm.entry_image.$error.accept"><?php echo $this->lang->line('invalid_filetype'); ?></p>
            </div>

            <div class="form-group-images form-group">
                <div class="row">
                    <div class="col-sm-6">
                        <img ng-if="mainctr.entry_image.filename" class="img-responsive" ng-src="{{'data:'+mainctr.entry_image.filetype+';base64,'+mainctr.entry_image.base64}}" alt="{{record.site_name}}">
                        <img ng-if="record.site_logo && !mainctr.entry_image.filename" class="img-responsive" ng-src="{{record.site_logo}}" alt="{{record.site_name}}">
                    </div>
                </div>
            </div>
        </div>

        <div class="form-footer">
            <button class="btn btn-primary" type="submit"><?php echo $this->lang->line('submit_save'); ?></button>
            <button class="btn btn-default" type="button" ng-click="close_modal($event)"><?php echo $this->lang->line('submit_cancel'); ?></button>
        </div>
    </form>
</div>
