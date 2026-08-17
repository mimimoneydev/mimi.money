(function () {
    angular.module('canned-messages', []).directive('cannedMessage', function ($timeout, $uibModal, $document, $window) {
        var templateUrl;
        try {
            angular.module('ui.bootstrap.modal');
            templateUrl = 'template/canned/button-a.html';
        } catch (e) {
            console.error('No Modal module found');
            return {};
        }

        return {
            restrict: 'AEC',
            templateUrl: templateUrl,
            scope: {
                source: '=messageData',
                options: '=cannedOptions'
            },
            link: function ($scope, elm, attrs) {

                $scope.openCannedModal = function () {
                    var modalInstance = $uibModal.open({
                        animation: true,
                        //windowTopClass: 'additional-section',
                        //windowTemplateUrl: 'template/canned/canned-modal-window.html',
                        ariaLabelledBy: 'modal-title',
                        ariaDescribedBy: 'modal-body',
                        templateUrl: 'template/canned/html-a.html',
                        controller: 'ModalInstanceCtrl',
                        resolve: {
                            cannedOptions: function () {
                                return $scope.options;
                            }
                        }
                    });
                    
                    modalInstance.rendered.then(function() {
                        var modal = angular.element('.modal'),
                        dialog = modal.find('.modal-dialog');
                        
                        var dialog_height = dialog.height();
                        var win_height = angular.element($window).height();
                        var calculated_height = (win_height - dialog_height);

                        dialog.animate({marginTop: Math.max(0, (calculated_height / 2))}, 500, 'linear');
                    });

                    modalInstance.result.then(function (cannedMesg) {
                        $scope.source += ' ' + cannedMesg + ' ';
                    }, function () {
                        console.log('Modal dismissed at: ' + new Date());
                    });
                }
            }
        }
    }).controller('ModalInstanceCtrl', function ($uibModalInstance, $scope, $http, $timeout, cannedOptions) {
        //console.log(cannedOptions);
        $scope.form_title = '';
        $scope.showForm = false;
        $scope.be_confirm = false;
        $scope.canned_messages = [];
        $scope.output = {result: 'error', description: ''};
        $scope.record = {};

        $scope.options = {
            selector_title: 'Canned Messages',
            title: 'Canned Messages',
            search_placeholder: 'Search ...',
            no_record: 'No record found.',
            form_title: 'Add New Message',
            canned_title: 'Title',
            canned_description: 'Description',
            canned_add_new: 'Add',
            canned_save: 'save',
            canned_cancel: 'Cancel',
            canned_edit: 'Edit',
            canned_delete: 'Delete',
            canned_delete_confirm: 'Confirm',
            confirm_del: 'Are you really want to delete this message?',
            validation_required: 'This field is mandatory.',
            listUrl: '',
            postUrl: '',
            deleteUrl: '',
            user: {}
        }

        //merge and clean up options
        $scope.options = angular.extend({}, $scope.options, cannedOptions);

        /*
         * To handle server error 
         * @param {type} response
         * @returns {undefined}
         */
        $scope.handleServerError = function (response) {
            console.log('There is something issue in API response.');
            $scope.output.description = '<strong>' + response.status + ' ' + response.statusText + ' !</strong> ';
            $scope.output.description += response.data;

            /*$timeout(function () {
             $scope.output = {result: 'error', description: ''};
             }, 3000);*/
        }

        /*
         * To handle server response 
         * @param {type} response
         * @returns {undefined}
         */
        $scope.handleServerResponse = function (response) {
            $scope.output = response.data;

            $timeout(function () {
                $scope.output = {result: 'error', description: ''};
            }, 3000);
        }

        /*
         * To get message from server
         * @returns {undefined}
         */
        $scope.getMessages = function () {
            if ($scope.options.listUrl) {
                $http.get($scope.options.listUrl).then(function (response) {
                    $scope.handleServerResponse(response);

                    if (response.data.result == 'success') {
                        $scope.canned_messages = response.data.canned_messages;
                    }
                }, function (response) {
                    $scope.handleServerError(response);
                });
            } else {
                $scope.output.description = 'Canned messages listing url is not defined.';
            }
        }

        $scope.getMessages();
        
        /*
         * To add new entry
         * @param Event event
         * @returns {undefined}
         */
        $scope.addMessage = function (event) {
            event.preventDefault();

            $scope.form_title = $scope.options.form_title;
            $scope.record = {};
            $scope.showForm = true;
            $scope.be_confirm = false;
        }

        /*
         * To edit entry
         * @param Event event
         * @param Object message
         * @returns {undefined}
         */
        $scope.editMessage = function (event, message) {
            event.preventDefault();

            var index = $scope.canned_messages.indexOf(message);
            $scope.form_title = $scope.options.canned_edit+' - '+message.title;
            $scope.record = angular.copy(message);
            $scope.record.row_index = index;
            $scope.showForm = true;
            $scope.be_confirm = false;
        }

        /*
         * To save new entry
         * @returns {undefined}
         */
        $scope.saveMessage = function () {
            if ($scope.options.postUrl) {
                $http.post($scope.options.postUrl, $scope.record).then(function (response) {
                    $scope.handleServerResponse(response);

                    if (response.data.result == 'success') {
                        if(response.data.is_new) {
                            $scope.canned_messages.push(response.data.created)
                        } else {
                            $scope.canned_messages[$scope.record.row_index] = $scope.record;
                        }

                        $scope.showForm = false;
                        $scope.record = {};
                        
                        $scope.cannedMessageForm.$setPristine();
                    }
                }, function (response) {
                    $scope.handleServerError(response);
                });
            } else {
                $scope.output.description = 'Canned messages save url is not defined.';
            }
        }

        /*
         * To delete entry
         * @param Event event
         * @param Object message
         * @returns {undefined}
         */
        $scope.deleteMessage = function (event, message) {
            event.preventDefault();
            
            $scope.record = angular.copy(message);
            $scope.be_confirm = true;
        }

        /*
         * To cancel delete action
         * @param {type} event
         * @returns {undefined}
         */
        $scope.deleteCancel = function (event) {
            event.preventDefault();
            
            $scope.be_confirm = false;
        }

        /*
         * To delete entry from database after confirmation
         * @param {type} event
         * @returns {undefined}
         */
        $scope.deleteConfirmed = function (event) {
            event.preventDefault();
            $scope.be_confirm = false;

            if ($scope.options.deleteUrl) {
                $http.get($scope.options.deleteUrl + $scope.record.id).then(function (response) {
                    $scope.handleServerResponse(response);

                    if (response.data.result == 'success') {
                        $scope.getMessages();
                        $scope.record = {};
                    }
                }, function (response) {
                    $scope.handleServerError(response);
                });
            } else {
                $scope.output.description = 'Canned messages delete url is not defined.';
            }
        }

        /*
         * Toggle form to add new entry
         * @param {type} event
         * @returns {undefined}
         */
        $scope.toggleForm = function (event) {
            event.preventDefault();

            $scope.showForm = !$scope.showForm;
            $scope.record = {};
        }

        /*
         * To selet item.
         * 
         * @param Event event
         * @param Object canned         * 
         */
        $scope.selectCannedModal = function (event, canned) {
            event.preventDefault();

            $uibModalInstance.close(canned.description);
        };

        /*
         * To close modal window
         */
        $scope.cancelCannedModal = function () {
            $uibModalInstance.dismiss('cancel');
        };
    }).run(['$templateCache', function ($templateCache) {
            var $buttonHtml = '<i role="button" class="chat-canned-message" ng-click="openCannedModal()" title="{{options.selector_title}}"></i>';
            $templateCache.put('template/canned/button-a.html', $buttonHtml);

            var $mHeader = '<div class="modal-header">'
                    + '<button type="button" class="close" aria-hidden="true" ng-click="cancelCannedModal()">&times;</button>'
                    + '<h3 class="modal-title" id="modal-title">{{showForm? form_title : options.title}} '
                    + '<a ng-hide="showForm" class="btn btn-primary btn-sm" href="#" ng-click="addMessage($event)" title="{{options.canned_add_new}}">{{options.canned_add_new}}</a></h3>'
                    + '</div>';

            var $mInput = '<div ng-hide="showForm" class="animate-show">'
                    + '<div class="form-group"><input class="form-control" ng-model="searchStr" placeholder="{{options.search_placeholder}}"></div>'
                    + '</div>';

            var $addForm = '<form ng-show="showForm" class="animate-show" name="cannedMessageForm" method="post" novalidate ng-submit="cannedMessageForm.$valid && saveMessage()">'
                    //+ '<pre>{{cannedMessageForm|json}}</pre>'
                    + '<div class="form-group">'
                    + '<input class="form-control" name="title" ng-model="record.title" placeholder="{{options.canned_title}}" required>'
                    + '<span style="color:red" ng-show="cannedMessageForm.$submitted || cannedMessageForm.title.$dirty">'
                    + '<span ng-show="cannedMessageForm.title.$error.required">{{options.validation_required}}</span>'
                    + '</span>'
                    + '</div>'
                    + '<div class="form-group">'
                    + '<textarea class="form-control" cols="40" rows="4" name="description" ng-model="record.description" placeholder="{{options.canned_description}}" required></textarea>'
                    + '<span style="color:red" ng-show="cannedMessageForm.$submitted || cannedMessageForm.description.$dirty">'
                    + '<span ng-show="cannedMessageForm.description.$error.required">{{options.validation_required}}</span>'
                    + '</span>'
                    + '</div>'
                    + '<div class="form-group">'
                    + '<button class="btn btn-primary" type="submit">{{options.canned_save}}</button>'
                    + '<button class="btn btn-default" type="button" ng-click="toggleForm($event)">{{options.canned_cancel}}</button>'
                    + '</div>'
                    + '</form>';

            var $confirmDelete = '<div ng-if="be_confirm" class="well">'
                    + '<p ng-bind-html="options.confirm_del"></p>'
                    + '<a href="#" class="btn btn-primary btn-sm" aria-label="close" ng-click="deleteConfirmed($event)">{{options.canned_delete_confirm}}</a>'
                    + '<a href="#" class="btn btn-default btn-sm" aria-label="close" ng-click="deleteCancel($event)">{{options.canned_cancel}}</a>'
                    + '</div>';

            var $message_box = '<div class="alert" ng-class="{\'alert-success\':output.result == \'success\', \'alert-danger\':output.result != \'success\'}" ng-if="output.description">'
                    + '<a href="#" class="close" aria-label="close" ng-click="output.description = \'\'">&times;</a>'
                    + '<p ng-bind-html="output.description"></p>'
                    + '</div>';

            var $templateHtml = $mHeader
                    + '<div class="modal-body" id="modal-body">'
                    //+ '<pre>{{options.user|json}}</pre>'
                    + $confirmDelete
                    + $message_box
                    + $mInput
                    + $addForm
                    + '<div ng-hide="showForm" class="animate-show canned-container">'
                    + '<ul class="canned-message-list">'
                    + '<li class="" ng-repeat="row in canned_messages | filter: searchStr | limitTo : 10">'
                    + '<div class="canned-actions">'
                    + '<span ng-if="row.user_id == options.user.id" class="pull-right">'
                    + '<a class="canned-action" href="#" ng-click="editMessage($event, row)" title="{{options.canned_edit}}"><i class="fa fa-pencil"></i></a>'
                    + '<a class="canned-action" href="#" ng-click="deleteMessage($event, row)" title="{{options.canned_delete}}"><i class="fa fa-trash"></i></a>'
                    + '</span>'
                    + '<a class="list-title" href="#" ng-click="selectCannedModal($event, row)">{{ row.title }}</a>'
                    + '</div>'
                    + '<div ng-click="selectCannedModal($event, row)" role="button" class="list-content" ng-bind-html="row.description | cut:false:130"></div>'
                    + '</li>'
                    + '</ul>'
                    + '<p class="alert alert-info" ng-if="!canned_messages.length" ng-bind-html="options.no_record"></p>'
                    + '</div>'
                    + '</div>';
            $templateCache.put('template/canned/html-a.html', $templateHtml);

            var $windowModalHtml = '<div class="modal-table"><div class="modal-cell"><div class="modal-content" uib-modal-transclude></div></div></div>';
            $templateCache.put("template/canned/canned-modal-window.html", $windowModalHtml);
        }
    ]);
}());