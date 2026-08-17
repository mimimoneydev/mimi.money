<?php if ($this->session->flashdata('success')): ?>
    <div class="alert alert-success">
        <a href="#" class="close" aria-label="close" data-dismiss="alert">&times;</a>
        <div><strong> CONFIRMATION : </strong> <?php echo $this->session->flashdata('success'); ?></div>
    </div>
<?php elseif ($this->session->flashdata('error')) : ?>
    <div class="alert alert-danger">
        <a href="#" class="close" aria-label="close" data-dismiss="alert">&times;</a>
        <div><strong> ERROR : </strong> <?php echo $this->session->flashdata('error'); ?></div>
    </div>
<?php endif; ?>

<?php if ((!isset($this->settings->licence_key) || $this->settings->licence_key == '') and $this->current_user): ?>
    <div ng-if="settings.plugin_validated == 'no'" class="alert alert-danger">
        <div><?php echo sprintf($this->lang->line('licence_key_missing'), site_url('c=settings')); ?></div>
    </div>
<?php endif; ?>
