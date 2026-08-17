(function () {
    var controlle_name = 'upgrade-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("UpgradeController", function ($scope, $http, $window, $log, Setting) {
        $scope.has_license_key = '';
        $scope.verified_license_key = false;
        $scope.processing = false;
        $scope.upgrade_text = cmodule.upgrade_text;
        $scope.processing_text = '';
        $scope.files_updated = cmodule.files_updated;
        $scope.action_type = cmodule.action_type;
        $scope.is_upgraded = false;
        
        $scope.record = {license_key: '', downloaded_filename: ''};
        $scope.plugins = [];

        /*
         * To get links of plugins.
         */
        $scope.getPluginsLinks = function () {
            $scope.toggleLoder();
            
            $http.get(Setting.site_url + "?c=upgrade&m=get_server&action=pro-links").success(function (response) {
                if(response.result == 'success') {
                    $scope.plugins = response.plugins;
                }
                
                $scope.toggleLoder();
            });
        }

        /*
         * Show upgrade otions
         * @returns {undefined}
         */
        $scope.showOptions = function () {
            if ($scope.has_license_key == 'no') {
                $scope.getPluginsLinks();
            }
        }

        /*
         * To verify license key from server
         * @param {type} event
         * @returns {undefined}
         */
        $scope.verify_license_key = function (event) {
            event.preventDefault();
            $scope.toggleLoder();

            $http.post(Setting.site_url + "?c=upgrade&m=verify_license_key", $scope.record).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;
                    $scope.verified_license_key = true;
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }

                $scope.toggleLoder();
            });
        }
        
        /*
         * To start upgrading process
         * @param {type} event
         */
        
        $scope.start_upgrade = function(event){
            event.preventDefault();
            $scope.processing = true;
            
            $scope.download_upgrade();
        }
        
        /*
         * To start updating databse process
         * @param {type} event
         */
        
        $scope.start_update_db = function(event){
            event.preventDefault();
            $scope.processing = true;
            
            $scope.update_db();
        }
        
        /*
         * This functin will download latest updates
         * @param {type} data
         * @returns {undefined}
         */

        $scope.download_upgrade = function () {
            $scope.processing_text = $scope.upgrade_text.upgrade_downloading;

            $http.get(site_url + "?c=upgrade&m=download").success(function (response) {
                if (response.result == 'success') {
                    $scope.action_type = 'upgrade';
                    $scope.notification.showMessage = true;
                    $scope.notification.message = $scope.notification.message + '<br>' + response.message;
                    $scope.record.downloaded_filename = response.downloaded_filename;

                    $scope.extract_download();
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                    $scope.processing = false;
                    $scope.processing_text = '';
                }
            });
        }

        /*
         * This functin will download latest updates
         * @param {type} data
         * @returns {undefined}
         */

        $scope.extract_download = function () {
            $scope.processing_text = $scope.upgrade_text.extracting;

            $http.get(site_url + "?c=upgrade&m=extract&downloaded_filename="+$scope.record.downloaded_filename).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = $scope.notification.message + '<br>' + response.message;

                    $scope.update_db();
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                    $scope.processing = false;
                    $scope.processing_text = '';
                }
            });
        }

        /*
         * This function will check all tables installed
         * @param {type} event
         * @returns {undefined}
         */

        $scope.update_db = function () {
            $scope.processing_text = $scope.upgrade_text.updating_db;

            $http.get(site_url + "?c=upgrade&m=update_db").success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = $scope.notification.message + '<br>' + response.message;

                    $scope.upgrade_complete();
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                    $scope.processing = false;
                    $scope.processing_text = '';
                }
            });
        }
        
        /*
         * To update latest version in databse.
         * @returns {undefined}
         */

        $scope.upgrade_complete = function () {
            $scope.processing_text = $scope.upgrade_text.configuring;
            $http.get(site_url + "?c=upgrade&m=upgrade_complete&action_type="+$scope.action_type).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.message = $scope.notification.message + '<br>' + response.message + '<br>-------------------------------------------------';
                    $scope.processing = false;
                    $scope.processing_text = '';
                    $scope.is_upgraded = true;
                }
            });
        }
    });
})();