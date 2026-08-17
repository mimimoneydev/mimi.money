<div class="settings-inner-container">
    <form name="settingForm" class="choose-theme" method="post" ng-submit="settingForm.$valid && update_settings($event)" novalidate>
        <div class="row">
            <div class="col-md-8">
                <div class="form-group">
                    <h5><?php echo $this->lang->line('licence_key'); ?></h5>
                    <div ng-show="settings.plugin_validated == 'yes' && !verify_lkey" class="input-group">
                        <ul class="list-inline">
                            <li><span title="<?php echo $this->lang->line('verified'); ?>" class="label label-verified"><?php echo $this->lang->line('verified'); ?></span> <span>{{settings.licence_key}}</span></li>
                            <li><a class="btn btn-primary btn-sm text-uppercase" title="<?php echo $this->lang->line('change_key'); ?>" href="#" ng-click="toggle_license_key($event)"><?php echo $this->lang->line('change'); ?></a></li>
                            <li><a class="btn btn-default btn-sm text-uppercase" title="<?php echo $this->lang->line('reset_key'); ?>" href="#" ng-confirm-message="<?php echo $this->lang->line('reset_confirm'); ?>" ng-confirm-click="unregister()"><?php echo $this->lang->line('remove'); ?></a></li>
                        </ul>
                    </div>
                    <div ng-if="settings.plugin_validated == 'no' || verify_lkey" class="row">
                        <div class="col-xs-8"><input class="form-control" type="text" name="license_key" ng-model="plugin.license_key" required /></div>
                        <div class="col-xs-4 text-right">
                            <button class="btn btn-success btn-x2 text-uppercase" ng-click="verify_license_key($event)" ng-disabled="!plugin.license_key"><?php echo $this->lang->line('verify'); ?></button>
                            <button ng-if="settings.plugin_validated == 'yes'" class="btn btn-default btn-x2 text-uppercase" ng-click="toggle_license_key($event)"><?php echo $this->lang->line('cancel'); ?></button>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('google_map_api_key'); ?></h5>
                    <input class="form-control" type="text" name="google_map_api_key" ng-model="settings.google_map_api_key" />
                    <p class="help-block small"><?php echo sprintf($this->lang->line('google_map_api_key_help'), 'https://developers.google.com/maps/documentation/javascript/get-api-key'); ?></p>
                </div>
                
                <div ng-show="settings.google_map_api_key" class="form-group animated" ng-class="{'bounceInLeft':settings.google_map_api_key,'bounceOutRight':!settings.google_map_api_key}">
                    <h5><?php echo $this->lang->line('google_map_zoom'); ?></h5>
                    <input class="form-control" type="number" min="2" ng-min="2" name="google_map_zoom" ng-model="settings.google_map_zoom" required />
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('admin_panel_logo'); ?></h5>                                            
                    <div class="input-group">
                        <span class="input-group-btn">
                            <span class="btn btn-sn btn-default btn-file">
                                Browse&hellip; <input type="file" name="admin_logo" ng-model="mainctr.admin_logo" accept="image/*" base-sixty-four-input>
                            </span>
                        </span>
                        <input type="text" ng-model="mainctr.admin_logo.filename" class="form-control" disabled="disabled">
                        <span class="input-group-btn"><button class="btn btn-primary btn-sn btn-x2" ng-disabled="!mainctr.admin_logo.filename" ng-click="clear_file_input($event)"><?php echo $this->lang->line('clear'); ?></button></span>
                    </div>
                </div>

                <div class="form-group-images form-group" ng-if="sitectr.admin_logo.filename || settings.admin_panel_logo">
                    <div class="row">
                        <div class="col-sm-4">
                            <img ng-if="mainctr.admin_logo.filename" class="img-responsive" ng-src="{{'data:'+mainctr.admin_logo.filetype+';base64,'+mainctr.admin_logo.base64}}" alt="{{settings.admin_panel_name}}">
                            <img ng-if="settings.admin_panel_logo && !mainctr.admin_logo.filename" class="img-responsive" ng-src="{{settings.admin_panel_logo}}" alt="{{settings.admin_panel_name}}">
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('admin_panel_name'); ?></h5>
                    <input class="form-control" type="text" ng-model="settings.admin_panel_name" required />
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('admin_panel_email'); ?></h5>
                    <input class="form-control" type="email" ng-model="settings.admin_panel_email" required />
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('operator_chat_theme'); ?></h5>
                    <div class="radio-list">
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="operator_chat_theme-bubbles-boom" name="operator_chat_theme" ng-model="settings.operator_chat_theme" value="bubbles-boom">
                            <label for="operator_chat_theme-bubbles-boom"><?php echo $this->lang->line('theme_bubbles_boom'); ?></label>
                        </div>
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="operator_chat_theme-theme-classic" name="operator_chat_theme" ng-model="settings.operator_chat_theme" value="theme-classic">
                            <label for="operator_chat_theme-theme-classic"><?php echo $this->lang->line('theme_classic'); ?></label>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <h5>
                        <?php echo $this->lang->line('agent_time_interwal'); ?> <span class="small">( <?php echo $this->lang->line('in_seconds'); ?> )</span>
                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" uib-tooltip="<?php echo $this->lang->line('agent_time_interwal_help'); ?>"><i class="fa fa-question-circle"></i></a>
                    </h5>
                    <input class="form-control" type="number" min="3" max="20" ng-min="3" ng-max="20" name="agent_time_interwal" ng-model="settings.agent_time_interwal" required />
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('notify_all_requests_admin'); ?></h5>
                    <div class="radio-list">
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="notify_all_requests_admin-yes" name="notify_all_requests_admin" ng-model="settings.notify_all_requests_admin" value="yes">
                            <label for="notify_all_requests_admin-yes"><?php echo $this->lang->line('yes'); ?></label>
                        </div>
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="notify_all_requests_admin-no" name="notify_all_requests_admin" ng-model="settings.notify_all_requests_admin" value="no">
                            <label for="notify_all_requests_admin-no"><?php echo $this->lang->line('no'); ?></label>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('enable_agent_initiate_chats'); ?></h5>
                    <div class="radio-list">
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="enable_agent_initiate_chats-yes" name="enable_agent_initiate_chats" ng-model="settings.enable_agent_initiate_chats" value="yes"> 
                            <label for="enable_agent_initiate_chats-yes"><?php echo $this->lang->line('yes'); ?></label>
                        </div>
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="enable_agent_initiate_chats-no" name="enable_agent_initiate_chats" ng-model="settings.enable_agent_initiate_chats" value="no"> 
                            <label for="enable_agent_initiate_chats-no"><?php echo $this->lang->line('no'); ?></label>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('enable_canned_messages'); ?></h5>
                    <div class="radio-list">
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="enable_canned_messages-yes" name="enable_canned_messages" ng-model="settings.enable_canned_messages" value="yes"> 
                            <label for="enable_canned_messages-yes"><?php echo $this->lang->line('yes'); ?></label>
                        </div>    
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="enable_canned_messages-no" name="enable_canned_messages" ng-model="settings.enable_canned_messages" value="no">
                            <label for="enable_canned_messages-no"><?php echo $this->lang->line('no'); ?></label>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <h5><?php echo $this->lang->line('enable_agent_file_sharing'); ?></h5>
                    <div class="radio-list">
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="enable_agent_file_sharing-yes" name="enable_agent_file_sharing" ng-model="settings.enable_agent_file_sharing" value="yes"> 
                            <label for="enable_agent_file_sharing-yes"><?php echo $this->lang->line('yes'); ?></label>
                        </div>
                        <div class="radio radio-replace radio-inline">
                            <input type="radio" id="enable_agent_file_sharing-no" name="enable_agent_file_sharing" ng-model="settings.enable_agent_file_sharing" value="no"> 
                            <label for="enable_agent_file_sharing-no"><?php echo $this->lang->line('no'); ?></label>
                        </div>
                    </div>
                </div>

                <div ng-hide="settings.enable_agent_file_sharing == 'no'" class="form-group animated" ng-class="{'bounceInLeft':settings.enable_agent_file_sharing == 'yes','bounceOutRight':settings.enable_agent_file_sharing == 'no'}">
                    <h5>
                        <?php echo $this->lang->line('agent_allowed_filetypes'); ?>
                        <a href="#" ng-click="disable_click($event)" data-toggle="tooltip" tooltip-placement="auto" tooltip-append-to-body="true" uib-tooltip-html="'<?php echo $this->lang->line('allowed_filetypes_help'); ?>'"><i class="fa fa-question-circle"></i></a>
                    </h5>
                    <input class="form-control" type="text" name="agent_allowed_filetypes" ng-model="settings.agent_allowed_filetypes" required />
                    <p class="help-block"><?php echo $this->lang->line('allowed_filetypes_example'); ?></p>
                </div>

                <div ng-hide="settings.enable_agent_file_sharing == 'no'" class="form-group animated" ng-class="{'bounceInLeft':settings.enable_agent_file_sharing == 'yes','bounceOutRight':settings.enable_agent_file_sharing == 'no'}">
                    <h5>
                        <?php echo $this->lang->line('agent_file_upload_size'); ?> <span class="small">( <?php echo $this->lang->line('filesize_unit'); ?> )</span>
                    </h5>
                    <input class="form-control" type="number" min="500" ng-min="500" name="agent_file_upload_size" ng-model="settings.agent_file_upload_size" required />
                </div>

                <div class="form-group form-action">
                    <button class="btn btn-primary"><?php echo $this->lang->line('submit_save'); ?></button>
                </div>
            </div>
        </div>
    </form>
</div>