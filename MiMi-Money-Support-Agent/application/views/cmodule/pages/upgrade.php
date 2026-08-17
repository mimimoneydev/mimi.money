<div ng-controller="UpgradeController">
    <div class="header-panel clearfix">
        <h2><?php echo $this->lang->line('upgrade'); ?></h2>
    </div>

    <div class="form-upgrade-plugins bs-card" ng-if="plugins.length > 0">
        <div class="row">
            <div ng-repeat="row in plugins" class="col-md-3 col-xs-4">
                <div class="thumbnail">
                    <a target="_new" ng-href="{{row.link}}" title="{{row.title}}"><img ng-src="{{row.logo_thumb}}" alt="{{row.title}}" class="img-responsive"></a>

                    <div class="caption text-center">
                        <h3><a target="_new" ng-href="{{row.link}}" title="{{row.title}}">{{row.title}}</a></h3>
                        <h5><strong>{{row.version}}</strong></h5>
                        <p><span><span><?php echo $this->lang->line('price'); ?>: </span> {{row.price|currency}}</span></p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>