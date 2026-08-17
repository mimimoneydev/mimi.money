<div id="filter" ng-controller="FilterCtrl">
    <div class="filter-inner">
        <div class="block-header">
            <button aria-label="Close" ng-click="hide_filter($event);" data-dismiss="filter-block" class="close" type="button"><span aria-hidden="true">×</span></button>
            <h3><?php echo $this->lang->line('filter');?></h3>
        </div>
        <form>
            <div class="form-group"><input ng-model="filters.keywords" ng-model-options="{ debounce: 250 }" class="form-control" type="text" placeholder="Type Keywords..."></div>
        </form>
    </div>
</div>