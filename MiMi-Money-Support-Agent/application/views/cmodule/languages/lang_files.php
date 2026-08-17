<div class="modal-header">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true" id="close-model">&times;</button>
    <h2 class="modal-title" ng-show="record.id">{{record.lang_name}} : <?php echo $this->lang->line('lang_files'); ?></h2>
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

    <div class="language-files-list">
        <div class="row">
            <div class="col-xs-12">
                <div class="files-container" ng-if="lang_files.length > 0">
                    <ul class="list-group">
                        <li class="list-group-item col-xs-6" ng-repeat="file in lang_files" ng-if="is_editable(file)"><a title="<?php echo $this->lang->line('click_to_edit'); ?>" href="#" ng-click="translate_flie($event, file)">{{file}}</a></li>
                    </ul>
                </div>
                
                <div class="no-files" ng-hide="lang_files.length > 0"><?php echo $this->lang->line('no_record_found'); ?></div>
            </div>
        </div>
    </div>
</div>
