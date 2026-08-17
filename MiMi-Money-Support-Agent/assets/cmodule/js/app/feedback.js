(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("FeedbackController", function ($scope, $http, $timeout, $window, Setting, $log, filterService) {
        $scope.filters = filterService;
        $scope.rating_status = {
            1: cmodule.lang_labels.rating_one, 
            2: cmodule.lang_labels.rating_two, 
            3: cmodule.lang_labels.rating_three, 
            4: cmodule.lang_labels.rating_four, 
            5: cmodule.lang_labels.rating_five
        };

        // filtring with agents
        $scope.$watchCollection("filters.agents", function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.filters_users();
            }
        });

        // fetching more users
        $scope.load_more = function () {
            $scope.loading = true;
            $scope.filters.offset = $scope.offset;

            $http.post(site_url + "?c=feedbacks&m=get_feddback_list", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    if (response.feddbacks.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.feddbacks, function (key, row) {
                        $scope.offset++;
                        $scope.records.push(row);
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
                $scope.loading = false;
            });
        }

        $scope.load_more();

        // fetching more users
        $scope.filters_users = function () {
            $scope.filters.offset = 0;

            $http.post(site_url + "?c=feedbacks&m=get_feddback_list", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    $scope.offset = response.feddbacks.length;
                    $scope.records = response.feddbacks;
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
         * Delete offline request from server
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.delete_record = function (record) {
            $http.post(Setting.site_url + "?c=feedbacks&m=delete&id=" + record.id).success(function (response) {
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
        $scope.agents = cmodule.agents;

        $scope.checkAll = function () {
            $scope.filters.agents = $scope.agents.map(function (item) {
                return item.id;
            });
        };

        $scope.uncheckAll = function () {
            $scope.filters.agents = [];
        };

        $scope.checkFirst = function () {
            $scope.filters.agents.splice(0, $scope.filters.agents.length);
            $scope.filters.agents.push(1);
        };
    });
})();