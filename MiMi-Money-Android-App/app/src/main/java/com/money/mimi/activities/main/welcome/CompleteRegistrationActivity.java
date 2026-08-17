package com.money.mimi.activities.main.welcome;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.activities.main.MainActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.fragments.bottomSheets.BottomSheetEditProfile;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.images.ImageUtils;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ProfileResponse;
import com.money.mimi.presenters.users.EditProfilePresenter;
import com.money.mimi.services.MainService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.money.mimi.app.AppConstants.EVENT_BUS_IMAGE_PROFILE_PATH;

/**
 * Created by Abderrahim El imame on 4/1/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CompleteRegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.username_input)
    AppCompatEditText usernameInput;

    @BindView(R.id.userAvatar)
    ImageView userAvatar;

    @BindView(R.id.addAvatar)
    FloatingActionButton addAvatar;

    @BindView(R.id.progress_bar_edit_profile)
    ProgressBar progressBar;

    @BindView(R.id.completeRegistration)
    TextView completeRegistration;


    @BindView(R.id.registerBtn)
    TextView registerBtn;

    @BindView(R.id.completeRegistrationLayout)
    NestedScrollView mView;


    private String PicturePath;
    private boolean hasProfileImage = false;
    private EditProfilePresenter mEditProfilePresenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.complete_registration_activity);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        setTypeFaces();
        mEditProfilePresenter = new EditProfilePresenter(this);
        mEditProfilePresenter.onCreate();
        registerBtn.setOnClickListener(this);
        addAvatar.setOnClickListener(v -> showImagePicker());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.SELECT_PROFILE_PICTURE
                || requestCode == AppConstants.SELECT_PROFILE_CAMERA) {
            mEditProfilePresenter.onActivityResult(this, requestCode, resultCode, data);
        }
    }

    private void showImagePicker() {
        BottomSheetEditProfile bottomSheetEditProfile = new BottomSheetEditProfile();
        bottomSheetEditProfile.show(getSupportFragmentManager(), bottomSheetEditProfile.getTag());
    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            completeRegistration.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            registerBtn.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            usernameInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.registerBtn:
                complete();
                break;

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
            case EVENT_BUS_IMAGE_PROFILE_PATH:
                progressBar.setVisibility(View.VISIBLE);
                PicturePath = String.valueOf(pusher.getData());
                if (PicturePath != null) {
                    try {
                        new UploadFileToServer().execute();
                    } catch (Exception e) {
                        AppHelper.LogCat(e);
                        AppHelper.CustomToast(this, getString(R.string.oops_something));
                    }

                }
                break;

        }

    }

    private void complete() {
        String username = usernameInput.getText().toString().trim();
        
        if (username.isEmpty()) {
            usernameInput.setError(getString(R.string.username_required));
            return;
        }
        
        if (username.length() < 2) {
            usernameInput.setError(getString(R.string.username_too_short));
            return;
        }
        
        mEditProfilePresenter.EditCurrentName(username, true);
    }
    
    private void proceedToMain() {
        PreferenceManager.setIsNeedInfo(this, false);
        if (!AppHelper.isServiceRunning(this, MainService.class)
                && PreferenceManager.getToken(this) != null
                && !PreferenceManager.isNeedProvideInfo(this))
            AppHelper.startMainService(this);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(intent);
        this.finish();
        AnimationsUtil.setSlideInAnimation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEditProfilePresenter.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setImage(String ImageUrl) {
        hasProfileImage = true;
        BitmapImageViewTarget target = new BitmapImageViewTarget(userAvatar) {
            @Override
            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                super.onResourceReady(bitmap, anim);
                userAvatar.setImageBitmap(bitmap);
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

        Glide.with(this)
                .load(EndPoints.EDIT_PROFILE_IMAGE_URL + ImageUrl)
                .asBitmap()
                .centerCrop()
                .transform(new CropCircleTransformation(this))
                .placeholder(R.drawable.image_holder_ur_circle)
                .error(R.drawable.image_holder_ur_circle)
                .override(AppConstants.EDIT_PROFILE_IMAGE_SIZE, AppConstants.EDIT_PROFILE_IMAGE_SIZE)
                .into(target);
    }

    /**
     * Uploading the image  to server
     */
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
                        AppHelper.CustomToast(CompleteRegistrationActivity.this, getString(R.string.failed_upload_image));
                    });
                    return profileResponse;
                }
                // create RequestBody instance from file
                requestFile = RequestBody.create(MediaType.parse("image/*"), imageByte);
            } else {
                requestFile = null;
            }
            APIHelper.initialApiUsersContacts().uploadImage(requestFile).subscribe(response -> {
                if (response.isSuccess()) {

                    if (PicturePath != null) {
                        File file = new File(PicturePath);
                        file.delete();
                    }


                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        AppHelper.CustomToast(CompleteRegistrationActivity.this, response.getMessage());
                        setImage(response.getUserImage());
                    });
                } else {
                    AppHelper.CustomToast(CompleteRegistrationActivity.this, response.getMessage());
                }
            }, throwable -> {
                AppHelper.CustomToast(CompleteRegistrationActivity.this, getString(R.string.failed_upload_image));
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

}
