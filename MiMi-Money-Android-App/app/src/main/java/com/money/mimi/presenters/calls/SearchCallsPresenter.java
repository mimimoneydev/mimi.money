package com.money.mimi.presenters.calls;


import com.money.mimi.activities.search.SearchCallsActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.interfaces.Presenter;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SearchCallsPresenter implements Presenter {
    private SearchCallsActivity mSearchCallsActivity;
    private Realm realm;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private UsersService mUsersContacts;


    public SearchCallsPresenter(SearchCallsActivity mSearchCallsActivity) {
        this.mSearchCallsActivity = mSearchCallsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        APIService mApiService = APIService.with(this.mSearchCallsActivity);
        mUsersContacts = new UsersService(realm, this.mSearchCallsActivity, mApiService);
        loadLocalData();
    }

    private void loadLocalData() {
        disposables.add(mUsersContacts.getAllCalls().subscribe(mSearchCallsActivity::ShowCalls, mSearchCallsActivity::onErrorLoading));
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