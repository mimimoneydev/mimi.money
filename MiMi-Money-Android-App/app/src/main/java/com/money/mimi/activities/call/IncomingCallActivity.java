package com.money.mimi.activities.call;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.activities.settings.PreferenceSettingsManager;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.images.BlurTransformationPicasso;
import com.money.mimi.models.calls.CallSaverModel;
import com.money.mimi.models.calls.CallsInfoModel;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.presenters.calls.CallsPresenter;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by Abderrahim El imame on 10/13/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class IncomingCallActivity extends AppCompatActivity {


    private String userPhone;
    private String userSocketId;
    private String callerSocketId;
    private Vibrator vibrator;
    private MediaPlayer mMediaPlayer;
    private Socket mSocket;
    private boolean isVideoCall;
    private boolean isSaved = false;
    private boolean isClosing = false;
    private final Handler socketHandler = new Handler(Looper.getMainLooper());
    private int socketAcquireAttempts;
    Timer timer;
    long autoRejectDelay = 30 * 1000;
    @BindView(R.id.caller_name)
    TextView callerName;

    @BindView(R.id.caller_image)
    ImageView callerImageView;


    @BindView(R.id.caller_phone)
    TextView callerPhoneField;

    @BindView(R.id.incoming_type)
    TextView incomingCallType;

    @BindView(R.id.accept_call)
    AppCompatImageButton sliderAccept;

    @BindView(R.id.reject_call)
    AppCompatImageButton sliderReject;


    String callerPhone;
    String callerImage;
    private CallsPresenter callsPresenter;
    private int callerID;
    private ContactsModel mContactsModel;
    private AudioManager mAudioManager;
    private int originalVolume;
    private int lastID;
    private int callerContactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        showOverLockScreen();
        if (AppHelper.isAndroid5()) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        setTypeFaces();
        connectToChatServer();

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }
        callerSocketId = extras.getString(AppConstants.CALLER_SOCKET_ID);
        userSocketId = extras.getString(AppConstants.USER_SOCKET_ID);
        callerPhone = extras.getString(AppConstants.CALLER_PHONE);
        callerImage = extras.getString(AppConstants.CALLER_IMAGE);
        userPhone = extras.getString(AppConstants.USER_PHONE);
        callerID = extras.getInt(AppConstants.CALLER_ID);
        isVideoCall = extras.getBoolean(AppConstants.IS_VIDEO_CALL);
        callsPresenter = new CallsPresenter(this, callerID);
        callsPresenter.onCreate();
        getUserInfo();
        saveToDataBase();
        String displayName = resolveDisplayName(callerPhone);
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = getString(R.string.app_name);
        }
        if (callerPhone != null && !displayName.equals(callerPhone)) {
            callerName.setText(displayName);
            callerPhoneField.setVisibility(View.VISIBLE);
            callerPhoneField.setText(callerPhone);
        } else {
            callerPhoneField.setVisibility(View.GONE);
            callerName.setText(displayName);
        }

        if (isVideoCall) {
            incomingCallType.setText(getString(R.string.video_call));
        } else {
            incomingCallType.setText(getString(R.string.voice_call));
        }

        sliderReject.setOnClickListener(v -> {
            AppHelper.LogCat("clicked sliderReject");
            rejectCall(false);

        });

        sliderAccept.setOnClickListener(v -> {
            AppHelper.LogCat("clicked sliderAccept");
            acceptCall();
        });

        String notificationAction = extras.getString(AppConstants.CALL_NOTIFICATION_ACTION);
        if (AppConstants.CALL_ACTION_ANSWER.equals(notificationAction)) {
            acceptCall();
            return;
        } else if (AppConstants.CALL_ACTION_REJECT.equals(notificationAction)) {
            rejectCall(false);
            return;
        }

        AnimationsUtil.ShakeAnimation(this, findViewById(R.id.accept_call));
        AnimationsUtil.ShakeAnimation(this, findViewById(R.id.reject_call));
        if (PreferenceSettingsManager.conversation_tones(this)) {
            Uri uri = PreferenceSettingsManager.getDefault_calls_notifications_settings_tone(this);
            if (uri != null) {
                try {
                    mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    if (mAudioManager != null) {
                        originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                    }
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.setLooping(true);
                        mMediaPlayer.start();
                    }
                } catch (Exception e) {
                    AppHelper.LogCat(e.getMessage());
                }
            }
        }

        if (PreferenceSettingsManager.getDefault_calls_notifications_settings_vibrate(this)) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] vibrate = new long[]{2000, 2000, 2000, 2000, 2000};
            if (vibrator != null) {
                vibrator.vibrate(vibrate, 1);
            }
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                rejectCall(true);
            }

        }, autoRejectDelay);


    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            callerName.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            callerPhoneField.setTypeface(AppHelper.setTypeFace(this, "Futura"));

        }
    }

    private void showOverLockScreen() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
    }


    private TextDrawable textDrawable(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = getApplicationContext().getString(R.string.app_name);
        }
        ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
        // generate random color
        int color = generator.getColor(name);
        String c = String.valueOf(name.toUpperCase().charAt(0));
        return TextDrawable.builder().buildRect(c, color);


    }

    @SuppressLint("StaticFieldLeak")
    public void getUserInfo() {
        try {
            MemoryCache memoryCache = new MemoryCache();
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

            mContactsModel = realm.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();

            if (mContactsModel == null) {
                callerName.setText(callerPhone != null ? callerPhone : getString(R.string.app_name));
                callerPhoneField.setVisibility(View.GONE);
                callerImageView.setImageDrawable(textDrawable(callerPhone));
                if (!realm.isClosed()) {
                    realm.close();
                }
                return;
            }


            String finalName;
            if (mContactsModel.getUsername() != null && !mContactsModel.getUsername().isEmpty()) {
                finalName = mContactsModel.getUsername();
            } else {
                finalName = mContactsModel.getWalletAddress();
            }
            callerContactId = mContactsModel.getId();
            TextDrawable drawable = textDrawable(finalName);

            int userId = callerContactId;
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, callerImage, IncomingCallActivity.this, userId, AppConstants.USER, AppConstants.FULL_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        ImageLoader.SetBitmapImage(bitmap, callerImageView);
                    } else {
                        Target target = new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                callerImageView.setImageBitmap(bitmap);

                            }

                            @Override
                            public void onBitmapFailed(Drawable errorDrawable) {
                                callerImageView.setImageDrawable(errorDrawable);
                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {
                                callerImageView.setImageDrawable(placeHolderDrawable);
                            }
                        };
                        callerImageView.setTag(target);
                        Picasso.with(IncomingCallActivity.this)
                                .load(EndPoints.PROFILE_IMAGE_URL + callerImage)
                                .transform(new BlurTransformationPicasso(IncomingCallActivity.this))
                                .placeholder(drawable)
                                .error(drawable)
                                .into(target);
                    }
                }
            }.execute();

            if (!realm.isClosed())
                realm.close();
        } catch (Exception e) {
            AppHelper.LogCat(e.getMessage());
        }

    }

    private int getHistoryCallId(int fromId, int toId, boolean isVideoCall, Realm realm) {
        String type;
        if (isVideoCall)
            type = AppConstants.VIDEO_CALL;
        else
            type = AppConstants.VOICE_CALL;


        try {
            CallsModel callsModel = realm.where(CallsModel.class)
                    .equalTo("from", fromId)
                    .equalTo("to", toId)
                    .equalTo("received", true)
                    .equalTo("type", type)
                    .findAll().first();
            return callsModel.getId();
        } catch (Exception e) {
            AppHelper.LogCat("call history id Exception MainService" + e.getMessage());
            return 0;
        }
    }


    public void saveToDataBase() {
        if (mContactsModel == null || callerContactId == 0) return;
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        DateTime current = new DateTime();
        String callTime = String.valueOf(current);

        int historyCallId = getHistoryCallId(callerContactId, PreferenceManager.getID(this), isVideoCall, realm);

        if (historyCallId == 0) {
            realm.executeTransactionAsync(realm1 -> {
                ContactsModel contactsModel1 = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();
                if (contactsModel1 == null) {
                    return;
                }

                int lastID = RealmBackupRestore.getCallLastId();
                CallsModel callsModel = new CallsModel();
                callsModel.setId(lastID);
                if (isVideoCall)
                    callsModel.setType(AppConstants.VIDEO_CALL);
                else
                    callsModel.setType(AppConstants.VOICE_CALL);
                callsModel.setContactsModel(contactsModel1);
                callsModel.setWalletAddress(callerPhone);
                callsModel.setCounter(1);
                callsModel.setFrom(contactsModel1.getId());
                callsModel.setTo(PreferenceManager.getID(this));
                callsModel.setDuration("00:00");
                callsModel.setDate(callTime);
                callsModel.setReceived(true);

                CallsInfoModel callsInfoModel = new CallsInfoModel();
                RealmList<CallsInfoModel> callsInfoModelRealmList = new RealmList<CallsInfoModel>();
                int lastInfoID = RealmBackupRestore.getCallInfoLastId();
                callsInfoModel.setId(lastInfoID);
                if (isVideoCall)
                    callsInfoModel.setType(AppConstants.VIDEO_CALL);
                else
                    callsInfoModel.setType(AppConstants.VOICE_CALL);
                callsInfoModel.setContactsModel(contactsModel1);
                callsInfoModel.setWalletAddress(callerPhone);
                callsInfoModel.setCallId(lastID);
                callsInfoModel.setFrom(contactsModel1.getId());
                callsInfoModel.setTo(PreferenceManager.getID(this));
                callsInfoModel.setDuration("00:00");
                callsInfoModel.setDate(callTime);
                callsInfoModel.setReceived(true);
                callsInfoModelRealmList.add(callsInfoModel);
                callsModel.setCallsInfoModels(callsInfoModelRealmList);
                realm1.copyToRealmOrUpdate(callsModel);
                this.lastID = lastID;
                //EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CALL_NEW_ROW, lastID));
            }, () -> {
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CALL_NEW_ROW, this.lastID));
            });
        } else {

            realm.executeTransactionAsync(realm1 -> {
                ContactsModel contactsModel1 = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();
                if (contactsModel1 == null) {
                    return;
                }

                int callCounter;
                CallsModel callsModel;
                RealmQuery<CallsModel> callsModelRealmQuery = realm1.where(CallsModel.class).equalTo("id", historyCallId);
                callsModel = callsModelRealmQuery.findFirst();
                if (callsModel == null) {
                    return;
                }

                callCounter = callsModel.getCounter();
                callCounter++;
                callsModel.setDate(callTime);
                callsModel.setCounter(callCounter);
                callsModel.setDuration("00:00");
                CallsInfoModel callsInfoModel = new CallsInfoModel();
                RealmList<CallsInfoModel> callsInfoModelRealmList = callsModel.getCallsInfoModels();
                int lastInfoID = RealmBackupRestore.getCallInfoLastId();
                callsInfoModel.setId(lastInfoID);
                if (isVideoCall)
                    callsInfoModel.setType(AppConstants.VIDEO_CALL);
                else
                    callsInfoModel.setType(AppConstants.VOICE_CALL);
                callsInfoModel.setContactsModel(contactsModel1);
                callsInfoModel.setWalletAddress(callerPhone);
                callsInfoModel.setCallId(callsModel.getId());
                callsInfoModel.setFrom(contactsModel1.getId());
                callsInfoModel.setTo(PreferenceManager.getID(this));
                callsInfoModel.setDuration("00:00");
                callsInfoModel.setDate(callTime);
                callsInfoModel.setReceived(true);
                callsInfoModelRealmList.add(callsInfoModel);
                callsModel.setCallsInfoModels(callsInfoModelRealmList);

                realm1.copyToRealmOrUpdate(callsModel);
                //  EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CALL_OLD_ROW, historyCallId));
            }, () -> {
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CALL_OLD_ROW, historyCallId));
            });
        }

        if (!isSaved) {
            try {
                RealmQuery<CallsModel> callsModelRealmQuery;
                String isVideo;
                if (isVideoCall) {
                    isVideo = "video";
                    callsModelRealmQuery = realm.where(CallsModel.class).equalTo("type", AppConstants.VIDEO_CALL).equalTo("from", callerContactId);
                } else {
                    isVideo = "audio";
                    callsModelRealmQuery = realm.where(CallsModel.class).equalTo("type", AppConstants.VOICE_CALL).equalTo("from", callerContactId);
                }
                CallsModel callsModel = null;
                if (callsModelRealmQuery != null && callsModelRealmQuery.isValid())
                    callsModel = callsModelRealmQuery.findFirst();
                if (callsModel != null && callsModel.isValid()) {
                    CallSaverModel callSaverModel = new CallSaverModel();
                    callSaverModel.setToId(callsModel.getTo());
                    callSaverModel.setFromId(callsModel.getFrom());

                    callSaverModel.setDate(callsModel.getDate());
                    callSaverModel.setDuration(callsModel.getDuration());
                    callSaverModel.setIsVideo(isVideo);
                    APIHelper.initialApiUsersContacts().saveReceivedCall(callSaverModel).subscribe(Response -> {
                        isSaved = Response.isSuccess();
                    }, throwable -> {
                        isSaved = false;
                    });
                }
            } catch (Exception e) {
                AppHelper.LogCat(e.getMessage());
            }

        }

        if (!realm.isClosed()) realm.close();
    }

    private String resolveDisplayName(String phone) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("walletAddress", phone).findFirst();
            if (contactsModel != null && contactsModel.getUsername() != null && !contactsModel.getUsername().isEmpty()) {
                return contactsModel.getUsername();
            }
        } finally {
            if (!realm.isClosed()) realm.close();
        }
        return phone;
    }

    public void showUserInfo(ContactsModel contactsModel) {
        if (contactsModel != null && contactsModel.getUsername() != null && !contactsModel.getUsername().isEmpty()) {
            callerName.setText(contactsModel.getUsername());
            callerPhoneField.setVisibility(View.VISIBLE);
            callerPhoneField.setText(contactsModel.getWalletAddress());
        }
    }


    /**
     * method to accept incoming calls
     */
    public void acceptCall() {
        if (isClosing) {
            return;
        }
        isClosing = true;
        com.money.mimi.helpers.notifications.NotificationsManager.cancelIncomingCallNotification(this);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }

        if (mMediaPlayer != null) {
            if (mAudioManager != null) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            }
            try {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
            } catch (Exception e) {
                AppHelper.LogCat(e.getMessage());
            }
            mMediaPlayer = null;
        }
        Intent intent = new Intent(IncomingCallActivity.this, CallActivity.class);
        intent.putExtra(AppConstants.USER_SOCKET_ID, userSocketId);
        intent.putExtra(AppConstants.USER_PHONE, userPhone);
        intent.putExtra(AppConstants.CALLER_PHONE_ACCEPT, callerPhone);
        intent.putExtra(AppConstants.CALLER_IMAGE, callerImage);
        intent.putExtra(AppConstants.USER_IMAGE, "");
        intent.putExtra(AppConstants.CALLER_SOCKET_ID, callerSocketId);
        intent.putExtra(AppConstants.IS_VIDEO_CALL, isVideoCall);
        intent.putExtra(AppConstants.IS_ACCEPTED_CALL, true);
        intent.putExtra(AppConstants.CALLER_ID, callerID);
        startActivity(intent);
        AnimationsUtil.setSlideInAnimation(this);
        try {
            JSONObject message = new JSONObject();
            message.put("userSocketId", userSocketId);
            message.put("callerSocketId", callerSocketId);
            emitCallSignal(AppConstants.SOCKET_ACCEPT_NEW_CALL, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Window wind = this.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    /**
     * Publish a hangUp command if rejecting call.
     *
     * @param noAnswer
     */
    public void rejectCall(boolean noAnswer) {
        if (Thread.currentThread() != getMainLooper().getThread()) {
            runOnUiThread(() -> rejectCall(noAnswer));
            return;
        }
        if (isClosing) {
            return;
        }
        isClosing = true;
        com.money.mimi.helpers.notifications.NotificationsManager.cancelIncomingCallNotification(this);
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (mMediaPlayer != null) {
            if (mAudioManager != null) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            }
            try {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
            } catch (Exception e) {
                AppHelper.LogCat(e.getMessage());
            }
            mMediaPlayer = null;
        }

        try {
            JSONObject message = new JSONObject();
            message.put("userSocketId", userSocketId);
            message.put("callerSocketId", callerSocketId);
            if (noAnswer)
                message.put("reason", AppConstants.NO_ANSWER);
            else
                message.put("reason", AppConstants.IGNORED);
            emitCallSignal(AppConstants.SOCKET_REJECT_NEW_CALL, message);
        } catch (JSONException e) {
            AppHelper.LogCat(" JSONException IncomingCallActivity rejectCall " + e.getMessage());
        }
        finish();
        AnimationsUtil.setSlideOutAnimation(this);


    }


    private boolean connectToChatServer() {
        WhatsCloneApplication app = (WhatsCloneApplication) getApplication();
        mSocket = app.getSocket();
        if (mSocket == null || !mSocket.connected()) {
            AppHelper.startMainService(this);
            AppHelper.LogCat("IncomingCallActivity waiting for foreground socket");
            if (socketAcquireAttempts++ < 12) {
                socketHandler.postDelayed(this::connectToChatServer, 250L);
            }
            return false;
        }
        socketAcquireAttempts = 0;
        mSocket.off(AppConstants.SOCKET_HANGUP_CALL, onHangUpCallResponse);
        mSocket.on(AppConstants.SOCKET_HANGUP_CALL, onHangUpCallResponse);
        return true;

    }

    private void emitCallSignal(String event, JSONObject message) {
        try {
            if (mSocket == null || !mSocket.connected()) {
                AppHelper.startMainService(this);
                AppHelper.LogCat("IncomingCallActivity cannot emit " + event + ": socket unavailable");
                return;
            }
            mSocket.emit(event, message);
        } catch (Exception e) {
            AppHelper.LogCat("IncomingCallActivity emit " + event + " failed: " + e.getMessage());
        }
    }

    private Emitter.Listener onHangUpCallResponse = this::handleHangUpCallResponse;

    private void handleHangUpCallResponse(Object... args) {
        if (Thread.currentThread() != getMainLooper().getThread()) {
            runOnUiThread(() -> handleHangUpCallResponse(args));
            return;
        }
        if (isClosing) {
            return;
        }
        isClosing = true;

        if (args == null || args.length == 0 || !(args[0] instanceof JSONObject)) {
            AppHelper.LogCat("IncomingCallActivity hangup payload is invalid");
            isClosing = false;
            return;
        }

        JSONObject data = (JSONObject) args[0];
        try {
            String from = data.getString("userSocketId");
            if (from.equals(PreferenceManager.getSocketID(WhatsCloneApplication.getInstance()))) {
                isClosing = false;
                return;
            }
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        } catch (JSONException e) {
            AppHelper.LogCat(" onHangUpCallResponse JSONException " + e.getMessage());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketHandler.removeCallbacksAndMessages(null);
        if (callsPresenter != null) {
            callsPresenter.onDestroy();
        }
        if (mSocket != null) {
            mSocket.off(AppConstants.SOCKET_HANGUP_CALL, onHangUpCallResponse);
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (mMediaPlayer != null) {
            try {
                if (mAudioManager != null) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                }
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer = null;
            } catch (Exception e) {
                AppHelper.LogCat(e.getMessage());
            }
        }
    }

    @Override
    @SuppressLint("MissingSuperCall") // Back rejects the call instead of navigating away.
    public void onBackPressed() {
        rejectCall(false);
    }


}
