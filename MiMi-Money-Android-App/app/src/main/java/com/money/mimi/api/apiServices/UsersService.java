package com.money.mimi.api.apiServices;

import android.content.Context;

import com.money.mimi.api.APIContact;
import com.money.mimi.api.APIService;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.models.BackupModel;
import com.money.mimi.models.NetworkModel;
import com.money.mimi.models.SettingsResponse;
import com.money.mimi.models.auth.JoinModelResponse;
import com.money.mimi.models.calls.CallSaverModel;
import com.money.mimi.models.calls.CallsInfoModel;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.messages.UpdateMessageModel;
import com.money.mimi.models.users.contacts.BlockResponse;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.models.users.contacts.ProfileResponse;
import com.money.mimi.models.users.contacts.SyncContacts;
import com.money.mimi.models.users.contacts.UsersBlockModel;
import com.money.mimi.models.users.status.EditStatus;
import com.money.mimi.models.users.status.StatusModel;
import com.money.mimi.models.users.status.StatusResponse;

import org.reactivestreams.Subscription;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import okhttp3.RequestBody;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class UsersService {
    private APIContact mApiContact;
    private Context mContext;
    private Realm realm;
    private APIService mApiService;
    private Subscription subscription;

    public UsersService(Realm realm, Context context, APIService mApiService) {
        this.mContext = context;
        this.realm = realm;
        this.mApiService = mApiService;

    }

    public UsersService(Context context, APIService mApiService) {
        this.mContext = context;
        this.mApiService = mApiService;

    }

    /**
     * method to initialize the api contact
     *
     * @return return value
     */
    private APIContact initializeApiContact() {
        if (mApiContact == null) {
            mApiContact = this.mApiService.RootService(APIContact.class, EndPoints.BACKEND_BASE_URL);
        }
        return mApiContact;
    }


    /**
     * method to get all contacts
     *
     * @return return value
     */
    public Observable<RealmResults<ContactsModel>> getAllContacts() {
        RealmResults<ContactsModel> contactsModel = realm.where(ContactsModel.class).notEqualTo("id", PreferenceManager.getID(mContext)).equalTo("Exist", true).sort("Activate", Sort.DESCENDING).sort("Linked", Sort.DESCENDING).sort("username", Sort.ASCENDING).findAll();
        return Observable.just(contactsModel);
    }

    /**
     * method to get linked contacts
     *
     * @return return value
     */
    public Observable<RealmResults<ContactsModel>> getLinkedContacts() {
        RealmResults<ContactsModel> contactsModel = realm.where(ContactsModel.class).notEqualTo("id", PreferenceManager.getID(mContext)).equalTo("Exist", true).equalTo("Linked", true).equalTo("Activate", true).sort("username", Sort.ASCENDING).findAll();
        return Observable.just(contactsModel);
    }

    public int getLinkedContactsSize() {
        RealmResults<ContactsModel> contactsModel = realm.where(ContactsModel.class).notEqualTo("id", PreferenceManager.getID(mContext)).equalTo("Exist", true).equalTo("Linked", true).equalTo("Activate", true).sort("username", Sort.ASCENDING).findAll();
        return contactsModel.size();
    }

    /**
     * method to get linked contacts
     *
     * @return return value
     */
    public Observable<RealmResults<UsersBlockModel>> getBlockedContacts() {
        RealmResults<UsersBlockModel> contactsModel = realm.where(UsersBlockModel.class).notEqualTo("contactsModel.id", PreferenceManager.getID(mContext)).equalTo("contactsModel.Linked", true).equalTo("contactsModel.Activate", true).sort("contactsModel.username", Sort.ASCENDING).findAll();
        return Observable.just(contactsModel).filter(RealmResults::isLoaded);
    }

    /**
     * method to update(syncing) contacts
     *
     * @param contacts
     * @return return value
     */
    public Observable<List<ContactsModel>> updateContacts(List<ContactsModel> contacts) {

        SyncContacts syncContacts = new SyncContacts();
        syncContacts.setContactsModelList(contacts);
        return initializeApiContact().contacts(syncContacts)
                .subscribeOn(Schedulers.io())
                // Read results in Android Main Thread (UI)
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::copyOrUpdateContacts);


    }

    /**
     * method to get general user information
     *
     * @param userID this is parameter  getContact for method
     * @return return value
     */
    public ContactsModel getContact(int userID) {

        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("id", userID).findFirst();
        if (!realm.isClosed()) realm.close();
        return contactsModel;
    }

    /**
     * method to get user information from the server
     *
     * @param userID this is parameter for getContactInfo method
     * @return return  value
     */
    public Observable<ContactsModel> getContactInfo(int userID) {

        Observable<ContactsModel> observable = initializeApiContact().contact(userID)
                // Request API data on IO Scheduler
                .subscribeOn(Schedulers.io())
                // Write to Realm on Computation scheduler
                .observeOn(Schedulers.computation())
                .map(this::copyOrUpdateContactInfo)
                .observeOn(AndroidSchedulers.mainThread())
                .map(contactsModel -> getContact(userID));
        // Read any cached results
        ContactsModel cachedWeather = getContact(userID);
        if (cachedWeather != null)
            // Merge with the observable from API
            observable = observable.mergeWith(Observable.just(cachedWeather));
        return observable;
    }

    /**
     * method to get all status
     *
     * @return return value
     */
    public RealmResults<StatusModel> getAllStatus() {
        return realm.where(StatusModel.class).equalTo("userID", PreferenceManager.getID(mContext)).sort("id", Sort.DESCENDING).findAll();
    }

    /**
     * method to get user status from server
     *
     * @return return value
     */
    public Observable<List<StatusModel>> getUserStatus() {
        Observable<List<StatusModel>> observable = initializeApiContact().status()
                // Request API data on IO Scheduler
                .subscribeOn(Schedulers.io())
                // Write to Realm on Computation scheduler
                .observeOn(Schedulers.computation())
                .map(this::copyOrUpdateStatus)
                .observeOn(AndroidSchedulers.mainThread())
                .map(status -> getAllStatus());
        // Read any cached results
        List<StatusModel> cachedWeather = getAllStatus();
        if (cachedWeather != null)
            // Merge with the observable from API
            observable = observable.mergeWith(Observable.just(cachedWeather));
        return observable;
    }

    /**
     * method to delete user status
     *
     * @param status this is parameter for deleteStatus method
     * @return return  value
     */
    public Observable<StatusResponse> deleteStatus(int status) {
        return initializeApiContact().deleteStatus(status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    /**
     * method to delete all user status
     *
     * @return return value
     */
    public Observable<StatusResponse> deleteAllStatus() {
        return initializeApiContact().deleteAllStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    /**
     * method to update user status
     *
     * @param statusID this is parameter for updateStatus method
     * @return return  value
     */
    public Observable<StatusResponse> updateStatus(int statusID) {
        return initializeApiContact().updateStatus(statusID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    /**
     * method to edit user status
     *
     * @param newStatus this is the first parameter for editStatus method
     * @param statusID  this is the second parameter for editStatus method
     * @return return  value
     */
    public Observable<StatusResponse> editStatus(String newStatus, int statusID) {
        EditStatus editStatus = new EditStatus();
        editStatus.setNewStatus(newStatus);
        editStatus.setStatusID(statusID);
        return initializeApiContact().editStatus(editStatus)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    /**
     * method to edit username
     *
     * @param newName this is parameter for editUsername method
     * @return return  value
     */
    public Observable<StatusResponse> editUsername(String newName) {
        return editUsername(newName, PreferenceManager.getID(mContext));
    }

    public Observable<StatusResponse> editUsername(String newName, int statusID) {
        EditStatus editUsername = new EditStatus();
        editUsername.setNewStatus(newName);
        editUsername.setStatusID(statusID);
        return initializeApiContact().editUsername(editUsername)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    /**
     * method to edit group name
     *
     * @param newName this is the first parameter for editGroupName method
     * @param groupID this is the second parameter for editGroupName method
     * @return return  value
     */
    public Observable<StatusResponse> editGroupName(String newName, int groupID) {
        EditStatus editGroupName = new EditStatus();
        editGroupName.setNewStatus(newName);
        editGroupName.setStatusID(groupID);
        return initializeApiContact().editGroupName(editGroupName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }


    /**
     * method to get current status fron local
     *
     * @return return value
     */
    public Observable<StatusModel> getCurrentStatusFromLocal() {
        StatusModel statusModels = realm.where(StatusModel.class).equalTo("userID", PreferenceManager.getID(mContext)).equalTo("current", true).findFirst();
        if (statusModels != null)
            return Observable.just(statusModels).filter(statusFromLocal -> statusFromLocal.isLoaded()).switchIfEmpty(Observable.just(new StatusModel()));
        else
            return Observable.just(new StatusModel());
    }

    public Observable<BlockResponse> saveEmittedCall(CallSaverModel callSaverModel) {
        return initializeApiContact().saveEmittedCall(callSaverModel)
                .subscribeOn(Schedulers.io())/*
                .observeOn(AndroidSchedulers.mainThread())*/
                .map(usersResponse -> usersResponse);

    }

    public Observable<BlockResponse> saveReceivedCall(CallSaverModel callSaverModel) {
        return initializeApiContact().saveReceivedCall(callSaverModel)
                .subscribeOn(Schedulers.io())/*
                .observeOn(AndroidSchedulers.mainThread())*/
                .map(usersResponse -> usersResponse);

    }

    public Observable<BlockResponse> saveAcceptedCall(CallSaverModel callSaverModel) {
        return initializeApiContact().saveAcceptedCall(callSaverModel)
                .subscribeOn(Schedulers.io())/*
                .observeOn(AndroidSchedulers.mainThread())*/
                .map(usersResponse -> usersResponse);

    }

    public Observable<BlockResponse> block(int userId) {
        return initializeApiContact().block(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(usersResponse -> usersResponse);

    }

    public Observable<BlockResponse> unbBlock(int userId) {
        return initializeApiContact().unBlock(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(usersResponse -> usersResponse);

    }

    public Observable<BackupModel> userHasBackup(String hasBackup) {
        return initializeApiContact().userHasBackup(hasBackup)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(usersResponse -> usersResponse);

    }

    /**
     * method to delete user status
     *
     * @param phone this is parameter for deleteStatus method
     * @return return  value
     */
    public Observable<JoinModelResponse> deleteAccount(String phone, String country) {
        return initializeApiContact().deleteAccount(phone, country)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> response);
    }

    /**
     * method to delete user status
     *
     * @return return  value
     */
    public Observable<ProfileResponse> uploadImage(RequestBody image) {
        return initializeApiContact().uploadImage(image)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> response);
    }


    /**
     * method to copy or update user status
     *
     * @param statusModels this is parameter for copyOrUpdateStatus method
     * @return return  value
     */
    private List<StatusModel> copyOrUpdateStatus(List<StatusModel> statusModels) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        realm.beginTransaction();
        List<StatusModel> statusModels1 = realm.copyToRealmOrUpdate(statusModels);
        realm.commitTransaction();
        if (!realm.isClosed()) realm.close();
        return statusModels1;
    }

    /**
     * method to copy or update contacts list
     *
     * @param mListContacts this is parameter for copyOrUpdateContacts method
     * @return return  value
     */
    private List<ContactsModel> copyOrUpdateContacts(List<ContactsModel> mListContacts) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<ContactsModel> validIncomingContacts = new java.util.ArrayList<>();
        for (ContactsModel contact : mListContacts) {
            if (isValidWalletContact(contact)) {
                validIncomingContacts.add(contact);
            }
        }
        mListContacts = validIncomingContacts;

        RealmResults<ContactsModel> walletContacts = realm.where(ContactsModel.class)
                .equalTo("Exist", true)
                .beginGroup()
                .beginsWith("walletAddress", "0x", io.realm.Case.INSENSITIVE)
                .or()
                .beginsWith("walletAddressTmp", "0x", io.realm.Case.INSENSITIVE)
                .endGroup()
                .findAll();
        List<ContactsModel> savedWalletContacts = new java.util.ArrayList<>();
        for (ContactsModel contact : realm.copyFromRealm(walletContacts)) {
            if (isValidWalletContact(contact)) {
                savedWalletContacts.add(contact);
            }
        }
        AppHelper.LogCat("copyOrUpdateContacts: server sent " + mListContacts.size() + " contacts, backing up " + savedWalletContacts.size() + " wallet contacts");

        for (ContactsModel saved : savedWalletContacts) {
            boolean alreadyInList = false;
            for (ContactsModel incoming : mListContacts) {
                String incomingPhone = incoming.getWalletAddress();
                String savedPhone = saved.getWalletAddress();
                if (incomingPhone != null && savedPhone != null && incomingPhone.equalsIgnoreCase(savedPhone)) {
                    alreadyInList = true;
                    incoming.setExist(true);
                    incoming.setLinked(true);
                    incoming.setActivate(true);
                    if (incoming.getFirstName() == null || incoming.getFirstName().isEmpty()) {
                        incoming.setFirstName(saved.getFirstName());
                    }
                    if (incoming.getLastName() == null || incoming.getLastName().isEmpty()) {
                        incoming.setLastName(saved.getLastName());
                    }
                    if (incoming.getCategory() == null || incoming.getCategory().isEmpty()) {
                        incoming.setCategory(saved.getCategory());
                    }
                    String savedUsername = saved.getUsername();
                    if (savedUsername != null && !savedUsername.isEmpty()
                            && !savedUsername.equalsIgnoreCase(savedPhone)) {
                        incoming.setUsername(savedUsername);
                    }
                    break;
                }
            }
            if (!alreadyInList) {
                AppHelper.LogCat("copyOrUpdateContacts: server doesn't have wallet " + saved.getWalletAddress() + " -> re-adding from backup");
                mListContacts.add(saved);
            }
        }

        realm.beginTransaction();
        RealmResults<ContactsModel> invalidContacts = realm.where(ContactsModel.class).findAll();
        for (int index = invalidContacts.size() - 1; index >= 0; index--) {
            ContactsModel contact = invalidContacts.get(index);
            if (!isValidWalletContact(contact)) {
                contact.deleteFromRealm();
            }
        }
        for (ContactsModel saved : savedWalletContacts) {
            String savedPhone = normalizeWallet(saved.getWalletAddress());
            if (savedPhone == null) continue;
            for (ContactsModel incoming : mListContacts) {
                String incomingPhone = normalizeWallet(incoming.getWalletAddress());
                if (incomingPhone == null || !incomingPhone.equals(savedPhone)) continue;
                migrateWalletContactIdentity(realm, saved, incoming);
                break;
            }
        }
        List<ContactsModel> contactsModels = realm.copyToRealmOrUpdate(mListContacts);
        realm.commitTransaction();
        AppHelper.LogCat("copyOrUpdateContacts: upserted " + mListContacts.size() + " contacts to Realm");
        if (!realm.isClosed()) realm.close();
        return contactsModels;
    }

    private void migrateWalletContactIdentity(Realm realm, ContactsModel oldContact, ContactsModel newContact) {
        if (oldContact == null || newContact == null) return;
        int oldId = oldContact.getId();
        int newId = newContact.getId();
        if (oldId == 0 || newId == 0 || oldId == newId) return;

        String walletAddress = newContact.getWalletAddress() != null
                ? newContact.getWalletAddress()
                : oldContact.getWalletAddress();
        String username = newContact.getUsername();
        if ((username == null || username.trim().isEmpty()) && oldContact.getUsername() != null) {
            username = oldContact.getUsername();
            newContact.setUsername(username);
        }
        if ((newContact.getFirstName() == null || newContact.getFirstName().isEmpty()) && oldContact.getFirstName() != null) {
            newContact.setFirstName(oldContact.getFirstName());
        }
        if ((newContact.getLastName() == null || newContact.getLastName().isEmpty()) && oldContact.getLastName() != null) {
            newContact.setLastName(oldContact.getLastName());
        }
        if ((newContact.getCategory() == null || newContact.getCategory().isEmpty()) && oldContact.getCategory() != null) {
            newContact.setCategory(oldContact.getCategory());
        }
        if (newContact.getWalletAddressTmp() == null) {
            newContact.setWalletAddressTmp(walletAddress);
        }
        newContact.setExist(true);
        newContact.setLinked(true);
        newContact.setActivate(true);

        RealmResults<ConversationsModel> conversations = realm.where(ConversationsModel.class)
                .equalTo("RecipientID", oldId)
                .findAll();
        for (ConversationsModel conversation : conversations) {
            conversation.setRecipientID(newId);
            conversation.setRecipientPhone(walletAddress);
            if (username != null && !username.trim().isEmpty()) {
                conversation.setRecipientUsername(username);
            }
            if (newContact.getImage() != null) {
                conversation.setRecipientImage(newContact.getImage());
            }
        }

        RealmResults<MessagesModel> recipientMessages = realm.where(MessagesModel.class)
                .equalTo("recipientID", oldId)
                .findAll();
        for (MessagesModel message : recipientMessages) {
            message.setRecipientID(newId);
        }

        RealmResults<MessagesModel> senderMessages = realm.where(MessagesModel.class)
                .equalTo("senderID", oldId)
                .findAll();
        for (MessagesModel message : senderMessages) {
            message.setSenderID(newId);
            if (walletAddress != null) {
                message.setWalletAddress(walletAddress);
            }
            if (username != null && !username.trim().isEmpty()) {
                message.setUsername(username);
            }
        }

        ContactsModel managedOld = realm.where(ContactsModel.class)
                .equalTo("id", oldId)
                .findFirst();
        if (managedOld != null && managedOld.isValid()) {
            managedOld.deleteFromRealm();
        }

        AppHelper.LogCat("Migrated wallet contact " + walletAddress + " from local id " + oldId + " to server id " + newId);
    }

    private String normalizeWallet(String walletAddress) {
        if (walletAddress == null) return null;
        String normalized = walletAddress.trim();
        return normalized.matches("^0x[a-fA-F0-9]{40}$") ? normalized.toLowerCase() : null;
    }

    private boolean isValidWalletContact(ContactsModel contact) {
        return contact != null
                && contact.getId() != 0
                && normalizeWallet(contact.getWalletAddress()) != null;
    }


    private List<ContactsModel> checkContactList(List<ContactsModel> contactsModelList, Realm realm) {
        return contactsModelList;
    }

    private boolean checkIfContactExist(int id, Realm realm) {
        RealmQuery<ContactsModel> query = realm.where(ContactsModel.class).equalTo("id", id);
        return query.count() != 0;

    }

    /**
     * method to copy or update user information
     *
     * @param contactsModel this is parameter for copyOrUpdateContactInfo method
     * @return return  value
     */
    private ContactsModel copyOrUpdateContactInfo(ContactsModel contactsModel) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        ContactsModel realmContact;
        realm.beginTransaction();
        boolean isWalletAddress = contactsModel.getWalletAddress() != null
                && contactsModel.getWalletAddress().trim().matches("^0x[a-fA-F0-9]{40}$");
        if (UtilsPhone.checkIfContactExist(mContext, contactsModel.getWalletAddress()) || isWalletAddress) {
            contactsModel.setExist(true);
            if (isWalletAddress) {
                contactsModel.setLinked(true);
                contactsModel.setActivate(true);
            }
        }
        realmContact = realm.copyToRealmOrUpdate(contactsModel);
        realm.commitTransaction();
        if (!realm.isClosed()) realm.close();

        return realmContact;
    }


    public Observable<SettingsResponse> getAppSettings() {
        return initializeApiContact().getAppSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(applicationVersion -> applicationVersion);
    }

    /**
     * method to get app privacy & terms
     *
     * @return return  value
     */
    public Observable<StatusResponse> getPrivacyTerms() {
        return initializeApiContact().getPrivacyTerms()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(privacyTerms -> privacyTerms);
    }

    /**
     * *
     * method to get all calls
     *
     * @return return value
     */
    public Observable<RealmResults<CallsModel>> getAllCalls() {
        RealmResults<CallsModel> callsModel = realm.where(CallsModel.class).sort("date", Sort.DESCENDING).findAll();
        return Observable.just(callsModel);
    }

    /**
     * *
     * method to get all calls details
     *
     * @return return value
     */
    public Observable<RealmResults<CallsInfoModel>> getAllCallsDetails(int callID) {
        RealmResults<CallsInfoModel> callsInfoModel = realm.where(CallsInfoModel.class)
                .equalTo("callId", callID)
                .sort("date", Sort.DESCENDING).findAll();
        return Observable.just(callsInfoModel);
    }

    /**
     * method to get general call information
     *
     * @param callID this is parameter  getContact for method
     * @return return value
     */
    public Observable<CallsModel> getCallDetails(int callID) {
        CallsModel callsModel = realm.where(CallsModel.class).equalTo("id", callID).findFirst();
        if (callsModel != null)
            return Observable.just(callsModel).filter(callsModel1 -> callsModel1.isLoaded()).switchIfEmpty(Observable.just(new CallsModel()));
        else
            return Observable.just(new CallsModel());
    }


    public Observable<NetworkModel> checkIfUserSession() {
        return initializeApiContact().checkNetwork()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(networkModel -> networkModel);
    }


    public Observable<StatusResponse> sendMessage(UpdateMessageModel updateMessageModel) {
        return initializeApiContact().sendMessage(updateMessageModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    public Observable<StatusResponse> sendGroupMessage(UpdateMessageModel updateMessageModel) {
        return initializeApiContact().sendGroupMessage(updateMessageModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    public Observable<StatusResponse> deleteAccountConfirmation(String code) {
        return initializeApiContact().deleteAccountConfirmation(code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }

    public Observable<StatusResponse> updateFcmToken(String fcmToken) {
        return initializeApiContact().updateFcmToken(fcmToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(statusResponse -> statusResponse);
    }
}
