(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, ['ngImgCrop']).controller("UserController", function ($scope, $http, filterService, $window, $timeout, Setting, $log) {
        var userctr = this;
        userctr.profile_image = {};

        $scope.filters = filterService;
        $scope.sites = cmodule.sites;
        $scope.tags = [];
        $scope.show_user_form = false;
        $scope.show_pass_form = false;
        $scope.show_users_list = true;
        $scope.sites_clicked = false;
        $scope.user = {role: 'agent', user_status: 'active', sites: [], tags: [], name: ''};
        $scope.usertags = [];

        // calling filter function on watch
        // filtring with keywords
        $scope.$watch("filters.keywords", function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.filters_users();
            }
        });

        // filtring with roles
        $scope.$watchCollection("filters.roles", function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.filters_users();
            }
        });

        // filtring with tags
        $scope.$watchCollection("filters.tags", function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.filters_users();
            }
        });

        // fetching user on ready
        $http.post(Setting.site_url + "?c=users&m=users_list", $scope.filters).success(function (response) {
            if (response.result == 'success') {
                $scope.offset = response.users.length;
                $scope.records = response.users;
            }
        });

        // fetching more users
        $scope.load_more = function () {
            $scope.loading = true;
            $scope.filters.offset = $scope.offset;

            $http.post(Setting.site_url + "?c=users&m=users_list", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    if (response.users.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.users, function (key, row) {
                        $scope.offset++;
                        $scope.records.push(row);
                    });
                } else if (response.result == 'failed') {
                    if (response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
                $scope.loading = false;
            });
        }

        // fetching more users
        $scope.filters_users = function () {
            $scope.filters.offset = 0;

            $http.post(Setting.site_url + "?c=users&m=users_list", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    $scope.offset = response.users.length;
                    $scope.records = response.users;
                } else if (response.result == 'failed') {
                    if (response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        /*
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            userctr.profile_image = {};
        }

        // this function will be call to toggle display user form.
        $scope.toggle_user_form = function (clearForm) {
            $scope.show_user_form = !$scope.show_user_form;
            $scope.show_users_list = !$scope.show_users_list;

            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;
            $scope.sites_clicked = false;

            $scope.usertags = [];
            userctr.profile_image = [];

            if (clearForm) {
                $scope.tags = [];
                $scope.user = {role: 'agent', user_status: 'active', sites: [], tags: []};
            }

            // to handle form validation reset
            $scope.userForm.$setPristine();
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
                        angular.forEach(response.departments, function (row, key) {
                            if ($.inArray(row.id, $scope.usertags) != -1) {
                                $scope.user.tags.push(row.id);
                            }
                        });
                    } else if (response.result == 'failed') {
                        if (response.error_type == 'login_session_expired') {
                            angular.element('#login-modal').modal('show');
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

        // this function will be call to add user.
        $scope.add = function (event) {
            event.preventDefault();
            $scope.toggle_user_form(true);
            $scope.is_edit = false;
        }

        // add new user
        $scope.get_user = function (user) {
            $http.get(Setting.site_url + "?c=users&m=get&id=" + user.id).success(function (response) {
                if (response.result == 'success') {
                    $scope.tags = response.departments;
                    $scope.user = response.user;
                    $scope.user.index = $scope.records.indexOf(user);
                    $scope.usertags = angular.copy($scope.user.tags);
                } else if (response.result == 'failed') {
                    if (response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        // update user
        $scope.edit_user = function (user) {
            $scope.toggle_user_form();
            $scope.get_user(user);
            $scope.is_edit = true;
            $scope.form_ready = true;
        }

        // save user
        $scope.save_user = function (event) {
            event.preventDefault();

            if (userctr.profile_image.filename) {
                $scope.user.filesize = userctr.profile_image.filesize;
                $scope.user.filetype = userctr.profile_image.filetype;
                $scope.user.filename = userctr.profile_image.filename;
                $scope.user.base64 = userctr.profile_image.base64;
                $scope.user.base64WithData = userctr.profile_image.base64WithData;
            }

            if ($scope.is_edit && $scope.user.id) {
                $http.post(Setting.site_url + "?c=users&m=update_user&id=" + $scope.user.id, $scope.user).success(function (response) {
                    if (response.result == 'success') {
                        response.user.profilePic = response.user.profile_picture;
                        $scope.records[$scope.user.index] = response.user;

                        $scope.toggle_user_form(true);
                        angular.element('#close-model').trigger('click');

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
            } else {
                $http.post(Setting.site_url + "?c=users&m=add_user", $scope.user).success(function (response) {
                    if (response.result == 'success') {
                        if($scope.records.length < 10) {
                            response.user.profilePic = response.user.profile_picture;
                            $scope.records.push(response.user);
                            $scope.offset++;
                        }

                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;

                        $scope.toggle_user_form(true);
                        angular.element('#close-model').trigger('click');
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

        // remove user profile picture
        $scope.remove_photo = function (id, event, confirm_msg) {
            event.preventDefault();
            if ($window.confirm(confirm_msg)) {
                $http.post(Setting.site_url + "?c=users&m=remove_picture&id=" + id).success(function (response) {
                    if (response.result == 'success') {
                        $scope.user.profile_pic = '';
                        $scope.user.profile_picture = '';

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

        // this function will be call to toggle display user password form.
        $scope.toggle_password_form = function (clearForm) {
            $scope.show_pass_form = !$scope.show_pass_form;
            $scope.show_users_list = !$scope.show_users_list;

            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;

            if (clearForm) {
                $scope.user = {role: 'agent', tags: []};
            }
        }

        //change password
        $scope.change_password = function (user) {
            $scope.toggle_password_form();
            $scope.user = user;
        }

        /*
         * update password
         * 
         * @param Event event
         * @param Int index
         * @returns {undefined}
         */
        $scope.update_password = function (event, index) {
            event.preventDefault();
            $http.post(Setting.site_url + "?c=users&m=update_password&id=" + $scope.user.id, $scope.user).success(function (response) {
                if (response.result == 'success') {
                    $scope.records[index] = response.user;
                    $scope.toggle_password_form(true);

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    angular.element('#close-passform-model').trigger('click');
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

        /*
         * Change user status
         * 
         * @param Event event
         * @param Object record
         * @returns {undefined}
         */
        $scope.toogle_status = function (event, record) {
            event.preventDefault();
            if (record.user_status == 'active') {
                $scope.block_user(record);
            } else {
                $scope.active_user(record);
            }
        }

        /*
         * Do active user
         * @param Object record
         * @returns {undefined}
         */
        $scope.active_user = function (record) {
            $http.post(Setting.site_url + "?c=users&m=update_status&id=" + record.id, {user_status: 'active'}).success(function (response) {
                if (response.result == 'success') {
                    record.user_status = 'active';

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

        /*
         * Blocked User
         * @param Object record
         * @returns {undefined}
         */
        $scope.block_user = function (record) {
            $http.post(Setting.site_url + "?c=users&m=update_status&id=" + record.id, {user_status: 'blocked'}).success(function (response) {
                if (response.result == 'success') {
                    record.user_status = 'blocked';

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

        /*
         * Delete user from server
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.delete_user = function (record) {
            $http.post(Setting.site_url + "?c=users&m=delete_user&id=" + record.id).success(function (response) {
                if (response.result == 'success') {
                    var index = $scope.records.indexOf(record);
                    $scope.records.splice(index, 1);

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
    });

    var controlle_name = 'helper-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("FilterCtrl", function ($scope, $http, filterService) {
        $scope.filters = filterService;
        $scope.tags = cmodule.tags;
        $scope.roles = cmodule.roles;
        $scope.selected_roles = [];
        $scope.selected_tags = [];

        $scope.toggle_check_roles = function (index, role_name) {
            if ($scope.selected_roles[index]) {
                $scope.filters.roles.push(role_name);
            } else {
                $scope.filters.roles.splice($.inArray(role_name, $scope.filters.roles), 1);
            }
        }

        $scope.toggle_check_tags = function (index, tag_id) {
            if ($scope.selected_tags[index]) {
                $scope.filters.tags.push(tag_id);
            } else {
                $scope.filters.tags.splice($.inArray(tag_id, $scope.filters.tags), 1);
            }
        }
    });
})();