package com.money.mimi.helpers.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.money.mimi.R;
import com.money.mimi.activities.main.MainActivity;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.activities.messages.MessagesPopupActivity;
import com.money.mimi.activities.settings.PreferenceSettingsManager;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.CropSquareTransformation;
import com.money.mimi.ui.TextDrawable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import me.leolin.shortcutbadger.ShortcutBadger;

import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;

/**
 * Created by Abderrahim El imame on 6/19/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class NotificationsManager {


    private static NotificationManager mNotificationManager;
    private static String username;
    // private int numMessages = 0;
    private static MemoryCache memoryCache;

    public NotificationsManager() {
    }

    @SuppressLint("StaticFieldLeak")
    public static void showUserNotification(Context mContext, int conversationID, String phone, String message, int userId, String Avatar) {

        if (AppHelper.isAndroid13() && ContextCompat.checkSelfPermission(mContext, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //for android O
        String CHANNEL_ID;
        NotificationChannel mChannel;
        //

        memoryCache = new MemoryCache();
        //  String text = UtilsString.unescapeJava(message);
        Intent messagingIntent = new Intent(mContext, MainActivity.class);
        messagingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent messagingPopupIntent = new Intent(mContext, MainActivity.class);
        messagingPopupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int counterUnreadConversation = getUnreadConversationsCounter();

        int counterUnreadMessages = getUnreadMessagesCounter();
        List<MessagesModel> msgs = getNotificationMessages(userId);
        String latestMessage = UtilsString.unescapeJava(message != null ? message : "");
        if (msgs == null || msgs.isEmpty()) {
            AppHelper.LogCat("showUserNotification: no unread message rows for user " + userId + ", using payload text");
        } else {
            MessagesModel latest = msgs.get(msgs.size() - 1);
            if (latest != null && latest.getMessage() != null) {
                latestMessage = UtilsString.unescapeJava(latest.getMessage());
            }
        }

        if (counterUnreadConversation == 1) {
            /**
             * this for default activity
             */
            messagingIntent = new Intent(mContext, MessagesActivity.class);
            messagingIntent.putExtra("conversationID", conversationID);
            messagingIntent.putExtra("recipientID", userId);
            messagingIntent.putExtra("isGroup", false);
            /**
             * this for popup activity
             */
            messagingPopupIntent = new Intent(mContext, MessagesPopupActivity.class);
            messagingPopupIntent.putExtra("conversationID", conversationID);
            messagingPopupIntent.putExtra("recipientID", userId);
            messagingPopupIntent.putExtra("isGroup", false);

        }


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack
        stackBuilder.addParentStack(MessagesActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(messagingIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        TaskStackBuilder stackPopupBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack

        stackPopupBuilder.addParentStack(MessagesPopupActivity.class);
        // Adds the Intent to the top of the stack
        stackPopupBuilder.addNextIntent(messagingPopupIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultMessagingPopupIntent = stackPopupBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        final NotificationCompat.Builder mNotifyBuilder;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        try {

            String walletAddress = null;
            if (msgs != null && !msgs.isEmpty() && msgs.get(0) != null) {
                walletAddress = msgs.get(0).getWalletAddress();
            }
            String name = UtilsPhone.getContactName(walletAddress);
            if (name != null) {
                username = name;
            } else {
                username = phone;
            }

        } catch (Exception e) {
            AppHelper.LogCat(" " + e.getMessage());
            username = phone;
        }
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (AppHelper.isAndroid8()) {

            CHANNEL_ID = username;// The id of the channel.
            CharSequence name = WhatsCloneApplication.getInstance().getString(R.string.app_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotifyBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setColor(AppHelper.getColor(mContext, R.color.colorAccent))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(resultPendingIntent)
                    .setChannelId(CHANNEL_ID);

            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            mNotifyBuilder = new NotificationCompat.Builder(mContext)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setColor(AppHelper.getColor(mContext, R.color.colorAccent))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(resultPendingIntent)
                    .setPriority(Notification.PRIORITY_HIGH);

        }

        if (counterUnreadConversation == 1) {
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_black_24dp, mContext.getString(R.string.reply_message), resultMessagingPopupIntent).build();
            mNotifyBuilder.addAction(action);

            inboxStyle.setBigContentTitle(username);

            mNotifyBuilder.setContentTitle(username);
            mNotifyBuilder.setContentText(latestMessage);
            inboxStyle.setSummaryText(counterUnreadMessages + " " + mContext.getString(R.string.new_messages_notify));
            if (msgs != null && !msgs.isEmpty()) {
                for (MessagesModel m : msgs) {
                    if (m != null) {
                        inboxStyle.addLine(UtilsString.unescapeJava(m.getMessage()));
                    }
                }
            } else {
                inboxStyle.addLine(latestMessage);
            }

        } else {
            inboxStyle.setBigContentTitle(mContext.getResources().getString(R.string.app_name));

            mNotifyBuilder.setContentTitle(username);
            mNotifyBuilder.setContentText(latestMessage);
            inboxStyle.setSummaryText(counterUnreadMessages + " " + mContext.getString(R.string.messages_from_notify) + " " + counterUnreadConversation + " " + mContext.getString(R.string.chats_notify));
            if (msgs != null && !msgs.isEmpty()) {
                for (MessagesModel m : msgs) {
                    if (m == null) {
                        continue;
                    }

                    if (m.getUsername() != null)
                        inboxStyle.addLine("".concat(m.getUsername()).concat(" : ").concat(UtilsString.unescapeJava(m.getMessage())));
                    else
                        inboxStyle.addLine("".concat(m.getWalletAddress()).concat(" : ").concat(UtilsString.unescapeJava(m.getMessage())));

                }
            } else {
                inboxStyle.addLine(latestMessage);
            }
        }
        mNotifyBuilder.setStyle(inboxStyle);
        TextDrawable drawable = textDrawable(mContext, username);
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = ImageLoader.GetCachedBitmapImage(memoryCache, Avatar, mContext, userId, AppConstants.USER, AppConstants.ROW_PROFILE);
                if (bitmap != null) {
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE, false);
                    Bitmap circleBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    BitmapShader shader = new BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    Paint paint = new Paint();
                    paint.setShader(shader);
                    paint.setAntiAlias(true);
                    Canvas c = new Canvas(circleBitmap);
                    c.drawCircle(scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2, scaledBitmap.getWidth() / 2, paint);
                    return circleBitmap;
                } else {
                    return null;
                }

            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    mNotifyBuilder.setLargeIcon(bitmap);
                } else {
                    Target target = new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            mNotifyBuilder.setLargeIcon(bitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {
                            Bitmap bitmap = convertToBitmap(drawable, AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE);
                            mNotifyBuilder.setLargeIcon(bitmap);
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            Bitmap bitmap = convertToBitmap(drawable, AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE);
                            mNotifyBuilder.setLargeIcon(bitmap);
                        }
                    };
                    Picasso.with(mContext)
                            .load(EndPoints.ROWS_IMAGE_URL + Avatar)
                            .transform(new CropSquareTransformation())
                            .resize(AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE)
                            .into(target);
                }
            }
        }.execute();


        if (PreferenceSettingsManager.conversation_tones(mContext)) {

            Uri uri = PreferenceSettingsManager.getDefault_message_notifications_settings_tone(mContext);
            if (uri != null)
                mNotifyBuilder.setSound(uri);
            else {
                int defaults = 0;
                defaults = defaults | Notification.DEFAULT_SOUND;
                mNotifyBuilder.setDefaults(defaults);
            }


        }

        if (PreferenceSettingsManager.getDefault_message_notifications_settings_vibrate(mContext)) {
            long[] vibrate = new long[]{2000, 2000, 2000, 2000, 2000};
            mNotifyBuilder.setVibrate(vibrate);
        } else {
            int defaults = 0;
            defaults = defaults | Notification.DEFAULT_VIBRATE;
            mNotifyBuilder.setDefaults(defaults);
        }


        String colorLight = PreferenceSettingsManager.getDefault_message_notifications_settings_light(mContext);
        if (colorLight != null) {
            mNotifyBuilder.setLights(Color.parseColor(colorLight), 1500, 1500);
        } else {
            int defaults = 0;
            defaults = defaults | Notification.DEFAULT_LIGHTS;
            mNotifyBuilder.setDefaults(defaults);
        }


        mNotifyBuilder.setAutoCancel(true);

        mNotificationManager.notify(userId, mNotifyBuilder.build());

        SetupBadger(mContext);
        EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));

    }

    private static Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
        Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, widthPixels, heightPixels);
        drawable.draw(canvas);

        return mutableBitmap;
    }

    private static TextDrawable textDrawable(Context context, String name) {
        if (name == null || name.isEmpty()) {
            name = context.getString(R.string.app_name);
        }
        ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
        // generate random color
        int color = generator.getColor(name);
        String c = String.valueOf(name.toUpperCase().charAt(0));
        return TextDrawable.builder().buildRound(c, color);


    }

    private static int getUnreadMessagesCounter() {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        ConversationsModel conversationsModel1 = realm.where(ConversationsModel.class).findFirst();
        int unRead = Integer.parseInt(conversationsModel1 != null ? conversationsModel1.getUnreadMessageCounter() : "0");
        if (!realm.isClosed()) realm.close();
        return unRead;
    }

    private static int getUnreadConversationsCounter() {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<ConversationsModel> conversationsModel1 = realm.where(ConversationsModel.class)
                .notEqualTo("UnreadMessageCounter", "0")
                .findAll();
        int counter = conversationsModel1.size();
        if (!realm.isClosed()) realm.close();
        return counter;
    }

    private static List<MessagesModel> getNotificationMessages(int userId) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MessagesModel> messagesModels = new RealmList<>();
        RealmResults<MessagesModel> messagesModel = realm.where(MessagesModel.class)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("senderID", userId).findAll();
        messagesModels.addAll(messagesModel);
        if (!realm.isClosed()) realm.close();
        return messagesModels;
    }


    @SuppressLint("StaticFieldLeak")
    public static void showGroupNotification(Context mContext, Intent resultIntent, Intent messagingGroupPopupIntent, String groupName, String message, int groupId, String Avatar) {

        if (AppHelper.isAndroid13() && ContextCompat.checkSelfPermission(mContext, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //for android O
        String CHANNEL_ID;
        NotificationChannel mChannel;
        //


        memoryCache = new MemoryCache();

        String text = UtilsString.unescapeJava(message);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack
        stackBuilder.addParentStack(MessagesActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        TaskStackBuilder stackGroupPopupBuilder = TaskStackBuilder.create(mContext);
        stackGroupPopupBuilder.addParentStack(MessagesPopupActivity.class);
        // Adds the Intent to the top of the stack
        stackGroupPopupBuilder.addNextIntent(messagingGroupPopupIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultMessagingGroupPopupIntent = stackGroupPopupBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        final NotificationCompat.Builder mNotifyBuilder;


        //   ++numMessages;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_black_24dp, mContext.getString(R.string.reply_message), resultMessagingGroupPopupIntent).build();
        if (AppHelper.isAndroid8()) {

            CHANNEL_ID = groupName;// The id of the channel.
            CharSequence name = WhatsCloneApplication.getInstance().getString(R.string.app_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotifyBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(action)
                    .setContentTitle(groupName)
                    .setContentText(text)
                    .setColor(AppHelper.getColor(mContext, R.color.colorAccent))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(resultPendingIntent)
                    .setChannelId(CHANNEL_ID);

            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            mNotifyBuilder = new NotificationCompat.Builder(mContext)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(action)
                    .setContentTitle(groupName)
                    .setContentText(text)
                    .setColor(AppHelper.getColor(mContext, R.color.colorAccent))
                    //  .setNumber(numMessages)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(resultPendingIntent)
                    .setPriority(Notification.PRIORITY_HIGH);
        }
        TextDrawable drawable = textDrawable(mContext, groupName);
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {

                Bitmap bitmap = ImageLoader.GetCachedBitmapImage(memoryCache, Avatar, mContext, groupId, AppConstants.USER, AppConstants.ROW_PROFILE);
                if (bitmap != null) {
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE, false);
                    Bitmap circleBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    BitmapShader shader = new BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    Paint paint = new Paint();
                    paint.setShader(shader);
                    paint.setAntiAlias(true);
                    Canvas c = new Canvas(circleBitmap);
                    c.drawCircle(scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2, scaledBitmap.getWidth() / 2, paint);
                    return circleBitmap;
                } else {
                    return null;
                }

            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    mNotifyBuilder.setLargeIcon(bitmap);
                } else {
                    Target target = new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            mNotifyBuilder.setLargeIcon(bitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {
                            Bitmap bitmap = convertToBitmap(drawable, AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE);
                            mNotifyBuilder.setLargeIcon(bitmap);
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            Bitmap bitmap = convertToBitmap(drawable, AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE);
                            mNotifyBuilder.setLargeIcon(bitmap);
                        }
                    };
                    Picasso.with(mContext)
                            .load(EndPoints.ROWS_IMAGE_URL + Avatar)
                            .transform(new CropSquareTransformation())
                            .resize(AppConstants.NOTIFICATIONS_IMAGE_SIZE, AppConstants.NOTIFICATIONS_IMAGE_SIZE)
                            .into(target);
                }
            }
        }.execute();
        mNotifyBuilder.setAutoCancel(true);


        if (PreferenceSettingsManager.conversation_tones(mContext)) {

            Uri uri = PreferenceSettingsManager.getDefault_message_group_notifications_settings_tone(mContext);
            if (uri != null)
                mNotifyBuilder.setSound(uri);
            else {
                int defaults = 0;
                defaults = defaults | Notification.DEFAULT_SOUND;
                mNotifyBuilder.setDefaults(defaults);
            }


        }

        if (PreferenceSettingsManager.getDefault_message_group_notifications_settings_vibrate(mContext)) {
            long[] vibrate = new long[]{2000, 2000, 2000, 2000, 2000};
            mNotifyBuilder.setVibrate(vibrate);
        } else {
            int defaults = 0;
            defaults = defaults | Notification.DEFAULT_VIBRATE;
            mNotifyBuilder.setDefaults(defaults);
        }


        String colorLight = PreferenceSettingsManager.getDefault_message_group_notifications_settings_light(mContext);
        if (colorLight != null) {
            mNotifyBuilder.setLights(Color.parseColor(colorLight), 1500, 1500);
        } else {
            int defaults = 0;
            defaults = defaults | Notification.DEFAULT_LIGHTS;
            mNotifyBuilder.setDefaults(defaults);
        }


        mNotificationManager.notify(groupId, mNotifyBuilder.build());

    }

    /**
     * method to get manager for notification
     */
    public static boolean getManager() {
        if (mNotificationManager != null) {
            return true;
        } else {
            return false;
        }

    }

    /***
     * method to cancel a specific notification
     *
     * @param index
     */
    public static void cancelNotification(int index) {
        //    numMessages = 0;
        mNotificationManager.cancel(index);
    }

    /**
     * method to set badger counter for the app
     */
    public static void SetupBadger(Context mContext) {

        int messageBadgeCounter = 0;
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        String DeviceName = android.os.Build.MANUFACTURER;
        String[] DevicesName = {
                "Sony",
                "Samsung",
                "LG",
                "HTC",
                "Xiaomi",
                "ASUS",
                "ADW",
                "NOVA",
                "Huawei",
                "ZUK",
                "APEX",
                "OPPO",
                "ZTE",
                "EverythingMe"
        };

        for (String device : DevicesName) {
            if (DeviceName.equals(device.toLowerCase())) {
                try {
                    List<MessagesModel> messagesModels = realm.where(MessagesModel.class)
                            .notEqualTo("id", 0)
                            .equalTo("status", AppConstants.IS_WAITING)
                            .notEqualTo("senderID", PreferenceManager.getID(mContext))
                            .findAll();

                    if (messagesModels.size() != 0) {
                        messageBadgeCounter = messagesModels.size();
                    }
                    try {
                        ShortcutBadger.applyCount(mContext.getApplicationContext(), messageBadgeCounter);
                    } catch (Exception e) {
                        AppHelper.LogCat(" ShortcutBadger Exception " + e.getMessage());
                    }
                } catch (Exception e) {
                    AppHelper.LogCat(" ShortcutBadger Exception " + e.getMessage());
                }
                break;
            }
        }
        if (!realm.isClosed())
            realm.close();

    }

    private static final String CALL_CHANNEL_ID = "incoming_call_channel_v3";
    private static final int CALL_NOTIFICATION_ID = 20001;
    private static final String ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT =
            "android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT";

    public static void showIncomingCallNotification(Context mContext, String callerSocketId,
                                                     int callerID, String callerPhone,
                                                     String callerImage, boolean isVideoCall) {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CALL_CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Incoming voice and video calls");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }

        Intent callIntent = new Intent(mContext, com.money.mimi.activities.call.IncomingCallActivity.class);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        callIntent.putExtra(AppConstants.CALLER_SOCKET_ID, callerSocketId);
        callIntent.putExtra(AppConstants.USER_SOCKET_ID, PreferenceManager.getSocketID(mContext));
        callIntent.putExtra(AppConstants.CALLER_PHONE, callerPhone != null ? callerPhone : "");
        callIntent.putExtra(AppConstants.CALLER_IMAGE, callerImage != null ? callerImage : "null");
        callIntent.putExtra(AppConstants.CALLER_ID, callerID);
        callIntent.putExtra(AppConstants.IS_VIDEO_CALL, isVideoCall);
        callIntent.putExtra(AppConstants.USER_PHONE, PreferenceManager.getWalletAddress(mContext));

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                mContext, 0, callIntent, pendingIntentFlags
        );
        Intent answerIntent = new Intent(callIntent);
        answerIntent.putExtra(AppConstants.CALL_NOTIFICATION_ACTION, AppConstants.CALL_ACTION_ANSWER);
        PendingIntent answerPendingIntent = PendingIntent.getActivity(
                mContext, 1, answerIntent, pendingIntentFlags
        );
        Intent rejectIntent = new Intent(callIntent);
        rejectIntent.putExtra(AppConstants.CALL_NOTIFICATION_ACTION, AppConstants.CALL_ACTION_REJECT);
        PendingIntent rejectPendingIntent = PendingIntent.getActivity(
                mContext, 2, rejectIntent, pendingIntentFlags
        );

        String callType = isVideoCall ? "Video call" : "Voice call";
        String callerAddress = callerPhone != null && !callerPhone.trim().isEmpty() ? callerPhone : "Unknown address";
        String callerName = resolveCallerName(callerAddress);
        String title = callType + ": " + callerName;
        String text = callerAddress.equals(callerName) ? "Incoming call" : callerAddress;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CALL_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(callType + ", " + callerName + "\n" + callerAddress))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(fullScreenPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .addAction(R.drawable.ic_call_white_24dp, "Answer", answerPendingIntent)
                .addAction(R.drawable.ic_call_end_white_24dp, "Decline", rejectPendingIntent);

        if (!canUseFullScreenIntent(mContext)) {
            AppHelper.LogCat("Incoming call full-screen intent is blocked by Android settings");
        }

        // If app is already foregrounded, launch the call UI immediately as well.
        if (isAppForeground()) {
            try {
                mContext.startActivity(callIntent);
            } catch (Exception e) {
                AppHelper.LogCat("Failed to launch IncomingCallActivity directly: " + e.getMessage());
            }
        }

        notificationManager.notify(CALL_NOTIFICATION_ID, builder.build());
    }

    public static boolean canUseFullScreenIntent(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return true;
        }
        if (android.os.Build.VERSION.SDK_INT < 34) {
            return true;
        }
        try {
            Object result = NotificationManager.class
                    .getMethod("canUseFullScreenIntent")
                    .invoke(notificationManager);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            AppHelper.LogCat("Unable to check full-screen intent permission: " + e.getMessage());
            return true;
        }
    }

    public static Intent buildFullScreenIntentSettingsIntent(Context context) {
        Intent intent = new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            return intent;
        }

        Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        fallbackIntent.setData(Uri.parse("package:" + context.getPackageName()));
        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return fallbackIntent;
    }

    private static boolean isAppForeground() {
        try {
            return com.money.mimi.helpers.ForegroundRuning.get().isForeground();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static String resolveCallerName(String callerAddress) {
        if (callerAddress == null || callerAddress.trim().isEmpty()) {
            return "Unknown caller";
        }
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            ContactsModel contact = realm.where(ContactsModel.class)
                    .equalTo("walletAddress", callerAddress)
                    .findFirst();
            if (contact != null && contact.getUsername() != null && !contact.getUsername().trim().isEmpty()) {
                return contact.getUsername();
            }
        } catch (Exception e) {
            AppHelper.LogCat("resolveCallerName failed: " + e.getMessage());
        } finally {
            if (!realm.isClosed()) {
                realm.close();
            }
        }
        return callerAddress;
    }

    public static void cancelIncomingCallNotification(Context mContext) {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(CALL_NOTIFICATION_ID);
        }
    }
}
