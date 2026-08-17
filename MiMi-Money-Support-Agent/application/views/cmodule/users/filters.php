<div id="filter" ng-controller="FilterCtrl">
    <div class="filter-inner">
        <div class="block-header">
            <button aria-label="Close" ng-click="hide_filter($event);" data-dismiss="filter-block" class="close" type="button"><span aria-hidden="true">×</span></button>
            <h3><?php echo $this->lang->line('filter'); ?></h3>
        </div>
        <div class="filter-inset mCustomScrollbar">
            <form>
                <div class="form-group"><input ng-model="filters.keywords" ng-model-options="{ debounce: 250 }" class="form-control" type="text" placeholder="Type Keywords..."></div>
                <div class="form-group">
                    <h5><?php echo $this->lang->line('user_type'); ?></h5>
                    <div ng-repeat="role in roles" class="checkbox checkbox-replace">
                        <input id="filter-role-{{$index}}" ng-model="selected_roles[$index]" ng-change="toggle_check_roles($index, role.name)" type="checkbox" ng-true-value="'{{role.name}}'" ng-false-value=""> 
                        <label for="filter-role-{{$index}}">{{role.label}}</label>
                    </div>
                </div>
                <div class="form-group">
                    <div class="category-block">
                        <h5><?php echo $this->lang->line('departments'); ?></h5>
                        <div ng-repeat="tag in tags" class="checkbox checkbox-replace">
                            <input id="filter-tag-{{$index}}" ng-model="selected_tags[$index]" ng-change="toggle_check_tags($index, tag.id)" type="checkbox" ng-true-value="'{{tag.id}}'" ng-false-value=""> 
                            <label for="filter-tag-{{$index}}">{{tag.tag_name}}</label>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>