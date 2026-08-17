package com.money.mimi.presenters.users;


import com.money.mimi.activities.settings.SettingsActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.interfaces.Presenter;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Abderrahim El imame on 20/02/2016. Email : abderrahim.elimame@gmail.com
 */
public class SettingsPresenter implements Presenter {
    private final SettingsActivity view;
    private final Realm realm;
    private UsersService mUsersContacts;

    private final CompositeDisposable disposables = new CompositeDisposable();


    public SettingsPresenter(SettingsActivity settingsActivity) {
        this.view = settingsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void
    onCreate() {
        APIService mApiService = APIService.with(view);
        mUsersContacts = new UsersService(realm, view, mApiService);
        loadData();
    }

    public void loadData() {
        disposables.add(mUsersContacts.getContactInfo(PreferenceManager.getID(view)).subscribe(view::ShowContact, throwable -> {
            AppHelper.LogCat(throwable.getMessage());
        }));
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