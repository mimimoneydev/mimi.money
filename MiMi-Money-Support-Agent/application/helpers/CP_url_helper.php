<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

/*
 * This function will return theme raltive path
 */

function theme_path($file_path) {
    $CI = & get_instance();
    return VIEWPATH . $CI->data['theme'] . '/' . $file_path;
}

/*
 * This function will load file from themes
 */

function theme_url($file_path = '') {
    $CI = & get_instance();
    return base_url('assets/' . $CI->data['theme'] . '/' . $file_path);
}

/*
 * This function will return upload dir path
 */

function upload_dir($imgpath) {
    $CI = & get_instance();
    return FCPATH . UPLOAD_DIR . $imgpath;
}

/*
 * This function will return uploaded file url
 */

function upload_url($file_path) {
    $CI = & get_instance();
    return base_url(UPLOAD_DIR . $file_path);
}

/*
 * Set file link with icon
 * 
 * @param String $fileurl
 * @param String $filetype
 * @param Boolean Default true
 * 
 * @return Html $link_html
 */

function file_link($fileurl, $size, $filename = '', $show_icon = true) {
    $link_html = '';
    $icon_html = '';

    if ($fileurl) {
        if (empty($filename)) {
            $filename = basename($fileurl);
        }
        $extension = strtolower(substr(strrchr($filename, '.'), 1));

        switch ($extension) {
            case 'jpeg':
            case 'jpg':
            case 'jpe':
            case 'png':
            case 'gif': $icon_html = '<img src="' . $fileurl . '" alt="' . $filename . '" />';
                break;

            case 'pdf':
                $icon_url = base_url('assets/tile-icons/pdf-icon.png');
                $icon_html = '<img src="' . $icon_url . '" alt="' . $filename . '" />';
                break;

            case 'txt':
                $icon_url = base_url('assets/tile-icons/txt-icon.png');
                $icon_html = '<img src="' . $icon_url . '" alt="' . $filename . '" />';
                break;

            case 'docx':
            case 'doc':
                $icon_url = base_url('assets/tile-icons/word-icon.png');
                $icon_html = '<img src="' . $icon_url . '" alt="' . $filename . '" />';
                break;

            default :
                $icon_url = base_url('assets/tile-icons/default-icon.png');
                $icon_html = '<img src="' . $fileurl . '" alt="' . $filename . '" />' ;
        }

        $link_html .= '<a class="chat-file-link" href="' . $fileurl . '" title="' . $filename . '" target="_new">';

        if ($show_icon and $icon_html) {
            $icon_html .= '<span>' . $filename. ' <br />'.  formatSizeUnits($size).'</span>';
            $link_html .= $icon_html;
        } else {
            $link_html .= $filename;
        }

        $link_html .= '</a>';
    }

    return $link_html;
}

/*
 * This function returns referer url
 */

function referer_url() {
    return $_SERVER['HTTP_REFERER'];
}

/*
 * Get domain name from url
 * 
 * @param String $url
 * 
 * @return boolean or String domain name
 */

if (!function_exists('get_domain')) {

    function get_domain($url) {
        //add http&#58;// in the event that a protocol prefix is missing from a URL.
        $url = prep_url($url);
        
        if(empty($url)) {
            return false;
        }
        
        $pieces = parse_url($url);
        $domain = isset($pieces['host']) ? $pieces['host'] : '';
        
        if (preg_match('/(?P<domain>[a-z0-9][a-z0-9\-\.]{1,63}\.[a-z\.]{2,20})$/i', $domain, $regs)) {
            return str_replace("www.", "", $regs['domain']);
        } elseif ($domain) {
            return str_replace("www.", "", $domain);
        }
        
        return false;
    }

}

/*
 * to check is same domain
 * 
 * @param String $domain
 * @param String $res_domain
 * 
 * @return boolean
 */

if (!function_exists('is_same_domain')) {

    function is_same_domain($domain, $res_domain) {
        $domain_array = explode(".", $domain);

        if ($domain == $res_domain) {
            return TRUE;
        } elseif (count($domain_array) > 2) {
            if (strstr($res_domain, $domain_array[0]) or strstr($res_domain, $domain_array[1])) {
                return TRUE;
            }
        } elseif (count($domain_array) == 2 and strstr($res_domain, $domain_array[0])) {
            return TRUE;
        }

        return false;
    }

}
