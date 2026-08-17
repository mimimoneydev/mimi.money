<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

/*
 * This is function will be call to get body classes
 * 
 * @return string of body classes
 */

function body_classes() {
    $CI = & get_instance();
    $body_classes = implode(" ", $CI->body_classes);

    return $body_classes;
}

/*
 * This is function will call to change password md5 format
 * 
 * $param $password
 * @return $password (md5 format)
 */

function hashpassword($password) {
    return md5($password);
}

/*
 * This function check current class and methos to add active status.
 * 
 * @param $class_name (Name of controller class)
 * @param $method_name (Name of Class method)
 * 
 * @return empty or actve (status)
 */

function is_active($class_name, $method_name = '') {
    $CI = & get_instance();

    $status = '';
    if ($CI->router->class == $class_name) {
        if ($method_name) {
            if ($method_name == $CI->router->method) {
                $status = 'active';
            }
        } else {
            $status = 'active';
        }
    }

    return $status;
}

/*
 * This function converts string to uniqe hash value
 * 
 * @param $strval
 * @return $hashed_val
 */

function convert_to_hash($strval) {
    $value = unpack('H*', $strval);
    $hashed_val = base_convert($value[1], 16, 2);

    return $hashed_val;
}

/*
 * this function reverse hash value to normal value
 * 
 * @param $hashed_val
 * @return $strval
 */

function convert_to_string($hashed_val) {
    $strval = pack('H*', base_convert($hashed_val, 2, 16));

    return $strval;
}

/*
 * This function will be use to get user location by his ip address
 * 
 * @param $ip_address
 * @return object of geo location
 */

function getLocationByIp($ip_address) {
    $location_url = "http://www.geoplugin.net/json.gp?ip=" . $ip_address;

    $CI = & get_instance();
    $CI->curl->create($location_url);
    $CI->curl->option(USERAGENT, $CI->input->user_agent());
    $CI->curl->option(RETURNTRANSFER, 1);
    $response = $CI->curl->execute();
    $loaction = json_decode($response);

    //and $loaction->geoplugin_status == 200
    if ($loaction and isset($loaction->geoplugin_status) and in_array($loaction->geoplugin_status, array(200, 201, 206))) {
        return $loaction;
    }

    return FALSE;
}

/*
 * This function will return all http header and values
 */
if (!function_exists('getallheaderst')) {

    function getallheaderst() {
        $headers = '';
        foreach ($_SERVER as $name => $value) {
            if (substr($name, 0, 5) == 'HTTP_') {
                $headers[str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))))] = $value;
            }
        }
        return $headers;
    }

}

/*
 * This function extract zip files
 * 
 * @param (path string) $fileurl
 * @param (path string) $extractPath
 */
if (!function_exists('extractZip')) {

    function extractZip($fileurl, $extractPath) {
        $CI = & get_instance();
        
        $output = array('result' => 'failed', 'errors' => '', 'message' => '');
        if (class_exists('ZipArchive')) {
            if (file_exists($fileurl)) {
                /* Open the Zip file */
                $zip = new ZipArchive;
                if ($zip->open($fileurl) != "true") {
                    $output['errors'] = $CI->lang->line('extracting_error');
                } else {
                    /* Extract Zip File */
                    $zip->extractTo($extractPath);
                    $zip->close();
                }

                $output['result'] = 'success';
                $output['message'] = $CI->lang->line('file_extracted');
            } else {
                $output['errors'] = $CI->lang->line('file_not_found');
            }
        } else {
            $output['errors'] = 'Class \'ZipArchive\' not found. PHP needs to have the <a href="http://www.php.net/manual/en/zip.installation.php">zip extension</a> installed';
        }

        return $output;
    }

}

/*
 * Getting MAC Address using PHP
 * Md. Nazmul Basher
 */
if (!function_exists('getMacAddress')) {

    function getMacAddress() {
        ob_start(); // Turn on output buffering
        system('ipconfig / all'); //Execute external program to display output
        $mycom = ob_get_contents(); // Capture the output into a variable
        ob_clean(); // Clean (erase) the output buffer

        $findme = "Physical";
        $pmac = strpos($mycom, $findme); // Find the position of Physical text
        $mac = substr($mycom, ($pmac + 36), 17); // Get Physical Address

        return $mycom;
    }

}

