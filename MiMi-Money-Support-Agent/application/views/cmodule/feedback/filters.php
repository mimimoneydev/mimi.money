<div id="filter" ng-controller="FilterCtrl">
    <div class="filter-inner">
        <div class="block-header">
            <button aria-label="Close" ng-click="hide_filter($event);" data-dismiss="filter-block" class="close" type="button"><span aria-hidden="true">×</span></button>
            <h3><?php echo $this->lang->line('filter'); ?></h3>
        </div>
        <form>
            <div class="form-group">
                <h5><?php echo $this->lang->line('agents'); ?></h5>
                <div ng-repeat="row in agents" class="checkbox checkbox-replace">
                    <input id="filter-row-{{$index}}" checklist-model="filters.agents" type="checkbox" checklist-value="row.id"> 
                    <label for="filter-row-{{$index}}">{{row.name}}</label>
                </div>
            </div>
            <button class="btn btn-primary btn-sm" ng-click="checkAll()"><?php echo $this->lang->line('check_all'); ?></button>
            <button class="btn btn-primary btn-sm" ng-click="uncheckAll()"><?php echo $this->lang->line('uncheck_all'); ?></button>
        </form>
    </div>
</div>