<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><?php echo $site_name; ?></title>
<link href="https://fonts.googleapis.com/css?family=Open+Sans:300,400,600,700,800|Poppins:300,400,500,600,700" rel="stylesheet" />
</head>
<body style="font-family: 'Poppins', sans-serif; color:#383838; font-weight: 300; font-size: 14px; padding:0; margin:0; background-color:#fff;">
    <div style="width: 640px; min-height: 500px; margin: 0 auto; padding: 0;">
        <?php include theme_path('emails/'.$template_file.'.php'); ?>
    </div>
</body>
</html>