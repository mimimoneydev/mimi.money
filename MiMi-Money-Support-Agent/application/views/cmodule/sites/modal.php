<div id="modal_window" class="modal fade" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog">
        <div class="modal-content">
            <div ng-if="modal == 'widget'"><?php include theme_path('sites/widget.php'); ?></div>
            <div ng-if="modal == 'site_form'"><?php include theme_path('sites/site_form.php'); ?></div>
        </div>
    </div>
</div>