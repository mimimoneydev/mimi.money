package com.money.mimi.activities.messages;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Vibrator;
import com.google.android.material.textfield.TextInputEditText;
import androidx.transition.TransitionManager;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.money.mimi.R;
import com.money.mimi.activities.settings.PreferenceSettingsManager;
import com.money.mimi.adapters.others.TextWatcherAdapter;
import com.money.mimi.adapters.recyclerView.messages.MessagesAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.animations.ViewAudioProxy;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.UtilsTime;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.interfaces.LoadingData;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.models.groups.MembersGroupModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.messages.UpdateMessageModel;
import com.money.mimi.models.notifications.NotificationsModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.models.users.contacts.UsersBlockModel;
import com.money.mimi.presenters.messages.MessagesPopupPresenter;
import com.money.mimi.services.MainService;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.socket.client.Socket;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static com.money.mimi.app.AppConstants.EVENT_BUS_DELETE_CONVERSATION_ITEM;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;


/**
 * Created by Abderrahim El imame on 05/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */

@SuppressLint("SetTextI18n")
public class MessagesPopupActivity extends AppCompatActivity implements LoadingData, RecyclerView.OnItemTouchListener, ActionMode.Callback, View.OnClickListener, NetworkListener {

    @BindView(R.id.activity_messages)
    LinearLayout mView;
    @BindView(R.id.listMessages)
    RecyclerView messagesList;
    @BindView(R.id.send_button)
    ImageButton SendButton;
    @BindView(R.id.send_record_button)
    ImageButton SendRecordButton;


    @BindView(R.id.emoticonBtn)
    ImageView EmoticonButton;

    @BindView(R.id.keyboradBtn)
    ImageView keyboradBtn;

    @BindView(R.id.MessageWrapper)
    EmojiconEditText messageWrapper;

