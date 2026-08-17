package com.money.mimi.presenters.users;


import com.money.mimi.activities.BlockedContactsActivity;
import com.money.mimi.activities.messages.TransferMessageContactsActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.interfaces.Presenter;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SelectContactsPresenter implements Presenter {
    private TransferMessageContactsActivity transferMessageContactsActivity;
    private BlockedContactsActivity blockedContactsActivity;
    private Realm realm;
    private final CompositeDisposable disposables = new CompositeDisposable();


    public SelectContactsPresenter(TransferMessageContactsActivity transferMessageContactsActivity) {
        this.transferMessageContactsActivity = transferMessageContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    public SelectContactsPresenter(BlockedContactsActivity blockedContactsActivity) {
        this.blockedContactsActivity = blockedContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {

        if (transferMessageContactsActivity != null) {
            APIService mApiService = APIService.with(this.transferMessageContactsActivity);
            UsersService mUsersContacts = new UsersService(realm, this.transferMessageContactsActivity, mApiService);
            disposables.add(mUsersContacts.getLinkedContacts().subscribe(transferMessageContactsActivity::ShowContacts, throwable -> {
                AppHelper.LogCat("Error contacts selector " + throwable.getMessage());
            }));
        } else {
            APIService mApiService = APIService.with(this.blockedContactsActivity);
            UsersService mUsersContacts = new UsersService(realm, this.blockedContactsActivity, mApiService);
            disposables.add(mUsersContacts.getBlockedContacts().subscribe(blockedContactsActivity::ShowContacts, throwable -> {
                AppHelper.LogCat("Error contacts selector " + throwable.getMessage());
            }));
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
}