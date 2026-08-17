<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

class Media {

    var $file = '';

    /*
     * construct function load image and image upload library
     */

    function __construct() {
        $this->ci = & get_instance();
        $this->ci->load->library('upload');
        $this->ci->load->library('image_lib');
    }

    /*
     * To upload file from BAse64 String
     * 
     * @param {String} $upload_path
     * @param {String} $filePrefix
     * @param {String} $base64WithDataArray
     * 
     * @return return file url
     */

    function upload_base64_file($upload_path, $file_prefix, $base64_array, $thumbnail_config = array()) {
        $result = array();
        
        //decode base64 string
        $upload_file = base64_decode($base64_array['base64']);
        $filename = $file_prefix . preg_replace("/[^a-zA-Z0-9-_.]+/", "", $base64_array['filename']);
        $result['filename'] = $filename;
        
        if (!is_dir($upload_path)) {
            mkdir($upload_path, 0777, TRUE);
        }
        
        $uploaded = file_put_contents($upload_path . $filename, $upload_file);
        $result['uploaded'] = $uploaded;
        
        if($uploaded) {
            if (isset($thumbnail_config['create_thumb']) and $thumbnail_config['create_thumb']) {
                $thumbnail_config['source_image'] = $upload_path . $filename;
                $thumbnail_config['new_image'] = $upload_path . '/thumb/' . $filename;
                if (!isset($thumbnail_config['thumb_marker'])) {
                    $thumbnail_config['thumb_marker'] = false;
                }
                
                $this->create_thumb($thumbnail_config);
            }
        }
        
        return $result;
    }

    /*
     * This function upload file on server
     * 
     * @param $file (name of file element)
     * @param $file_config (upload path, types and file_name etc.)
     * @param $thumbnail_config ( thumbnail settings array)
     * 
     * @return $output (array of file upload class output)
     */

    function upload_media($file, $file_config = array(), $thumbnail_config = array()) {
        $output = array();
        $this->ci->upload->initialize($file_config);

        if (!$this->ci->upload->do_upload($file)) {
            $output['error'] = $this->ci->upload->display_errors();
        } else {
            $output = $this->ci->upload->data();

            if (isset($thumbnail_config['create_thumb']) and $thumbnail_config['create_thumb']) {
                $thumbnail_config['source_image'] = $output['full_path'];
                $thumbnail_config['new_image'] = $output['file_path'] . '/thumb/' . $output['file_name'];
                if (!isset($thumbnail_config['thumb_marker']))
                    $thumbnail_config['thumb_marker'] = false;
                $this->create_thumb($thumbnail_config);
            }
        }

        return $output;
    }

    /*
     * This function create a thumb version of an image.
     * 
     * @param $config
     */

    function create_thumb($config = array()) {
        $this->ci->image_lib->initialize($config);
        $this->ci->image_lib->resize();
        $this->ci->image_lib->clear();
    }

    /*
     * This function return thumbnail user of uploaded image
     * 
     * @param $filename
     * @param $filepath
     * @return image thumbnail url
     */

    function get_thumbnail($filename, $filepath, $email = '', $d = '') {
        if($filename) return base_url(UPLOAD_DIR . $filepath . 'thumb/' . $filename);
        
        return '';
    }
    
    /*
     * This function return thumbnail user of uploaded image
     * 
     * @param $filename
     * @param $filepath
     * @return image thumbnail url
     */

    function get_image($filename, $filepath, $email = '', $d = '') {
        if($filename) return base_url(UPLOAD_DIR . $filepath . $filename);
        
        return '';
    }

    /*
     * This function deleted files form server permanentaly
     * 
     * @param $filename
     * @param $filepath
     */

    function delete_media($filename, $filepath) {
        if ($filename and file_exists(upload_dir($filepath.$filename))) {
            unlink(upload_dir($filepath.$filename));
            
            $thumbnail = $filepath . 'thumb/' .$filename;
            if (file_exists(upload_dir($thumbnail))) {
                unlink(upload_dir($thumbnail));
            }
        }
    }

}
