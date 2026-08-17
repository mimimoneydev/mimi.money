package com.money.mimi.presenters.users;


import com.money.mimi.activities.search.SearchContactsActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.api.apiServices.UsersService;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SearchContactsPresenter implements Presenter {
    private SearchContactsActivity mSearchContactsActivity;
    private Realm realm;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private UsersService mUsersContacts;


    public SearchContactsPresenter(SearchContactsActivity mSearchContactsActivity) {
        this.mSearchContactsActivity = mSearchContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        APIService mApiService = APIService.with(this.mSearchContactsActivity);
        mUsersContacts = new UsersService(realm, this.mSearchContactsActivity, mApiService);
        loadLocalData();
    }

    private void loadLocalData() {
        disposables.add(mUsersContacts.getAllContacts().subscribe(mSearchContactsActivity::ShowContacts, mSearchContactsActivity::onErrorLoading));
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