(function () {
    'use strict';
    angular.module('cmodule.filters', []).
            filter('oneCapLetter', function () {
                return function (input) {
                    if (!input) {
                        return;
                    }

                    return input.substring(0, 1).toLowerCase().replace(/\b[a-z]/g, function (letter) {
                        return letter.toUpperCase();
                    });
                };
            })
            .filter('newlines', function () {
                var ishtml = function (str) {
                    var a = document.createElement('div');
                    a.innerHTML = str;
                    for (var c = a.childNodes, i = c.length; i--; ) {
                        if (c[i].nodeType == 1)
                            return true;
                    }
                    return false;
                }

                return function (text) {
                    if (text) {
                        if (ishtml(text)) {
                            return text.replace(/\n/g, ' <br/> ')
                                    .replace(/([A-Za-z0-9._%+-]+@+[A-Za-z0-9._%+-]+\.[^\s]+)/g, '<a target="_blank" href="mailto:$1">$1</a>');
                        }
                        return text.replace(/\n/g, ' <br/> ')
                                .replace(/((ftp|http)[^\s]+)/g, '<a target="_blank" href="$1">$1</a>')
                                //.replace(/(www\.[^\s]+)/g, '<a target="_blank" href="http://$1">$1</a>')
                                .replace(/([A-Za-z0-9._%+-]+@+[A-Za-z0-9._%+-]+\.[^\s]+)/g, '<a target="_blank" href="mailto:$1">$1</a>');
                    }

                    return text;
                }
            });

    angular.module("cmodule", ['ngSanitize', 'ngAnimate', 'angularRangeSlider', 'cmodule.filters', 'ui.bootstrap', 'angular-smilies', 'naif.base64'], ['$httpProvider', function ($httpProvider) {
            // Use x-www-form-urlencoded Content-Type
            $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';

            /**
             * The workhorse; converts an object to x-www-form-urlencoded serialization.
             * @param {Object} obj
             * @return {String}
             */
            var param = function (obj) {
                var query = '', name, value, fullSubName, subName, subValue, innerObj, i;

                for (name in obj) {
                    value = obj[name];

                    if (value instanceof Array) {
                        for (i = 0; i < value.length; ++i) {
                            subValue = value[i];
                            fullSubName = name + '[' + i + ']';
                            innerObj = {};
                            innerObj[fullSubName] = subValue;
                            query += param(innerObj) + '&';
                        }
                    } else if (value instanceof Object) {
                        for (subName in value) {
                            subValue = value[subName];
                            fullSubName = name + '[' + subName + ']';
                            innerObj = {};
                            innerObj[fullSubName] = subValue;
                            query += param(innerObj) + '&';
                        }
                    } else if (value !== undefined && value !== null)
                        query += encodeURIComponent(name) + '=' + encodeURIComponent(value) + '&';
                }

                return query.length ? query.substr(0, query.length - 1) : query;
            };

            // Override $http service's default transformRequest
            $httpProvider.defaults.transformRequest = [function (data) {
                    return angular.isObject(data) && String(data) !== '[object File]' ? param(data) : data;
                }];
        }]).controller("BodyController", ['$http', '$scope', '$interval', '$timeout', '$location', '$document', '$window', '$log', 'orderByFilter', function ($http, $scope, $interval, $timeout, $location, $document, $window, $log, orderByFilter) {
            $scope.message_stored = [];
            $scope.rendered_list = [];
            $scope.settings = {'theme': ''};
            $scope.tags = [];
            $scope.visitor = {};
            $scope.cbwindow = angular.fromJson($window.cbwindow);

            if ($window.siteuser !== undefined && $window.siteuser) {
                $scope.visitor = angular.copy(angular.fromJson($window.siteuser));
            }

            $scope.agent = {name: ''};
            $scope.chat_session = {};
            $scope.messages = [];

            $scope.anonymous_box_id = "#anonymous_message_box";
            $scope.anonymous_visitor = {};
            $scope.anonymous_user = {};
            $scope.anonymous_messages = [];

            $scope.windowTitle = $document[0].title;
            $scope.chatboxTitle = '';
            $scope.chatboxState = '';
            $http.defaults.headers.common['Accesstoken'] = access_token;

            // to handle image backgrouund color
            $scope.colors = ['#f16364', '#f58559', '#f9a43e', '#e4c62e', '#67bf74', '#59a2be', '#2093cd', '#ad62a7', '#805781', '#e57373', '#f06292', '#a1887f'
                        , '#ba68c8', '#9575cd', '#7986cb', '#64b5f6', '#4fc3f7', '#4dd0e1', '#4db6ac', '#81c784', '#aed581', '#ff8a65', '#d4e157', '#ffd54f', '#ffb74d', '#90a4ae'];

            $scope.rand_color = '';
            $scope.emailFormat = /^[a-z]+[a-z0-9._-]+@[a-z0-9.-]+\.[a-z.]{2,5}$/;

            // to handle page parent detail
            $scope.current_page = {'page_url': purl, 'page_title': ptitle};

            $scope.user_agent = 'browser';
            $scope.custom_styles = '';
            $scope.is_agents_online = false;
            $scope.online_agent = null;
            $scope.locked_agent = null;

            $scope.miliseconds = new Date().getTime();
            $scope.form_title = '';
            $scope.time_difference = 0;

            $scope.message_box_id = "#message_box";
            $scope.heartbeat_status = ['requested', 'forward', 'open', 'on-hold', 'disconnected'];
            $scope.chatHeartbeatTime = 3000;
            $scope.time_interval = 30000;
            var stop_heartbeat = undefined;
            var stop_users_request = undefined;
            var past_scrolled = 0;

            $scope.visible_widget = 'start';
            $scope.is_scroll_start = false;
            $scope.is_scrollable = true;
            $scope.is_typing = false;
            $scope.is_waiting = false;
            $scope.minimized = false;
            $scope.offline_request_sent = false;
            $scope.ask_for_transcript = 'no';
            $scope.ask_to_visitor_email = 'no';
            $scope.visitor_email = '';
            $scope.ask_to_confirm = 'no';
            $scope.confirm_close_session = 'no';
            $scope.last_id = 0;
            $scope.display_loader = true;
            $scope.new_msg_indecator = false;
            $scope.blink_chatbox = false;
            $scope.new_message = '';
            $scope.is_file_sending = false;
            $scope.chatfiles = [];

            // to handle notification
            $scope.showError = false;
            $scope.errors = '';
            $scope.showMessage = false;
            $scope.success_message = '';

            // to handle feedback
            $scope.feedback_sent = false;
            $scope.feedback = {'rating': 4};
            $scope.rating_status = {1: 'Poor', 2: 'Fair', 3: 'Good', 4: 'Very good', 5: 'Excellent'};

            //Play Standart
            $scope.play = function () {
                var audio = document.getElementById("audio1");
                audio.play();
            }

            // watch the chatbox state
            $scope.$watch("chatboxState", function (newState, oldState) {
                if (newState === 'focus') {
                    $document[0].title = $scope.windowTitle;
                    angular.element('#chat-cmodule-header').removeClass('blinking');
                    $scope.blink_chatbox = false;
                }
            });

            /*
             * This function will return a random color
             * @returns {undefined}
             */
            $scope.getColor = function () {
                var rand_color = $scope.colors[Math.floor((Math.random() * 26) + 1)];
                return rand_color;
            }
            $scope.profile_color = $scope.getColor();
            
            /*
             * disable_click
             * 
             * Disabled prevet event 
             * 
             * @type    function
             * @date    17-01-2018
             * @since   5.1.1
             * 
             * @param   Event   event
             * @return  n/a
             */
            $scope.disable_click = function(event){
                event.preventDefault();
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

                            var $prev_row_ele = angular.element('#chat-cmodule-chat-row-' + (key - 1))
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
                            angular.element('#chat-cmodule-chat-row-' + key + ' .chat-row-avatar').remove();
                        }
                        angular.element('#chat-cmodule-chat-row-' + key).addClass(row_class);
                    }
                });
            }

            $scope.$on('onRepeatLast', function (scope, element, attrs) {
                if ($scope.settings.theme == 'bubbles_boom') {
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

            /*
             * To reset chat box 
             * @returns {undefined}
             */
            $scope.reset = function () {
                $scope.message_stored = [];
                $scope.visitor = {};
                $scope.agent = {};
                $scope.chat_session = {};
                $scope.messages = [];

                $scope.anonymous_visitor = {};
                $scope.anonymous_user = {};
                $scope.anonymous_messages = [];

                $scope.new_msg_indecator = false;
                $scope.blink_chatbox = false;
                $scope.new_message = '';
                $scope.visible_widget = 'start';
                $scope.is_scroll_start = false;
                $scope.is_scrollable = true;
                $scope.is_typing = false;
                $scope.is_waiting = false;
                $scope.minimized = false;
                $scope.offline_request_sent = false;
                $scope.ask_for_transcript = 'no';
                $scope.ask_to_visitor_email = 'no';
                $scope.visitor_email = '';
                $scope.ask_to_confirm = 'no';
                $scope.confirm_close_session = 'no';
                $scope.last_id = 0;
                $scope.display_loader = false;

                // to handle notification
                $scope.showError = false;
                $scope.errors = '';
                $scope.showMessage = false;
                $scope.success_message = '';

                if ($window.siteuser !== undefined && $window.siteuser) {
                    $scope.visitor = angular.copy(angular.fromJson($window.siteuser));
                }

                // to handle feedback
                $scope.feedback_sent = false;
                $scope.feedback = {'rating': 4};
                $scope.form_title = $scope.settings.chat_start_title;
                $scope.current_page.siteuser = $scope.visitor;

                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=get_session", $scope.current_page).success(function (response) {
                    $scope.settings = response.settings;
                    $scope.tags = response.tags;
                    $scope.chatHeartbeatTime = parseInt($scope.settings.time_interwal) * 1000;

                    // update locked agent info into form
                    if ($scope.locked_agent) {
                        $scope.form_title = $scope.locked_agent.display_name;
                        $scope.profile_color = $scope.locked_agent.profile_color;
                        if ($scope.locked_agent.profile_picture) {
                            $scope.settings.default_avatar = $scope.locked_agent.profile_picture;
                        }
                    } else {
                        $scope.form_title = $scope.settings.chat_start_title;
                    }

                    // updating theme.
                    $scope.updateTheme();
                    $scope.setChatStatus(response);

                    if ($scope.settings.chat_mode == 'online' && angular.isDefined(stop_users_request) === false && angular.isDefined(stop_heartbeat) === false) {
                        stop_users_request = $interval(function () {
                            $scope.get_online_agents();
                        }, $scope.time_interval);
                    }
                });

                // to handle form validation reset
                $scope.offlineForm.$setPristine();
                $scope.onlineForm.$setPristine();
                $scope.feedbackForm.$setPristine();
                $scope.anonymousForm.$setPristine();
            }

            // get server currant time in milliseconds
            $scope.get_server_time = function () {
                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=get_server_time").success(function (response) {
                    if (response.result == 'success') {
                        $scope.miliseconds = new Date().getTime();
                        $scope.time_difference = parseInt(response.milliseconds) - $scope.miliseconds;
                    }
                });
            }

            //get currant time
            $scope.currant_time = function () {
                $scope.miliseconds = new Date().getTime();
                var miliseconds = ($scope.miliseconds + $scope.time_difference).toString();
                $scope.message_stored.push(miliseconds);

                return miliseconds;
            }

            /*
             * To update theme according to settings
             * @returns {undefined}
             */
            $scope.updateTheme = function () {
                $scope.custom_styles = ".chat-cmodule-header, .chatnox-btn-default, .chat-cmodule-header *, .chat-cmodule-header, .chat-cmodule-widget-head, .cmodule-window-widget-title, .chat-cmodule .cmodule-chat-icon, .chat-cmodule .widget-bar.cmodule-agent-avatar .cmodule-window-title { color: " + $scope.settings.title_color + " !important; }";
                $scope.custom_styles += ".chat-cmodule-header, .chatnox-btn-default, .chat-cmodule-header, .chat-cmodule-widget-head, .chat-cmodule .cmodule-chat-btn { background-color: " + $scope.settings.background_color + " !important; }";

                $scope.custom_styles += ".theme-bubbles_boom .chat-cmodule-message-reply-row .chat-cmodule-message * { color: " + $scope.settings.title_color + " !important; }";
                $scope.custom_styles += ".theme-bubbles_boom .chat-cmodule-message-reply-row .chat-cmodule-message { background-color: " + $scope.settings.background_color + " !important; }";
            }

            /*
             * Check Operator ping for initiate chat request
             * 
             * @param Object
             * @returns {undefined}
             */
            $scope.check_operator_ping = function (chatdata) {
                $scope.anonymous_visitor = chatdata.anonymous_visitor;
                $scope.form_title = $scope.anonymous_visitor.operator_name;
                $scope.profile_color = $scope.anonymous_visitor.operator_profile_color;

                if ($scope.anonymous_visitor.operator_profile_pic) {
                    $scope.settings.default_avatar = $scope.anonymous_visitor.operator_profile_picture;
                } else {
                    $scope.settings.default_avatar = '';
                }

                angular.element($scope.anonymous_box_id).mCustomScrollbar({
                    /* keyboard default options */
                    keyboard: {
                        enable: true,
                        scrollType: "stepless",
                        scrollAmount: "auto"
                    },
                    callbacks: {
                        onInit: function () {

                        },
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

                if (!angular.equals($scope.anonymous_messages, chatdata.anonymous_messages)) {
                    $scope.anonymous_messages = chatdata.anonymous_messages;
                    angular.element($scope.anonymous_box_id).mCustomScrollbar('scrollTo', 'bottom', {
                        scrollInertia: 100,
                        timeout: 10
                    });
                }

                if ($scope.visible_widget != 'initiate-form') {
                    $scope.visible_widget = 'operator-request';
                }

                $timeout(function () {
                    angular.element($scope.anonymous_box_id).mCustomScrollbar('scrollTo', 'bottom', {
                        scrollInertia: 100,
                        timeout: 10
                    });
                }, 1000);
            }

            /*
             * Start operator initiated chat reuest
             * 
             * @returns {undefined}
             */
            $scope.connect_requested_operator = function () {
                $scope.display_loader = true;

                $scope.anonymous_user.sort_order = $scope.currant_time();
                $scope.anonymous_user.page_url = $scope.current_page.page_url;
                $scope.anonymous_user.page_title = $scope.current_page.page_title;

                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=connect_operator", $scope.anonymous_user).success(function (response) {
                    if (response.result == 'success') {
                        $scope.display_loader = false;
                        $scope.is_scrollable = true;

                        $scope.anonymous_visitor = {};
                        $scope.anonymous_user = {};
                        $scope.anonymous_messages = [];

                        $scope.visitor = response.data.visitor;
                        $scope.agent = response.data.agent;
                        $scope.chat_session = response.data.chat_session;
                        $scope.messages = response.data.chatHistory;
                        $scope.last_id = response.data.last_id;
                        $scope.message_stored = response.data.message_stored;

                        $scope.visible_widget = 'chatting-widget';

                        $scope.start_chat();
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            /*
             * Submit initiated chat form
             * 
             * @returns {undefined}
             */
            $scope.submit_initiate_form = function (event) {
                $scope.connect_requested_operator();
                event.preventDefault();
            }

            /*
             * Start initiated chat
             *  
             * @param Event event
             * @returns {undefined}
             */
            $scope.start_initiated_chat = function (event) {
                if ($scope.settings.initiate_bypass_chat == 'yes') {
                    var $visitor_id = $scope.miliseconds;
                    $scope.anonymous_user = angular.copy($scope.anonymous_visitor);

                    if ($scope.anonymous_user.email == '') {
                        $scope.anonymous_user.email = $visitor_id + '@' + $scope.randomString(8, 'alpha') + '.com';
                    }

                    $scope.connect_requested_operator();
                } else {
                    $scope.anonymous_user = angular.copy($scope.anonymous_visitor);
                    $scope.anonymous_user.initiate_bypass_chat = 'no';

                    $scope.visible_widget = 'initiate-form';
                }

                event.preventDefault();
            }

            /*
             * To set chat panel status onlinr 
             * @returns {undefined}
             */
            $scope.setChatStatus = function (response) {
                if ($scope.settings.chat_mode == 'online') {
                    if (response.is_agents_online) {
                        if ($scope.settings.enable_specific_agent_request == 'yes' && response.agents_list && response.agents_list.length > 0) {
                            $scope.online_agent = response.agents_list[Math.floor(Math.random() * response.agents_list.length)];
                        }

                        $scope.is_agents_online = true;

                        if ($scope.visible_widget == 'offline-widget') {
                            $scope.form_title = $scope.settings.online_form_title;
                            $scope.visible_widget = 'online-widget';
                        } else if ($scope.visible_widget == 'start' && $scope.form_title == $scope.settings.offline_form_title) {
                            $scope.form_title = $scope.settings.online_form_title;
                        }

                        if ($scope.settings.enable_agent_initiate_chats == 'yes' && response.data && response.data.anonymous_visitor && response.data.anonymous_visitor.operator_id != 0) {
                            $scope.check_operator_ping(response.data);
                        }

                    } else {
                        $scope.online_agent = null;
                        $scope.locked_agent = null;
                        $scope.is_agents_online = false;

                        if ($scope.visible_widget == 'online-widget') {
                            $scope.visible_widget = 'offline-widget';
                            $scope.form_title = $scope.settings.offline_form_title;
                        } else if ($scope.visible_widget == 'start') {
                            $scope.form_title = $scope.settings.offline_form_title;
                        }
                    }
                } else {
                    $scope.form_title = $scope.settings.offline_form_title;

                    if (angular.isDefined(stop_users_request)) {
                        $interval.cancel(stop_users_request);
                        stop_users_request = undefined;
                    }
                }
            }


            // get online agents
            $scope.get_online_agents = function () {
                // get online users
                $http.get(cmodule_site_url + "?d=visitors&c=chat&m=get_online_agents").success(function (response) {
                    if (response.result == 'success') {
                        $scope.setChatStatus(response);
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            /*
             * To generate random string
             * @param Intger length
             * @param String type
             * @returns String result
             */
            $scope.randomString = function (length, type) {
                var chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
                if (type === 'num') {
                    chars = '0123456789';
                } else if (type === 'alpha') {
                    chars = 'abcdefghijklmnopqrstuvwxyz';
                } else if (type === 'alphac') {
                    chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
                } else if (type === 'alphacm') {
                    chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
                }

                var result = '';
                for (var i = length; i > 0; --i)
                    result += chars[Math.floor(Math.random() * chars.length)];
                return result;
            }

            /*
             * Initiate by pass chat
             * @returns {undefined}
             */
            $scope.initiate_bypass_chat = function () {
                $scope.visitor.initiate_bypass_chat = 'yes';
                var $cbuser_info = false;
                var $visitor_id = $scope.miliseconds;
                var $robotobj = {
                    name: 'Visitor ' + $visitor_id,
                    email: $visitor_id + '@' + $scope.randomString(8, 'alpha') + '.com',
                    message: 'Hi'
                };

                if ($scope.visitor.name) {
                    $cbuser_info = true;
                } else {
                    $scope.visitor.name = $robotobj.name;
                }

                if ($scope.visitor.email) {
                    $cbuser_info = true;
                } else {
                    $scope.visitor.email = $robotobj.email;
                }

                if ($scope.visitor.message) {
                    $cbuser_info = true;
                } else {
                    $scope.visitor.message = $robotobj.message;
                }

                if ($cbuser_info) {
                    $scope.visitor.initiate_bypass_chat = 'no';
                }

                $scope.visitor.sort_order = $scope.currant_time();
                $scope.visitor.page_url = $scope.current_page.page_url;
                $scope.visitor.page_title = $scope.current_page.page_title;

                if ($scope.locked_agent) {
                    $scope.visitor.agent_id = $scope.locked_agent.id;
                }

                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=request", $scope.visitor).success(function (response) {
                    if (response.result == 'success') {
                        $scope.visitor = response.data.visitor;
                        $scope.agent = response.data.agent;
                        $scope.chat_session = response.data.chat_session;
                        $scope.messages = response.data.chatHistory;
                        $scope.last_id = response.data.last_id;
                        $scope.message_stored = response.data.message_stored;

                        $scope.visible_widget = 'chatting-widget';
                        $scope.is_waiting = true;
                        $scope.form_title = $scope.settings.chat_waiting_title;

                        $scope.start_chat();
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            /*
             * To show chatbox form
             * @returns {Boolean}
             */
            $scope.visible_form = function () {
                $scope.showError = false;
                $scope.showMessage = false;

                if ($scope.is_agents_online && $scope.settings.chat_mode == 'online') {
                    if ($scope.online_agent && $scope.settings.enable_specific_agent_request == 'yes') {
                        $scope.locked_agent = angular.copy($scope.online_agent);
                    }

                    if ($scope.settings.initiate_bypass_chat === 'yes') {
                        $scope.visible_widget = 'initaiting-bypasschat-widget';
                        $scope.form_title = $scope.settings.chat_waiting_title;

                        $scope.initiate_bypass_chat();
                    } else {
                        $scope.visible_widget = 'online-widget';
                        $scope.form_title = $scope.settings.online_form_title;
                    }

                    // update locked agent info into form
                    if ($scope.locked_agent) {
                        $scope.form_title = $scope.locked_agent.display_name;
                        $scope.profile_color = $scope.locked_agent.profile_color;

                        if ($scope.locked_agent.profile_picture) {
                            $scope.settings.default_avatar = $scope.locked_agent.profile_picture;
                        }
                    }
                } else {
                    $scope.visible_widget = 'offline-widget';
                    $scope.form_title = $scope.settings.offline_form_title;
                }
            }

            // check if session exists.
            $scope.get_session = function () {
                $scope.current_page.siteuser = $scope.visitor;

                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=get_session", $scope.current_page).success(function (response) {
                    $scope.tags = response.tags;
                    $scope.settings = response.settings;
                    $scope.rating_status = {
                        1: $scope.settings.label_feedback.rating_one,
                        2: $scope.settings.label_feedback.rating_two,
                        3: $scope.settings.label_feedback.rating_three,
                        4: $scope.settings.label_feedback.rating_four,
                        5: $scope.settings.label_feedback.rating_five
                    };

                    $scope.form_title = $scope.settings.chat_start_title;
                    $scope.chatHeartbeatTime = parseInt($scope.settings.time_interwal) * 1000;
                    $scope.user_agent = response.user_agent;
                    $scope.minimized = response.data.minimized;

                    // updating theme.
                    $scope.updateTheme();
                    $scope.setChatStatus(response);

                    if ($scope.settings.theme == 'classic') {
                        $scope.message_box_id = '#classic_message_box';
                        $scope.anonymous_box_id = "#anonymous_classic_message_box";
                    }

                    if (response.result == 'success') {
                        $scope.visitor = response.data.visitor;
                        $scope.agent = response.data.agent;
                        $scope.chat_session = response.data.chat_session;
                        $scope.messages = response.data.chatHistory;
                        $scope.last_id = response.data.last_id;
                        $scope.message_stored = response.data.message_stored;

                        $timeout(function () {
                            $scope.scroll_chat();
                        }, 100);

                        if (response.data.show_feedback_form == 'yes' && $scope.chat_session.session_status == 'closed') {
                            $scope.visible_widget = 'feedback-widget';
                            $scope.form_title = $scope.settings.label_give_feedback;
                        } else if ($scope.chat_session) {
                            $scope.visible_widget = 'chatting-widget';
                            $scope.form_title = $scope.settings.chat_waiting_title;

                            if (response.data.agent.id) {
                                $scope.form_title = $scope.agent.display_name;

                                if ($scope.agent.profile_pic) {
                                    $scope.settings.default_avatar = $scope.agent.profilePic;
                                } else {
                                    $scope.settings.default_avatar = '';
                                }

                                $scope.is_typing = ($scope.agent.is_typing > 0) ? true : false;

                                if ($scope.chat_session.session_status == 'closed') {
                                    $scope.showError = true;
                                    $scope.errors = $scope.settings.label_session_closed;
                                    $scope.new_message = $scope.errors;

                                    $timeout(function () {
                                        $scope.showError = false;
                                        $scope.errors = '';
                                    }, 2500);
                                }
                            } else {
                                $scope.is_waiting = true;
                            }
                        }

                        //start chat
                        $scope.start_chat();
                    } else if (response.result == 'no-session') {
                        if ($scope.user_agent !== 'mobile' && $scope.settings.initiate_bypass_chat === 'no' && $scope.settings.open_chatbox_automatically === 'yes') {
                            $timeout(function () {
                                if ($scope.visible_widget === 'start') {
                                    if ($scope.is_agents_online && $scope.settings.chat_mode == 'online') {
                                        $scope.visible_widget = 'online-widget';
                                        $scope.form_title = $scope.settings.online_form_title;

                                        // update locked agent info into form
                                        if ($scope.online_agent && $scope.settings.enable_specific_agent_request == 'yes') {
                                            $scope.locked_agent = angular.copy($scope.online_agent);
                                            $scope.form_title = $scope.locked_agent.display_name;
                                            $scope.profile_color = $scope.locked_agent.profile_color;

                                            if ($scope.locked_agent.profile_picture) {
                                                $scope.settings.default_avatar = $scope.locked_agent.profile_picture;
                                            }
                                        }
                                    } else {
                                        $scope.visible_widget = 'offline-widget';
                                        $scope.form_title = $scope.settings.offline_form_title;
                                    }
                                }
                            }, (parseInt($scope.settings.time_automatically_open_chatbox) * 1000));
                        }

                        if ($scope.settings.chat_mode == 'online') {
                            stop_users_request = $interval(function () {
                                $scope.get_online_agents();
                            }, $scope.time_interval);
                        }
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }

                    angular.element($scope.message_box_id).mCustomScrollbar({
                        /* keyboard default options */
                        keyboard: {
                            enable: true,
                            scrollType: "stepless",
                            scrollAmount: "auto"
                        },
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

                    $scope.display_loader = false;
                });
            }

            /*
             * This function will show chatbox
             */
            $scope.show_chatbox = function () {
                $scope.get_server_time();
                $scope.get_session();
            }

            //send request
            $scope.send_request = function (event) {
                event.preventDefault();
                $scope.display_loader = true;

                $scope.visitor.sort_order = $scope.currant_time();
                $scope.visitor.page_url = $scope.current_page.page_url;
                $scope.visitor.page_title = $scope.current_page.page_title;

                if ($scope.locked_agent) {
                    $scope.visitor.agent_id = $scope.locked_agent.id;
                }

                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=request", $scope.visitor).success(function (response) {
                    if (response.result == 'success') {
                        $scope.display_loader = false;
                        $scope.visitor = response.data.visitor;
                        $scope.agent = response.data.agent;
                        $scope.chat_session = response.data.chat_session;
                        $scope.messages = response.data.chatHistory;
                        $scope.last_id = response.data.last_id;
                        $scope.message_stored = response.data.message_stored;

                        $scope.visible_widget = 'chatting-widget';
                        $scope.is_waiting = true;
                        $scope.form_title = $scope.settings.chat_waiting_title;

                        $scope.start_chat();
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            // send offline request
            $scope.send_offline_request = function (event) {
                event.preventDefault();
                $scope.display_loader = true;

                $scope.visitor.page_url = $scope.current_page.page_url;
                $scope.visitor.page_title = $scope.current_page.page_title;
                $http.post(cmodule_site_url + "?d=visitors&c=orequests&m=request", $scope.visitor).success(function (response) {
                    if (response.result == 'success') {
                        $scope.showMessage = true;
                        $scope.success_message = $scope.settings.offline_submission_message;

                        $timeout(function () {
                            $scope.reset();
                        }, 2500);
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            // start chatting.
            $scope.start_chat = function () {
                if (angular.isDefined(stop_users_request)) {
                    $interval.cancel(stop_users_request);
                    stop_users_request = undefined;
                }

                if (angular.isDefined(stop_heartbeat)) {
                    $interval.cancel(stop_heartbeat);
                    stop_heartbeat = undefined;
                }

                if ($scope.user_agent !== 'mobile' && $scope.chat_session && angular.element.inArray($scope.chat_session.session_status, $scope.heartbeat_status) != -1) {
                    stop_heartbeat = $interval(function () {
                        $scope.chatHeartbeat();
                    }, $scope.chatHeartbeatTime);
                } else if ($scope.settings.chat_mode == 'online') {
                    stop_heartbeat = $interval(function () {
                        $scope.get_online_agents();
                    }, $scope.time_interval);
                }
            }

            //chatHeartbeat
            $scope.chatHeartbeat = function () {
                var typing = ($scope.new_message) ? 1 : 0;
                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=chatHeartbeat&last_id=" + $scope.last_id + '&typing=' + typing, $scope.visitor).success(function (response) {
                    if (response.result == 'success') {
                        $scope.setChatStatus(response);
                        $scope.chat_session = response.chat_session;
                        $scope.agent = response.agent;
                        $scope.last_id = response.last_id;
                        $scope.form_title = $scope.settings.chat_waiting_title;

                        if (response.agent.id) {
                            $scope.locked_agent = null;
                            $scope.chatboxTitle = response.agent.display_name + ' says...';
                            $scope.form_title = $scope.agent.display_name;
                            $scope.profile_color = $scope.agent.profile_color;

                            if ($scope.agent.profile_pic) {
                                $scope.settings.default_avatar = $scope.agent.profilePic;
                            } else {
                                $scope.settings.default_avatar = '';
                            }

                            $scope.is_typing = ($scope.agent.is_typing > 0) ? true : false;
                            $scope.is_waiting = false;

                            if ($scope.chat_session.session_status == 'closed') {
                                $scope.showError = true;
                                $scope.errors = $scope.settings.label_session_closed;
                                $scope.new_message = $scope.settings.label_session_closed;

                                $timeout(function () {
                                    $scope.showError = false;
                                    $scope.errors = '';

                                    if (angular.isDefined(stop_heartbeat)) {
                                        $interval.cancel(stop_heartbeat);
                                        stop_heartbeat = undefined;

                                        if ($scope.settings.chat_mode == 'online') {
                                            stop_users_request = $interval(function () {
                                                $scope.get_online_agents();
                                            }, $scope.time_interval);
                                        }
                                    }
                                }, 2500);
                            }
                        }

                        angular.forEach(response.chatMessagesData, function (row, key) {
                            if (angular.element.inArray(row.sort_order, $scope.message_stored) == -1 && angular.element.inArray(row.id, $scope.message_stored) == -1) {
                                $scope.messages.push(row);
                                $scope.message_stored.push(row.id);

                                if ($scope.is_scrollable) {
                                    $scope.new_msg_indecator = false;
                                } else {
                                    $scope.new_msg_indecator = true;
                                }

                                if (row.sender_id !== $scope.visitor.id) {
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

                        /*if ($scope.blink_chatbox) {
                         $document[0].title = $scope.chatboxTitle;
                         angular.element('#chat-cmodule-header').toggleClass('blinking');
                         }*/

                        if (response.chatMessagesData.length > 0) {
                            $scope.scroll_chat();
                        }
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            // submit message on enter key press
            $scope.submit_message = function (event) {
                if (event.keyCode == 13 && $scope.new_message) {
                    if (!event.shiftKey) {
                        $scope.send_message(event);
                    }
                } else if (event.keyCode == 13) {
                    event.preventDefault();
                }
            }

            // sending new message 
            $scope.send_message = function (event) {
                event.preventDefault();
                if ($scope.new_message) {
                    if (!$scope.new_msg_indecator && $scope.is_scroll_start) {
                        $scope.is_scrollable = true;
                    }

                    //prepare json data
                    var message_data = {
                        name: $scope.visitor.name,
                        chat_session_id: $scope.chat_session.id,
                        chat_message: $scope.new_message,
                        message_status: 'unread',
                        sender_id: $scope.visitor.id,
                        sort_order: $scope.currant_time(),
                        class: ''
                    };

                    $scope.messages.push(message_data);
                    $scope.new_message = '';
                    $scope.scroll_chat();
                    // sending request to send message
                    $http.post(cmodule_site_url + "?d=visitors&c=chat&m=send", message_data).success(function (response) {
                        if (response.result == 'success') {
                            //$scope.messages.push(response.message_row);
                            //$scope.new_message = '';

                            //$scope.scroll_chat();
                        } else if (response.result == 'failed') {
                            if (response.error) {
                                $scope.displayError(response);
                            }
                            $scope.new_message = message_data.chat_message;
                        }
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
                angular.forEach(fileObjs, function (file, order) {
                    if (file.filetype && file.filename && file.filesize && file.base64) {
                        file.chat_session_id = $scope.chat_session.id;
                        file.sender_id = $scope.visitor.id;
                        file.message_status = 'unread';
                        file.sort_order = $scope.currant_time();

                        $log.log(file);

                        $scope.is_file_sending = true;
                        $http.post(cmodule_site_url + "?d=visitors&c=chat&m=upload_file", file).success(function (response) {
                            if (response.result == 'success') {
                                if (response.message_data) {
                                    $scope.messages.push(response.message_data);
                                }
                            } else if (response.result == 'failed') {
                                if (response.error) {
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
                //alert("An error occurred while reading file: " + file.name);

                var errorData = {error: "An error occurred while reading file: " + file.name};
                $scope.displayError(errorData);
                reader.abort();
            };

            //minimize_chat        
            $scope.minimize_chat = function (event) {
                event.preventDefault();
                $scope.minimized = 'yes';
                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=minimize").success(function (response) {
                    if (response.result == 'success') {
                        $scope.minimized = response.minimized;
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                        $scope.minimized = 'no';
                    }
                });
            }

            //minimize_chat        
            $scope.maximize_chat = function (event) {
                event.preventDefault();
                $scope.minimized = 'no';
                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=maximize").success(function (response) {
                    if (response.result == 'success') {
                        $scope.minimized = response.minimized;
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                        $scope.minimized = 'yes';
                    }
                });
            }

            /*
             * Check is chatbox closable
             * 
             * @return Boolean
             */
            $scope.is_closable = function () {
                if ($scope.visible_widget == 'operator-request' || $scope.visible_widget == 'operator-request' || $scope.visible_widget == 'initaiting-bypasschat-widget') {
                    return false;
                }

                return true;
            }

            // close chat 
            $scope.end_chat = function (event) {
                event.preventDefault();

                if ($scope.visible_widget == 'chatting-widget') {
                    if ($scope.confirm_close_session == 'yes') {
                        if ($scope.settings.send_chat_transcript_to_visitor != 'ask_to_visiter') {
                            $scope.display_loader = true;

                            var data = {
                                send_chat_transcript: $scope.settings.send_chat_transcript_to_visitor,
                                visitor_email: $scope.visitor_email
                            };
                            
                            $http.post(cmodule_site_url + "?d=visitors&c=chat&m=end", data).success(function (response) {
                                if (response.result == 'success') {
                                    $scope.ask_for_transcript = 'no';
                                    $scope.chat_session = response.chat_session;

                                    if (response.show_feedback_form == 'yes') {
                                        $scope.visible_widget = 'feedback-widget';
                                        $scope.form_title = $scope.settings.label_give_feedback;
                                    } else {
                                        $scope.reset();
                                    }

                                    if (angular.isDefined(stop_heartbeat)) {
                                        $interval.cancel(stop_heartbeat);
                                        stop_heartbeat = undefined;

                                        if ($scope.settings.chat_mode == 'online') {
                                            stop_users_request = $interval(function () {
                                                $scope.get_online_agents();
                                            }, $scope.time_interval);
                                        }
                                    }
                                } else if (response.result == 'failed') {
                                    $scope.displayError(response);
                                }

                                $scope.display_loader = false;
                            });
                        } else {
                            if ($scope.minimized) {
                                $scope.maximize_chat(event);
                            }
                            $scope.ask_for_transcript = 'yes';
                        }
                    } else {
                        if ($scope.minimized) {
                            $scope.maximize_chat(event);
                        }
                        $scope.ask_to_confirm = 'yes';
                    }
                } else {
                    $scope.reset();
                }
            }

            // send feedback
            $scope.send_feedback = function (event) {
                event.preventDefault();
                $scope.display_loader = true;

                $scope.feedback.chat_session_id = $scope.chat_session.id;
                $scope.feedback.feedback_by = $scope.visitor.id;
                $scope.feedback.feedback_to = $scope.agent.id;
                $scope.feedback.sort_order = $scope.currant_time();

                $http.post(cmodule_site_url + "?d=visitors&c=chat&m=send_feedback", $scope.feedback).success(function (response) {
                    if (response.result == 'success') {
                        $scope.showMessage = true;
                        $scope.success_message = $scope.settings.feedback_submission_message;

                        $timeout(function () {
                            $scope.reset();
                        }, 2500);
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }
            
            /*
             * close_session
             * 
             * Function will end currunt runing chat session. 
             * 
             * @type    function
             * @date    17-01-2018
             * @since   5.1.1
             * 
             * @param   Event   event
             * @return  n/a
             */
            $scope.close_session = function(event){
                event.preventDefault();
                $scope.display_loader = true;
                
                $http.get(cmodule_site_url + "?d=visitors&c=chat&m=close_session").success(function (response) {
                    if (response.result == 'success') {
                        $scope.reset();
                    } else if (response.result == 'failed') {
                        $scope.displayError(response);
                    }
                });
            }

            // scroll chat box
            $scope.scroll_chat = function () {
                if ($scope.is_scrollable) {
                    //scrolling window to footer
                    //angular.element($scope.message_box_id).animate({scrollTop: angular.element($scope.message_box_id)[0].scrollHeight}, 1000);

                    angular.element($scope.message_box_id).mCustomScrollbar('scrollTo', 'bottom', {
                        scrollInertia: 100,
                        timeout: 10
                    });
                }
            }

            // will display error is accur
            $scope.displayError = function (data) {
                $scope.showError = true;
                $scope.errors = data.error;

                $timeout(function () {
                    $scope.display_loader = false;
                    $scope.showError = false;
                    $scope.errors = '';
                }, 2500);
            }

            // disable click
            $scope.disable_click = function (event) {
                event.preventDefault();
            }
        }
    ]).directive('onLastRepeat', function () {
        return function (scope, element, attrs) {
            if (scope.$last) {
                setTimeout(function () {
                    scope.$emit('onRepeatLast', element, attrs);
                }, 1);
            }
        };
    });
})();