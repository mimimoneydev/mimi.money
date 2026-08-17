package com.money.mimi.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import androidx.appcompat.app.AppCompatDelegate;

import com.orhanobut.logger.Logger;
import com.money.mimi.BuildConfig;
import com.money.mimi.R;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.ForegroundRuning;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.SystemBarInsets;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.receivers.NetworkChangeListener;
import com.money.mimi.services.MainService;
import com.money.mimi.services.sync.BackgroundSyncScheduler;
import com.money.mimi.telemetry.AppTelemetry;
import com.money.mimi.wallet.MimiWalletConnect;

import java.net.URISyntaxException;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class WhatsCloneApplication extends Application {

    static WhatsCloneApplication mInstance;
    public static final long TIMEOUT = 60 * 1000;
    private static Socket mSocket = null;
    private static String mCurrentServerUrl = null;
    private final Handler lifecycleHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopForegroundSocket = () ->
            stopService(new android.content.Intent(this, MainService.class));

    public static synchronized void connectSocket() {
        disposeSocketLocked();
        Context ctx = getInstance();
        String preferredUrl = ctx != null ? PreferenceManager.getSocketServerUrl(ctx) : null;
        String url = EndPoints.BACKEND_CHAT_SERVER_URL;
        if (preferredUrl != null) {
            url = preferredUrl;
        }
        mCurrentServerUrl = url;
        mSocket = createSocket(url);
    }

    public static synchronized void connectSocketWithUrl(String serverUrl) {
        disposeSocketLocked();
        mCurrentServerUrl = serverUrl;
        mSocket = createSocket(serverUrl);
    }

    private static void disposeSocketLocked() {
        if (mSocket == null) return;
        try {
            mSocket.off();
            mSocket.disconnect();
            mSocket.close();
        } catch (RuntimeException ignored) {
        }
        mSocket = null;
    }

    public static Socket createSocket(String serverUrl) {
        IO.Options options = new IO.Options();
        options.forceNew = true;
        options.timeout = TIMEOUT;
        options.reconnection = false;
        options.query = "token=" + AppConstants.APP_KEY_SECRET;

        try {
            return IO.socket(serverUrl, options);
        } catch (URISyntaxException e) {
            AppHelper.LogCat("URISyntaxException " + e.getMessage());
            return null;
        }
    }

    public static String getPrimaryServerUrl() {
        Context ctx = getInstance();
        String preferred = ctx != null ? PreferenceManager.getSocketServerUrl(ctx) : null;
        return preferred != null ? preferred : EndPoints.BACKEND_CHAT_SERVER_URL;
    }

    public static String getFallbackServerUrl(String currentUrl) {
        if (currentUrl.equals(EndPoints.BACKEND_CHAT_SERVER_URL)) {
            return EndPoints.BACKEND_CHAT_SERVER_FALLBACK_URL;
        }
        return EndPoints.BACKEND_CHAT_SERVER_URL;
    }

    public static String getCurrentServerUrl() {
        return mCurrentServerUrl;
    }

    public static void saveCurrentServerUrl() {
        Context ctx = getInstance();
        if (ctx != null && mCurrentServerUrl != null) {
            PreferenceManager.setSocketServerUrl(ctx, mCurrentServerUrl);
        }
    }

    public static void resetSocketPreference() {
        mCurrentServerUrl = null;
        Context ctx = getInstance();
        if (ctx != null) {
            PreferenceManager.setSocketServerUrl(ctx, null);
        }
    }


    public Socket getSocket() {
        return mSocket;
    }

    public static synchronized WhatsCloneApplication getInstance() {
        return mInstance;
    }

    public void setmInstance(WhatsCloneApplication mInstance) {
        WhatsCloneApplication.mInstance = mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setmInstance(this);
        AppTelemetry.initialize(this);
        AppTelemetry.OperationTrace startupTrace = AppTelemetry.startTrace("app_initialization");
        SystemBarInsets.install(this);

        initRealm();
        configureForegroundLifecycle(ForegroundRuning.init(this));
        if (AppConstants.DEBUGGING_MODE)
            Logger.init(AppConstants.TAG).hideThreadInfo();

        if (!PreferenceManager.getLanguage(this).equals(""))
            setDefaultLocale(this, new Locale(PreferenceManager.getLanguage(this)));
        else {
            if (Locale.getDefault().toString().startsWith("en_")) {
                PreferenceManager.setLanguage(this, "en");
            }
        }
        PreferenceManager.migrateLegacyWalletSecrets(this);
        MimiWalletConnect.initialize(this);
        // Apply saved dark theme preference
        if (PreferenceManager.isDarkThemeEnabled(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        AppHelper.LogCat("Realm.getGlobalInstanceCount " + Realm.getGlobalInstanceCount(getRealmDatabaseConfiguration()));
        startupTrace.stop();
    }

    private void configureForegroundLifecycle(ForegroundRuning foreground) {
        foreground.addListener(new ForegroundRuning.Listener() {
            @Override
            public void onBecameForeground() {
                lifecycleHandler.removeCallbacks(stopForegroundSocket);
                AppHelper.startMainService(WhatsCloneApplication.this);
                BackgroundSyncScheduler.enqueue(WhatsCloneApplication.this);
            }

            @Override
            public void onBecameBackground() {
                // Allow activity transitions and brief task switching without socket churn.
                lifecycleHandler.removeCallbacks(stopForegroundSocket);
                lifecycleHandler.postDelayed(stopForegroundSocket, 15_000L);
            }
        });
    }


    @SuppressWarnings("deprecation")
    protected void setDefaultLocale(Context context, Locale locale) {

        Locale.setDefault(locale);
        Configuration appConfig = new Configuration();
        appConfig.locale = locale;
        context.getResources().updateConfiguration(appConfig, context.getResources().getDisplayMetrics());

    }

    public void setConnectivityListener(NetworkListener listener) {
        NetworkChangeListener.networkListener = listener;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // MainService.disconnectSocket();
        if (!getRealmDatabaseInstance().isClosed()) {
            getRealmDatabaseInstance().close();
        }
    }

    public static RealmConfiguration getRealmDatabaseConfiguration() {
        return new RealmConfiguration.Builder().name(getInstance().getString(R.string.app_name) + PreferenceManager.getToken(getInstance()) + ".realm").deleteRealmIfMigrationNeeded().allowWritesOnUiThread(true).build();
        // return new RealmConfiguration.Builder().name(getInstance().getString(R.string.app_name) + PreferenceManager.getToken(getInstance()) + ".realm").schemaVersion(2).migration(new RealmMigrations()).build();
    }

    public static Realm getRealmDatabaseInstance() {
        return Realm.getInstance(getRealmDatabaseConfiguration());
    }

    public static boolean DeleteRealmDatabaseInstance() {
        return Realm.deleteRealm(getRealmDatabaseConfiguration());
    }

    public void initRealm() {
        Realm.init(this);
    }

}
