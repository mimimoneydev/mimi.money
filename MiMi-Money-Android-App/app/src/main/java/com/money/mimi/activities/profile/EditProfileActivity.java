package com.money.mimi.activities.profile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.activities.status.StatusActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.fragments.bottomSheets.BottomSheetEditProfile;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.images.ImageUtils;
import com.money.mimi.interfaces.LoadingData;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.models.users.contacts.ProfileResponse;
import com.money.mimi.presenters.users.EditProfilePresenter;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.money.mimi.app.AppConstants.EVENT_BUS_IMAGE_PROFILE_PATH;
import static com.money.mimi.app.AppConstants.EVENT_BUS_UPDATE_CURRENT_SATUS;


/**
 * Created by Abderrahim El imame on 27/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class EditProfileActivity extends AppCompatActivity implements LoadingData, NetworkListener {

    @BindView(R.id.userAvatar)
    ImageView userAvatar;
    @BindView(R.id.addAvatar)
    FloatingActionButton addAvatar;
    @BindView(R.id.username)
    TextView username;
    @BindView(R.id.status)
    EmojiconTextView status;
    @BindView(R.id.numberPhone)
    TextView numberPhone;
    @BindView(R.id.editProfile)
    NestedScrollView mView;
    @BindView(R.id.progress_bar_edit_profile)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ContactsModel mContactsModel;
    private EditProfilePresenter mEditProfilePresenter;

    private String PicturePath;
    private MemoryCache memoryCache;
    private Socket mSocket;
    private TextDrawable drawable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initializerView();
        connectToChatServer();
        setTypeFaces();
        memoryCache = new MemoryCache();
        mEditProfilePresenter = new EditProfilePresenter(this);
        mEditProfilePresenter.onCreate();
        ActivityCompat.setEnterSharedElementCallback(this, new SharedElementCallback() {
            @Override
            public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
                addAvatar.setVisibility(View.GONE);
                final Animation animation = AnimationUtils.loadAnimation(EditProfileActivity.this, R.anim.scale_for_button_animtion_enter);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        addAvatar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                addAvatar.startAnimation(animation);

            }
        });

    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addAvatar.setOnClickListener(v -> {
            BottomSheetEditProfile bottomSheetEditProfile = new BottomSheetEditProfile();
            bottomSheetEditProfile.show(getSupportFragmentManager(), bottomSheetEditProfile.getTag());
        });
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.statusLayout)
    public void launchStatus() {
        Intent mIntent = new Intent(this, StatusActivity.class);
        startActivity(mIntent);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.editUsernameBtn)
    public void launchEditUsername() {
        if (mContactsModel.getUsername() != null) {
            Intent mIntent = new Intent(this, EditUsernameActivity.class);
            mIntent.putExtra("currentUsername", mContactsModel.getUsername());
            mIntent.putExtra("targetUserID", mContactsModel.getId());
            startActivity(mIntent);
        } else {
            Intent mIntent = new Intent(this, EditUsernameActivity.class);
            mIntent.putExtra("currentUsername", "");
            mIntent.putExtra("targetUserID", mContactsModel.getId());
            startActivity(mIntent);
        }
    }

    /**
     * method to show contact info
     *
     * @param mContactsModel this is parameter for ShowContact  method
     */
    @SuppressLint("StaticFieldLeak")
    public void ShowContact(ContactsModel mContactsModel) {
        final String finalName;
        String name = UtilsPhone.getContactName(mContactsModel.getWalletAddress());
        if (name != null) {
            finalName = name;
        } else {
            finalName = mContactsModel.getWalletAddress();
        }
        this.mContactsModel = mContactsModel;
        if (mContactsModel.getWalletAddress() != null) {
            numberPhone.setText(mContactsModel.getWalletAddress());
        }
        if (mContactsModel.getStatus() != null) {
            String state = UtilsString.unescapeJava(mContactsModel.getStatus());
            status.setText(state);
        } else {
            status.setText(getString(R.string.no_status));
        }
        if (mContactsModel.getUsername() != null) {
            drawable = textDrawable(mContactsModel.getUsername());
            username.setText(mContactsModel.getUsername());
        } else {
            drawable = textDrawable(finalName);
            username.setText(getString(R.string.no_username));
        }


        String ImageUrl = mContactsModel.getImage();
        int recipientId = mContactsModel.getId();

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, EditProfileActivity.this, recipientId, AppConstants.USER, AppConstants.EDIT_PROFILE);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    ImageLoader.SetBitmapImage(bitmap, userAvatar);
                } else {


                    BitmapImageViewTarget target = new BitmapImageViewTarget(userAvatar) {
                        @Override
                        public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            userAvatar.setImageBitmap(bitmap);
                            ImageLoader.DownloadImage(memoryCache, EndPoints.EDIT_PROFILE_IMAGE_URL + ImageUrl, ImageUrl, EditProfileActivity.this, recipientId, AppConstants.USER, AppConstants.EDIT_PROFILE);


                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            userAvatar.setImageDrawable(errorDrawable);
                        }

                        @Override
                        public void onLoadStarted(Drawable placeHolderDrawable) {
                            super.onLoadStarted(placeHolderDrawable);
                            userAvatar.setImageDrawable(placeHolderDrawable);
                        }
                    };
                    Glide.with(EditProfileActivity.this)
                            .load(EndPoints.EDIT_PROFILE_IMAGE_URL + ImageUrl)
                            .asBitmap()
                            .centerCrop()
                            .transform(new CropCircleTransformation(EditProfileActivity.this))
                            .placeholder(drawable)
                            .error(drawable)
                            .override(AppConstants.EDIT_PROFILE_IMAGE_SIZE, AppConstants.EDIT_PROFILE_IMAGE_SIZE)
                            .into(target);
                }
            }
        }.execute();

        userAvatar.setOnClickListener(v -> {
            if (mContactsModel.isValid() && mContactsModel.getImage() != null) {
                if (FilesManager.isFilePhotoProfileExists(this, FilesManager.getProfileImage(mContactsModel.getImage()))) {
                    AppHelper.LaunchImagePreviewActivity(this, AppConstants.PROFILE_IMAGE, mContactsModel.getImage());
                } else {
                    AppHelper.LaunchImagePreviewActivity(EditProfileActivity.this, AppConstants.PROFILE_IMAGE_FROM_SERVER, mContactsModel.getImage());
                }
            }
        });


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final Animation animation = AnimationUtils.loadAnimation(EditProfileActivity.this, R.anim.scale_for_button_animtion_exit);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        addAvatar.setVisibility(View.GONE);
                        if (AppHelper.isAndroid5()) {
                            ActivityCompat.finishAfterTransition(EditProfileActivity.this);
                        } else {
                            finish();

                            AnimationsUtil.setSlideOutAnimation(EditProfileActivity.this);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                addAvatar.startAnimation(animation);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressLint("MissingSuperCall") // The exit animation finishes the Activity explicitly.
    public void onBackPressed() {
        final Animation animation = AnimationUtils.loadAnimation(EditProfileActivity.this, R.anim.scale_for_button_animtion_exit);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                addAvatar.setVisibility(View.GONE);

                if (AppHelper.isAndroid5()) {
                    ActivityCompat.finishAfterTransition(EditProfileActivity.this);
                } else {
                    finish();
                    AnimationsUtil.setSlideOutAnimation(EditProfileActivity.this);
                }

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        addAvatar.startAnimation(animation);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEditProfilePresenter.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mEditProfilePresenter.onActivityResult(this, requestCode, resultCode, data);
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
            case EVENT_BUS_IMAGE_PROFILE_PATH:
                progressBar.setVisibility(View.VISIBLE);
                PicturePath = String.valueOf(pusher.getData());
                if (PicturePath != null) {
                    try {
                        new UploadFileToServer().execute();
                    } catch (Exception e) {
                        AppHelper.LogCat(e);
                        AppHelper.CustomToast(EditProfileActivity.this, getString(R.string.oops_something));
                    }

                }
                break;
            case EVENT_BUS_UPDATE_CURRENT_SATUS:
                mEditProfilePresenter.loadData();
                break;
            case AppConstants.EVENT_BUS_USERNAME_PROFILE_UPDATED:
                mEditProfilePresenter.loadData();
                break;
        }

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

    private void setImage(String ImageUrl) {
        BitmapImageViewTarget target = new BitmapImageViewTarget(userAvatar) {
            @Override
            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                super.onResourceReady(bitmap, anim);
                userAvatar.setImageBitmap(bitmap);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MINE_IMAGE_PROFILE_UPDATED));
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("senderId", PreferenceManager.getID(EditProfileActivity.this));
                    jsonObject.put("walletAddress", PreferenceManager.getWalletAddress(EditProfileActivity.this));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mSocket != null)
                    mSocket.emit(AppConstants.SOCKET_IMAGE_PROFILE_UPDATED, jsonObject);

            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                super.onLoadFailed(e, errorDrawable);
                userAvatar.setImageDrawable(errorDrawable);
            }

            @Override
            public void onLoadStarted(Drawable placeholder) {
                super.onLoadStarted(placeholder);
                userAvatar.setImageDrawable(placeholder);
            }
        };

        Glide.with(EditProfileActivity.this)
                .load(EndPoints.EDIT_PROFILE_IMAGE_URL + ImageUrl)
                .asBitmap()
                .centerCrop()
                .transform(new CropCircleTransformation(EditProfileActivity.this))
                .placeholder(drawable)
                .error(drawable)
                .override(AppConstants.EDIT_PROFILE_IMAGE_SIZE, AppConstants.EDIT_PROFILE_IMAGE_SIZE)
                .into(target);
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
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_not_available), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
        } else if (isConnecting && isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_available), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
        } else {
            AppHelper.Snackbar(this, mView, getString(R.string.waiting_for_network), AppConstants.MESSAGE_COLOR_WARNING, AppConstants.TEXT_COLOR);

        }
    }

    @Override
    public void onShowLoading() {

    }

    @Override
    public void onHideLoading() {

    }

    @Override
    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat(throwable.getMessage());
    }


    /**
     * Uploading the image  to server
     */
    @SuppressLint("StaticFieldLeak")
    private class UploadFileToServer extends AsyncTask<Void, Integer, ProfileResponse> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AppHelper.LogCat("onPreExecute  image ");
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            AppHelper.LogCat("progress image " + (int) (progress[0]));
        }

        @Override
        protected ProfileResponse doInBackground(Void... params) {
            return uploadFile();
        }

        private ProfileResponse uploadFile() {

            RequestBody requestFile;
            final ProfileResponse profileResponse = null;
            if (PicturePath != null) {
                byte[] imageByte = ImageUtils.compressImage(PicturePath);
                if (imageByte == null || imageByte.length == 0) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        AppHelper.hideDialog();
                        AppHelper.CustomToast(EditProfileActivity.this, getString(R.string.failed_upload_image));
                    });
                    return profileResponse;
                }
                // create RequestBody instance from file
                requestFile = RequestBody.create(MediaType.parse("image/*"), imageByte);
            } else {
                requestFile = null;
            }
            EditProfileActivity.this.runOnUiThread(() -> AppHelper.showDialog(EditProfileActivity.this, "Updating ... "));
            APIHelper.initialApiUsersContacts().uploadImage(requestFile).subscribe(response -> {
                if (response.isSuccess()) {
                    if (PicturePath != null) {
                        File file = new File(PicturePath);
                        file.delete();
                    }
                    runOnUiThread(() -> {
                        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                        realm.executeTransactionAsync(realm1 -> {
                            ContactsModel contactsModel = realm1.where(ContactsModel.class).equalTo("id", PreferenceManager.getID(EditProfileActivity.this)).findFirst();
                            contactsModel.setImage(response.getUserImage());
                            realm1.copyToRealmOrUpdate(contactsModel);

                        }, () -> new Handler().postDelayed(() -> {
                            progressBar.setVisibility(View.GONE);
                            AppHelper.hideDialog();
                            AppHelper.CustomToast(EditProfileActivity.this, response.getMessage());
                            setImage(response.getUserImage());
                        }, 700), error -> AppHelper.LogCat("error update group image in group model " + error.getMessage()));
                        realm.close();
                    });
                } else {
                    AppHelper.CustomToast(EditProfileActivity.this, response.getMessage());
                    AppHelper.hideDialog();
                }
            }, throwable -> {
                AppHelper.hideDialog();
                AppHelper.CustomToast(EditProfileActivity.this, getString(R.string.failed_upload_image));
                AppHelper.LogCat("Failed  upload your image " + throwable.getMessage());
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            });
            return profileResponse;
        }


        @Override
        protected void onPostExecute(ProfileResponse response) {
            super.onPostExecute(response);
            // AppHelper.LogCat("Response from server: " + response);

        }


    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            status.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            username.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            numberPhone.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }
}
