(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, ['ui.tinymce']).controller("SiteSettingController", function ($scope, $http, $timeout, $window, Setting, $log) {
        var ssctr = this;
        ssctr.entry_avatar = {};
        
        $scope.tinymceOptions = {
            theme: 'modern',
            plugins: 'autolink link image imagetools advlist code fullscreen lists colorpicker textcolor wordcount spellchecker contextmenu emoticons',
            menubar: false,
            statusbar: true,
            image_advtab: true,
            advlist_bullet_styles: "circle",
            toolbar: 'bold italic forecolor backcolor | alignleft aligncenter alignright alignjustify | numlist bullist link image'
        };

        $scope.site = cmodule.site;
        $scope.settings = cmodule.site_settings;
        $scope.tags = cmodule.tags;
        $scope.settings.time_interwal = parseInt(cmodule.site_settings.time_interwal);
        $scope.settings.time_automatically_open_chatbox = parseInt(cmodule.site_settings.time_automatically_open_chatbox);
        $scope.settings.file_upload_size = parseInt(cmodule.site_settings.file_upload_size);
        $scope.settings.departments = ($scope.settings.departments) ? cmodule.site_settings.departments.split(',') : [];

        /*
         * Reset notification
         * @returns {undefined}
         */
        $scope.reset_notification = function () {
            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;
        }

        /*
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            ssctr.entry_avatar = {};
        }

        /*
         * Update site settings
         * @param event event
         * @param index index
         * @returns {undefined}
         */
        $scope.update_settings = function (event, index) {
            event.preventDefault();
            $scope.toggleLoder();

            //hide notification
            $scope.reset_notification();
            
            if (ssctr.entry_avatar.filename) {
                $scope.settings.entry_avatar = ssctr.entry_avatar;
            }

            $http.post(Setting.site_url + "?c=sites&m=update_site_settings&site_id=" + $scope.site.id, $scope.settings).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;
                    
                    if (response.default_avatar) {
                        $scope.settings.default_avatar = response.default_avatar;
                        $scope.settings.entry_avatar = ssctr.entry_avatar = {};
                    }
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

                $scope.toggleLoder();
            });
        }

        /*
         * update theme style
         * @returns {undefined}
         */
        $scope.update_theme = function () {
            $scope.custom_styles = ".chat-cmodule-header, .chatnox-btn-default, .chat-cmodule-header *, .chat-cmodule-header, .chat-cmodule-widget-head, .cmodule-window-widget-title, .chat-cmodule .cmodule-chat-icon { color: " + $scope.settings.title_color + " !important; }";
            $scope.custom_styles += ".chat-cmodule-header, .chatnox-btn-default, .chat-cmodule-header, .chat-cmodule-widget-head, .chat-cmodule .cmodule-chat-btn { background-color: " + $scope.settings.background_color + " !important; }";
            
            $scope.custom_styles += ".theme-bubbles_boom .chat-cmodule-message-reply-row .chat-cmodule-message * { color: " + $scope.settings.title_color + " !important; }";
            $scope.custom_styles += ".theme-bubbles_boom .chat-cmodule-message-reply-row .chat-cmodule-message { background-color: " + $scope.settings.background_color + " !important; }";
            
            $scope.update_custom_styles($scope.custom_styles);
        }

        $scope.update_theme();
    });
})();