    @BindView(R.id.toolbar_title)
    EmojiconTextView ToolbarTitle;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.toolbar_image)
    ImageView ToolbarImage;
    @BindView(R.id.toolbar_status)
    TextView statusUser;
    @BindView(R.id.toolbarLinear)
    LinearLayout ToolbarLinearLayout;
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.emojicons)
    FrameLayout emojiIconLayout;
    @BindView(R.id.arrow_back)
    LinearLayout BackButton;
    @BindView(R.id.send_message)
    LinearLayout SendMessageLayout;
    @BindView(R.id.groupSend)
    LinearLayout groupLeftSendMessageLayout;
    @BindView(R.id.send_message_panel)
    View sendMessagePanel;


    EmojIconActions emojIcon;
    final int MIN_INTERVAL_TIME = 2000;
    long mStartTime;
    private boolean emoticonShown = false;
    public Intent mIntent = null;
    private MessagesAdapter mMessagesAdapter;
    public Context context;
    private String messageTransfer = null;
    private ContactsModel mUsersModel;
    private GroupsModel mGroupsModel;
    private ContactsModel mUsersModelRecipient;
    private String FileSize = "0";
    private String Duration = "0";
    private String FileImagePath = null;
    private String FileVideoThumbnailPath = null;
    private String FileVideoPath = null;
    private String FileAudioPath = null;
    private String FileDocumentPath = null;
    private MessagesPopupPresenter mMessagesPresenter;
    private int ConversationID;
    private int groupID;
    private boolean isGroup;

    //for sockets
    private Socket mSocket;
    private int senderId;
    private int recipientId;
    private static final int TYPING_TIMER_LENGTH = 600;
    private boolean isTyping = false;
    private Handler mTypingHandler = new Handler();
    private boolean isSeen = false;
    private boolean isOpen;
    private Realm realm;

    //for audio
    @BindView(R.id.recording_time_text)
    TextView recordTimeText;
    @BindView(R.id.record_panel)
    View recordPanel;
    @BindView(R.id.slide_text_container)
    View slideTextContainer;
    @BindView(R.id.slideToCancelText)
    TextView slideToCancelText;
    private MediaRecorder mMediaRecorder = null;
    private float startedDraggingX = -1;
    private float distCanMove = convertToDp(80);
    private long startTime = 0L;
    private Timer recordTimer;

    /* for serach */
    @BindView(R.id.close_btn_search_view)
    ImageView closeBtn;
    @BindView(R.id.search_input)
    TextInputEditText searchInput;
    @BindView(R.id.clear_btn_search_view)
    ImageView clearBtn;
    @BindView(R.id.app_bar_search_view)
    View searchView;


    private GestureDetectorCompat gestureDetector;
    private ActionMode actionMode;
    private MemoryCache memoryCache;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_messages);
        ButterKnife.bind(this);
        ToolbarTitle.setSelected(true);
        realm = WhatsCloneApplication.getRealmDatabaseInstance();

        memoryCache = new MemoryCache();
        if (getIntent().getExtras() != null) {
            if (getIntent().hasExtra("recipientID")) {
                recipientId = getIntent().getExtras().getInt("recipientID");
            }
            if (getIntent().hasExtra("groupID")) {
                groupID = getIntent().getExtras().getInt("groupID");
            }

            if (getIntent().hasExtra("conversationID")) {
                ConversationID = getIntent().getExtras().getInt("conversationID");
            }
            if (getIntent().hasExtra("isGroup")) {
                isGroup = getIntent().getExtras().getBoolean("isGroup");
            }

        }

        connectToChatServer();
        senderId = PreferenceManager.getID(this);
        initializerView();
        setTypeFaces();
        mMessagesPresenter = new MessagesPopupPresenter(this);
        mMessagesPresenter.onCreate();
        initializerMessageWrapper();


        if (getIntent().getExtras() != null) {
            if (getIntent().hasExtra("messageCopied")) {
                ArrayList<String> messageCopied = getIntent().getExtras().getStringArrayList("messageCopied");
                for (String message : messageCopied) {
                    messageTransfer = message;
                    new Handler().postDelayed(this::sendMessage, 50);
                }
            }

        }


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        if (isGroup)
            new Handler().postDelayed(() -> unSentMessagesGroup(groupID), 1000);
        else
            new Handler().postDelayed(() -> unSentMessagesForARecipient(recipientId), 1000);

    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            slideToCancelText.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            messageWrapper.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            searchInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            ToolbarTitle.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            statusUser.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            recordTimeText.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }


    public void ShowGroupMembers(List<MembersGroupModel> groupsModelMembers) {
        if (isGroup) {
            try {
                if (groupsModelMembers.size() != 0) {
                    int arraySize = groupsModelMembers.size();
                    StringBuilder names = new StringBuilder();
                    for (int x = 0; x <= arraySize - 1; x++) {
                        if (!groupsModelMembers.get(x).isDeleted()) {
                            if (x <= 1) {
                                String finalName;
                                if (groupsModelMembers.get(x).getUserId() == PreferenceManager.getID(this)) {
                                    if (groupsModelMembers.get(x).isLeft()) {
                                        groupLeftSendMessageLayout.setVisibility(View.VISIBLE);
                                        SendMessageLayout.setVisibility(View.GONE);
                                    } else {
                                        groupLeftSendMessageLayout.setVisibility(View.GONE);
                                        SendMessageLayout.setVisibility(View.VISIBLE);
                                    }
                                    finalName = getString(R.string.you);
                                } else {
                                    String phone = getContactNameFromRealm(groupsModelMembers.get(x).getWalletAddress());
                                    if (phone != null) {
                                        try {
                                            finalName = phone.substring(0, 5);
                                        } catch (Exception e) {
                                            AppHelper.LogCat(e);
                                            finalName = phone;
                                        }
                                    } else {
                                        finalName = groupsModelMembers.get(x).getWalletAddress().substring(0, 5);
                                    }

                                }
                                names.append(finalName);
                                names.append(",");
                            }
                        }
                    }
                    String groupsNames = UtilsString.removelastString(names.toString());
                    statusUser.setVisibility(View.VISIBLE);
                    statusUser.setText(groupsNames);
                    AnimationsUtil.slideStatus(statusUser);
                }
            } catch (Exception e) {
                AppHelper.LogCat(e.getMessage());
            }

        }
    }

    /**
     * method initialize the view
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initializerView() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mMessagesAdapter = new MessagesAdapter(realm);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
        messagesList.setLayoutManager(layoutManager);
        messagesList.setAdapter(mMessagesAdapter);
        messagesList.setItemAnimator(new DefaultItemAnimator());
        messagesList.getItemAnimator().setChangeDuration(0);
        messagesList.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(this, new RecyclerViewBenOnGestureListener());

        mView.setBackground(AppHelper.getDrawable(this, R.drawable.bg_popup_msg));


        EmoticonButton.setOnClickListener(v -> {
            if (!emoticonShown) {
                emoticonShown = true;
                emojIcon = new EmojIconActions(MessagesPopupActivity.this, mView, messageWrapper, EmoticonButton);
                emojIcon.setIconsIds(R.drawable.ic_keyboard_gray_24dp, R.drawable.ic_emoticon_24dp);
                emojIcon.ShowEmojIcon();
            }

        });

        slideToCancelText.setText(R.string.slide_to_cancel_audio);
        SendButton.setOnClickListener(v -> sendMessage());
        SendRecordButton.setOnTouchListener((view, motionEvent) -> {
            setDraggingAnimation(motionEvent, view);
            return true;

        });
    }

    /**
     * method to initialize the massage wrapper
     */
    private void initializerMessageWrapper() {


        final Context context = this;
        messageWrapper.setFocusable(true);
        messageWrapper.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } else {
                AppHelper.LogCat("Has focused");
                emitMessageSeen();
                if (isGroup) {
                    new Handler().postDelayed(() -> mMessagesPresenter.updateGroupConversationStatus(), 500);
                } else {
                    new Handler().postDelayed(() -> mMessagesPresenter.updateConversationStatus(), 500);
                }
            }

        });

        messageWrapper.setOnClickListener(v1 -> {
            if (emoticonShown) {
                emoticonShown = false;
                //  EmoticonButton.setVisibility(View.VISIBLE);
                emojIcon.closeEmojIcon();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

        });
        messageWrapper.addTextChangedListener(new TextWatcherAdapter() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                SendRecordButton.setVisibility(View.VISIBLE);
                SendButton.setVisibility(View.GONE);


            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (messageWrapper.getLineCount() >= 6) {
                    messageWrapper.setScroller(new Scroller(MessagesPopupActivity.this));
                    messageWrapper.setMaxLines(6);
                    messageWrapper.setVerticalScrollBarEnabled(true);
                    messageWrapper.setMovementMethod(new ScrollingMovementMethod());
                }

                if (!isSeen)
                    emitMessageSeen();
                isSeen = true;
                SendRecordButton.setVisibility(View.GONE);
                SendButton.setVisibility(View.VISIBLE);


                if (!mSocket.connected()) return;
                if (isGroup) {
                    try {
                        if (mGroupsModel.getMembers() != null && mGroupsModel.getMembers().size() != 0) {
                            for (MembersGroupModel membersGroupModel : mGroupsModel.getMembers()) {
                                if (!isTyping && s.length() != 0) {
                                    isTyping = true;
                                    JSONObject data = new JSONObject();
                                    try {
                                        data.put("recipientId", membersGroupModel.getUserId());
                                        data.put("senderId", senderId);
                                        data.put("groupId", groupID);
                                    } catch (JSONException e) {
                                        AppHelper.LogCat(e);
                                    }
                                    mSocket.emit(AppConstants.SOCKET_IS_MEMBER_TYPING, data);
                                }

                                mTypingHandler.removeCallbacks(onTypingTimeout);
                                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
                            }
                        }
                    } catch (Exception e) {
                        AppHelper.LogCat(e);
                    }
                } else {

                    if (!isTyping && s.length() != 0) {
                        isTyping = true;
                        JSONObject data = new JSONObject();
                        try {
                            data.put("recipientId", recipientId);
                            data.put("senderId", senderId);
                        } catch (JSONException e) {
                            AppHelper.LogCat(e);
                        }
                        mSocket.emit(AppConstants.SOCKET_IS_TYPING, data);
                    }

                    mTypingHandler.removeCallbacks(onTypingTimeout);
                    mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);

                }

                if (PreferenceSettingsManager.enter_send(MessagesPopupActivity.this)) {
                    messageWrapper.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                    messageWrapper.setSingleLine(true);
                    messageWrapper.setOnEditorActionListener((v, actionId, event) -> {
                        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
                            sendMessage();
                        }
                        return false;
                    });
                }

            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    SendRecordButton.setVisibility(View.VISIBLE);
                    SendButton.setVisibility(View.GONE);
                }

            }
        });
        messageWrapper.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        messageWrapper.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        messageWrapper.setSingleLine(false);
    }

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!isTyping) return;

            isTyping = false;
            if (isGroup) {

                for (MembersGroupModel membersGroupModel : mGroupsModel.getMembers()) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("recipientId", membersGroupModel.getUserId());
                        json.put("senderId", senderId);
                        json.put("groupId", groupID);
                    } catch (JSONException e) {
                        AppHelper.LogCat(e);
                    }
                    mSocket.emit(AppConstants.SOCKET_IS_MEMBER_STOP_TYPING, json);
                    isTyping = false;


                }
            } else {
                JSONObject json = new JSONObject();
                try {
                    json.put("recipientId", recipientId);
                    json.put("senderId", senderId);
                } catch (JSONException e) {
                    AppHelper.LogCat(e);
                }
                mSocket.emit(AppConstants.SOCKET_IS_STOP_TYPING, json);
                isTyping = false;

            }
        }
    };


    /**
     * method to send the new message
     */
    private void sendMessage() {

        isSeen = false;
        if (isGroup) {
            new Handler().postDelayed(() -> mMessagesPresenter.updateGroupConversationStatus(), 500);
        } else {
            new Handler().postDelayed(() -> mMessagesPresenter.updateConversationStatus(), 500);
        }

        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_START_CONVERSATION));//for change viewpager current item to 0
        String messageBody = UtilsString.escapeJava(messageWrapper.getText().toString().trim());
        if (messageTransfer != null)
            messageBody = messageTransfer;

        if (FileImagePath == null && FileAudioPath == null && FileDocumentPath == null && FileVideoPath == null) {
            if (messageBody.isEmpty()) return;
        }

        if (!FileSize.equals("0") && Long.parseLong(FileSize) > AppConstants.MAX_UPLOAD_FILE_SIZE) {
            AppHelper.CustomToast(this, getString(R.string.file_too_large, 50));
            FileImagePath = null;
            FileVideoPath = null;
            FileAudioPath = null;
            FileDocumentPath = null;
            FileVideoThumbnailPath = null;
            FileSize = "0";
            Duration = "0";
            return;
        }
        DateTime current = new DateTime();
        String sendTime = String.valueOf(current);

        if (isGroup) {
            final JSONObject messageGroup = new JSONObject();
            try {
                messageGroup.put("messageBody", messageBody);
                messageGroup.put("senderId", senderId);
                messageGroup.put("recipientId", 0);
                try {

                    messageGroup.put("senderName", "null");

                    messageGroup.put("walletAddress", mUsersModel.getWalletAddress());
                    if (mGroupsModel.getGroupImage() != null)
                        messageGroup.put("GroupImage", mGroupsModel.getGroupImage());
                    else
                        messageGroup.put("GroupImage", "null");
                    if (mGroupsModel.getGroupName() != null)
                        messageGroup.put("GroupName", mGroupsModel.getGroupName());
                    else
                        messageGroup.put("GroupName", "null");
                } catch (Exception e) {
                    AppHelper.LogCat(e);
                }

                messageGroup.put("groupID", groupID);
                messageGroup.put("date", sendTime);
                messageGroup.put("isGroup", true);

                if (FileImagePath != null)
                    messageGroup.put("image", FileImagePath);
                else
                    messageGroup.put("image", "null");

                if (FileVideoPath != null)
                    messageGroup.put("video", FileVideoPath);
                else
                    messageGroup.put("video", "null");

                if (FileVideoThumbnailPath != null)
                    messageGroup.put("thumbnail", FileVideoThumbnailPath);
                else
                    messageGroup.put("thumbnail", "null");

                if (FileAudioPath != null)
                    messageGroup.put("audio", FileAudioPath);
                else
                    messageGroup.put("audio", "null");

                if (FileDocumentPath != null)
                    messageGroup.put("document", FileDocumentPath);
                else
                    messageGroup.put("document", "null");

                if (!FileSize.equals("0"))
                    messageGroup.put("fileSize", FileSize);
                else
                    messageGroup.put("fileSize", "0");

                if (!Duration.equals("0"))
                    messageGroup.put("duration", Duration);
                else
                    messageGroup.put("duration", "0");

                messageGroup.put("userToken", PreferenceManager.getToken(this));
            } catch (JSONException e) {
                AppHelper.LogCat("send group message " + e.getMessage());
            }
            new Handler().postDelayed(() -> {
                setStatusAsWaiting(messageGroup, true);
                unSentMessagesGroup(groupID);
            }, 100);
            AppHelper.LogCat("send group message to");

        } else {
            final JSONObject message = new JSONObject();
            try {
                message.put("messageBody", messageBody);
                message.put("recipientId", recipientId);
                message.put("senderId", senderId);
                try {

                    if (mUsersModel.getUsername() != null)
                        message.put("senderName", mUsersModel.getUsername());
                    else
                        message.put("senderName", "null");


                    if (mUsersModel.getImage() != null)
                        message.put("senderImage", mUsersModel.getImage());
                    else
                        message.put("senderImage", "null");
                    message.put("walletAddress", mUsersModel.getWalletAddress());
                    if (mUsersModelRecipient != null && mUsersModelRecipient.getWalletAddress() != null)
                        message.put("recipientWalletAddress", mUsersModelRecipient.getWalletAddress());
                } catch (Exception e) {
                    AppHelper.LogCat("Sender name " + e.getMessage());
                }


                message.put("date", sendTime);
                message.put("isGroup", false);
                message.put("conversationId", ConversationID);
                if (FileImagePath != null)
                    message.put("image", FileImagePath);
                else
                    message.put("image", "null");

                if (FileVideoPath != null)
                    message.put("video", FileVideoPath);
                else
                    message.put("video", "null");

                if (FileVideoThumbnailPath != null)
                    message.put("thumbnail", FileVideoThumbnailPath);
                else
                    message.put("thumbnail", "null");

                if (FileAudioPath != null)
                    message.put("audio", FileAudioPath);
                else
                    message.put("audio", "null");


                if (FileDocumentPath != null)
                    message.put("document", FileDocumentPath);
                else
                    message.put("document", "null");


                if (!FileSize.equals("0"))
                    message.put("fileSize", FileSize);
                else
                    message.put("fileSize", "0");

                if (!Duration.equals("0"))
                    message.put("duration", Duration);
                else
                    message.put("duration", "0");

                message.put("userToken", PreferenceManager.getToken(this));
            } catch (JSONException e) {
                AppHelper.LogCat("send message " + e.getMessage());
            }
            new Handler().postDelayed(() -> {
                setStatusAsWaiting(message, false);
                unSentMessagesForARecipient(recipientId);
            }, 100);
        }
        messageWrapper.setText("");
        messageTransfer = null;


        finish();
    }

    /**
     * method to check  for unsent messages group
     *
     * @param groupID this parameter of  unSentMessagesGroup  method
     */
    private void unSentMessagesGroup(int groupID) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

        List<MessagesModel> messagesModelsList = realm.where(MessagesModel.class)
                .notEqualTo("id", 0)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("isGroup", true)
                .equalTo("groupID", groupID)
                .equalTo("conversationID", ConversationID)
                .equalTo("isFileUpload", true)
                .equalTo("senderID", PreferenceManager.getID(this))
                .sort("id", Sort.ASCENDING).findAll();
        AppHelper.LogCat("size " + messagesModelsList.size());
        if (messagesModelsList.size() != 0) {
            for (MessagesModel messagesModel : messagesModelsList) {
                MainService.sendMessagesGroup(this, mUsersModel, mGroupsModel, messagesModel);
            }
        }
        if (!realm.isClosed())
            realm.close();
    }


    /**
     * method to check for unsent user messages
     *
     * @param recipientID this is parameter of unSentMessagesForARecipient method
     */
    private void unSentMessagesForARecipient(int recipientID) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MessagesModel> messagesModelsList = realm.where(MessagesModel.class)
                .notEqualTo("id", 0)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("recipientID", recipientID)
                .equalTo("isGroup", false)
                .equalTo("senderID", PreferenceManager.getID(this))
                .sort("id", Sort.ASCENDING).findAll();
        AppHelper.LogCat("size " + messagesModelsList.size());
        if (messagesModelsList.size() != 0) {
            for (MessagesModel messagesModel : messagesModelsList) {
                boolean hasFile = !messagesModel.getImageFile().equals("null")
                        || !messagesModel.getVideoFile().equals("null")
                        || !messagesModel.getAudioFile().equals("null")
                        || !messagesModel.getDocumentFile().equals("null");
                if (hasFile && !messagesModel.isFileUpload()) {
                    MainService.sendMessagesFiles(messagesModel);
                } else if (messagesModel.isFileUpload()) {
                    MainService.sendMessages(messagesModel);
                }
            }
        }
        realm.close();

    }

    /**
     * method to get a conversation id
     *
     * @param recipientId this is the first parameter for getConversationId method
     * @param senderId    this is the second parameter for getConversationId method
     * @param realm       this is the thirded parameter for getConversationId method
     * @return conversation id
     */
    private int getConversationId(int recipientId, int senderId, Realm realm) {
        try {
            ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                    .beginGroup()
                    .equalTo("RecipientID", recipientId)
                    .or()
                    .equalTo("RecipientID", senderId)
                    .endGroup().findFirst();
            return conversationsModelNew.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Get conversation id Exception MessagesPopupActivity " + e.getMessage());
            return 0;
        }
    }


    /**
     * method to save new message as waitng messages
     *
     * @param data    this is the first parameter for setStatusAsWaiting method
     * @param isgroup this is the second parameter for setStatusAsWaiting method
     */
    private void setStatusAsWaiting(JSONObject data, boolean isgroup) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            if (isgroup) {
                int senderId = data.getInt("senderId");
                String messageBody = data.getString("messageBody");
                String senderName = data.getString("senderName");
                String senderPhone = data.getString("walletAddress");
                String GroupImage = data.getString("GroupImage");
                String GroupName = data.getString("GroupName");
                String dateTmp = data.getString("date");
                String video = data.getString("video");
                String thumbnail = data.getString("thumbnail");
                boolean isGroup = data.getBoolean("isGroup");
                String image = data.getString("image");
                String audio = data.getString("audio");
                String document = data.getString("document");
                String fileSize = data.getString("fileSize");
                String duration = data.getString("duration");
                int groupID = data.getInt("groupID");
                realm.executeTransactionAsync(realm1 -> {

                    int lastID = RealmBackupRestore.getMessageLastId();
                    ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("groupID", groupID).findFirst();
                    RealmList<MessagesModel> messagesModelRealmList = conversationsModel.getMessages();
                    MessagesModel messagesModel = new MessagesModel();
                    messagesModel.setId(lastID);
                    messagesModel.setDate(dateTmp);
                    messagesModel.setStatus(AppConstants.IS_WAITING);
                    messagesModel.setUsername(senderName);
                    messagesModel.setSenderID(PreferenceManager.getID(this));
                    messagesModel.setGroup(isGroup);
                    messagesModel.setMessage(messageBody);
                    messagesModel.setGroupID(groupID);
                    messagesModel.setImageFile(image);
                    messagesModel.setVideoFile(video);
                    messagesModel.setAudioFile(audio);
                    messagesModel.setDocumentFile(document);
                    messagesModel.setFileSize(fileSize);
                    messagesModel.setDuration(duration);
                    messagesModel.setVideoThumbnailFile(thumbnail);
                    if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null")) {
                        messagesModel.setFileUpload(false);
                        messagesModel.setFileDownLoad(false);
                    } else {
                        messagesModel.setFileUpload(true);
                        messagesModel.setFileDownLoad(true);
                    }
                    messagesModel.setConversationID(conversationsModel.getId());
                    messagesModelRealmList.add(messagesModel);
                    conversationsModel.setLastMessage(messageBody);
                    conversationsModel.setLastMessageId(lastID);
                    conversationsModel.setMessages(messagesModelRealmList);
                    conversationsModel.setStatus(AppConstants.IS_WAITING);
                    conversationsModel.setUnreadMessageCounter("0");
                    conversationsModel.setRecipientID(0);
                    realm1.copyToRealmOrUpdate(conversationsModel);
                    runOnUiThread(() -> addMessage(messagesModel));


                }, () -> {
                    if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null"))
                        return;

                    UpdateMessageModel updateMessageModel = new UpdateMessageModel();
                    try {
                        updateMessageModel.setSenderId(data.getInt("senderId"));
                        updateMessageModel.setRecipientId(data.getInt("recipientId"));
                        updateMessageModel.setMessageBody(data.getString("messageBody"));
                        updateMessageModel.setSenderName(data.getString("senderName"));
                        updateMessageModel.setGroupName(data.getString("GroupName"));
                        updateMessageModel.setGroupImage(data.getString("GroupImage"));
                        updateMessageModel.setGroupID(data.getInt("groupID"));
                        updateMessageModel.setDate(data.getString("date"));
                        updateMessageModel.setWalletAddress(data.getString("walletAddress"));
                        updateMessageModel.setVideo(data.getString("video"));
                        updateMessageModel.setThumbnail(data.getString("thumbnail"));
                        updateMessageModel.setImage(data.getString("image"));
                        updateMessageModel.setAudio(data.getString("audio"));
                        updateMessageModel.setDocument(data.getString("document"));
                        updateMessageModel.setFileSize(data.getString("fileSize"));
                        updateMessageModel.setDuration(data.getString("duration"));
                        updateMessageModel.setGroup(data.getBoolean("isGroup"));
                        updateMessageModel.setUserToken(PreferenceManager.getToken(MessagesPopupActivity.this));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    MainService.sendMessage(updateMessageModel, true);

                }, error -> {
                    AppHelper.LogCat("Save group message failed MessagesPopupActivity " + error.getMessage());
                });


            } else {
                AppHelper.LogCat("esedd message ");

                int senderId = data.getInt("senderId");
                int recipientId = data.getInt("recipientId");
                String messageBody = data.getString("messageBody");
                String senderName = data.getString("senderName");
                String dateTmp = data.getString("date");
                String video = data.getString("video");
                String thumbnail = data.getString("thumbnail");
                boolean isGroup = data.getBoolean("isGroup");
                String image = data.getString("image");
                String audio = data.getString("audio");
                String document = data.getString("document");
                String phone = data.getString("walletAddress");
                String fileSize = data.getString("fileSize");
                String duration = data.getString("duration");

                String recipientName = mUsersModelRecipient.getUsername();
                String recipientImage = mUsersModelRecipient.getImage();
                String recipientPhone = mUsersModelRecipient.getWalletAddress();
                int conversationID = getConversationId(recipientId, senderId, realm);
                if (conversationID == 0) {
                    realm.executeTransactionAsync(realm1 -> {


                        int lastConversationID = RealmBackupRestore.getConversationLastId();
                        int lastID = RealmBackupRestore.getMessageLastId();
                        RealmList<MessagesModel> messagesModelRealmList = new RealmList<MessagesModel>();
                        MessagesModel messagesModel = new MessagesModel();
                        messagesModel.setId(lastID);
                        messagesModel.setUsername(senderName);
                        messagesModel.setRecipientID(recipientId);
                        messagesModel.setDate(dateTmp);
                        messagesModel.setStatus(AppConstants.IS_WAITING);
                        messagesModel.setGroup(isGroup);
                        messagesModel.setSenderID(senderId);
                        messagesModel.setConversationID(lastConversationID);
                        messagesModel.setMessage(messageBody);
                        messagesModel.setImageFile(image);
                        messagesModel.setVideoFile(video);
                        messagesModel.setAudioFile(audio);
                        messagesModel.setDocumentFile(document);
                        messagesModel.setFileSize(fileSize);
                        messagesModel.setDuration(duration);
                        messagesModel.setVideoThumbnailFile(thumbnail);
                        if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null")) {
                            messagesModel.setFileUpload(false);
                            messagesModel.setFileDownLoad(false);
                        } else {
                            messagesModel.setFileUpload(true);
                            messagesModel.setFileDownLoad(true);
                        }
                        messagesModel.setWalletAddress(phone);
                        messagesModelRealmList.add(messagesModel);
                        ConversationsModel conversationsModel1 = new ConversationsModel();
                        conversationsModel1.setRecipientID(recipientId);
                        conversationsModel1.setLastMessage(messageBody);
                        conversationsModel1.setRecipientUsername(recipientName);
                        conversationsModel1.setRecipientImage(recipientImage);
                        conversationsModel1.setMessageDate(dateTmp);
                        conversationsModel1.setId(lastConversationID);
                        conversationsModel1.setStatus(AppConstants.IS_WAITING);
                        conversationsModel1.setRecipientPhone(recipientPhone);
                        conversationsModel1.setMessages(messagesModelRealmList);
                        conversationsModel1.setUnreadMessageCounter("0");
                        conversationsModel1.setLastMessageId(lastID);
                        conversationsModel1.setCreatedOnline(true);
                        realm1.copyToRealmOrUpdate(conversationsModel1);
                        ConversationID = lastConversationID;
                        runOnUiThread(() -> addMessage(messagesModel));
                        try {
                            data.put("messageId", lastID);
                            data.put("conversationId", lastConversationID);
                        } catch (JSONException e) {
                            AppHelper.LogCat("last id");
                        }
                    }, () -> {

                        if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null"))
                            return;
                        UpdateMessageModel updateMessageModel = new UpdateMessageModel();

                        try {
                            updateMessageModel.setSenderId(data.getInt("senderId"));
                            updateMessageModel.setRecipientId(data.getInt("recipientId"));
                            updateMessageModel.setMessageId(data.getInt("messageId"));
                            updateMessageModel.setConversationId(data.getInt("conversationId"));
                            updateMessageModel.setMessageBody(data.getString("messageBody"));
                            updateMessageModel.setSenderName(data.getString("senderName"));
                            updateMessageModel.setSenderImage(data.getString("senderImage"));
                            updateMessageModel.setWalletAddress(data.getString("walletAddress"));
                            updateMessageModel.setRecipientWalletAddress(recipientPhone);
                            updateMessageModel.setDate(data.getString("date"));
                            updateMessageModel.setVideo(data.getString("video"));
                            updateMessageModel.setThumbnail(data.getString("thumbnail"));
                            updateMessageModel.setImage(data.getString("image"));
                            updateMessageModel.setAudio(data.getString("audio"));
                            updateMessageModel.setDocument(data.getString("document"));
                            updateMessageModel.setFileSize(data.getString("fileSize"));
                            updateMessageModel.setDuration(data.getString("duration"));
                            updateMessageModel.setGroup(data.getBoolean("isGroup"));
                            updateMessageModel.setUserToken(PreferenceManager.getToken(MessagesPopupActivity.this));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        MainService.sendMessage(updateMessageModel, false);
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_NEW_ROW, ConversationID));
                    }, error -> AppHelper.LogCat("Error  conversation id MessagesActivity " + error.getMessage()));


                } else {

                    realm.executeTransactionAsync(realm1 -> {
                        try {


                            int lastID = RealmBackupRestore.getMessageLastId();

                            AppHelper.LogCat("last ID  message   MessagesActivity" + lastID);
                            ConversationsModel conversationsModel;
                            RealmQuery<ConversationsModel> conversationsModelRealmQuery = realm1.where(ConversationsModel.class).equalTo("id", conversationID);
                            conversationsModel = conversationsModelRealmQuery.findAll().first();
                            MessagesModel messagesModel = new MessagesModel();
                            messagesModel.setId(lastID);
                            messagesModel.setUsername(senderName);
                            messagesModel.setRecipientID(recipientId);
                            messagesModel.setDate(dateTmp);
                            messagesModel.setStatus(AppConstants.IS_WAITING);
                            messagesModel.setGroup(isGroup);
                            messagesModel.setSenderID(senderId);
                            messagesModel.setConversationID(conversationID);
                            messagesModel.setMessage(messageBody);
                            messagesModel.setImageFile(image);
                            messagesModel.setVideoFile(video);
                            messagesModel.setAudioFile(audio);
                            messagesModel.setDocumentFile(document);
                            messagesModel.setFileSize(fileSize);
                            messagesModel.setDuration(duration);
                            messagesModel.setVideoThumbnailFile(thumbnail);
                            if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null")) {
                                messagesModel.setFileUpload(false);
                                messagesModel.setFileDownLoad(false);
                            } else {
                                messagesModel.setFileUpload(true);
                                messagesModel.setFileDownLoad(true);
                            }
                            messagesModel.setWalletAddress(phone);
                            conversationsModel.getMessages().add(messagesModel);
                            conversationsModel.setLastMessageId(lastID);
                            conversationsModel.setLastMessage(messageBody);
                            conversationsModel.setMessageDate(dateTmp);
                            conversationsModel.setCreatedOnline(true);
                            realm1.copyToRealmOrUpdate(conversationsModel);
                            runOnUiThread(() -> addMessage(messagesModel));
                            try {
                                data.put("messageId", lastID);
                            } catch (JSONException e) {
                                AppHelper.LogCat("last id");
                            }
                        } catch (Exception e) {
                            AppHelper.LogCat("Exception  last id message  MessagesActivity " + e.getMessage());
                        }
                    }, () -> {

                        if (!image.equals("null") || !video.equals("null") || !audio.equals("null") || !document.equals("null") || !thumbnail.equals("null"))
                            return;
                        UpdateMessageModel updateMessageModel = new UpdateMessageModel();
                        try {
                            updateMessageModel.setSenderId(data.getInt("senderId"));
                            updateMessageModel.setRecipientId(data.getInt("recipientId"));
                            updateMessageModel.setMessageId(data.getInt("messageId"));
                            updateMessageModel.setConversationId(data.getInt("conversationId"));
                            updateMessageModel.setMessageBody(data.getString("messageBody"));
                            updateMessageModel.setSenderName(data.getString("senderName"));
                            updateMessageModel.setSenderImage(data.getString("senderImage"));
                            updateMessageModel.setWalletAddress(data.getString("walletAddress"));
                            updateMessageModel.setRecipientWalletAddress(recipientPhone);
                            updateMessageModel.setDate(data.getString("date"));
                            updateMessageModel.setVideo(data.getString("video"));
                            updateMessageModel.setThumbnail(data.getString("thumbnail"));
                            updateMessageModel.setImage(data.getString("image"));
                            updateMessageModel.setAudio(data.getString("audio"));
                            updateMessageModel.setDocument(data.getString("document"));
                            updateMessageModel.setFileSize(data.getString("fileSize"));
                            updateMessageModel.setDuration(data.getString("duration"));
                            updateMessageModel.setGroup(data.getBoolean("isGroup"));
                            updateMessageModel.setUserToken(PreferenceManager.getToken(MessagesPopupActivity.this));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        MainService.sendMessage(updateMessageModel, false);
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_OLD_ROW, conversationID));
                    }, error -> AppHelper.LogCat("Error  last id  MessagesActivity " + error.getMessage()));
                }
            }


        } catch (JSONException e) {
            AppHelper.LogCat("JSONException  MessagesActivity " + e);
        }

        FileAudioPath = null;
        FileVideoPath = null;
        FileDocumentPath = null;
        FileImagePath = null;
        FileVideoThumbnailPath = null;
        FileSize = "0";
        Duration = "0";
        if (!realm.isClosed())
            realm.close();
    }


    /**
     * method to emit that message are seen by user
     */
    private void emitMessageSeen() {
        if (isGroup) {
            MainService.RecipientMarkMessageAsSeenGroup(this, groupID);
        } else {
            JSONObject json = new JSONObject();
            try {
                json.put("recipientId", recipientId);
                json.put("senderId", senderId);

                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_SEEN, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * method to show all user messages
     *
     * @param messagesModels this is parameter for ShowMessages method
     */
    public void ShowMessages(List<MessagesModel> messagesModels) {

        RealmList<MessagesModel> mMessagesList = new RealmList<MessagesModel>();
        for (MessagesModel messagesModel : messagesModels) {
            mMessagesList.add(messagesModel);
            ConversationID = messagesModel.getConversationID();
        }
        mMessagesAdapter.setMessages(mMessagesList);
    }

    /**
     * method to update  contact information
     *
     * @param contactsModels this is parameter for updateContact method
     */
    public void updateContact(ContactsModel contactsModels) {
        mUsersModel = contactsModels;
    }


    /**
     * method to update group information
     *
     * @param groupsModel
     */
    @SuppressLint("StaticFieldLeak")
    public void updateGroupInfo(GroupsModel groupsModel) {
        mGroupsModel = groupsModel;
        String groupImage = groupsModel.getGroupImage();

        String name = UtilsString.unescapeJava(groupsModel.getGroupName());
        ToolbarTitle.setText(name);
        TextDrawable drawable = textDrawable(name);
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return ImageLoader.GetCachedBitmapImage(memoryCache, groupImage, MessagesPopupActivity.this, groupID, AppConstants.GROUP, AppConstants.ROW_PROFILE);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    ImageLoader.SetBitmapImage(bitmap, ToolbarImage);
                } else {
                    Glide.with(MessagesPopupActivity.this)
                            .load(EndPoints.ROWS_IMAGE_URL + groupsModel.getGroupImage())
                            .asBitmap()
                            .centerCrop()
                            .transform(new CropCircleTransformation(MessagesPopupActivity.this))
                            .placeholder(drawable)
                            .error(drawable)
                            .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                            .into(ToolbarImage);
                }
            }
        }.execute();
        List<MembersGroupModel> groupsModelMembers = groupsModel.getMembers();// realm.where(MembersGroupModel.class).equalTo("groupID", groupID).equalTo("Deleted", false).findAll();
        int arraySize = groupsModelMembers.size();
        StringBuilder names = new StringBuilder();
        for (int x = 0; x <= arraySize - 1; x++) {
            if (!groupsModelMembers.get(x).isLeft() && !groupsModelMembers.get(x).isDeleted()) {
                if (x <= 1) {
                    String finalName;
                    if (groupsModelMembers.get(x).getUserId() == PreferenceManager.getID(this)) {
                        if (groupsModelMembers.get(x).isLeft()) {
                            groupLeftSendMessageLayout.setVisibility(View.VISIBLE);
                            SendMessageLayout.setVisibility(View.GONE);
                        } else {
                            groupLeftSendMessageLayout.setVisibility(View.GONE);
                            SendMessageLayout.setVisibility(View.VISIBLE);
                        }
                        finalName = getString(R.string.you);
                    } else {
                        String phone = getContactNameFromRealm(groupsModelMembers.get(x).getWalletAddress());
                        if (phone != null) {
                            try {
                                finalName = phone.substring(0, 5);
                            } catch (Exception e) {
                                AppHelper.LogCat(e);
                                finalName = phone;
                            }
                        } else {
                            finalName = groupsModelMembers.get(x).getWalletAddress().substring(0, 5);
                        }

                    }
                    names.append(finalName);
                    names.append(",");
                }
            }
        }
        String groupsNames = UtilsString.removelastString(names.toString());
        statusUser.setVisibility(View.VISIBLE);
        statusUser.setText(groupsNames);
        AnimationsUtil.slideStatus(statusUser);
    }

    @SuppressLint("StaticFieldLeak")
    public void updateContactRecipient(ContactsModel contactsModels) {
        mUsersModelRecipient = contactsModels;
        String name = null;
        try {
            if (contactsModels.getUsername() != null) {
                ToolbarTitle.setText(contactsModels.getUsername());
                name = contactsModels.getUsername();
            } else {
                name = getContactNameFromRealm(contactsModels.getWalletAddress());
                if (name != null) {
                    ToolbarTitle.setText(name);
                } else {
                    ToolbarTitle.setText(contactsModels.getWalletAddress());
                    name = contactsModels.getWalletAddress();
                }
            }

        } catch (Exception e) {
            AppHelper.LogCat(" Recipient username  is null MessagesPopupActivity" + e.getMessage());
        }
        TextDrawable drawable = textDrawable(name);
        String imageUser = contactsModels.getImage();
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return ImageLoader.GetCachedBitmapImage(memoryCache, imageUser, MessagesPopupActivity.this, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    ImageLoader.SetBitmapImage(bitmap, ToolbarImage);
                } else {

                    Glide.with(MessagesPopupActivity.this)
                            .load(EndPoints.ROWS_IMAGE_URL + contactsModels.getImage())
                            .asBitmap()
                            .centerCrop()
                            .transform(new CropCircleTransformation(MessagesPopupActivity.this))
                            .placeholder(drawable)
                            .error(drawable)
                            .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                            .into(ToolbarImage);
                }
            }
        }.execute();

    }

    private TextDrawable textDrawable(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = getApplicationContext().getString(R.string.app_name);
        }
        ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
        // generate random color
        int color = generator.getColor(name);
        String c = String.valueOf(name.toUpperCase().charAt(0));
        return TextDrawable.builder().buildRound(c, color);


    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBackPressed() {
        if (emoticonShown) {
            emoticonShown = false;
            //   EmoticonButton.setVisibility(View.VISIBLE);
            emojIcon.closeEmojIcon();
            SendMessageLayout.setBackground(getResources().getDrawable(android.R.color.transparent));
        } else {
            super.onBackPressed();
            mMessagesAdapter.stopAudio();
            if (NotificationsManager.getManager()) {
                if (isGroup)
                    NotificationsManager.cancelNotification(groupID);
                else
                    NotificationsManager.cancelNotification(recipientId);
            }
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMessagesPresenter.onDestroy();
        realm.close();
    }

    @Override
    public void onShowLoading() {

    }

    @Override
    public void onHideLoading() {

    }

    @Override
    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("Messages " + throwable.getMessage());
    }

    /**
     * method to connect to the chat sever by socket
     */
    private void connectToChatServer() {

        WhatsCloneApplication app = (WhatsCloneApplication) getApplication();
        mSocket = app.getSocket();
        if (mSocket == null || !mSocket.connected()) {
            AppHelper.startMainService(this);
            return;
        }
        emitUserIsOnline();
        if (isGroup) {
            AppHelper.LogCat("here group seen");
        } else {
            emitMessageSeen();
        }

    }


    private void emitUserIsOnline() {

        JSONObject json = new JSONObject();
        try {
            json.put("connected", true);
            json.put("senderId", senderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit(AppConstants.SOCKET_IS_ONLINE, json);
    }


     private boolean checkIfUserBlockedExist(int userId, Realm realm) {
         RealmQuery<UsersBlockModel> query = realm.where(UsersBlockModel.class).equalTo("contactsModel.id", userId);
         return query.count() != 0;
     }

    private String getContactNameFromRealm(String walletAddress) {
        try {
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            ContactsModel contact = realm.where(ContactsModel.class)
                    .equalTo("walletAddress", walletAddress)
                    .findFirst();
            String name = null;
            if (contact != null) {
                name = contact.getUsername();
            }
            if (!realm.isClosed()) realm.close();
            return name;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * method to update  group members  to show them on toolbar status
     *
     * @param statusUserTyping this is the first parameter for  updateGroupMemberStatus method
     * @param memberName       this is the second parameter for updateGroupMemberStatus method
     */
    private void updateGroupMemberStatus(int statusUserTyping, String memberName) {
        StringBuilder names = new StringBuilder();
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MembersGroupModel> groups = realm.where(MembersGroupModel.class).equalTo("groupID", groupID).equalTo("Deleted", false).equalTo("isLeft", false).findAll();
        int arraySize = groups.size();
        if (arraySize != 0) {
            for (int x = 0; x < arraySize; x++) {
                if (x <= 1) {
                    String finalName;
                    if (groups.get(x).getUserId() == PreferenceManager.getID(this)) {
                        finalName = "You";

                    } else {
                        String phone = getContactNameFromRealm(groups.get(x).getWalletAddress());
                        if (phone != null) {
                            try {
                                finalName = phone.substring(0, 7);
                            } catch (Exception e) {
                                AppHelper.LogCat(e);
                                finalName = phone;
                            }

                        } else {
                            finalName = groups.get(x).getWalletAddress().substring(0, 7);
                        }

                    }
                    names.append(finalName);
                    names.append(",");

                }

            }
        } else {
            names.append("");
        }

        String groupsNames = UtilsString.removelastString(names.toString());
        switch (statusUserTyping) {
            case AppConstants.STATUS_USER_TYPING:
                statusUser.setVisibility(View.VISIBLE);
                statusUser.setText(memberName + " " + getString(R.string.isTyping));
                break;
            case AppConstants.STATUS_USER_STOP_TYPING:
                statusUser.setVisibility(View.VISIBLE);
                statusUser.setText(groupsNames);
                break;
            default:
                statusUser.setVisibility(View.VISIBLE);
                statusUser.setText(groupsNames);
                break;
        }

        if (!realm.isClosed()) realm.close();
    }

    private void showStatus() {
        TransitionManager.beginDelayedTransition(mView);
        statusUser.setVisibility(View.VISIBLE);
    }


    /**
     * method to update user status
     *
     * @param statusUserTyping this is the first parameter for  updateUserStatus method
     */
    private void updateUserStatus(int statusUserTyping) {
        if (isGroup) return;
        if (!checkIfUserBlockedExist(recipientId, realm)) {
            switch (statusUserTyping) {
                case AppConstants.STATUS_USER_TYPING:
                    showStatus();
                    statusUser.setText(getString(R.string.isTyping));
                    AppHelper.LogCat("typing...");
                    break;
                case AppConstants.STATUS_USER_DISCONNECTED:
                    showStatus();
                    statusUser.setText(getString(R.string.isOffline));
                    AppHelper.LogCat("Offline...");
                    break;
                case AppConstants.STATUS_USER_CONNECTED:
                    showStatus();
                    statusUser.setText(getString(R.string.isOnline));
                    AnimationsUtil.slideStatus(statusUser);
                    AppHelper.LogCat("Online...");
                    break;
                case AppConstants.STATUS_USER_STOP_TYPING:
                    showStatus();
                    statusUser.setText(getString(R.string.isOnline));
                    break;

            }
        }

    }

    /**
     * method to check if user is exist
     *
     * @param id    this is the first parameter for checkIfUserIsExist method
     * @param realm this is the second parameter for checkIfUserIsExist method
     * @return this is for what checkIfUserIsExist method will return
     */
    private boolean checkIfUserIsExist(int id, Realm realm) {
        RealmQuery<ContactsModel> query = realm.where(ContactsModel.class).equalTo("id", id);
        return query.count() == 0 ? false : true;

    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        int messageId = pusher.getMessageId();
        switch (pusher.getAction()) {
            case AppConstants.EVENT_BUS_NEW_MESSAGE_MESSAGES_NEW_ROW:
                MessagesModel messagesModel = pusher.getMessagesModel();
                if (messagesModel.getSenderID() == recipientId && messagesModel.getRecipientID() == senderId) {
                    addMessage(messagesModel);
                    new Handler().postDelayed(() -> mMessagesPresenter.updateConversationStatus(), 500);
                }

                break;
            case AppConstants.EVENT_BUS_NEW_GROUP_MESSAGE_MESSAGES_NEW_ROW:
                if (isGroup) {
                    MessagesModel messagesModel1 = pusher.getMessagesModel();
                    if (messagesModel1.getSenderID() != PreferenceManager.getID(this)) {
                        addMessage(messagesModel1);
                        new Handler().postDelayed(() -> mMessagesPresenter.updateGroupConversationStatus(), 500);
                    }
                }
                break;


            case AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_MESSAGES:
            case AppConstants.EVENT_BUS_MESSAGE_IS_SENT_FOR_MESSAGES:
            case AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_MESSAGES:
                new Handler().postDelayed(() -> mMessagesAdapter.updateStatusMessageItem(messageId), 500);
                break;
            case AppConstants.EVENT_BUS_UPLOAD_MESSAGE_FILES:
                if (pusher.getMessagesModel().isGroup())
                    unSentMessagesGroup(pusher.getMessagesModel().getGroupID());
                else
                    unSentMessagesForARecipient(pusher.getMessagesModel().getRecipientID());
                break;

            case AppConstants.EVENT_BUS_ITEM_IS_ACTIVATED_MESSAGES:
                int idx = messagesList.getChildAdapterPosition(pusher.getView());
                if (actionMode != null) {
                    ToggleSelection(idx);
                    return;
                }
                break;

            case AppConstants.EVENT_BUS_NEW_USER_NOTIFICATION:
                NotificationsModel newUserNotification = pusher.getNotificationsModel();
                if (newUserNotification.getRecipientId() == recipientId) {
                    return;
                } else {

                    if (newUserNotification.getAppName() != null && newUserNotification.getAppName().equals(getApplicationContext().getPackageName())) {

                        if (newUserNotification.getFile() != null) {
                            NotificationsManager.showUserNotification(getApplicationContext(), newUserNotification.getConversationID(), newUserNotification.getWalletAddress(), newUserNotification.getFile(), recipientId, newUserNotification.getImage());
                        } else {
                            NotificationsManager.showUserNotification(getApplicationContext(), newUserNotification.getConversationID(), newUserNotification.getWalletAddress(), newUserNotification.getMessage(), recipientId, newUserNotification.getImage());
                        }
                    }

                }

                break;
            case AppConstants.EVENT_BUS_NEW_GROUP_NOTIFICATION:
                NotificationsModel newGroupNotification = pusher.getNotificationsModel();
                if (newGroupNotification.getGroupID() == groupID) {
                    return;
                } else {
                    if (newGroupNotification.getAppName() != null && newGroupNotification.getAppName().equals(getApplicationContext().getPackageName())) {

                        /**
                         * this for default activity
                         */
                        Intent messagingGroupIntent = new Intent(getApplicationContext(), MessagesActivity.class);
                        messagingGroupIntent.putExtra("conversationID", newGroupNotification.getConversationID());
                        messagingGroupIntent.putExtra("groupID", newGroupNotification.getGroupID());
                        messagingGroupIntent.putExtra("isGroup", newGroupNotification.isGroup());
                        messagingGroupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        /**
                         * this for popup activity
                         */
                        Intent messagingGroupPopupIntent = new Intent(getApplicationContext(), MessagesPopupActivity.class);
                        messagingGroupPopupIntent.putExtra("conversationID", newGroupNotification.getConversationID());
                        messagingGroupPopupIntent.putExtra("groupID", newGroupNotification.getGroupID());
                        messagingGroupPopupIntent.putExtra("isGroup", newGroupNotification.isGroup());
                        messagingGroupPopupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        String message;
                        String userName = getContactNameFromRealm(newGroupNotification.getWalletAddress());
                        switch (newGroupNotification.getMessage()) {
                            case AppConstants.CREATE_GROUP:
                                if (userName != null) {
                                    message = "" + userName + getApplicationContext().getString(R.string.he_created_this_group);
                                } else {
                                    message = "" + newGroupNotification.getWalletAddress() + getApplicationContext().getString(R.string.he_created_this_group);
                                }


                                break;
                            case AppConstants.LEFT_GROUP:
                                if (userName != null) {
                                    message = "" + userName + getApplicationContext().getString(R.string.he_left);
                                } else {
                                    message = "" + newGroupNotification.getWalletAddress() + getApplicationContext().getString(R.string.he_left);
                                }

                                break;
                            default:
                                message = newGroupNotification.getMessage();
                                break;
                        }
                        if (newGroupNotification.getFile() != null) {
                            NotificationsManager.showGroupNotification(getApplicationContext(), messagingGroupIntent, messagingGroupPopupIntent, newGroupNotification.getGroupName(), newGroupNotification.getMemberName() + " : " + newGroupNotification.getFile(), newGroupNotification.getGroupID(), newGroupNotification.getImage());
                        } else {
                            NotificationsManager.showGroupNotification(getApplicationContext(), messagingGroupIntent, messagingGroupPopupIntent, newGroupNotification.getGroupName(), newGroupNotification.getMemberName() + " : " + message, newGroupNotification.getGroupID(), newGroupNotification.getImage());
                        }
                    }
                }

                break;
            case AppConstants.EVENT_BUS_USER_TYPING:
                if (!checkIfUserBlockedExist(recipientId, realm)) {
                    if (pusher.getSenderID() == recipientId && pusher.getRecipientID() == senderId) {
                        updateUserStatus(AppConstants.STATUS_USER_TYPING);
                    }
                }
                break;
            case AppConstants.EVENT_BUS_USER_STOP_TYPING:
                if (!checkIfUserBlockedExist(recipientId, realm)) {
                    if (pusher.getSenderID() == recipientId && pusher.getRecipientID() == senderId) {
                        updateUserStatus(AppConstants.STATUS_USER_STOP_TYPING);
                    }
                }
                break;
            case AppConstants.EVENT_BUS_MEMBER_TYPING:
                if (pusher.getGroupID() != 0) {
                    if (!checkIfUserBlockedExist(pusher.getSenderID(), realm)) {
                        ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("id", pusher.getSenderID()).findFirst();
                        String finalName;
                        String name = getContactNameFromRealm(contactsModel.getWalletAddress());
                        if (name != null) {
                            finalName = name;
                        } else {
                            finalName = contactsModel.getWalletAddress();
                        }
                        if (pusher.getGroupID() == groupID) {
                            if (pusher.getSenderID() == PreferenceManager.getID(this)) return;
                            updateGroupMemberStatus(AppConstants.STATUS_USER_TYPING, finalName);
                        }
                    }
                }

                break;
            case AppConstants.EVENT_BUS_MEMBER_STOP_TYPING:
                updateGroupMemberStatus(AppConstants.STATUS_USER_STOP_TYPING, null);
                break;

            case AppConstants.EVENT_BUS_UPDATE_USER_STATE:
                if (pusher.getData().equals(AppConstants.EVENT_BUS_USER_IS_ONLINE))
                    updateUserStatus(AppConstants.STATUS_USER_CONNECTED);
                else if (pusher.getData().equals(AppConstants.EVENT_BUS_USER_IS_OFFLINE))
                    updateUserStatus(AppConstants.STATUS_USER_DISCONNECTED);
                break;

        }
        //  });
    }


    /**
     * method to add a new message to list messages
     *
     * @param newMsg this is the parameter for addMessage
     */

    private void addMessage(MessagesModel newMsg) {
        mMessagesAdapter.addMessage(newMsg);
        scrollToBottom();
    }

    /**
     * method to scroll to the bottom of list
     */
    private void scrollToBottom() {
        messagesList.scrollToPosition(mMessagesAdapter.getItemCount() - 1);
    }

    /**
     * method to set teh draging animation for audio layout
     *
     * @param motionEvent this is the first parameter for setDraggingAnimation  method
     * @param view        this the second parameter for  setDraggingAnimation  method
     * @return this is what method will return
     */
    private boolean setDraggingAnimation(MotionEvent motionEvent, View view) {

        sendMessagePanel.setVisibility(View.GONE);
        recordPanel.setVisibility(View.VISIBLE);
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideTextContainer
                    .getLayoutParams();
            params.leftMargin = convertToDp(30);
            slideTextContainer.setLayoutParams(params);
            ViewAudioProxy.setAlpha(slideTextContainer, 1);
            startedDraggingX = -1;
            mStartTime = System.currentTimeMillis();
            startRecording();
            SendRecordButton.getParent().requestDisallowInterceptTouchEvent(true);
            recordPanel.setVisibility(View.VISIBLE);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            startedDraggingX = -1;
            recordPanel.setVisibility(View.GONE);
            sendMessagePanel.setVisibility(View.VISIBLE);

            long intervalTime = System.currentTimeMillis() - mStartTime;
            if (intervalTime < MIN_INTERVAL_TIME) {

                messageWrapper.setError(getString(R.string.hold_to_record));
                try {
                    if (FilesManager.isFileRecordExists(FileAudioPath)) {
                        boolean deleted = FilesManager.getFileRecord(FileAudioPath).delete();
                        if (deleted)
                            FileAudioPath = null;
                    }
                } catch (Exception e) {
                    AppHelper.LogCat("Exception record path file  MessagesPopupActivity");
                }
            } else {

                sendMessage();
                FileAudioPath = null;

            }
            stopRecording();
        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            float x = motionEvent.getX();
            if (x < -distCanMove) {
                AppHelper.LogCat("here we will delete  the file ");
                try {
                    if (FilesManager.isFileRecordExists(FileAudioPath)) {
                        boolean deleted = FilesManager.getFileRecord(FileAudioPath).delete();
                        if (deleted)
                            FileAudioPath = null;
                    }


                } catch (Exception e) {
                    AppHelper.LogCat("Exception exist record  " + e.getMessage());
                }
                FileAudioPath = null;
                stopRecording();
            }
            x = x + ViewAudioProxy.getX(SendRecordButton);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideTextContainer
                    .getLayoutParams();
            if (startedDraggingX != -1) {
                float dist = (x - startedDraggingX);
                params.leftMargin = convertToDp(30) + (int) dist;
                slideTextContainer.setLayoutParams(params);
                float alpha = 1.0f + dist / distCanMove;
                if (alpha > 1) {
                    alpha = 1;
                } else if (alpha < 0) {
                    alpha = 0;
                }
                ViewAudioProxy.setAlpha(slideTextContainer, alpha);
            }
            if (x <= ViewAudioProxy.getX(slideTextContainer) + slideTextContainer.getWidth()
                    + convertToDp(30)) {
                if (startedDraggingX == -1) {
                    startedDraggingX = x;
                    distCanMove = (recordPanel.getMeasuredWidth()
                            - slideTextContainer.getMeasuredWidth() - convertToDp(48)) / 2.0f;
                    if (distCanMove <= 0) {
                        distCanMove = convertToDp(80);
                    } else if (distCanMove > convertToDp(80)) {
                        distCanMove = convertToDp(80);
                    }
                }
            }
            if (params.leftMargin > convertToDp(30)) {
                params.leftMargin = convertToDp(30);
                slideTextContainer.setLayoutParams(params);
                ViewAudioProxy.setAlpha(slideTextContainer, 1);
                startedDraggingX = -1;
            }
        }

        view.onTouchEvent(motionEvent);
        return true;
    }

    /**
     * method to start recording audio
     */
    private void startRecording() {

        if (PermissionHandler.checkPermission(this, Manifest.permission.RECORD_AUDIO)) {
            AppHelper.LogCat("Record audio permission already granted.");
        } else {
            AppHelper.LogCat("Please request Record audio permission.");
            PermissionHandler.requestPermission(this, Manifest.permission.RECORD_AUDIO);
        }

        try {
            startRecordingAudio();
            startTime = SystemClock.uptimeMillis();
            recordTimer = new Timer();
            UpdaterTimerTask updaterTimerTask = new UpdaterTimerTask();
            recordTimer.schedule(updaterTimerTask, 1000, 1000);
            vibrate();
        } catch (Exception e) {
            AppHelper.LogCat("IOException start audio " + e.getMessage());
        }


    }


    /**
     * method to stop recording auido
     */
    @SuppressLint("SetTextI18n")
    private void stopRecording() {
        if (recordTimer != null) {
            recordTimer.cancel();
        }
        if (recordTimeText.getText().toString().equals("00:00")) {
            return;
        }
        recordTimeText.setText("00:00");
        vibrate();
        recordPanel.setVisibility(View.GONE);
        sendMessagePanel.setVisibility(View.VISIBLE);
        stopRecordingAudio();


    }

    /**
     * method to initialize the audio for start recording
     *
     * @throws IOException
     */
    @SuppressLint("SetTextI18n")
    private void startRecordingAudio() throws IOException {
        stopRecordingAudio();
        FileAudioPath = FilesManager.getFileRecordPath(this);
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile(FileAudioPath);
        mMediaRecorder.setOnErrorListener(errorListener);
        mMediaRecorder.setOnInfoListener(infoListener);
        mMediaRecorder.prepare();
        mMediaRecorder.start();

    }

    /**
     * method to reset and clear media recorder
     */
    private void stopRecordingAudio() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
                FileAudioPath = null;
            }
        } catch (Exception e) {
            AppHelper.LogCat("Exception stop recording " + e.getMessage());
        }

    }

    private MediaRecorder.OnErrorListener errorListener = (mr, what, extra) -> AppHelper.LogCat("Error: " + what + ", " + extra);

    private MediaRecorder.OnInfoListener infoListener = (mr, what, extra) -> AppHelper.LogCat("Warning: " + what + ", " + extra);

    /**
     * method to make device vibrate when user start recording
     */
    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private int convertToDp(float value) {
        return (int) Math.ceil(1 * value);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    /**
     * method to toggle the selection
     *
     * @param position this is parameter for  ToggleSelection method
     */
    private void ToggleSelection(int position) {
        mMessagesAdapter.toggleSelection(position);
        String title = String.format("%s selected", mMessagesAdapter.getSelectedItemCount());
        actionMode.setTitle(title);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.select_messages_menu, menu);
        getSupportActionBar().hide();
        if (AppHelper.isAndroid5()) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(AppHelper.getColor(this, R.color.colorGrayDarkerBar));
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

        int arraySize = mMessagesAdapter.getSelectedItems().size();
        switch (menuItem.getItemId()) {
            case R.id.copy_message:
                int currentPosition;
                for (int x = 0; x < arraySize; x++) {
                    currentPosition = mMessagesAdapter.getSelectedItems().get(x);
                    MessagesModel messagesModel = mMessagesAdapter.getItem(currentPosition);
                    if (AppHelper.copyText(this, messagesModel)) {
                        AppHelper.CustomToast(MessagesPopupActivity.this, getString(R.string.message_is_copied));
                        if (actionMode != null) {
                            mMessagesAdapter.clearSelections();
                            actionMode.finish();
                            getSupportActionBar().show();
                        }
                    }


                }

                break;
            case R.id.delete_message:
                Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.message_delete);

                builder.setPositiveButton(R.string.Yes, (dialog, whichButton) -> {
                    AppHelper.showDialog(this, getString(R.string.deleting_chat));
                    for (int x = 0; x < arraySize; x++) {
                        int currentPosition1 = mMessagesAdapter.getSelectedItems().get(x);
                        MessagesModel messagesModel = mMessagesAdapter.getItem(currentPosition1);
                        int messageId = messagesModel.getId();
                        EventBus.getDefault().post(new Pusher(EVENT_BUS_DELETE_CONVERSATION_ITEM, ConversationID));
                        realm.executeTransactionAsync(realm1 -> {
                            MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("id", messageId).equalTo("conversationID", ConversationID).findFirst();
                            messagesModel1.deleteFromRealm();
                        }, () -> {
                            AppHelper.LogCat("Message deleted successfully MessagesPopupActivity ");
                            mMessagesAdapter.removeMessageItem(currentPosition1);
                            scrollToBottom();
                            RealmResults<MessagesModel> messagesModel1 = realm.where(MessagesModel.class).equalTo("conversationID", ConversationID).findAll();
                            if (messagesModel1.size() == 0) {
                                realm.executeTransactionAsync(realm1 -> {
                                    ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                                    conversationsModel1.deleteFromRealm();
                                }, () -> {
                                    AppHelper.LogCat("Conversation deleted successfully MessagesPopupActivity ");

                                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));
                                    NotificationsManager.SetupBadger(this);
                                }, error -> {
                                    AppHelper.LogCat("delete conversation failed MessagesPopupActivity " + error.getMessage());

                                });
                            } else {
                                MessagesModel lastMessage = realm.where(MessagesModel.class).equalTo("conversationID", ConversationID).findAll().last();
                                realm.executeTransactionAsync(realm1 -> {
                                    ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                                    conversationsModel1.setLastMessage(lastMessage.getMessage());
                                    conversationsModel1.setLastMessageId(lastMessage.getId());
                                    realm1.copyToRealmOrUpdate(conversationsModel1);
                                }, () -> {
                                    AppHelper.LogCat("Conversation deleted successfully  MessagesPopupActivity ");

                                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));
                                    NotificationsManager.SetupBadger(this);
                                }, error -> {
                                    AppHelper.LogCat("delete conversation failed  MessagesPopupActivity" + error.getMessage());

                                });
                            }
                        }, error -> {
                            AppHelper.LogCat("delete message failed  MessagesPopupActivity" + error.getMessage());

                        });

                    }
                    AppHelper.hideDialog();

                    if (actionMode != null) {
                        mMessagesAdapter.clearSelections();
                        actionMode.finish();
                        getSupportActionBar().show();
                    }

                });

                builder.setNegativeButton(R.string.No, (dialog, whichButton) -> {

                });

                builder.show();
                realm.close();
                break;
            case R.id.transfer_message:
                if (arraySize != 0) {
                    ArrayList<String> messagesModelList = new ArrayList<>();
                    for (int x = 0; x < arraySize; x++) {
                        currentPosition = mMessagesAdapter.getSelectedItems().get(x);
                        MessagesModel messagesModel = mMessagesAdapter.getItem(currentPosition);
                        String message = UtilsString.unescapeJava(messagesModel.getMessage());
                        messagesModelList.add(message);
                    }
                    if (messagesModelList.size() != 0) {
                        Intent intent = new Intent(this, TransferMessageContactsActivity.class);
                        intent.putExtra("messageCopied", messagesModelList);
                        startActivity(intent);
                        finish();
                    } else {
                        AppHelper.CustomToast(MessagesPopupActivity.this, getString(R.string.this_message_empty));
                    }
                }
                break;
            default:
                return false;
        }


        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.actionMode = null;
        mMessagesAdapter.clearSelections();
        getSupportActionBar().show();
        if (AppHelper.isAndroid5()) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(AppHelper.getColor(this, R.color.colorPrimaryDark));
        }
    }

    @Override
    public void onClick(View view) {

        int position = messagesList.getChildAdapterPosition(view);
        if (actionMode != null) {
            ToggleSelection(position);
            return;
        }
    }


    private class RecyclerViewBenOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            try {
                View view = messagesList.findChildViewUnder(e.getX(), e.getY());

                int currentPosition = messagesList.getChildAdapterPosition(view);
                MessagesModel messagesModel = mMessagesAdapter.getItem(currentPosition);
                if (!messagesModel.isGroup()) {

                    if (actionMode != null) {
                        return;
                    }
                    actionMode = startActionMode(MessagesPopupActivity.this);
                    ToggleSelection(currentPosition);
                }
                super.onLongPress(e);
            } catch (Exception e1) {
                AppHelper.LogCat(" onLongPress " + e1.getMessage());
            }
        }

    }


    private class UpdaterTimerTask extends TimerTask {

        @Override
        public void run() {
            long timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            long timeSwapBuff = 0L;
            long updatedTime = timeSwapBuff + timeInMilliseconds;
            Duration = String.valueOf(updatedTime);
            final String recordTime = UtilsTime.getFileTime(updatedTime);
            runOnUiThread(() -> {
                try {
                    if (recordTimeText != null) {
                        recordTimeText.setText(recordTime);
                    }

                } catch (Exception e) {
                    AppHelper.LogCat("Exception record MessagesPopupActivity");
                }

            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        WhatsCloneApplication.getInstance().setConnectivityListener(this);
        connectToChatServer();
    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnecting, boolean isConnected) {
        if (!isConnecting && !isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_not_available), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
        } else if (isConnecting && isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_available), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);

            if (isGroup)
                new Handler().postDelayed(() -> unSentMessagesGroup(groupID), 1000);
            else
                new Handler().postDelayed(() -> unSentMessagesForARecipient(recipientId), 1000);
        } else {
            AppHelper.Snackbar(this, mView, getString(R.string.waiting_for_network), AppConstants.MESSAGE_COLOR_WARNING, AppConstants.TEXT_COLOR);

        }
    }


}
