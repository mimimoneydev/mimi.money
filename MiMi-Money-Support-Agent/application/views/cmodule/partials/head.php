<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title><?php echo $this->data['pagetitle']; ?> <?php echo $this->data['title']; ?></title>
<meta name="description" content="Chatbull - Live Chat Support Plugin">
<meta name="keywords" content="live chat, livechat, wp live chat, php chat, php live chat, live chat support, plugin, support, customers, visitors, potential customers, website, admin, agents, multiple platform, mobile application, desktop application, web application, one time investment, convert visitors to sales, monetize website, communicate, increase revenue, increase sale">

<link rel="stylesheet" type="text/css" href="<?php echo theme_url('css/font-awesome.min.css'); ?>" media="all" />
<link rel="stylesheet" type="text/css" href="<?php echo theme_url('css/bootstrap.min.css'); ?>" media="all" />
<link rel="stylesheet" type="text/css" href="<?php echo base_url('assets/animate.css/animate.min.css'); ?>" media="all" />
<link rel="stylesheet" type="text/css" href="<?php echo base_url('assets/angular-bootstrap-colorpicker-master/css/colorpicker.min.css'); ?>" />
<link rel="stylesheet" type="text/css" href="<?php echo base_url('assets/scrollbar-plugin/css/jquery.mCustomScrollbar.css'); ?>" />
<link rel="stylesheet" type="text/css" href="<?php echo base_url('assets/angular-smilies/dist/angular-smilies-embed.min.css'); ?>" />
<link rel="stylesheet" type="text/css" href="<?php echo base_url('assets/angular-canned-messages/angular-canned-messages.css'); ?>" />
<link rel="stylesheet" type="text/css" href="<?php echo theme_url('css/cmodule-core.css'); ?>" />
<?php echo $this->files['css_header'];?>

<!--[if lt IE 9]>
      <script src="<?php echo theme_url('js/html5shiv.min.js'); ?>"></script>
      <script src="<?php echo theme_url('js/respond.min.js'); ?>"></script>
<![endif]-->

<script type="text/javascript">
    var site_url = '<?php echo site_url(); ?>';
    var theme_url = '<?php echo theme_url(); ?>';
    var theme_name = '<?php echo $this->data['theme']; ?>';
    <?php $projectname = strstr($_SERVER['REQUEST_URI'], '/index.php', true);?>
    var project_name = '<?php echo $projectname; ?>';
    var cbsiteurl = '<?php echo CHATBULL_SITEURL; ?>';
    var product_name = '<?php echo PRODUCT_NAME; ?>';
    var lang_lables = <?php echo json_encode($this->data['lang_lables']); ?>;
</script>

<script type="text/javascript" src="<?php echo theme_url('js/jquery.js'); ?>"></script>
<script type="text/javascript" src="<?php echo theme_url('js/bootstrap.min.js'); ?>"></script>
<script type="text/javascript" src="<?php echo base_url('assets/scrollbar-plugin/js/jquery.mCustomScrollbar.concat.min.js'); ?>"></script>
<script type="text/javascript" src="<?php echo theme_url('js/app/scrollbar-function.js'); ?>"></script>
<?php echo $this->files['js_header'];?>

<link rel="apple-touch-icon" sizes="57x57" href="<?php echo theme_url('icon/apple-touch-icon-57x57.png'); ?>">
<link rel="apple-touch-icon" sizes="60x60" href="<?php echo theme_url('icon/apple-touch-icon-60x60.png'); ?>">
<link rel="apple-touch-icon" sizes="72x72" href="<?php echo theme_url('icon/apple-touch-icon-72x72.png'); ?>">
<link rel="apple-touch-icon" sizes="76x76" href="<?php echo theme_url('icon/apple-touch-icon-76x76.png'); ?>">
<link rel="apple-touch-icon" sizes="114x114" href="<?php echo theme_url('icon/apple-touch-icon-114x114.png'); ?>">
<link rel="apple-touch-icon" sizes="120x120" href="<?php echo theme_url('icon/apple-touch-icon-120x120.png'); ?>">
<link rel="apple-touch-icon" sizes="144x144" href="<?php echo theme_url('icon/apple-touch-icon-144x144.png'); ?>">
<link rel="apple-touch-icon" sizes="152x152" href="<?php echo theme_url('icon/apple-touch-icon-152x152.png'); ?>">
<link rel="apple-touch-icon" sizes="180x180" href="<?php echo theme_url('icon/apple-icon-180x180.png'); ?>">

<link rel="icon" type="image/png" href="<?php echo theme_url('icon/android-icon-192x192.png'); ?>" sizes="192x192">
<link rel="icon" type="image/png" href="<?php echo theme_url('icon/favicon-32x32.png'); ?>" sizes="32x32">
<link rel="icon" type="image/png" href="<?php echo theme_url('icon/favicon-96x96.png'); ?>" sizes="96x96">
<link rel="icon" type="image/png" href="<?php echo theme_url('icon/favicon-16x16.png'); ?>" sizes="16x16">
<link rel="shortcut icon" href="<?php echo theme_url('icon/favicon.ico'); ?>" type="image/x-icon">
<link rel="icon" href="<?php echo theme_url('icon/favicon.ico'); ?>" type="image/x-icon">

<link rel="manifest" href="<?php echo theme_url('icon/manifest.json'); ?>">
<meta name="msapplication-TileColor" content="#da532c">
<meta name="msapplication-TileImage" content="<?php echo theme_url('icon/mstile-144x144.png'); ?>">
<meta name="theme-color" content="#ffffff">