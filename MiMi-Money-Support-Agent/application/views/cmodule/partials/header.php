<nav class="navbar navbar-default header-top">
     <div class="container">
          <div class="clearfix">
               <div class="pull-left">
                    <div class="navbar-header">
                         <a class="navbar-brand" href="<?php echo site_url(); ?>" title="{{settings.admin_panel_name}}">
                              <img ng-src="{{settings.admin_panel_logo}}" alt="{{settings.admin_panel_name}}" title="{{settings.admin_panel_name}}">
                         </a>
                    </div>
               </div>

               <div class="pull-right">
                    <ul class="user-info pull-left pull-none-xsm nav navbar-nav navbar-right">
                         <li class="profile-info dropdown">
                              <a data-toggle="dropdown" class="dropdown-toggle animated bounceInDown" href="<?php echo site_url('c=users&m=edit_profile'); ?>">
                                   <img ng-show="user.profile_pic" class="img-circle avatar" ng-src="{{user.profile_picture}}" alt="Profile Picture" title="" />
                                   <span ng-hide="user.profile_pic" ng-style="{backgroundColor: user.profile_color}" class="user-avatar" title="{{user.name}}">{{user.name|oneCapLetter}}</span>
                              </a>
                              <ul class="dropdown-menu">
                                   <li><a href="<?php echo site_url('c=users&m=edit_profile'); ?>"><?php echo $this->lang->line('edit_profile'); ?></a></li>
                                   <li><a href="<?php echo site_url('c=users&m=change_password'); ?>"><?php echo $this->lang->line('change_password'); ?></a></li>
                                   <li><a href="<?php echo site_url('c=admin&m=logout'); ?>"><?php echo $this->lang->line('logout'); ?></a></li>
                              </ul>
                         </li>
                    </ul>
               </div>
          </div>
     </div>
</nav>
