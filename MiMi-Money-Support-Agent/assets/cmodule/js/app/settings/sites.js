(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("SitesController", function ($scope, $http, $timeout, $window, Setting, $log) {
        var mainctr = this;
        mainctr.entry_image = {};
        $scope.modal = '';

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
         * Reset Form
         * @returns {undefined}
         */
        $scope.reset_form = function () {
            $scope.record = {};
            mainctr.entry_image = {};
            $scope.modal = '';
        }

        /*
         * Hide modal
         * @param {type} event
         * @returns {undefined}
         */
        $scope.hide_modal = function () {
            $scope.reset_form();
            angular.element('#modal_window').modal('hide');

            $timeout(function () {
                $scope.reset_notification();
            }, 5000);
        }

        /*
         * Close modal onclick
         * @param {type} event
         * @returns {undefined}
         */
        $scope.close_modal = function (event) {
            event.preventDefault();
            $scope.hide_modal();
        }

        /*
         * To fetch all access tikens
         * @returns {undefined}
         */
        $scope.get_sites = function () {
            $http.get(Setting.site_url + "?c=sites&m=get_sites").success(function (response) {
                if (response.result == 'success') {
                    $scope.records = response.sites;
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
            });
        }

        $scope.get_sites();

        /*
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            mainctr.entry_image = {};
            mainctr.admin_logo = {};
        }

        /*
         * Display add new record form
         * @param {type} event
         * @returns {undefined}
         */
        $scope.add_record = function (event) {
            event.preventDefault();
            $scope.reset_notification();
            $scope.reset_form();

            $scope.modal = 'site_form';
            angular.element('#modal_window').modal('show');
        }

        /*
         * Display edit record form
         * @param {type} index
         * @param {type} record
         * @returns {undefined}
         */
        $scope.edit_record = function (event, record) {
            event.preventDefault();
            $scope.reset_notification();
            $scope.reset_form();

            $scope.modal = 'site_form';
            $scope.record = angular.copy(record);
            $scope.record.index = $scope.records.indexOf(record);
            angular.element('#modal_window').modal('show');
        }

        /*
         * Save record
         * @param Event event
         * @param Object lang_image
         * @returns {undefined}
         */
        $scope.save_record = function (event) {
            event.preventDefault();
            $scope.toggleLoder();
            $scope.reset_notification();

            if (mainctr.entry_image.filename) {
                $scope.record.filesize = mainctr.entry_image.filesize;
                $scope.record.filetype = mainctr.entry_image.filetype;
                $scope.record.filename = mainctr.entry_image.filename;
                $scope.record.base64 = mainctr.entry_image.base64;
            }

            if ($scope.record.id) {
                $http.post(Setting.site_url + "?c=sites&m=update&id=" + $scope.record.id, $scope.record).success(function (response) {
                    if (response.result == 'success') {
                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;

                        if (response.site_logo) {
                            $scope.record.site_logo = response.site_logo;
                        }

                        $scope.records[$scope.record.index] = $scope.record;
                        $scope.hide_modal();
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
            } else {
                $http.post(Setting.site_url + "?c=sites&m=store", $scope.record).success(function (response) {
                    if (response.result == 'success') {
                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;

                        $scope.records.push(response.new_record);
                        $scope.hide_modal();
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
        }

        /*
         * Open modal for site widget code
         */
        $scope.get_widget = function (event, record) {
            event.preventDefault();
            $scope.reset_notification();
            $scope.modal = 'widget';
            $scope.record = angular.copy(record);
            angular.element('#modal_window').modal('show');
        }

        /*
         * Delete record
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.delete_record = function (record) {
            $http.get(Setting.site_url + "?c=sites&m=destroy&id=" + record.id).success(function (response) {
                if (response.result == 'success') {
                    var index = $scope.records.indexOf(record);
                    $scope.records.splice(index, 1);

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    $scope.hide_modal();
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
            });
        }
    });
})();