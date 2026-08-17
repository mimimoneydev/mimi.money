package com.money.mimi.presenters.messages;

import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.fragments.home.ConversationsFragment;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class ConversationsPresenter implements Presenter {
    private final ConversationsFragment conversationsFragmentView;
    private final Realm realm;


    public ConversationsPresenter(ConversationsFragment conversationsFragment) {
        this.conversationsFragmentView = conversationsFragment;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {
    }

    @Override
    public void onCreate() {
        if (!EventBus.getDefault().isRegistered(conversationsFragmentView))
            EventBus.getDefault().register(conversationsFragmentView);
        loadData(false);


    }


    private void loadData(boolean isRefresh) {
        if (isRefresh) {
            conversationsFragmentView.onShowLoading();
        }

        getConversationFromLocal(isRefresh);
        syncGroupsAndReload(isRefresh);
    }

    private void syncGroupsAndReload(boolean isRefresh) {
        try {
            APIHelper.initializeApiGroups().updateGroups().subscribe(groupsModelList -> {
                AppHelper.LogCat("groupsModelList " + groupsModelList.size());
            }, throwable -> {
                AppHelper.LogCat("onerror " + throwable.getMessage());
            }, () -> {
                AppHelper.LogCat("oncomplete ");
                getConversationFromLocal(isRefresh);
            });
        } catch (Exception e) {
            AppHelper.LogCat("conversation presenter " + e.getMessage());
        }
    }

    private void getConversationFromLocal(boolean isRefresh) {
        try {
            RealmResults<ConversationsModel> conversationsModels = realm.where(ConversationsModel.class)
                    .sort("LastMessageId", Sort.DESCENDING)
                    .findAll();
            AppHelper.LogCat("conversationsModels " + conversationsModels.size());
            List<ConversationsModel> detachedConversations = realm.copyFromRealm(conversationsModels);
            conversationsFragmentView.UpdateConversation(detachedConversations);
            if (isRefresh) {
                conversationsFragmentView.onHideLoading();
            } else {
                conversationsFragmentView.onProgressHide();
            }
        } catch (Exception e) {
            conversationsFragmentView.onErrorLoading(e);
            conversationsFragmentView.onProgressHide();
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
        EventBus.getDefault().unregister(conversationsFragmentView);
        realm.close();
    }

    @Override
    public void onLoadMore() {
    }

    @Override
    public void onRefresh() {
        loadData(true);

    }

    @Override
    public void onStop() {

    }

    public void getGroupInfo(int groupID) {
        AppHelper.LogCat("update image group profile");
        APIHelper.initializeApiGroups().getGroupInfo(groupID).subscribe(groupsModel -> {
            int ConversationID = getConversationGroupId(groupsModel.getId());
            if (ConversationID != 0) {
                realm.executeTransaction(realm1 -> {
                    ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                    conversationsModel.setRecipientImage(groupsModel.getGroupImage());
                    conversationsModel.setRecipientUsername(groupsModel.getGroupName());
                    realm1.copyToRealmOrUpdate(conversationsModel);
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                });
            }
        }, throwable -> {
            AppHelper.LogCat("Get group info conversation presenter " + throwable.getMessage());
        });
    }

    private int getConversationGroupId(int GroupID) {
        try {
            ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("groupID", GroupID).findFirst();
            return conversationsModel.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id Exception ContactFragment" + e.getMessage());
            return 0;
        }
    }

    public void getGroupInfo(int groupID, MessagesModel messagesModel) {
        AppHelper.LogCat("group id exited " + groupID);
        APIHelper.initializeApiGroups().getGroupInfo(groupID).subscribe(groupsModel -> {
            conversationsFragmentView.sendGroupMessage(groupsModel, messagesModel);
        }, throwable -> {
            AppHelper.LogCat("Get group info conversation presenter " + throwable.getMessage());
        });

    }

    public void getGroupInfo(int groupID, int conversationID) {
        AppHelper.LogCat("group id created " + groupID);
        APIHelper.initializeApiGroups().getGroupInfo(groupID).subscribe(groupsModel -> {
            conversationsFragmentView.sendGroupMessage(groupsModel, conversationID);
        }, throwable -> {
            AppHelper.LogCat("Get group info conversation presenter " + throwable.getMessage());
        });
    }
}
