(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("ChathistoryController", function ($scope, $http, $timeout, $window, Setting, $log, filterService, orderByFilter) {
        $scope.filters = filterService;
        $scope.conversations = [];
        $scope.rendered_list = [];
        $scope.visitor = {};
        $scope.chat_session = {};

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

            $http.post(Setting.site_url + "?c=chat_history&m=get_chat_session", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    if (response.chat_sessions.length == 0) {
                        $scope.showNoMoreRecordAlert();
                    }

                    $.each(response.chat_sessions, function (key, row) {
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

            $http.post(Setting.site_url + "?c=chat_history&m=get_chat_session", $scope.filters).success(function (response) {
                if (response.result == 'success') {
                    $scope.offset = response.chat_sessions.length;
                    $scope.records = response.chat_sessions;
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

        // get conversations of chat between visitor and agents
        $scope.get_conversations = function (session) {
            $scope.chat_session = session;
            $http.post(Setting.site_url + "?c=chat_history&m=get_conversations&id=" + session.id).success(function (response) {
                if (response.result == 'success') {
                    $scope.conversations = response.messages;
                    $scope.visitor = response.visitor;
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
         * Reserting listing design
         * 
         * @returns {undefined}
         */
        $scope.reset_listing_design = function () {
            $scope.rendered_list = orderByFilter($scope.conversations, 'sort_order');

            angular.forEach($scope.rendered_list, function (current_row, key) {
                var prev_row = (key > 0) ? $scope.rendered_list[(key - 1)] : {};
                var next_row = (key < ($scope.rendered_list.length - 1)) ? $scope.rendered_list[(key + 1)] : {};
                var row_class = 'single';
                var same_sender = false;

                if (prev_row.sender_id) {
                    if (prev_row.sender_id == current_row.sender_id) {
                        row_class = 'ctr';
                        same_sender = true;

                        var $prev_row_ele = angular.element('#chat-row-' + (key - 1))
                        if ($prev_row_ele.hasClass('bottom') || $prev_row_ele.hasClass('single')) {
                            $prev_row_ele.removeClass('bottom').removeClass('single');
                        }
                    }
                }

                if (next_row.sender_id) {
                    if (next_row.sender_id == current_row.sender_id) {
                        if (same_sender) {
                            row_class = 'ctr';
                        } else {
                            row_class = 'top';
                        }
                    } else {
                        if (same_sender) {
                            row_class = 'bottom';
                        }
                    }
                } else {
                    if (same_sender) {
                        row_class = 'bottom';
                    }
                }

                if (row_class) {
                    if (row_class == 'top' || row_class == 'ctr') {
                        angular.element('#chat-row-' + key + ' .chat-row-avatar').remove();
                    }
                    angular.element('#chat-row-' + key).addClass(row_class);
                }
            });
        }

        $scope.$on('onRepeatLast', function (scope, element, attrs) {
            if ($scope.settings.operator_chat_theme == 'bubbles-boom') {
                $scope.reset_listing_design();
            }
        });

        /*
         * Delete chat request from server
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.delete_record = function (record) {
            $http.post(Setting.site_url + "?c=chat_history&m=delete&id=" + record.id).success(function (response) {
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