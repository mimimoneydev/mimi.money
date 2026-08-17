<div class="login-section">
    <div class="login-container">
        <div class="" style="margin: 0 auto; width: 60%; padding: 2% 0;">
            <div class="login-header">
                <h2><?php echo $this->data['pagetitle']; ?></h2>
            </div>
            <div class="">
                <p><?php echo $this->lang->line('intalled_suucess'); ?></p>
                <div><?php echo $messages;?></div>
                <p><a href="<?php echo site_url('admin');?>" target="_blank">Click here</a> to login to Admin section. </p>
            </div>
        </div>
    </div>
</div>