package com.money.mimi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.ForegroundRuning;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.services.MainService;
import com.money.mimi.services.sync.BackgroundSyncScheduler;

/**
 * Lightweight connectivity signal. Server validation belongs to foreground UI flows and
 * durable synchronization belongs to WorkManager, never to a short-lived receiver callback.
 */
public class NetworkChangeListener extends BroadcastReceiver {
    public static NetworkListener networkListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }
        Context appContext = context.getApplicationContext();
        boolean connected = isConnected(appContext);
        if (networkListener != null) {
            networkListener.onNetworkConnectionChanged(connected, connected);
        }
        if (!connected) {
            appContext.stopService(new Intent(appContext, MainService.class));
            return;
        }
        if (PreferenceManager.getToken(appContext) == null) return;

        BackgroundSyncScheduler.enqueue(appContext);
        try {
            if (ForegroundRuning.get().isForeground()) {
                AppHelper.startMainService(appContext);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isConnected(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = manager.getActiveNetwork();
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
