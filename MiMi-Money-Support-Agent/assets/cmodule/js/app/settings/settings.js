(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("SettingController", function ($scope, $http, $timeout, $window, Setting, $log) {
        var mainctr = this;
        mainctr.admin_logo = {};

        $scope.settings = Setting.settings;
        $scope.verify_lkey = true;
        $scope.plugin = {license_key: angular.copy($scope.settings.licence_key)};

        // set numeric value
        $scope.settings.agent_file_upload_size = parseInt(Setting.settings.agent_file_upload_size);
        $scope.settings.google_map_zoom = parseInt(Setting.settings.google_map_zoom);
        $scope.settings.agent_time_interwal = parseInt(Setting.settings.agent_time_interwal);

        if ($scope.settings.plugin_validated == 'yes') {
            $scope.verify_lkey = false;
        }

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
         * To change license key will show input box.
         * @param {type} event
         * @returns {undefined}
         */
        $scope.toggle_license_key = function (event) {
            event.preventDefault();

            $scope.verify_lkey = !$scope.verify_lkey;
        }

        /*
         * To verify license key from server
         * @param {type} event
         * @returns {undefined}
         */
        $scope.verify_license_key = function (event) {
            event.preventDefault();
            $scope.toggleLoder();

            //hide notification
            $scope.reset_notification()

            $http.post(Setting.site_url + "?c=settings&m=verify_license_key", $scope.plugin).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    $scope.settings.licence_key = angular.copy($scope.plugin.license_key);
                    $scope.settings.plugin_validated = 'yes';
                    $scope.verify_lkey = false;
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }

                $scope.toggleLoder();
            });
        }

        /*
         * To unregister license key from server
         * 
         * @returns {undefined}
         */
        $scope.unregister = function () {
            $scope.toggleLoder();
            //hide notification
            $scope.reset_notification()

            $http.post(Setting.site_url + "?c=settings&m=unregister", $scope.plugin).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    $scope.plugin.license_key = '';
                    $scope.settings.licence_key = angular.copy($scope.plugin.license_key);
                    $scope.settings.plugin_validated = 'no';
                    $scope.verify_lkey = true;
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }

                $scope.toggleLoder();
            });
        }

        // update password
        $scope.update_settings = function (event, index) {
            event.preventDefault();
            $scope.toggleLoder();

            //hide notification
            $scope.reset_notification()

            if (mainctr.admin_logo.filename) {
                $scope.settings.admin_logo = mainctr.admin_logo;
            }

            $http.post(Setting.site_url + "?c=settings&m=update_settings", $scope.settings).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    if (response.admin_panel_logo) {
                        $scope.settings.admin_panel_logo = response.admin_panel_logo;
                        $scope.settings.admin_logo = mainctr.admin_logo = {};
                    }
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }

                $scope.toggleLoder();
            });
        }

        /*
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            mainctr.admin_logo = {};
        }
    });
})();