package com.money.mimi.api;

import com.money.mimi.api.apiServices.AuthService;
import com.money.mimi.api.apiServices.ConversationsService;
import com.money.mimi.api.apiServices.GroupsService;
import com.money.mimi.api.apiServices.MessagesService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.WhatsCloneApplication;

/**
 * Created by Abderrahim El imame on 4/11/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class APIHelper {

    public static UsersService initialApiUsersContacts() {
        APIService mApiService = APIService.with(WhatsCloneApplication.getInstance());
        return new UsersService(WhatsCloneApplication.getRealmDatabaseInstance(), WhatsCloneApplication.getInstance(), mApiService);
    }


    public static GroupsService initializeApiGroups() {
        APIService mApiService = APIService.with(WhatsCloneApplication.getInstance());
        return new GroupsService(WhatsCloneApplication.getRealmDatabaseInstance(), WhatsCloneApplication.getInstance(), mApiService);
    }

    public static ConversationsService initializeConversationsService() {
        return new ConversationsService(WhatsCloneApplication.getRealmDatabaseInstance());
    }

    public static MessagesService initializeMessagesService() {
        return new MessagesService(WhatsCloneApplication.getRealmDatabaseInstance());
    }

    public static AuthService initializeAuthService() {
        APIService mApiService = APIService.with(WhatsCloneApplication.getInstance());
        return new AuthService(WhatsCloneApplication.getInstance(), mApiService);
    }
}
