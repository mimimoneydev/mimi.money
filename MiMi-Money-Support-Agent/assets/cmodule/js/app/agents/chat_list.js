(function () {
    var controlle_name = 'sidebar-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("ChatlistController", function ($scope, $http, $interval, $timeout, $filter, Setting, $log, operatorPanel) {
        $scope.operatorPanel = operatorPanel;
        var orderBy = $filter('orderBy');
        $scope.chat_history = [];
        $scope.chat_session_id = cmodule.chat_session_id;
        $scope.anonymous_user_id = 0;
        $scope.user = $scope.agent = Setting.user;
        $scope.last_id = 0;
        $scope.loaded_sessions = [];
        $scope.in_process = false;
        $scope.counter = 0;
        $scope.time_interval = parseInt(Setting.settings.agent_time_interwal) * 1000;

        /*
         * get closed chats
         * @param {type} showLoder
         * @returns {undefined}
         */
        $scope.get_closed_chats = function (showLoder) {
            if (showLoder) {
                $scope.toggleLoder();
            }

            $scope.in_process = true;

            $http.post(Setting.site_url + "?d=agents&c=chat&m=closed", {excepts: $scope.loaded_sessions}).success(function (response) {
                if (response.result == 'success') {
                    angular.forEach(response.data.closed_chats, function (row, key) {
                        if ($.inArray(row.id, $scope.loaded_sessions) == -1) {
                            row.message_id = parseFloat(row.message_id);
                            $scope.chat_history.push(row);
                            $scope.loaded_sessions.push(row.id);
                        }
                    });
                }

                if (showLoder) {
                    $scope.toggleLoder();
                }

                $scope.in_process = false;
                $scope.counter++;
            });
        }

        $scope.$watch("loaded_sessions", function (response, stored) {
            angular.element(".chat-sidebar").mCustomScrollbar({
                /* keyboard default options */
                keyboard: {
                    enable: true,
                    scrollType: "stepless",
                    scrollAmount: "auto"
                },
                callbacks: {
                    onScrollStart: function () {

                    },
                    whileScrolling: function () {
                        if (this.mcs.topPct > 98 && $scope.in_process === false) {
                            //$scope.get_closed_chats();
                        }
                    },
                    onScroll: function () {
                        if (this.mcs.topPct > 90 && $scope.in_process === false && $scope.counter < 6) {
                            $scope.get_closed_chats(false);
                        }
                    },
                    onTotalScroll: function () {
                        if ($scope.in_process === false && $scope.counter > 5) {
                            $scope.get_closed_chats(false);
                        }
                    }
                }
            });
        });

        // init requests
        $scope.get_related_data();

        $interval(function () {
            $scope.get_related_data();
        }, $scope.time_interval);

        /*
         * To paly sound id condition ox
         */

        $scope.canPlaySound = function (current_session_id, list_session_id, message_count) {
            if (!current_session_id && current_session_id !== list_session_id && message_count > 0) {
                $scope.play();
            }
        }

        // get chat session
        $scope.get_chat_session = function (record) {
            $scope.toggleLoder();

            if ($scope.chat_session_id != record.id) {
                //$scope.operatorPanel.right_sidebar = false;
            }

            $scope.storeTypedMessage($scope.chat_session_id);
            $scope.chat_session_id = record.id;
            $scope.anonymous_user_id = 0;

            $http.post(Setting.site_url + "?d=agents&c=chat&m=get_session&csid=" + $scope.chat_session_id).success(function (response) {
                if (response.result == 'success') {
                    response.chat_session_id = $scope.chat_session_id;
                    $scope.set_chat_session(response);
                }

                $scope.toggleLoder();
            });
        }

        /*
         * Check Visitor is connected to initiated chat request
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.is_visitor_connected = function (record) {
            if (record.chat_session_id > 0) {
                if (record.id == $scope.anonymous_user_id) {
                    $timeout(function () {
                        angular.element('.nav-tabs a[href="#recent-chats"]').tab('show');
                        $scope.fire_trigger('#chat-session-' + record.chat_session_id, 'click');
                        $scope.change_visible_area('workroom');
                        $scope.anonymous_user_id = 0;
                    }, 3000);
                }

                $http.post(Setting.site_url + "?d=agents&c=anonymous_chat&m=close&visitor_id=" + record.id).success(function (response) {
                    if (response.result == 'success') {

                    }
                });
            }
        }

        /*
         * Get all message of anonymous visitor and set globaly
         */
        $scope.getAnonymousUser = function (record) {
            $scope.toggleLoder();
            
            if ($scope.anonymous_user_id != record.id) {
                //$scope.operatorPanel.right_sidebar = false;
            }

            $scope.chat_session_id = 0;
            $scope.anonymous_user_id = record.id;
            record.counter = (record.counter) ? record.counter++ : 1;

            $http.post(Setting.site_url + "?d=agents&c=anonymous_chat&m=get_messages&visitor_id=" + $scope.anonymous_user_id).success(function (response) {
                if (response.result == 'success') {
                    record.messages = response.messages;
                    $scope.show_anonymous_user(record);
                }

                $scope.toggleLoder();
            });
        }

    });
})();