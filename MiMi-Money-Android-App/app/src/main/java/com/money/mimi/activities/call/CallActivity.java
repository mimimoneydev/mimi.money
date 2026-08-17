package com.money.mimi.activities.call;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.activities.main.MainActivity;
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
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.call.PeerConnectionParameters;
import com.money.mimi.helpers.call.WebRtcClient;
import com.money.mimi.helpers.images.BlurTransformationPicasso;
import com.money.mimi.models.calls.CallPusher;
import com.money.mimi.models.calls.CallSaverModel;
import com.money.mimi.models.calls.CallsInfoModel;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.NetworkChangeDetector;
import org.webrtc.NetworkMonitorAutoDetect;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import pl.bclogic.pulsator4droid.library.PulsatorLayout;


/**
 * Created by Abderrahim El imame on 10/20/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CallActivity extends AppCompatActivity {
    private static final String VIDEO_CODEC_VP8 = "VP8";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private WebRtcClient webRtcClient;

    private String userPhone;
    private String userSocketId = "";
    private String callerPhone = "";
    private String callerPhoneAccept = "";
    private String callerImage = "";
    private String userImage = "";
    private String callerSocketId = "";
    private boolean isVideoCall = false;
    private boolean isAccepted = false;
    private boolean backPressed = false;
    private boolean isEndingCall = false;
    private boolean isWebRtcClosing = false;
    private Thread backPressedThread = null;

    Timer timer;
    long autoHangupDelay = 60 * 1000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService callCleanupExecutor = Executors.newSingleThreadExecutor();
    private Handler customHandler = mainHandler;
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    private volatile String lastCallDuration = "00:00";


    @BindView(R.id.remote_video_view)
    SurfaceViewRenderer remoteVideoView;

    @BindView(R.id.local_video_view)
    SurfaceViewRenderer localVideoView;

    @BindView(R.id.call_timer)
    TextView callTimer;

    @BindView(R.id.call_status)
    TextView callStatus;

    @BindView(R.id.connection_status)
    TextView connectionStatus;

    @BindView(R.id.video_call_layout)
    RelativeLayout videoCallLayout;

    @BindView(R.id.calling_layout)
    FrameLayout callingLayout;


    @BindView(R.id.caller_image)
    ImageView callerImageView;

    @BindView(R.id.caller_phone)
    TextView callerPhoneField;

    @BindView(R.id.call_title)
    TextView callTitle;

    @BindView(R.id.voice_pulsator)
    PulsatorLayout voicePulsator;

    @BindView(R.id.switch_camera)
    AppCompatImageView switchCamera;

    @BindView(R.id.hang_up)
    AppCompatImageView hangUpBtn;

    @BindView(R.id.hang_up_layout)
    FrameLayout hangUpLayout;

    @BindView(R.id.mic_toggle)
    AppCompatImageView micToggle;

    @BindView(R.id.speaker)
    AppCompatImageView speaker;

    private NetworkMonitorAutoDetect networkMonitorAutoDetect;
    private int callerID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppHelper.startMainService(this);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_call);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        if (AppHelper.isAndroid5()) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }
        userSocketId = extras.getString(AppConstants.USER_SOCKET_ID);
        callerSocketId = extras.getString(AppConstants.CALLER_SOCKET_ID);
        userPhone = extras.getString(AppConstants.USER_PHONE);
        callerImage = extras.getString(AppConstants.CALLER_IMAGE);
        userImage = extras.getString(AppConstants.USER_IMAGE);
        callerPhone = extras.getString(AppConstants.CALLER_PHONE);
        callerPhoneAccept = extras.getString(AppConstants.CALLER_PHONE_ACCEPT);
        isVideoCall = extras.getBoolean(AppConstants.IS_VIDEO_CALL);
        isAccepted = extras.getBoolean(AppConstants.IS_ACCEPTED_CALL);
        callerID = extras.getInt(AppConstants.CALLER_ID);
        initializerView();
        setTypeFaces();
    }

    private void initializerView() {

        timer = new Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                hangUp();
            }

        }, autoHangupDelay);

        getCallerInfo();
        if (isAccepted) {
            callStatus.setText(getString(R.string.incoming_call));
            callerPhoneField.setVisibility(View.VISIBLE);
            callerPhoneField.setText(resolveDisplayName(callerPhoneAccept));
        } else {
            callStatus.setText(getString(R.string.calling));
            callerPhoneField.setVisibility(View.VISIBLE);
            callerPhoneField.setText(resolveDisplayName(callerPhone));
            saveCallToLocalDB();
        }

        initializeWebRtc();

        if (isVideoCall) {
            if (webRtcClient != null) {
                remoteVideoView.init(webRtcClient.getRootEglBase().getEglBaseContext(), null);
                remoteVideoView.setScalingType(scalingType);
                remoteVideoView.setKeepScreenOn(true);

                localVideoView.init(webRtcClient.getRootEglBase().getEglBaseContext(), null);
                localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                localVideoView.setKeepScreenOn(true);
                localVideoView.setZOrderMediaOverlay(true);
            }

            videoCallLayout.setVisibility(View.VISIBLE);
            callingLayout.setVisibility(View.GONE);
            callTitle.setText(R.string.video_call);

        } else {
            callingLayout.setVisibility(View.VISIBLE);
            videoCallLayout.setVisibility(View.GONE);
            callTitle.setText(R.string.voice_call);
            if (webRtcClient != null && !isAccepted) {
                webRtcClient.startOutgoingSound();
            }
        }


        switchCamera.setOnClickListener(v -> {
            if (webRtcClient != null) {
                webRtcClient.switchCamera(this);
            }
        });
        hangUpBtn.setOnClickListener(v -> {
            hangUp();
        });

        micToggle.setOnClickListener(v -> {
            if (webRtcClient != null) {
                if (webRtcClient.toggleMic()) {
                    micToggle.setImageDrawable(AppHelper.getVectorDrawable(this, R.drawable.ic_mic_white_active_24dp));
                } else {
                    micToggle.setImageDrawable(AppHelper.getVectorDrawable(this, R.drawable.ic_mic_off_white_24dp));
                }
            }
        });
        speaker.setOnClickListener(v -> {
            if (webRtcClient != null) {
                if (webRtcClient.enableSpeaker()) {
                    speaker.setImageDrawable(AppHelper.getVectorDrawable(this, R.drawable.ic_volume_off_white_24dp));
                } else {
                    speaker.setImageDrawable(AppHelper.getVectorDrawable(this, R.drawable.ic_volume_up_white_24dp));
                }
            }
        });
        networkDetection();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            callTimer.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            callTitle.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            callStatus.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            connectionStatus.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            callerPhoneField.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }


    private void networkDetection() {
        networkMonitorAutoDetect = new NetworkMonitorAutoDetect(new NetworkChangeDetector.Observer() {
            @Override
            public void onConnectionTypeChanged(NetworkChangeDetector.ConnectionType connectionType) {
                AppHelper.LogCat("onConnectionTypeChanged " + connectionType.name());
                connectionStatus.setVisibility(View.VISIBLE);
                switch (connectionType) {
                    case CONNECTION_WIFI:
                    case CONNECTION_4G:
                    case CONNECTION_3G:
                        connectionStatus.setVisibility(View.GONE);
                        break;
                    case CONNECTION_2G:
                        connectionStatus.setText(R.string.you_are_using_a_slower_connection);
                        new Handler().postDelayed(() -> {
                            connectionStatus.setVisibility(View.GONE);
                        }, 4000);
                        break;
                    case CONNECTION_NONE:
                    case CONNECTION_UNKNOWN:
                        connectionStatus.setText(R.string.connection_is_not_available);
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                            }
                            runOnUiThread(CallActivity.this::hangUp);
                        }).start();
                        break;
                }
            }

            @Override
            public void onNetworkConnect(NetworkChangeDetector.NetworkInformation networkInformation) {
            }

            @Override
            public void onNetworkDisconnect(long l) {
            }

            @Override
            public void onNetworkPreference(List<NetworkChangeDetector.ConnectionType> connectionTypes, int preference) {
            }
        }, this);
    }

    private void saveCallToLocalDB() {

        DateTime current = new DateTime();
        String callTime = String.valueOf(current);
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

        int historyCallId = getHistoryCallId(PreferenceManager.getID(this), callerID, isVideoCall, realm);

        if (historyCallId == 0) {
            realm.executeTransaction(realm1 -> {
                ContactsModel contactsModel1;
                if (isAccepted) {
                    contactsModel1 = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhoneAccept).findFirst();
                } else {
                    contactsModel1 = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();
                }
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
                if (isAccepted)
                    callsModel.setWalletAddress(callerPhoneAccept);
                else
                    callsModel.setWalletAddress(callerPhone);
                callsModel.setFrom(PreferenceManager.getID(this));
                callsModel.setTo(contactsModel1.getId());
                callsModel.setDuration("00:00");
                callsModel.setCounter(1);
                callsModel.setDate(callTime);
                callsModel.setReceived(false);

                CallsInfoModel callsInfoModel = new CallsInfoModel();
                RealmList<CallsInfoModel> callsInfoModelRealmList = new RealmList<CallsInfoModel>();
                int lastInfoID = RealmBackupRestore.getCallInfoLastId();
                callsInfoModel.setId(lastInfoID);
                if (isVideoCall)
                    callsInfoModel.setType(AppConstants.VIDEO_CALL);
                else
                    callsInfoModel.setType(AppConstants.VOICE_CALL);
                callsInfoModel.setContactsModel(contactsModel1);
                if (isAccepted)
                    callsInfoModel.setWalletAddress(callerPhoneAccept);
                else
                    callsInfoModel.setWalletAddress(callerPhone);
                callsInfoModel.setFrom(PreferenceManager.getID(this));
                callsInfoModel.setCallId(lastID);
                callsInfoModel.setTo(contactsModel1.getId());
                callsInfoModel.setDuration("00:00");
                callsInfoModel.setDate(callTime);
                callsInfoModel.setReceived(false);
                callsInfoModelRealmList.add(callsInfoModel);
                callsModel.setCallsInfoModels(callsInfoModelRealmList);
                realm1.copyToRealmOrUpdate(callsModel);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CALL_NEW_ROW, lastID));
            });
        } else {

            realm.executeTransaction(realm1 -> {
                ContactsModel contactsModel1;
                if (isAccepted) {
                    contactsModel1 = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhoneAccept).findFirst();
                } else {
                    contactsModel1 = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();
                }
                if (contactsModel1 == null) {
                    return;
                }


                int callCounter;
                CallsModel callsModel;
                RealmQuery<CallsModel> callsModelRealmQuery = realm1.where(CallsModel.class).equalTo("id", historyCallId);
                callsModel = callsModelRealmQuery.findAll().first();

                callCounter = callsModel.getCounter();
                callCounter++;
                callsModel.setDate(callTime);
                callsModel.setCounter(callCounter);
                CallsInfoModel callsInfoModel = new CallsInfoModel();
                RealmList<CallsInfoModel> callsInfoModelRealmList = callsModel.getCallsInfoModels();
                int lastInfoID = RealmBackupRestore.getCallInfoLastId();
                callsInfoModel.setId(lastInfoID);
                if (isVideoCall)
                    callsInfoModel.setType(AppConstants.VIDEO_CALL);
                else
                    callsInfoModel.setType(AppConstants.VOICE_CALL);
                callsInfoModel.setContactsModel(contactsModel1);
                if (isAccepted)
                    callsInfoModel.setWalletAddress(callerPhoneAccept);
                else
                    callsInfoModel.setWalletAddress(callerPhone);
                callsInfoModel.setFrom(PreferenceManager.getID(this));
                callsInfoModel.setTo(contactsModel1.getId());
                callsInfoModel.setCallId(callsModel.getId());
                callsInfoModel.setDuration("00:00");
                callsInfoModel.setDate(callTime);
                callsInfoModel.setReceived(false);
                callsInfoModelRealmList.add(callsInfoModel);
                callsModel.setCallsInfoModels(callsInfoModelRealmList);

                realm1.copyToRealmOrUpdate(callsModel);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CALL_OLD_ROW, historyCallId));
            });
        }

        realm.close();
    }

    private int getHistoryCallId(int fromId, int toId, boolean isVideoCall, Realm realm) {
        String type;
        CallsModel callsModel;
        if (isVideoCall)
            type = AppConstants.VIDEO_CALL;
        else
            type = AppConstants.VOICE_CALL;


        try {

            callsModel = realm.where(CallsModel.class)
                    .equalTo("from", fromId)
                    .equalTo("to", toId)
                    .equalTo("received", false)
                    .equalTo("type", type)
                    .findAll().first();
            return callsModel.getId();
        } catch (Exception e) {
            AppHelper.LogCat("call history id Exception MainService" + e.getMessage());
            return 0;
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

    private void getCallerInfo() {
        MemoryCache memoryCache = new MemoryCache();
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        ContactsModel mContactsModel;
        if (isAccepted) {
            mContactsModel = realm.where(ContactsModel.class).equalTo("walletAddress", callerPhoneAccept).findFirst();
        } else {
            mContactsModel = realm.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();
        }

        String finalName ;
        if (mContactsModel == null) {
            finalName = (callerPhone != null) ? callerPhone : "";
        } else if (mContactsModel.getUsername() != null && !mContactsModel.getUsername().isEmpty()) {
            finalName = mContactsModel.getUsername();
        } else {
            finalName = mContactsModel.getWalletAddress();
        }
        String displayName = finalName;
        runOnUiThread(() -> callerPhoneField.setText(displayName));
        TextDrawable drawable = textDrawable(finalName);

        String image = (mContactsModel != null) ? mContactsModel.getImage() : null;
        int userId = (mContactsModel != null) ? mContactsModel.getId() : 0;
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return ImageLoader.GetCachedBitmapImage(memoryCache, image, CallActivity.this,userId, AppConstants.USER, AppConstants.FULL_PROFILE);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    callerImageView.setImageBitmap(bitmap);
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
                    Picasso.with(CallActivity.this)
                            .load(EndPoints.PROFILE_IMAGE_URL + callerImage)
                            .placeholder(drawable)
                            .transform(new BlurTransformationPicasso(CallActivity.this))
                            .error(drawable)
                            .into(target);
                }
            }
        }.execute();
        if (!realm.isClosed())
            realm.close();
    }


    /**
     * Initialize webrtc webRtcClient
     * Set up the peer connection parameters get some video information and then pass these information to WebrtcClient class.
     */
    private void initializeWebRtc() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(isVideoCall, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP8, true, 1, AUDIO_CODEC_OPUS, true);
        webRtcClient = new WebRtcClient(this, params, userSocketId, callerSocketId, isVideoCall, isAccepted);


    }


    /**
     * Handle when people click hangUp button
     * Destroy all video resources and connection
     */
    public void hangUp() {
        if (Thread.currentThread() != getMainLooper().getThread()) {
            runOnUiThread(this::hangUp);
            return;
        }
        endCall(true, true);
    }

    private void finishCallActivityDelayed() {
        mainHandler.postDelayed(this::finishCallActivity, 700);
    }

    private void finishCallActivity() {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }
        if (isTaskRoot()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
        AnimationsUtil.setSlideOutAnimation(this);
    }


    /**
     * Handle onDestroy event which is implement by RtcListener class
     * Destroy the video source
     */
    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        stopTimer();

        if (isVideoCall) {
            if (remoteVideoView != null) {
                try {
                    remoteVideoView.release();
                } catch (Exception e) {
                    AppHelper.LogCat("remoteVideoView release error " + e.getMessage());
                }
            }
            if (localVideoView != null) {
                try {
                    localVideoView.release();
                } catch (Exception e) {
                    AppHelper.LogCat("localVideoView release error " + e.getMessage());
                }
            }
        }

        if (networkMonitorAutoDetect != null)
            networkMonitorAutoDetect.destroy();
        closeWebRtcClientAsync(detachWebRtcClient());


        if (voicePulsator != null && voicePulsator.isStarted())
            voicePulsator.stop();

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }


        callCleanupExecutor.shutdown();

        super.onDestroy();
    }


    public void onCallReady(String callId) {
        AppHelper.LogCat("onCallReady " + callId);
        if (callerPhone != null) {
            AppHelper.LogCat("callerPhone hmm" + callerPhone);
            if (webRtcClient != null) {
                webRtcClient.startNewCall(callerSocketId, callId, userPhone, userImage, PreferenceManager.getID(this), isVideoCall);
            } else {
                hangUp();
            }
        } else {
            AppHelper.LogCat(" answer start camera callerPhone null");
        }
    }


    /**
     * This function is being call to answer call from other user
     * send init signal to the caller and connect
     * start the camera
     *
     * @param callerId the id of the caler
     */
    public void answer(String callerId) throws JSONException {
        if (webRtcClient != null) {
            webRtcClient.signalingServer(callerId, "init", null);
        } else {
            hangUp();
        }
    }


    private void updateUserCall(String duration) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            realm.executeTransaction(realm1 -> {
            ContactsModel contactsModel;
            CallsInfoModel callsInfoModel;
            CallsModel callsModel;

            RealmQuery<CallsModel> callsModelRealmQuery;
            RealmQuery<CallsInfoModel> callsInfoModelRealmQuery;
            if (isAccepted) {
                contactsModel = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhoneAccept).findFirst();
                if (contactsModel == null) {
                    AppHelper.LogCat("updateUserCall skipped: accepted contact not found " + callerPhoneAccept);
                    return;
                }
                if (isVideoCall)
                    callsModelRealmQuery = realm1.where(CallsModel.class).equalTo("type", AppConstants.VIDEO_CALL).equalTo("from", contactsModel.getId());
                else
                    callsModelRealmQuery = realm1.where(CallsModel.class).equalTo("type", AppConstants.VOICE_CALL).equalTo("from", contactsModel.getId());
            } else {
                contactsModel = realm1.where(ContactsModel.class).equalTo("walletAddress", callerPhone).findFirst();
                if (contactsModel == null) {
                    AppHelper.LogCat("updateUserCall skipped: outgoing contact not found " + callerPhone);
                    return;
                }
                if (isVideoCall)
                    callsModelRealmQuery = realm1.where(CallsModel.class).equalTo("type", AppConstants.VIDEO_CALL).equalTo("to", contactsModel.getId());
                else
                    callsModelRealmQuery = realm1.where(CallsModel.class).equalTo("type", AppConstants.VOICE_CALL).equalTo("to", contactsModel.getId());
            }

            callsModel = callsModelRealmQuery.findAll().last();
            if (callsModel == null) {
                AppHelper.LogCat("updateUserCall skipped: call row not found");
                return;
            }
            callsModel.setDuration(duration);
            callsInfoModelRealmQuery = realm1.where(CallsInfoModel.class).equalTo("callId", callsModel.getId());
            callsInfoModel = callsInfoModelRealmQuery.findAll().last();
            if (callsInfoModel != null) {
                callsInfoModel.setDuration(duration);
            }
            realm1.copyToRealmOrUpdate(callsModel);
            if (callsInfoModel != null) {
                realm1.copyToRealmOrUpdate(callsInfoModel);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CALL_OLD_ROW, callsInfoModel.getCallId()));
            }
            String isVideo;
            if (isVideoCall) {
                isVideo = "video";
            } else {
                isVideo = "audio";
            }
            if (isAccepted) {
                CallSaverModel callSaverModel = new CallSaverModel();

                callSaverModel.setToId(callsModel.getTo());
                callSaverModel.setFromId(callsModel.getFrom());
                callSaverModel.setDate(callsModel.getDate());
                callSaverModel.setDuration(duration);
                callSaverModel.setIsVideo(isVideo);
                APIHelper.initialApiUsersContacts().saveAcceptedCall(callSaverModel).subscribe(Response -> {
                    if (Response.isSuccess()) {
                        AppHelper.LogCat(Response.getMessage());
                    } else {
                        AppHelper.LogCat(Response.getMessage());
                    }
                }, throwable -> {
                    AppHelper.LogCat(throwable.getMessage());
                });
            } else {
                CallSaverModel callSaverModel = new CallSaverModel();
                callSaverModel.setToId(callsModel.getTo());
                callSaverModel.setFromId(callsModel.getFrom());

                callSaverModel.setDate(callsModel.getDate());
                callSaverModel.setDuration(duration);
                callSaverModel.setIsVideo(isVideo);
                APIHelper.initialApiUsersContacts().saveEmittedCall(callSaverModel).subscribe(Response -> {

                }, throwable -> {

                });
            }

            });
        } catch (Exception e) {
            AppHelper.LogCat("updateUserCall failed: " + e.getMessage());
        }
        if (!realm.isClosed())
            realm.close();


    }


    public void onReject() {
        endCall(false, true);
    }

    public void onHangUp() {
        endCall(false, true);
    }


    public void onAcceptCall(String callId) {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            runOnUiThread(() -> {
                try {
                    answer(callId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    private String getTimer() {
        return callTimer.getText().toString().trim();
    }

    private void setTimer() {
        runOnUiThread(() -> callTimer.setVisibility(View.VISIBLE));
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);
    }

    private void stopTimer() {
        if (Thread.currentThread() != getMainLooper().getThread()) {
            mainHandler.post(this::stopTimer);
            return;
        }
        if (callTimer != null) {
            lastCallDuration = getTimer();
            callTimer.setVisibility(View.GONE);
        }
        timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);
    }


    public void onStatusChanged(String newStatus) {
        runOnUiThread(() -> {
            callStatus.setVisibility(View.VISIBLE);
            switch (newStatus) {
                case AppConstants.USER_DISCONNECT:
                    callStatus.setText(newStatus);
                    callStatus.setTextColor(AppHelper.getColor(this, R.color.colorRedDark));
                    break;
                case AppConstants.USER_CONNECTING:
                    if (webRtcClient != null && !isAccepted) {
                        webRtcClient.stopOutgoingSound();
                    }
                    callStatus.setText(newStatus);
                    callStatus.setTextColor(AppHelper.getColor(this, R.color.colorWhite));
                    break;
                case AppConstants.USER_CLOSED:
                    endCall(true, true);
                    break;
                case AppConstants.USER_COMPLETED:

                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }

                    callStatus.setText(AppConstants.USER_CONNECTED);
                    callStatus.setTextColor(AppHelper.getColor(this, R.color.colorWhite));
                    if (!isVideoCall) {
                        voicePulsator.setVisibility(View.VISIBLE);
                        voicePulsator.start();
                    }


                    break;
                case AppConstants.USER_CONNECTED:
                    connectionStatus.setVisibility(View.GONE);
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    setTimer();

                    callStatus.setText(newStatus);
                    callStatus.setTextColor(AppHelper.getColor(this, R.color.colorWhite));

                    new Handler().postDelayed(() -> {
                        animateHideElement(switchCamera);
                        animateHideElement(micToggle);
                        animateHideElement(speaker);
                        animateHideElement(hangUpLayout);

                    }, 3000);
                    break;
            }


        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If we've received a touch notification that the user has touched
        //make buttons showing
        if (MotionEvent.ACTION_DOWN == event.getAction()) {

            animateShowElement(switchCamera);
            animateShowElement(micToggle);
            animateShowElement(speaker);
            animateShowElement(hangUpLayout);

            return true;
        } else if (MotionEvent.ACTION_CANCEL == event.getAction()) {

            new Handler().postDelayed(() -> {
                animateHideElement(callStatus);
                animateHideElement(switchCamera);
                animateHideElement(micToggle);
                animateHideElement(speaker);
                animateHideElement(hangUpLayout);

            }, 3000);
        }

        return super.onTouchEvent(event);
    }

    private void animateHideElement(View view) {
        view.animate()
                .translationY(0)
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.GONE);
                    }
                });
    }

    private void animateShowElement(View view) {

        // Start the animation
        view.animate()
                .translationY(0)
                .alpha(1.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.VISIBLE);
                    }
                });
    }


    public void onLocalStream(MediaStream localStream) {
        if (isVideoCall) {
            AppHelper.LogCat(" onLocalStream videoTracks: " + localStream.videoTracks.size());
            if (localStream.videoTracks.size() == 0) return;
            localStream.videoTracks.get(0).addSink(localVideoView);

        }
    }

    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        if (isVideoCall) {
            AppHelper.LogCat(" onAddRemoteStream videoTracks: " + remoteStream.videoTracks.size());
            if (remoteStream.audioTracks.size() > 1 || remoteStream.videoTracks.size() > 1) {
                AppHelper.LogCat(" stream: " + remoteStream.toString());
                return;
            }
            if (remoteStream.videoTracks.size() == 1) {
                remoteStream.videoTracks.get(0).addSink(remoteVideoView);
            }

        }
    }


    public void onRemoveRemoteStream(int endPoint) {
        if (isVideoCall) {
        }
    }


    @Override
    public void onBackPressed() {
        if (!this.backPressed) {
            this.backPressed = true;
            AppHelper.CustomToast(this, "Press again to end the call.");
            this.backPressedThread = new Thread(() -> {
                try {
                    hangUp();
                    Thread.sleep(5000);
                    backPressed = false;
                } catch (InterruptedException e) {
                    AppHelper.LogCat(" Successfully interrupted");
                }
            });
            this.backPressedThread.start();
            return;
        }
        if (this.backPressedThread != null)
            this.backPressedThread.interrupt();
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;


            updatedTime = timeSwapBuff + timeInMilliseconds;


            int secs = (int) (updatedTime / 1000);

            int mins = secs / 60;

            secs = secs % 60;


            callTimer.setText("" + mins + ":" + String.format(Locale.getDefault(), "%02d", secs));
            customHandler.postDelayed(this, 1000);

        }

    };


    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallPusher pusher) {
        switch (pusher.getEvent()) {
            case AppConstants.EVENT_BUS_ACCEPT_CALL:
                onAcceptCall(pusher.getCallId());
                break;
            case AppConstants.EVENT_BUS_REJECT_CALL:
                onReject();
                break;
            case AppConstants.EVENT_BUS_HANG_UP:
                onHangUp();
                break;
            case AppConstants.EVENT_BUS_CALL_READY:
                onCallReady(pusher.getCallId());
                break;
            case AppConstants.EVENT_BUS_STATUS_CHANGED:
                onStatusChanged(pusher.getConnectionStatus());
                break;
            case AppConstants.EVENT_BUS_LOCAL_STREAM:
                onLocalStream(pusher.getMediaStream());
                break;
            case AppConstants.EVENT_BUS_ADD_REMOTE_STREAM:
                onAddRemoteStream(pusher.getMediaStream(), pusher.getEndPoint());
                break;
            case AppConstants.EVENT_BUS_REMOVE_REMOTE_STREAM:
                onRemoveRemoteStream(pusher.getEndPoint());
                break;
            case AppConstants.EVENT_BUS_ON_PEER_CLOSED:
                onPeerConnectionClosed();
                break;

        }


    }

    public void onPeerConnectionClosed() {
        AppHelper.LogCat("onPeerConnectionClosed ");
        endCall(false, true);

    }

    private void endCall(boolean notifyPeer, boolean saveCall) {
        if (Thread.currentThread() != getMainLooper().getThread()) {
            mainHandler.post(() -> endCall(notifyPeer, saveCall));
            return;
        }
        if (isEndingCall) {
            return;
        }
        isEndingCall = true;

        String duration = captureCallDuration();
        stopTimer();
        showCallEndedState();
        WebRtcClient client = detachWebRtcClient();
        finishCallActivityDelayed();

        callCleanupExecutor.execute(() -> {
            if (notifyPeer && client != null) {
                try {
                    JSONObject messageJSON = new JSONObject();
                    messageJSON.put("callerSocketId", callerSocketId);
                    messageJSON.put("userSocketId", PreferenceManager.getSocketID(getApplicationContext()));
                    client.hangUpCall(messageJSON);
                } catch (JSONException e) {
                    AppHelper.LogCat("hangUp payload failed: " + e.getMessage());
                } catch (Exception e) {
                    AppHelper.LogCat("hangUp emit failed: " + e.getMessage());
                }
            }

            closeWebRtcClient(client);

            if (saveCall) {
                updateUserCall(duration);
            }
        });
    }

    private String captureCallDuration() {
        if (callTimer == null) {
            return lastCallDuration;
        }
        String duration = callTimer.getText().toString().trim();
        if (duration.isEmpty()) {
            duration = lastCallDuration;
        }
        lastCallDuration = duration;
        return duration;
    }

    private void showCallEndedState() {
        if (callStatus == null) {
            return;
        }
        if (callStatus.getVisibility() == View.GONE) {
            callStatus.setVisibility(View.VISIBLE);
        }
        callStatus.setTextColor(AppHelper.getColor(this, R.color.colorRedDark));
        callStatus.setText(R.string.call_ended);
    }

    private synchronized WebRtcClient detachWebRtcClient() {
        WebRtcClient client = webRtcClient;
        webRtcClient = null;
        return client;
    }

    private void closeWebRtcClientAsync(WebRtcClient client) {
        if (client == null) {
            return;
        }
        callCleanupExecutor.execute(() -> closeWebRtcClient(client));
    }

    private void closeWebRtcClient(WebRtcClient client) {
        if (client == null) {
            return;
        }
        synchronized (this) {
            if (isWebRtcClosing) {
                return;
            }
            isWebRtcClosing = true;
        }
        try {
            if (!isAccepted) {
                client.stopOutgoingSound();
            }
            client.closeAllConnections();
        } catch (Exception e) {
            AppHelper.LogCat("closeWebRtcClient failed: " + e.getMessage());
        } finally {
            synchronized (this) {
                isWebRtcClosing = false;
            }
        }
    }


}
