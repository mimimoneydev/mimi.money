(function () {
    var module_name = 'layout-services';
    modules.push(module_name);
    angular.module(module_name, []).factory('operatorPanel', function () {
        return {
            right_sidebar: false
        };
    });
})();