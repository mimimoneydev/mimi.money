<?php

if (!defined('BASEPATH'))
    exit('No direct script access allowed');

class CP_Config extends CI_Config {

    var $config_path = ''; // Set in the constructor below
    var $database_path = ''; // Set in the constructor below
    var $autoload_path = ''; // Set in the constructor below

    /**
     * Constructor
     *
     * @access  public
     * @return  void
     */

    function __construct() {
        parent::__construct();

        define('EXT', '.php');

        $this->config_path = APPPATH . 'config/config' . EXT;
        $this->database_path = APPPATH . 'config/database' . EXT;
        $this->autoload_path = APPPATH . 'config/autoload' . EXT;
    }

    /**
     * Update the config file
     */
    function config_update($config_array = array(), $nullable = array()) {
        if (!is_array($config_array) && count($config_array) == 0) {
            return FALSE;
        }
        
        $nullable = array_merge(array('sess_save_path'), $nullable);

        @chmod($this->config_path, FILE_WRITE_MODE);

        // Is the config file writable?
        if (!is_really_writable($this->config_path)) {
            show_error($this->config_path . ' make config file writeable.');
        }

        // Read the config file as PHP
        require $this->config_path;

        // load the file helper
        $this->CI = & get_instance();
        $this->CI->load->helper('file');

        // Read the config data as a string
        $config_file = read_file($this->config_path);

        // Trim it
        $config_file = trim($config_file);

        // Do we need to add totally new items to the config file?
        if (is_array($config_array)) {
            foreach ($config_array as $key => $val) {
                if (isset($this->config[$key]) or in_array($key, $nullable)) {
                    $pattern = '/\$config\[\\\'' . $key . '\\\'\]\s+=\s+[^\;]+/';
                    
                    if(strtolower($val) == 'true' or strtolower($val) == 'false' or strtolower($val) == 'null' or is_numeric($val)) {
                        $replace = "\$config['$key'] = ". $val;
                    } else {
                        $replace = "\$config['$key'] = '$val'";
                    }
                    
                    $config_file = preg_replace($pattern, $replace, $config_file);
                } else {
                    if(strtolower($val) == 'true' or strtolower($val) == 'false' or strtolower($val) == 'null' or is_numeric($val)) {
                        $config_file .= "\n\$config['$key'] = $val;";
                    } else {
                        $config_file .= "\n\$config['$key'] = '$val';";
                    }
                }
            }
        }

        if (!$fp = fopen($this->config_path, FOPEN_WRITE_CREATE_DESTRUCTIVE)) {
            return FALSE;
        }

        flock($fp, LOCK_EX);
        fwrite($fp, $config_file, strlen($config_file));
        flock($fp, LOCK_UN);
        fclose($fp);

        @chmod($this->config_path, FILE_READ_MODE);

        return TRUE;
    }

    // --------------------------------------------------------------------

    /**
     * Update Database Config File
     */
    function db_config_update($dbconfig = array()) {
        @chmod($this->database_path, FILE_WRITE_MODE);

        // Is the database file writable?
        if (!is_really_writable($this->database_path)) {
            show_error($this->database_path . ' make database.php file writeable.');
        }

        // load the file helper
        $this->CI = & get_instance();
        $this->CI->load->helper('file');

        // Read the config file as PHP
        require $this->database_path;

        // Now we read the file data as a string
        $config_file = read_file($this->database_path);

        if (count($dbconfig) > 0) {
            foreach ($dbconfig as $key => $val) {
                $pattern = '/\$db\[\\\'' . $active_group . '\\\'\]\[\\\'' . $key . '\\\'\]\s+=\s+[^\;]+/';
                $replace = "\$db['$active_group']['$key'] = '$val'";

                $config_file = preg_replace($pattern, $replace, $config_file);
            }
        }

        $config_file = trim($config_file);

        // Write the file
        if (!$fp = fopen($this->database_path, FOPEN_WRITE_CREATE_DESTRUCTIVE)) {
            return FALSE;
        }

        flock($fp, LOCK_EX);
        fwrite($fp, $config_file, strlen($config_file));
        flock($fp, LOCK_UN);
        fclose($fp);

        @chmod($this->database_path, FILE_READ_MODE);

        return TRUE;
    }

    // --------------------------------------------------------------------

    /**
     * Update autoload Config File
     */
    function autoload_update($config_array = array()) {
        if (!is_array($config_array) && count($config_array) == 0) {
            return FALSE;
        }

        @chmod($this->autoload_path, FILE_WRITE_MODE);

        // Is the config file writable?
        if (!is_really_writable($this->autoload_path)) {
            show_error($this->autoload_path . ' make config file writeable.');
        }

        // Read the config file as PHP
        require $this->autoload_path;

        // load the file helper
        $this->CI = & get_instance();
        $this->CI->load->helper('file');

        // Read the config data as a string
        $config_file = read_file($this->autoload_path);

        // Trim it
        $config_file = trim($config_file);

        // Do we need to add totally new items to the config file?
        if (is_array($config_array)) {
            foreach ($config_array as $key => $val) {
                $pattern = '/\$autoload\[\\\'' . $key . '\\\'\]\s+=\s+[^\;]+/';
                $replace = "\$autoload['$key'] = array($val)";
                $config_file = preg_replace($pattern, $replace, $config_file);
            }
        }

        if (!$fp = fopen($this->autoload_path, FOPEN_WRITE_CREATE_DESTRUCTIVE)) {
            return FALSE;
        }

        flock($fp, LOCK_EX);
        fwrite($fp, $config_file, strlen($config_file));
        flock($fp, LOCK_UN);
        fclose($fp);

        @chmod($this->autoload_path, FILE_READ_MODE);

        return TRUE;
    }

}
