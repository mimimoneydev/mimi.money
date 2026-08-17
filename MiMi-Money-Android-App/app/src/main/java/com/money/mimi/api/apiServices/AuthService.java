package com.money.mimi.api.apiServices;

import android.content.Context;

import com.money.mimi.api.APIAuthentication;
import com.money.mimi.api.APIService;
import com.money.mimi.app.EndPoints;
import com.money.mimi.models.auth.JoinModelResponse;
import com.money.mimi.models.auth.LoginModel;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Abderrahim El imame on 10/4/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class AuthService {

    private APIAuthentication apiAuthentication;
    private Context mContext;
    private APIService mApiService;


    public AuthService(Context context, APIService mApiService) {
        this.mContext = context;
        this.mApiService = mApiService;
    }

    /**
     * method to initialize the api auth
     *
     * @return return value
     */
    private APIAuthentication initializeApiAuth() {
        if (apiAuthentication == null) {
            apiAuthentication = this.mApiService.AuthService(APIAuthentication.class, EndPoints.BACKEND_BASE_URL);
        }
        return apiAuthentication;
    }

    public Observable<JoinModelResponse> join(LoginModel loginModel) {
        return initializeApiAuth().join(loginModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<JoinModelResponse> resend(String phone) {
        return initializeApiAuth().resend(phone)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<JoinModelResponse> verifyUser(String code) {
        return initializeApiAuth().verifyUser(code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
