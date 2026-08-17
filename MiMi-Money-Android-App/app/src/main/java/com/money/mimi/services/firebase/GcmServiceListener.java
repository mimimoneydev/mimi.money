package com.money.mimi.services.firebase;

import android.content.Intent;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.ForegroundRuning;
import com.money.mimi.helpers.notifications.DappNoticeStore;
import com.money.mimi.models.notifications.DappNotice;
import com.money.mimi.activities.call.IncomingCallActivity;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.activities.messages.MessagesPopupActivity;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.services.MainService;
import com.money.mimi.services.sync.BackgroundSyncScheduler;

import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;

public class GcmServiceListener extends FirebaseMessagingService {
    private static final String NOTICE_CHANNEL_ID = "mimi_money_notices";
    private static final int TYPE_SIMPLE = 1;
    private static final int TYPE_DIALOG = 2;
    private static final int TYPE_WEB = 3;
    private static final int TYPE_NEWS = 4;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            AppHelper.LogCat("FCM onMessageReceived from: " + remoteMessage.getFrom());
            if (remoteMessage.getNotification() != null) {
                AppHelper.LogCat("FCM notification body: " + remoteMessage.getNotification().getBody());
            }
        } catch (Exception e) {
            AppHelper.LogCat("FCM onMessageReceived exception: " + e.getMessage());
        }

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            AppHelper.LogCat("FCM data payload: " + data.toString());
            String actionType = data.get("actionType");
            if (actionType == null) {
                handleDappNotice(data);
                return;
            }

            switch (actionType) {
                case AppConstants.SOCKET_NEW_MESSAGE_SERVER:
                    handleNewMessage(remoteMessage);
                    break;
                case AppConstants.SOCKET_NEW_MESSAGE_GROUP_SERVER:
                    handleNewGroupMessage(remoteMessage);
                    break;
                case "new_call":
                    handleNewCall(remoteMessage);
                    break;
                default:
                    if (isDappNoticePayload(data)) {
                        handleDappNotice(data);
                    } else {
                        AppHelper.LogCat("FCM unknown actionType: " + actionType);
                    }
                    break;
            }
        } else if (remoteMessage.getNotification() != null) {
            handleNotificationOnlyNotice(remoteMessage.getNotification());
        }
    }

    @Override
    public void onNewToken(String token) {
        AppHelper.LogCat("FCM onNewToken: " + token);
        NoticeRegistrationManager.register(this, token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(RegistrationIntentService.EXTRA_FCM_TOKEN, token);
        RegistrationIntentService.enqueueWork(this, intent);
    }

    private void handleNewMessage(RemoteMessage remoteMessage) {
        AppHelper.LogCat("FCM handleNewMessage");
        Intent serviceIntent = new Intent(this, com.money.mimi.services.MainService.class);
        serviceIntent.setAction("new_user_message_notification_whatsclone");
        serviceIntent.putExtra("conversationID", remoteMessage.getData().get("conversationID"));
        serviceIntent.putExtra("recipientID", remoteMessage.getData().get("recipientID"));
        serviceIntent.putExtra("senderId", remoteMessage.getData().get("senderId"));
        serviceIntent.putExtra("userImage", remoteMessage.getData().get("userImage"));
        serviceIntent.putExtra("senderImage", remoteMessage.getData().get("senderImage"));
        serviceIntent.putExtra("username", remoteMessage.getData().get("senderName"));
        serviceIntent.putExtra("walletAddress", remoteMessage.getData().get("walletAddress"));
        serviceIntent.putExtra("recipientWalletAddress", remoteMessage.getData().get("recipientWalletAddress"));
        serviceIntent.putExtra("messageId", remoteMessage.getData().get("messageId"));
        serviceIntent.putExtra("message", remoteMessage.getData().get("messageBody"));
        serviceIntent.putExtra("date", remoteMessage.getData().get("date"));
        serviceIntent.putExtra("image", remoteMessage.getData().get("image"));
        serviceIntent.putExtra("video", remoteMessage.getData().get("video"));
        serviceIntent.putExtra("audio", remoteMessage.getData().get("audio"));
        serviceIntent.putExtra("document", remoteMessage.getData().get("document"));
        serviceIntent.putExtra("thumbnail", remoteMessage.getData().get("thumbnail"));
        serviceIntent.putExtra("duration", remoteMessage.getData().get("duration"));
        serviceIntent.putExtra("fileSize", remoteMessage.getData().get("fileSize"));
        serviceIntent.putExtra("file", remoteMessage.getData().get("file"));
        serviceIntent.putExtra("app", getPackageName());
        if (!startMainServiceSafely(serviceIntent)) {
            Map<String, String> data = remoteMessage.getData();
            PendingPushStore.enqueue(this, data, false);
            BackgroundSyncScheduler.enqueue(this);
            String file = data.get("file");
            String message = hasText(file) ? file : data.get("messageBody");
            NotificationsManager.showUserNotification(
                    this,
                    parseInt(data.get("conversationID")),
                    data.get("walletAddress"),
                    message,
                    parseInt(data.get("recipientID")),
                    data.get("userImage"));
        }
    }

    private void handleNewGroupMessage(RemoteMessage remoteMessage) {
        AppHelper.LogCat("FCM handleNewGroupMessage");
        Intent serviceIntent = new Intent(this, com.money.mimi.services.MainService.class);
        serviceIntent.setAction("new_group_message_notification_whatsclone");
        serviceIntent.putExtra("conversationID", remoteMessage.getData().get("conversationID"));
        serviceIntent.putExtra("recipientID", remoteMessage.getData().get("senderId"));
        serviceIntent.putExtra("groupID", remoteMessage.getData().get("groupID"));
        serviceIntent.putExtra("groupImage", remoteMessage.getData().get("groupImage"));
        serviceIntent.putExtra("username", remoteMessage.getData().get("senderName"));
        serviceIntent.putExtra("senderPhone", remoteMessage.getData().get("walletAddress"));
        serviceIntent.putExtra("groupName", remoteMessage.getData().get("groupName"));
        serviceIntent.putExtra("message", remoteMessage.getData().get("messageBody"));
        serviceIntent.putExtra("app", getPackageName());
        if (!startMainServiceSafely(serviceIntent)) {
            PendingPushStore.enqueue(this, remoteMessage.getData(), true);
            BackgroundSyncScheduler.enqueue(this);
            showGroupMessageNotification(remoteMessage.getData());
        }
    }

    private boolean startMainServiceSafely(Intent serviceIntent) {
        return AppHelper.startMainService(this, serviceIntent);
    }

    private void handleNewCall(RemoteMessage remoteMessage) {
        AppHelper.LogCat("FCM handleNewCall - showing call notification");

        String callerSocketId = remoteMessage.getData().get("callerSocketId");
        String callerPhone = remoteMessage.getData().get("callerPhone");
        String callerIdStr = remoteMessage.getData().get("callerId");
        String callerName = remoteMessage.getData().get("callerName");
        String callerImage = remoteMessage.getData().get("callerImage");
        String callType = remoteMessage.getData().get("callType");
        boolean isVideoCall = "video".equalsIgnoreCase(callType);

        int callerID = 0;
        try {
            callerID = Integer.parseInt(callerIdStr != null ? callerIdStr : "0");
        } catch (NumberFormatException e) {
            AppHelper.LogCat("FCM handleNewCall invalid callerId: " + callerIdStr);
        }

        if (callerSocketId == null || callerID == 0) {
            AppHelper.LogCat("FCM handleNewCall missing call data, cannot show call notification");
            return;
        }

        com.money.mimi.helpers.notifications.NotificationsManager.showIncomingCallNotification(
                this, callerSocketId, callerID, callerPhone, callerImage, isVideoCall);
    }

    private void showGroupMessageNotification(Map<String, String> data) {
        int conversationId = parseInt(data.get("conversationID"));
        int groupId = parseInt(data.get("groupID"));
        String groupName = data.get("groupName");
        String senderName = hasText(data.get("senderName"))
                ? data.get("senderName") : data.get("walletAddress");
        String body = data.get("messageBody");
        String displayMessage = (hasText(senderName) ? senderName + " : " : "")
                + (body != null ? body : "");

        Intent messageIntent = new Intent(this, MessagesActivity.class);
        messageIntent.putExtra("conversationID", conversationId);
        messageIntent.putExtra("groupID", groupId);
        messageIntent.putExtra("isGroup", true);
        messageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent popupIntent = new Intent(this, MessagesPopupActivity.class);
        popupIntent.putExtra("conversationID", conversationId);
        popupIntent.putExtra("groupID", groupId);
        popupIntent.putExtra("isGroup", true);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        NotificationsManager.showGroupNotification(this, messageIntent, popupIntent,
                hasText(groupName) ? groupName : getString(R.string.app_name),
                displayMessage, groupId, data.get("groupImage"));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value != null ? value : "0");
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value);
    }

    private void handleDappNotice(Map<String, String> data) {
        if (!isDappNoticePayload(data)) {
            AppHelper.LogCat("FCM payload ignored: no notice fields");
            return;
        }
        DappNotice notice = DappNoticeStore.fromFcm(data);
        if (notice == null) {
            return;
        }

        int type = parseNoticeType(notice.getType());
        if (type == TYPE_NEWS) {
            DappNoticeStore.save(this, notice);
        }

        if (type == TYPE_DIALOG && showDialogNoticeIfForeground(notice)) {
            return;
        }

        showDappNoticeNotification(notice);
    }

    private void handleNotificationOnlyNotice(RemoteMessage.Notification notification) {
        HashMap<String, String> data = new HashMap<>();
        if (notification.getTitle() != null) {
            data.put("title", notification.getTitle());
        }
        if (notification.getBody() != null) {
            data.put("msg", notification.getBody());
        }
        if (notification.getImageUrl() != null) {
            data.put("image", notification.getImageUrl().toString());
        }
        handleDappNotice(data);
    }

    private boolean isDappNoticePayload(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        String type = data.get("type");
        if (!TextUtils.isEmpty(type)) {
            try {
                int noticeType = Integer.parseInt(type);
                if (noticeType >= 1 && noticeType <= 4) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return hasAny(data, "title", "notification_title", "subject")
                && hasAny(data, "msg", "message", "body", "notification_body");
    }

    private boolean hasAny(Map<String, String> data, String... keys) {
        for (String key : keys) {
            if (!TextUtils.isEmpty(data.get(key))) {
                return true;
            }
        }
        return false;
    }

    private void showDappNoticeNotification(DappNotice notice) {
        if (notice == null) {
            return;
        }
        if (AppHelper.isAndroid13() && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTICE_CHANNEL_ID,
                    getString(R.string.wallet_dapp_notifications_title),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        Intent intent = buildNoticeIntent(notice);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) (notice.getReceivedAt() % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(AppHelper.getColor(this, R.color.colorAccent))
                .setContentTitle(notice.getTitle())
                .setContentText(notice.getMessage())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Bitmap picture = loadNoticeImage(notice.getImage());
        if (picture != null) {
            builder.setLargeIcon(picture)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(picture)
                            .setSummaryText(notice.getMessage()));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notice.getMessage()));
        }

        manager.notify((int) (notice.getReceivedAt() % Integer.MAX_VALUE), builder.build());
    }

    private Intent buildNoticeIntent(DappNotice notice) {
        int type = parseNoticeType(notice.getType());
        Intent intent;
        if (type == TYPE_DIALOG) {
            intent = new Intent(this, com.money.mimi.activities.notifications.DappNoticeDialogActivity.class);
            intent.putExtra("title", notice.getTitle());
            intent.putExtra("msg", notice.getMessage());
            intent.putExtra("link", notice.getLink());
            intent.putExtra("image", notice.getImage());
        } else {
            intent = new Intent(this, com.money.mimi.activities.main.MainActivity.class);
            if (type == TYPE_WEB && isHttpUrl(notice.getLink())) {
                intent.putExtra("open_dapp_url", notice.getLink());
            } else if (type == TYPE_NEWS) {
                intent.putExtra("open_dapp_notifications", true);
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private boolean showDialogNoticeIfForeground(DappNotice notice) {
        Activity currentActivity = getForegroundActivity();
        if (currentActivity == null || currentActivity.isFinishing()) {
            return false;
        }
        try {
            Intent intent = buildNoticeIntent(notice);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            currentActivity.runOnUiThread(() -> {
                try {
                    currentActivity.startActivity(intent);
                } catch (Exception e) {
                    AppHelper.LogCat("FCM foreground dialog launch failed: " + e.getMessage());
                    showDappNoticeNotification(notice);
                }
            });
            return true;
        } catch (Exception e) {
            AppHelper.LogCat("FCM foreground dialog setup failed: " + e.getMessage());
            return false;
        }
    }

    private Activity getForegroundActivity() {
        try {
            ForegroundRuning foreground = ForegroundRuning.get();
            if (foreground.isForeground()) {
                return foreground.getCurrentActivity();
            }
        } catch (IllegalStateException e) {
            AppHelper.LogCat("FCM foreground tracker unavailable: " + e.getMessage());
        }
        return null;
    }

    private int parseNoticeType(String value) {
        try {
            int type = Integer.parseInt(value == null ? "" : value.trim());
            if (type >= TYPE_SIMPLE && type <= TYPE_NEWS) {
                return type;
            }
        } catch (Exception ignored) {
        }
        return TYPE_SIMPLE;
    }

    private boolean isHttpsUrl(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && !TextUtils.isEmpty(uri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isHttpUrl(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && !TextUtils.isEmpty(uri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private Bitmap loadNoticeImage(String imageUrl) {
        String normalizedImageUrl = DappNoticeStore.normalizeImageUrl(imageUrl);
        if (!isHttpUrl(normalizedImageUrl)) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(normalizedImageUrl).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(4000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                return BitmapFactory.decodeStream(connection.getInputStream());
            }
        } catch (Exception e) {
            AppHelper.LogCat("FCM notice image load failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
}
