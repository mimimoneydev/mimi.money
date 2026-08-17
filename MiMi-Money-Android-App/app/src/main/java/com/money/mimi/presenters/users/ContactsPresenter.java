package com.money.mimi.presenters.users;


import android.Manifest;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import com.money.mimi.R;
import com.money.mimi.activities.NewConversationContactsActivity;
import com.money.mimi.activities.PrivacyActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.models.users.contacts.ContactsModel;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class ContactsPresenter implements Presenter {
    private NewConversationContactsActivity newConversationContactsActivity;
    private PrivacyActivity privacyActivity;
    private Realm realm;
    private UsersService mUsersContacts;
    private boolean hasSyncedDeviceContacts = false;


    public ContactsPresenter(NewConversationContactsActivity newConversationContactsActivity) {
        this.newConversationContactsActivity = newConversationContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }


    public ContactsPresenter(PrivacyActivity privacyActivity) {
        this.privacyActivity = privacyActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {
    }

    @Override
    public void onCreate() {
        if (privacyActivity != null) {
            APIService mApiService = APIService.with(privacyActivity);
            mUsersContacts = new UsersService(realm, privacyActivity, mApiService);
            getPrivacyTerms();
        } else if (newConversationContactsActivity != null) {
            APIService mApiService = APIService.with(newConversationContactsActivity);
            mUsersContacts = new UsersService(realm, newConversationContactsActivity, mApiService);
            getContacts();
        }

    }


    public void getContacts() {
        if (newConversationContactsActivity != null) {
            if (!hasSyncedDeviceContacts) {
                hasSyncedDeviceContacts = true;
                loadDataFromServer();
            }
            try {
                mUsersContacts.getAllContacts().subscribe(contactsModels -> {
                    newConversationContactsActivity.ShowContacts(contactsModels);
                }, throwable -> {
                    newConversationContactsActivity.onErrorLoading(throwable);
                }, () -> {
                    newConversationContactsActivity.onHideLoading();
                });
                try {
                    PreferenceManager.setContactSize(newConversationContactsActivity, mUsersContacts.getLinkedContactsSize());
                } catch (Exception e) {
                    AppHelper.LogCat(" Exception size contact fragment");
                }
            } catch (Exception e) {
                AppHelper.LogCat("getAllContacts Exception ContactsPresenter ");
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
        if (!realm.isClosed())
            realm.close();
    }

    @Override
    public void onLoadMore() {

    }


    @Override
    public void onRefresh() {
        if (newConversationContactsActivity != null) {
            newConversationContactsActivity.onShowLoading();
            getContacts();
            mUsersContacts.getContactInfo(PreferenceManager.getID(newConversationContactsActivity)).subscribe(contactsModel -> AppHelper.LogCat("getContactInfo"), AppHelper::LogCat);
        }


    }

    @Override
    public void onStop() {

    }


    private void loadDataFromServer() {

        Observable.create((ObservableOnSubscribe<List<ContactsModel>>) subscriber -> {
            try {
                List<ContactsModel> contactsModels = new java.util.ArrayList<>();
                if (newConversationContactsActivity != null
                        && ContextCompat.checkSelfPermission(newConversationContactsActivity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    for (ContactsModel contact : UtilsPhone.GetPhoneContacts()) {
                        addWalletContactIfMissing(contactsModels, contact);
                    }
                }
                Realm walletRealm = WhatsCloneApplication.getRealmDatabaseInstance();
                io.realm.RealmResults<ContactsModel> walletContacts = walletRealm.where(ContactsModel.class)
                        .equalTo("Exist", true)
                        .beginGroup()
                        .beginsWith("walletAddress", "0x", io.realm.Case.INSENSITIVE)
                        .or()
                        .beginsWith("walletAddressTmp", "0x", io.realm.Case.INSENSITIVE)
                        .endGroup()
                        .findAll();
                for (ContactsModel wc : walletContacts) {
                    ContactsModel syncModel = new ContactsModel();
                    syncModel.setWalletAddress(wc.getWalletAddress());
                    syncModel.setWalletAddressTmp(wc.getWalletAddressTmp());
                    addWalletContactIfMissing(contactsModels, syncModel);
                }
                if (!walletRealm.isClosed()) walletRealm.close();
                persistWalletContactsLocally(contactsModels);
                AppHelper.LogCat("loadDataFromServer: sending " + contactsModels.size() + " wallet contacts to server");
                subscriber.onNext(contactsModels);
                subscriber.onComplete();
            } catch (Exception throwable) {
                subscriber.onError(throwable);
            }
        }).subscribeOn(Schedulers.computation()).subscribe(contacts -> {
            mUsersContacts.updateContacts(contacts).subscribe(contactsModelList -> {
                getContacts();
                new Handler().postDelayed(() -> {
                    mUsersContacts.getContactInfo(PreferenceManager.getID(newConversationContactsActivity)).subscribe(contactsModel -> AppHelper.LogCat("info user ContactsPresenter"), throwable -> AppHelper.LogCat("On error ContactsPresenter"));
                }, 2000);
            }, throwable -> {
                newConversationContactsActivity.onErrorLoading(throwable);
            }, () -> {

            });
        }, throwable -> {
            AppHelper.LogCat(" " + throwable.getMessage());
        });

    }

    private void addWalletContactIfMissing(List<ContactsModel> contactsModels, ContactsModel contact) {
        if (contact == null || contact.getWalletAddress() == null) return;
        String walletAddress = contact.getWalletAddress();
        if (!walletAddress.trim().matches("^0x[a-fA-F0-9]{40}$")) return;
        for (ContactsModel existing : contactsModels) {
            if (existing.getWalletAddress() != null
                    && existing.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                return;
            }
        }
        contactsModels.add(contact);
    }

    private void persistWalletContactsLocally(List<ContactsModel> contactsModels) {
        if (contactsModels == null || contactsModels.isEmpty()) return;
        Realm localRealm = null;
        try {
            localRealm = WhatsCloneApplication.getRealmDatabaseInstance();
            Realm finalLocalRealm = localRealm;
            finalLocalRealm.executeTransaction(transactionRealm -> {
                for (ContactsModel contact : contactsModels) {
                    if (contact == null || contact.getWalletAddress() == null) continue;
                    String walletAddress = contact.getWalletAddress().trim();
                    if (!walletAddress.matches("^0x[a-fA-F0-9]{40}$")) continue;

                    ContactsModel existing = transactionRealm.where(ContactsModel.class)
                            .equalTo("walletAddress", walletAddress, io.realm.Case.INSENSITIVE)
                            .findFirst();
                    if (existing == null) {
                        existing = transactionRealm.where(ContactsModel.class)
                                .equalTo("walletAddressTmp", walletAddress, io.realm.Case.INSENSITIVE)
                                .findFirst();
                    }
                    if (existing == null) {
                        existing = transactionRealm.createObject(ContactsModel.class, createLocalWalletContactId(transactionRealm));
                    }

                    String username = contact.getUsername();
                    existing.setUsername(username != null && !username.trim().isEmpty() ? username : walletAddress);
                    existing.setWalletAddress(walletAddress);
                    existing.setWalletAddressTmp(walletAddress);
                    existing.setContactID(contact.getContactID());
                    existing.setImage(contact.getImage());
                    existing.setExist(true);
                    existing.setLinked(true);
                    existing.setActivate(true);
                }
            });
        } catch (Exception e) {
            AppHelper.LogCat("persistWalletContactsLocally failed: " + e.getMessage());
        } finally {
            if (localRealm != null && !localRealm.isClosed()) localRealm.close();
        }
    }

    private int createLocalWalletContactId(Realm localRealm) {
        int walletId = -(int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        if (walletId >= 0) walletId = -1;
        while (localRealm.where(ContactsModel.class).equalTo("id", walletId).findFirst() != null) {
            walletId--;
        }
        return walletId;
    }


    private void getPrivacyTerms() {
        mUsersContacts.getPrivacyTerms().subscribe(statusResponse -> {
            if (statusResponse.isSuccess()) {
                privacyActivity.showPrivcay(statusResponse.getMessage());
            } else {
                AppHelper.LogCat(" " + statusResponse.getMessage());
            }

        }, throwable -> {
            AppHelper.LogCat(" " + throwable.getMessage());
        });
    }
}
