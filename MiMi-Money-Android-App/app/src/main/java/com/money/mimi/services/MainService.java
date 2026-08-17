package com.money.mimi.services;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.money.mimi.R;
import com.money.mimi.activities.call.IncomingCallActivity;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.activities.messages.MessagesPopupActivity;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.messages.UpdateMessageModel;
import com.money.mimi.models.notifications.NotificationsModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.models.users.contacts.UsersBlockModel;
import com.money.mimi.models.users.status.StatusResponse;
import com.money.mimi.receivers.MessagesReceiverBroadcast;
import com.money.mimi.services.firebase.PendingPushStore;
import com.money.mimi.services.sync.BackgroundSyncScheduler;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS;
import static com.money.mimi.app.AppConstants.EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS;


/**
 * Created by Abderrahim El imame on 6/21/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class MainService extends Service {

    private static final String CHANNEL_ID = "main_service_background_channel_v2";
    private static final int NOTIFICATION_ID = 10001;
    
    private Context mContext;
    public static Socket mSocket;
    private MessagesReceiverBroadcast mChangeListener;
    private Intent mIntent;
    private static Handler handler;
    private int mTries = 0;
    private boolean mFallbackAttempted = false;
    private boolean mContactSyncAttempted = false;
    private boolean mReceiverRegistered = false;
    private volatile boolean mStopping = false;
    private volatile boolean mConnecting = false;
    private volatile boolean mReconnectScheduled = false;

    private static final int MAX_RECONNECT_TRIES = 8;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 30000;
    private static final double BACKOFF_MULTIPLIER = 1.5;
    private static final long CONTACT_SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000L;
    private static final String SYNC_PREFS = "mimi_sync_meta";
    private static final String LAST_CONTACT_SYNC = "last_contact_sync";
    private final Random mRandom = new Random();

    public MainService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        handler = new Handler();
        AppHelper.LogCat("MainService started for foreground application session");
        
        connectToServer(mContext);
        setupBroadcastReceiver();
        drainPendingPushes();
    }

    private void drainPendingPushes() {
        for (JSONObject item : PendingPushStore.drain(this)) {
            JSONObject payload = item.optJSONObject("payload");
            if (payload == null) continue;
            try {
                if (item.optBoolean("group", false)) {
                    payload.put("groupId", payload.optInt("groupID", payload.optInt("groupId", 0)));
                    payload.put("senderPhone", payload.optString("walletAddress", ""));
                    saveNewMessageGroup(payload);
                } else {
                    payload.put("recipientId", payload.optInt("recipientID", payload.optInt("recipientId", 0)));
                    saveNewMessage(payload);
                }
            } catch (JSONException e) {
                AppHelper.LogCat("Unable to drain pending FCM payload: " + e.getMessage());
            }
        }
    }
    
    private void setupBroadcastReceiver() {
        mChangeListener = new MessagesReceiverBroadcast() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            protected void MessageReceived(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case "new_user_message_notification_whatsclone":
                        handler.postDelayed(() -> {
                            String Application = intent.getExtras().getString("app");
                            String file = intent.getExtras().getString("file");
                            String userphone = intent.getExtras().getString("walletAddress");
                            String messageBody = intent.getExtras().getString("message");
                            int recipientId = intent.getExtras().getInt("recipientID");
                            int senderId = intent.getExtras().getInt("senderId");
                            int conversationID = intent.getExtras().getInt("conversationID");
                            String userImage = intent.getExtras().getString("userImage");

                            if (Application != null && Application.equals(mContext.getPackageName())) {
                                if (AppHelper.isActivityRunning(mContext, "activities.messages.MessagesActivity")) {
                                    NotificationsModel notificationsModel = new NotificationsModel();
                                    notificationsModel.setConversationID(conversationID);
                                    notificationsModel.setFile(file);
                                    notificationsModel.setGroup(false);
                                    notificationsModel.setImage(userImage);
                                    notificationsModel.setWalletAddress(userphone);
                                    notificationsModel.setMessage(messageBody);
                                    notificationsModel.setRecipientId(recipientId);
                                    notificationsModel.setSenderId(senderId);
                                    notificationsModel.setAppName(Application);
                                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_USER_NOTIFICATION, notificationsModel));
                                } else {
                                    if (file != null) {
                                        NotificationsManager.showUserNotification(mContext, conversationID, userphone, file, recipientId, userImage);
                                    } else {
                                        NotificationsManager.showUserNotification(mContext, conversationID, userphone, messageBody, recipientId, userImage);
                                    }
                                }
                            }
                        }, 500);
                        break;
                    case "new_group_message_notification_whatsclone":
                        String application = intent.getExtras().getString("app");
                        String File = intent.getExtras().getString("file");
                        String userPhone = intent.getExtras().getString("senderPhone");
                        String groupName = UtilsString.unescapeJava(intent.getExtras().getString("groupName"));
                        String messageGroupBody = intent.getExtras().getString("message");
                        int groupID = intent.getExtras().getInt("groupID");
                        String groupImage = intent.getExtras().getString("groupImage");
                        int conversationId = intent.getExtras().getInt("conversationID");
                        String memberName;
                        String name = UtilsPhone.getContactName(userPhone);
                        if (name != null) {
                            memberName = name;
                        } else {
                            memberName = userPhone;
                        }
                        String message;
                        String userName = UtilsPhone.getContactName(userPhone);
                        switch (messageGroupBody) {
                            case AppConstants.CREATE_GROUP:
                                if (userName != null) {
                                    message = "" + userName + mContext.getString(R.string.he_created_this_group);
                                } else {
                                    message = "" + userPhone + mContext.getString(R.string.he_created_this_group);
                                }
                                break;
                            case AppConstants.LEFT_GROUP:
                                if (userName != null) {
                                    message = "" + userName + mContext.getString(R.string.he_left);
                                } else {
                                    message = "" + userPhone + mContext.getString(R.string.he_left);
                                }
                                break;
                            default:
                                message = messageGroupBody;
                                break;
                        }
                        Intent messagingGroupIntent = new Intent(mContext, MessagesActivity.class);
                        messagingGroupIntent.putExtra("conversationID", conversationId);
                        messagingGroupIntent.putExtra("groupID", groupID);
                        messagingGroupIntent.putExtra("isGroup", true);
                        messagingGroupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Intent messagingGroupPopupIntent = new Intent(mContext, MessagesPopupActivity.class);
                        messagingGroupPopupIntent.putExtra("conversationID", conversationId);
                        messagingGroupPopupIntent.putExtra("groupID", groupID);
                        messagingGroupPopupIntent.putExtra("isGroup", true);
                        messagingGroupPopupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (application != null && application.equals(mContext.getPackageName())) {
                            if (AppHelper.isActivityRunning(mContext, "activities.messages.MessagesActivity")) {
                                NotificationsModel notificationsModel = new NotificationsModel();
                                notificationsModel.setConversationID(conversationId);
                                notificationsModel.setFile(File);
                                notificationsModel.setGroup(true);
                                notificationsModel.setImage(groupImage);
                                notificationsModel.setWalletAddress(userPhone);
                                notificationsModel.setMessage(messageGroupBody);
                                notificationsModel.setMemberName(memberName);
                                notificationsModel.setGroupID(groupID);
                                notificationsModel.setGroupName(groupName);
                                notificationsModel.setAppName(application);
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_GROUP_NOTIFICATION, notificationsModel));
                            } else {
                                if (File != null) {
                                    NotificationsManager.showGroupNotification(mContext, messagingGroupIntent, messagingGroupPopupIntent, groupName, memberName + " : " + File, groupID, groupImage);
                                } else {
                                    NotificationsManager.showGroupNotification(mContext, messagingGroupIntent, messagingGroupPopupIntent, groupName, memberName + " : " + message, groupID, groupImage);
                                }
                            }
                        }
                        break;
                    case "new_user_joined_notification_whatsclone":
                        String Userphone = intent.getExtras().getString("walletAddress");
                        String MessageBody = intent.getExtras().getString("message");
                        int RecipientId = intent.getExtras().getInt("recipientID");
                        int ConversationID = intent.getExtras().getInt("conversationID");
                        NotificationsManager.showUserNotification(mContext, ConversationID, Userphone, MessageBody, RecipientId, null);
                        break;
                }
            }
        };

        try {
            IntentFilter notificationsFilter = new IntentFilter();
            notificationsFilter.addAction("new_user_message_notification_whatsclone");
            notificationsFilter.addAction("new_group_message_notification_whatsclone");
            notificationsFilter.addAction("new_user_joined_notification_whatsclone");
            ContextCompat.registerReceiver(
                    getApplication(), mChangeListener, notificationsFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);
            mReceiverRegistered = true;
        } catch (Exception e) {
            AppHelper.LogCat("registerReceiver failed: " + e.getMessage());
            mReceiverRegistered = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_service_name),
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Background service for messaging and calls");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_connected))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setOngoing(true)
                .setLocalOnly(true)
                .setSilent(true)
                .setShowWhen(false);
        return builder.build();
    }

    /**
     * method to disconnect user form server
     */
    public static void disconnectSocket() {

        if (mSocket != null) {
            mSocket.off(Socket.EVENT_CONNECT);
            mSocket.off(Socket.EVENT_DISCONNECT);
            mSocket.off(Socket.EVENT_CONNECT_ERROR);
            mSocket.io().off(io.socket.client.Manager.EVENT_RECONNECT);

            mSocket.off(AppConstants.SOCKET_IS_ONLINE);
            //user messages

            mSocket.off(AppConstants.SOCKET_SAVE_NEW_MESSAGE);
            mSocket.off(AppConstants.SOCKET_NEW_MESSAGE_SERVER);
            mSocket.off(AppConstants.SOCKET_NEW_MESSAGE_GROUP_SERVER);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_DELIVERED);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_SEEN);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_DOWNLOADED);
            mSocket.off(AppConstants.SOCKET_IS_STOP_TYPING);
            mSocket.off(AppConstants.SOCKET_IS_TYPING);
            mSocket.off(AppConstants.SOCKET_CONNECTED);
            mSocket.off(AppConstants.SOCKET_DISCONNECTED);
            mSocket.off(AppConstants.SOCKET_NEW_USER_JOINED);
            mSocket.off(AppConstants.SOCKET_IMAGE_PROFILE_UPDATED);
            mSocket.off(AppConstants.SOCKET_IMAGE_GROUP_UPDATED);
            //groups
            mSocket.off(AppConstants.SOCKET_IS_MEMBER_STOP_TYPING);
            mSocket.off(AppConstants.SOCKET_IS_MEMBER_TYPING);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN);

            //calls — only off() SOCKET_RECEIVE_NEW_CALL (managed by MainService);
            //all other call listeners are managed by WebRtcClient and CallManager
            mSocket.off(AppConstants.SOCKET_RECEIVE_NEW_CALL);


            mSocket.disconnect();
            mSocket.close();
            mSocket = null;

        }
        AppHelper.LogCat("disconnect in service");
    }

    /**
     * method for server connection initialization
     */
    public void connectToServer(Context mContext) {
        if (mStopping || mConnecting || (mSocket != null && mSocket.connected())) return;
        mConnecting = true;
        WhatsCloneApplication.connectSocket();
        mSocket = WhatsCloneApplication.getInstance().getSocket();

        if (mSocket == null) {
            mConnecting = false;
            AppHelper.LogCat("Socket creation failed; using bounded reconnect policy");
            reconnect(mContext);
            return;
        }

        mFallbackAttempted = false;
        mSocket.connect();

        mSocket.once(Socket.EVENT_CONNECT, args -> {
            mConnecting = false;
            mTries = 0;
            mFallbackAttempted = false;
            WhatsCloneApplication.saveCurrentServerUrl();
            AppHelper.LogCat("New Connection chat is created " + mSocket.id() + " via " + WhatsCloneApplication.getCurrentServerUrl());

            registerSocketEventHandlers();
            syncDeviceWalletContacts(false);
            unSentMessages(mContext);

            if (mSocket.id() != null) {
                PreferenceManager.setSocketID(this, mSocket.id());

                JSONObject json = new JSONObject();
                try {
                    json.put("connected", true);
                    json.put("connectedId", PreferenceManager.getID(mContext));
                    json.put("userToken", PreferenceManager.getToken(mContext));
                    json.put("socketId", PreferenceManager.getSocketID(mContext));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mSocket != null)
                    mSocket.emit(AppConstants.SOCKET_CONNECTED, json);
            } else {
                reconnect(mContext);
            }
        }).on(Socket.EVENT_CONNECT_ERROR, args -> {
            mConnecting = false;
            AppHelper.LogCat("Socket EVENT_CONNECT_ERROR on " + WhatsCloneApplication.getCurrentServerUrl());
            for (Object o : args) {
                if (o != null) AppHelper.LogCat("connect error detail: " + o.toString());
            }

            if (!mFallbackAttempted) {
                mFallbackAttempted = true;
                String currentUrl = WhatsCloneApplication.getCurrentServerUrl();
                String fallbackUrl = WhatsCloneApplication.getFallbackServerUrl(currentUrl);
                AppHelper.LogCat("Attempting fallback to: " + fallbackUrl);

                mSocket.off();
                mSocket.disconnect();
                mSocket.close();

                WhatsCloneApplication.connectSocketWithUrl(fallbackUrl);
                mSocket = WhatsCloneApplication.getInstance().getSocket();

                if (mSocket != null) {
                    mConnecting = true;
                    mSocket.connect();
                    mSocket.once(Socket.EVENT_CONNECT, args2 -> {
                        mConnecting = false;
                        mTries = 0;
                        mFallbackAttempted = false;
                        WhatsCloneApplication.saveCurrentServerUrl();
                        AppHelper.LogCat("Fallback connected " + mSocket.id() + " via " + WhatsCloneApplication.getCurrentServerUrl());

                        if (mSocket.id() != null) {
                            PreferenceManager.setSocketID(MainService.this, mSocket.id());
                            JSONObject json = new JSONObject();
                            try {
                                json.put("connected", true);
                                json.put("connectedId", PreferenceManager.getID(mContext));
                                json.put("userToken", PreferenceManager.getToken(mContext));
                                json.put("socketId", PreferenceManager.getSocketID(mContext));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mSocket.emit(AppConstants.SOCKET_CONNECTED, json);
                        }
                    }).on(Socket.EVENT_CONNECT_ERROR, args2 -> {
                        mConnecting = false;
                        AppHelper.LogCat("Fallback also failed on " + fallbackUrl);
                        reconnect(mContext);
                    }).on(Socket.EVENT_DISCONNECT, args2 -> {
                        AppHelper.LogCat("Fallback connection lost " + mSocket.id());
                        try {
                            JSONObject jsonConnected = new JSONObject();
                            jsonConnected.put("connectedId", PreferenceManager.getID(mContext));
                            jsonConnected.put("userToken", PreferenceManager.getToken(mContext));
                            jsonConnected.put("socketId", PreferenceManager.getSocketID(mContext));
                            if (mSocket != null)
                                mSocket.emit(AppConstants.SOCKET_DISCONNECTED, jsonConnected);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    mSocket.io().on(io.socket.client.Manager.EVENT_RECONNECT, args2 -> {
                        AppHelper.LogCat("Manager EVENT_RECONNECT (fallback)");
                        reconnect(mContext);
                    });

                    registerSocketEventHandlers();
                } else {
                    reconnect(mContext);
                }
            } else {
                reconnect(mContext);
            }
        }).on(Socket.EVENT_DISCONNECT, args -> {
            mConnecting = false;
            AppHelper.LogCat("You  lost connection to chat server " + (mSocket != null ? mSocket.id() : "null"));

            JSONObject jsonConnected = new JSONObject();
            try {
                jsonConnected.put("connectedId", PreferenceManager.getID(mContext));
                jsonConnected.put("userToken", PreferenceManager.getToken(mContext));
                jsonConnected.put("socketId", PreferenceManager.getSocketID(mContext));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mSocket != null)
                mSocket.emit(AppConstants.SOCKET_DISCONNECTED, jsonConnected);
            if (!mStopping && !mFallbackAttempted) reconnect(mContext);
        });
         mSocket.io().on(io.socket.client.Manager.EVENT_RECONNECT, args -> {
             AppHelper.LogCat("Manager EVENT_RECONNECT ");
             reconnect(mContext);
         });

    }

    private void registerSocketEventHandlers() {
        unregisterSocketEventHandlers();
        SenderMarkMessageAsDelivered();
        SenderMarkMessageAsSeen();
        SenderMarkMessageAsDownloaded();
        MemberMarkMessageAsDelivered();
        notifyOtherUser();
        getNotifyFromOtherNewUser();
        getNotifyForImageProfileChanged();
        onReceiveNewCall();

        isUserConnected(mContext);
        checkIfUserIsOnline();
        setTypingEvent();
        onReceiveNewMessage();
    }

    private void unregisterSocketEventHandlers() {
        if (mSocket == null) return;
        mSocket.off(AppConstants.SOCKET_IS_MESSAGE_DELIVERED);
        mSocket.off(AppConstants.SOCKET_IS_MESSAGE_SEEN);
        mSocket.off(AppConstants.SOCKET_IS_MESSAGE_DOWNLOADED);
        mSocket.off(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED);
        mSocket.off(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN);
        mSocket.off(AppConstants.SOCKET_IS_ONLINE);
        mSocket.off(AppConstants.SOCKET_NEW_USER_JOINED);
        mSocket.off(AppConstants.SOCKET_IMAGE_PROFILE_UPDATED);
        mSocket.off(AppConstants.SOCKET_IMAGE_GROUP_UPDATED);
        // NOTE: Call-related listeners (SOCKET_CALL_USER_PING, SOCKET_RESET_SOCKET_ID,
        // SOCKET_SIGNALING_SERVER, SOCKET_MAKE_NEW_CALL, SOCKET_REJECT_NEW_CALL,
        // SOCKET_ACCEPT_NEW_CALL, SOCKET_HANGUP_CALL) are managed by WebRtcClient
        // and CallManager. Do NOT off() them here or active calls will break on reconnect.
        mSocket.off(AppConstants.SOCKET_RECEIVE_NEW_CALL);
        mSocket.off(AppConstants.SOCKET_IS_TYPING);
        mSocket.off(AppConstants.SOCKET_IS_STOP_TYPING);
        mSocket.off(AppConstants.SOCKET_IS_MEMBER_TYPING);
        mSocket.off(AppConstants.SOCKET_IS_MEMBER_STOP_TYPING);
        mSocket.off(AppConstants.SOCKET_SAVE_NEW_MESSAGE);
        mSocket.off(AppConstants.SOCKET_NEW_MESSAGE_SERVER);
        mSocket.off(AppConstants.SOCKET_NEW_MESSAGE_GROUP_SERVER);
        mSocket.off(AppConstants.SOCKET_CONNECTED);
        mSocket.off(AppConstants.SOCKET_DISCONNECTED);
    }

    private void syncDeviceWalletContacts(boolean force) {
        if (mContactSyncAttempted) return;
        long lastSync = getSharedPreferences(SYNC_PREFS, MODE_PRIVATE)
                .getLong(LAST_CONTACT_SYNC, 0L);
        if (!force && System.currentTimeMillis() - lastSync < CONTACT_SYNC_INTERVAL_MS) {
            return;
        }
        mContactSyncAttempted = true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            AppHelper.LogCat("syncDeviceWalletContacts: READ_CONTACTS not granted");
            return;
        }

        List<ContactsModel> contacts = new ArrayList<>();
        for (ContactsModel contact : UtilsPhone.GetPhoneContacts()) {
            addWalletContactIfMissing(contacts, contact);
        }

        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            List<ContactsModel> walletContacts = realm.copyFromRealm(
                    realm.where(ContactsModel.class)
                            .equalTo("Exist", true)
                            .beginGroup()
                            .beginsWith("walletAddress", "0x", io.realm.Case.INSENSITIVE)
                            .or()
                            .beginsWith("walletAddressTmp", "0x", io.realm.Case.INSENSITIVE)
                            .endGroup()
                            .findAll()
            );
            for (ContactsModel contact : walletContacts) {
                addWalletContactIfMissing(contacts, contact);
            }
        } catch (Exception e) {
            AppHelper.LogCat("syncDeviceWalletContacts local read failed: " + e.getMessage());
        } finally {
            if (!realm.isClosed()) realm.close();
        }

        if (contacts.isEmpty()) {
            AppHelper.LogCat("syncDeviceWalletContacts: no wallet contacts to sync");
            return;
        }

        APIHelper.initialApiUsersContacts().updateContacts(contacts).subscribe(
                contactsModelList -> {
                    getSharedPreferences(SYNC_PREFS, MODE_PRIVATE).edit()
                            .putLong(LAST_CONTACT_SYNC, System.currentTimeMillis()).apply();
                    AppHelper.LogCat("syncDeviceWalletContacts: synced " + contacts.size() + " wallet contacts, server returned " + contactsModelList.size());
                    unSentMessages(MainService.this);
                },
                throwable -> AppHelper.LogCat("syncDeviceWalletContacts failed: " + throwable.getMessage())
        );
    }

    private void addWalletContactIfMissing(List<ContactsModel> contacts, ContactsModel contact) {
        if (contact == null || contact.getWalletAddress() == null) return;
        String walletAddress = contact.getWalletAddress().trim();
        if (!walletAddress.matches("^0x[a-fA-F0-9]{40}$")) return;
        for (ContactsModel existing : contacts) {
            if (existing.getWalletAddress() != null
                    && existing.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                return;
            }
        }
        ContactsModel syncModel = new ContactsModel();
        syncModel.setWalletAddress(walletAddress);
        syncModel.setWalletAddressTmp(contact.getWalletAddressTmp() != null ? contact.getWalletAddressTmp() : walletAddress);
        syncModel.setUsername(contact.getUsername());
        syncModel.setFirstName(contact.getFirstName());
        syncModel.setLastName(contact.getLastName());
        syncModel.setCategory(contact.getCategory());
        syncModel.setImage(contact.getImage());
        syncModel.setExist(true);
        syncModel.setLinked(true);
        syncModel.setActivate(true);
        contacts.add(syncModel);
    }

    /**
     * method to reconnect sockets with exponential backoff
     * Retries up to MAX_RECONNECT_TRIES (1000) times with increasing delay.
     * Delay formula: min(INITIAL_BACKOFF_MS * MULTIPLIER^attempt, MAX_BACKOFF_MS) +/- jitter
     */
    public void reconnect(Context mContext) {
        if (mStopping || mReconnectScheduled) return;
        if (mTries < MAX_RECONNECT_TRIES) {
            mTries++;
            mReconnectScheduled = true;
            double exponentialDelay = INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, mTries - 1);
            long delayMs = (long) Math.min(exponentialDelay, MAX_BACKOFF_MS);
            long jitter = (long) (delayMs * 0.25 * (mRandom.nextDouble() * 2 - 1));
            delayMs = Math.max(INITIAL_BACKOFF_MS, delayMs + jitter);
            AppHelper.LogCat("Reconnect attempt " + mTries + "/" + MAX_RECONNECT_TRIES + " in " + delayMs + "ms");
            handler.postDelayed(() -> {
                mReconnectScheduled = false;
                if (mStopping) return;
                connectToServer(mContext);
                handler.postDelayed(() -> updateStatusDeliveredOffline(mContext), 1500);
            }, delayMs);
        } else {
            AppHelper.LogCat("Max reconnect attempts (" + MAX_RECONNECT_TRIES + ") reached, resetting socket preference");
            WhatsCloneApplication.resetSocketPreference();
            stopSelf();
        }

    }

    /**
     * method to receive notification if a new user Joined
     */
    private void getNotifyFromOtherNewUser() {
        mSocket.on(AppConstants.SOCKET_NEW_USER_JOINED, args -> {
            final JSONObject jsonObject = firstJson(args);
            if (jsonObject == null) return;
            try {
                int senderId = parseIntField(jsonObject, "userId");
                if (senderId == 0) senderId = parseIntField(jsonObject, "senderId");
                String phone = jsonObject.optString("walletAddress", "");
                String name = jsonObject.optString("name", "");
                if (senderId == PreferenceManager.getID(mContext)) return;
                if (UtilsPhone.checkIfContactExist(mContext, phone)) {
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_USER_JOINED, jsonObject));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean checkIfGroupExist(int groupId, Realm realm) {
        RealmQuery<GroupsModel> query = realm.where(GroupsModel.class).equalTo("id", groupId);
        return query.count() != 0;

    }


    /**
     * method when a user change the image profile
     */
    private void getNotifyForImageProfileChanged() {
        mSocket.on(AppConstants.SOCKET_IMAGE_PROFILE_UPDATED, args -> {
            final JSONObject jsonObject = firstJson(args);
            if (jsonObject == null) return;
            try {
                int senderId = parseIntField(jsonObject, "userId");
                if (senderId == 0) senderId = parseIntField(jsonObject, "senderId");
                String phone = jsonObject.optString("walletAddress", "");
                if (senderId == PreferenceManager.getID(mContext)) return;
                if (UtilsPhone.checkIfContactExist(mContext, phone)) {
                    APIHelper.initialApiUsersContacts().getContactInfo(senderId).subscribe(contactsModel -> {
                        AppHelper.LogCat("contactsModel " + contactsModel.toString());
                        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                        int ConversationID = getConversationId(contactsModel.getId(), PreferenceManager.getID(mContext), realm);
                        if (ConversationID != 0) {
                            realm.executeTransaction(realm1 -> {
                                ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                                conversationsModel.setRecipientImage(contactsModel.getImage());
                                conversationsModel.setRecipientUsername(contactsModel.getUsername());
                                realm1.copyToRealmOrUpdate(conversationsModel);
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                            });
                        }

                        if (!realm.isClosed())
                            realm.close();
                    }, throwable -> {
                        AppHelper.LogCat("" + throwable.getMessage());
                    });

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        mSocket.on(AppConstants.SOCKET_IMAGE_GROUP_UPDATED, args -> {
            final JSONObject jsonObject = firstJson(args);
            if (jsonObject == null) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            try {
                int groupId = jsonObject.getInt("groupId");
                if (!checkIfGroupExist(groupId, realm)) return;
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_IMAGE_GROUP_UPDATED, groupId));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!realm.isClosed()) realm.close();
        });
    }

    /**
     * method to send notification if i join to the app
     */
    private void notifyOtherUser() {
        if (PreferenceManager.isNewUser(mContext)) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("senderId", PreferenceManager.getID(mContext));
                jsonObject.put("walletAddress", PreferenceManager.getWalletAddress(mContext));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit(AppConstants.SOCKET_NEW_USER_JOINED, jsonObject);
            PreferenceManager.setIsNewUser(mContext, false);
        }
    }


    /**
     * method to check if user is online or not
     */
    private void checkIfUserIsOnline() {

        if (mSocket != null) {
            mSocket.on(AppConstants.SOCKET_IS_ONLINE, args -> {
                final JSONObject data = firstJson(args);
                if (data == null) return;
                try {
                    int senderID = parseIntField(data, "senderId");
                    if (senderID == 0) return;
                    if (senderID == PreferenceManager.getID(mContext)) return;
                    if (data.getBoolean("connected")) {
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_USER_STATE, AppConstants.EVENT_BUS_USER_IS_ONLINE, senderID));
                    } else {
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_USER_STATE, AppConstants.EVENT_BUS_USER_IS_OFFLINE, senderID));
                    }
                } catch (JSONException e) {
                    AppHelper.LogCat(e);
                }
            });

        }
    }

    private void setTypingEvent() {

        mSocket.on(AppConstants.SOCKET_IS_TYPING, args -> {
            AppHelper.LogCat("SOCKET_IS_TYPING ");
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {

                int senderID = parseIntField(data, "senderId");
                int recipientID = parseIntField(data, "recipientId");
                if (senderID == 0 || recipientID == 0) return;
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_USER_TYPING, recipientID, senderID));
            } catch (Exception e) {
                AppHelper.LogCat(e);
            }
        }).on(AppConstants.SOCKET_IS_STOP_TYPING, args -> {
            AppHelper.LogCat("SOCKET_IS_STOP_TYPING ");
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                int senderID = parseIntField(data, "senderId");
                int recipientID = parseIntField(data, "recipientId");
                if (senderID == 0 || recipientID == 0) return;
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_USER_STOP_TYPING, recipientID, senderID));
            } catch (Exception e) {
                AppHelper.LogCat(e);
            }
        }).on(AppConstants.SOCKET_IS_MEMBER_TYPING, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                int senderID = parseIntField(data, "senderId");
                int groupId = parseIntField(data, "groupId");
                if (senderID == 0 || groupId == 0) return;
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MEMBER_TYPING, groupId, senderID));
            } catch (Exception e) {
                AppHelper.LogCat(e);
            }
        }).on(AppConstants.SOCKET_IS_MEMBER_STOP_TYPING, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                int senderID = parseIntField(data, "senderId");
                int groupId = parseIntField(data, "groupId");
                if (senderID == 0 || groupId == 0) return;
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MEMBER_STOP_TYPING, groupId, senderID));
            } catch (Exception e) {
                AppHelper.LogCat(e);
            }
        });

    }

    /**
     * method to check if user is connected to server
     *
     * @param mContext
     */
    private static void isUserConnected(Context mContext) {
        if (mSocket != null) {
            mSocket.on(AppConstants.SOCKET_CONNECTED, args -> {
                final JSONObject data = firstJson(args);
                if (data == null) return;
                try {
                    int connectedId = data.getInt("connectedId");
                    String socketId = data.getString("socketId");
                    boolean connected = data.getBoolean("connected");

                    if (connectedId == PreferenceManager.getID(mContext)) return;
                    try {
                        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                        ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("id", connectedId).findFirst();
                        if (contactsModel != null) {
                            if (connected) {
                                realm.beginTransaction();
                                contactsModel.setSocketId(socketId);
                                realm.copyToRealmOrUpdate(contactsModel);
                                realm.commitTransaction();
                                AppHelper.LogCat("User with id  --> " + connectedId + " is connected <---");
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_USER_STATE, AppConstants.EVENT_BUS_USER_IS_ONLINE, contactsModel.getId()));
                            } else {
                                realm.beginTransaction();
                                contactsModel.setSocketId(null);
                                realm.copyToRealmOrUpdate(contactsModel);
                                realm.commitTransaction();
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_USER_STATE, AppConstants.EVENT_BUS_USER_IS_OFFLINE, contactsModel.getId()));
                                AppHelper.LogCat("User with id  --> " + connectedId + " is disconnected  <---");
                            }
                        }
                        realm.close();
                    } catch (Exception e) {
                        AppHelper.LogCat(" isUserConnected Exception mainService " + e.getMessage());
                    } //// TODO: 4/7/17 hna luser is connected


                } catch (JSONException e) {
                    AppHelper.LogCat(e);
                }

            });
        }
    }

    private static boolean checkIfUnsentMessagesExist(int recipientId, Realm realm, Context mContext) {
        RealmQuery<MessagesModel> query = realm.where(MessagesModel.class)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("recipientID", recipientId)
                .equalTo("isGroup", false)
                .equalTo("isFileUpload", true)
                .equalTo("senderID", PreferenceManager.getID(mContext));

        return query.count() != 0;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mStopping) return START_NOT_STICKY;
        AppHelper.LogCat("MainService  has started");
        handleServiceIntent(intent);
        if (mSocket == null || !mSocket.connected()) {
            connectToServer(mContext);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        AppHelper.LogCat("MainService dataSync foreground-service timeout; stopping safely");
        stopSafely(startId);
    }

    private void stopSafely(int startId) {
        mStopping = true;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf(startId);
    }

    private void handleServiceIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if ("new_user_message_notification_whatsclone".equals(action)) {
            JSONObject data = serviceIntentToMessageJson(intent);
            if (data != null) {
                AppHelper.LogCat("MainService saving FCM message fallback");
                saveNewMessage(data);
            }
        } else if ("new_group_message_notification_whatsclone".equals(action)) {
            JSONObject data = serviceIntentToGroupMessageJson(intent);
            if (data != null) {
                AppHelper.LogCat("MainService saving FCM group message fallback");
                saveNewMessageGroup(data);
            }
        } else if (AppConstants.ACTION_SYNC_WALLET_CONTACTS.equals(action)) {
            AppHelper.LogCat("MainService syncing wallet contacts after permission grant");
            mContactSyncAttempted = false;
            syncDeviceWalletContacts(true);
            unSentMessages(this);
        }
    }

    private JSONObject serviceIntentToMessageJson(Intent intent) {
        try {
            JSONObject data = new JSONObject();
            data.put("recipientId", parseIntentInt(intent, "recipientID"));
            data.put("senderId", parseIntentInt(intent, "senderId"));
            data.put("walletAddress", stringExtra(intent, "walletAddress"));
            data.put("recipientWalletAddress", stringExtra(intent, "recipientWalletAddress"));
            data.put("messageBody", stringExtra(intent, "message"));
            data.put("senderName", stringExtra(intent, "username"));
            String senderImage = stringExtra(intent, "senderImage");
            if (senderImage.isEmpty()) senderImage = stringExtra(intent, "userImage");
            data.put("senderImage", senderImage);
            data.put("date", stringExtra(intent, "date"));
            data.put("video", nullStringExtra(intent, "video"));
            data.put("thumbnail", nullStringExtra(intent, "thumbnail"));
            data.put("image", nullStringExtra(intent, "image"));
            data.put("audio", nullStringExtra(intent, "audio"));
            data.put("document", nullStringExtra(intent, "document"));
            data.put("duration", nullStringExtra(intent, "duration"));
            data.put("fileSize", nullStringExtra(intent, "fileSize"));
            data.put("messageId", parseIntentInt(intent, "messageId"));
            return data;
        } catch (JSONException e) {
            AppHelper.LogCat("serviceIntentToMessageJson failed: " + e.getMessage());
            return null;
        }
    }

    private JSONObject serviceIntentToGroupMessageJson(Intent intent) {
        try {
            JSONObject data = new JSONObject();
            data.put("senderId", parseIntentInt(intent, "senderId"));
            data.put("groupId", parseIntentInt(intent, "groupID"));
            data.put("messageBody", stringExtra(intent, "message"));
            data.put("senderName", stringExtra(intent, "username"));
            data.put("senderPhone", stringExtra(intent, "senderPhone"));
            data.put("groupName", stringExtra(intent, "groupName"));
            data.put("groupImage", stringExtra(intent, "groupImage"));
            data.put("date", stringExtra(intent, "date"));
            return data;
        } catch (JSONException e) {
            AppHelper.LogCat("serviceIntentToGroupMessageJson failed: " + e.getMessage());
            return null;
        }
    }

    private int parseIntentInt(Intent intent, String key) {
        Object value = intent.getExtras() != null ? intent.getExtras().get(key) : null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value != null ? String.valueOf(value) : "0");
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String stringExtra(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value != null ? value : "";
    }

    private String nullStringExtra(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value != null && !value.isEmpty() ? value : "null";
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mStopping = true;
        super.onDestroy();

        AppHelper.LogCat("MainService has stopped");
        NotificationsManager.SetupBadger(mContext);
        // service finished
        if (mChangeListener != null && mReceiverRegistered) {
            try {
                getApplication().unregisterReceiver(mChangeListener);
            } catch (IllegalArgumentException e) {
                AppHelper.LogCat("MainService receiver already unregistered: " + e.getMessage());
            } finally {
                mReceiverRegistered = false;
            }
        }
        disconnectSocket();
        if (handler != null) handler.removeCallbacksAndMessages(null);

    }

    /**
     * method to check  for all unsent messages
     */
    public synchronized static void unSentMessages(Context mContext) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

        List<MessagesModel> messagesModelsList = realm.where(MessagesModel.class)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("isGroup", false)
                .equalTo("senderID", PreferenceManager.getID(mContext))
                .sort("id", Sort.ASCENDING).findAll();

        AppHelper.LogCat("size " + messagesModelsList.size());
        if (messagesModelsList.size() != 0) {

            for (MessagesModel messagesModel : messagesModelsList) {
                boolean hasFile = hasFilePayload(messagesModel);
                if (hasFile && !messagesModel.isFileUpload()) {
                    sendMessagesFiles(messagesModel);
                } else {
                    sendMessages(messagesModel);
                }
            }
        }
        realm.close();

    }

    /**
     * WorkManager entry point. Realm objects are detached before network calls and the
     * operation is bounded so Android may safely stop the worker between retries.
     */
    public static boolean syncPendingMessagesBlocking(Context context) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MessagesModel> pending;
        try {
            pending = realm.copyFromRealm(realm.where(MessagesModel.class)
                    .equalTo("status", AppConstants.IS_WAITING)
                    .equalTo("isGroup", false)
                    .equalTo("senderID", PreferenceManager.getID(context))
                    .sort("id", Sort.ASCENDING)
                    .findAll());
        } finally {
            if (!realm.isClosed()) realm.close();
        }

        int processed = 0;
        for (MessagesModel message : pending) {
            if (processed++ >= 25) break;
            if (!sendPendingMessageBlocking(message)) return false;
        }
        return true;
    }

    private static boolean sendPendingMessageBlocking(MessagesModel message) {
        if (hasFilePayload(message) && !message.isFileUpload()) {
            // Upload workers own local media; never send device paths as message URLs.
            return true;
        }
        UpdateMessageModel update = buildUpdateMessageModel(message);
        if (update.getSenderId() == 0
                || (update.getRecipientId() == 0
                && (update.getRecipientWalletAddress() == null
                || update.getRecipientWalletAddress().trim().isEmpty()))) {
            AppHelper.LogCat("Skipping invalid pending message " + message.getId());
            return true;
        }
        try {
            StatusResponse response = APIHelper.initialApiUsersContacts().sendMessage(update)
                    .timeout(25, TimeUnit.SECONDS)
                    .blockingFirst();
            if (response != null && response.isSuccess()) {
                makeMessageAsSent(update.getSenderId(), update.getMessageId());
                emitSocketMessage(update, false);
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            AppHelper.LogCat("Pending message sync failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * method to send unsentMessages
     *
     * @param messagesModel this i parameter for sendMessages method
     */
    public static void sendMessages(MessagesModel messagesModel) {
        UpdateMessageModel updateMessageModel = buildUpdateMessageModel(messagesModel);
        boolean hasFile = hasFilePayload(messagesModel);
        if (hasFile && !messagesModel.isFileUpload()) {
            // File not uploaded yet — do not send local paths to API; upload will retry
            AppHelper.LogCat("sendMessages: skipping message " + messagesModel.getId() + " — file not uploaded yet");
        } else {
            MainService.sendMessage(updateMessageModel, false);
        }
    }

    private static UpdateMessageModel buildUpdateMessageModel(MessagesModel messagesModel) {
        UpdateMessageModel updateMessageModel = new UpdateMessageModel();
        int senderId = messagesModel.getSenderID();
        if (senderId == 0) {
            senderId = PreferenceManager.getID(WhatsCloneApplication.getInstance());
        }
        updateMessageModel.setSenderId(senderId);
        updateMessageModel.setRecipientId(messagesModel.getRecipientID());
        updateMessageModel.setMessageId(messagesModel.getId());
        updateMessageModel.setConversationId(messagesModel.getConversationID());
        updateMessageModel.setMessageBody(messagesModel.getMessage());
        updateMessageModel.setSenderName(messagesModel.getUsername());
        updateMessageModel.setSenderImage("null");
        updateMessageModel.setWalletAddress(messagesModel.getWalletAddress());
        updateMessageModel.setRecipientWalletAddress(resolveRecipientWalletAddress(messagesModel.getRecipientID(), messagesModel.getConversationID()));
        updateMessageModel.setDate(messagesModel.getDate());
        updateMessageModel.setVideo(messagesModel.getVideoFile());
        updateMessageModel.setThumbnail(messagesModel.getVideoThumbnailFile());
        updateMessageModel.setImage(messagesModel.getImageFile());
        updateMessageModel.setAudio(messagesModel.getAudioFile());
        updateMessageModel.setDocument(messagesModel.getDocumentFile());
        updateMessageModel.setFileSize(messagesModel.getFileSize());
        updateMessageModel.setDuration(messagesModel.getDuration());
        updateMessageModel.setGroup(messagesModel.isGroup());
        updateMessageModel.setUserToken(PreferenceManager.getToken(WhatsCloneApplication.getInstance()));
        return updateMessageModel;
    }

    private static boolean hasFilePayload(MessagesModel message) {
        return hasValue(message.getImageFile()) || hasValue(message.getVideoFile())
                || hasValue(message.getAudioFile()) || hasValue(message.getDocumentFile());
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value);
    }

    public static void sendMessage(UpdateMessageModel updateMessageModel, boolean forGroup) {
        if (updateMessageModel.getSenderId() == 0) {
            AppHelper.LogCat("Cannot send message: senderId is 0");
            return;
        }
        if (!forGroup && updateMessageModel.getRecipientId() == 0
                && (updateMessageModel.getRecipientWalletAddress() == null || updateMessageModel.getRecipientWalletAddress().trim().isEmpty())) {
            AppHelper.LogCat("Cannot send message: missing recipient id and recipient wallet address");
            return;
        }
        if (forGroup) {
            APIHelper.initialApiUsersContacts().sendGroupMessage(updateMessageModel).subscribe(response -> {
                if (response.isSuccess()) {
                    MemberMarkMessageAsSent(updateMessageModel.getGroupID());
                    emitSocketMessage(updateMessageModel, true);
                }
            }, throwable -> {

            });
        } else {
            APIHelper.initialApiUsersContacts().sendMessage(updateMessageModel).subscribe(response -> {
                if (response.isSuccess()) {
                    makeMessageAsSent(updateMessageModel.getSenderId(), updateMessageModel.getMessageId());
                    emitSocketMessage(updateMessageModel, false);
                } else {
                    AppHelper.LogCat("sendMessage API failed: " + response.getMessage());
                    BackgroundSyncScheduler.enqueue(WhatsCloneApplication.getInstance());
                }
            }, throwable -> {
                AppHelper.LogCat("sendMessage API error: " + throwable.getMessage());
                BackgroundSyncScheduler.enqueue(WhatsCloneApplication.getInstance());
            });
        }

    }

    private static void emitSocketMessage(UpdateMessageModel updateMessageModel, boolean forGroup) {
        if (mSocket == null || !mSocket.connected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("recipientId", updateMessageModel.getRecipientId());
            data.put("messageId", updateMessageModel.getMessageId());
            data.put("conversationId", updateMessageModel.getConversationId());
            data.put("messageBody", updateMessageModel.getMessageBody());
            data.put("senderId", updateMessageModel.getSenderId());
            data.put("walletAddress", updateMessageModel.getWalletAddress());
            data.put("recipientWalletAddress", updateMessageModel.getRecipientWalletAddress());
            data.put("senderName", updateMessageModel.getSenderName());
            data.put("date", updateMessageModel.getDate());
            data.put("isGroup", updateMessageModel.isGroup());
            data.put("image", updateMessageModel.getImage());
            data.put("video", updateMessageModel.getVideo());
            data.put("audio", updateMessageModel.getAudio());
            data.put("document", updateMessageModel.getDocument());
            data.put("thumbnail", updateMessageModel.getThumbnail());
            data.put("duration", updateMessageModel.getDuration());
            data.put("fileSize", updateMessageModel.getFileSize());
            data.put("senderImage", updateMessageModel.getSenderImage());
            data.put("userToken", updateMessageModel.getUserToken());
            if (forGroup) {
                data.put("groupId", updateMessageModel.getGroupID());
                mSocket.emit(AppConstants.SOCKET_SAVE_NEW_GROUP_MESSAGE, data);
            } else {
                mSocket.emit(AppConstants.SOCKET_SAVE_NEW_MESSAGE, data);
            }
        } catch (JSONException e) {
            AppHelper.LogCat("emitSocketMessage JSONException " + e.getMessage());
        }
    }

    private static String resolveRecipientWalletAddress(int recipientId, int conversationId) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            ContactsModel contact = null;
            if (recipientId != 0) {
                contact = realm.where(ContactsModel.class).equalTo("id", recipientId).findFirst();
            }
            if (contact != null && contact.getWalletAddress() != null && !contact.getWalletAddress().trim().isEmpty()) {
                return contact.getWalletAddress();
            }
            ConversationsModel conversation = realm.where(ConversationsModel.class).equalTo("id", conversationId).findFirst();
            if (conversation != null && conversation.getRecipientPhone() != null && !conversation.getRecipientPhone().trim().isEmpty()) {
                return conversation.getRecipientPhone();
            }
        } catch (Exception e) {
            AppHelper.LogCat("resolveRecipientWalletAddress failed: " + e.getMessage());
        } finally {
            if (!realm.isClosed()) {
                realm.close();
            }
        }
        return null;
    }

    /**
     * method to send unsentMessages who has files
     *
     * @param messagesModel this i parameter for sendMessages method
     */
    public static void sendMessagesFiles(MessagesModel messagesModel) {
        if (!messagesModel.isFileUpload()) {
            AppHelper.LogCat("sendMessagesFiles: skipping message " + messagesModel.getId() + " — files not yet uploaded");
            return;
        }
        UpdateMessageModel updateMessageModel = new UpdateMessageModel();
        int senderId = messagesModel.getSenderID();
        if (senderId == 0) {
            senderId = PreferenceManager.getID(WhatsCloneApplication.getInstance());
        }
        updateMessageModel.setSenderId(senderId);
        updateMessageModel.setRecipientId(messagesModel.getRecipientID());
        updateMessageModel.setMessageId(messagesModel.getId());
        updateMessageModel.setConversationId(messagesModel.getConversationID());
        updateMessageModel.setMessageBody(messagesModel.getMessage());
        updateMessageModel.setSenderName(messagesModel.getUsername());
        updateMessageModel.setSenderImage("null");
        updateMessageModel.setWalletAddress(messagesModel.getWalletAddress());
        updateMessageModel.setRecipientWalletAddress(resolveRecipientWalletAddress(messagesModel.getRecipientID(), messagesModel.getConversationID()));
        updateMessageModel.setDate(messagesModel.getDate());
        updateMessageModel.setVideo(messagesModel.getVideoFile());
        updateMessageModel.setThumbnail(messagesModel.getVideoThumbnailFile());
        updateMessageModel.setImage(messagesModel.getImageFile());
        updateMessageModel.setAudio(messagesModel.getAudioFile());
        updateMessageModel.setDocument(messagesModel.getDocumentFile());
        updateMessageModel.setFileSize(messagesModel.getFileSize());
        updateMessageModel.setDuration(messagesModel.getDuration());
        updateMessageModel.setGroup(messagesModel.isGroup());
        updateMessageModel.setUserToken(PreferenceManager.getToken(WhatsCloneApplication.getInstance()));

        MainService.sendMessage(updateMessageModel, false);
    }


    /**
     * method to  update status delivered when user was offline and come online
     * and he has a new messages (unread)
     *
     * @param mContext
     */

    private static void updateStatusDeliveredOffline(Context mContext) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MessagesModel> messagesModels = realm.where(MessagesModel.class)
                .equalTo("recipientID", PreferenceManager.getID(mContext))
                .equalTo("status", AppConstants.IS_WAITING).findAll();
        if (messagesModels.size() != 0) {
            for (MessagesModel messagesModel : messagesModels) {
                RecipientMarkMessageAsDelivered(mContext, messagesModel.getId(), messagesModel.getSenderID());
            }
        }
    }

    /**
     * method to mark messages as delivered by recipient
     *
     * @param mContext
     * @param messageId   this is the  parameter for RecipientMarkMessageAsDelivered method
     * @param recipientId
     */
    public static void RecipientMarkMessageAsDelivered(Context mContext, int messageId, int recipientId) {
        try {
            JSONObject json = new JSONObject();
            json.put("senderId", PreferenceManager.getID(mContext));
            json.put("recipientId", recipientId);
            json.put("messageId", messageId);

            if (mSocket != null) {
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_DELIVERED, json);
                AppHelper.LogCat("--> Recipient mark message as  delivered <-- " + messageId);
            }


        } catch (Exception e) {
            AppHelper.LogCat(e);
        }

    }

    /**
     * method to emit that message are seen by user
     */
    public static void emitMessageSeen(Context mContext, int senderId) {
        emitMessageSeen(mContext, senderId, 0);
    }

    public static void emitMessageSeen(Context mContext, int senderId, int messageId) {
        JSONObject json = new JSONObject();
        try {
            json.put("recipientId", senderId);
            json.put("senderId", PreferenceManager.getID(mContext));
            if (messageId > 0) {
                json.put("messageId", messageId);
            }

            if (mSocket != null)
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_SEEN, json);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * method to mark messages as delivered by recipient
     *
     * @param mContext
     * @param groupId  this is the  parameter for RecipientMarkMessageAsDeliveredGroup method
     */
    public static void RecipientMarkMessageAsDeliveredGroup(Context mContext, int groupId) {
        try {
            JSONObject json = new JSONObject();
            json.put("senderId", PreferenceManager.getID(mContext));
            json.put("groupId", groupId);

            if (mSocket != null) {
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED, json);
            }


        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        AppHelper.LogCat("--> Recipient mark message as  delivered <--");
    }

    /**
     * method to mark messages as seen by recipient
     *
     * @param mContext
     * @param groupId  this is the  parameter for RecipientMarkMessageAsSeenGroup method
     */
    public static void RecipientMarkMessageAsSeenGroup(Context mContext, int groupId) {
        try {
            JSONObject json = new JSONObject();
            json.put("senderId", PreferenceManager.getID(mContext));
            json.put("groupId", groupId);

            if (mSocket != null) {
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN, json);
            }


        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        AppHelper.LogCat("--> Recipient mark message as  delivered <--");
    }

    /**
     * method to update status for a specific  message (as delivered by sender)
     */
    private void SenderMarkMessageAsDelivered() {

        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_DELIVERED, args -> {
            AppHelper.LogCat("--> Sender mark message as  delivered: update status  fsds <-- ");
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                int senderId = data.getInt("senderId");
                if (senderId == PreferenceManager.getID(mContext))
                    return;
                updateDeliveredStatus(data);
                AppHelper.LogCat("--> Sender mark message as  delivered: update status  <--");

            } catch (Exception e) {
                AppHelper.LogCat(e);
            }

        });
    }


    /**
     * method to update status for a specific  message (as delivered by sender) in realm database
     *
     * @param data this is parameter for  updateDeliveredStatus
     */
    private void updateDeliveredStatus(JSONObject data) {
        try {
            int messageId = data.getInt("messageId");
            int senderId = data.getInt("senderId");
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            realm.executeTransaction(realm1 -> {
                MessagesModel messagesModel = realm1.where(MessagesModel.class).equalTo("id", messageId).equalTo("status", AppConstants.IS_SENT).findFirst();
                if (messagesModel != null) {
                    messagesModel.setStatus(AppConstants.IS_DELIVERED);
                    realm1.copyToRealmOrUpdate(messagesModel);
                    AppHelper.LogCat("Delivered successfully");
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_MESSAGES, messageId));
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS, messagesModel.getConversationID()));
                } else {
                    AppHelper.LogCat("Delivered failed ");
                }
            });
            realm.close();
        } catch (JSONException e) {
            AppHelper.LogCat("Save data to realm delivered JSONException " + e.getMessage());
        }
    }


    public static boolean checkIfUserBlockedExist(int userId, Realm realm) {
        RealmQuery<UsersBlockModel> query = realm.where(UsersBlockModel.class).equalTo("contactsModel.id", userId);
        return query.count() != 0;
    }

    /**
     * method to make message as sent
     */
    private static void makeMessageAsSent(int SenderID, int messageId) {
        if (SenderID != PreferenceManager.getID(WhatsCloneApplication.getInstance()))
            return;
        updateStatusAsSentBySender(messageId);


    }

    /**
     * method to update user register id firbase
     */
    /**
      * method to update status as seen by sender (if recipient have been seen the message)
     */
    private void SenderMarkMessageAsSeen() {
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_SEEN, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            updateSeenStatus(data);
        });

    }

    private void SenderMarkMessageAsDownloaded() {
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_DOWNLOADED, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            updateDownloadedStatus(data);
        });

    }

    private void updateDownloadedStatus(JSONObject data) {
        try {
            int senderId = data.getInt("senderId");
            int messageId = data.getInt("messageId");
            if (senderId != PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            MessagesModel messagesModel = realm.where(MessagesModel.class)
                    .equalTo("id", messageId)
                    .beginGroup()
                        .equalTo("status", AppConstants.IS_SEEN)
                        .or()
                        .equalTo("status", AppConstants.IS_DELIVERED)
                    .endGroup()
                    .findFirst();
            if (messagesModel != null) {
                realm.executeTransaction(realm1 -> {
                    messagesModel.setStatus(AppConstants.IS_DOWNLOADED);
                    realm1.copyToRealmOrUpdate(messagesModel);
                });
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_DOWNLOADED_FOR_MESSAGES));
            }
            if (!realm.isClosed()) realm.close();
        } catch (Exception e) {
            AppHelper.LogCat("updateDownloadedStatus Exception " + e.getMessage());
        }
    }

    public static void emitMessageDownloaded(Context mContext, int messageId, int recipientId) {
        Socket socket = mSocket;
        if (socket != null && socket.connected()) {
            try {
                JSONObject json = new JSONObject();
                json.put("messageId", messageId);
                json.put("senderId", recipientId);
                json.put("recipientId", PreferenceManager.getID(mContext));
                socket.emit(AppConstants.SOCKET_IS_MESSAGE_DOWNLOADED, json);
            } catch (JSONException e) {
                AppHelper.LogCat("emitMessageDownloaded Exception " + e.getMessage());
            }
        }
    }


    /**
     * method to get a conversation id by groupId
     *
     * @param groupId this is the first parameter for getConversationId method
     * @param realm   this is the second parameter for getConversationId method
     * @return conversation id
     */
    private static int getConversationIdByGroupId(int groupId, Realm realm) {
        try {
            ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                    .equalTo("groupID", groupId)
                    .findAll().first();
            return conversationsModelNew.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id  (group) Exception MainService  " + e.getMessage());
            return 0;
        }
    }

    /**
     * method to update status as seen by sender (if recipient have been seen the message)
     */
    private static void MemberMarkMessageAsSent(int groupId) {
        updateGroupSentStatus(groupId);
    }

    /**
     * method to update status as delivered by sender (if recipient have been seen the message)
     */
    private void MemberMarkMessageAsDelivered() {
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            // AppHelper.LogCat("SOCKET_IS_MESSAGE_GROUP_DELIVERED ");
            updateGroupDeliveredStatus(data);
        });
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            // AppHelper.LogCat("SOCKET_IS_MESSAGE_GROUP_SEEN ");

            updateGroupSeenStatus(data);
        });

    }

    /**
     * method to update status as delivered by sender
     *
     * @param data this is parameter for updateSeenStatus method
     */
    private void updateGroupDeliveredStatus(JSONObject data) {


        try {
            int groupId = parseIntField(data, "groupId");
            int senderId = parseIntField(data, "senderId");
            if (groupId == 0 || senderId == 0) return;
            AppHelper.LogCat("groupId " + groupId);
            AppHelper.LogCat("sen hhh " + senderId);
            if (senderId == PreferenceManager.getID(mContext)) return;

            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int ConversationID = getConversationIdByGroupId(groupId, realm);
            AppHelper.LogCat("conversation  id seen " + ConversationID);
            List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                    .equalTo("conversationID", ConversationID)
                    .equalTo("isGroup", true)
                    .equalTo("groupID", groupId)
                    .equalTo("status", AppConstants.IS_SENT)
                    .findAll();
            if (messagesModelsRealm.size() != 0) {
                for (MessagesModel messagesModel1 : messagesModelsRealm) {

                    realm.executeTransaction(realm1 -> {
                        MessagesModel messagesModel = realm1.where(MessagesModel.class)
                                .equalTo("groupID", groupId)
                                .equalTo("id", messagesModel1.getId())
                                .equalTo("status", AppConstants.IS_SENT).findFirst();
                        if (messagesModel != null) {
                            messagesModel.setStatus(AppConstants.IS_DELIVERED);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            AppHelper.LogCat("Delivered successfully MainService");

                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_MESSAGES, messagesModel.getId()));

                        } else {
                            AppHelper.LogCat("Seen  failed MainService ");
                        }
                    });

                }
            }
            realm.close();
            EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS, ConversationID));

        } catch (Exception e) {
            AppHelper.LogCat("Save to realm seen MainService " + e.getMessage());
        }

    }

    /**
     * method to update status as seen by sender (group)
     *
     * @param data this is parameter for updateSeenStatus method
     */
    private void updateGroupSeenStatus(JSONObject data) {

        try {
            int groupId = parseIntField(data, "groupId");
            int senderId = parseIntField(data, "senderId");
            if (groupId == 0 || senderId == 0) return;
            AppHelper.LogCat("groupId " + groupId);
            AppHelper.LogCat("sen " + senderId);
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int ConversationID = getConversationIdByGroupId(groupId, realm);
            AppHelper.LogCat("conversation  id seen " + ConversationID);
            List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                    .equalTo("conversationID", ConversationID)
                    .equalTo("isGroup", true)
                    .equalTo("groupID", groupId)
                    .beginGroup()
                    .equalTo("status", AppConstants.IS_SENT)
                    .or()
                    .equalTo("status", AppConstants.IS_DELIVERED)
                    .endGroup()
                    .findAll();
            if (messagesModelsRealm.size() != 0) {
                for (MessagesModel messagesModel1 : messagesModelsRealm) {

                    realm.executeTransaction(realm1 -> {
                        MessagesModel messagesModel = realm1.where(MessagesModel.class)
                                .equalTo("groupID", groupId)
                                .equalTo("id", messagesModel1.getId())
                                .beginGroup()
                                .equalTo("status", AppConstants.IS_SENT)
                                .or()
                                .equalTo("status", AppConstants.IS_DELIVERED)
                                .endGroup()
                                .findFirst();
                        if (messagesModel != null) {
                            messagesModel.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            AppHelper.LogCat("seen successfully");
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_MESSAGES, messagesModel.getId()));

                        } else {
                            AppHelper.LogCat("Seen  failed MainService (group)");
                        }
                    });
                }
            }
            realm.close();
            EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS, ConversationID));

        } catch (Exception e) {
            AppHelper.LogCat("Save to realm seen " + e);
        }

    }


    /**
     * method to update status as sent by sender
     */
    private static void updateGroupSentStatus(int groupId) {
        int senderId = PreferenceManager.getID(WhatsCloneApplication.getInstance());
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        int ConversationID = getConversationIdByGroupId(groupId, realm);
        List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                .equalTo("conversationID", ConversationID)
                .equalTo("isGroup", true)
                .equalTo("groupID", groupId)
                .equalTo("senderID", senderId)
                .equalTo("status", AppConstants.IS_WAITING)
                .findAll();
        if (messagesModelsRealm.size() != 0) {
            for (MessagesModel messagesModel1 : messagesModelsRealm) {

                realm.executeTransaction(realm1 -> {
                    MessagesModel messagesModel = realm1.where(MessagesModel.class)
                            .equalTo("isGroup", true)
                            .equalTo("isFileUpload", true)
                            .equalTo("groupID", groupId)
                            .equalTo("senderID", senderId)
                            .equalTo("id", messagesModel1.getId())
                            .equalTo("status", AppConstants.IS_WAITING).findFirst();
                    if (messagesModel != null) {
                        messagesModel.setStatus(AppConstants.IS_SENT);
                        realm1.copyToRealmOrUpdate(messagesModel);
                        AppHelper.LogCat("Sent successfully MainService");
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SENT_FOR_MESSAGES, messagesModel.getId()));
                    } else {
                        AppHelper.LogCat("Sent  failed  MainService");
                    }
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS, ConversationID));
                });

            }
        }
        if (!realm.isClosed())
            realm.close();

    }

    /**
     * method to update status as seen by sender (if recipient have been seen the message)  in realm database
     *
     * @param data this is parameter for updateSeenStatus method
     */
    private void updateSeenStatus(JSONObject data) {

        try {
            int recipientId = data.getInt("recipientId");
            int senderId = data.getInt("senderId");
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int ConversationID = getConversationId(senderId, recipientId, realm);
            List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                    .equalTo("conversationID", ConversationID)
                    .equalTo("isGroup", false)
                    .beginGroup()
                    .equalTo("status", AppConstants.IS_DELIVERED)
                    .or()
                    .equalTo("status", AppConstants.IS_SENT)
                    .endGroup()
                    .findAll();
            if (messagesModelsRealm.size() != 0) {
                for (MessagesModel messagesModel1 : messagesModelsRealm) {

                    realm.executeTransaction(realm1 -> {
                        MessagesModel messagesModel = realm1.where(MessagesModel.class)
                                .equalTo("recipientID", senderId)
                                .equalTo("senderID", recipientId)
                                .equalTo("id", messagesModel1.getId())
                                .beginGroup()
                                .equalTo("status", AppConstants.IS_DELIVERED)
                                .or()
                                .equalTo("status", AppConstants.IS_SENT)
                                .endGroup()
                                .findFirst();
                        if (messagesModel != null) {
                            messagesModel.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            AppHelper.LogCat("Seen successfully MainService");
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_MESSAGES, messagesModel.getId()));
                        } else {
                            AppHelper.LogCat("Seen  failed  MainService");
                        }
                    });
                }
            }
            EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS, ConversationID));
            realm.close();
        } catch (JSONException e) {
            AppHelper.LogCat("Save to realm seen  Exception" + e.getMessage());
        }

    }

    /**
     * method to get a conversation id
     *
     * @param recipientId this is the first parameter for getConversationId method
     * @param senderId    this is the second parameter for getConversationId method
     * @param realm       this is the thirded parameter for getConversationId method
     * @return conversation id
     */
    public static int getConversationId(int recipientId, int senderId, Realm realm) {
        ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                .beginGroup()
                .equalTo("RecipientID", recipientId)
                .or()
                .equalTo("RecipientID", senderId)
                .endGroup()
                .findFirst();
        return conversationsModelNew != null ? conversationsModelNew.getId() : 0;
    }


    /**
     * method to update status for the send message by sender  (as sent message ) in realm  database
     *
     * @param messageId this is the first parameter for updateStatusAsSentBySender method
     */
    private static void updateStatusAsSentBySender(int messageId) {


        try {
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            try {
                realm.executeTransaction(realm1 -> {
                    MessagesModel messagesModel = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                    messagesModel.setStatus(AppConstants.IS_SENT);
                    realm1.copyToRealmOrUpdate(messagesModel);
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SENT_FOR_MESSAGES, messageId));
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS, messagesModel.getConversationID()));
                });
            } catch (Exception e) {
                AppHelper.LogCat(" Is sent messages Realm Error" + e.getMessage());
            }
            if (!realm.isClosed())
                realm.close();

        } catch (Exception e) {
            AppHelper.LogCat("null object Exception MainService" + e.getMessage());
        }


    }


    /**
     * method to send group messages
     *
     * @param messagesModel this is parameter of sendMessagesGroup method
     */
    public static void sendMessagesGroup(Activity activity, ContactsModel mUsersModel, GroupsModel mGroupsModel, MessagesModel messagesModel) {

        JSONObject message = new JSONObject();
        try {

            if (mUsersModel != null && mUsersModel.getUsername() != null) {
                message.put("senderName", mUsersModel.getUsername());
            } else {
                message.put("senderName", "null");
            }
            if (mUsersModel != null)
                message.put("walletAddress", mUsersModel.getWalletAddress());
            else
                message.put("walletAddress", null);


            if (mGroupsModel != null && mGroupsModel.getGroupImage() != null)
                message.put("GroupImage", mGroupsModel.getGroupImage());
            else
                message.put("GroupImage", "null");
            if (mGroupsModel != null && mGroupsModel.getGroupName() != null)
                message.put("GroupName", mGroupsModel.getGroupName());
            else
                message.put("GroupName", "null");

            message.put("messageBody", messagesModel.getMessage());
            message.put("senderId", messagesModel.getSenderID());
            message.put("recipientId", messagesModel.getRecipientID());
            if (mGroupsModel != null && mGroupsModel.getGroupName() != null)
                message.put("groupID", mGroupsModel.getId());
            else
                message.put("groupID", messagesModel.getGroupID());
            message.put("date", messagesModel.getDate());
            message.put("isGroup", true);
            message.put("image", messagesModel.getImageFile());
            message.put("video", messagesModel.getVideoFile());
            message.put("audio", messagesModel.getAudioFile());
            message.put("thumbnail", messagesModel.getVideoThumbnailFile());
            message.put("document", messagesModel.getDocumentFile());

            if (!messagesModel.getFileSize().equals("0"))
                message.put("fileSize", messagesModel.getFileSize());
            else
                message.put("fileSize", "0");

            if (!messagesModel.getDuration().equals("0"))
                message.put("duration", messagesModel.getDuration());
            else
                message.put("duration", "0");

            message.put("userToken", PreferenceManager.getToken(activity));


            UpdateMessageModel updateMessageModel = new UpdateMessageModel();
            try {
                updateMessageModel.setSenderId(message.getInt("senderId"));
                updateMessageModel.setRecipientId(message.getInt("recipientId"));
                updateMessageModel.setMessageBody(message.getString("messageBody"));
                updateMessageModel.setSenderName(message.getString("senderName"));
                updateMessageModel.setGroupName(message.getString("GroupName"));
                updateMessageModel.setGroupImage(message.getString("GroupImage"));
                updateMessageModel.setGroupID(message.getInt("groupID"));
                updateMessageModel.setDate(message.getString("date"));
                updateMessageModel.setWalletAddress(message.getString("walletAddress"));
                updateMessageModel.setVideo(message.getString("video"));
                updateMessageModel.setThumbnail(message.getString("thumbnail"));
                updateMessageModel.setImage(message.getString("image"));
                updateMessageModel.setAudio(message.getString("audio"));
                updateMessageModel.setDocument(message.getString("document"));
                updateMessageModel.setFileSize(message.getString("fileSize"));
                updateMessageModel.setDuration(message.getString("duration"));
                updateMessageModel.setGroup(message.getBoolean("isGroup"));
                updateMessageModel.setUserToken(message.getString("userToken"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MainService.sendMessage(updateMessageModel, true);
        } catch (JSONException e) {
            AppHelper.LogCat(e.getMessage());
        }


    }


    private static int parseIntField(JSONObject data, String field) {
        int val = data.optInt(field, 0);
        if (val == 0) {
            try { val = Integer.parseInt(data.optString(field, "0")); } catch (NumberFormatException ignored) {}
        }
        return val;
    }

    private static JSONObject firstJson(Object... args) {
        if (args == null || args.length == 0 || !(args[0] instanceof JSONObject)) {
            AppHelper.LogCat("Ignoring malformed socket payload");
            return null;
        }
        return (JSONObject) args[0];
    }

    private void onReceiveNewMessage() {
        mSocket.on(AppConstants.SOCKET_NEW_MESSAGE_SERVER, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            AppHelper.LogCat("Socket.IO: received socket_new_message_server");
            int senderId = parseIntField(data, "senderId");
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            if (checkIfUserBlockedExist(senderId, realm)) {
                if (!realm.isClosed()) realm.close();
                return;
            }
            if (!realm.isClosed()) realm.close();
            saveNewMessage(data);
        });

        mSocket.on(AppConstants.SOCKET_NEW_MESSAGE_GROUP_SERVER, args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            AppHelper.LogCat("Socket.IO: received socket_new_group_message_server");
            int senderId = parseIntField(data, "senderId");
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            if (checkIfUserBlockedExist(senderId, realm)) {
                if (!realm.isClosed()) realm.close();
                return;
            }
            if (!realm.isClosed()) realm.close();
            saveNewMessageGroup(data);
        });
    }

    private void saveNewMessage(JSONObject data) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            int recipientId = parseIntField(data, "recipientId");
            int senderId = parseIntField(data, "senderId");
            String phone = data.optString("walletAddress", "");
            String messageBody = data.optString("messageBody", "");
            String senderName = data.optString("senderName", "");
            String senderImage = data.optString("senderImage", "null");
            String date = data.optString("date", "");
            String video = data.optString("video", "null");
            String thumbnail = data.optString("thumbnail", "null");
            String image = data.optString("image", "null");
            String audio = data.optString("audio", "null");
            String document = data.optString("document", "null");
            String duration = data.optString("duration", "null");
            String fileSize = data.optString("fileSize", "null");
            int messageId = data.optInt("messageId", 0);

            if (senderId == PreferenceManager.getID(this)) return;

            int conversationID = getConversationId(recipientId, senderId, realm);
            boolean isDuplicate = false;

            if (messageId != 0) {
                isDuplicate = realm.where(MessagesModel.class)
                        .equalTo("id", messageId)
                        .count() > 0;
            }

            if (!isDuplicate) {
                isDuplicate = realm.where(MessagesModel.class)
                        .equalTo("senderID", senderId)
                        .equalTo("date", date)
                        .equalTo("conversationID", conversationID)
                        .count() > 0;
            }

            if (isDuplicate) {
                AppHelper.LogCat("saveNewMessage: duplicate detected, skipping (senderId=" + senderId + " messageId=" + messageId + ")");
                if (!realm.isClosed()) realm.close();
                return;
            }
            if (conversationID == 0) {
                realm.executeTransaction(realm1 -> {
                    int lastConversationID = RealmBackupRestore.getConversationLastId();
                    int lastID = RealmBackupRestore.getMessageLastId();
                    int UnreadMessageCounter = 1;

                    io.realm.RealmList<MessagesModel> messagesModelRealmList = new io.realm.RealmList<>();
                    MessagesModel messagesModel = new MessagesModel();
                    messagesModel.setId(lastID);
                    messagesModel.setUsername(senderName);
                    messagesModel.setRecipientID(recipientId);
                    messagesModel.setDate(date);
                    messagesModel.setStatus(AppConstants.IS_WAITING);
                    messagesModel.setGroup(false);
                    messagesModel.setSenderID(senderId);
                    messagesModel.setFileUpload(true);
                    if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null")) {
                        messagesModel.setFileDownLoad(false);
                    } else {
                        messagesModel.setFileDownLoad(true);
                    }
                    messagesModel.setDuration(duration);
                    messagesModel.setFileSize(fileSize);
                    messagesModel.setConversationID(lastConversationID);
                    messagesModel.setMessage(messageBody);
                    messagesModel.setImageFile(image);
                    messagesModel.setVideoFile(video);
                    messagesModel.setAudioFile(audio);
                    messagesModel.setDocumentFile(document);
                    messagesModel.setVideoThumbnailFile(thumbnail);
                    messagesModel.setWalletAddress(phone);
                    messagesModelRealmList.add(messagesModel);

                    ConversationsModel conversationsModel1 = new ConversationsModel();
                    conversationsModel1.setRecipientID(senderId);
                    conversationsModel1.setLastMessage(messageBody);
                    conversationsModel1.setRecipientUsername(senderName);
                    conversationsModel1.setMessageDate(date);
                    conversationsModel1.setId(lastConversationID);
                    conversationsModel1.setStatus(AppConstants.IS_WAITING);
                    conversationsModel1.setRecipientPhone(phone);
                    conversationsModel1.setGroup(false);
                    conversationsModel1.setMessages(messagesModelRealmList);
                    conversationsModel1.setUnreadMessageCounter(String.valueOf(UnreadMessageCounter));
                    conversationsModel1.setLastMessageId(lastID);
                    conversationsModel1.setCreatedOnline(true);
                    realm1.copyToRealmOrUpdate(conversationsModel1);

                    String FileType = null;
                    if (!messagesModel.getImageFile().equals("null")) FileType = "Image";
                    else if (!messagesModel.getVideoFile().equals("null")) FileType = "Video";
                    else if (!messagesModel.getAudioFile().equals("null")) FileType = "Audio";
                    else if (!messagesModel.getDocumentFile().equals("null")) FileType = "Document";

                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_MESSAGES_NEW_ROW, messagesModel));
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_NEW_ROW, lastConversationID));

                    mIntent = new Intent("new_user_message_notification_whatsclone");
                    mIntent.setPackage(getPackageName());
                    mIntent.putExtra("conversationID", lastConversationID);
                    mIntent.putExtra("recipientID", senderId);
                    mIntent.putExtra("senderId", senderId);
                    mIntent.putExtra("userImage", senderImage);
                    mIntent.putExtra("username", senderName);
                    mIntent.putExtra("file", FileType);
                    mIntent.putExtra("walletAddress", phone);
                    mIntent.putExtra("messageId", messageId);
                    mIntent.putExtra("message", messageBody);
                    mIntent.putExtra("app", this.getPackageName());
                    sendBroadcast(mIntent);
                });
            } else {
                realm.executeTransaction(realm1 -> {
                    int UnreadMessageCounter = 0;
                    int lastID = RealmBackupRestore.getMessageLastId();

                    ConversationsModel conversationsModel;
                    RealmQuery<ConversationsModel> conversationsModelRealmQuery = realm1.where(ConversationsModel.class).equalTo("id", conversationID);
                    conversationsModel = conversationsModelRealmQuery.findAll().first();

                    UnreadMessageCounter = Integer.parseInt(conversationsModel.getUnreadMessageCounter());
                    UnreadMessageCounter++;
                    MessagesModel messagesModel = new MessagesModel();
                    messagesModel.setId(lastID);
                    messagesModel.setUsername(senderName);
                    messagesModel.setRecipientID(recipientId);
                    messagesModel.setDate(date);
                    messagesModel.setStatus(AppConstants.IS_WAITING);
                    messagesModel.setGroup(false);
                    messagesModel.setSenderID(senderId);
                    messagesModel.setFileUpload(true);
                    if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null")) {
                        messagesModel.setFileDownLoad(false);
                    } else {
                        messagesModel.setFileDownLoad(true);
                    }
                    messagesModel.setFileSize(fileSize);
                    messagesModel.setDuration(duration);
                    messagesModel.setConversationID(conversationID);
                    messagesModel.setMessage(messageBody);
                    messagesModel.setImageFile(image);
                    messagesModel.setVideoFile(video);
                    messagesModel.setAudioFile(audio);
                    messagesModel.setDocumentFile(document);
                    messagesModel.setVideoThumbnailFile(thumbnail);
                    messagesModel.setWalletAddress(phone);
                    conversationsModel.getMessages().add(messagesModel);
                    conversationsModel.setLastMessageId(lastID);
                    conversationsModel.setRecipientID(senderId);
                    conversationsModel.setLastMessage(messageBody);
                    conversationsModel.setMessageDate(date);
                    conversationsModel.setCreatedOnline(true);
                    conversationsModel.setRecipientUsername(senderName);
                    conversationsModel.setRecipientPhone(phone);
                    conversationsModel.setGroup(false);
                    conversationsModel.setStatus(AppConstants.IS_WAITING);
                    conversationsModel.setUnreadMessageCounter(String.valueOf(UnreadMessageCounter));
                    realm1.copyToRealmOrUpdate(conversationsModel);

                    String FileType = null;
                    if (!messagesModel.getImageFile().equals("null")) FileType = "Image";
                    else if (!messagesModel.getVideoFile().equals("null")) FileType = "Video";
                    else if (!messagesModel.getAudioFile().equals("null")) FileType = "Audio";
                    else if (!messagesModel.getDocumentFile().equals("null")) FileType = "Document";

                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_MESSAGES_NEW_ROW, messagesModel));
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_OLD_ROW, conversationID));

                    mIntent = new Intent("new_user_message_notification_whatsclone");
                    mIntent.setPackage(getPackageName());
                    mIntent.putExtra("conversationID", conversationID);
                    mIntent.putExtra("recipientID", senderId);
                    mIntent.putExtra("senderId", senderId);
                    mIntent.putExtra("userImage", senderImage);
                    mIntent.putExtra("username", senderName);
                    mIntent.putExtra("file", FileType);
                    mIntent.putExtra("walletAddress", phone);
                    mIntent.putExtra("messageId", messageId);
                    mIntent.putExtra("message", messageBody);
                    mIntent.putExtra("app", this.getPackageName());
                    sendBroadcast(mIntent);
                });
            }

            handler.post(() -> RecipientMarkMessageAsDelivered(MainService.this, messageId, senderId));
            handler.postDelayed(() -> {
                if (AppHelper.isActivityRunning(MainService.this, "activities.messages.MessagesActivity")) {
                    AppHelper.LogCat("MessagesActivity running, emitting seen");
                    emitMessageSeen(MainService.this, senderId, messageId);
                }
            }, 500);

        } catch (Exception e) {
            AppHelper.LogCat("saveNewMessage Exception MainService " + e.getMessage());
        }
        if (!realm.isClosed()) realm.close();
        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_COUNTER));
        NotificationsManager.SetupBadger(this);
    }

    private void saveNewMessageGroup(JSONObject data) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            int senderId = data.getInt("senderId");
            int groupID = data.getInt("groupID");
            String senderName = data.optString("senderName", "");
            String messageBody = data.optString("messageBody", "");
            String date = data.optString("date", "");
            String groupImage = data.optString("GroupImage", "null");
            String groupName = data.optString("GroupName", "");
            String phone = data.optString("walletAddress", "");
            String image = data.optString("image", "null");
            String video = data.optString("video", "null");
            String thumbnail = data.optString("thumbnail", "null");
            String audio = data.optString("audio", "null");
            String document = data.optString("document", "null");
            String duration = data.optString("duration", "null");
            String fileSize = data.optString("fileSize", "null");

            if (senderId == PreferenceManager.getID(this)) return;

            if (!checkIfGroupExist(groupID, realm)) {
                if (!realm.isClosed()) realm.close();
                return;
            }

            int conversationID = getConversationIdByGroupId(groupID, realm);
            if (conversationID == 0) {
                if (!realm.isClosed()) realm.close();
                return;
            }

            realm.executeTransaction(realm1 -> {
                int lastID = RealmBackupRestore.getMessageLastId();
                int UnreadMessageCounter = 0;
                ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", conversationID).findFirst();
                UnreadMessageCounter = Integer.parseInt(conversationsModel.getUnreadMessageCounter());
                UnreadMessageCounter++;

                MessagesModel messagesModel = new MessagesModel();
                messagesModel.setId(lastID);
                messagesModel.setDate(date);
                messagesModel.setSenderID(senderId);
                messagesModel.setUsername(senderName);
                messagesModel.setWalletAddress(phone);
                messagesModel.setRecipientID(0);
                messagesModel.setStatus(AppConstants.IS_WAITING);
                messagesModel.setGroup(true);
                messagesModel.setFileUpload(true);
                if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null")) {
                    messagesModel.setFileDownLoad(false);
                } else {
                    messagesModel.setFileDownLoad(true);
                }
                messagesModel.setFileSize(fileSize);
                messagesModel.setDuration(duration);
                messagesModel.setConversationID(conversationID);
                messagesModel.setMessage(messageBody);
                messagesModel.setImageFile(image);
                messagesModel.setVideoFile(video);
                messagesModel.setAudioFile(audio);
                messagesModel.setDocumentFile(document);
                messagesModel.setVideoThumbnailFile(thumbnail);
                conversationsModel.getMessages().add(messagesModel);
                conversationsModel.setLastMessageId(lastID);
                conversationsModel.setLastMessage(messageBody);
                conversationsModel.setMessageDate(date);
                conversationsModel.setCreatedOnline(true);
                conversationsModel.setStatus(AppConstants.IS_WAITING);
                conversationsModel.setUnreadMessageCounter(String.valueOf(UnreadMessageCounter));
                realm1.copyToRealmOrUpdate(conversationsModel);

                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_MESSAGES_NEW_ROW, messagesModel));
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_OLD_ROW, conversationID));

                mIntent = new Intent("new_group_message_notification_whatsclone");
                mIntent.setPackage(getPackageName());
                mIntent.putExtra("conversationID", conversationID);
                mIntent.putExtra("groupID", groupID);
                mIntent.putExtra("senderId", senderId);
                mIntent.putExtra("username", senderName);
                mIntent.putExtra("message", messageBody);
                mIntent.putExtra("app", this.getPackageName());
                sendBroadcast(mIntent);
            });

        } catch (Exception e) {
            AppHelper.LogCat("saveNewMessageGroup Exception MainService " + e.getMessage());
        }
        if (!realm.isClosed()) realm.close();
        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_COUNTER));
        NotificationsManager.SetupBadger(this);
    }

    private void onReceiveNewCall() {
        mSocket.on(AppConstants.SOCKET_RECEIVE_NEW_CALL, onReceiveNewCall);
    }

    /**
     * Receive call emitter callback when others call you.
     *
     * @param args json value contain callerid, userid and caller name
     */
    private Emitter.Listener onReceiveNewCall = args -> {
        AppHelper.LogCat("onReceiveNewCall called");
        JSONObject data = firstJson(args);
        if (data == null) return;
        try {
            String callerSocketId = data.getString("from");
            String callerPhone = data.getString("callerPhone");
            int callerID = data.getInt("callerID");
            String callerImage = data.getString("callerImage");
            boolean isVideoCall = data.getBoolean("isVideoCall");

            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            if (!checkIfUserBlockedExist(callerID, realm)) {
                if (!realm.isClosed())
                    realm.close();

                boolean appInForeground = AppHelper.isActivityRunning(MainService.this, "activities.call.IncomingCallActivity")
                        || AppHelper.isActivityRunning(MainService.this, "activities.call.CallActivity")
                        || AppHelper.isActivityRunning(MainService.this, "activities.main.MainActivity");
                boolean canLaunchDirectly = appInForeground && isDeviceInteractiveAndUnlocked();

                if (canLaunchDirectly) {
                    Intent intent = new Intent(getApplicationContext(), IncomingCallActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(AppConstants.CALLER_SOCKET_ID, callerSocketId);
                    intent.putExtra(AppConstants.USER_SOCKET_ID, PreferenceManager.getSocketID(this));
                    intent.putExtra(AppConstants.CALLER_PHONE, callerPhone);
                    intent.putExtra(AppConstants.CALLER_IMAGE, callerImage);
                    intent.putExtra(AppConstants.CALLER_ID, callerID);
                    intent.putExtra(AppConstants.IS_VIDEO_CALL, isVideoCall);
                    intent.putExtra(AppConstants.USER_PHONE, PreferenceManager.getWalletAddress(this));
                    getApplicationContext().startActivity(intent);
                } else {
                    com.money.mimi.helpers.notifications.NotificationsManager.showIncomingCallNotification(
                            getApplicationContext(), callerSocketId, callerID, callerPhone, callerImage, isVideoCall);
                }
            } else {
                try {
                    JSONObject message = new JSONObject();
                    message.put("userSocketId", PreferenceManager.getSocketID(this));
                    message.put("callerSocketId", callerSocketId);
                    message.put("reason", AppConstants.NO_ANSWER);
                    if (mSocket != null) {
                        mSocket.emit(AppConstants.SOCKET_REJECT_NEW_CALL, message);
                    }
                } catch (JSONException e) {
                    AppHelper.LogCat(" JSONException IncomingCallActivity rejectCall " + e.getMessage());
                }
            }
        } catch (JSONException e) {
            AppHelper.LogCat("JSONException Call" + e.getMessage());
        }

    };

    private boolean isDeviceInteractiveAndUnlocked() {
        boolean interactive = true;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            interactive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                    ? powerManager.isInteractive()
                    : powerManager.isScreenOn();
        }

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = keyguardManager != null && keyguardManager.isKeyguardLocked();
        return interactive && !locked;
    }


}
