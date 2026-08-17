<div class="modal-header">
    <button type="button" class="close" ng-click="close_modal($event)">&times;</button>
    <h2 class="modal-title"><?php echo $this->lang->line('chatbox_code'); ?></h2>
</div>
<div class="modal-body">
    <?php
    $html_code = '<script type="text/javascript">';
    $html_code .= "var cbuser = {name: '', email: '', message: ''}";
    $html_code .= ", access_token = '{{record.token}}'";
    $html_code .= ", cburl = '" . preg_replace('#^https?:#', '', base_url()) . "';";
    $html_code .= "document.write('<script type=\"text/javascript\" src=\"' + cburl + 'assets/cmodule-chat/js/chatbull-init.js\"></' + 'script>');";
    $html_code .= '</script>';
    ?>
    <div>
        <div class="sourse-code" select-on-click><?php highlight_string($html_code); ?></div>
    </div>    
</div>
<div class="modal-footer"></div>