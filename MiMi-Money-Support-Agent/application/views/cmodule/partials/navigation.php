<nav class="navbar navbar-primary ">
    <div class="container">
        <div class="header-navigation">
            <div class="pull-left">
                <ul class="main-menu nav navbar-nav clearfix">
                    <li><a href="<?php echo site_url(''); ?>" class="<?php echo is_active('admin'); ?>"><i class="fa fa-dashboard"></i><span class="title"><?php echo $this->lang->line('dashboard'); ?></span></a></li>
                    <li><a href="<?php echo site_url('c=tags'); ?>" class="<?php echo is_active('tags'); ?>"><i class="fa fa-sitemap"></i><span class="title"><?php echo $this->lang->line('departments'); ?></span></a></li>
                    <li><a href="<?php echo site_url('c=users'); ?>" class="<?php echo is_active('users'); ?>"><i class="fa fa-users"></i><span class="title"><?php echo $this->lang->line('agents_and_visitors'); ?></span></a></li>
                    <li><a href="<?php echo site_url('c=sites'); ?>" class="<?php echo is_active('sites'); ?>"><i class="fa fa-cogs"></i><span class="title"><?php echo $this->lang->line('sites'); ?></span></a></li>
                    <li class="hidden-sm hidden-xs"><a href="<?php echo site_url('c=chat_history'); ?>" class="<?php echo is_active('chat_history'); ?>"><i class="fa fa-history"></i><span class="title"><?php echo $this->lang->line('chat_history'); ?></span></a></li>
                    <li class="hidden-sm hidden-xs"><a href="<?php echo site_url('c=settings'); ?>" class="<?php echo is_active('settings'); ?>"><i class="fa fa-gear"></i><span class="title"><?php echo $this->lang->line('settings'); ?></span></a></li>
                    <li class="dropdown">
                        <?php
                        $more_active = '';
                        $more_active .= ' ' . is_active('orequests');
                        $more_active .= ' ' . is_active('feedbacks');
                        $more_active .= ' ' . is_active('languages');
                        $more_active .= ' ' . is_active('canned_messages');
                        ?>
                        <a href="#more" data-toggle="dropdown" class="dropdown-toggle <?php echo $more_active; ?>"><?php echo $this->lang->line('more_menu'); ?> <span class="caret"></span></a>
                        <ul class="dropdown-menu dropdown-menu-right">
                            <li class="hidden-md hidden-lg"><a href="<?php echo site_url('c=chat_history'); ?>" class="<?php echo is_active('chat_history'); ?>"><i class="fa fa-history"></i><span class="title"><?php echo $this->lang->line('chat_history'); ?></span></a></li>
                            <li class="hidden-md hidden-lg"><a href="<?php echo site_url('c=settings'); ?>" class="<?php echo is_active('settings'); ?>"><i class="fa fa-gear"></i><span class="title"><?php echo $this->lang->line('settings'); ?></span></a></li>
                            <li><a href="<?php echo site_url('c=orequests'); ?>" class="<?php echo is_active('orequests'); ?>"><i class="fa fa-bell"></i><span class="title"><?php echo $this->lang->line('offline_requests'); ?></span></a></li>
                            <li><a href="<?php echo site_url('c=feedbacks'); ?>" class="<?php echo is_active('feedbacks'); ?>"><i class="fa fa-comment"></i><span class="title"><?php echo $this->lang->line('feedbacks'); ?></span></a></li>
                            <li><a href="<?php echo site_url('c=languages'); ?>" class="<?php echo is_active('languages'); ?>"><i class="fa fa-language"></i><span class="title"><?php echo $this->lang->line('languages'); ?></span></a></li>
                            <li><a href="<?php echo site_url('c=canned_messages'); ?>" class="<?php echo is_active('canned_messages'); ?>"><i class="fa fa-envelope"></i><span class="title"><?php echo $this->lang->line('canned_messages'); ?></span></a></li>
                            <li ng-if="settings.current_product_name != 'chatbull'" ng-init="upgrade_available()" ng-show="is_upgradable"><a href="<?php echo site_url('c=upgrade'); ?>" class="<?php echo is_active('upgrade'); ?> upgrade-link"><i class="fa fa-cogs"></i><span class="title"><?php echo $this->lang->line('upgrade'); ?></span></a></li>
                        </ul>
                    </li>
                </ul>
            </div>

            <div class="pull-right">
                <a ng-if="settings.plugin_validated == 'yes'" href="<?php echo site_url('d=agents&c=agents'); ?>" target="_blank" class="btn btn-default agent-link"><i class="fa fa-user-secret"></i><span class="title"><?php echo $this->lang->line('chat_panel'); ?></span></a>
            </div>
        </div>
    </div>
</nav>
