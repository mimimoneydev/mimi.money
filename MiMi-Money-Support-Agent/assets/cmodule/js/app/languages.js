(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("LanguagesController", function ($scope, $http, $timeout, Setting, $log, $window) {
        $scope.formSection = '';
        $scope.lang_files = [];
        $scope.filedata = {};
        $scope.copied_data = {};
        $scope.current_file = '';
        $scope.lang_image = {};
        $scope.readonly_files = ['calendar_lang.php', 'db_lang.php', 'email_lang.php', 'ftp_lang.php', 'imglib_lang.php', 'migration_lang.php', 'date_lang.php',
            'number_lang.php', 'pagination_lang.php', 'profiler_lang.php', 'unit_test_lang.php', 'index.html', 'token_lang.php', 'install_lang.php', 'upgrade_lang.php'];

        /*
         * To get all records from server
         */
        $scope.getAll = function () {
            $scope.toggleLoder();

            $http.get(Setting.site_url + "?c=languages&m=all").success(function (response) {
                if (response.result == 'success') {
                    $scope.records = response.languages;
                }

                $scope.toggleLoder();
            });
        }

        $scope.getAll();

        /*
         * Reset Form
         * @returns {undefined}
         */
        $scope.reset_form = function () {
            $scope.record = {};
            $scope.formSection = '';
            $scope.lang_image = {};
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
         * Hide modal
         * @param {type} event
         * @returns {undefined}
         */
        $scope.hide_modal = function () {
            $scope.reset_form();

            angular.element('#formblock').modal('hide');

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
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            $scope.lang_image = {};
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
            $scope.formSection = 'lang_form';
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

            $scope.formSection = 'lang_form';
            $scope.record = angular.copy(record);
            $scope.record.index = $scope.records.indexOf(record);
            angular.element('#formblock').modal('show');
        }

        /*
         * Save record
         * @param Event event
         * @param Object lang_image
         * @returns {undefined}
         */
        $scope.save_record = function (event, lang_image) {
            event.preventDefault();
            $scope.toggleLoder();
            $scope.reset_notification();

            if (lang_image.filename) {
                $scope.record.filesize = lang_image.filesize;
                $scope.record.filetype = lang_image.filetype;
                $scope.record.filename = lang_image.filename;
                $scope.record.base64 = lang_image.base64;
            }

            if ($scope.record.id) {
                $http.post(Setting.site_url + "?c=languages&m=update&id=" + $scope.record.id, $scope.record).success(function (response) {
                    if (response.result == 'success') {
                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;

                        if (response.lang_flag) {
                            $scope.record.lang_flag = response.lang_flag;
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
                $http.post(Setting.site_url + "?c=languages&m=store", $scope.record).success(function (response) {
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
         * Change status
         * 
         * @param Event event
         * @param Object record
         * @returns {undefined}
         */
        $scope.toogle_status = function (event, record) {
            event.preventDefault();
            $scope.toggleLoder();

            if (record.lang_status != 'publish') {
                $http.get(Setting.site_url + "?c=languages&m=activate&id=" + record.id).success(function (response) {
                    $scope.toggleLoder();
                    if (response.result == 'success') {
                        $scope.getAll();

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
        }

        /*
         * Delete record
         * 
         * @param Object record
         * @returns {undefined}
         */
        $scope.delete_record = function (record) {
            $http.get(Setting.site_url + "?c=languages&m=destroy&id=" + record.id).success(function (response) {
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

        /*
         * Get all files of a language and display a list
         * 
         * @param Event event
         * @param Object record
         * @returns {undefined}
         */
        $scope.list_files = function (event, record) {
            event.preventDefault();
            $scope.toggleLoder();
            $scope.record = angular.copy(record);

            $http.get(Setting.site_url + "?c=languages&m=lang_files&id=" + record.id).success(function (response) {
                if (response.result == 'success') {
                    $scope.formSection = 'lang_files';
                    $scope.lang_files = response.files;
                    angular.element('#formblock').modal('show');
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
         * Check is file can be edit or not
         * @param String filename
         * @returns {Boolean}
         */
        $scope.is_editable = function (filename) {
            if ($.inArray(filename, $scope.readonly_files) != -1) {
                return false;
            }

            return true;
        }

        /*
         * Translate file to selected language
         * 
         * @param Event event
         * @param String file
         * @returns {undefined}
         */
        $scope.translate_flie = function (event, file) {
            event.preventDefault();
            $scope.reset_notification();
            $scope.current_file = file;

            $http.get(Setting.site_url + "?c=languages&m=get_file&id=" + $scope.record.id + "&langfile=" + $scope.current_file).success(function (response) {
                if (response.result == 'success') {
                    $scope.formSection = 'translate_form';
                    $scope.filedata = response.filedata;
                    $scope.copied_data = response.english_data;
                    
                    var modal = angular.element('#formblock'),
                    dialog = modal.find('.modal-dialog');
                    dialog.addClass('modal-lg');
                    
                    // Dividing by two centers the modal exactly, but dividing by three 
                    // or four works better for larger screens.
                    $timeout(function () {
                        var dialog_height = dialog.height();
                        var win_height = angular.element($window).height();
                        var calculated_height = (win_height - dialog_height);

                        dialog.animate({marginTop: Math.max(0, (calculated_height / 2))}, 500, 'linear');
                    }, 300);
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

        /*
         * Back to files listing view
         * @param {type} event
         * @returns {undefined}
         */
        $scope.back_to_files = function (event) {
            event.preventDefault();
            $scope.current_file = '';
            $scope.formSection = 'lang_files';
            
            var modal = angular.element('#formblock'),
            dialog = modal.find('.modal-dialog');
    
            dialog.removeClass('modal-lg');

            // Dividing by two centers the modal exactly, but dividing by three 
            // or four works better for larger screens.
            $timeout(function () {
                var dialog_height = dialog.height();
                var win_height = angular.element($window).height();
                var calculated_height = (win_height - dialog_height);

                dialog.animate({marginTop: Math.max(0, (calculated_height / 2))}, 500, 'linear');
            }, 100);

            $timeout(function () {
                $scope.reset_notification();
            }, 5000);
        }

        /*
         * Translate data to selected language
         */
        $scope.translate_data = function (event) {
            event.preventDefault();

            $http.post(Setting.site_url + "?c=languages&m=update_translate&id=" + $scope.record.id + "&langfile=" + $scope.current_file, $scope.filedata).success(function (response) {
                if (response.result == 'success') {
                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;

                    $scope.back_to_files(event);
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

            $log.log($scope.filedata);
        }

    });
})();