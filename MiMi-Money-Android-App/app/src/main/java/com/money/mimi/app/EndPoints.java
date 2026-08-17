package com.money.mimi.app;

public class EndPoints {

    private static final boolean DEBUG_MODE = false;
    private static final String LOCAL_BASE_URL = "http://10.0.2.2:8888/";
    private static final String LOCAL_SOCKET_URL = "http://10.0.2.2:9001";
    
    private static final String PROD_BASE_URL = "https://com.mimi.money/";
    private static final String PROD_SOCKET_URL = "https://com.mimi.money:9001";
    
    public static final String BACKEND_BASE_URL = DEBUG_MODE ? LOCAL_BASE_URL : PROD_BASE_URL;
    
    private static final String CHAT_SERVER_DIRECT_URL = DEBUG_MODE ? LOCAL_SOCKET_URL : PROD_SOCKET_URL;
    private static final String CHAT_SERVER_PROXY_URL = DEBUG_MODE ? LOCAL_SOCKET_URL : PROD_SOCKET_URL;
    static final String BACKEND_CHAT_SERVER_URL = CHAT_SERVER_DIRECT_URL;
    static final String BACKEND_CHAT_SERVER_FALLBACK_URL = CHAT_SERVER_PROXY_URL;

    public static final String SEND_MESSAGE = "api.php?cmd=sendMessage";
    public static final String SEND_GROUP_MESSAGE = "api.php?cmd=saveMessageGroup";
    
    public static final String JOIN = "api.php?cmd=Join";
    public static final String RESEND_REQUEST_SMS = "api.php?cmd=Resend";
    public static final String VERIFY_USER = "api.php?cmd=VerifyUser";
    public static final String CHECK_NETWORK = "api.php?cmd=CheckNetwork";

    public static final String CREATE_GROUP = "api.php?cmd=createGroup";
    public static final String ADD_MEMBERS_TO_GROUP = "api.php?cmd=addMembersToGroup";
    public static final String REMOVE_MEMBER_FROM_GROUP = "api.php?cmd=removeMemberFromGroup";
    public static final String MAKE_MEMBER_AS_ADMIN = "api.php?cmd=makeMemberAdmin";
    public static final String REMOVE_MEMBER_AS_ADMIN = "api.php?cmd=makeAdminMember";
    public static final String GROUPS_lIST = "api.php?cmd=getGroups";
    public static final String GROUP_MEMBERS_lIST = "api.php?cmd=GetGroupMembers";
    public static final String EXIT_GROUP = "api.php?cmd=ExitGroup";
    public static final String DELETE_GROUP = "api.php?cmd=DeleteGroup";
    public static final String GET_GROUP = "api.php?cmd=GetGroup";
    public static final String UPLOAD_GROUP_PROFILE_IMAGE = "api.php?cmd=uploadGroupImage";
    public static final String EDIT_GROUP_NAME = "api.php?cmd=EditGroupName";

    public static final String UPLOAD_MESSAGES_IMAGE = "api.php?cmd=uploadMessagesImage";
    public static final String UPLOAD_MESSAGES_VIDEO = "api.php?cmd=uploadMessagesVideo";
    public static final String UPLOAD_MESSAGES_AUDIO = "api.php?cmd=uploadMessagesAudio";
    public static final String UPLOAD_MESSAGES_DOCUMENT = "api.php?cmd=uploadMessagesDocument";
    public static final String HAS_BACKUP = "api.php?cmd=userHasBackup";

    public static final String SEND_CONTACTS = "api.php?cmd=SendContacts";
    public static final String GET_CONTACT = "api.php?cmd=GetContact";
    public static final String GET_STATUS = "api.php?cmd=GetStatus";
    public static final String SAVE_EMITTED_CALL = "api.php?cmd=saveEmittedCall";
    public static final String SAVE_ACCEPTED_CALL = "api.php?cmd=saveAcceptedCall";
    public static final String SAVE_RECEIVED_CALL = "api.php?cmd=saveReceivedCall";
    public static final String BLOCK_USER = "api.php?cmd=blockUser";
    public static final String UN_BLOCK_USER = "api.php?cmd=unBlockUser";
    public static final String DELETE_ALL_STATUS = "api.php?cmd=DeleteAllStatus";
    public static final String DELETE_STATUS = "api.php?cmd=DeleteStatus";
    public static final String UPDATE_STATUS = "api.php?cmd=UpdateStatus";
    public static final String EDIT_STATUS = "api.php?cmd=EditStatus";
    public static final String EDIT_NAME = "api.php?cmd=EditName";
    public static final String UPLOAD_PROFILE_IMAGE = "api.php?cmd=uploadImage";
    public static final String DELETE_ACCOUNT = "api.php?cmd=DeleteUserAccount";
    public static final String DELETE_ACCOUNT_CONFIRMATION = "api.php?cmd=DeleteUserAccountConfirmation";

    public static final String PROFILE_IMAGE_URL = BACKEND_BASE_URL + "image/profile/";
    public static final String PROFILE_PREVIEW_IMAGE_URL = BACKEND_BASE_URL + "image/profilePreview/";
    public static final String PROFILE_PREVIEW_HOLDER_IMAGE_URL = BACKEND_BASE_URL + "image/profilePreviewHolder/";
    public static final String ROWS_IMAGE_URL = BACKEND_BASE_URL + "image/rowImage/";
    public static final String SETTINGS_IMAGE_URL = BACKEND_BASE_URL + "image/settings/";
    public static final String EDIT_PROFILE_IMAGE_URL = BACKEND_BASE_URL + "image/editProfile/";

    public static final String MESSAGE_DOCUMENT_URL = BACKEND_BASE_URL + "document/messageDocument/";
    public static final String MESSAGE_HOLDER_IMAGE_URL = BACKEND_BASE_URL + "image/messageImageHolder/";
    public static final String MESSAGE_IMAGE_URL = BACKEND_BASE_URL + "image/messageImage/";
    public static final String MESSAGE_VIDEO_THUMBNAIL_URL = BACKEND_BASE_URL + "video/messageVideoThumbnail/";
    public static final String MESSAGE_AUDIO_URL = BACKEND_BASE_URL + "audio/messageAudio/";

    public static final String MESSAGE_DOCUMENT_DOWNLOAD_URL = "document/messageDocument/";
    public static final String MESSAGE_BACKUP_DOWNLOAD_URL = "backup/messageBackup/";
    public static final String MESSAGE_IMAGE_DOWNLOAD_URL = "image/messageImage/";
    public static final String MESSAGE_VIDEO_DOWNLOAD_THUMBNAIL_URL = "video/messageVideoThumbnail/";
    public static final String MESSAGE_VIDEO_DOWNLOAD_URL = "video/messageVideo/";
    public static final String MESSAGE_AUDIO_DOWNLOAD_URL = "audio/messageAudio/";

    public static final String GET_APPLICATION_SETTINGS = "api.php?cmd=GetAppSettings";
    public static final String GET_APPLICATION_PRIVACY = "api.php?cmd=GetApplicationPrivacy";
    public static final String GET_ICE_SERVERS = "api.php?cmd=GetIceServers";
    public static final String UPDATE_FCM_TOKEN = "api.php?cmd=updateRegisteredId";
}
