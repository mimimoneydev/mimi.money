(function () {
    var controlle_name = 'anonymous-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("AnonymousController", function ($scope, $http, $interval, $timeout, Setting, $log, $window, operatorPanel, orderByFilter) {
        $scope.operatorPanel = operatorPanel;
        $scope.canned_messages = [];
        $scope.user = Setting.user;
        $scope.anonymous_user = {name: ''};
        $scope.showError = false;
        $scope.errors = '';
        $scope.messages = [];
        $scope.rendered_list = [];
        $scope.message_stored = [];
        $scope.new_message = '';
        $scope.anonymous_user_id = 0;

        $scope.cannedOptions = {
            selector_title: cmodule.canned.canned_messages,
            title: cmodule.canned.canned_selector_title,
            search_placeholder: cmodule.canned.canned_search_placeholder,
            no_record: cmodule.canned.canned_no_result,
            form_title: cmodule.canned.canned_form_title,
            canned_title: cmodule.canned.canned_title,
            canned_description: cmodule.canned.canned_description,
            canned_add_new: cmodule.canned.canned_add_new,
            canned_save: cmodule.canned.canned_save,
            canned_cancel: cmodule.canned.canned_cancel,
            canned_edit: cmodule.canned.canned_edit,
            canned_delete: cmodule.canned.canned_delete,
            canned_delete_confirm: cmodule.canned.canned_delete_confirm,
            confirm_del: cmodule.canned.canned_confirm_del,
            validation_required: cmodule.canned.canned_validation_required,
            listUrl: site_url + '?d=agents&c=canned_messages',
            postUrl: site_url + '?d=agents&c=canned_messages&m=save',
            deleteUrl: site_url + '?d=agents&c=canned_messages&m=delete&id=',
            user: $scope.user
        }
        
        $scope.sidebarDrawerOpen = false;
        
        /**
         * Toggle left Sidebar Drawer Open
         * @param {Event} event
         * @returns {undefined}
         */
        $scope.toggleSidebarDrawer = function(event) {
            event.preventDefault();
            
            $scope.sidebarDrawerOpen = !$scope.sidebarDrawerOpen;
        }

        /*
         * Reserting listing design
         * 
         * @returns {undefined}
         */
        $scope.reset_listing_design = function () {
            $scope.rendered_list = orderByFilter($scope.messages, 'sort_order');

            angular.forEach($scope.rendered_list, function (current_row, key) {
                var prev_row = (key > 0) ? $scope.rendered_list[(key - 1)] : {};
                var next_row = (key < ($scope.rendered_list.length - 1)) ? $scope.rendered_list[(key + 1)] : {};
                var row_class = 'single';
                var same_sender = false;

                if (prev_row.sender_id) {
                    if (prev_row.sender_id == current_row.sender_id) {
                        row_class = 'ctr';
                        same_sender = true;
                        
                        var $prev_row_ele = angular.element('#message-row-' + (key - 1))
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
                    angular.element('#message-row-' + key).addClass(row_class);
                }
            });
        }

        $scope.$on('onRepeatLast', function (scope, element, attrs) {
            if ($scope.settings.operator_chat_theme == 'bubbles-boom') {
                $scope.reset_listing_design();
            }

            $timeout(function () {
                $scope.scroll_chat();
            }, 100);
        });

        // set chat history window height.
        $scope.set_height = function () {
            var windowHeight = angular.element($window).outerHeight();
            var headerMargin = parseInt(angular.element('#anonymous-header').css('margin-bottom'));
            var headerHeight = (angular.element('#anonymous-header').outerHeight()) + headerMargin;
            var chatFormHeight = angular.element('#anonymous-chat-form').outerHeight();
            var bottomMargin = 35;

            angular.element('#anonymous-visitor-panel').height(windowHeight);
            angular.element('#list-message').height(windowHeight - (headerHeight + chatFormHeight + bottomMargin));
        }

        //get currant time
        $scope.currant_time = function () {
            $scope.miliseconds = new Date().getTime();
            var miliseconds = ($scope.miliseconds + $scope.time_difference).toString();
            $scope.message_stored.push(miliseconds);

            return miliseconds;
        }

        // get chat session
        $scope.$watch("anonymousData", function (response, stored) {
            if (response != stored && response.id && $scope.anonymous_user_id != response.id) {
                //hide notification
                $scope.notification.showMessage = false;
                $scope.notification.showErrors = false;
                $scope.new_message = '';
                $scope.anonymous_user_id = response.id;
                $scope.anonymous_user = response;
                $scope.anonymous_user.visited_pages = angular.fromJson($scope.anonymous_user.meta_data);
                $scope.messages = response.messages;

                angular.element("#list-message").mCustomScrollbar();
                angular.element("#anonymous-visitor-panel").mCustomScrollbar();
                $scope.change_visible_area('anonymous');

                // managing chatbox height
                $timeout(function () {
                    $scope.set_height();
                    $scope.operatorPanel.right_sidebar = true;
                }, 300);

                //reset height on window resize
                angular.element($window).on('resize', function () {
                    $scope.set_height();
                });
            } else if (response != stored && response.id && stored.fetched_at != response.fetched_at) {
                angular.element("#list-message").mCustomScrollbar();
                angular.element("#anonymous-visitor-panel").mCustomScrollbar();
                $scope.change_visible_area('anonymous');
                $scope.anonymous_user = response;
                $scope.anonymous_user.visited_pages = angular.fromJson($scope.anonymous_user.meta_data);

                // managing chatbox height
                $timeout(function () {
                    $scope.set_height();
                    $scope.operatorPanel.right_sidebar = true;
                }, 300);

                //reset height on window resize
                angular.element($window).on('resize', function () {
                    $scope.set_height();
                });
            }
        });

        /*
         * sending message to visitor
         * 
         * @param Event event
         * @returns {undefined}
         */
        $scope.submit_message = function (event) {
            if (event.keyCode == 13 && $scope.new_message) {
                if (!event.shiftKey) {
                    $scope.send_message(event);
                }
            } else if (event.keyCode == 13) {
                event.preventDefault();
            }
        }

        /*
         * sending message to visitor
         * 
         * @param Event event
         * @returns {undefined}
         */
        $scope.send_message = function (event) {
            event.preventDefault();
            if ($scope.new_message) {
                //prepare json data
                var message_data = {
                    name: $scope.user.name,
                    temp_visitor_id: $scope.anonymous_user.id,
                    chat_message: $scope.new_message,
                    sort_order: $scope.currant_time(),
                    message_status: 'unread',
                    sender_id: $scope.user.id,
                    class: ''
                };

                $scope.new_message = '';
                $scope.messages.push(message_data);
                var message_index = $scope.messages.indexOf(message_data);

                $scope.scroll_chat();
                $http.post(Setting.site_url + "?d=agents&c=anonymous_chat&m=send", message_data).success(function (response) {
                    if (response.result == 'success') {
                        $scope.messages[message_index] = response.message_row;
                    } else if (response.result == 'failed') {
                        if (response.errors) {
                            $scope.messages.splice(message_index, 1);
                            $scope.displayError(response);
                        }

                        $scope.new_message = message_data.chat_message;
                    }

                    $scope.scroll_chat();
                });
            }
        }

        /*
         * Scroll chat to bottom
         */
        $scope.scroll_chat = function () {
            angular.element("#list-message").mCustomScrollbar('scrollTo', 'bottom', {
                scrollInertia: 100,
                timeout: 10
            });
        }

        /*
         * To display erro notification and message
         * 
         * @param Object data
         */
        $scope.displayError = function (data) {
            $scope.notification.showErrors = true;
            $scope.notification.errors = data.errors;
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