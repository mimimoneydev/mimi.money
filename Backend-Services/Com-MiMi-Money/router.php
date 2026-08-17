<?php
$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$uri = ltrim($uri, '/');

if ($uri === '' || $uri === 'index.php') {
    return false;
}

if (php_sapi_name() === 'cli-server') {
    $staticExtensions = ['css', 'js', 'png', 'jpg', 'jpeg', 'gif', 'ico', 'svg', 'woff', 'woff2', 'ttf', 'eot'];
    $ext = pathinfo($uri, PATHINFO_EXTENSION);
    if (in_array($ext, $staticExtensions) && file_exists(__DIR__ . '/' . $uri)) {
        return false;
    }
}

$routes = [
    'Join' => 'api.php?cmd=Join',
    'VerifyUser' => 'api.php?cmd=VerifyUser',
    'Resend' => 'api.php?cmd=Resend',
    'CheckNetwork' => 'api.php?cmd=CheckNetwork',
    'GetAppSettings' => 'api.php?cmd=GetAppSettings',
    'GetApplicationVersion' => 'api.php?cmd=GetApplicationVersion',
    'GetApplicationPrivacy' => 'api.php?cmd=GetApplicationPrivacy',
    'updateRegisteredId' => 'api.php?cmd=updateRegisteredId',
    'GetAdmobInformation' => 'api.php?cmd=GetAdmobInformation',
    'GetAdmobInterstitialInformation' => 'api.php?cmd=GetAdmobInterstitialInformation',
    'GetVideoAdmobInformation' => 'api.php?cmd=GetVideoAdmobInformation',
    'SendContacts' => 'api.php?cmd=SendContacts',
    'blockUser/([0-9]+)' => 'api.php?cmd=blockUser&userId=$1',
    'unBlockUser/([0-9]+)' => 'api.php?cmd=unBlockUser&userId=$1',
    'saveAcceptedCall' => 'api.php?cmd=saveAcceptedCall',
    'saveEmittedCall' => 'api.php?cmd=saveEmittedCall',
    'saveReceivedCall' => 'api.php?cmd=saveReceivedCall',
    'GetStatus' => 'api.php?cmd=GetStatus',
    'DeleteAllStatus' => 'api.php?cmd=DeleteAllStatus',
    'UpdateStatus/([0-9]+)' => 'api.php?cmd=UpdateStatus&statusID=$1',
    'EditStatus' => 'api.php?cmd=EditStatus',
    'EditName' => 'api.php?cmd=EditName',
    'uploadImage' => 'api.php?cmd=uploadImage',
    'DeleteAccount' => 'api.php?cmd=DeleteUserAccount',
    'DeleteUserAccountConfirmation' => 'api.php?cmd=DeleteUserAccountConfirmation',
    'Messages/send' => 'api.php?cmd=sendMessage',
    'Groups/createGroup' => 'api.php?cmd=createGroup',
    'Groups/addMembersToGroup' => 'api.php?cmd=addMembersToGroup',
    'Groups/removeMemberFromGroup' => 'api.php?cmd=removeMemberFromGroup',
    'Groups/makeMemberAdmin' => 'api.php?cmd=makeMemberAdmin',
    'Groups/makeAdminMember' => 'api.php?cmd=makeAdminMember',
    'Groups/all' => 'api.php?cmd=getGroups',
    'Groups/saveMessage' => 'api.php?cmd=saveMessageGroup',
    'EditGroupName' => 'api.php?cmd=EditGroupName',
    'uploadGroupImage' => 'api.php?cmd=uploadGroupImage',
    'uploadMessagesImage' => 'api.php?cmd=uploadMessagesImage',
    'uploadMessagesAudio' => 'api.php?cmd=uploadMessagesAudio',
    'uploadMessagesDocument' => 'api.php?cmd=uploadMessagesDocument',
    'uploadMessagesVideo' => 'api.php?cmd=uploadMessagesVideo',
    'userHasBackup' => 'api.php?cmd=userHasBackup',
];