/*
 * To show plugin info.
 */
if (!function_exists('version_info')) {

    function version_info() {
        $CI = & get_instance();
        
        // change chatbull brand name.
        $pname = ucfirst(str_replace('chatbull', 'ChatBull', PRODUCT_NAME));

        // remove dash from name
        $pname = str_replace('-', ' ', $pname);

        echo '<p class="text-capitalize">' . $CI->lang->line('current_version') . ' ' . $pname . ' ' . CHATBULL_VERSION . '</p>';
    }

}

/*
 * To show powered by info.
 */
if (!function_exists('powered_by')) {

    function powered_by() {
        $CI = & get_instance();
        
        // change chatbull brand name.
        $pname = ucfirst(str_replace('chatbull', 'ChatBull', PRODUCT_NAME));

        // remove dash from name
        $pname = str_replace('-', ' ', $pname);

        echo '<p class="text-capitalize">' . $CI->lang->line('powered_by') . ' <a href="' . POWERED_BY_URL . '" target="_new">' . POWERED_BY . '</a> <span class="text-capitalize pull-right">' . $CI->lang->line('current_version') . ' <a href="' . CHATBULL_SITEURL . '" target="_new">' . $pname . ' ' . CHATBULL_VERSION . '</a></span></p>';
    }

}

/*
 * To show powered by info.
 */
if (!function_exists('plugin_name')) {

    function plugin_name() {
        // change chatbull brand name.
        $pname = ucfirst(str_replace('chatbull', 'ChatBull', PRODUCT_NAME));

        // remove dash from name
        $pname = str_replace('-', ' ', $pname);

        return $pname;
    }

}

/*
 * Change size unit in a format
 * 
 * @param Unit $bytes
 * @return formated $bytes
 */
if (!function_exists('formatSizeUnits')) {

    function formatSizeUnits($bytes) {
        if ($bytes >= 1073741824) {
            $bytes = number_format($bytes / 1073741824, 2) . ' GB';
        } elseif ($bytes >= 1048576) {
            $bytes = number_format($bytes / 1048576, 2) . ' MB';
        } elseif ($bytes >= 1024) {
            $bytes = number_format($bytes / 1024, 2) . ' KB';
        } elseif ($bytes > 1) {
            $bytes = $bytes . ' bytes';
        } elseif ($bytes == 1) {
            $bytes = $bytes . ' byte';
        } else {
            $bytes = '0 bytes';
        }

        return $bytes;
    }

}

/* 
 * Convert hexdec color string to rgb(a) string 
 * 
 * @param Color Code $color
 * @param Int $opacity
 * 
 * @return RGBS CODE $output
 */
 
function hex2rgba($color, $opacity = false) {
 
	$default = 'rgb(0,0,0)';
 
	//Return default if no color provided
	if(empty($color))
          return $default; 
 
	//Sanitize $color if "#" is provided 
        if ($color[0] == '#' ) {
        	$color = substr( $color, 1 );
        }
 
        //Check if color has 6 or 3 characters and get values
        if (strlen($color) == 6) {
                $hex = array( $color[0] . $color[1], $color[2] . $color[3], $color[4] . $color[5] );
        } elseif ( strlen( $color ) == 3 ) {
                $hex = array( $color[0] . $color[0], $color[1] . $color[1], $color[2] . $color[2] );
        } else {
                return $default;
        }
 
        //Convert hexadec to rgb
        $rgb =  array_map('hexdec', $hex);
 
        //Check if opacity is set(rgba or rgb)
        if($opacity){
        	if(abs($opacity) > 1)
        		$opacity = 1.0;
        	$output = 'rgba('.implode(",",$rgb).','.$opacity.')';
        } else {
        	$output = 'rgb('.implode(",",$rgb).')';
        }
 
        //Return rgb(a) color string
        return $output;
}
