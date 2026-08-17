package com.money.mimi.presenters.messages;


import com.money.mimi.activities.search.SearchConversationsActivity;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.api.apiServices.ConversationsService;

import io.realm.Realm;
import io.reactivex.disposables.CompositeDisposable;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SearchConversationsPresenter implements Presenter {
    private SearchConversationsActivity mSearchConversationsActivity;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private Realm realm;


    public SearchConversationsPresenter(SearchConversationsActivity mSearchConversationsActivity) {
        this.mSearchConversationsActivity = mSearchConversationsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        ConversationsService mConversationsService = new ConversationsService(realm);
        disposables.add(mConversationsService.getConversations().subscribe(mSearchConversationsActivity::ShowConversation, mSearchConversationsActivity::onErrorLoading));
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