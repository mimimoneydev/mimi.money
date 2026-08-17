package com.money.mimi.services.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessaging;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NoticeRegistrationManager {
    private static final String BASE_URL = "https://notice.mimi.money";
    private static final String REGISTRATION_URL = BASE_URL + "/user/register";
    private static final String APP_TYPE = "ADVANCE_PUSH";
    private static final String PREF_NAME = "GCM";
    private static final String KEY_TOKEN = "gcm_id";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private NoticeRegistrationManager() {
    }

    public static void register(Context context, String token) {
        if (context == null || TextUtils.isEmpty(token)) {
            AppHelper.LogCat("Notice registration skipped: missing context or FCM token");
            return;
        }
        Context appContext = context.getApplicationContext();
        saveToken(appContext, token);
        subscribeTopics();

        RequestBody body = new FormBody.Builder()
                .add("email", registrationEmail(appContext))
                .add("app_type", APP_TYPE)
                .add("gcm_id", token)
                .add("device_model", safe(Build.MODEL))
                .add("device_api", String.valueOf(Build.VERSION.SDK_INT))
                .add("device_os", deviceOsName())
                .add("device_name", deviceName())
                .add("timezone", TimeZone.getDefault().getID())
                .add("last_lat", "0")
                .add("last_long", "0")
                .add("device_memory", deviceMemory())
                .add("device_id", safe(Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID)))
                .add("pin_code", "")
                .build();

        Request request = new Request.Builder()
                .url(REGISTRATION_URL)
                .post(body)
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppHelper.LogCat("Notice registration failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String responseBody = response.body() == null ? "" : response.body().string();
                    if (response.isSuccessful()) {
                        AppHelper.LogCat("Notice registration success: " + response.code() + " " + responseBody);
                    } else {
                        AppHelper.LogCat("Notice registration error: " + response.code() + " " + responseBody);
                    }
                } catch (Exception e) {
                    AppHelper.LogCat("Notice registration response error: " + e.getMessage());
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    public static String getSavedToken(Context context) {
        if (context == null) {
            return "";
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    private static void saveToken(Context context, String token) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply();
    }

    private static void subscribeTopics() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("global");
            FirebaseMessaging.getInstance().subscribeToTopic(APP_TYPE);
        } catch (Exception e) {
            AppHelper.LogCat("Notice topic subscription failed: " + e.getMessage());
        }
    }

    private static String registrationEmail(Context context) {
        String mobile = PreferenceManager.getMobileNumber(context);
        if (!TextUtils.isEmpty(mobile)) {
            return mobile;
        }
        String walletAddress = PreferenceManager.getWalletAddress(context);
        if (!TextUtils.isEmpty(walletAddress)) {
            return walletAddress;
        }
        int userId = PreferenceManager.getID(context);
        if (userId > 0) {
            return String.valueOf(userId);
        }
        return "";
    }

    private static String deviceName() {
        String manufacturer = safe(Build.MANUFACTURER);
        String model = safe(Build.MODEL);
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String deviceOsName() {
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            try {
                if (field.getInt(null) == Build.VERSION.SDK_INT) {
                    return field.getName();
                }
            } catch (Exception ignored) {
            }
        }
        return "UNSPECIFIED";
    }

    private static String deviceMemory() {
        RandomAccessFile reader = null;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            String load = reader.readLine();
            if (load == null) {
                return "";
            }
            String value = load.replaceAll("\\D+", "");
            if (TextUtils.isEmpty(value)) {
                return "";
            }
            double kb = Double.parseDouble(value);
            DecimalFormat format = new DecimalFormat("#.##");
            double mb = kb / 1024.0;
            double gb = kb / 1048576.0;
            if (gb > 1) {
                return format.format(gb) + " GB";
            }
            if (mb > 1) {
                return format.format(mb) + " MB";
            }
            return format.format(kb) + " KB";
        } catch (Exception ignored) {
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String capitalize(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
