package com.money.mimi.services.firebase;

import android.content.Intent;
import androidx.core.app.JobIntentService;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.money.mimi.api.APIHelper;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;

public class RegistrationIntentService extends JobIntentService {

    public static final String EXTRA_FCM_TOKEN = "extra_fcm_token";
    private static final int JOB_ID = 1001;

    public static void enqueueWork(android.content.Context context, Intent work) {
        enqueueWork(context, RegistrationIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        try {
            String token = intent == null ? null : intent.getStringExtra(EXTRA_FCM_TOKEN);
            if (token == null) {
                token = Tasks.await(FirebaseMessaging.getInstance().getToken());
            }
            if (token == null) {
                AppHelper.LogCat("FCM token is null, cannot register");
                return;
            }
            AppHelper.LogCat("FCM token obtained: " + token.substring(0, Math.min(20, token.length())) + "...");
            NoticeRegistrationManager.register(this, token);
            sendTokenToServer(token);
        } catch (Exception e) {
            AppHelper.LogCat("RegistrationIntentService error: " + e.getMessage());
        }
    }

    private void sendTokenToServer(String token) {
        if (PreferenceManager.getToken(this) == null) {
            AppHelper.LogCat("User not logged in, skipping FCM token upload");
            return;
        }
        try {
            APIHelper.initialApiUsersContacts().updateFcmToken(token).subscribe(response -> {
                if (response.isSuccess()) {
                    AppHelper.LogCat("FCM token updated on server successfully");
                } else {
                    AppHelper.LogCat("FCM token update failed: " + response.getMessage());
                }
            }, throwable -> {
                AppHelper.LogCat("FCM token update error: " + throwable.getMessage());
            });
        } catch (Exception e) {
            AppHelper.LogCat("FCM token upload exception: " + e.getMessage());
        }
    }
}
