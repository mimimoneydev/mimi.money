var app;
var modules = ['cmodule.filters', 'chatbull.directives', 'cmodule.services', 'ngSanitize', 'ngAnimate', 'colorpicker.module', 'checklist-model', 'canned-messages', 'ui.bootstrap', 'angular-smilies', 'naif.base64'];
(function () {
    app = angular.module("cmodule", modules, function ($httpProvider) {
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
    }).constant("Setting", {
        site_url: site_url,
        theme_url: theme_url,
        cbsiteurl: cbsiteurl,
        product_name: product_name,
        lang_lables: lang_lables,
        user: cmodule.user,
        settings: cmodule.settings
    });

    app.controller("BodyController", function ($http, $scope, alertService, $timeout, Setting, $window, operatorPanel) {
        $scope.agentPanel = operatorPanel;
        $scope.user = Setting.user;
        $scope.login = {};
        $scope.settings = Setting.settings;
        $scope.params = {c: 'admin', m: ''};
        $scope.is_upgradable = false;

        $scope.notification = alertService;
        $scope.display_loader = false;
        $scope.showNoRecordMessage = false;
        $scope.body_classes = '';
        $scope.filter_button = true;
        $scope.custom_styles = '';
        $scope.visible_area = "main-content";
        $scope.online_users = [];
        $scope.recent_chats = [];
        $scope.new_requests = [];
        $scope.anonymous_users = [];

        $scope.new_requests_counter = 0;
        $scope.offlineRequestsCounter = 0;
        $scope.last_request_id = 0;
        $scope.miliseconds = new Date().getTime();
        $scope.time_difference = 0;
        $scope.chatSession = {};
        $scope.typedMessages = {};
        $scope.anonymousData = {};

        // sidebar alert
        $scope.show_sidebar_alert = false;
        $scope.sidebar_alert_class = '';
        $scope.sidebar_message = '';

        $scope.offset = 0;
        $scope.records = [];
        $scope.is_edit = false;
        $scope.loading = false;
        $scope.is_sidebar_collapsed = false;
        $scope.colors = ['#f16364', '#f58559', '#f9a43e', '#e4c62e', '#67bf74', '#59a2be', '#2093cd', '#ad62a7', '#805781', '#e57373', '#f06292', '#a1887f'
                    , '#ba68c8', '#9575cd', '#7986cb', '#64b5f6', '#4fc3f7', '#4dd0e1', '#4db6ac', '#81c784', '#aed581', '#ff8a65', '#d4e157', '#ffd54f', '#ffb74d', '#90a4ae'];

        $scope.rand_color = '';
        $scope.active_color = '';
        
        $scope.windowWidth = $window.innerWidth;
        $scope.leftSidebarDrawerOpen = false;
        
        angular.element($window).bind('resize', function(){
           $scope.windowWidth = $window.innerWidth;
        });
        
        /**
         * Toggle left Sidebar Drawer Open
         * @param {Event} event
         * @returns {undefined}
         */
        $scope.toggleLeftSidebarDrawer = function(event) {
            event.preventDefault();
            
            $scope.leftSidebarDrawerOpen = !$scope.leftSidebarDrawerOpen;
        }

        /**
         * Vertically center Bootstrap 3 modals so they aren't always stuck at the top
         */
        function reposition() {
            var modal = angular.element(this),
                    dialog = modal.find('.modal-dialog');
            modal.css('display', 'block');

            // Dividing by two centers the modal exactly, but dividing by three 
            // or four works better for larger screens.
            $timeout(function () {
                var dialog_height = dialog.height();
                var win_height = angular.element($window).height();
                var calculated_height = (win_height - dialog_height);
            
                dialog.animate({marginTop: Math.max(0, (calculated_height / 2))}, 500, 'linear');
            }, 300);
        }
        // Reposition when a modal is shown
        angular.element('.modal').on('show.bs.modal', reposition);
        // Reposition when the window is resized
        angular.element($window).on('resize', function () {
            angular.element('.modal:visible').each(reposition);
        });

        /*
         * Ti dismis message
         * @param {Event} event
         * @returns {undefined}
         */
        $scope.dismis_message = function (event) {
            event.preventDefault();

            $scope.params.m = 'dismis_message';
            $http.get(site_url, {params: $scope.params}).success(function (response) {

            });
        }

        /*
         * To check is upgrades are available for current project.
         * @returns {undefined}
         */
        $scope.upgrade_available = function () {
            $http.get(Setting.site_url + "?c=admin&m=upgrade_available").success(function (response) {
                if (response.result == 'success') {
                    $scope.is_upgradable = true;
                }
            });
        }

        /*
         * This function will override user.
         * @returns {undefined}
         */
        $scope.overrideUser = function (userobj) {
            $scope.user = userobj;
        }

        /*
         * This function will return a random color
         * @returns {undefined}
         */
        $scope.getColor = function () {
            var rand_color = $scope.colors[Math.floor((Math.random() * 26) + 1)];
            return rand_color;
        }

        /**
         * 
         * @param {type} sid
         * @returns {undefined}
         */

        $scope.showNoMoreRecordAlert = function () {
            $scope.showNoRecordMessage = true;
            $timeout(function () {
                $scope.showNoRecordMessage = false;
            }, 5000);
        }

        /*
         * This function will store typed message in message box.
         * @param {type} sid
         * @returns {undefined}
         */

        $scope.storeTypedMessage = function (sid) {
            var new_message = angular.element("#message").val();
            if (sid > 0) {
                $scope.typedMessages[sid] = new_message;
            }
        }

        /*
         * This function will show message in sidebar
         * @param {type} alertMessage
         * @param {type} alertClass
         * @returns {undefined}
         */

        $scope.notifySidebarAlert = function (alertMessage, alertClass) {
            $scope.show_sidebar_alert = true;
            $scope.sidebar_alert_class = alertClass;
            $scope.sidebar_message = alertMessage;

            $timeout(function () {
                $scope.show_sidebar_alert = false;
                $scope.sidebar_alert_class = '';
                $scope.sidebar_message = '';
            }, 5000);
        }

        /*
         * This function will toggle diplay to loader
         */

        $scope.toggleLoder = function () {
            $scope.display_loader = !$scope.display_loader;
        }

        // get server currant time in milliseconds
        $http.get(site_url + "?d=agents&c=chat&m=get_server_time").success(function (response) {
            if (response.result == 'success') {
                $scope.miliseconds = new Date().getTime();
                $scope.time_difference = parseInt(response.milliseconds) - $scope.miliseconds;
            }
        });

        // change visible area
        $scope.change_visible_area = function (area_name) {
            $scope.visible_area = area_name;
        }

        /*
         * This function update data
         * @returns {undefined}
         */
        $scope.syncData = function (key, data) {
            $scope[key] = data;
        }

        // get online users
        $scope.get_online_users = function () {
            // get online users
            $http.get(site_url + "?d=agents&c=agents&m=get_online_users").success(function (response) {
                $scope.online_users = response;
            });
        }

        //set chect session.
        $scope.set_chat_session = function (session_data) {
            $scope.chatSession = session_data;
        }

        /*
         * Set anonymous user
         * 
         * @param Object anonymous_user
         * @returns {undefined}
         */
        $scope.show_anonymous_user = function (anonymous_user) {
            $scope.anonymousData = anonymous_user;
            $scope.anonymousData.fetched_at = new Date().getTime();
        }

        // hide filter
        $scope.hide_filter = function (event) {
            event.preventDefault();
            $scope.body_classes = '';
            $scope.filter_button = !$scope.filter_button;
            $scope.is_sidebar_collapsed = false;
        }

        // show filter
        $scope.show_filter = function (event) {
            event.preventDefault();
            $scope.body_classes = 'filter-visible';
            $scope.filter_button = !$scope.filter_button;
            $scope.is_sidebar_collapsed = true;
        }

        // toggle resize sidebar
        $scope.toogle_collapsed_sidebar = function (event) {
            event.preventDefault();
            if ($scope.is_sidebar_collapsed) {
                $scope.is_sidebar_collapsed = false;
            } else {
                $scope.is_sidebar_collapsed = true;
            }
        }

        // disable click
        $scope.disable_click = function (event) {
            event.preventDefault();
        }

        //update custom style
        $scope.update_custom_styles = function (new_styles) {
            $scope.custom_styles = new_styles;
        }

        // call custom trigger 
        $scope.fire_trigger = function (trigger_selector, trigger_event) {
            angular.element(trigger_selector).trigger(trigger_event);
        }

        //Play Standart
        $scope.play = function () {
            var audio = document.getElementById("audio1");
            audio.play();
        }
        
        /*
         * To login user
         * @returns {undefined}
         */
        $scope.login_user = function () {
            $scope.toggleLoder();
            
            $http.post(Setting.site_url + "?c=admin&m=logiin_ajax", $scope.login).success(function (response) {
                if (response.result == 'success') {
                    $timeout(function () {                
                        $scope.notification.login_error = false;
                        $scope.notification.showErrors = false;
                        $scope.notification.errors = '';

                        angular.element('#login-modal').modal('hide');
                    }, 300);
                } else if (response.result == 'failed') {
                    $scope.notification.login_error = true;
                    $scope.notification.errors = response.errors;
                }
                
                $scope.toggleLoder();
            });
        }

        // get online users, new requests and recent chats
        $scope.get_related_data = function () {
            $http.post(site_url + "?d=agents&c=agents&m=get_related_data").success(function (response) {
                if (response.result == 'success') {
                    if (!angular.equals($scope.recent_chats, response.data.chatListData)) {
                        if ($scope.recent_chats.length > 0 && $scope.recent_chats.length < response.data.chatListData.length) {
                            $scope.play();
                        }

                        //angular.merge($scope.recent_chats, response.data.chatListData);
                        $scope.recent_chats = response.data.chatListData;
                    }

                    // play sound on new request
                    if ($scope.new_requests_counter < response.data.new_requests_counter) {
                        $scope.play();
                    }

                    $scope.new_requests_counter = response.data.new_requests_counter;
                    $scope.offlineRequestsCounter = response.data.offline_requests;
                    $scope.online_users = response.data.online_users;

                    if (!angular.equals($scope.anonymous_users, response.data.anonymous_users)) {
                        $scope.anonymous_users = response.data.anonymous_users;
                    }
                } else if (response.result == 'failed') {
                    if(response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        //$scope.get_related_data();

        // get recent chats
        $scope.get_recent_chats = function () {
            $http.post(site_url + "?d=agents&c=chat&m=index").success(function (response) {
                if (response.result == 'success') {
                    if (!angular.equals($scope.recent_chats, response.data.chatListData)) {
                        $scope.recent_chats = response.data.chatListData;
                    }
                } else if (response.result == 'failed') {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }
            });
        }

        // get new requests
        $scope.get_new_requests = function () {
            $scope.toggleLoder();
            $http.post(site_url + "?d=agents&c=agents&m=get_new_requests&last_request_id=" + $scope.last_request_id).success(function (response) {
                if (response.result == 'success') {
                    $scope.new_requests = response.data.new_requests;
                    /*$scope.last_request_id = response.data.last_request_id;
                     angular.forEach(response.data.new_requests, function (row, key) {
                     $scope.new_requests.push(row);
                     });*/
                } else if (response.result == 'failed') {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }

                $scope.toggleLoder();
                $scope.change_visible_area('new-requests');
                
                $timeout(function () {
                    $scope.set_newrequests_height();
                }, 300);
                
                //reset height on window resize
                angular.element($window).on('resize', function () {
                    $scope.set_newrequests_height();
                });

                angular.element(".new-requests-wrapper").mCustomScrollbar({
                    /* keyboard default options */
                    keyboard: {
                        enable: true,
                        scrollType: "stepless",
                        scrollAmount: "auto"
                    }
                });
            });
        }
        
        // set new requests window height.
        $scope.set_newrequests_height = function () {
            var windowHeight = angular.element($window).outerHeight();
            var headerMargin = parseInt(angular.element('#new-requests-header').css('margin-bottom'));
            var headerHeight = (angular.element('#new-requests-header').outerHeight()) + headerMargin;
            var bottomMargin = 20;

            angular.element('.new-requests-wrapper').height(windowHeight - headerHeight - bottomMargin);
        }

        // show new request
        $scope.show_new_requests = function (event) {
            event.preventDefault();
            $scope.agentPanel.right_sidebar = false;
            
            $scope.get_new_requests(); 
        }

        // accept Request
        $scope.accept_request = function (event, row) {
            event.preventDefault();

            $http.post(site_url + "?d=agents&c=chat&m=accept_request&request_id=" + row.id + '&csid=' + row.chat_session_id).success(function (response) {
                if (response.result == 'success') {
                    var index = $scope.new_requests.indexOf(row);
                    $scope.new_requests.splice(index, 1);

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    $timeout(function () {
                        angular.element('.nav-tabs a[href="#recent-chats"]').tab('show');
                        $scope.fire_trigger('#chat-session-' + row.chat_session_id, 'click');
                        $scope.change_visible_area('workroom');
                    }, 3000);

                } else if (response.result == 'failed') {
                    var index = $scope.new_requests.indexOf(row);
                    $scope.new_requests.splice(index, 1);

                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }
            });
        }
    });
})();