(function () {
    var controlle_name = 'install-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("InstallController", function ($http, $scope, $timeout) {
        /*$scope.notification = alertService;
        $scope.display_loader = false;
        $scope.body_classes = '';
        $scope.custom_styles = '';*/
        $scope.visible_area = cmodule.intallation_step;
        $scope.step_title = 'Welcome to Chatbull installer';
        $scope.setup_db = false;
        $scope.setup_user = false;
        $scope.setup_complete = false;
        $scope.setup_completed_msg = '';
        $scope.db = {};
        $scope.user = {};

        $scope.$watch("visible_area", function (newvalue) {
            if (newvalue == 'setup-db') {
                $scope.setup_db = true;
                $scope.setup_user = false;
                $scope.setup_complete = false;
            } else if (newvalue == 'setup-user') {
                $scope.setup_db = false;
                $scope.setup_user = true;
                $scope.setup_complete = false;
            } else if (newvalue == 'setup-complete') {
                $scope.setup_db = false;
                $scope.setup_user = false;
                $scope.setup_complete = true;
            }
        });

        /*
         * This function will check all tables installed
         * @param {type} event
         * @returns {undefined}
         */

        $scope.is_db_installed = function (data) {
            $http.get(site_url + "?c=install&m=all_tables_created").success(function (response) {
                if (response.result == 'success') {
                    $scope.setup_db = true;
                    $scope.visible_area = "setup-user";

                    $scope.notification.showMessage = true;
                    $scope.notification.message = data.message;

                    $scope.toggleLoder();
                } else {
                    $scope.is_db_installed(data);
                }
            });
        }

        /*
         * This function will install database and table.
         * and also configure config files.
         */

        $scope.setup_database = function (event) {
            event.preventDefault();
            $scope.toggleLoder();
            $scope.notification.showErrors = false;
            $scope.notification.showMessage = false;

            $http.post(site_url + "?c=install&m=setup_db", $scope.db).success(function (response) {
                if (response.result == 'success') {
                    $scope.is_db_installed(response);
                } else if (response.result == 'failed') {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                    $scope.toggleLoder();
                }
            });
        }

        /*
         * This function will insert user in databse as admin
         */

        $scope.setup_admin = function (event) {
            event.preventDefault();
            $scope.toggleLoder();
            $scope.notification.showErrors = false;
            $scope.notification.showMessage = false;

            $http.post(site_url + "?c=install&m=setup_user", $scope.user).success(function (response) {
                if (response.result == 'success') {
                    $scope.setup_user = true;
                    $scope.setup_complete = true;
                    $scope.visible_area = "setup-complete";

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    $scope.setup_completed_msg = response.completed_message;
                    $scope.step_title = response.page_title;
                } else if (response.result == 'failed') {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }
                $scope.toggleLoder();
            });
        }
    });
})();