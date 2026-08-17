(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("UpgradeController", function ($scope, $http, $timeout, $window, Setting, $log) {
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
        
        $scope.getPluginsLinks();
    });
})();