package com.money.mimi.presenters.users;


import com.money.mimi.activities.profile.ProfilePreviewActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.GroupsService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.users.Pusher;

import org.greenrobot.eventbus.EventBus;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Abderrahim El imame on 20/02/2016. Email : abderrahim.elimame@gmail.com
 */
public class ProfilePreviewPresenter implements Presenter {
    private ProfilePreviewActivity profilePreviewActivity;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private Realm realm;


    public ProfilePreviewPresenter(ProfilePreviewActivity profilePreviewActivity) {
        this.profilePreviewActivity = profilePreviewActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }


    @Override
    public void onStart() {

    }

    @Override
    public void
    onCreate() {
        if (profilePreviewActivity != null) {
            APIService mApiService = APIService.with(profilePreviewActivity);

            if (profilePreviewActivity.getIntent().hasExtra("userID")) {
                int userID = profilePreviewActivity.getIntent().getExtras().getInt("userID");
                UsersService mUsersContacts = new UsersService(realm, profilePreviewActivity, mApiService);
                disposables.add(mUsersContacts.getContactInfo(userID).subscribe(contactsModel -> {
                    profilePreviewActivity.ShowContact(contactsModel);
                    int ConversationID = getConversationId(contactsModel.getId(), PreferenceManager.getID(profilePreviewActivity), realm);
                    if (ConversationID != 0) {
                        realm.executeTransaction(realm1 -> {
                            ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                            conversationsModel.setRecipientImage(contactsModel.getImage());
                            realm1.copyToRealmOrUpdate(conversationsModel);
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                        });
                    }
                }, throwable -> {
                    profilePreviewActivity.onErrorLoading(throwable);
                }));

            }

            if (profilePreviewActivity.getIntent().hasExtra("groupID")) {
                GroupsService mGroupsService = new GroupsService(realm, profilePreviewActivity, mApiService);
                int groupID = profilePreviewActivity.getIntent().getExtras().getInt("groupID");


                disposables.add(mGroupsService.getGroupInfo(groupID).subscribe(groupsModel -> {
                    profilePreviewActivity.ShowGroup(groupsModel);
                    int ConversationID = getConversationGroupId(groupsModel.getId(), realm);
                    if (ConversationID != 0) {
                        realm.executeTransaction(realm1 -> {
                            ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                            assert conversationsModel != null;
                            conversationsModel.setRecipientImage(groupsModel.getGroupImage());
                            conversationsModel.setRecipientUsername(groupsModel.getGroupName());
                            realm1.copyToRealmOrUpdate(conversationsModel);
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                        });
                    }
                }, throwable -> {
                    profilePreviewActivity.onErrorLoading(throwable);
                }));

            }
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
        disposables.clear();
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

    /**
     * method to get a conversation id
     *
     * @param recipientId this is the first parameter for getConversationId method
     * @param senderId    this is the second parameter for getConversationId method
     * @return conversation id
     */
    private int getConversationId(int recipientId, int senderId, Realm realm) {
        try {
            ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                    .beginGroup()
                    .equalTo("RecipientID", recipientId)
                    .or()
                    .equalTo("RecipientID", senderId)
                    .endGroup().findAll().first();
            return conversationsModelNew.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id Exception ContactFragment" + e.getMessage());
            return 0;
        }
    }

    private int getConversationGroupId(int GroupID, Realm realm) {
        try {
            ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("groupID", GroupID).findFirst();
            return conversationsModel.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id Exception ContactFragment" + e.getMessage());
            return 0;
        }
    }
}