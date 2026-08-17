(function () {
    app.controller("WorkroomController", function ($http, $scope, $interval, $timeout, Setting, $log, $window, operatorPanel, orderByFilter) {
        $scope.operatorPanel = operatorPanel;
        $scope.canned_messages = [];
        $scope.user = cmodule.user;
        $scope.visitor = {name: ''};
        $scope.agents_list = [];
        $scope.departments_list = [];
        $scope.forward = {to: 'agents'};

        $scope.message_stored = [];
        $scope.rendered_list = [];
        $scope.is_typing = false;
        $scope.is_scrollable = true;
        $scope.is_total_scrolled = false;
        $scope.last_message_id = 0;

        $scope.new_msg_indecator = false;
        $scope.visible_chat = true;
        $scope.visible_forward = false;
        $scope.showError = false;
        $scope.errors = '';
        $scope.blink_chatbox = false;
        $scope.messages = [];
        $scope.new_message = '';
        $scope.is_file_sending = false;
        $scope.chatfiles = [];
        $scope.labels = {};

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

        $scope.chatHeartbeatTime = parseInt(Setting.settings.agent_time_interwal) * 1000;
        var stop_heartbeat = undefined;

        $scope.chatboxState = '';
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

            //work your magic
            if ($scope.chatboxState === '') {
                angular.element("#message").focus();
            }

            $timeout(function () {
                $scope.scroll_chat();
            }, 100);
        });

        // set chat history window height.
        $scope.set_height = function (csession) {
            var windowHeight = angular.element($window).outerHeight();
            var headerMargin = parseInt(angular.element('#workroom-header').css('margin-bottom'));
            var headerHeight = (angular.element('#workroom-header').outerHeight()) + headerMargin;
            var typingIndecatorHeight = angular.element('.status-typing-indecator').outerHeight();
            var chatFormHeight = angular.element('#chatForm').outerHeight();
            var bottomMargin = 20;

            angular.element('#workroom-visitor-panel').height(windowHeight);

            if (csession.session_status != 'open' && csession.session_status != 'on-hold' && csession.session_status != 'disconnected') {
                angular.element('.chat-container-frame').height(windowHeight - headerHeight - bottomMargin);
            } else {
                var bottomMargin = 35;
                angular.element('.chat-container-frame').height(windowHeight - (headerHeight + typingIndecatorHeight + chatFormHeight + bottomMargin));
            }
        }

        //get currant time
        $scope.currant_time = function () {
            $scope.miliseconds = new Date().getTime();
            var miliseconds = ($scope.miliseconds + $scope.time_difference).toString();
            $scope.message_stored.push(miliseconds);

            return miliseconds;
        }

        // get chat session
        $scope.$watch("chatSession", function (response, stored) {
            if (response != stored && ($scope.chat_session_id != response.chat_session_id || $scope.visible_area != 'workroom')) {
                $scope.fire_trigger('#formblock', 'click');
                $scope.fire_trigger('#offlineformblock', 'click');

                //hide notification
                $scope.notification.showMessage = false;
                $scope.notification.showErrors = false;
                $scope.is_scrollable = true;
                $scope.is_total_scrolled = false;
                $scope.new_message = '';
                $scope.forward = {to: 'agents'};

                if ($scope.typedMessages[response.chat_session_id]) {
                    $scope.new_message = $scope.typedMessages[response.chat_session_id];
                }
                var past_scrolled = 0;

                angular.element("#message_box").mCustomScrollbar({
                    callbacks: {
                        onScrollStart: function () {
                            $scope.is_scroll_start = true;
                            past_scrolled = this.mcs.topPct;
                        },
                        onScroll: function () {
                            if (past_scrolled > this.mcs.topPct) {
                                $scope.is_scrollable = false;
                            }

                        },
                        onTotalScroll: function () {
                            $scope.is_total_scrolled = true;
                            $scope.is_scrollable = true;
                            $scope.new_msg_indecator = false;
                        }
                    }
                });

                $scope.chat_session_id = response.chat_session_id;
                $scope.messages = response.data.chatHistory;
                $scope.visitor = response.data.visitor;
                
                if (response.data.visitor.meta_data) {
                    $scope.visitor.visited_pages = angular.fromJson(response.data.visitor.meta_data);
                } else {
                    $scope.visitor.visited_pages = [
                        {
                            page_title: $scope.visitor.page_title,
                            page_url: $scope.visitor.page_url
                        }
                    ];
                }

                $scope.chat_session = response.data.chat_session;
                $scope.last_message_id = response.data.last_id;
                $scope.message_stored = response.data.message_stored;
                $scope.labels = response.data.labels;

                $scope.change_visible_area('workroom');
                angular.element("#workroom-visitor-panel").mCustomScrollbar();

                // managing chatbox height
                $timeout(function () {
                    $scope.set_height(response.data.chat_session);
                    $scope.operatorPanel.right_sidebar = true;
                }, 300);

                //reset height on window resize
                angular.element($window).on('resize', function () {
                    $scope.set_height($scope.chat_session);
                });

                if ($scope.visitor.id) {
                    $scope.is_typing = ($scope.visitor.is_typing > 0) ? true : false;
                }

                if (angular.isDefined(stop_heartbeat)) {
                    $interval.cancel(stop_heartbeat);
                    stop_heartbeat = undefined;
                }

                if ($scope.chat_session.session_status == 'open' || $scope.chat_session.session_status == 'on-hold' || $scope.chat_session.session_status == 'disconnected') {
                    $scope.start_chat();
                }
            }
        });

        // start chatting.
        $scope.start_chat = function () {
            $scope.chatHeartbeat();
            stop_heartbeat = $interval(function () {
                $scope.chatHeartbeat();
            }, $scope.chatHeartbeatTime);
        }

        //chatHeartbeat
        $scope.chatHeartbeat = function () {
            var old_chat_session = $scope.chat_session;
            var typing = ($scope.new_message) ? 1 : 0;
            $http.post(site_url + "?d=agents&c=chat&m=chat_heartbeat&csid=" + $scope.chat_session_id + '&last_message_id=' + $scope.last_message_id + '&typing=' + typing).success(function (response) {
                if (response.result == 'success') {
                    $scope.visitor = response.visitor;
                    
                    if (response.visitor.meta_data) {
                        $scope.visitor.visited_pages = angular.fromJson(response.visitor.meta_data);
                    } else {
                        $scope.visitor.visited_pages = [
                            {
                                page_title: $scope.visitor.page_title,
                                page_url: $scope.visitor.page_url
                            }
                        ];
                    }

                    if ($scope.visitor.id) {
                        $scope.is_typing = ($scope.visitor.is_typing > 0) ? true : false;
                    }

                    if (old_chat_session.session_status != 'closed' && response.chat_session.session_status == 'closed') {
                        var closed_message = $scope.visitor.name + $scope.labels.visitor_left_chat;
                        $scope.notifySidebarAlert(closed_message, 'alert-success');
                        $timeout(function () {
                            //$scope.fire_trigger('#tab-chat-history', 'click');
                            if ($scope.recent_chats.length > 0) {
                                var firstUser = $scope.recent_chats[0];
                                $scope.fire_trigger('#chat-session-' + firstUser.id, 'click');
                                $scope.change_visible_area('workroom');
                            } else {
                                $scope.fire_trigger('#link-new-requests', 'click');
                            }
                        }, 3000);
                    }

                    $scope.chat_session = response.chat_session;
                    $scope.last_message_id = response.last_id;

                    angular.forEach(response.chatMessagesData, function (row, key) {
                        if ($.inArray(row.sort_order, $scope.message_stored) == -1 && $.inArray(row.id, $scope.message_stored) == -1) {
                            $scope.messages.push(row);
                            $scope.message_stored.push(row.id);

                            if ($scope.is_scrollable) {
                                $scope.new_msg_indecator = false;
                            } else {
                                $scope.new_msg_indecator = true;
                            }

                            if (row.sender_id === $scope.visitor.id) {
                                $scope.playSound = true;
                                $scope.blink_chatbox = true;
                            }
                        }
                        //$scope.blink_chatbox = false;
                    });

                    if ($scope.playSound) {
                        $scope.play();
                        $scope.playSound = false;
                    }

                    if (response.chatMessagesData.length > 0) {
                        $scope.scroll_chat();
                    }
                } else if (response.result == 'failed') {
                    $scope.displayError(response);
                }
            });
        }

        // send messae to visitor
        $scope.submit_message = function (event) {
            if (event.keyCode == 13 && $scope.new_message) {
                if (!event.shiftKey) {
                    $scope.send_message(event);
                }
            } else if (event.keyCode == 13) {
                event.preventDefault();
            }
        }

        // sending new message to visitor
        $scope.send_message = function (event) {
            event.preventDefault();
            $scope.reset_notifications();

            if ($scope.new_message) {
                if (!$scope.new_msg_indecator && $scope.is_scroll_start) {
                    $scope.is_scrollable = true;
                }

                //prepare json data
                var message_data = {
                    name: $scope.user.name,
                    profile_color: $scope.user.profile_color,
                    profile_pic: $scope.user.profile_pic,
                    profilePic: $scope.user.profile_picture,
                    csid: $scope.chat_session_id,
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
                $http.post(site_url + "?d=agents&c=chat&m=send", message_data).success(function (response) {
                    if (response.result == 'success') {
                        $scope.messages[message_index] = response.message_row;

                        //$scope.messages.push(response.message_row);
                        //$scope.new_message = '';
                        //$scope.scroll_chat();
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
         * Upload file in chat
         * 
         * @param Event - event
         * @param Array - fileObjs
         * @param Array - filelist
         */
        $scope.upload_files = function (event, fileObjs, filelist) {
            $scope.reset_notifications();

            angular.forEach(fileObjs, function (file, order) {
                if (file.filetype && file.filename && file.filesize && file.base64) {
                    file.csid = $scope.chat_session_id;
                    file.sender_id = $scope.user.id;
                    file.message_status = 'unread';
                    file.sort_order = $scope.currant_time();

                    $scope.is_file_sending = true;
                    $http.post(site_url + "?d=agents&c=chat&m=upload_file", file).success(function (response) {
                        if (response.result == 'success') {
                            if (response.message_row) {
                                $scope.messages.push(response.message_row);
                            }
                        } else if (response.result == 'failed') {
                            if (response.errors) {
                                $scope.displayError(response);
                            }
                        }

                        $scope.is_file_sending = false;
                    });
                }
            });
        }

        /*
         * Handle file upload error.
         */
        $scope.file_error_handler = function (event, reader, file, fileList, fileObjs, object) {
            $log.log("An error occurred while reading file: " + file.name);
            var errorData = {error: "An error occurred while reading file: " + file.name};
            $scope.displayError(errorData);
            reader.abort();
        };

        // forward chat
        $scope.display_forward = function (event) {
            event.preventDefault();
            $scope.reset_notifications();

            $http.post(site_url + "?d=agents&c=chat&m=get_forward_data&csid=" + $scope.chat_session_id).success(function (response) {
                if (response.result == 'success') {
                    $scope.visible_forward = true;
                    $scope.visible_chat = false;

                    $scope.agents_list = response.data.agents_list;
                    $scope.departments_list = response.data.departments_list;
                } else if (response.result == 'failed') {
                    $scope.displayError(response);
                }
            });
        }

        // forward chat
        $scope.forward_chat = function (event, forward_data) {
            event.preventDefault();
            $scope.reset_notifications();

            $http.post(site_url + "?d=agents&c=chat&m=forward_chat&csid=" + $scope.chat_session_id, forward_data).success(function (response) {
                if (response.result == 'success') {
                    $scope.chat_session = response.chat_session;

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    if (angular.isDefined(stop_heartbeat)) {
                        $interval.cancel(stop_heartbeat);
                        stop_heartbeat = undefined;
                    }

                    $scope.visible_forward = false;
                    $scope.visible_chat = true;
                    $scope.forward = {to: 'agents'};

                    $timeout(function () {
                        $scope.reset_notifications();
                        angular.element('#close-model').trigger('click');
                    }, 300);
                } else if (response.result == 'failed') {
                    $scope.displayError(response);
                }
            });
        }

        // close chat 
        $scope.end_chat = function (event) {
            event.preventDefault();

            $http.post(site_url + "?d=agents&c=chat&m=end&csid=" + $scope.chat_session_id).success(function (response) {
                if (response.result == 'success') {
                    $scope.chat_session = response.chat_session;
                    $timeout(function () {
                        $scope.fire_trigger('#tab-chat-history', 'click');
                    }, 2000);
                } else if (response.result == 'failed') {
                    $scope.displayError(response);
                }
            });
        }

        $scope.scroll_chat = function () {
            if ($scope.is_scrollable) {
                angular.element("#message_box").mCustomScrollbar('scrollTo', 'bottom', {
                    scrollInertia: 100,
                    timeout: 10
                });
            }
        }

        /*
         * Reset alerts and notofications
         * @returns {undefined}
         */
        $scope.reset_notifications = function () {
            $scope.notification.showMessage = false;
            $scope.notification.message = '';

            $scope.notification.showErrors = false;
            $scope.notification.showErrors = '';
        }

        $scope.displayError = function (data) {
            $scope.notification.showErrors = true;
            if (data.errors) {
                $scope.notification.errors = data.errors;
            } else if (data.error) {
                $scope.notification.errors = data.error;
            }

            $scope.scroll_chat();
            $timeout(function () {
                $scope.reset_notifications();
            }, 1000);
        }
    });

    app.directive('onLastRepeat', function () {
        return function (scope, element, attrs) {
            if (scope.$last) {
                setTimeout(function () {
                    scope.$emit('onRepeatLast', element, attrs);
                }, 1);
            }
        };
    });

    /*
     This directive allows us to pass a function in on an enter key to do what we want.
     */
    app.directive('ngEnter', function () {
        return function (scope, element, attrs) {
            element.bind("keydown keypress", function (event) {
                if (event.which === 13) {
                    scope.$apply(function () {
                        scope.$eval(attrs.ngEnter);
                    });

                    event.preventDefault();
                }
            });
        };
    }).directive('enterSubmit', function () {
        return {
            restrict: 'A',
            link: function (scope, elem, attrs) {

                elem.bind('keydown', function (event) {
                    var code = event.keyCode || event.which;

                    if (code === 13) {
                        if (!event.shiftKey) {
                            event.preventDefault();
                            scope.$apply(attrs.enterSubmit);
                        }
                    }
                });
            }
        }
    });
})();