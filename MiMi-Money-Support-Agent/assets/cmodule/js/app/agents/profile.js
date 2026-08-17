(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, ['ngImgCrop']).controller("ProfileController", function ($scope, $http, $interval, $timeout, $window, Setting, $log) {
        var userctr = this;
        userctr.profile_image = {};
        
        $scope.user = Setting.user;
        $scope.show_pass_form = true;
        $scope.online_users = [];

        /*
         * To cleas file input
         * @param {type} event
         * @returns {undefined}
         */
        $scope.clear_file_input = function (event) {
            event.preventDefault();
            userctr.profile_image = {};
        }
        
        // set operator profile window height.
        $scope.set_height = function () {
            var windowHeight = angular.element($window).outerHeight();
            var headerMargin = parseInt(angular.element('#operator-profile-header').css('margin-bottom'));
            var headerHeight = (angular.element('#operator-profile-header').outerHeight()) + headerMargin;
            var bottomMargin = 20;

            angular.element('.operator-profile-wrapper').height(windowHeight - headerHeight - bottomMargin);
        }
        
        $scope.set_height();
        //reset height on window resize
        angular.element($window).on('resize', function () {
            $scope.set_height();
        });

        // fetching user on ready
        $http.get(site_url + "?d=agents&c=agents&m=get&id=" + $scope.user.id).success(function (response) {
            if (response.result == 'success') {
                $scope.user = response.user;

                angular.element(".operator-profile-wrapper").mCustomScrollbar({
                    /* keyboard default options */
                    keyboard: {
                        enable: true,
                        scrollType: "stepless",
                        scrollAmount: "auto"
                    }
                });
            } else {
                $scope.notification.showErrors = true;
                $scope.notification.errors = response.errors;
            }
        });

        $scope.cancel = function () {
            window.location = site_url + '?d=agents&c=agents';
        }

        $scope.toggle_password_form = function () {
            window.location = site_url + '?d=agents&c=agents';
        }

        // update password
        $scope.update_password = function (event, index) {
            event.preventDefault();
            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;

            $http.post(site_url + "?d=agents&c=agents&m=update_password&id=" + $scope.user.id, $scope.user).success(function (response) {
                if (response.result == 'success') {
                    $scope.user.pass = '';
                    $scope.user.confirm_pass = '';

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }
            });
        }

        // save user
        $scope.save_user = function (event) {
            event.preventDefault();
            
            //hide notification
            $scope.notification.showMessage = false;
            $scope.notification.showErrors = false;
            
            if (userctr.profile_image.filename) {
                $scope.user.filesize = userctr.profile_image.filesize;
                $scope.user.filetype = userctr.profile_image.filetype;
                $scope.user.filename = userctr.profile_image.filename;
                $scope.user.base64 = userctr.profile_image.base64;
                $scope.user.base64WithData = userctr.profile_image.base64WithData;
            }

            $http.post(site_url + "?d=agents&c=agents&m=update_profile&id=" + $scope.user.id, $scope.user).success(function (response) {
                if (response.result == 'success') {
                    var imageUrl = response.profile_picture + '?decache=' + Math.random();
                    $scope.user.profile_picture = response.profile_picture;
                    $scope.user.profile_pic = response.profile_pic;
                    userctr.profile_image = {};

                    // overriding user
                    $scope.overrideUser($scope.user);

                    $scope.notification.showMessage = true;
                    $scope.notification.message = response.message;
                } else {
                    $scope.notification.showErrors = true;
                    $scope.notification.errors = response.errors;
                }
            });
        }

        // remove user profile picture
        $scope.remove_photo = function (id, event, confirm_msg) {
            event.preventDefault();
            if ($window.confirm(confirm_msg)) {
                $http.post(site_url + "?d=agents&c=agents&m=remove_picture&id=" + id).success(function (response) {
                    if (response.result == 'success') {
                        $scope.user.profile_pic = '';
                        $scope.user.profile_picture = '';

                        // overriding user
                        $scope.overrideUser($scope.user);

                        $scope.notification.showMessage = true;
                        $scope.notification.message = response.message;
                    } else {
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                });
            }
        }
    });
})();