package com.money.mimi.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.money.mimi.app.AppConstants;

import java.util.ArrayList;
import java.util.List;

public final class StartupPermissions {

    private static final String PREFS_NAME = "startup_permissions";
    private static final String KEY_REQUESTED = "requested";
    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    private StartupPermissions() {
    }

    public static void requestRequiredPermissions(Activity activity) {
        requestRequiredPermissions(activity, false);
    }

    public static void requestRequiredPermissions(Activity activity, boolean forceRetry) {
        if (activity == null || !AppHelper.isAndroid6()) {
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!forceRetry && prefs.getBoolean(KEY_REQUESTED, false)) {
            return;
        }

        List<String> permissions = getMissingRuntimePermissions(activity);
        if (!permissions.isEmpty()) {
            prefs.edit().putBoolean(KEY_REQUESTED, true).apply();
            ActivityCompat.requestPermissions(
                    activity,
                    permissions.toArray(new String[0]),
                    AppConstants.PERMISSION_REQUEST_CODE
            );
            return;
        } else {
            prefs.edit().putBoolean(KEY_REQUESTED, true).apply();
        }
    }

    public static boolean hasContactsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static List<String> getMissingRuntimePermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        // Sensitive feature permissions are requested only at the point of use.
        // Asking for camera, microphone, location and contacts on the welcome
        // screen is unnecessary, harms trust, and grants broader access than the
        // current screen needs.
        if (Build.VERSION.SDK_INT >= 33) {
            addIfMissing(activity, permissions, POST_NOTIFICATIONS);
        }

        return permissions;
    }

    private static void addIfMissing(Activity activity, List<String> permissions, String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

}
