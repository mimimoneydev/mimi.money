<?php


include 'config/DataBase.php';
include 'config/Config.php';
include 'application/helpers/Helper.php';
include 'application/helpers/ImageResize.php';


$_DB = new DataBase($_Config);
$_DB->connect();
$_DB->selectDB();
$_GB = new Helper($_DB);
use Eventviva\ImageResize;

function getMimeType($path)
{
    $ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
    $mimeTypes = array(
        'jpg'  => 'image/jpeg',
        'jpeg' => 'image/jpeg',
        'png'  => 'image/png',
        'gif'  => 'image/gif',
        'webp' => 'image/webp',
        'bmp'  => 'image/bmp',
        'svg'  => 'image/svg+xml',
        'mp4'  => 'video/mp4',
        'avi'  => 'video/x-msvideo',
        'mov'  => 'video/quicktime',
        'mkv'  => 'video/x-matroska',
        'webm' => 'video/webm',
        '3gp'  => 'video/3gpp',
        'mp3'  => 'audio/mpeg',
        'wav'  => 'audio/wav',
        'ogg'  => 'audio/ogg',
        'aac'  => 'audio/aac',
        'm4a'  => 'audio/mp4',
        'flac' => 'audio/flac',
        'pdf'  => 'application/pdf',
        'doc'  => 'application/msword',
        'docx' => 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'xls'  => 'application/vnd.ms-excel',
        'xlsx' => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'ppt'  => 'application/vnd.ms-powerpoint',
        'pptx' => 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'txt'  => 'text/plain',
        'zip'  => 'application/zip',
        'rar'  => 'application/x-rar-compressed',
    );
    return isset($mimeTypes[$ext]) ? $mimeTypes[$ext] : 'application/octet-stream';
}

function serveFile($path)
{
    if (!file_exists($path)) {
        header($_SERVER["SERVER_PROTOCOL"] . " 404 Not Found");
        die("Error: File not found.");
    }
    header($_SERVER["SERVER_PROTOCOL"] . " 200 OK");
    header("Cache-Control: public");
    header("Content-Type: " . getMimeType($path));
    header("Content-Transfer-Encoding: Binary");
    header("Content-Disposition: attachment; filename=\"" . basename($path) . "\"");
    header("Content-Length:" . filesize($path));
    readfile($path);
    die();
}

if (isset($_GET['hash'])) {

    if (isset($_GET['images'])) {
        $path = $_GB->getSafeImage($_GET['hash']);
        if ($path != null) {
            if (isset($_GET['profile'])) {
                serveFile($path);
            } else if (isset($_GET['profilePreview'])) {
                $image = new ImageResize($path);
                $image->crop(300, 300);
                $image->output(IMAGETYPE_PNG, 9);
            } elseif (isset($_GET['profilePreviewHolder'])) {
                $image = new ImageResize($path);
                $image->crop(30, 30);
                $image->output(IMAGETYPE_PNG, 9);
            } else if (isset($_GET['rowImage'])) {
                $image = new ImageResize($path);
                $image->crop(70, 70);
                $image->output(IMAGETYPE_PNG, 9);
            } else if (isset($_GET['settings'])) {
                $image = new ImageResize($path);
                $image->crop(100, 100);
                $image->output(IMAGETYPE_PNG, 9);
            } else if (isset($_GET['editProfile'])) {
                serveFile($path);
            } else if (isset($_GET['messageImage'])) {
                serveFile($path);
            } else if (isset($_GET['messageImageHolder'])) {
                $image = new ImageResize($path);
                $image->crop(30, 30);
                $image->output(IMAGETYPE_PNG, 9);
            }
        } else {
            ob_clean();
            header('Content-Type: image/jpg');
            echo file_get_contents(null);
        }
    } else if (isset($_GET['videos'])) {
        if (isset($_GET['messageVideo'])) {
            $url = $_GB->getVideoFileUrl($_GET['hash']);
            serveFile($url);
        } else if (isset($_GET['messageVideoThumbnail'])) {
            $urlThumbnail = $_GB->getVideoThumbnailFileUrl($_GET['hash']);
            if ($urlThumbnail != null) {
                $imageThumbnail = new ImageResize($urlThumbnail);
                $imageThumbnail->crop(500, 500);
                $imageThumbnail->output(IMAGETYPE_PNG, 9);
            } else {
                ob_clean();
                header('Content-Type: image/jpg');
                echo file_get_contents(null);
            }
        }

    } else if (isset($_GET['audios'])) {
        if (isset($_GET['messageAudio'])) {
            $url = $_GB->getAudioFileUrl($_GET['hash']);
            serveFile($url);
        }
    } else if (isset($_GET['documents'])) {
        if (isset($_GET['messageDocument'])) {
            $url = $_GB->getDocumentFileUrl($_GET['hash']);
            serveFile($url);
        } else if (isset($_GET['messageBackup'])) {
            $backupPath = 'uploads/backups/' . $_GET['hash'];
            if (file_exists($backupPath)) {
                serveFile($backupPath);
            } else {
                header($_SERVER["SERVER_PROTOCOL"] . " 404 Not Found");
                die("Error: Backup file not found.");
            }
        }
    }


}
