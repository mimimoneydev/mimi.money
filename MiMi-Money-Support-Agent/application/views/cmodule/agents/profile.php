<div class="row-container clearfix" ng-controller="ProfileController as  userctr">
    <div ng-show="visible_area == 'main-content'" class="col-lg-12">
        <div id="operator-profile-header" class="container-header-secondary">
            <div class="header-fixed clearfix">
                <a href="#" class="btn btn-action hidden-lg hidden-md hidden-sm pull-left" ng-click="toggleLeftSidebarDrawer($event)">
                    <i class="fa fa-bars"></i>
                </a>
                <div class="pull-left user-info-section">
                    <img ng-show="user.profile_pic" class="img-circle avatar" ng-src="{{user.profile_picture}}" alt="Profile Picture" title="" />
                    <span ng-hide="user.profile_pic" ng-style="{backgroundColor: user.profile_color}" class="user-avatar" title="{{user.name}}">{{user.name|oneCapLetter}}</span>
                    <span class="profile-info entry-title">
                        <span class="user-name">{{user.name}}</span>
                        <span class="title-2"><?php echo $this->lang->line('edit_profile'); ?></span>
                    </span>                    
                </div>
                <div class="pull-right">
                    <ul class="request-list">
                        <li><a href="" ng-click="show_new_requests($event)" class="btn btn-link"><?php echo $this->lang->line('new_requests'); ?> <span ng-show="new_requests_counter > 0">({{new_requests_counter}})</span></a></li>
                        <li><a href="<?php echo site_url('d=agents&c=orequests'); ?>" class="btn btn-link"><?php echo $this->lang->line('offline_requests'); ?>  <span ng-show="offlineRequestsCounter > 0">({{offlineRequestsCounter}})</span></a></li>
                    </ul>
                </div>
            </div>
        </div>

        <div class="operator-profile-wrapper">
            <div class="alert alert-success" ng-show="notification.showMessage">
                <a href="#" class="close" aria-label="close" ng-click="notification.showMessage = false">&times;</a>
                <div ng-bind-html="'<strong>Success!</strong> ' + notification.message"></div>            
            </div>

            <div class="alert alert-danger" ng-show="notification.showErrors">
                <a href="#" class="close" aria-label="close" ng-click="notification.showErrors = false">&times;</a>
                <div ng-bind-html="notification.errors"></div>
            </div>

            <div class="clearfix">
                <form name="userForm" action="" method="post" ng-submit="save_user($event) && userForm.$valid">
                    <input type="hidden" ng-model="user.role" value="agent" required>
                    <div class="row">
                        <div class="clearfix">
                            <div class="col-md-6">
                                <div class="form-group">
                                    <input class="form-control" type="text" placeholder="Name" ng-model="user.name" required />
                                </div>

                                <div class="form-group">
                                    <input class="form-control" type="text" placeholder="Display Name" ng-model="user.display_name" required />
                                </div>

                                <div class="form-group">
                                    <input class="form-control" type="text" placeholder="Contact Number" ng-model="user.contact_number" />
                                </div>

                                <div class="form-group row">
                                    <div class="col-sm-2 col-xs-3"><span ng-style="{'background-color':user.profile_color}" class="user-avatar" title="<?php echo $this->lang->line('profile_color'); ?>">{{user.name|oneCapLetter}}</span></div>
                                    <div class="col-sm-10 col-xs-9">
                                        <input class="form-control" colorpicker colorpicker-with-input="true" colorpicker-position="bottom" colorpicker-close-on-select type="text" ng-model="user.profile_color" ng-style="{'background-color':user.profile_color, color:'#ffffff'}" ng-readonly="user.profile_color" required />
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-6">                                
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
                        </div>
                        <div class="col-md-12">
                            <hr>
                            <div class="form-group">
                                <button class="btn btn-primary" type="submit" ng-disabled="!userForm.$valid"><?php echo $this->lang->line('submit'); ?></button>
                                <button class="btn btn-default" type="button" ng-click="cancel()"><?php echo $this->lang->line('submit_cancel'); ?></button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>