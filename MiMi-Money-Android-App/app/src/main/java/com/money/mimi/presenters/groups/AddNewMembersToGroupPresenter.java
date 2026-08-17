package com.money.mimi.presenters.groups;

import com.money.mimi.activities.groups.AddNewMembersToGroupActivity;
import com.money.mimi.api.APIService;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.interfaces.Presenter;
import com.money.mimi.api.apiServices.UsersService;

import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 26/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class AddNewMembersToGroupPresenter implements Presenter {
    private  AddNewMembersToGroupActivity view;
    private  Realm realm;


    public AddNewMembersToGroupPresenter(AddNewMembersToGroupActivity addMembersToGroupActivity) {
        this.view = addMembersToGroupActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        APIService mApiService = APIService.with(view);
        UsersService mUsersContacts = new UsersService(realm, view, mApiService);
        mUsersContacts.getLinkedContacts().subscribe(view::ShowContacts, throwable -> AppHelper.LogCat("AddNewMembersToGroupPresenter "+throwable.getMessage()));
    }


    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {
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