package com.money.mimi.wallet;

import android.content.Context;
import android.webkit.WebView;

import com.money.mimi.helpers.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DappBrowserSupportTest {
    private Object newBridge() throws Exception {
        Context context = TestSupport.newContext();
        PreferenceManager.setWalletSelectedNetworkKey(context, WalletConfig.DEFAULT_NETWORK_KEY);
        Web3Provider.reset();
        Class<?> bridgeClass = Class.forName("com.money.mimi.wallet.DappBrowserSupport$JsBridge");
        Constructor<?> ctor = bridgeClass.getDeclaredConstructor(Context.class, WebView.class);
        ctor.setAccessible(true);
        Object bridge = ctor.newInstance(context, null);
        invoke(bridge, "setOriginUrl", new Class[]{String.class}, "https://geto.space/welcome");
        return bridge;
    }

    private Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw e;
        }
    }

    private Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    public void parseMessageSignPayload_acceptsAddressInSecondPosition() throws Exception {
        Object bridge = newBridge();
        Object payload = invoke(bridge, "parseMessageSignPayload", new Class[]{JSONArray.class, String.class},
                new JSONArray().put("hello").put("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"), "personal_sign");

        assertEquals("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826", field(payload, "address"));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) field(payload, "data"));
    }

    @Test
    public void parseMessageSignPayload_rejectsWhenNoAddressIsPresent() throws Exception {
        Object bridge = newBridge();

        try {
            invoke(bridge, "parseMessageSignPayload", new Class[]{JSONArray.class, String.class},
                    new JSONArray().put("hello").put("world"), "personal_sign");
            fail("Expected invalid params error");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires one address parameter"));
        }
    }

    @Test
    public void parseTypedDataRequest_preservesStringPayloadAndNormalizesAddress() throws Exception {
        Object bridge = newBridge();
        String typedData = "{\"types\":{\"EIP712Domain\":[],\"Ping\":[{\"name\":\"value\",\"type\":\"string\"}]},\"primaryType\":\"Ping\",\"domain\":{},\"message\":{\"value\":\"pong\"}}";
        Object request = invoke(bridge, "parseTypedDataRequest", new Class[]{String.class, JSONArray.class},
                "eth_signTypedData_v4", new JSONArray().put(typedData).put("CD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"));

        assertEquals("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826", field(request, "address"));
        assertEquals(typedData, field(request, "payload"));
    }

    @Test
    public void request_returnsSuccessForCurrentChainSwitch() throws Exception {
        Object bridge = newBridge();
        JSONObject response = new JSONObject((String) invoke(bridge, "request", new Class[]{String.class, String.class},
                "wallet_switchEthereumChain", "[{\"chainId\":\"0x1\"}]"));

        assertTrue(response.has("result"));
        assertNull(response.opt("error"));
    }

    @Test
    public void request_validatesSwitchAndAddChainFailures() throws Exception {
        Object bridge = newBridge();

        JSONObject switchResponse = new JSONObject((String) invoke(bridge, "request", new Class[]{String.class, String.class},
                "wallet_switchEthereumChain", "[{\"chainId\":\"0x539\"}]"));
        assertEquals(4902, switchResponse.getJSONObject("error").getInt("code"));

        JSONObject addResponse = new JSONObject((String) invoke(bridge, "request", new Class[]{String.class, String.class},
                "wallet_addEthereumChain", "[{\"chainId\":\"0x539\",\"chainName\":\"Localhost\"}]"));
        assertEquals(-32602, addResponse.getJSONObject("error").getInt("code"));
    }

    @Test
    public void buildInjectionScript_exposesDetectableProviderShape() throws Exception {
        Method method = DappBrowserSupport.class.getDeclaredMethod("buildInjectionScript", Context.class);
        method.setAccessible(true);

        String script = (String) method.invoke(null, TestSupport.newContext());

        assertTrue(script.contains("isMimiWallet:true"));
        assertTrue(script.contains("isMetaMask:true"));
        assertTrue(script.contains("eip6963:announceProvider"));
        assertTrue(script.contains("ethereum#initialized"));
        assertTrue(script.contains("selectedProvider=provider"));
        assertTrue(script.contains("nativeRequest(args.method,args.params||[])"));
    }

    @Test
    public void paymentProvider_canSwitchToBaseAndRejectsMalformedTransaction() throws Exception {
        Object bridge = newBridge();
        Context context = (Context) field(bridge, "appContext");
        assertTrue(WalletConfig.switchToChainId(context, 8453L));
        assertEquals("0x2105", invoke(bridge, "getChainIdHex", new Class[]{}));

        JSONObject paymentResponse = new JSONObject((String) invoke(bridge, "request",
                new Class[]{String.class, String.class}, "eth_sendTransaction", "[]"));
        assertEquals(-32602, paymentResponse.getJSONObject("error").getInt("code"));
    }
}
