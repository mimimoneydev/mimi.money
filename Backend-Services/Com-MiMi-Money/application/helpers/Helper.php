<?php

/**
 * Created by Abderrahim El imame.
 * Email : abderrahim.elimame@gmail.com
 * Date: 20/02/2016
 * Time: 00:01
 */
class Helper
{

    public $_DB;

    function __construct($_DB)
    {
        $this->_DB = $_DB;
    }


    /**
     * Check the json response message
     * @param $array
     */
    public function Json($array)
    {
        ob_clean();
        header('Content-Type: application/json; charset=utf-8');
        if (is_array($array)) {
            echo json_encode($array);
        } else {
            echo $array;
        }
    }

    public function DecodeJson($array)
    {
        ob_clean();
        header('Content-Type: application/json; charset=utf-8');
        if (is_array($array)) {
            echo json_decode($array);
        } else {
            echo $array;
        }
    }

    /**
     * Function to get the date by days
     * @param $date
     * @return bool|string
     */
    public function Date($date)
    {
        //$date = strtotime($dt);

        $time_dd = date("d", $date);
        $time_MM = date("M", $date);
        $now = time();
        $c_dd = date("d", $now);
        $c_MM = date("M", $now);
        if ($time_MM == $c_MM) {
            if ($time_dd == $c_dd) {
                //days
                $newFormat = date('H:i', $date);
                return $newFormat;
            } else if ($time_dd == $c_dd - 1) {
                //yesterday
                $yesterday = 'Yesterday ';
                $newFormat = date('H:i', $date);
                return $yesterday . '' . $newFormat;
            } else if ($time_dd > $c_dd - 6 && $time_dd < $c_dd - 1) {
                //week
                $newFormat = date('l H:i', $date);
                return $newFormat;
            } else {
                //month
                $newFormat = date('D M H:i', $date);
                return $newFormat;
            }
        }
        //month
        $newFormat = date('D M Y', $date);
        return $newFormat;
    }

    /**
     * function to get a safe image
     * @param $Hash
     * @return null|string
     */
    public function getSafeImage($Hash)
    {
        $Hash = $this->_DB->escapeString($Hash);
        $query = $this->_DB->select('images', '*', "`image_hash` = '$Hash'");
        if ($this->_DB->numRows($query) != 0) {
            $fetch = $this->_DB->fetchAssoc($query);
            $path = 'uploads/imagesFiles/' . $fetch['image_path'] . '/' . $fetch['image_new_name'];
            return $path;
        } else {
            return null;
        }

    }


