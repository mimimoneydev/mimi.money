package com.money.mimi.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.models.users.contacts.ContactsModel;

import io.realm.Realm;
import io.realm.RealmResults;

public class PermissionHandler {


    public static boolean checkPermission(Activity activity, String permission) {
        if (AppHelper.isAndroid6()) {
            // System pickers and app-specific storage do not require broad media access.
            if (isLegacyStoragePermission(permission) && android.os.Build.VERSION.SDK_INT > 28) return true;
            int result = ContextCompat.checkSelfPermission(activity, permission);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public static boolean isLegacyStoragePermission(String permission) {
        return Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission);
    }

    public static void requestPermission(Activity mActivity, String permission) {

        if (isLegacyStoragePermission(permission) && android.os.Build.VERSION.SDK_INT > 28) {
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) {
            String title = null;
            String Message = null;
            switch (permission) {
                case Manifest.permission.CAMERA:
                    title = mActivity.getString(R.string.camera_permission);
                    Message = mActivity.getString(R.string.camera_permission_message);
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    title = mActivity.getString(R.string.audio_permission);
                    Message = mActivity.getString(R.string.record_audio_permission_message);
                    break;

                case Manifest.permission.MODIFY_AUDIO_SETTINGS:
                    title = mActivity.getString(R.string.camera_permission);
                    Message = mActivity.getString(R.string.settings_audio_permission_message);
                    break;
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    title = mActivity.getString(R.string.storage_permission);
                    Message = mActivity.getString(R.string.write_storage_permission_message);
                    break;
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                    title = mActivity.getString(R.string.storage_permission);
                    Message = mActivity.getString(R.string.read_storage_permission_message);
                    break;
                case Manifest.permission.READ_CONTACTS:
                    title = mActivity.getString(R.string.contacts_permission);
                    Message = mActivity.getString(R.string.read_contacts_permission_message);
                    break;
                case Manifest.permission.WRITE_CONTACTS:
                    title = mActivity.getString(R.string.contacts_permission);
                    Message = mActivity.getString(R.string.write_contacts_permission_message);
                    break;

                case Manifest.permission.RECEIVE_SMS:
                    title = mActivity.getString(R.string.receive_sms_permission);
                    Message = mActivity.getString(R.string.receive_sms_permission_message);
                    break;

                case Manifest.permission.READ_SMS:
                    title = mActivity.getString(R.string.read_sms_permission);
                    Message = mActivity.getString(R.string.read_sms_permission_message);
                    break;
                case Manifest.permission.CALL_PHONE:
                    title = mActivity.getString(R.string.call_phone_permission);
                    Message = mActivity.getString(R.string.call_phone_permission_message);
                    break;
                case Manifest.permission.GET_ACCOUNTS:
                    title = mActivity.getString(R.string.get_accounts_permission);
                    Message = mActivity.getString(R.string.get_accounts_permission_message);
                    break;

            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(title);
            builder.setMessage(Message);
            builder.setPositiveButton(mActivity.getString(R.string.yes), (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
                intent.setData(uri);
                if (permission.equals(Manifest.permission.READ_CONTACTS)) {
                    mActivity.startActivityForResult(intent, AppConstants.CONTACTS_PERMISSION_REQUEST_CODE);
                } else {
                    mActivity.startActivityForResult(intent, AppConstants.PERMISSION_REQUEST_CODE);
                }
            });
            builder.setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                if (permission.equals(Manifest.permission.READ_CONTACTS)) {
                    Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                    realm.executeTransactionAsync(realm1 -> {
                        RealmResults<ContactsModel> contactsModel = realm1.where(ContactsModel.class).findAll();
                        if (contactsModel.size() != 0) {
                            contactsModel.deleteAllFromRealm();
                        }
                    }, dialog::dismiss, AppHelper::LogCat);
                } else {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else {
            if (permission.equals(Manifest.permission.READ_CONTACTS)) {
                ActivityCompat.requestPermissions(mActivity, new String[]{permission}, AppConstants.CONTACTS_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(mActivity, new String[]{permission}, AppConstants.PERMISSION_REQUEST_CODE);
            }
        }
    }
}
