package com.money.mimi.wallet;

import com.money.mimi.R;

import java.util.Locale;

public final class WalletLogoResolver {
    private WalletLogoResolver() {
    }

    public static int getNetworkLogoRes(String key) {
        if (key == null) return R.drawable.ic_network_generic;
        String normalized = key.toLowerCase(Locale.US);
        if ("ethereum".equals(normalized)) return R.drawable.ic_network_ethereum;
        if ("bnb".equals(normalized)) return R.drawable.ic_network_bnb;
        if ("base".equals(normalized)) return R.drawable.ic_network_base;
        if ("polygon".equals(normalized)) return R.drawable.ic_network_polygon;
        if ("arbitrum".equals(normalized)) return R.drawable.ic_network_arbitrum;
        if ("avalanche".equals(normalized)) return R.drawable.ic_network_avalanche;
        if ("celo".equals(normalized)) return R.drawable.ic_network_celo;
        return R.drawable.ic_network_generic;
    }
}
