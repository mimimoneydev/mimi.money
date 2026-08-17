(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("CannedMessagesController", function ($scope, $http, $timeout, Setting, $log, filterService) {
        $scope.filters = filterService;
        $scope.canned = cmodule.canned;

        // filtring with agents
        $scope.$watchCollection("filters.keywords", function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.filters_canned_messages();
            }
        });

        // fetching more users
        $scope.load_more = function () {
            $scope.loading = true;
            $scope.filters.offset = $scope.offset;

            $http.post(site_url + "?c=canned_messages&m=get_messages", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    if (response.canned_messages.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.canned_messages, function (key, row) {
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
        $scope.filters_canned_messages = function () {
            $scope.filters.offset = 0;

            $http.post(site_url + "?c=canned_messages&m=get_messages", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    $scope.offset = response.canned_messages.length;
                    $scope.records = response.canned_messages;
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
         * To handle server error 
         * @param {type} response
         * @returns {undefined}
         */
        $scope.handleServerError = function (response) {
            console.log(response);
            console.log('There is something issue in API response.');
            $scope.output.description = '<strong>' + response.status + ' ' + response.statusText + ' !</strong> ';
            $scope.output.description += response.data;

            /*$timeout(function () {
             $scope.output = {result: 'error', description: ''};
             }, 3000);*/
        }

        /*
         * Reset form data
         * @param {type} event
         * @returns {undefined}
         */
        $scope.resetForm = function (event) {
            event.preventDefault();

            $scope.record = {};
        }

        /*
         * To add new entry
         * @param Event event
         * @returns {undefined}
         */
        $scope.addMessage = function (event) {
            event.preventDefault();

            $scope.form_title = $scope.canned.canned_form_title;
            $scope.record = {};
        }

        /*
         * To edit entry
         * @param Event event
         * @param Object message
         * @returns {undefined}
         */
        $scope.editMessage = function (event, message) {
            event.preventDefault();
            var index = $scope.records.indexOf(message);
            $scope.form_title = $scope.canned.canned_edit + ' - ' + message.title;
            $scope.record = angular.copy(message);
            $scope.record.row_index = index;
        }

        /*
         * To save new entry
         * @returns {undefined}
         */
        $scope.saveMessage = function () {
            $http.post(site_url + "?c=canned_messages&m=save", $scope.record).then(function (response) {
                if (response.data.result == 'success') {
                    if (response.data.is_new) {
                        $scope.records.push(response.data.created)
                    } else {
                        $scope.records[$scope.record.row_index] = $scope.record;
                    }

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.data.description;
                    $scope.record = {};

                    angular.element('#formblock').modal('hide');
                    $scope.entryForm.$setPristine();
                } else {
                    if (response.data.error_type && response.data.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.data.errors;
                    } else {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.data.description;
                    }
                }
            }, function (response) {
                $scope.handleServerError(response);
            });
        }

        // delete entry from server
        $scope.deleteMessage = function (message) {
            $http.get(site_url + "?c=canned_messages&m=delete&id=" + message.id).then(function (response) {
                if (response.data.result == 'success') {
                    var index = $scope.records.indexOf(message);
                    $scope.records.splice(index, 1);

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.data.description;
                } else {
                    if (response.data.error_type && response.data.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.data.errors;
                    } else {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.data.description;
                    }
                }
            }, function (response) {
                $scope.handleServerError(response);
            });
        }

    });

    var controlle_name = 'helper-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("FilterCtrl", function ($scope, $http, filterService) {
        $scope.filters = filterService;
    });
})();