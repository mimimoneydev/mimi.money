package com.money.mimi.activities.profile;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.activities.groups.AddNewMembersToGroupActivity;
import com.money.mimi.activities.groups.EditGroupActivity;
import com.money.mimi.activities.media.MediaActivity;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.adapters.recyclerView.groups.GroupMembersAdapter;
import com.money.mimi.adapters.recyclerView.media.MediaProfileAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIGroups;
import com.money.mimi.api.APIHelper;
import com.money.mimi.api.APIService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.fragments.bottomSheets.BottomSheetEditGroupImage;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.UtilsTime;
import com.money.mimi.helpers.call.CallManager;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.models.groups.GroupResponse;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.models.groups.MembersGroupModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.models.users.status.StatusResponse;
import com.money.mimi.presenters.groups.EditGroupPresenter;
import com.money.mimi.presenters.users.ProfilePresenter;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.Realm;
import io.socket.client.Socket;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Abderrahim El imame on 27/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class ProfileActivity extends AppCompatActivity implements NetworkListener {

    @BindView(R.id.cover)
    ImageView UserCover;
    @BindView(R.id.anim_toolbar)
    Toolbar toolbar;/*
    @BindView(R.id.appbar)
    AppBarLayout AppBarLayout;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbar;*/
    @BindView(R.id.containerProfile)
    LinearLayout containerProfile;
    @BindView(R.id.created_title)
    EmojiconTextView mCreatedTitle;
    @BindView(R.id.group_container_title)
    LinearLayout GroupTitleContainer;
    @BindView(R.id.group_edit)
    FloatingActionButton EditGroupBtn;
    @BindView(R.id.statusPhoneContainer)
    CardView statusPhoneContainer;
    @BindView(R.id.status)
    TextView status;
    @BindView(R.id.numberPhone)
    TextView numberPhone;
    @BindView(R.id.status_date)
    TextView status_date;
    @BindView(R.id.send_message)
    ImageView sendMessageBtn;
    @BindView(R.id.call_voice)
    ImageView callVideoBtn;
    @BindView(R.id.call_video)
    ImageView callVoiceBtn;
    @BindView(R.id.MembersList)
    RecyclerView MembersList;
    @BindView(R.id.participantContainer)
    CardView participantContainer;
    @BindView(R.id.participantContainerExit)
    LinearLayout participantContainerExit;
    @BindView(R.id.participantContainerDelete)
    LinearLayout participantContainerDelete;
    @BindView(R.id.participantCounter)
    TextView participantCounter;
    @BindView(R.id.media_counter)
    TextView mediaCounter;
    @BindView(R.id.media_section)
    CardView mediaSection;

    @BindView(R.id.mediaProfileList)
    RecyclerView mediaList;
    @BindView(R.id.shareBtn)
    FloatingActionButton shareBtn;


    private MediaProfileAdapter mMediaProfileAdapter;
    private GroupMembersAdapter mGroupMembersAdapter;
    private ContactsModel mContactsModel;
    private GroupsModel mGroupsModel;
    public int userID;
    public int groupID;
    private boolean isGroup;
    private int mutedColor;
    private int mutedColorStatusBar;
    private final CompositeDisposable disposables = new CompositeDisposable();

    int numberOfColors = 24;
    private ProfilePresenter mProfilePresenter;
    private boolean isAnAdmin;
    private APIService mApiService;
    private String PicturePath;
    private MemoryCache memoryCache;
    private Intent mIntent;
    private Socket mSocket;
    private String name = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        initializerView();
        connectToChatServer();
        setTypeFaces();
        memoryCache = new MemoryCache();
        if (getIntent().hasExtra("userID")) {
            isGroup = getIntent().getExtras().getBoolean("isGroup");
            userID = getIntent().getExtras().getInt("userID");
        }


        if (getIntent().hasExtra("groupID")) {
            isGroup = getIntent().getExtras().getBoolean("isGroup");
            groupID = getIntent().getExtras().getInt("groupID");
        }
        mApiService = new APIService(this);
        mProfilePresenter = new ProfilePresenter(this);
        mProfilePresenter.onCreate();


        participantContainerExit.setOnClickListener(v -> {

            String name = UtilsString.unescapeJava(mGroupsModel.getGroupName());
            if (name.length() > 10) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.exit_group) + name.substring(0, 10) + "... " + "" + getString(R.string.group_ex))
                        .setPositiveButton(getString(R.string.exit), (dialog, which) -> {
                            AppHelper.showDialog(this, getString(R.string.exiting_group_dialog));
                            mProfilePresenter.ExitGroup();
                        }).setNegativeButton(getString(R.string.cancel), null).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.exit_group) + name + "" + getString(R.string.group_ex))
                        .setPositiveButton(getString(R.string.exit), (dialog, which) -> {
                            AppHelper.showDialog(this, getString(R.string.exiting_group_dialog));
                            mProfilePresenter.ExitGroup();
                        }).setNegativeButton(getString(R.string.cancel), null).show();
            }


        });

        participantContainerDelete.setOnClickListener(v -> {
            String name = UtilsString.unescapeJava(mGroupsModel.getGroupName());
            if (name.length() > 10) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.delete) + name.substring(0, 10) + "... " + "" + getString(R.string.group_ex))
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                            AppHelper.showDialog(this, getString(R.string.deleting_group_dialog));
                            mProfilePresenter.DeleteGroup();
                        }).setNegativeButton(getString(R.string.cancel), null).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.delete) + name + "" + getString(R.string.group_ex))
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                            AppHelper.showDialog(this, getString(R.string.deleting_group_dialog));
                            mProfilePresenter.DeleteGroup();
                        }).setNegativeButton(getString(R.string.cancel), null).show();
            }
        });
        callVideoBtn.setOnClickListener(view -> makeCall(true));
        callVideoBtn.setOnClickListener(view -> makeCall(true));

        EditGroupBtn.setOnClickListener(view -> launchEditGroupName());
        sendMessageBtn.setOnClickListener(view -> sendMessage(mContactsModel));
        shareBtn.setOnClickListener(view -> shareContact(mContactsModel));

    }

    /**
     * method to connect to the chat sever by socket
     */
    private void connectToChatServer() {

        WhatsCloneApplication app = (WhatsCloneApplication) getApplication();
        mSocket = app.getSocket();
        if (mSocket == null || !mSocket.connected()) {
            AppHelper.startMainService(this);
        }


    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            mCreatedTitle.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            status.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            numberPhone.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            status_date.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            participantCounter.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            mediaCounter.setTypeface(AppHelper.setTypeFace(this, "Futura"));

        }
    }

    private void makeCall(boolean isVideoCall) {
        if (!isVideoCall) {
            CallManager.callContact(ProfileActivity.this, true, false, userID);
        } else {
            CallManager.callContact(ProfileActivity.this, true, true, userID);
        }
    }


    /**
     * method to initialize group members view
     */
    private void initializerGroupMembersView(boolean admin) {

        participantContainer.setVisibility(View.VISIBLE);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mGroupMembersAdapter = new GroupMembersAdapter(this, admin);
        MembersList.setLayoutManager(mLinearLayoutManager);
        MembersList.setAdapter(mGroupMembersAdapter);
        //checkIfIsAnAdmin();

    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mediaList.setLayoutManager(linearLayoutManager);
        mMediaProfileAdapter = new MediaProfileAdapter(this);
        mediaList.setAdapter(mMediaProfileAdapter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isGroup) {
            if (isAnAdmin) {
                getMenuInflater().inflate(R.menu.profile_menu_group_add, menu);
            } /*else {
                if (!left)
                    getMenuInflater().inflate(R.menu.profile_menu_group, menu);
            }*/

        } else {
            if (mContactsModel != null)
                if (UtilsPhone.checkIfContactExist(this, mContactsModel.getWalletAddress())) {
                    if (userID == PreferenceManager.getID(this)) {
                        getMenuInflater().inflate(R.menu.profile_menu_mine, menu);
                    } else {
                        getMenuInflater().inflate(R.menu.profile_menu, menu);
                    }
                } else if (userID == PreferenceManager.getID(this)) {
                    getMenuInflater().inflate(R.menu.profile_menu_mine, menu);
                } else {
                    getMenuInflater().inflate(R.menu.profile_menu_user_not_exist, menu);
                }

        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        } else if (item.getItemId() == R.id.add_contact) {
            Intent mIntent = new Intent(this, AddNewMembersToGroupActivity.class);
            mIntent.putExtra("groupID", groupID);
            mIntent.putExtra("profileAdd", "add");
            startActivity(mIntent);
        } else if (item.getItemId() == R.id.edit_contact) {
            editContact(mContactsModel);
        } else if (item.getItemId() == R.id.view_contact) {
            viewContact(mContactsModel);
        } else if (item.getItemId() == R.id.add_new_contact) {
            addNewContact();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.media_selection)
    public void launchMediaActivity() {

        if (isGroup) {
            mIntent = new Intent(this, MediaActivity.class);
            mIntent.putExtra("groupID", groupID);
            mIntent.putExtra("isGroup", true);
            mIntent.putExtra("Username", mGroupsModel.getGroupName());
            startActivity(mIntent);
            AnimationsUtil.setSlideInAnimation(this);

        } else {
            String finalName = resolveDisplayName(mContactsModel);
            mIntent = new Intent(this, MediaActivity.class);
            mIntent.putExtra("userID", userID);
            mIntent.putExtra("isGroup", false);
            mIntent.putExtra("Username", finalName);
            startActivity(mIntent);
            AnimationsUtil.setSlideInAnimation(this);

        }
    }

    private void addNewContact() {
        try {
            Intent mIntent = new Intent(Intent.ACTION_INSERT);
            mIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            mIntent.putExtra(ContactsContract.Intents.Insert.PHONE, mContactsModel.getWalletAddress());
            startActivityForResult(mIntent, AppConstants.SELECT_ADD_NEW_CONTACT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void launchEditGroupName() {
        Intent mIntent = new Intent(this, EditGroupActivity.class);
        mIntent.putExtra("currentGroupName", mGroupsModel.getGroupName());
        mIntent.putExtra("groupID", mGroupsModel.getId());
        startActivity(mIntent);
    }

    public void ShowContact(ContactsModel contactsModel) {
        mContactsModel = contactsModel;
        try {
            updateUI(null, mContactsModel);
            invalidateOptionsMenu();
        } catch (Exception e) {
            AppHelper.LogCat("Error ContactsModel in profile UI Exception " + e.getMessage());
        }
    }

    public void ShowMedia(List<MessagesModel> messagesModel) {
        if (messagesModel.size() != 0) {
            mediaSection.setVisibility(View.VISIBLE);
            mediaCounter.setText(String.valueOf(messagesModel.size()));
            mMediaProfileAdapter.setMessages(messagesModel);

        } else {
            mediaSection.setVisibility(View.GONE);
        }

    }

    public void ShowGroup(GroupsModel groupsModel) {
        mGroupsModel = groupsModel;
        try {
            updateUI(mGroupsModel, null);
        } catch (Exception e) {
            AppHelper.LogCat("Error GroupsModel in profile UI Exception " + e.getMessage());
        }
    }

    @SuppressLint("StaticFieldLeak")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateUI(GroupsModel mGroupsModel, ContactsModel mContactsModel) {


        if (isGroup) {
            GroupTitleContainer.setVisibility(View.VISIBLE);
            statusPhoneContainer.setVisibility(View.GONE);
            shareBtn.setVisibility(View.GONE);
            if (mGroupsModel.isAdmin()) {
                EditGroupBtn.setVisibility(View.VISIBLE);
                if (mGroupsModel.isLeft()) {
                    participantContainerExit.setVisibility(View.GONE);
                    participantContainerDelete.setVisibility(View.VISIBLE);
                } else {
                    participantContainerExit.setVisibility(View.VISIBLE);
                    participantContainerDelete.setVisibility(View.GONE);
                }
            } else {
                if (mGroupsModel.isLeft()) {
                    participantContainerExit.setVisibility(View.GONE);
                    participantContainerDelete.setVisibility(View.VISIBLE);
                    EditGroupBtn.setVisibility(View.GONE);
                } else {
                    participantContainerExit.setVisibility(View.VISIBLE);
                    participantContainerDelete.setVisibility(View.GONE);
                    EditGroupBtn.setVisibility(View.VISIBLE);
                }
            }


            DateTime messageDate = UtilsTime.getCorrectDate(mGroupsModel.getCreatedDate());
            String groupDate = UtilsTime.convertDateToString(this, messageDate);
            if (mGroupsModel.getCreatorID() == PreferenceManager.getID(this)) {
                mCreatedTitle.setText(String.format(getString(R.string.created_by_you_at) + " %s", groupDate));
            } else {
                String name = UtilsPhone.getContactName(mGroupsModel.getCreator());
                if (name != null) {
                    mCreatedTitle.setText(String.format(getString(R.string.created_by) + " %s " + getString(R.string.group_at) + " %s ", name, groupDate));
                } else {
                    mCreatedTitle.setText(String.format(getString(R.string.created_by) + " %s " + getString(R.string.group_at) + " %s ", mGroupsModel.getCreator(), groupDate));
                }
            }
            String name = UtilsString.unescapeJava(mGroupsModel.getGroupName());
            if (name.length() > 10)
                getSupportActionBar().setTitle(name.substring(0, 10) + "... " + "");
            else
                getSupportActionBar().setTitle(name);


            String ImageUrl = mGroupsModel.getGroupImage();
            int groupId = mGroupsModel.getId();
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, ProfileActivity.this, groupId, AppConstants.GROUP, AppConstants.FULL_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        UserCover.setImageBitmap(bitmap);
                        Palette.from(bitmap).maximumColorCount(numberOfColors).generate(palette -> {
                            Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                            if (swatchColorDark != null) {
                                try {
                                    mutedColor = swatchColorDark.getRgb();
                                    toolbar.setBackgroundColor(mutedColor);
                                    if (AppHelper.isAndroid5()) {

                                        float hsv[] = new float[3];
                                        Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                        hsv[2] = 0.2f;
                                        mutedColorStatusBar = Color.HSVToColor(hsv);
                                        getWindow().setStatusBarColor(mutedColorStatusBar);
                                    }
                                } catch (Exception e) {
                                    AppHelper.LogCat(" " + e.getMessage());
                                }
                            } else {
                                List<Palette.Swatch> swatches = palette.getSwatches();
                                for (Palette.Swatch swatch : swatches) {
                                    if (swatch != null) {
                                        mutedColor = swatch.getRgb();
                                        toolbar.setBackgroundColor(mutedColor);
                                        if (AppHelper.isAndroid5()) {
                                            float hsv[] = new float[3];
                                            Color.colorToHSV(swatch.getRgb(), hsv);
                                            hsv[2] = 0.2f;
                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                        }
                                        break;
                                    }
                                }

                            }
                        });
                    } else {

                        new AsyncTask<Void, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(Void... params) {
                                return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, ProfileActivity.this, groupId, AppConstants.GROUP, AppConstants.ROW_PROFILE);
                            }

                            @Override
                            protected void onPostExecute(Bitmap holderBitmap) {
                                super.onPostExecute(holderBitmap);
                                Drawable drawable;
                                if (holderBitmap != null)
                                    drawable = new BitmapDrawable(getResources(), holderBitmap);
                                else
                                    drawable = textDrawable(name);

                                BitmapImageViewTarget target = new BitmapImageViewTarget(UserCover) {
                                    @Override
                                    public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                        super.onResourceReady(bitmap, anim);
                                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_IMAGE_GROUP_UPDATED, groupID));
                                        UserCover.setImageBitmap(bitmap);
                                        Palette.from(bitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                            Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                            if (swatchColorDark != null) {
                                                try {
                                                    mutedColor = swatchColorDark.getRgb();
                                                    toolbar.setBackgroundColor(mutedColor);
                                                    if (AppHelper.isAndroid5()) {

                                                        float hsv[] = new float[3];
                                                        Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                                        hsv[2] = 0.2f;
                                                        mutedColorStatusBar = Color.HSVToColor(hsv);
                                                        getWindow().setStatusBarColor(mutedColorStatusBar);
                                                    }
                                                } catch (Exception e) {
                                                    AppHelper.LogCat(" " + e.getMessage());
                                                }
                                            } else {
                                                List<Palette.Swatch> swatches = palette.getSwatches();
                                                for (Palette.Swatch swatch : swatches) {
                                                    if (swatch != null) {
                                                        mutedColor = swatch.getRgb();
                                                        toolbar.setBackgroundColor(mutedColor);
                                                        if (AppHelper.isAndroid5()) {
                                                            float hsv[] = new float[3];
                                                            Color.colorToHSV(swatch.getRgb(), hsv);
                                                            hsv[2] = 0.2f;
                                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                                        }
                                                        break;
                                                    }
                                                }

                                            }
                                        });
                                        ImageLoader.DownloadImage(memoryCache, EndPoints.PROFILE_IMAGE_URL + ImageUrl, ImageUrl, ProfileActivity.this, groupId, AppConstants.GROUP, AppConstants.FULL_PROFILE);

                                    }

                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                        if (holderBitmap != null) {
                                            UserCover.setImageBitmap(holderBitmap);
                                            Palette.from(holderBitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                                if (swatchColorDark != null) {
                                                    try {
                                                        mutedColor = swatchColorDark.getRgb();
                                                        toolbar.setBackgroundColor(mutedColor);
                                                        if (AppHelper.isAndroid5()) {

                                                            float hsv[] = new float[3];
                                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                                            hsv[2] = 0.2f;
                                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                                        }
                                                    } catch (Exception ex) {
                                                        AppHelper.LogCat(" " + e.getMessage());
                                                    }
                                                } else {
                                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                                    for (Palette.Swatch swatch : swatches) {
                                                        if (swatch != null) {
                                                            mutedColor = swatch.getRgb();
                                                            toolbar.setBackgroundColor(mutedColor);
                                                            if (AppHelper.isAndroid5()) {
                                                                float hsv[] = new float[3];
                                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                                hsv[2] = 0.2f;
                                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                                            }
                                                            break;
                                                        }
                                                    }

                                                }
                                            });
                                        } else {
                                            UserCover.setImageDrawable(drawable);
                                        }
                                    }

                                    @Override
                                    public void onLoadStarted(Drawable placeholder) {
                                        super.onLoadStarted(placeholder);
                                        if (holderBitmap != null) {
                                            UserCover.setImageBitmap(holderBitmap);
                                            Palette.from(holderBitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                                if (swatchColorDark != null) {
                                                    try {
                                                        mutedColor = swatchColorDark.getRgb();
                                                        toolbar.setBackgroundColor(mutedColor);
                                                        if (AppHelper.isAndroid5()) {

                                                            float hsv[] = new float[3];
                                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                                            hsv[2] = 0.2f;
                                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                                        }
                                                    } catch (Exception e) {
                                                        AppHelper.LogCat(" " + e.getMessage());
                                                    }
                                                } else {
                                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                                    for (Palette.Swatch swatch : swatches) {
                                                        if (swatch != null) {
                                                            mutedColor = swatch.getRgb();
                                                            toolbar.setBackgroundColor(mutedColor);
                                                            if (AppHelper.isAndroid5()) {
                                                                float hsv[] = new float[3];
                                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                                hsv[2] = 0.2f;
                                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                                            }
                                                            break;
                                                        }
                                                    }

                                                }
                                            });
                                        } else {
                                            UserCover.setImageDrawable(drawable);
                                        }
                                    }
                                };
                                Glide.with(ProfileActivity.this)
                                        .load(EndPoints.PROFILE_IMAGE_URL + ImageUrl)
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(drawable)
                                        .error(drawable)
                                        .override(AppConstants.PROFILE_IMAGE_SIZE, AppConstants.PROFILE_IMAGE_SIZE)
                                        .into(target);

                            }
                        }.execute();


                    }
                }
            }.execute();


            UserCover.setOnClickListener(view -> {
                BottomSheetEditGroupImage bottomSheetEditGroupImage = new BottomSheetEditGroupImage();
                bottomSheetEditGroupImage.show(getSupportFragmentManager(), bottomSheetEditGroupImage.getTag());
            });
            isAnAdmin = mGroupsModel.isAdmin();

            disposables.add(APIHelper.initializeApiGroups().updateGroupMembers(mGroupsModel.getId()).subscribe(this::ShowGroupMembers, this::onErrorLoading));


        } else {
            EditGroupBtn.setVisibility(View.GONE);
            if (userID == PreferenceManager.getID(this)) {
                sendMessageBtn.setVisibility(View.GONE);
                callVideoBtn.setVisibility(View.GONE);
                callVoiceBtn.setVisibility(View.GONE);
                shareBtn.setVisibility(View.GONE);
            } else {
                sendMessageBtn.setVisibility(View.VISIBLE);
                callVideoBtn.setVisibility(View.VISIBLE);
                callVoiceBtn.setVisibility(View.VISIBLE);
                shareBtn.setVisibility(View.VISIBLE);
            }

            name = resolveDisplayName(mContactsModel);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(name);
            }

            GroupTitleContainer.setVisibility(View.GONE);
            statusPhoneContainer.setVisibility(View.VISIBLE);
            String Status;
            if (mContactsModel.getStatus() != null) {
                Status = UtilsString.unescapeJava(mContactsModel.getStatus());
            } else {
                Status = getString(R.string.no_status);
            }

            status.setText(Status);
            numberPhone.setText(mContactsModel.getWalletAddress());
            status_date.setText(mContactsModel.getStatus_date());

            String userImageUrl = mContactsModel.getImage();
            int userId = mContactsModel.getId();


            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, userImageUrl, ProfileActivity.this, userId, AppConstants.USER, AppConstants.FULL_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        // AnimationsUtil.expandToolbar(containerProfile, bitmap, AppBarLayout);
                        UserCover.setImageBitmap(bitmap);
                        Palette.from(bitmap).maximumColorCount(numberOfColors).generate(palette -> {
                            Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                            if (swatchColorDark != null) {
                                try {
                                    mutedColor = swatchColorDark.getRgb();
                                    toolbar.setBackgroundColor(mutedColor);
                                    if (AppHelper.isAndroid5()) {

                                        float hsv[] = new float[3];
                                        Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                        hsv[2] = 0.2f;
                                        mutedColorStatusBar = Color.HSVToColor(hsv);
                                        getWindow().setStatusBarColor(mutedColorStatusBar);
                                    }
                                } catch (Exception e) {
                                    AppHelper.LogCat(" " + e.getMessage());
                                }
                            } else {
                                List<Palette.Swatch> swatches = palette.getSwatches();
                                for (Palette.Swatch swatch : swatches) {
                                    if (swatch != null) {
                                        mutedColor = swatch.getRgb();
                                        toolbar.setBackgroundColor(mutedColor);
                                        if (AppHelper.isAndroid5()) {
                                            float hsv[] = new float[3];
                                            Color.colorToHSV(swatch.getRgb(), hsv);
                                            hsv[2] = 0.2f;
                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                        }
                                        break;
                                    }
                                }

                            }
                        });
                    } else {

                        new AsyncTask<Void, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(Void... params) {
                                return ImageLoader.GetCachedBitmapImage(memoryCache, userImageUrl, ProfileActivity.this, userId, AppConstants.USER, AppConstants.ROW_PROFILE);
                            }

                            @Override
                            protected void onPostExecute(Bitmap holderBitmap) {
                                super.onPostExecute(holderBitmap);
                                Drawable drawable;
                                if (holderBitmap != null)
                                    drawable = new BitmapDrawable(getResources(), holderBitmap);
                                else
                                    drawable = textDrawable(name);

                                BitmapImageViewTarget target = new BitmapImageViewTarget(UserCover) {
                                    @Override
                                    public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                        super.onResourceReady(bitmap, anim);
                                        UserCover.setImageBitmap(bitmap);
                                        Palette.from(bitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                            Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                            if (swatchColorDark != null) {
                                                try {
                                                    mutedColor = swatchColorDark.getRgb();
                                                    toolbar.setBackgroundColor(mutedColor);
                                                    if (AppHelper.isAndroid5()) {

                                                        float hsv[] = new float[3];
                                                        Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                                        hsv[2] = 0.2f;
                                                        mutedColorStatusBar = Color.HSVToColor(hsv);
                                                        getWindow().setStatusBarColor(mutedColorStatusBar);
                                                    }
                                                } catch (Exception e) {
                                                    AppHelper.LogCat(" " + e.getMessage());
                                                }
                                            } else {
                                                List<Palette.Swatch> swatches = palette.getSwatches();
                                                for (Palette.Swatch swatch : swatches) {
                                                    if (swatch != null) {
                                                        mutedColor = swatch.getRgb();
                                                        toolbar.setBackgroundColor(mutedColor);
                                                        if (AppHelper.isAndroid5()) {
                                                            float hsv[] = new float[3];
                                                            Color.colorToHSV(swatch.getRgb(), hsv);
                                                            hsv[2] = 0.2f;
                                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                                        }
                                                        break;
                                                    }
                                                }

                                            }
                                        });
                                        ImageLoader.DownloadImage(memoryCache, EndPoints.PROFILE_IMAGE_URL + userImageUrl, userImageUrl, ProfileActivity.this, userId, AppConstants.USER, AppConstants.FULL_PROFILE);


                                    }

                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                        if (holderBitmap != null) {
                                            // AnimationsUtil.expandToolbar(containerProfile, holderBitmap, AppBarLayout);
                                            UserCover.setImageBitmap(holderBitmap);
                                            Palette.from(holderBitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                                if (swatchColorDark != null) {
                                                    try {
                                                        mutedColor = swatchColorDark.getRgb();
                                                        toolbar.setBackgroundColor(mutedColor);
                                                        if (AppHelper.isAndroid5()) {

                                                            float hsv[] = new float[3];
                                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                                            hsv[2] = 0.2f;
                                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                                        }
                                                    } catch (Exception ex) {
                                                        AppHelper.LogCat(" " + e.getMessage());
                                                    }
                                                } else {
                                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                                    for (Palette.Swatch swatch : swatches) {
                                                        if (swatch != null) {
                                                            mutedColor = swatch.getRgb();
                                                            toolbar.setBackgroundColor(mutedColor);
                                                            if (AppHelper.isAndroid5()) {
                                                                float hsv[] = new float[3];
                                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                                hsv[2] = 0.2f;
                                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                                            }
                                                            break;
                                                        }
                                                    }

                                                }
                                            });
                                        } else {
                                            UserCover.setImageDrawable(drawable);
                                        }
                                    }

                                    @Override
                                    public void onLoadStarted(Drawable placeholder) {
                                        super.onLoadStarted(placeholder);
                                        if (holderBitmap != null) {
                                            UserCover.setImageBitmap(holderBitmap);
                                            Palette.from(holderBitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                                if (swatchColorDark != null) {
                                                    try {
                                                        mutedColor = swatchColorDark.getRgb();
                                                        toolbar.setBackgroundColor(mutedColor);
                                                        if (AppHelper.isAndroid5()) {

                                                            float hsv[] = new float[3];
                                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                                            hsv[2] = 0.2f;
                                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                                        }
                                                    } catch (Exception e) {
                                                        AppHelper.LogCat(" " + e.getMessage());
                                                    }
                                                } else {
                                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                                    for (Palette.Swatch swatch : swatches) {
                                                        if (swatch != null) {
                                                            mutedColor = swatch.getRgb();
                                                            toolbar.setBackgroundColor(mutedColor);
                                                            if (AppHelper.isAndroid5()) {
                                                                float hsv[] = new float[3];
                                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                                hsv[2] = 0.2f;
                                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                                            }
                                                            break;
                                                        }
                                                    }

                                                }
                                            });
                                        } else {
                                            UserCover.setImageDrawable(drawable);
                                        }
                                    }
                                };


                                Glide.with(ProfileActivity.this)
                                        .load(EndPoints.PROFILE_IMAGE_URL + userImageUrl)
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(drawable)
                                        .error(drawable)
                                        .override(AppConstants.PROFILE_IMAGE_SIZE, AppConstants.PROFILE_IMAGE_SIZE)
                                        .into(target);

                            }
                        }.execute();


                    }
                }
            }.execute();

            if (userImageUrl != null) {
                if (FilesManager.isFilePhotoProfileExists(this, FilesManager.getProfileImage(userImageUrl))) {
                    UserCover.setOnClickListener(view -> AppHelper.LaunchImagePreviewActivity(this, AppConstants.PROFILE_IMAGE, userImageUrl));
                } else {
                    UserCover.setOnClickListener(view -> AppHelper.LaunchImagePreviewActivity(ProfileActivity.this, AppConstants.PROFILE_IMAGE_FROM_SERVER, userImageUrl));
                }
            }


        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        mProfilePresenter.onDestroy();
    }


    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("Profile throwable " + throwable.getMessage());
    }

    public void onErrorDeleting() {
        AppHelper.Snackbar(this, containerProfile, getString(R.string.failed_to_delete_this_group_check_connection), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);

    }

    public void onErrorExiting() {
        AppHelper.Snackbar(this, containerProfile, getString(R.string.failed_to_exit_this_group_check_connection), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);

    }

    /**
     * method to show group members list
     *
     * @param membersGroupModels this is parameter for ShowGroupMembers  method
     */
    public void ShowGroupMembers(List<MembersGroupModel> membersGroupModels) {

        if (membersGroupModels.size() != 0) {
            initializerGroupMembersView(isAnAdmin);
            mGroupMembersAdapter.setContacts(membersGroupModels);
            participantCounter.setText(String.valueOf(membersGroupModels.size()));
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String imagePath = null;
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;
            switch (requestCode) {
                case AppConstants.SELECT_ADD_NEW_CONTACT:
                    invalidateOptionsMenu();
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_CONTACT_ADDED));
                    break;
                case AppConstants.SELECT_PROFILE_PICTURE:
                    imageUri = data != null ? data.getData() : null;
                    break;
                case AppConstants.SELECT_PROFILE_CAMERA:
                    imageUri = data != null && data.getData() != null ? data.getData() : EditGroupPresenter.consumeLastCameraImageUri();
                    break;
            }

            if (imageUri != null) {
                imagePath = FilesManager.getPath(this, imageUri);
            }

            if (imagePath != null) {
                imagePath = FilesManager.persistFile(this, imagePath, FilesManager.FILE_TYPE_IMAGE);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_PATH_GROUP, imagePath));
            } else if (requestCode == AppConstants.SELECT_PROFILE_PICTURE || requestCode == AppConstants.SELECT_PROFILE_CAMERA) {
                AppHelper.LogCat("imagePath is null for group profile request " + requestCode);
            }
        }
    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {

        switch (pusher.getAction()) {
            case AppConstants.EVENT_BUS_DELETE_GROUP:
                AppHelper.Snackbar(this, containerProfile, pusher.getData(), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
                new Handler().postDelayed(this::finish, 500);
                break;
            case AppConstants.EVENT_BUS_PATH_GROUP:
                PicturePath = pusher.getData();
                try {
                    new UploadFileToServer().execute();
                } catch (Exception e) {
                    AppHelper.LogCat(e);
                    AppHelper.CustomToast(this, getString(R.string.oops_something));
                }
                break;
            case AppConstants.EVENT_BUS_ADD_MEMBER:
                new Handler().postDelayed(() -> mProfilePresenter.updateUIGroupData(pusher.getGroupID()), 500);
                break;
            case AppConstants.EVENT_BUS_EXIT_THIS_GROUP:
                participantContainerExit.setVisibility(View.GONE);
                participantContainerDelete.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> mProfilePresenter.updateUIGroupData(pusher.getGroupID()), 500);
                break;
            case AppConstants.EVENT_BUS_UPDATE_GROUP_NAME:
                new Handler().postDelayed(() -> mProfilePresenter.updateUIGroupData(pusher.getGroupID()), 500);
                break;
            case AppConstants.EVENT_BUS_USERNAME_PROFILE_UPDATED:
                if (!isGroup) {
                    int updatedUserID = pusher.getUserID();
                    if (updatedUserID == 0) {
                        updatedUserID = userID;
                    }
                    if (updatedUserID == userID) {
                        int finalUpdatedUserID = updatedUserID;
                        new Handler().postDelayed(() -> mProfilePresenter.updateUIContactData(finalUpdatedUserID), 500);
                    }
                }
                break;

        }


    }


    private void editContact(ContactsModel mContactsModel) {
        if (userID == PreferenceManager.getID(this)) {
            AppHelper.LaunchActivity(this, EditProfileActivity.class);
        } else {
            Intent intent = new Intent(this, EditUsernameActivity.class);
            intent.putExtra("currentUsername", mContactsModel.getUsername() != null ? mContactsModel.getUsername() : "");
            intent.putExtra("targetUserID", mContactsModel.getId());
            startActivity(intent);
        }
    }

    private void viewContact(ContactsModel mContactsModel) {
        long ContactID = UtilsPhone.getContactID(this, mContactsModel.getWalletAddress());
        try {
            if (ContactID != 0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, ContactID));
                startActivity(intent);
            }
        } catch (Exception e) {
            AppHelper.LogCat("error view contact " + e.getMessage());
        }
    }

    private void sendMessage(ContactsModel mContactsModel) {
        Intent messagingIntent = new Intent(this, MessagesActivity.class);
        messagingIntent.putExtra("conversationID", 0);
        messagingIntent.putExtra("recipientID", mContactsModel.getId());
        messagingIntent.putExtra("isGroup", false);
        startActivity(messagingIntent);
    }


    private void shareContact(ContactsModel mContactsModel) {
        if (mContactsModel == null) return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/*");
        String subject = null;
        if (mContactsModel.getUsername() != null) {
            subject = mContactsModel.getUsername();
        }
        if (mContactsModel.getWalletAddress() != null) {
            if (subject != null) {
                subject = subject + " " + mContactsModel.getWalletAddress();
            } else {
                subject = mContactsModel.getWalletAddress();
            }
        }
        if (subject != null) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, subject);
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.shareContact)));
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

    private String resolveDisplayName(ContactsModel contactsModel) {
        if (contactsModel == null) {
            return "";
        }
        String username = contactsModel.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        String contactName = UtilsPhone.getContactName(contactsModel.getWalletAddress());
        if (contactName != null && !contactName.trim().isEmpty()) {
            return contactName;
        }
        return contactsModel.getWalletAddress();
    }

    @SuppressLint("StaticFieldLeak")
    private void setImage(String ImageUrl) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("groupId", groupID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mSocket != null)
            mSocket.emit(AppConstants.SOCKET_IMAGE_GROUP_UPDATED, jsonObject);

        ImageLoader.DownloadImage(memoryCache, EndPoints.PROFILE_IMAGE_URL + ImageUrl, ImageUrl, ProfileActivity.this, groupID, AppConstants.GROUP, AppConstants.FULL_PROFILE);


        String groupImage = mGroupsModel.getGroupImage();
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return ImageLoader.GetCachedBitmapImage(memoryCache, groupImage, ProfileActivity.this, groupID, AppConstants.GROUP, AppConstants.FULL_PROFILE);
            }

            @Override
            protected void onPostExecute(Bitmap holderBitmap) {
                super.onPostExecute(holderBitmap);
                if (holderBitmap != null) {
                    Drawable drawable;
                    drawable = new BitmapDrawable(getResources(), holderBitmap);
                    BitmapImageViewTarget target = new BitmapImageViewTarget(UserCover) {
                        @Override
                        public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            // AnimationsUtil.expandToolbar(containerProfile, holderBitmap, AppBarLayout);
                            UserCover.setImageBitmap(bitmap);
                            Palette.from(bitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                if (swatchColorDark != null) {
                                    try {
                                        mutedColor = swatchColorDark.getRgb();
                                        toolbar.setBackgroundColor(mutedColor);
                                        if (AppHelper.isAndroid5()) {

                                            float hsv[] = new float[3];
                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                            hsv[2] = 0.2f;
                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                        }
                                    } catch (Exception e) {
                                        AppHelper.LogCat(" " + e.getMessage());
                                    }
                                } else {
                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                    for (Palette.Swatch swatch : swatches) {
                                        if (swatch != null) {
                                            mutedColor = swatch.getRgb();
                                            toolbar.setBackgroundColor(mutedColor);
                                            if (AppHelper.isAndroid5()) {
                                                float hsv[] = new float[3];
                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                hsv[2] = 0.2f;
                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                            }
                                            break;
                                        }
                                    }

                                }
                            });
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            UserCover.setImageBitmap(holderBitmap);
                            Palette.from(holderBitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                if (swatchColorDark != null) {
                                    try {
                                        mutedColor = swatchColorDark.getRgb();
                                        toolbar.setBackgroundColor(mutedColor);
                                        if (AppHelper.isAndroid5()) {

                                            float hsv[] = new float[3];
                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                            hsv[2] = 0.2f;
                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                        }
                                    } catch (Exception ex) {
                                        AppHelper.LogCat(" " + e.getMessage());
                                    }
                                } else {
                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                    for (Palette.Swatch swatch : swatches) {
                                        if (swatch != null) {
                                            mutedColor = swatch.getRgb();
                                            toolbar.setBackgroundColor(mutedColor);
                                            if (AppHelper.isAndroid5()) {
                                                float hsv[] = new float[3];
                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                hsv[2] = 0.2f;
                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                            }
                                            break;
                                        }
                                    }

                                }
                            });
                        }

                        @Override
                        public void onLoadStarted(Drawable placeholder) {
                            super.onLoadStarted(placeholder);

                            UserCover.setImageBitmap(holderBitmap);
                            Palette.from(holderBitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                if (swatchColorDark != null) {
                                    try {
                                        mutedColor = swatchColorDark.getRgb();
                                        toolbar.setBackgroundColor(mutedColor);
                                        if (AppHelper.isAndroid5()) {

                                            float hsv[] = new float[3];
                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                            hsv[2] = 0.2f;
                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                        }
                                    } catch (Exception e) {
                                        AppHelper.LogCat(" " + e.getMessage());
                                    }
                                } else {
                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                    for (Palette.Swatch swatch : swatches) {
                                        if (swatch != null) {
                                            mutedColor = swatch.getRgb();
                                            toolbar.setBackgroundColor(mutedColor);
                                            if (AppHelper.isAndroid5()) {
                                                float hsv[] = new float[3];
                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                hsv[2] = 0.2f;
                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                            }
                                            break;
                                        }
                                    }

                                }
                            });
                        }
                    };
                    Glide.with(ProfileActivity.this)
                            .load(EndPoints.PROFILE_IMAGE_URL + ImageUrl)
                            .asBitmap()
                            .centerCrop()
                            .placeholder(drawable)
                            .error(drawable)
                            .override(AppConstants.PROFILE_IMAGE_SIZE, AppConstants.PROFILE_IMAGE_SIZE)
                            .into(target);

                } else {
                    Drawable drawable;
                    drawable = textDrawable(mGroupsModel.getGroupName());
                    BitmapImageViewTarget target = new BitmapImageViewTarget(UserCover) {
                        @Override
                        public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            UserCover.setImageBitmap(bitmap);
                            Palette.from(bitmap).maximumColorCount(numberOfColors).generate(palette -> {
                                Palette.Swatch swatchColorDark = palette.getDarkVibrantSwatch();
                                if (swatchColorDark != null) {
                                    try {
                                        mutedColor = swatchColorDark.getRgb();
                                        toolbar.setBackgroundColor(mutedColor);
                                        if (AppHelper.isAndroid5()) {

                                            float hsv[] = new float[3];
                                            Color.colorToHSV(swatchColorDark.getRgb(), hsv);
                                            hsv[2] = 0.2f;
                                            mutedColorStatusBar = Color.HSVToColor(hsv);
                                            getWindow().setStatusBarColor(mutedColorStatusBar);
                                        }
                                    } catch (Exception e) {
                                        AppHelper.LogCat(" " + e.getMessage());
                                    }
                                } else {
                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                    for (Palette.Swatch swatch : swatches) {
                                        if (swatch != null) {
                                            mutedColor = swatch.getRgb();
                                            toolbar.setBackgroundColor(mutedColor);
                                            if (AppHelper.isAndroid5()) {
                                                float hsv[] = new float[3];
                                                Color.colorToHSV(swatch.getRgb(), hsv);
                                                hsv[2] = 0.2f;
                                                mutedColorStatusBar = Color.HSVToColor(hsv);
                                                getWindow().setStatusBarColor(mutedColorStatusBar);
                                            }
                                            break;
                                        }
                                    }

                                }
                            });
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            UserCover.setImageDrawable(errorDrawable);
                        }

                        @Override
                        public void onLoadStarted(Drawable placeholder) {
                            super.onLoadStarted(placeholder);
                            UserCover.setImageDrawable(placeholder);
                        }
                    };
                    Glide.with(ProfileActivity.this)
                            .load(EndPoints.PROFILE_IMAGE_URL + ImageUrl)
                            .asBitmap()
                            .centerCrop()
                            .placeholder(drawable)
                            .error(drawable)
                            .override(AppConstants.PROFILE_IMAGE_SIZE, AppConstants.PROFILE_IMAGE_SIZE)
                            .into(target);
                }
            }
        }.execute();


    }


    public void UpdateGroupUI(GroupsModel groupsModel) {
        try {
            updateUI(groupsModel, null);
        } catch (Exception e) {
            AppHelper.LogCat("Exception " + e.getMessage());
        }

    }


    /**
     * Uploading the file to server
     */
    private class UploadFileToServer extends AsyncTask<Void, Integer, StatusResponse> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
        }

        @Override
        protected StatusResponse doInBackground(Void... params) {
            return uploadFile();
        }

        private StatusResponse uploadFile() {
            RequestBody requestFile;
            final StatusResponse statusResponse = null;
            if (PicturePath != null) {
                // use the FileUtils to get the actual file by uri
                File file = new File(PicturePath);
                // create RequestBody instance from file
                requestFile =
                        RequestBody.create(MediaType.parse("image/*"), file);
            } else {
                requestFile = null;
            }
            APIGroups apiGroups = mApiService.RootService(APIGroups.class, EndPoints.BACKEND_BASE_URL);
            ProfileActivity.this.runOnUiThread(() -> AppHelper.showDialog(ProfileActivity.this, "Updating ... "));
            Call<GroupResponse> statusResponseCall = apiGroups.uploadImage(requestFile, groupID);
            statusResponseCall.enqueue(new Callback<GroupResponse>() {
                @Override
                public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                    if (response.isSuccessful()) {
                        AppHelper.hideDialog();
                        if (response.body().isSuccess()) {
                            int groupId = response.body().getGroupID();
                            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                            realm.executeTransactionAsync(realm1 -> {
                                        GroupsModel groupsModel = realm1.where(GroupsModel.class).equalTo("id", groupId).findFirst();
                                        groupsModel.setGroupImage(response.body().getGroupImage());
                                        realm1.copyToRealmOrUpdate(groupsModel);

                                    }, () -> realm.executeTransactionAsync(realm1 -> {
                                        ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("groupID", groupId).findFirst();
                                        conversationsModel.setRecipientImage(response.body().getGroupImage());
                                        realm1.copyToRealmOrUpdate(conversationsModel);
                                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, conversationsModel.getId()));
                                    }, () -> {
                                        setImage(response.body().getGroupImage());
                                        AppHelper.CustomToast(ProfileActivity.this, response.body().getMessage());
                                    }, error -> AppHelper.LogCat("error update group image in conversation model " + error.getMessage())),
                                    error -> AppHelper.LogCat("error update group image in group model " + error.getMessage()));
                            realm.close();
                        } else {
                            AppHelper.CustomToast(ProfileActivity.this, response.body().getMessage());
                        }
                    } else {
                        AppHelper.hideDialog();
                        AppHelper.CustomToast(ProfileActivity.this, response.message());
                    }
                }

                @Override
                public void onFailure(Call<GroupResponse> call, Throwable t) {
                    AppHelper.hideDialog();
                    AppHelper.LogCat("Failed  upload your image " + t.getMessage());
                }
            });
            return statusResponse;
        }


        @Override
        protected void onPostExecute(StatusResponse response) {
            super.onPostExecute(response);
            // AppHelper.LogCat("Response from server: " + response);

        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        WhatsCloneApplication.getInstance().setConnectivityListener(this);
    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnecting, boolean isConnected) {
        if (!isConnecting && !isConnected) {
            AppHelper.Snackbar(this, containerProfile, getString(R.string.connection_is_not_available), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
        } else if (isConnecting && isConnected) {
            AppHelper.Snackbar(this, containerProfile, getString(R.string.connection_is_available), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
        } else {
            AppHelper.Snackbar(this, containerProfile, getString(R.string.waiting_for_network), AppConstants.MESSAGE_COLOR_WARNING, AppConstants.TEXT_COLOR);

        }
    }
}
