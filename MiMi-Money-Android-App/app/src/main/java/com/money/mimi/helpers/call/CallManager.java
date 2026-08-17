package com.money.mimi.helpers.call;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

import com.money.mimi.R;
import com.money.mimi.activities.call.CallActivity;
import com.money.mimi.activities.call.CallAlertActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.models.users.contacts.ContactsModel;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.socket.client.Ack;
import io.socket.client.Socket;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Abderrahim El imame on 12/21/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CallManager {
    private static final long CALL_PING_TIMEOUT_MS = 6000;


    /**
     * method to call a user
     */
    public static void callContact(Activity mActivity, boolean isNeedFinish, boolean isVideoCall, int userID) {

        if (isVideoCall) {
            if (PermissionHandler.checkPermission(mActivity, Manifest.permission.CAMERA)) {

            } else {
                AppHelper.LogCat("Please request camera  permission.");
                PermissionHandler.requestPermission(mActivity, Manifest.permission.CAMERA);
                return;
            }

            if (PermissionHandler.checkPermission(mActivity, Manifest.permission.RECORD_AUDIO)) {

            } else {
                AppHelper.LogCat("Please request Record audio permission.");
                PermissionHandler.requestPermission(mActivity, Manifest.permission.RECORD_AUDIO);
                return;
            }
        } else {

            if (PermissionHandler.checkPermission(mActivity, Manifest.permission.RECORD_AUDIO)) {


            } else {
                AppHelper.LogCat("Please request Record audio permission.");
                PermissionHandler.requestPermission(mActivity, Manifest.permission.RECORD_AUDIO);
                return;
            }
        }


        if (!isNetworkAvailable(mActivity)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
            alert.setMessage(mActivity.getString(R.string.you_couldnt_call_this_user_network));
            alert.setPositiveButton(R.string.ok, (dialog, which) -> {
            });
            alert.setCancelable(false);
            alert.show();
        } else {
            WhatsCloneApplication app = (WhatsCloneApplication) mActivity.getApplication();
            Socket mSocket;
            mSocket = app.getSocket();
            Realm cacheRealm = WhatsCloneApplication.getRealmDatabaseInstance();
            ContactsModel cachedContact = cacheRealm.where(ContactsModel.class).equalTo("id", userID).findFirst();
            String cachedSocketId = cachedContact != null ? cachedContact.getSocketId() : null;
            if (!cacheRealm.isClosed()) {
                cacheRealm.close();
            }
            if (mSocket != null && mSocket.connected()) {
                if (isUsableSocketId(cachedSocketId)) {
                    makeCall(isNeedFinish, mActivity, cachedSocketId, isVideoCall, userID);
                    return;
                }
                if (mSocket.connected()) {
                    JSONObject data = new JSONObject();
                    try {
                        data.put("recipientId", userID);
                        data.put("senderId", PreferenceManager.getID(mActivity));
                        AppHelper.LogCat("socket not null");
                        AtomicBoolean callHandled = new AtomicBoolean(false);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.postDelayed(() -> {
                            if (callHandled.compareAndSet(false, true)) {
                                AppHelper.LogCat("Call ping timed out for user " + userID);
                                showCallUnavailable(mActivity);
                            }
                        }, CALL_PING_TIMEOUT_MS);
                        mSocket.emit(AppConstants.SOCKET_CALL_USER_PING, data, (Ack) argObjects -> {
                            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                            try {
                                if (!callHandled.compareAndSet(false, true)) {
                                    return;
                                }
                                if (argObjects == null || argObjects.length == 0 || !(argObjects[0] instanceof JSONObject)) {
                                    showCallUnavailable(mActivity);
                                    return;
                                }
                                JSONObject dataString = (JSONObject) argObjects[0];
                                boolean connected = dataString.getBoolean("connected");
                                String socketId = dataString.optString("socketId", null);
                                ContactsModel existingContact = realm.where(ContactsModel.class).equalTo("id", userID).findFirst();
                                String latestCachedSocketId = existingContact != null ? existingContact.getSocketId() : null;
                                if (connected || isUsableSocketId(socketId) || isUsableSocketId(latestCachedSocketId)) {
                                    String resolvedSocketId = isUsableSocketId(socketId) ? socketId : latestCachedSocketId;
                                    AppHelper.LogCat("User  connected and ready to call him connecteddd  " + socketId);
                                    realm.executeTransactionAsync(realm1 -> {
                                        ContactsModel contactsModel1 = realm1.where(ContactsModel.class).equalTo("id", userID).findFirst();
                                        if (contactsModel1 != null) {
                                            contactsModel1.setSocketId(resolvedSocketId);
                                            realm1.copyToRealmOrUpdate(contactsModel1);
                                        }
                                    });
                                    makeCall(isNeedFinish, mActivity, resolvedSocketId, isVideoCall, userID);
                                } else {
                                    AppHelper.LogCat("User  not connected and not ready to call him mess 2" + socketId);
                                    realm.executeTransactionAsync(realm1 -> {
                                        ContactsModel contactsModel1 = realm1.where(ContactsModel.class).equalTo("id", userID).findFirst();
                                        if (contactsModel1 != null) {
                                            contactsModel1.setSocketId(null);
                                            realm1.copyToRealmOrUpdate(contactsModel1);
                                        }
                                    });
                                    showCallUnavailable(mActivity);
                                }
                            } catch (JSONException e) {
                                showCallUnavailable(mActivity);
                            } catch (Exception e) {
                                AppHelper.LogCat("Call ping exception " + e.getMessage());
                                showCallUnavailable(mActivity);
                            } finally {
                                if (!realm.isClosed())
                                    realm.close();
                            }
                        });
                    } catch (Exception e) {
                        AppHelper.LogCat("Exception" + e.getMessage());
                        showCallUnavailable(mActivity);
                    }
                } else {

                    showCallUnavailable(mActivity);
                }
            } else {
                AppHelper.startMainService(mActivity);
                showCallUnavailable(mActivity);
            }

        }
    }


    private static boolean isUsableSocketId(String socketId) {
        return socketId != null && !socketId.trim().isEmpty() && !"null".equalsIgnoreCase(socketId);
    }

    private static void showCallUnavailable(Activity mActivity) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        mActivity.runOnUiThread(() -> {
            if (mActivity.isFinishing()) {
                return;
            }
            Intent mIntent = new Intent(mActivity, CallAlertActivity.class);
            mActivity.startActivity(mIntent);
            AnimationsUtil.setSlideInAnimation(mActivity);
        });
    }


    private static void makeCall(boolean isNeedFinish, Activity mActivity, String callerSocketId, boolean isVideoCall, int userID) {
        if (!isUsableSocketId(callerSocketId) || callerSocketId.equals(PreferenceManager.getSocketID(mActivity))) {
            showCallUnavailable(mActivity);
            return;
        }
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        ContactsModel contactsModelCaller = realm.where(ContactsModel.class).equalTo("id", PreferenceManager.getID(mActivity)).findFirst();
        ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("id", userID).findFirst();
        if (contactsModelCaller == null || contactsModel == null) {
            realm.close();
            Intent mIntent = new Intent(mActivity, CallAlertActivity.class);
            mActivity.startActivity(mIntent);
            AnimationsUtil.setSlideInAnimation(mActivity);
            return;
        }
        String recipientPhone = contactsModel.getWalletAddress();

        String callerImage = null;
        if (contactsModel.getImage() != null)
            callerImage = contactsModel.getImage();

        String userImage = null;
        if (contactsModelCaller.getImage() != null)
            userImage = contactsModelCaller.getImage();

        Intent intent = new Intent(mActivity, CallActivity.class);
        intent.putExtra(AppConstants.USER_SOCKET_ID, PreferenceManager.getSocketID(mActivity));
        intent.putExtra(AppConstants.USER_PHONE, PreferenceManager.getWalletAddress(mActivity));
        intent.putExtra(AppConstants.CALLER_SOCKET_ID, callerSocketId);
        intent.putExtra(AppConstants.CALLER_PHONE, recipientPhone);
        intent.putExtra(AppConstants.CALLER_IMAGE, callerImage);
        intent.putExtra(AppConstants.USER_IMAGE, userImage);
        intent.putExtra(AppConstants.IS_ACCEPTED_CALL, false);
        intent.putExtra(AppConstants.IS_VIDEO_CALL, isVideoCall);
        intent.putExtra(AppConstants.CALLER_ID, userID);
        mActivity.startActivity(intent);
        if (isNeedFinish)
            mActivity.finish();
        AnimationsUtil.setSlideInAnimation(mActivity);
        realm.close();
    }

    private static boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager cm = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
