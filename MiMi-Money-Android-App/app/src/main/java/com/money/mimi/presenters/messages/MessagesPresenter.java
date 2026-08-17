package com.money.mimi.presenters.messages;


import android.os.Handler;

import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.GroupsService;
import com.money.mimi.api.apiServices.MessagesService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.services.MainService;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;

import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_IS_READ;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class MessagesPresenter implements Presenter {
    private final MessagesActivity view;
    private final Realm realm;
    private int RecipientID, ConversationID, GroupID;
    private Boolean isGroup;
    private MessagesService mMessagesService;
    private UsersService mUsersContacts;

    private final CompositeDisposable disposables = new CompositeDisposable();


    public MessagesPresenter(MessagesActivity messagesActivity) {
        this.view = messagesActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        if (!EventBus.getDefault().isRegistered(view)) EventBus.getDefault().register(view);

        if (view.getIntent().getExtras() != null) {
            if (view.getIntent().hasExtra("conversationID")) {
                ConversationID = view.getIntent().getExtras().getInt("conversationID");
            }
            if (view.getIntent().hasExtra("recipientID")) {
                RecipientID = view.getIntent().getExtras().getInt("recipientID");
            }

            if (view.getIntent().hasExtra("groupID")) {
                GroupID = view.getIntent().getExtras().getInt("groupID");
            }

            if (view.getIntent().hasExtra("isGroup")) {
                isGroup = view.getIntent().getExtras().getBoolean("isGroup");
            }

        }

        
        APIService mApiService = APIService.with(view.getApplicationContext());
        mMessagesService = new MessagesService(realm);
        mUsersContacts = new UsersService(realm, view.getApplicationContext(), mApiService);
        GroupsService mGroupsService = new GroupsService(realm, view.getApplicationContext(), mApiService);

        disposables.add(
                mUsersContacts.getContactInfo(PreferenceManager.getID(view))
                        .subscribe(view::updateContact, view::onErrorLoading)
        );

        if (isGroup) {

            mGroupsService.getGroupInfo(GroupID).subscribe(view::updateGroupInfo, view::onErrorLoading);
           // mGroupsService.updateGroupMembers(GroupID).subscribe(view::ShowGroupMembers, view::onErrorLoading);
            loadLocalGroupData();
            new Handler().postDelayed(this::updateGroupConversationStatus, 500);
        } else {

            getRecipientInfo();
            loadLocalData();
            new Handler().postDelayed(this::updateConversationStatus, 500);
        }

    }

    public void getRecipientInfo() {

        disposables.add(
                mUsersContacts.getContactInfo(RecipientID)
                        .subscribe(contactsModel -> {
                            view.updateContactRecipient(contactsModel);
                            int ConversationID = getConversationId(contactsModel.getId(), PreferenceManager.getID(view), realm);
                            if (ConversationID != 0) {
                                realm.executeTransaction(realm1 -> {
                                    ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                                    conversationsModel.setRecipientImage(contactsModel.getImage());
                                    conversationsModel.setRecipientUsername(contactsModel.getUsername());
                                    conversationsModel.setRecipientPhone(contactsModel.getWalletAddress());
                                    realm1.copyToRealmOrUpdate(conversationsModel);
                                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                                });
                            }
                        }, view::onErrorLoading)
        );
    }

    public void updateConversationStatus() {
        try {
            realm.executeTransaction(realm1 -> {
                ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).equalTo("RecipientID", RecipientID).findFirst();

                if (conversationsModel1 != null) {
                    conversationsModel1.setStatus(AppConstants.IS_SEEN);
                    conversationsModel1.setUnreadMessageCounter("0");
                    realm1.copyToRealmOrUpdate(conversationsModel1);

                    List<MessagesModel> messagesModel = realm1.where(MessagesModel.class).equalTo("conversationID", ConversationID).equalTo("senderID", RecipientID).findAll();
                    for (MessagesModel messagesModel1 : messagesModel) {
                        if (messagesModel1.getStatus() == AppConstants.IS_WAITING) {
                            messagesModel1.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel1);
                        }
                    }
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));

                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_READ, ConversationID));
                    NotificationsManager.SetupBadger(view);
                    
                    if (RecipientID != 0) {
                        MainService.emitMessageSeen(view, RecipientID);
                    }
                }
            });
        } catch (Exception e) {
            AppHelper.LogCat("There is no conversation unRead MessagesPresenter ");
        }
    }

    public void updateGroupConversationStatus() {
        try {
            realm.executeTransaction(realm1 -> {
                ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).equalTo("groupID", GroupID).findFirst();

                if (conversationsModel1 != null) {
                    conversationsModel1.setStatus(AppConstants.IS_SEEN);
                    conversationsModel1.setUnreadMessageCounter("0");
                    realm1.copyToRealmOrUpdate(conversationsModel1);

                    List<MessagesModel> messagesModel = realm1.where(MessagesModel.class).equalTo("conversationID", ConversationID).notEqualTo("senderID", PreferenceManager.getID(view)).findAll();
                    for (MessagesModel messagesModel1 : messagesModel) {
                        if (messagesModel1.getStatus() == AppConstants.IS_WAITING) {
                            messagesModel1.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel1);
                        }
                    }
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_READ, ConversationID));
                    NotificationsManager.SetupBadger(view);
                    
                    if (GroupID != 0) {
                        MainService.RecipientMarkMessageAsSeenGroup(view, GroupID);
                    }
                }
            });
        } catch (Exception e) {
            AppHelper.LogCat("There is no conversation unRead MessagesPresenter ");
        }
    }

    private void loadLocalGroupData() {
        if (NotificationsManager.getManager())
            NotificationsManager.cancelNotification(GroupID);
        disposables.add(
                mMessagesService.getConversation(ConversationID)
                        .subscribe(view::ShowMessages, view::onErrorLoading, view::onHideLoading)
        );
    }

    private void loadLocalData() {
        if (NotificationsManager.getManager())
            NotificationsManager.cancelNotification(RecipientID);
        try {
            disposables.add(
                    mMessagesService.getConversation(ConversationID, RecipientID, PreferenceManager.getID(view))
                            .subscribe(view::ShowMessages, view::onErrorLoading)
            );
        } catch (Exception e) {
            AppHelper.LogCat("" + e.getMessage());
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
        EventBus.getDefault().unregister(view);
        disposables.clear();
        if (!realm.isClosed())
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
}
