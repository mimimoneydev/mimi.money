<div class="filter-container row-container clearfix" ng-controller="ProfileController as  userctr">
    <div class="col-lg-12">
        <h2><?php echo $this->lang->line('edit_profile'); ?></h2>

        <div class="alert alert-success" ng-show="notification.showMessage">
            <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
            <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
        </div>

        <div class="alert alert-danger" ng-show="notification.showErrors">
            <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
            <div ng-bind-html="notification.errors"></div>
        </div>

        <div class="bs-card">
            <form name="userForm" method="post" ng-submit="save_user($event) && userForm.$valid" novalidate>
                <input type="hidden" ng-model="user.role" value="agent" required>
                <div class="row">
                    <div class="clearfix">
                        <div class="col-sm-6">
                            <div class="form-group">
                                <input class="form-control" type="text" placeholder="Name" ng-model="user.name" required />
                            </div>

                            <div class="form-group">
                                <input class="form-control" type="text" placeholder="Display Name" ng-model="user.display_name" required />
                            </div>

                            <div class="form-group row">
                                <div class="col-sm-2 col-xs-3"><span ng-style="{'background-color':user.profile_color}" class="user-avatar" title="<?php echo $this->lang->line('profile_color'); ?>">{{user.name|oneCapLetter}}</span></div>
                                <div class="col-sm-10 col-xs-9">
                                    <input class="form-control" colorpicker colorpicker-with-input="true" colorpicker-position="bottom" colorpicker-close-on-select type="text" ng-model="user.profile_color" ng-style="{'background-color':user.profile_color, color:'#ffffff'}" ng-readonly="user.profile_color" required />
                                </div>
                            </div>

                            <div class="form-group form-group-picture" ng-if="user.profile_pic && !userctr.profile_image.base64">
                                <img class="img-responsive img-circle img-border" ng-src="{{user.profile_picture}}" alt="Profile Picture" title="" />
                                <a href="#/" ng-click="remove_photo(user.id, $event, '<?php echo $this->lang->line('confirm_deleted_ppic'); ?>')" class="remove-user-pic">
                                    <span class="fa-stack fa-lg">
                                        <i class="fa fa-circle fa-stack-2x"></i>
                                        <i class="fa fa-times fa-stack-1x fa-inverse"></i>
                                    </span>
                                </a>
                            </div>
                            
                            <div class="form-group row clearfix" ng-if="userctr.profile_image.base64">
                                <div class="col-xs-6">
                                    <div class="picture-crop-area">
                                        <img-crop image="'data:'+userctr.profile_image.filetype+';base64,'+userctr.profile_image.base64" result-image="userctr.profile_image.base64WithData" result-image-format="{{userctr.profile_image.filetype}}"></img-crop>
                                    </div>
                                </div>

                                <div ng-if="userctr.profile_image.base64WithData" class="col-xs-6">
                                    <img class="img-responsive img-circle center-block img-border" ng-src="{{userctr.profile_image.base64WithData}}" alt="Profile Picture">
                                </div>
                            </div>

                            <div class="form-group">
                                <div class="input-group" ng-class="{'ng-invalid-border':(userForm.$submitted || userForm.$touched) && userForm.profile_image.$invalid}">
                                    <span class="input-group-btn">
                                        <span class="btn btn-sn btn-default btn-file">
                                            Browse… <input type="file" ng-change="userctr.profile_image.base64WithData = ''" name="profile_image" ng-model="userctr.profile_image" accept="image/*" base-sixty-four-input>
                                        </span>
                                    </span>
                                    <input type="text" ng-model="userctr.profile_image.filename" class="form-control" disabled="disabled">
                                    <span class="input-group-btn"><button ng-disabled="!userctr.profile_image.filename" class="btn btn-primary btn-sn btn-x2" ng-click="clear_file_input($event);"><?php echo $this->lang->line('clear'); ?></button></span>
                                </div>
                                
                                <p class="text-danger" ng-show="userForm.profile_image.$error.maxsize"><?php echo $this->lang->line('exceeded_filesize'); ?></p>
                                <p class="text-danger" ng-show="userForm.profile_image.$error.accept"><?php echo $this->lang->line('invalid_filetype'); ?></p>
                            </div>
                        </div>
                        <div class="col-sm-6">
                            <div class="form-group" ng-hide="is_edit">
                                <input class="form-control" type="email" placeholder="Email" ng-model="user.email" required />
                            </div>

                            <div class="form-group">
                                <input class="form-control" type="text" placeholder="Contact Number" ng-model="user.contact_number" />
                            </div>

                            <div ng-if="sites.length" class="row">
                                <div class="col-sm-6">
                                    <div class="form-group clearfix">
                                        <label><?php echo $this->lang->line('sites'); ?></label>
                                        <div ng-repeat="site in sites" class="checkbox checkbox-replace" ng-class="{'ng-invalid-border': (userForm.$submitted || userForm.user_sites.$dirty) && userForm.user_sites.$invalid}">
                                            <input type="checkbox" id="user-site-{{$index}}" name="user_sites" checklist-model="user.sites" checklist-value="site.id" ng-required="user.sites.length == 0 && user.role == 'agent'"> 
                                            <label for="user-site-{{$index}}" ng-click="site_clicked()">{{site.site_name}}</label>
                                        </div>
                                    </div>
                                </div>

                                <div class="col-sm-6">
                                    <div ng-if="tags.length > 0" class="form-group clearfix">
                                        <label><?php echo $this->lang->line('departments'); ?></label>
                                        <div ng-repeat="tag in tags" class="checkbox checkbox-replace">
                                            <input type="checkbox" id="user-tag-{{$index}}" name="user_tags" checklist-model="user.tags" checklist-value="tag.id"> 
                                            <label for="user-tag-{{$index}}">{{tag.tag_name}}</label>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-lg-12">
                        <div class="form-footer clearfix">
                            <button class="btn btn-primary" type="submit" ng-disabled="!userForm.$valid"><?php echo $this->lang->line('submit'); ?></button>
                            <button class="btn btn-default" type="button" ng-click="cancel()"><?php echo $this->lang->line('submit_cancel'); ?></button>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>