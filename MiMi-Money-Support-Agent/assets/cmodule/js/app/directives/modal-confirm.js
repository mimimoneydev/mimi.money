(function () {
    var controlle_name = 'confirm-modal';
    modules.push(controlle_name);
    angular.module(controlle_name, []).directive('ngConfirmClick', function ($uibModal, Setting, $window, $log) {

        var ModalInstanceCtrl = function ($scope, $uibModalInstance) {
            $scope.ok = function () {
                $uibModalInstance.close();
            };
            $scope.cancel = function () {
                $uibModalInstance.dismiss('cancel');
            };
        };

        return {
            restrict: 'A',
            scope: {
                ngConfirmClick: "&",
                item: "="
            },
            link: function (scope, element, attrs) {
                element.bind('click', function (event) {
                    event.preventDefault();
                    
                    var message = attrs.ngConfirmMessage || "Are you sure to delete ?";

                    //*This doesn't works
                    var modalHtml = '<div class="modal-body">';
                    modalHtml += '<p>' + message + '</p>';
                    modalHtml += '<div class="form-footer"><button class="btn btn-primary" ng-click="ok()">'+Setting.lang_lables.yes+'</button><button class="btn btn-default" ng-click="cancel()">'+Setting.lang_lables.no+'</button></div>';
                    modalHtml += '</div>';
                    var modalInstance = $uibModal.open({
                        template: modalHtml,
                        controller: ModalInstanceCtrl
                    });
                    
                    modalInstance.rendered.then(function() {
                        var modal = angular.element('.modal'),
                        dialog = modal.find('.modal-dialog');
                        
                        var dialog_height = dialog.height();
                        var win_height = angular.element($window).height();
                        var calculated_height = (win_height - dialog_height);

                        dialog.animate({marginTop: Math.max(0, (calculated_height / 2))}, 500, 'linear');
                    });

                    modalInstance.result.then(function () {
                        scope.ngConfirmClick({item: scope.item}); //raise an error : $digest already in progress
                    }, function () {
                        //Modal dismissed
                    });
                });
            }
        }
    });
})();