    /**
     * Function to uploads new images
     * @param $array
     * @param string $dir
     * @return null|string
     */
    public function uploadImage($array, $dir = './')
    {
        $valid_formats = array("jpg", "png", "gif", "bmp", "jpeg", "PNG", "JPG", "JPEG", "GIF", "BMP");
        if (!empty($array)) {
            $tmp = $array["tmp_name"];
            $name = $array["name"];
            $extension = $this->getExtension($name);
            if (in_array($extension, $valid_formats)) {
                $new_name = md5(time() . '-' . $name) .'.'. $extension;
                $day_folder = date('d-m-y', time());
                if (is_dir($dir . 'uploads/imagesFiles/' . $day_folder)) {
                    $path = $day_folder;
                } else {
                    if (mkdir($dir . 'uploads/imagesFiles/' . $day_folder)) {
                        $path = $day_folder;
                    } else {
                        $path = '';
                    }
                }
                if (move_uploaded_file($tmp, $dir . 'uploads/imagesFiles/' . $path . '/' . $new_name)) {
                    $imgHash = md5($tmp . $new_name . uniqid() . time());
                    $imageData = array(
                        'image_original_name' => $this->_DB->escapeString($name),
                        'image_new_name' => $new_name,
                        'image_path' => $path,
                        'image_hash' => $imgHash,
                        'image_type' => 0
                    );
                    $query = $this->_DB->insert('images', $imageData);
                    if ($query) {
                        return $imgHash;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Function to uploads new files audio
     * @param $array
     * @param string $dir
     * @return null|string
     */
    public
    function uploadAudio($array, $dir = './')
    {
        $valid_formats = array("mp3", "wav");
        if (!empty($array)) {
            $tmp = $array["tmp_name"];
            $name = $array["name"];

            $extension = $this->getExtension($name);
            if (in_array($extension, $valid_formats)) {
                $new_name = md5(time() . '-' . $name) .'.'. $extension;
                $day_folder = date('d-m-y', time());
                if (is_dir($dir . 'uploads/audioFiles/' . $day_folder)) {
                    $path = $day_folder;
                } else {
                    if (mkdir($dir . 'uploads/audioFiles/' . $day_folder)) {
                        $path = $day_folder;
                    } else {
                        $path = '';
                    }
                }
                if (move_uploaded_file($tmp, $dir . 'uploads/audioFiles/' . $path . '/' . $new_name)) {
                    $audioHash = md5($tmp . $new_name . uniqid() . time());
                    $audioData = array(
                        'audio_original_name' => $this->_DB->escapeString($name),
                        'audio_new_name' => $new_name,
                        'audio_path' => $path,
                        'audio_hash' => $audioHash
                    );
                    $query = $this->_DB->insert('audios', $audioData);
                    if ($query) {
                        return $audioHash;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * function to get audio file url
     * @param $Hash
     * @return null|string
     */
    public
    function getAudioFileUrl($Hash)
    {
        $Hash = $this->_DB->escapeString($Hash);
        $query = $this->_DB->select('audios', '*', "`audio_hash` = '$Hash'");
        if ($this->_DB->numRows($query) != 0) {
            $fetch = $this->_DB->fetchAssoc($query);
            $path = 'uploads/audioFiles/' . $fetch['audio_path'] . '/' . $fetch['audio_new_name'];
            return $path;
        } else {
            return null;
        }

    }

    /**
     * Function to uploads new  documents
     * @param $array
     * @param string $dir
     * @return null|string
     */
    public
    function uploadDocument($array, $dir = './')
    {

        $valid_formats = array("pdf", "doc", "PDF");
        if (!empty($array)) {
            $tmp = $array["tmp_name"];
            $name = $array["name"];
            //$tempExtension = explode(".", $array["name"]);
            //$extension = end($tempExtension);
            $extension = $this->getExtension($name);
            if (in_array($extension, $valid_formats)) {
                $new_name = md5(time() . '-' . $name) . '.'. $extension;
                $day_folder = date('d-m-y', time());

                if (is_dir($dir . 'uploads/documentFiles/' . $day_folder)) {
                    $path = $day_folder;
                } else {
                    if (mkdir($dir . 'uploads/documentFiles/' . $day_folder)) {
                        $path = $day_folder;
                    } else {
                        $path = '';
                    }
                }
                if (move_uploaded_file($tmp, $dir . 'uploads/documentFiles/' . $path . '/' . $new_name)) {
                    $documentHash = md5($tmp . $new_name . uniqid() . time());
                    $documentData = array(
                        'document_original_name' => $this->_DB->escapeString($name),
                        'document_new_name' => $new_name,
                        'document_path' => $path,
                        'document_hash' => $documentHash
                    );
                    $query = $this->_DB->insert('documents', $documentData);
                    if ($query) {
                        return $documentHash;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }


    /**
     * function to get document file url
     * @param $Hash
     * @return null|string
     */
    public
    function getDocumentFileUrl($Hash)
    {
        $Hash = $this->_DB->escapeString($Hash);
        $query = $this->_DB->select('documents', '*', "`document_hash` = '$Hash'");
        if ($this->_DB->numRows($query) != 0) {
            $fetch = $this->_DB->fetchAssoc($query);
            $path = 'uploads/documentFiles/' . $fetch['document_path'] . '/' . $fetch['document_new_name'];
            return $path;
        } else {
            return null;
        }

    }

    /**
     * Function to uploads new  videos
     * @param $array
     * @param string $dir
     * @return null|string
     */
    public function uploadVideo($array, $dir = './')
    {

        $valid_formats = array("mp4", "avi", "mov", "MP4", "AVI", "MOV");
        if (!empty($array)) {
            $tmp = $array["tmp_name"];
            $name = $array["name"];
            $extension = $this->getExtension($name);
            if (in_array($extension, $valid_formats)) {
                $new_name = md5(time() . '-' . $name) . '.'. $extension;
                $day_folder = date('d-m-y', time());

                if (is_dir($dir . 'uploads/videosFiles/videos/' . $day_folder)) {
                    $path = $day_folder;
                } else {
                    if (mkdir($dir . 'uploads/videosFiles/videos/' . $day_folder, 0755, true)) {
                        $path = $day_folder;
                    } else {
                        $path = '';
                    }
                }
                if (move_uploaded_file($tmp, $dir . 'uploads/videosFiles/videos/' . $path . '/' . $new_name)) {
                    $videoHash = md5($tmp . $new_name . uniqid() . time());
                    
                    // Generate thumbnail using FFmpeg
                    $thumbnailName = md5(time() . '-thumb-' . $name) . '.jpg';
                    $thumbnailDir = $dir . 'uploads/videosFiles/thumbnail/' . $path;
                    if (!is_dir($thumbnailDir)) {
                        mkdir($thumbnailDir, 0755, true);
                    }
                    $videoPath = $dir . 'uploads/videosFiles/videos/' . $path . '/' . $new_name;
                    $thumbnailPath = $thumbnailDir . '/' . $thumbnailName;
                    
                    // Extract frame at 1 second (or first frame if video is shorter)
                    $ffmpegCmd = "ffmpeg -y -i " . escapeshellarg($videoPath) . 
                                " -ss 00:00:01 -vframes 1 -q:v 2 " . 
                                escapeshellarg($thumbnailPath) . " 2>/dev/null";
                    exec($ffmpegCmd, $output, $returnCode);
                    
                    $thumbnailHash = null;
                    if (file_exists($thumbnailPath)) {
                        $thumbnailHash = md5($thumbnailPath . uniqid() . time());
                        $thumbnailData = array(
                            'image_original_name' => $thumbnailName,
                            'image_new_name' => $thumbnailName,
                            'image_type' => 1,
                            'image_path' => $path,
                            'image_hash' => $thumbnailHash
                        );
                        $this->_DB->insert('images', $thumbnailData);
                    }
                    
                    $videoData = array(
                        'video_original_name' => $this->_DB->escapeString($name),
                        'video_new_name' => $new_name,
                        'video_path' => $path,
                        'video_hash' => $videoHash,
                        'thumbnail_hash' => $thumbnailHash
                    );
                    
                    // Add thumbnail_hash column if not exists
                    try {
                        $query = $this->_DB->insert('videos', $videoData);
                    } catch (Exception $e) {
                        // Fallback without thumbnail_hash
                        unset($videoData['thumbnail_hash']);
                        $query = $this->_DB->insert('videos', $videoData);
                    }
                    
                    if ($query) {
                        return $videoHash;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

    }


    /**
     * function to get video file url
     * @param $Hash
     * @return null|string
     */
    public
    function getVideoFileUrl($Hash)
    {
        $Hash = $this->_DB->escapeString($Hash);
        $query = $this->_DB->select('videos', '*', "`video_hash` = '$Hash'");
        if ($this->_DB->numRows($query) != 0) {
            $fetch = $this->_DB->fetchAssoc($query);
            $path = 'uploads/videosFiles/videos/' . $fetch['video_path'] . '/' . $fetch['video_new_name'];
            return $path;
        } else {
            return null;
        }

    }

    /**
     * Function to get auto-generated video thumbnail hash
     * @param $videoHash
     * @return null|string
     */
    public function getVideoThumbnailHash($videoHash)
    {
        $videoHash = $this->_DB->escapeString($videoHash);
        $query = $this->_DB->select('videos', 'thumbnail_hash', "`video_hash` = '$videoHash'");
        if ($this->_DB->numRows($query) != 0) {
            $fetch = $this->_DB->fetchAssoc($query);
            $this->_DB->free($query);
            return !empty($fetch['thumbnail_hash']) ? $fetch['thumbnail_hash'] : null;
        }
        $this->_DB->free($query);
        return null;
    }

    /**
     * Function to uploads new  documents
     * @param $array
     * @param string $dir
     * @return null|string
     */
    public
    function uploadVideoThumbnail($array, $dir = './')
    {

        $valid_formats = array("jpg", "png", "gif", "bmp", "jpeg", "PNG", "JPG", "JPEG", "GIF", "BMP");
        if (!empty($array)) {
            $tmp = $array["tmp_name"];
            $name = $array["name"];
            //$tempExtension = explode(".", $array["name"]);
            //$extension = end($tempExtension);
            $extension = $this->getExtension($name);
            if (in_array($extension, $valid_formats)) {
                $new_name = md5(time() . '-' . $name) . '.'. $extension;
                $day_folder = date('d-m-y', time());

                if (is_dir($dir . 'uploads/videosFiles/thumbnail/' . $day_folder)) {
                    $path = $day_folder;
                } else {
                    if (mkdir($dir . 'uploads/videosFiles/thumbnail/' . $day_folder)) {
                        $path = $day_folder;
                    } else {
                        $path = '';
                    }
                }

                if (move_uploaded_file($tmp, $dir . 'uploads/videosFiles/thumbnail/' . $path . '/' . $new_name)) {
                    $videoThumbnailHash = md5($tmp . $new_name . uniqid() . time());
                    $videoThumbnailData = array(
                        'image_original_name' => $this->_DB->escapeString($name),
                        'image_new_name' => $new_name,
                        'image_path' => $path,
                        'image_hash' => $videoThumbnailHash,
                        'image_type' => 1
                    );
                    $query = $this->_DB->insert('images', $videoThumbnailData);
                    if ($query) {
                        return $videoThumbnailHash;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

    }


    /**
     * function to get document file url
     * @param $Hash
     * @return null|string
     */
    public
    function getVideoThumbnailFileUrl($Hash)
    {
        $Hash = $this->_DB->escapeString($Hash);
        $query = $this->_DB->select('images', '*', "`image_hash` = '$Hash'");
        if ($this->_DB->numRows($query) != 0) {
            $fetch = $this->_DB->fetchAssoc($query);
            $path = 'uploads/videosFiles/thumbnail/' . $fetch['image_path'] . '/' . $fetch['image_new_name'];
            return $path;
        } else {
            return null;
        }

    }


    /**********************************
     *       Method for Sessions
     *********************************/
    /**
     * Function to set session
     * @param $key
     * @param $value
     */
    function setSession($key, $value)
    {
        $_SESSION[$key] = $value;
    }

    /**
     * Function to get session
     * @param $key
     * @return bool
     */
    function getSession($key)
    {
        if (isset($_SESSION[$key])) {
            return $_SESSION[$key];
        } else {
            return false;
        }
    }

    /**
     * Function to unset session
     * @param $key
     * @return bool
     */
    function unsetSession($key)
    {
        if (isset($_SESSION[$key])) {
            unset($_SESSION[$key]);
        } else {
            return false;
        }
    }

    /**
     * Display Error Message
     * @param $messageError
     * @param string $error_type
     * @return string
     */
    function ErrorDisplay($messageError, $error_type = 'no')
    {
        switch ($error_type) {
            case 'no':
                $msg = '<center><div class="form-group has-error">  <label class="control-label" for="inputError"><i class="fa fa-times-circle-o"></i>  ';
                $msg .= $messageError;
                $msg .= '</label></div></center>';
                return $msg;
                break;
            case 'yes':
                $msg = '<center><div class="form-group has-success"> <label class="control-label" for="inputSuccess"><i class="fa fa-check"></i> ';
                $msg .= $messageError;
                $msg .= '</label></div></center>';
                return $msg;
                break;
        }
    }

    /**
     * Function to refresh pages
     * @param $url
     * @param string $time
     * @return string
     */
    public
    function refreshPage($url, $time = '0')
    {
        return "<meta http-equiv=\"refresh\" content=\"$time;URL='$url'\" /> ";
    }

    /**
     * Function to get Settings
     * @param $name
     * @return mixed
     */
    public
    function getSettings($name)
    {
        $query = $this->_DB->select('settings', '`value`', "`name` = '{$name}'");
        $fetch = $this->_DB->fetchAssoc($query);
        return $fetch ? $fetch['value'] : null;
    }

    /**
     * Function to update Config information
     * @param $name
     * @param $value
     */
    public
    function updateSettings($name, $value)
    {
        $name = $this->_DB->escapeString($name);
        $value = $this->_DB->escapeString($value);

        if ($this->_DB->CountRows('settings', "`name` = '{$name}'") > 0) {
            $this->_DB->update('settings', "`value` = '{$value}'", "`name` = '{$name}'");
        } else {
            $this->_DB->insert('settings', array('name' => $name, 'value' => $value));
        }
    }


    /**
     * Function to uploads new images
     * @param $array
     * @param string $dir
     * @return null|string
     */
    public
    function uploadAdminImage($array, $dir = '../')
    {
        $valid_formats = array("jpg", "png", "gif", "bmp", "jpeg", "PNG", "JPG", "JPEG", "GIF", "BMP");
        if (!empty($array)) {
            $tmp = $array["tmp_name"];
            $name = $array["name"];
            $extension = $this->getExtension($name);
            if (in_array($extension, $valid_formats)) {
                $new_name = md5(time() . '-' . $name) .'.'. $extension;
                $day_folder = date('d-m-y', time());
                if (is_dir($dir . 'uploads/imagesFiles/' . $day_folder)) {
                    $path = $day_folder;
                } else {
                    if (mkdir($dir . 'uploads/imagesFiles/' . $day_folder)) {
                        $path = $day_folder;
                    } else {
                        $path = '';
                    }
                }
                if (move_uploaded_file($tmp, $dir . 'uploads/imagesFiles/' . $path . '/' . $new_name)) {
                    $imgHash = md5($tmp . $new_name . uniqid() . time());
                    $imageData = array(
                        'image_original_name' => $this->_DB->escapeString($name),
                        'image_new_name' => $new_name,
                        'image_path' => $path,
                        'image_hash' => $imgHash,
                        'image_type' => 0
                    );
                    $query = $this->_DB->insert('images', $imageData);
                    if ($query) {
                        return $imgHash;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    function getExtension($str)
    {
        $i = strrpos($str, ".");
        if (!$i) {
            return "";
        }
        $l = strlen($str) - $i;
        $ext = substr($str, $i + 1, $l);
        return $ext;
    }

    public function pushToSocket($event, $data, $targetUserId = null, $targetGroupId = null)
    {
        $pushUrl = getenv('SOCKET_PUSH_URL') ?: 'http://127.0.0.1:9001/push';
        $payload = array(
            'event' => $event,
            'data' => $data
        );
        if ($targetUserId !== null) {
            $payload['targetUserId'] = $targetUserId;
        }
        if ($targetGroupId !== null) {
            $payload['targetGroupId'] = $targetGroupId;
        }
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $pushUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        if (strpos($pushUrl, 'https://') === 0) {
            $verifyTls = filter_var(getenv('SOCKET_TLS_VERIFY') ?: 'true', FILTER_VALIDATE_BOOLEAN);
            curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, $verifyTls ? 2 : 0);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, $verifyTls);
        }
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        $result = curl_exec($ch);
        if ($result === FALSE) {
            error_log('pushToSocket curl error: ' . curl_error($ch));
        }
        curl_close($ch);
        return $result;
    }

    private static $fcmAccessTokenCache = null;
    private static $fcmAccessTokenExpiry = 0;

    private function getFcmAccessToken()
    {
        if (self::$fcmAccessTokenCache && time() < self::$fcmAccessTokenExpiry) {
            return self::$fcmAccessTokenCache;
        }

        $serviceAccountPath = $this->getSettings('fcmServiceAccountPath');
        if (empty($serviceAccountPath) || !file_exists($serviceAccountPath)) {
            return null;
        }
        $serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);
        if (!$serviceAccount || !isset($serviceAccount['private_key']) || !isset($serviceAccount['client_email'])) {
            return null;
        }

        // Base64URL encoding function for JWT
        $base64UrlEncode = function($data) {
            return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
        };

        $now = time();
        $header = $base64UrlEncode(json_encode(array('alg' => 'RS256', 'typ' => 'JWT')));
        $claim = array(
            'iss' => $serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'iat' => $now,
            'exp' => $now + 3600
        );
        $payload = $base64UrlEncode(json_encode($claim));
        $signatureInput = $header . '.' . $payload;

        openssl_sign($signatureInput, $signature, $serviceAccount['private_key'], OPENSSL_ALGO_SHA256);
        $signatureB64 = $base64UrlEncode($signature);

        $jwt = $header . '.' . $payload . '.' . $signatureB64;

        $ch = curl_init('https://oauth2.googleapis.com/token');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query(array(
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        )));
        $response = curl_exec($ch);
        curl_close($ch);

        $tokenData = json_decode($response, true);
        if (isset($tokenData['access_token'])) {
            $expiresIn = isset($tokenData['expires_in']) ? intval($tokenData['expires_in']) : 3600;
            self::$fcmAccessTokenCache = $tokenData['access_token'];
            self::$fcmAccessTokenExpiry = time() + $expiresIn - 60;
            return $tokenData['access_token'];
        }
        return null;
    }

    private function sendFcmV1($token, $stringData)
    {
        $projectId = $this->getSettings('fcmProjectId');
        if (empty($projectId)) {
            return false;
        }

        $accessToken = $this->getFcmAccessToken();
        if (!$accessToken) {
            return false;
        }

        $payload = array(
            'message' => array(
                'token' => $token,
                'data' => $stringData
            )
        );
        $ch = curl_init('https://fcm.googleapis.com/v1/projects/' . $projectId . '/messages:send');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array(
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        if ($result === FALSE) {
            error_log('FCM v1 send error: ' . curl_error($ch));
        }
        curl_close($ch);
        error_log('FCM v1 response code: ' . $httpCode . ' body: ' . $result);
        return $result;
    }

    private function sendFcmLegacy($token, $stringData)
    {
        $serverKey = $this->getSettings('googleApiKey');
        if (empty($serverKey)) {
            return false;
        }

        $payload = array(
            'to' => $token,
            'data' => $stringData,
            'priority' => 'high',
            'content_available' => true
        );
        $ch = curl_init('https://fcm.googleapis.com/fcm/send');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array(
            'Authorization: key=' . $serverKey,
            'Content-Type: application/json'
        ));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        if ($result === FALSE) {
            error_log('FCM legacy send error: ' . curl_error($ch));
        }
        curl_close($ch);
        error_log('FCM legacy response code: ' . $httpCode . ' body: ' . $result);
        return $result;
    }

    public function sendCallPush($recipientId, $callData)
    {
        $query = $this->_DB->select('users', '`registered_id`', "`id` = '{$recipientId}'");
        $fetch = $this->_DB->fetchAssoc($query);
        if (!$fetch || empty($fetch['registered_id'])) {
            return false;
        }

        $fcmData = array();
        foreach ($callData as $key => $value) {
            $fcmData[$key] = strval($value);
        }
        $fcmData['actionType'] = 'new_call';

        return $this->sendMessageThroughFCM($fetch['registered_id'], $fcmData);
    }

    public function sendMessageThroughFCM($token, $data)
    {
        if (empty($token)) {
            return false;
        }

        $stringData = array();
        foreach ($data as $key => $value) {
            $stringData[$key] = strval($value);
        }

        $v1Result = $this->sendFcmV1($token, $stringData);
        if ($v1Result !== false) {
            return $v1Result;
        }

        return $this->sendFcmLegacy($token, $stringData);
    }

    public function sendGroupMessageThroughFCM($notificationKey, $data)
    {
        if (empty($notificationKey)) {
            return false;
        }

        $stringData = array();
        foreach ($data as $key => $value) {
            $stringData[$key] = strval($value);
        }

        $v1Result = $this->sendFcmV1($notificationKey, $stringData);
        if ($v1Result !== false) {
            return $v1Result;
        }

        return $this->sendFcmLegacy($notificationKey, $stringData);
    }

    public function testFcmConnection()
    {
        $result = array(
            'success' => false,
            'message' => '',
            'projectId' => null,
            'serviceAccountPath' => null,
            'serviceAccountExists' => false,
            'accessTokenGenerated' => false
        );

        $projectId = $this->getSettings('fcmProjectId');
        $serviceAccountPath = $this->getSettings('fcmServiceAccountPath');

        $result['projectId'] = $projectId;
        $result['serviceAccountPath'] = $serviceAccountPath;

        if (empty($projectId)) {
            $result['message'] = 'FCM project ID not configured';
            return $result;
        }

        if (empty($serviceAccountPath)) {
            $result['message'] = 'FCM service account path not configured';
            return $result;
        }

        if (!file_exists($serviceAccountPath)) {
            $result['message'] = 'FCM service account file not found at: ' . $serviceAccountPath;
            return $result;
        }

        $result['serviceAccountExists'] = true;

        $accessToken = $this->getFcmAccessToken();
        if ($accessToken) {
            $result['accessTokenGenerated'] = true;
            $result['success'] = true;
            $result['message'] = 'FCM is properly configured. Access token generated successfully.';
        } else {
            $result['message'] = 'Failed to generate FCM access token. Check service account JSON.';
        }

        return $result;
    }

}
