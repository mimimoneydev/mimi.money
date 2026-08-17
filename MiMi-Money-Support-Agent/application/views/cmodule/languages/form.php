<div id="formblock" class="modal fade" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog">
        <div class="modal-content">
            <div ng-if="formSection == 'lang_form'"><?php include theme_path('languages/lang_form.php'); ?></div>
            <div ng-if="formSection == 'lang_files'"><?php include theme_path('languages/lang_files.php'); ?></div>
            <div ng-if="formSection == 'translate_form'"><?php include theme_path('languages/translate_form.php'); ?></div>
        </div>
    </div>
</div>