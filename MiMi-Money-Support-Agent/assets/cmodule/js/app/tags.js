(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("TagController", function ($scope, $http, $timeout, Setting, $log) {
        $scope.tag = {'tag_status': 'publish'};
        $scope.records = [];

        // fetching user on ready
        $http.post(site_url + "?c=tags&m=get_tags", {offset: $scope.offset}).success(function (response) {
            if (response.result == 'success') {
                if (response.departments) {
                    $scope.offset = response.departments.length;
                    $scope.records = response.departments;
                }
            }
        });

        // fetching more users
        $scope.load_more = function () {
            $scope.loading = true;

            $http.post(site_url + "?c=tags&m=get_tags", {offset: $scope.offset}).success(function (response) {
                if (response.result == 'success') {
                    if (response.departments.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.departments, function (key, row) {
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

        // this function will be call to add user.
        $scope.reset_tag = function () {
            $scope.tag = {'tag_status': 'publish'};
            $scope.is_edit = false;

            // to handle form validation reset
            $scope.tagForm.$setPristine();
        }

        // update user
        $scope.edit = function (record) {
            $scope.tag = angular.copy(record);
            $scope.tag.index = $scope.records.indexOf(record);
            $scope.is_edit = true;
        }

        // save user
        $scope.save_tag = function (event) {
            event.preventDefault();
            if ($scope.is_edit && $scope.tag.id) {
                $http.post(site_url + "?c=tags&m=update_tag&id=" + $scope.tag.id, $scope.tag).success(function (response) {
                    if (response.result == 'success') {
                        $scope.records[$scope.tag.index] = $scope.tag;
                        $scope.reset_tag();

                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;

                        angular.element('#formblock').modal('hide');
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
                $http.post(site_url + "?c=tags&m=add_tag", $scope.tag).success(function (response) {
                    if (response.result == 'success') {
                        if($scope.records.length < 10) {
                            $scope.records.push(response.tag);
                            $scope.offset++;
                        }
                        
                        $scope.reset_tag();

                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;

                        angular.element('#formblock').modal('hide');
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

        $scope.toogle_status = function (event, record) {
            event.preventDefault();
            if (record.tag_status == 'publish') {
                $scope.unpublish(record);
            } else {
                $scope.publish(record);
            }
        }

        $scope.publish = function (record) {
            $http.post(site_url + "?c=tags&m=update_status&id=" + record.id, {tag_status: 'publish'}).success(function (response) {
                if (response.result == 'success') {
                    record.tag_status = 'publish';

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

        $scope.unpublish = function (record) {
            $http.post(site_url + "?c=tags&m=update_status&id=" + record.id, {tag_status: 'unpublish'}).success(function (response) {
                if (response.result == 'success') {
                    record.tag_status = 'unpublish';

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

        $scope.remove = function (record) {
            $http.post(site_url + "?c=tags&m=delete_tag&id=" + record.id).success(function (response) {
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
})();