(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, ['ngImgCrop']).controller("ProfileController", function ($scope, $http, $timeout, $window, Setting, $log) {
        var userctr = this;
        userctr.profile_image = {};
        
        $scope.sites = cmodule.sites;
        $scope.tags = [];
        $scope.user = cmodule.user;
        $scope.sites_clicked = false;
        $scope.usertags = [];
        $scope.show_pass_form = true

        /*
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            userctr.profile_image = {};
        }

        /*
         * Get site departments
         * @returns {undefined}
         */
        $scope.get_departments = function () {
            $scope.user.tags = [];
            if ($scope.user.sites.length > 0) {
                $http.post(Setting.site_url + "?c=users&m=get_departments", $scope.user).success(function (response) {
                    if (response.result == 'success') {
                        $scope.tags = response.departments;
                        angular.forEach(response.departments, function(row, key) {
                            if ($.inArray(row.id, $scope.usertags) != -1) {
                                $scope.user.tags.push(row.id);
                            }
                        });
                    } else if (response.result == 'failed') {
                        if (response.error_type && response.error_type == 'login_session_expired') {
                            angular.element('#login-modal').modal('show');
                            $scope.notification.showErrors = true;
                            $scope.notification.errors = response.errors;
                        } else {
                            $scope.notification.showErrors = true;
                            $scope.notification.errors = response.errors;
                        }
                    }
                });
            } else {
                $scope.tags = [];
            }
        }
        
        $scope.site_clicked = function () {
            $scope.sites_clicked = true;
        }

        // on change users sites on user form
        $scope.$watchCollection("user.sites", function (newValue, oldValue) {
            if ($scope.sites_clicked && newValue != oldValue) {
                $scope.get_departments();
            }
        });

        // fetching user on ready
        $http.get(Setting.site_url + "?c=users&m=get&id=" + $scope.user.id).success(function (response) {
            //$scope.user = response;
            if (response.result == 'success') {
                $scope.tags = response.departments;
                $scope.user = response.user;
                $scope.usertags = angular.copy($scope.user.tags);
            }
        });

        $scope.cancel = function () {
            window.location = Setting.site_url;
        }

        $scope.toggle_password_form = function () {
            window.location = site_url;
        }

        // update password
        $scope.update_password = function (event, index) {
            event.preventDefault();
            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;

            $http.post(Setting.site_url + "?c=users&m=update_password&id=" + $scope.user.id, $scope.user).success(function (response) {
                if (response.result == 'success') {
                    $scope.user.pass = '';
                    $scope.user.confirm_pass = '';

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;
                } else if (response.result == 'failed') {
                    if (response.error_type && response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    } else {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        // save user
        $scope.save_user = function (event) {
            event.preventDefault();
            
            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;
            
            if (userctr.profile_image.filename) {
                $scope.user.filesize = userctr.profile_image.filesize;
                $scope.user.filetype = userctr.profile_image.filetype;
                $scope.user.filename = userctr.profile_image.filename;
                $scope.user.base64 = userctr.profile_image.base64;
                $scope.user.base64WithData = userctr.profile_image.base64WithData;
            }

            $http.post(Setting.site_url + "?c=users&m=update_profile&id=" + $scope.user.id, $scope.user).success(function (response) {
                if (response.result == 'success') {
                    $scope.usertags = angular.copy($scope.user.tags);
                    
                    $scope.user.profile_picture = response.profile_picture;
                    $scope.user.profile_pic = response.profile_pic;
                    userctr.profile_image = {};

                    // overriding user
                    $scope.overrideUser($scope.user);

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;
                } else if (response.result == 'failed') {
                    if (response.error_type && response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    } else {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        // remove user profile picture
        $scope.remove_photo = function (id, event, confirm_msg) {
            event.preventDefault();
            if ($window.confirm(confirm_msg)) {
                $http.post(Setting.site_url + "?c=users&m=remove_picture&id=" + id).success(function (response) {
                    if (response.result == 'success') {
                        $scope.user.profile_pic = '';
                        $scope.user.profile_picture = '';

                        // overriding user
                        $scope.overrideUser($scope.user);

                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;
                    } else if (response.result == 'failed') {
                        if (response.error_type && response.error_type == 'login_session_expired') {
                            angular.element('#login-modal').modal('show');
                            $scope.notification.showErrors = true;
                            $scope.notification.errors = response.errors;
                        } else {
                            $scope.notification.showErrors = true;
                            $scope.notification.errors = response.errors;
                        }
                    }
                });
            }
        }
    });
})();