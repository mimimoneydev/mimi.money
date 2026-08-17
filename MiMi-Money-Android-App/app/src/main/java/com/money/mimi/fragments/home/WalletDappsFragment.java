package com.money.mimi.fragments.home;

import androidx.annotation.Nullable;

public class WalletDappsFragment extends WalletDappWebFragment {

    public static WalletDappsFragment newInstance(@Nullable String url) {
        WalletDappsFragment fragment = new WalletDappsFragment();
        putUrlArgs(fragment, url);
        return fragment;
    }

    public static WalletDappsFragment newWalletConnectInstance(String walletConnectUri) {
        WalletDappsFragment fragment = new WalletDappsFragment();
        putWalletConnectArgs(fragment, walletConnectUri);
        return fragment;
    }

    public static WalletDappsFragment newNotificationsInstance() {
        WalletDappsFragment fragment = new WalletDappsFragment();
        putNotificationsArgs(fragment);
        return fragment;
    }
}
