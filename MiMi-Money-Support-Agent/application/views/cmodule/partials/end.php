<?php echo $this->files['css_footer'];?>
<script src="<?php echo theme_url("js/angularjs/angular.min.js"); ?>"></script>
<script src="<?php echo theme_url("js/angularjs/angular-sanitize.min.js"); ?>"></script>
<script src="<?php echo theme_url("js/angularjs/angular-animate.min.js"); ?>"></script>
<script src="<?php echo base_url("assets/angular-bootstrap-colorpicker-master/js/bootstrap-colorpicker-module.min.js"); ?>"></script>
<script src="<?php echo base_url("assets/checklist-model/checklist-model.js"); ?>"></script>
<script src="<?php echo base_url("assets/angular-bootstrap/ui-bootstrap-tpls.min.js"); ?>"></script>
<script src="<?php echo base_url("assets/angular-smilies/dist/angular-smilies.js"); ?>"></script>
<script src="<?php echo base_url("assets/angular-base64-upload/dist/angular-base64-upload.min.js"); ?>"></script>
<script src="<?php echo base_url("assets/angular-canned-messages/angular-canned-messages.js"); ?>"></script>
<script src="<?php echo theme_url("js/app/filters.js"); ?>"></script>
<script src="<?php echo theme_url("js/app/directives.js"); ?>"></script>
<script src="<?php echo theme_url("js/app/app.js"); ?>"></script>
<script src="<?php echo theme_url("js/app/services/layout.js"); ?>"></script>
<?php echo $this->files['js_footer'];?>
<style ng-bind-html="custom_styles"></style>