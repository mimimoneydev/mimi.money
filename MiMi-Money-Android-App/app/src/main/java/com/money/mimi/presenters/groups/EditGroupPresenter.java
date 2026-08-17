package com.money.mimi.presenters.groups;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.money.mimi.R;
import com.money.mimi.activities.groups.EditGroupActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.fragments.bottomSheets.BottomSheetEditGroupImage;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.users.Pusher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import org.greenrobot.eventbus.EventBus;
import io.realm.Realm;
import io.socket.client.Socket;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class EditGroupPresenter implements Presenter {
    private EditGroupActivity view;
    private BottomSheetEditGroupImage bottomSheetEditGroupImage;
    private Realm realm;
    private UsersService mUsersContacts;
    private APIService mApiService;
    private Socket mSocket;
    private static Uri lastCameraImageUri;


    public EditGroupPresenter(EditGroupActivity editGroupActivity) {
        this.view = editGroupActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }

    public EditGroupPresenter(BottomSheetEditGroupImage bottomSheetEditGroupImage) {
        this.bottomSheetEditGroupImage = bottomSheetEditGroupImage;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }

    public static void setLastCameraImageUri(Uri uri) {
        lastCameraImageUri = uri;
    }

    public static Uri consumeLastCameraImageUri() {
        Uri uri = lastCameraImageUri;
        lastCameraImageUri = null;
        return uri;
    }


    @Override
    public void onStart() {

    }

    @Override
    public void
    onCreate() {
        if (view != null) {

            this.mApiService = APIService.with(view);
            this.mUsersContacts = new UsersService(this.realm, view, this.mApiService);
            connectToChatServer();
        } else if (bottomSheetEditGroupImage != null) {
            this.mApiService = APIService.with(bottomSheetEditGroupImage.getActivity());
            this.mUsersContacts = new UsersService(this.realm, bottomSheetEditGroupImage.getActivity(), this.mApiService);
        }
    }


    /**
     * method to connect to the chat sever by socket
     */
    private void connectToChatServer() {

        WhatsCloneApplication app = (WhatsCloneApplication) view.getApplication();
        mSocket = app.getSocket();
        if (mSocket == null || !mSocket.connected()) {
            AppHelper.startMainService(view);
        }


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


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String imagePath = null;
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;
            switch (requestCode) {
                case AppConstants.SELECT_ADD_NEW_CONTACT:
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_CONTACT_ADDED));
                    break;
                case AppConstants.SELECT_PROFILE_PICTURE:
                    imageUri = data != null ? data.getData() : null;
                    break;
                case AppConstants.SELECT_PROFILE_CAMERA:
                    imageUri = data != null && data.getData() != null ? data.getData() : consumeLastCameraImageUri();
                    break;
            }

            if (imageUri != null && bottomSheetEditGroupImage.getActivity() != null) {
                imagePath = FilesManager.getPath(bottomSheetEditGroupImage.getActivity(), imageUri);
            }

            if (imagePath != null && bottomSheetEditGroupImage.getActivity() != null) {
                imagePath = FilesManager.persistFile(bottomSheetEditGroupImage.getActivity(), imagePath, FilesManager.FILE_TYPE_IMAGE);
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_PATH_GROUP, imagePath));
            } else if (requestCode == AppConstants.SELECT_PROFILE_PICTURE || requestCode == AppConstants.SELECT_PROFILE_CAMERA) {
                AppHelper.LogCat("imagePath is null for group request " + requestCode);
            }
        }

    }


    public void EditCurrentName(String name, int groupID) {
        mUsersContacts.editGroupName(name, groupID).subscribe(statusResponse -> {
            if (statusResponse.isSuccess()) {
                realm.executeTransactionAsync(realm1 -> {
                            GroupsModel groupsModel = realm1.where(GroupsModel.class).equalTo("id", groupID).findFirst();
                            groupsModel.setGroupName(name);
                            realm1.copyToRealmOrUpdate(groupsModel);
                        }, () -> realm.executeTransactionAsync(realm1 -> {
                            ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("groupID", groupID).findFirst();
                            conversationsModel.setRecipientUsername(name);
                            realm1.copyToRealmOrUpdate(conversationsModel);
                        }, () -> {
                            AppHelper.Snackbar(view.getBaseContext(), view.findViewById(R.id.ParentLayoutStatusEdit), statusResponse.getMessage(), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_GROUP_NAME, groupID));
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CREATE_GROUP));

                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("groupId", groupID);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (mSocket != null)
                                mSocket.emit(AppConstants.SOCKET_IMAGE_GROUP_UPDATED, jsonObject);
                            view.finish();
                        }, error -> AppHelper.LogCat("error update group name in conversation model " + error.getMessage())),
                        error -> AppHelper.LogCat("error update group name in group model  " + error.getMessage()));
            } else {
                AppHelper.Snackbar(view.getBaseContext(), view.findViewById(R.id.ParentLayoutStatusEdit), statusResponse.getMessage(), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
            }
        }, AppHelper::LogCat);

    }

}
