(function () {
    var controlle_name = 'update-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("UpdateController", function ($http, $scope, $timeout) {
        //$scope.notification = alertService;
        $scope.new_version = cmodule.new_version;
        $scope.next_version = '';
        $scope.download_button = true;
        $scope.processing = false;
        $scope.processing_text = 'Starting downloading...';
        $scope.body_classes = '';
        $scope.pagetitle = 'Chatbull Updates';

        /*
         * To check next version of current version
         * @param {type} event
         */

        $scope.get_next_version = function () {
            $http.get(site_url + "?c=update&m=next_version").success(function (response) {
                if (response.result == 'success') {                    
                    if(response.next_version <= $scope.new_version ){
                        $scope.next_version = response.next_version;                        
                        $scope.download_update();
                    }
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                    $scope.processing = false;
                    $scope.processing_text = '';
                }
            });
        }
        
        /*
         * To start updating process
         * @param {type} event
         */
        
        $scope.start_update = function(event){
            event.preventDefault();
            $scope.processing = true;
            
            $scope.get_next_version();
        }

        /*
         * This functin will download latest updates
         * @param {type} data
         * @returns {undefined}
         */

        $scope.download_update = function () {
            $scope.processing_text = 'Downloading Chatbull version '+$scope.next_version+'...';

            $http.get(site_url + "?c=update&m=download").success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = $scope.notification.message + '<br>' + response.message;

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
            $scope.processing_text = 'Extracting files...';

            $http.get(site_url + "?c=update&m=extract").success(function (response) {
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
            $scope.processing_text = 'Installing updates...';

            $http.get(site_url + "?c=update&m=update_db").success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = $scope.notification.message + '<br>' + response.message + '<br>-------------------------------------------------';

                    if (response.current_version == $scope.new_version) {
                        $scope.processing = false;
                        $scope.processing_text = '';
                        $scope.download_button = false;
                        
                        $scope.update_version();
                    } else {
                        $scope.get_next_version();
                    }
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

        $scope.update_version = function () {
            $http.get(site_url + "?c=update&m=update_version").success(function (response) {
                if (response.result == 'success') {
                    
                }
            });
        }
    });
})();