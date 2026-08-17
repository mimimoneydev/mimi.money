(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("RequestController", function ($scope, $http, $timeout, $window, Setting, $log) {
        $scope.user = cmodule.user;
        $scope.request = {};
        $scope.conversations = [];

        $scope.$on('onRepeatLast', function (scope, element, attrs) {
            //work your magic
            angular.element("#message").focus();
        });

        // fetching more users
        $scope.load_more = function () {
            $scope.loading = true;

            $http.post(site_url + "?c=orequests&m=get_requests", {offset: $scope.offset}).success(function (response) {
                if (response.result == 'success') {
                    if (response.offline_requests.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.offline_requests, function (key, row) {
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

        $scope.get_conversations = function (request) {
            $scope.request = angular.copy(request);
            $http.post(site_url + "?c=orequests&m=get_conversations&id=" + request.id).success(function (response) {
                if (response.result == 'success') {
                    $scope.conversations = response.conversations;
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

        // sending new message 
        $scope.send_message = function (event) {
            event.preventDefault();
            if ($scope.new_message) {

                //prepare json data
                var message_data = {
                    message: $scope.new_message,
                    sender_id: $scope.user.id
                };

                $http.post(site_url + "?c=orequests&m=reply_request&id=" + $scope.request.id, message_data).success(function (response) {
                    if (response.result == 'success') {
                        $scope.conversations.push(response.message_row);
                        $scope.new_message = '';
                        $scope.scroll_chat();
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

        $scope.scroll_chat = function () {
            angular.element("#message_box").scrollTop(angular.element("#message_box")[0].scrollHeight);
        }

        $scope.displayError = function (data) {
            $scope.showError = true;
            $scope.errors = data.error;
        }

        /*
         * Delete offline request from server
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.delete_record = function (record) {
            $http.post(Setting.site_url + "?c=orequests&m=delete&id=" + record.id).success(function (response) {
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
    }).directive('onLastRepeat', function () {
        return function (scope, element, attrs) {
            if (scope.$last) {
                setTimeout(function () {
                    scope.$emit('onRepeatLast', element, attrs);
                }, 1);
            }
        };
    });
})();