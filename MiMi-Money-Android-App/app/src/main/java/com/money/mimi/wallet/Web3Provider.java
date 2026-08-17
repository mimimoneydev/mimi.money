package com.money.mimi.wallet;

import android.content.Context;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public class Web3Provider {
    private static volatile Web3j web3j;
    private static volatile String activeRpcUrl;

    public static Web3j get(Context context) {
        final String rpcUrl = WalletConfig.getRpcUrl(context);
        if (web3j == null || activeRpcUrl == null || !activeRpcUrl.equals(rpcUrl)) {
            synchronized (Web3Provider.class) {
                if (web3j == null || activeRpcUrl == null || !activeRpcUrl.equals(rpcUrl)) {
                    if (web3j != null) {
                        try {
                            web3j.shutdown();
                        } catch (Exception ignore) {
                        }
                    }
                    web3j = Web3j.build(new HttpService(rpcUrl));
                    activeRpcUrl = rpcUrl;
                }
            }
        }
        return web3j;
    }

    @Deprecated
    public static Web3j get() {
        return get(null);
    }

    public static void reset() {
        synchronized (Web3Provider.class) {
            if (web3j != null) {
                try {
                    web3j.shutdown();
                } catch (Exception ignore) {
                }
            }
            web3j = null;
            activeRpcUrl = null;
        }
    }
}

