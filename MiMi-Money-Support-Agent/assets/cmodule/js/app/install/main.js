var modules = ['cmodule.filters', 'cmodule.services', 'ngSanitize', 'ngAnimate'];
(function () {
    angular.module("cmodule", modules, function ($httpProvider) {
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
        cbsiteurl: cbsiteurl,
        product_name: product_name
    }).controller("MainController", function ($http, $scope, alertService, $timeout) {
        $scope.notification = alertService;
        $scope.display_loader = false;
        $scope.body_classes = '';
        $scope.custom_styles = '';
        
        /*
         * This function will toggle diplay to loader
         */

        $scope.toggleLoder = function () {
            $scope.display_loader = !$scope.display_loader;
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
    });
})();