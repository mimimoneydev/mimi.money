package com.money.mimi.presenters.users;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.money.mimi.R;
import com.money.mimi.activities.main.MainActivity;
import com.money.mimi.activities.main.welcome.CompleteRegistrationActivity;
import com.money.mimi.activities.profile.EditProfileActivity;
import com.money.mimi.activities.profile.EditUsernameActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.services.MainService;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class EditProfilePresenter implements Presenter {
    private EditProfileActivity view;
    private EditUsernameActivity editUsernameActivity;

    private CompleteRegistrationActivity completeRegistrationActivity;
    private Realm realm;
    private UsersService mUsersContacts;
    private boolean isEditUsername = false;
    private APIService mApiService;
    private static Uri lastCameraImageUri;

    public APIService getmApiService() {
        return mApiService;
    }

    public EditProfilePresenter(CompleteRegistrationActivity completeRegistrationActivity) {
        this.completeRegistrationActivity = completeRegistrationActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    public EditProfilePresenter(EditProfileActivity editProfileActivity) {
        this.view = editProfileActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }


    public EditProfilePresenter() {
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    public EditProfilePresenter(EditUsernameActivity editUsernameActivity, boolean b) {
        this.isEditUsername = b;
        this.editUsernameActivity = editUsernameActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    public static void setLastCameraImageUri(Uri uri) {
        lastCameraImageUri = uri;
    }


    @Override
    public void onStart() {

    }

    @Override
    public void
    onCreate() {
        if (!isEditUsername) {
            if (completeRegistrationActivity != null) {
                APIService mApiService = APIService.with(completeRegistrationActivity);
                mUsersContacts = new UsersService(realm, completeRegistrationActivity, mApiService);
            } else {
                mApiService = APIService.with(view);
                mUsersContacts = new UsersService(realm, view, mApiService);
                loadData();
            }
        } else {
            mApiService = APIService.with(editUsernameActivity);
            this.mUsersContacts = new UsersService(realm, editUsernameActivity, mApiService);

        }

    }

    public void loadData() {
        mUsersContacts.getContactInfo(PreferenceManager.getID(view)).subscribe(contactsModel -> {
            view.ShowContact(contactsModel);
        }, throwable -> {
            view.onErrorLoading(throwable);
        });


    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {
        realm.close();
    }

    @Override
    public void onLoadMore() {

    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onStop() {

    }


    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        String imagePath = null;
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;
            switch (requestCode) {
                case AppConstants.SELECT_PROFILE_PICTURE:
                    imageUri = data != null ? data.getData() : null;
                    break;
                case AppConstants.SELECT_PROFILE_CAMERA:
                    imageUri = data != null && data.getData() != null ? data.getData() : lastCameraImageUri;
                    lastCameraImageUri = null;
                    break;
            }

            if (imageUri != null) {
                imagePath = FilesManager.getPath(activity, imageUri);
            }

            if (imagePath != null) {
                imagePath = FilesManager.persistFile(activity, imagePath, FilesManager.FILE_TYPE_IMAGE);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_IMAGE_PROFILE_PATH, imagePath));
            } else {
                AppHelper.LogCat("imagePath is null for profile request " + requestCode);
            }
        }

    }


    public void EditCurrentName(String name, boolean forComplete) {
        int statusID;
        if (forComplete) {
            statusID = PreferenceManager.getID(completeRegistrationActivity);
        } else {
            statusID = PreferenceManager.getID(editUsernameActivity);
        }
        EditCurrentName(name, forComplete, statusID);
    }

    public void EditCurrentName(String name, boolean forComplete, int statusID) {
        mUsersContacts.editUsername(name, statusID).subscribe(statusResponse -> {
            if (statusResponse.isSuccess()) {
                if (forComplete) {
                    AppHelper.Snackbar(completeRegistrationActivity.getBaseContext(), completeRegistrationActivity.findViewById(R.id.completeRegistrationLayout), statusResponse.getMessage(), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
                    PreferenceManager.setIsNeedInfo(completeRegistrationActivity, false);

                    if (!AppHelper.isServiceRunning(completeRegistrationActivity, MainService.class)
                            && PreferenceManager.getToken(completeRegistrationActivity) != null
                            && !PreferenceManager.isNeedProvideInfo(completeRegistrationActivity))
                        AppHelper.startMainService(completeRegistrationActivity);
                    Intent intent = new Intent(completeRegistrationActivity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    completeRegistrationActivity.startActivity(intent);
                    completeRegistrationActivity.finish();
                    AnimationsUtil.setSlideInAnimation(completeRegistrationActivity);
                } else {
                    AppHelper.Snackbar(editUsernameActivity.getBaseContext(), editUsernameActivity.findViewById(R.id.ParentLayoutStatusEdit), statusResponse.getMessage(), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
                    Pusher pusher = new Pusher(AppConstants.EVENT_BUS_USERNAME_PROFILE_UPDATED);
                    pusher.setUserID(statusID);
                    EventBus.getDefault().post(pusher);
                    editUsernameActivity.finish();
                }
            } else {
                if (!forComplete) {
                    AppHelper.Snackbar(editUsernameActivity.getBaseContext(), editUsernameActivity.findViewById(R.id.ParentLayoutStatusEdit), statusResponse.getMessage(), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
                } else {
                    AppHelper.Snackbar(completeRegistrationActivity.getBaseContext(), completeRegistrationActivity.findViewById(R.id.completeRegistrationLayout), completeRegistrationActivity.getString(R.string.oops_something), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);

                }
            }
        }, AppHelper::LogCat);

    }

}
