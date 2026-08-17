package com.money.mimi.api.apiServices;

import android.content.Context;

import com.money.mimi.api.APIGroups;
import com.money.mimi.api.APIService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;
import com.money.mimi.models.groups.GroupResponse;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.models.groups.MembersGroupModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import okhttp3.RequestBody;

import static com.money.mimi.app.AppConstants.EVENT_BUS_DELETE_CONVERSATION_ITEM;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class GroupsService {
    private APIGroups mApiGroups;
    private Context mContext;
    private Realm realm;
    private APIService mApiService;
    private int lastConversationID;

    public GroupsService(Realm realm, Context context, APIService mApiService) {
        this.mContext = context;
        this.realm = realm;
        this.mApiService = mApiService;

    }

    /**
     * method to initialize the api groups
     *
     * @return return value
     */
    private APIGroups initializeApiGroups() {
        if (mApiGroups == null) {
            mApiGroups = this.mApiService.RootService(APIGroups.class, EndPoints.BACKEND_BASE_URL);
        }
        return mApiGroups;
    }

    /**
     * method to get all groups list
     *
     * @return return value
     */
    public Observable<List<GroupsModel>> getGroups() {
        List<GroupsModel> groups = realm.where(GroupsModel.class).findAll();
        return Observable.just(groups);
    }

    /**
     * method to update groups
     *
     * @return return value
     */

    public Observable<List<GroupsModel>> updateGroups() {
        return initializeApiGroups().groups()
                // Request API data on computation Scheduler
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::checkGroups);
    }

    /**
     * method to get single group information
     *
     * @param groupID this is parameter for  getGroupInfo method
     * @return return value
     */
    public Observable<GroupsModel> getGroupInfo(int groupID) {

        Observable<GroupsModel> observable = initializeApiGroups().getGroup(groupID)
                // Request API data on IO Scheduler
                .subscribeOn(Schedulers.io())
                // Write to Realm on Computation scheduler
                .observeOn(Schedulers.computation())
                .map(this::copyOrUpdateGroup)
                .observeOn(AndroidSchedulers.mainThread())
                .map(groupsModel -> getGroup(groupID));

        // Read any cached results
        GroupsModel cachedWeather = getGroup(groupID);
        if (cachedWeather != null)
            // Merge with the observable from API
            observable = observable.mergeWith(Observable.just(cachedWeather));
        return observable;
    }

    /**
     * method to get group information from local
     *
     * @param groupID this is parameter for getGroup method
     * @return return value
     */
    public GroupsModel getGroup(int groupID) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        GroupsModel groupsModel = realm.where(GroupsModel.class).equalTo("id", groupID).findFirst();
        if (!realm.isClosed()) realm.close();
        return groupsModel;
    }


    private List<GroupsModel> checkGroups(List<GroupsModel> groupsModels) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        if (groupsModels.size() != 0) {
            for (GroupsModel groupsModel1 : groupsModels) {
                if (!groupsModel1.isDeleted()) {

                    if (!checkIfGroupConversationExist(groupsModel1.getId(), realm)) {
                        RealmList<MessagesModel> newMessagesModelRealmList = new RealmList<MessagesModel>();
                        realm.executeTransaction(realm1 -> {
                            int lastConversationID = RealmBackupRestore.getConversationLastId();
                            int lastID;
                            lastID = RealmBackupRestore.getMessageLastId();
                            MessagesModel messagesModel = new MessagesModel();
                            messagesModel.setId(lastID);
                            messagesModel.setDate(groupsModel1.getCreatedDate());
                            messagesModel.setSenderID(groupsModel1.getCreatorID());
                            messagesModel.setRecipientID(0);
                            messagesModel.setWalletAddress(groupsModel1.getCreator());
                            messagesModel.setStatus(AppConstants.IS_SEEN);
                            messagesModel.setUsername(null);
                            messagesModel.setGroup(true);
                            messagesModel.setMessage(AppConstants.CREATE_GROUP);
                            messagesModel.setGroupID(groupsModel1.getId());
                            messagesModel.setConversationID(lastConversationID);
                            messagesModel.setImageFile(null);
                            messagesModel.setVideoFile(null);
                            messagesModel.setAudioFile(null);
                            messagesModel.setDocumentFile(null);
                            messagesModel.setVideoThumbnailFile(null);
                            messagesModel.setFileUpload(true);
                            messagesModel.setFileDownLoad(true);
                            messagesModel.setFileSize(null);
                            messagesModel.setDuration(null);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            newMessagesModelRealmList.add(messagesModel);
                            realm1.copyToRealmOrUpdate(groupsModel1);
                            ConversationsModel conversationsModel = new ConversationsModel();
                            conversationsModel.setId(lastConversationID);
                            conversationsModel.setLastMessageId(lastID);
                            conversationsModel.setRecipientID(0);
                            conversationsModel.setCreatorID(groupsModel1.getCreatorID());
                            conversationsModel.setRecipientUsername(groupsModel1.getGroupName());
                            conversationsModel.setRecipientImage(groupsModel1.getGroupImage());
                            conversationsModel.setGroupID(groupsModel1.getId());
                            conversationsModel.setMessageDate(groupsModel1.getCreatedDate());
                            conversationsModel.setGroup(true);
                            conversationsModel.setMessages(newMessagesModelRealmList);
                            conversationsModel.setStatus(AppConstants.IS_SEEN);
                            conversationsModel.setUnreadMessageCounter("0");
                            conversationsModel.setCreatedOnline(true);
                            realm1.copyToRealmOrUpdate(conversationsModel);
                            this.lastConversationID = lastConversationID;
                        });

                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_NEW_ROW, this.lastConversationID));

                    }
                    copyOrUpdateGroup(groupsModel1);
                } else {
                    realm.executeTransactionAsync(realm1 -> {
                        GroupsModel groupsModel = realm1.where(GroupsModel.class).equalTo("id", groupsModel1.getId()).findFirst();
                        ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("groupID", groupsModel1.getId()).findFirst();
                        if (conversationsModel != null && conversationsModel.getId() != 0) {
                            EventBus.getDefault().post(new Pusher(EVENT_BUS_DELETE_CONVERSATION_ITEM, conversationsModel.getId()));
                            RealmResults<MessagesModel> messagesModel1 = realm1.where(MessagesModel.class).equalTo("conversationID", conversationsModel.getId()).findAll();
                            messagesModel1.deleteAllFromRealm();
                            conversationsModel.deleteFromRealm();
                            groupsModel.deleteFromRealm();
                        }
                    });
                }
            }
        } else {
            realm.executeTransactionAsync(realm1 -> {
                RealmResults<GroupsModel> groupsModels1 = realm1.where(GroupsModel.class).findAll();
                groupsModels1.deleteAllFromRealm();
                RealmResults<ConversationsModel> conversationsModels = realm1.where(ConversationsModel.class).equalTo("isGroup", true).findAll();
                for (ConversationsModel conversationsModel : conversationsModels) {
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_DELETE_CONVERSATION_ITEM, conversationsModel.getId()));
                    RealmResults<MessagesModel> messagesModel1 = realm1.where(MessagesModel.class).equalTo("conversationID", conversationsModel.getId()).findAll();
                    messagesModel1.deleteAllFromRealm();
                    conversationsModel.deleteFromRealm();
                }
            });
        }
        if (checkIfZeroExist(realm)) {
            realm.executeTransactionAsync(realm1 -> {
                RealmResults<MessagesModel> messagesModel = realm1.where(MessagesModel.class).equalTo("id", 0).findAll();
                for (MessagesModel messagesModel1 : messagesModel) {
                    messagesModel1.deleteFromRealm();
                }
            }, () -> {
                AppHelper.LogCat("messagesModel with 0 id removed");
            }, error -> {
                AppHelper.LogCat("messagesModel with 0 id failed to remove " + error.getMessage());
            });
        }
        if (!realm.isClosed()) realm.close();
        return groupsModels;
    }

    /**
     * method to copy or update a single group
     *
     * @param groupsModel this is parameter for copyOrUpdateGroup method
     * @return return value
     */
    private GroupsModel copyOrUpdateGroup(GroupsModel groupsModel) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        realm.beginTransaction();
        GroupsModel realmGroups = realm.copyToRealmOrUpdate(groupsModel);
        realm.commitTransaction();
        if (!realm.isClosed()) realm.close();
        return realmGroups;
    }

    /**
     * method to check if a group conversation exist
     *
     * @param groupID this is parameter for checkIfGroupConversationExist method
     * @return return value
     */
    private boolean checkIfGroupConversationExist(int groupID, Realm realm) {
        RealmQuery<ConversationsModel> query = realm.where(ConversationsModel.class).equalTo("groupID", groupID);
        return query.count() != 0;
    }

    /**
     * method to check for id 0
     *
     * @return return value
     */
    public boolean checkIfZeroExist(Realm realm) {
        RealmQuery<MessagesModel> query = realm.where(MessagesModel.class).equalTo("id", 0);
        return query.count() != 0;
    }


    /**
     * methods for get group members
     *
     * @param groupID this is parameter for getGroupMembers method
     * @return return value
     */
    public List<MembersGroupModel> getGroupMembers(int groupID) {

        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MembersGroupModel> membersGroupModels = realm.where(MembersGroupModel.class).equalTo("groupID", groupID).equalTo("Deleted", false).equalTo("isLeft", false).findAll();
        if (!realm.isClosed()) realm.close();
        return membersGroupModels;
    }

    /**
     * method to update group members
     *
     * @param groupID this is parameter for updateGroupMembers method
     * @return return value
     */


    public Observable<List<MembersGroupModel>> updateGroupMembers(int groupID) {
        Observable<List<MembersGroupModel>> observable = initializeApiGroups().groupMembers(groupID)
                // Request API data on IO Scheduler
                .subscribeOn(Schedulers.io())
                // Write to Realm on Computation scheduler
                .observeOn(Schedulers.computation())
                .map(this::copyOrUpdateGroupMembers)
                .observeOn(AndroidSchedulers.mainThread())
                .map(membersGroupModels -> getGroupMembers(groupID));
        // Read any cached results
        List<MembersGroupModel> cachedWeather = getGroupMembers(groupID);
        if (cachedWeather != null)
            // Merge with the observable from API
            observable = observable.mergeWith(Observable.just(cachedWeather));
        return observable;
    }

    /**
     * method to copy or update group members
     *
     * @param groupMembers this is parameter for copyOrUpdateGroupMembers method
     * @return return value
     */
    private List<MembersGroupModel> copyOrUpdateGroupMembers
    (List<MembersGroupModel> groupMembers) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MembersGroupModel> finalList = checkGroupMembers(groupMembers, realm);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        realm.beginTransaction();
        List<MembersGroupModel> realmGroupMembers = realm.copyToRealmOrUpdate(finalList);
        realm.commitTransaction();
        if (!realm.isClosed()) realm.close();
        return realmGroupMembers;
    }

    private List<MembersGroupModel> checkGroupMembers(List<MembersGroupModel> groupMembers, Realm realm) {
        if (groupMembers.size() != 0) {
            realm.executeTransactionAsync(realm1 -> {
                RealmResults<MembersGroupModel> groupMember = realm1.where(MembersGroupModel.class).findAll();
                groupMember.deleteAllFromRealm();
            });
        }
        return groupMembers;
    }


    public Observable<GroupResponse> createGroup(int userID,
                                                 RequestBody name,
                                                 RequestBody image,
                                                 RequestBody ids,
                                                 RequestBody notificationKey,
                                                 String date) {
        return initializeApiGroups().createGroup(userID, name, image, ids, notificationKey, date)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(groupResponse -> groupResponse);

    }

    /**
     * method to exit a group
     *
     * @param groupID this is parameter for ExitGroup method
     * @return return value
     */
    public Observable<GroupResponse> ExitGroup(int groupID) {
        return initializeApiGroups().exitGroup(groupID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * method to delete group
     *
     * @param groupID this is parameter for DeleteGroup method
     * @return return value
     */
    public Observable<GroupResponse> DeleteGroup(int groupID) {
        return initializeApiGroups().deleteGroup(groupID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
