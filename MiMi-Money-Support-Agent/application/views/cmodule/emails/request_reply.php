<table cellpadding="0" cellspacing="0" border="0" width="100%" align="center">
    <tbody>
        <tr>
            <td>
                <table width="100%" bgcolor="#324ee1" cellpadding="0" cellspacing="0" border="0" style="color: #fff; text-align: center; padding: 45px 20px 27px;">
                    <tr>
                        <td>
                            <img src="<?php echo $site_logo; ?>" alt="<?php echo $site_name; ?>" title="<?php echo $site_name; ?>" />
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        
        <tr>
            <td>
                <table width="100%" bgcolor="#f1f1f1" cellpadding="0" cellspacing="0" border="0" style="font-size: 14px; color:#6e6e6e; padding: 35px 45px; margin-bottom: 15px; border-radius: 4px; ">
                    <tbody>
                        <tr>
                            <td><p style=" color:#383838; font-size: 24px; margin-top: 0; margin-bottom: 10px;"><?php echo $this->lang->line('hello') . ' ' . $name; ?>,</p></td>
                        </tr>
                        <tr>
                            <td><p style="margin-top: 0; margin-bottom: 20px;"><?php echo $sender->name; ?> is replied on request.</p></td>
                        </tr>
                        <tr>
                            <td><p style="margin-top: 0; margin-bottom: 20px;"><?php echo $message; ?></p></td>
                        </tr>
                        <tr>
                            <td><p style="margin-top: 0; margin-bottom: 10px;">To reply please click <a href="<?php echo $link; ?>">here</a>.</p></td>
                        </tr>
                    </tbody>
                </table>
            </td>
        </tr>  
    </tbody>
</table>