if (preg_match('#^GetContact/([0-9]+)$#', $uri, $m)) {
    $_GET['cmd'] = 'GetContact';
    $_GET['userID'] = $m[1];
    require __DIR__ . '/api.php';
    return true;
}
if (preg_match('#^GetGroup/([0-9]+)$#', $uri, $m)) {
    $_GET['cmd'] = 'GetGroup';
    $_GET['groupID'] = $m[1];
    require __DIR__ . '/api.php';
    return true;
}
if (preg_match('#^GetGroupMembers/([0-9]+)$#', $uri, $m)) {
    $_GET['cmd'] = 'GetGroupMembers';
    $_GET['groupID'] = $m[1];
    require __DIR__ . '/api.php';
    return true;
}
if (preg_match('#^ExitGroup/([0-9]+)$#', $uri, $m)) {
    $_GET['cmd'] = 'ExitGroup';
    $_GET['groupID'] = $m[1];
    require __DIR__ . '/api.php';
    return true;
}
if (preg_match('#^DeleteGroup/([0-9]+)$#', $uri, $m)) {
    $_GET['cmd'] = 'DeleteGroup';
    $_GET['groupID'] = $m[1];
    require __DIR__ . '/api.php';
    return true;
}
if (preg_match('#^DeleteStatus/(.+)$#', $uri, $m)) {
    $_GET['cmd'] = 'DeleteStatus';
    $_GET['statusId'] = $m[1];
    require __DIR__ . '/api.php';
    return true;
}

$imagePatterns = [
    '#^image/profile/(.+)$#' => ['images', 'profile'],
    '#^image/profilePreview/(.+)$#' => ['images', 'profilePreview'],
    '#^image/profilePreviewHolder/(.+)$#' => ['images', 'profilePreviewHolder'],
    '#^image/rowImage/(.+)$#' => ['images', 'rowImage'],
    '#^image/settings/(.+)$#' => ['images', 'settings'],
    '#^image/editProfile/(.+)$#' => ['images', 'editProfile'],
    '#^image/messageImage/(.+)$#' => ['images', 'messageImage'],
    '#^image/messageImageHolder/(.+)$#' => ['images', 'messageImageHolder'],
];
foreach ($imagePatterns as $pattern => $flags) {
    if (preg_match($pattern, $uri, $m)) {
        $_GET['hash'] = $m[1];
        $_GET['images'] = true;
        $_GET[$flags[1]] = true;
        require __DIR__ . '/safe_files.php';
        return true;
    }
}

$videoPatterns = [
    '#^video/messageVideo/(.+)$#' => 'messageVideo',
    '#^video/messageVideoThumbnail/(.+)$#' => 'messageVideoThumbnail',
];
foreach ($videoPatterns as $pattern => $type) {
    if (preg_match($pattern, $uri, $m)) {
        $_GET['hash'] = $m[1];
        $_GET['videos'] = true;
        $_GET[$type] = true;
        require __DIR__ . '/safe_files.php';
        return true;
    }
}

$audioPatterns = [
    '#^audio/messageAudio/(.+)$#' => 'messageAudio',
];
foreach ($audioPatterns as $pattern => $type) {
    if (preg_match($pattern, $uri, $m)) {
        $_GET['hash'] = $m[1];
        $_GET['audios'] = true;
        $_GET[$type] = true;
        require __DIR__ . '/safe_files.php';
        return true;
    }
}

$docPatterns = [
    '#^document/messageDocument/(.+)$#' => 'messageDocument',
    '#^backup/messageBackup/(.+)$#' => 'messageBackup',
];
foreach ($docPatterns as $pattern => $type) {
    if (preg_match($pattern, $uri, $m)) {
        $_GET['hash'] = $m[1];
        $_GET['documents'] = true;
        $_GET[$type] = true;
        require __DIR__ . '/safe_files.php';
        return true;
    }
}

if (isset($routes[$uri])) {
    $target = $routes[$uri];
    $parts = parse_url($target);
    parse_str($parts['query'], $queryParams);
    foreach ($queryParams as $k => $v) {
        $_GET[$k] = $v;
    }
    require __DIR__ . '/api.php';
    return true;
}

if (file_exists(__DIR__ . '/' . $uri)) {
    return false;
}

http_response_code(404);
echo json_encode(['success' => false, 'message' => 'Not found']);
