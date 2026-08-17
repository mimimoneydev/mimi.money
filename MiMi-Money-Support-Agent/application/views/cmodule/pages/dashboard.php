<div ng-controller="DashboardController">
    <div class="header-panel clearfix">        
        <div class="sites-block">
            <h2 class="page-title"><?php echo $this->lang->line('dashboard'); ?></h2>
            <div class="dropdown">
                <span class="filter-by"><?php echo $this->lang->line('dash_filter_by_site'); ?></span>
                <a class="select-box select-box-filter dropdown-toggle text-capitalize" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                    {{current_site.site_name||'<?php echo $this->lang->line('dash_all'); ?>'}}
                    <i class="fa fa-angle-down fa-lg" aria-hidden="true"></i>
                </a>
                <ul class="dropdown-menu">
                    <li ng-class="{'active':!current_site.id}"><a href="#" ng-click="reload_dashboard($event, 'all')"><?php echo $this->lang->line('dash_all'); ?></a></li>
                    <li ng-repeat="site in sites" ng-class="{'active':site.id == current_site.id}"><a href="#" ng-click="reload_dashboard($event, site)">{{site.site_name}}</a></li>
                </ul>
            </div>
        </div>
    </div>
    
    <div class="row">
        <div class="col-lg-9 col-md-8 col-sm-12">
            <div class="panel panel-default animated bounceInLeft">
                <div class="panel-heading"> 
                    <div class="panel-title"><?php echo $this->lang->line('dash_overall_visitors'); ?> 
                        <div class="pull-right">
                            <ul class="list-inline">
                                <li class="dropdown">
                                    <span class="filter-by"><?php echo $this->lang->line('dash_year'); ?></span>
                                    <a class="select-box select-box-filter dropdown-toggle" type="button" id="dropdown-visitors1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                                        {{current_year.year}}
                                        <i class="fa fa-angle-down fa-lg" aria-hidden="true"></i>
                                    </a>                                    
                                    <ul class="dropdown-menu">
                                        <li ng-repeat="row in working_years|orderBy:'-year'" ng-class="{active:row.year == current_year.year}">
                                            <a href="#" ng-click="change_year($event, row)">{{row.year}}</a>
                                        </li>
                                    </ul>
                                </li>

                                <li class="dropdown">
                                    <span class="filter-by"><?php echo $this->lang->line('dash_month'); ?></span>
                                    <a class="select-box select-box-filter dropdown-toggle text-capitalize" type="button" id="dropdown-visitors2" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                                        {{current_month.name}}
                                        <i class="fa fa-angle-down fa-lg" aria-hidden="true"></i>
                                    </a>
                                    
                                    <ul class="dropdown-menu dropdown-menu-right">
                                        <li ng-repeat="row in current_year.months" ng-class="{active:row.month == current_month.month}">
                                            <a href="#" ng-click="change_month($event, row)">{{row.name}}</a>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </div>  
                    </div>
                </div>

                <div class="panel-body">
                    <div><span id="container-legend"></span><p class="info-block help-block"><?php echo $this->lang->line('zoom_and_clear'); ?></p></div>
                    <div ng-mouseover="UseTooltip()" id="placeholder" style="height:300px; width: 100%;"></div>                    
                </div>
            </div>
        </div>

        <div class="col-lg-3 col-md-4 col-sm-12">
            <div class="row">
                <div class="col-md-12 col-sm-6">
                    <div class="panel panel-default overall-requests animated bounceInRight">
                        <div class="panel-heading"> 
                            <div class="panel-title"><?php echo $this->lang->line('dash_overall_requests'); ?></div> 
                        </div>
                        <div class="panel-body">
                            <div class="overall-requests-container">
                                <div class="request-row online-requests clearfix"><span class="pull-left"><i class="fa fa-commenting-o" aria-hidden="true"></i> <?php echo $this->lang->line('dash_online_requests'); ?></span> <span class="pull-right">{{online_requests.total}}</span></div>
                                <div class="request-row offline-requests clearfix"><span class="pull-left"><i class="fa fa-comment-o" aria-hidden="true"></i> <?php echo $this->lang->line('dash_offline_requests'); ?></span> <span class="pull-right">{{offline_requests.total}}</span></div>
                            </div>
                        </div>

                        <div class="panel-footer text-center hidden-sm hidden-xs">
                            <a class="btn btn-link" href="<?php echo site_url('d=agents&c=agents'); ?>" target="_new"><i class="fa fa-external-link" aria-hidden="true"></i> <?php echo $this->lang->line('dash_launch_chat_panel'); ?></a>
                        </div>
                    </div>
                </div>

                <div class="col-md-12 col-sm-6">
                    <div class="panel panel-default most-rated-operators animated bounceInRight">
                        <div class="panel-heading"> 
                            <div class="panel-title"><?php echo $this->lang->line('dash_most_rated_operators'); ?></div> 
                        </div>
                        <div class="panel-body text-center">
                            <ul ng-if="most_rated_operators.length" class="list-inline operators-inline-list">
                                <li ng-repeat="operator in most_rated_operators" class="animated bounceInDown">
                                    <span class="user-avatar-block">
                                        <img ng-if="operator.profile_pic" class="img-circle avatar" ng-src="{{operator.profile_picture}}" alt="{{operator.name}}" tooltip-append-to-body="true" uib-tooltip="{{operator.name}}">
                                        <span ng-if="!operator.profile_pic" ng-style="{'background-color':operator.profile_color}" class="user-avatar" tooltip-append-to-body="true" uib-tooltip="{{operator.name}}">{{operator.name|oneCapLetter}}</span>
                                    </span>

                                    <div class="rating-number">
                                        <span>{{(operator.total_rating / operator.total_record)|math_round}} <i class="fa fa-star" aria-hidden="true"></i></span>
                                    </div>
                                </li>
                            </ul>
                            <p ng-if="!most_rated_operators.length" class="text-center no-result"><?php echo $this->lang->line('dash_no_record_found'); ?></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-md-4 col-sm-12">
            <div class="panel panel-primary operators-and-requests panel-operators animated bounceInRight">
                <div class="panel-heading"> 
                    <div class="panel-title"><?php echo $this->lang->line('dash_operators_and_requests'); ?></div> 
                </div>
                <div class="panel-body">
                    <div ng-show="operators_n_requests.length" class="panel-body-inner mCustomScrollbar">
                        <ul class="user-list operator-list list-unstyled">
                            <li ng-repeat="operator in operators_n_requests" class="clearfix animated bounceInRight">
                                <span class="chat-counter pull-right">{{operator.total_chat}}</span>

                                <span class="pull-left">
                                    <span class="user-avatar-block">
                                        <img ng-if="operator.profile_pic" class="img-circle avatar" ng-src="{{operator.profile_picture}}" alt="{{operator.name}}" title="{{operator.name}}">
                                        <span ng-if="!operator.profile_pic" ng-style="{'background-color':operator.profile_color}" class="user-avatar" title="{{operator.name}}">{{operator.name|oneCapLetter}}</span>
                                    </span>
                                    <span class="operator-info">
                                        <span class="user-name">{{operator.name}}</span>
                                    </span>
                                </span>
                            </li>
                        </ul>
                    </div>
                    
                    <div ng-hide="operators_n_requests.length" class="panel-body-inner">
                        <p class="text-center no-result"><?php echo $this->lang->line('dash_no_record_found'); ?></p>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-4 col-sm-6">
            <div class="panel panel-primary panel-operators animated fadeIn">
                <div class="panel-heading"> 
                    <div class="panel-title"><?php echo $this->lang->line('dash_most_visited_pages'); ?></div> 
                </div>
                <div class="panel-body">
                    <div ng-show="pageviews.length" class="panel-body-inner mCustomScrollbar">
                        <div class="table-responsive">
                            <table class="table layout-fixed user-table">
                                <thead>
                                    <tr>
                                        <th><?php echo $this->lang->line('title'); ?></th>
                                        <th class="text-center" width="72"><?php echo $this->lang->line('visits'); ?></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr ng-repeat="row in pageviews">
                                        <td><a ng-href="{{row.page_url}}" target="_blank">{{row.page_title||'<?php echo $this->lang->line('no_page_title'); ?>'}}</a></td>
                                        <td class="text-center">{{row.total}}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    
                    <div ng-hide="pageviews.length" class="panel-body-inner">
                        <p class="text-center no-result"><?php echo $this->lang->line('dash_no_record_found'); ?></p>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-4 col-sm-6">
            <div class="panel panel-primary recent-chats panel-operators animated bounceInLeft">
                <div class="panel-heading"> 
                    <div class="panel-title"><?php echo $this->lang->line('dash_recent_chats'); ?></div> 
                </div>
                <div class="panel-body">
                    <div ng-show="recent_chats.length" class="panel-body-inner mCustomScrollbar">
                        <ul class="user-list recent-chats-list list-unstyled">
                            <li ng-repeat="chat in recent_chats" class="animated bounceInLeft">
                                <span class="user-avatar-block">
                                    <img ng-if="chat.profile_pic" class="img-circle avatar" ng-src="{{chat.profile_picture}}" alt="{{chat.name}}" title="{{chat.name}}">
                                    <span ng-if="!chat.profile_pic" ng-style="{'background-color':chat.profile_color}" class="user-avatar" title="{{chat.name}}">{{chat.name|oneCapLetter}}</span>
                                </span>
                                <span class="operator-info profile-info">
                                    <span class="user-name">{{chat.name}}</span>
                                    <span ng-if="chat.city" class="user-location">{{chat.city}}, {{chat.state}}, {{chat.country}}</span>
                                </span>
                            </li>
                        </ul>
                    </div>
                    
                    <div ng-hide="recent_chats.length" class="panel-body-inner">
                        <p class="text-center no-result"><?php echo $this->lang->line('dash_no_record_found'); ?></p>
                    </div>
                </div>
            </div>
        </div>
    </div>	

    <div class="row">
        <div class="col-sm-12">
            <div class="panel panel-primary">
                <div class="panel-heading"> 
                    <div class="panel-title">
                        <i class="fa fa-map-marker"></i>
                        <?php echo $this->lang->line('visitors_map'); ?>
                        <div class="pull-right">
                            <div class="dropdown">
                                <span class="filter-by text-white"><?php echo $this->lang->line('dash_filter_by'); ?></span>
                                <a class="select-box select-box-filter dropdown-toggle text-capitalize text-white" type="button" id="dropdown-visitors" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                                    {{filter_by|str_human}}
                                    <i class="fa fa-angle-down fa-lg" aria-hidden="true"></i>
                                </a>
                                <ul class="dropdown-menu dropdown-menu-right" aria-labelledby="dropdown-visitors">
                                    <li ng-class="{active:filter_by == 'all'}"><a href="#" ng-click="filter_map($event, 'all')"><?php echo $this->lang->line('dash_all'); ?></a></li>
                                    <li ng-class="{active:filter_by == 'attended_visitors'}"><a href="#" ng-click="filter_map($event, 'attended_visitors')"><?php echo $this->lang->line('dash_attended_visitors'); ?></a></li>
                                    <li ng-class="{active:filter_by == 'online_visitors'}"><a href="#" ng-click="filter_map($event, 'online_visitors')"><?php echo $this->lang->line('dash_online_visitors'); ?></a></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="map-container">
                    <?php if ($this->settings->google_map_api_key): ?>
                        <div id="usersMap" class="maps embed-responsive embed-responsive-16by9" style="padding-bottom: 30%;"></div>
                    <?php else: ?>
                        <div class="alert alert-info">
                            <div><?php echo sprintf($this->lang->line('google_map_api_key_missing'), site_url('c=settings')); ?></div>            
                        </div>
                    <?php endif; ?>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12"><?php powered_by(); ?></div>
    </div>
</div>