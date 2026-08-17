package com.money.mimi.wallet;

import android.content.Context;

import com.money.mimi.helpers.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WalletConfig {
    public static final String DEFAULT_NETWORK_KEY = "ethereum";
    private static final int MAX_CUSTOM_NETWORKS = 50;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_SYMBOL_LENGTH = 16;
    private static final int MAX_URL_LENGTH = 512;
    private static final Map<String, NetworkDefinition[]> BUILT_IN_NETWORKS = new LinkedHashMap<>();

    static {
        // Add built-in networks here. Custom networks/explorers can also be stored at runtime via
        // PreferenceManager.setWalletCustomNetworksJson(...).
        registerBuiltIn(
                new NetworkDefinition("ethereum", "Ethereum", 1L, "ETH", "https://ethereum-rpc.publicnode.com", "https://etherscan.io/", null, false)
        );
        registerBuiltIn(
                new NetworkDefinition("bnb", "BNB Chain", 56L, "BNB", "https://bsc-dataseed.bnbchain.org", "https://bscscan.com/", null, false)
        );
        registerBuiltIn(
                new NetworkDefinition("base", "Base", 8453L, "ETH", "https://base-rpc.publicnode.com", "https://basescan.org/", null, false)
        );
        registerBuiltIn(
                new NetworkDefinition("polygon", "Polygon", 137L, "POL", "https://polygon-bor-rpc.publicnode.com", "https://polygonscan.com/", null, false)
        );
        registerBuiltIn(
                new NetworkDefinition("arbitrum", "Arbitrum", 42161L, "ETH", "https://arbitrum-one-rpc.publicnode.com", "https://arbiscan.io/", null, false)
        );
        registerBuiltIn(
                new NetworkDefinition("avalanche", "Avalanche", 43114L, "AVAX", "https://avalanche-c-chain-rpc.publicnode.com", "https://snowtrace.io/", null, false)
        );
        registerBuiltIn(
                new NetworkDefinition("celo", "Celo", 42220L, "CELO", "https://forno.celo.org", "https://celoscan.io/", null, false)
        );
    }

    public static List<NetworkDefinition> getAvailableNetworks(Context context) {
        Context app = getAppContext(context);
        ArrayList<NetworkDefinition> networks = new ArrayList<>();
        for (NetworkDefinition[] pair : BUILT_IN_NETWORKS.values()) {
            if (pair != null && pair.length > 0 && pair[0] != null) {
                networks.add(pair[0]);
            }
        }
        if (app != null) {
            networks.addAll(getCustomNetworks(app));
        }
        return networks;
    }

    public static List<NetworkDefinition> getAllNetworks(Context context) {
        ArrayList<NetworkDefinition> networks = new ArrayList<>();
        for (NetworkDefinition[] pair : BUILT_IN_NETWORKS.values()) {
            if (pair != null) {
                if (pair.length > 0 && pair[0] != null) networks.add(pair[0]);
            }
        }
        Context app = getAppContext(context);
        if (app != null) {
            networks.addAll(getCustomNetworks(app));
        }
        return networks;
    }

    public static NetworkDefinition getActiveNetwork(Context context) {
        Context app = getAppContext(context);
        List<NetworkDefinition> networks = getAvailableNetworks(app);
        String selectedKey = app != null ? PreferenceManager.getWalletSelectedNetworkKey(app) : DEFAULT_NETWORK_KEY;
        if (selectedKey != null) {
            for (NetworkDefinition network : networks) {
                if (network.key.equalsIgnoreCase(selectedKey)) {
                    return network;
                }
            }
        }
        for (NetworkDefinition network : networks) {
            if (DEFAULT_NETWORK_KEY.equalsIgnoreCase(network.key)) {
                if (app != null) PreferenceManager.setWalletSelectedNetworkKey(app, network.key);
                return network;
            }
        }
        NetworkDefinition fallback = networks.isEmpty() ? getFallbackNetwork() : networks.get(0);
        if (app != null) PreferenceManager.setWalletSelectedNetworkKey(app, fallback.key);
        return fallback;
    }

    public static String getNetworkDisplayName(Context context) {
        return getActiveNetwork(context).displayName;
    }

    public static String getRpcUrl(Context context) {
        return getActiveNetwork(context).rpcUrl;
    }

    public static long getChainId(Context context) {
        return getActiveNetwork(context).chainId;
    }

    public static String getCurrencySymbol(Context context) {
        return getActiveNetwork(context).currencySymbol;
    }

    public static String getExplorerBase(Context context) {
        return getActiveNetwork(context).explorerBase;
    }

    public static String getExplorerApiKey(Context context) {
        return getActiveNetwork(context).explorerApiKey;
    }

    public static String getExplorerAddressUrl(Context context, String address) {
        String explorerBase = getExplorerBase(context);
        if (explorerBase == null || explorerBase.isEmpty() || address == null || address.isEmpty()) {
            return null;
        }
        return explorerBase + "address/" + address;
    }

    public static NetworkDefinition findNetworkByChainId(Context context, long chainId) {
        for (NetworkDefinition network : getAllNetworks(context)) {
            if (network.chainId == chainId) {
                return network;
            }
        }
        return null;
    }

    public static boolean selectNetwork(Context context, NetworkDefinition network) {
        Context app = getAppContext(context);
        if (app == null || network == null) {
            return false;
        }
        NetworkDefinition normalized = normalize(network);
        if (normalized == null) {
            return false;
        }
        boolean keyOk = PreferenceManager.setWalletSelectedNetworkKey(app, normalized.key);
        PreferenceManager.setWalletTestnetEnabled(app, normalized.testnet);
        Web3Provider.reset();
        return keyOk;
    }

    public static boolean switchToChainId(Context context, long chainId) {
        NetworkDefinition network = findNetworkByChainId(context, chainId);
        return network != null && selectNetwork(context, network);
    }

    public static synchronized NetworkDefinition addOrUpdateCustomNetwork(Context context, NetworkDefinition candidate) {
        Context app = getAppContext(context);
        if (app == null || candidate == null || candidate.chainId <= 0L) {
            return null;
        }

        NetworkDefinition builtIn = findBuiltInByChainId(candidate.chainId);
        if (builtIn != null) {
            return builtIn;
        }

        ArrayList<NetworkDefinition> customNetworks = new ArrayList<>(getCustomNetworks(app));
        int existingIndex = -1;
        String existingKey = null;
        for (int i = 0; i < customNetworks.size(); i++) {
            NetworkDefinition existing = customNetworks.get(i);
            if (existing.chainId == candidate.chainId) {
                existingIndex = i;
                existingKey = existing.key;
                break;
            }
        }

        String requestedKey = safeTrim(candidate.key);
        if (existingKey != null && !existingKey.isEmpty()) {
            requestedKey = existingKey;
        }
        if (requestedKey.isEmpty() || !isCustomKey(requestedKey)) {
            requestedKey = buildCustomKey(candidate.chainId, candidate.displayName);
        }

        NetworkDefinition prepared = normalize(new NetworkDefinition(
                requestedKey,
                candidate.displayName,
                candidate.chainId,
                candidate.currencySymbol,
                candidate.rpcUrl,
                candidate.explorerBase,
                candidate.explorerApiKey,
                candidate.testnet
        ));
        if (prepared == null) {
            return null;
        }

        if (existingIndex >= 0) {
            customNetworks.set(existingIndex, prepared);
        } else {
            if (customNetworks.size() >= MAX_CUSTOM_NETWORKS) {
                return null;
            }
            customNetworks.add(prepared);
        }

        if (!PreferenceManager.setWalletCustomNetworksJson(app, new Gson().toJson(customNetworks))) {
            return null;
        }
        return prepared;
    }

    public static List<NetworkDefinition> getCustomNetworkDefinitions(Context context) {
        Context app = getAppContext(context);
        if (app == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getCustomNetworks(app));
    }

    public static synchronized boolean deleteCustomNetworks(Context context, List<String> keysToDelete) {
        Context app = getAppContext(context);
        if (app == null || keysToDelete == null || keysToDelete.isEmpty()) {
            return false;
        }

        ArrayList<String> normalizedKeys = new ArrayList<>();
        for (String key : keysToDelete) {
            String normalized = safeTrim(key).toLowerCase(Locale.US);
            if (isCustomKey(normalized)) {
                normalizedKeys.add(normalized);
            }
        }
        if (normalizedKeys.isEmpty()) {
            return false;
        }

        ArrayList<NetworkDefinition> customNetworks = new ArrayList<>(getCustomNetworks(app));
        ArrayList<NetworkDefinition> kept = new ArrayList<>();
        boolean removedSelected = false;
        String selectedKey = safeTrim(PreferenceManager.getWalletSelectedNetworkKey(app)).toLowerCase(Locale.US);
        for (NetworkDefinition network : customNetworks) {
            if (network == null || normalizedKeys.contains(safeTrim(network.key).toLowerCase(Locale.US))) {
                if (network != null && safeTrim(network.key).equalsIgnoreCase(selectedKey)) {
                    removedSelected = true;
                }
            } else {
                kept.add(network);
            }
        }

        if (kept.size() == customNetworks.size()) {
            return false;
        }
        if (!PreferenceManager.setWalletCustomNetworksJson(app, new Gson().toJson(kept))) {
            return false;
        }
        if (removedSelected) {
            PreferenceManager.setWalletSelectedNetworkKey(app, DEFAULT_NETWORK_KEY);
        }
        return true;
    }

    private static void registerBuiltIn(NetworkDefinition mainnet) {
        BUILT_IN_NETWORKS.put(mainnet.key, new NetworkDefinition[]{mainnet});
    }

    private static Context getAppContext(Context context) {
        return context == null ? null : context.getApplicationContext();
    }

    private static NetworkDefinition getFallbackNetwork() {
        return BUILT_IN_NETWORKS.get(DEFAULT_NETWORK_KEY)[0];
    }

    private static NetworkDefinition findBuiltInByChainId(long chainId) {
        for (NetworkDefinition[] pair : BUILT_IN_NETWORKS.values()) {
            if (pair == null) continue;
            for (NetworkDefinition network : pair) {
                if (network != null && network.chainId == chainId) {
                    return network;
                }
            }
        }
        return null;
    }

    private static List<NetworkDefinition> getCustomNetworks(Context context) {
        String json = PreferenceManager.getWalletCustomNetworksJson(context);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<NetworkDefinition>>() {}.getType();
            ArrayList<NetworkDefinition> raw = new Gson().fromJson(json, type);
            ArrayList<NetworkDefinition> sanitized = new ArrayList<>();
            if (raw == null) return sanitized;
            for (NetworkDefinition candidate : raw) {
                NetworkDefinition normalized = normalize(candidate);
                if (normalized != null) {
                    sanitized.add(normalized);
                }
            }
            return sanitized;
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    private static NetworkDefinition normalize(NetworkDefinition candidate) {
        if (candidate == null) return null;
        String key = safeTrim(candidate.key);
        String displayName = limit(safeTrim(candidate.displayName), MAX_NAME_LENGTH);
        String symbol = limit(safeTrim(candidate.currencySymbol), MAX_SYMBOL_LENGTH);
        String rpcUrl = safeTrim(candidate.rpcUrl);
        String explorerBase = safeTrim(candidate.explorerBase);
        if (key.isEmpty() || displayName.isEmpty() || symbol.isEmpty() || rpcUrl.isEmpty() || candidate.chainId <= 0L) {
            return null;
        }
        if (rpcUrl.length() > MAX_URL_LENGTH || explorerBase.length() > MAX_URL_LENGTH) {
            return null;
        }
        if (!isHttpsUrl(rpcUrl) || (!explorerBase.isEmpty() && !isHttpsUrl(explorerBase))) {
            return null;
        }
        return new NetworkDefinition(
                key.toLowerCase(),
                displayName,
                candidate.chainId,
                symbol,
                rpcUrl,
                ensureTrailingSlash(explorerBase),
                safeTrim(candidate.explorerApiKey),
                candidate.testnet
        );
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isCustomKey(String key) {
        return safeTrim(key).startsWith("custom_");
    }

    private static String buildCustomKey(long chainId, String displayName) {
        String slug = safeTrim(displayName).toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_");
        slug = slug.replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "custom_" + chainId : "custom_" + chainId + "_" + slug;
    }

    private static String ensureTrailingSlash(String url) {
        String trimmed = safeTrim(url);
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    private static boolean isHttpsUrl(String value) {
        try {
            java.net.URI uri = new java.net.URI(safeTrim(value));
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty()
                    && uri.getUserInfo() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public static class NetworkDefinition {
        public String key;
        public String displayName;
        public long chainId;
        public String currencySymbol;
        public String rpcUrl;
        public String explorerBase;
        public String explorerApiKey;
        public boolean testnet;

        public NetworkDefinition() {
        }

        public NetworkDefinition(String key, String displayName, long chainId, String currencySymbol,
                                 String rpcUrl, String explorerBase, String explorerApiKey, boolean testnet) {
            this.key = key;
            this.displayName = displayName;
            this.chainId = chainId;
            this.currencySymbol = currencySymbol;
            this.rpcUrl = rpcUrl;
            this.explorerBase = ensureTrailingSlash(explorerBase);
            this.explorerApiKey = explorerApiKey;
            this.testnet = testnet;
        }
    }
}
