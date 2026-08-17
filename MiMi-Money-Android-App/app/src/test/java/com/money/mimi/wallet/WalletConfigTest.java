package com.money.mimi.wallet;

import android.content.Context;

import com.money.mimi.helpers.PreferenceManager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WalletConfigTest {
    private Context app;

    @Before
    public void setUp() {
        app = TestSupport.newContext();
        PreferenceManager.setWalletSelectedNetworkKey(app, WalletConfig.DEFAULT_NETWORK_KEY);
        PreferenceManager.setWalletTestnetEnabled(app, false);
        Web3Provider.reset();
    }

    @Test
    public void addOrUpdateCustomNetwork_addsNetworkAndCanSwitchToIt() {
        WalletConfig.NetworkDefinition added = WalletConfig.addOrUpdateCustomNetwork(app,
                new WalletConfig.NetworkDefinition("", "My Chain", 31337L, "ETH",
                        "https://rpc.example", "https://explorer.example", null, true));

        assertNotNull(added);
        assertEquals("custom_31337_my_chain", added.key);
        assertEquals("https://explorer.example/", added.explorerBase);
        assertEquals(added.key, WalletConfig.findNetworkByChainId(app, 31337L).key);
        assertTrue(WalletConfig.switchToChainId(app, 31337L));
        assertEquals(31337L, WalletConfig.getChainId(app));
        assertTrue(PreferenceManager.isWalletTestnetEnabled(app));
        assertEquals(added.key, PreferenceManager.getWalletSelectedNetworkKey(app));
    }

    @Test
    public void addOrUpdateCustomNetwork_updatesExistingEntryAndRetainsKey() {
        WalletConfig.NetworkDefinition first = WalletConfig.addOrUpdateCustomNetwork(app,
                new WalletConfig.NetworkDefinition("", "My Chain", 31337L, "ETH",
                        "https://rpc.one", "https://scan.one", null, false));
        WalletConfig.NetworkDefinition updated = WalletConfig.addOrUpdateCustomNetwork(app,
                new WalletConfig.NetworkDefinition("", "Better Chain", 31337L, "ETH",
                        "https://rpc.two", "https://scan.two", null, false));

        assertEquals(first.key, updated.key);
        assertEquals("Better Chain", WalletConfig.findNetworkByChainId(app, 31337L).displayName);
        assertEquals("https://rpc.two", WalletConfig.findNetworkByChainId(app, 31337L).rpcUrl);
    }

    @Test
    public void addOrUpdateCustomNetwork_returnsBuiltInForKnownChainAndUnknownSwitchFails() {
        WalletConfig.NetworkDefinition builtIn = WalletConfig.addOrUpdateCustomNetwork(app,
                new WalletConfig.NetworkDefinition("custom_1", "Fake Ethereum", 1L, "ETH",
                        "https://bad.example", "https://badscan.example", null, true));

        assertNotNull(builtIn);
        assertEquals("ethereum", builtIn.key);
        assertEquals(1L, builtIn.chainId);
        assertFalse(WalletConfig.switchToChainId(app, 999999L));
    }

    @Test
    public void crooCheckoutNetwork_switchesToBuiltInBase() {
        assertTrue(WalletConfig.switchToChainId(app, 8453L));
        assertEquals(8453L, WalletConfig.getChainId(app));
        assertEquals("base", PreferenceManager.getWalletSelectedNetworkKey(app));
        assertEquals("Base", WalletConfig.getNetworkDisplayName(app));
    }
}
