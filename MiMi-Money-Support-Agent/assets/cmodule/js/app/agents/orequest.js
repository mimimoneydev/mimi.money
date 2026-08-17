(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("RequestController", function ($scope, $http, $interval, $timeout, $window, Setting, $log) {
        $scope.user = Setting.user;
        $scope.request = {};
        $scope.conversations = [];

        $scope.$on('onRepeatLast', function (scope, element, attrs) {
            //work your magic
            angular.element("#message").focus();
        });
        
        // set offline requests window height.
        $scope.set_height = function () {
            var windowHeight = angular.element($window).outerHeight();
            var headerMargin = parseInt(angular.element('#offline-requests-header').css('margin-bottom'));
            var headerHeight = (angular.element('#offline-requests-header').outerHeight()) + headerMargin;
            var bottomMargin = 20;

            angular.element('.offline-requests-wrapper').height(windowHeight - headerHeight - bottomMargin);
        }
        
        $scope.set_height();
        //reset height on window resize
        angular.element($window).on('resize', function () {
            $scope.set_height();
        });

        // fetching more users
        $scope.load_more = function () {
            $scope.loading = true;

            $http.post(site_url + "?d=agents&c=orequests&m=get_requests", {offset: $scope.offset}).success(function (response) {
                if (response.result == 'success') {
                    if (response.offline_requests.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.offline_requests, function (key, row) {
                        $scope.offset++;
                        $scope.records.push(row);
                    });
                    $scope.loading = false;

                    angular.element(".offline-requests-wrapper").mCustomScrollbar({
                        /* keyboard default options */
                        keyboard: {
                            enable: true,
                            scrollType: "stepless",
                            scrollAmount: "auto"
                        }
                    });
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }
            });
        }

        $scope.load_more();

        $scope.remove = function (record, conf_message) {
            var confirm_delete = confirm(conf_message);
            if (confirm_delete) {
                $http.post(site_url + "?d=agents&c=orequests&m=delete_request&request_id=" + record.id).success(function (response) {
                    if (response.result == 'success') {
                        var index = $scope.records.indexOf(record);
                        $scope.records.splice(index, 1);

                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;
                    } else {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                });
            }
        }

        $scope.get_conversations = function (request) {
            $scope.request = request;
            $http.post(site_url + "?d=agents&c=orequests&m=get_conversations&request_id=" + request.id).success(function (response) {
                if (response.result == 'success') {
                    $scope.conversations = response.conversations;
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
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

                $http.post(site_url + "?d=agents&c=orequests&m=reply_request&request_id=" + $scope.request.id, message_data).success(function (response) {
                    if (response.result == 'success') {
                        $scope.request.request_status = 'open';
                        $scope.conversations.push(response.message_row);
                        $scope.new_message = '';
                        $scope.scroll_chat();
                    } else if (response.result == 'failed') {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                });
            }
        }

        $scope.scroll_chat = function () {
            angular.element("#offline_message_box").mCustomScrollbar('scrollTo', 'bottom', {
                scrollInertia: 100,
                timeout: 10
            });
        }

        $scope.displayError = function (data) {
            $scope.showError = true;
            $scope.errors = data.errors;
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