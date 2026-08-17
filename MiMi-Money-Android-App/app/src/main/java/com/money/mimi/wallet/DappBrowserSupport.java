package com.money.mimi.wallet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.Base64;
import android.text.InputType;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public final class DappBrowserSupport {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String JS_BRIDGE_NAME = "MimiWalletAndroid";
    private static final String WALLET_PROVIDER_ICON_FALLBACK_DATA_URI = "data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='128'%20height='128'%20viewBox='0%200%20128%20128'%3E%3Crect%20width='128'%20height='128'%20rx='28'%20fill='%23006450'/%3E%3Ctext%20x='64'%20y='75'%20text-anchor='middle'%20font-family='Arial,sans-serif'%20font-size='34'%20font-weight='700'%20fill='white'%3EMiMi%3C/text%3E%3C/svg%3E";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final long APPROVAL_TIMEOUT_SECONDS = 300L;
    private static final Set<String> BLOCKED_METHODS = new HashSet<>(Arrays.asList(
            "eth_sendTransaction",
            "eth_sign",
            "eth_signTypedData",
            "eth_signTypedData_v3",
            "eth_signTypedData_v4",
            "personal_sign",
            "wallet_addEthereumChain",
            "wallet_switchEthereumChain"
    ));

    private DappBrowserSupport() {
    }

    public static void attach(WebView webView, Context context) {
        if (webView == null || context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }
        webView.removeJavascriptInterface(JS_BRIDGE_NAME);
        webView.addJavascriptInterface(new JsBridge(context.getApplicationContext(), webView), JS_BRIDGE_NAME);
        injectProvider(webView);
    }

    public static void injectProvider(final WebView webView) {
        if (webView == null) return;
        webView.post(() -> {
            String js = buildInjectionScript(webView.getContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(js, null);
            } else {
                webView.loadUrl("javascript:" + js);
            }
        });
    }

    private static String buildInjectionScript(Context context) {
        String providerIcon = JSONObject.quote(walletProviderIconDataUri(context));
        return "(function(){"
                + "if(!window." + JS_BRIDGE_NAME + "){return;}"
                + "if(window.__mimiWalletInjected){if(window.__mimiWalletSync){window.__mimiWalletSync();}return;}"
                + "var listeners={};"
                + "function emit(event,payload){(listeners[event]||[]).slice().forEach(function(fn){try{fn(payload);}catch(e){}});}"
                + "function toError(err){var e=new Error((err&&err.message)||'Wallet request failed');if(err&&err.code!=null){e.code=err.code;}if(err&&err.data!=null){e.data=err.data;}return e;}"
                + "function syncOrigin(){try{window." + JS_BRIDGE_NAME + ".setOriginUrl(String(window.location&&window.location.href||''));}catch(e){}}"
                + "function nativeRequest(method,params){if(!window." + JS_BRIDGE_NAME + "||typeof window." + JS_BRIDGE_NAME + ".request!=='function'){throw toError({code:-32603,message:'MiMi Money wallet bridge is unavailable. Please reopen this dApp.'});}var raw=window." + JS_BRIDGE_NAME + ".request(method,JSON.stringify(params||[]));var parsed=raw?JSON.parse(raw):{};if(parsed.error){throw toError(parsed.error);}return parsed.result;}"
                + "var provider={isMimiWallet:true,isMiMiMoney:true,isMetaMask:true,autoRefreshOnNetworkChange:false,_metamask:{isUnlocked:function(){return Promise.resolve(true);}},enable:function(){return this.request({method:'eth_requestAccounts'});},isConnected:function(){return true;},request:function(args){args=args||{};if(!args.method){return Promise.reject(toError({code:-32600,message:'Missing wallet method'}));}return Promise.resolve().then(function(){var result=nativeRequest(args.method,args.params||[]);if(window.__mimiWalletSync&&(args.method==='eth_requestAccounts'||args.method==='wallet_requestPermissions'||args.method==='wallet_switchEthereumChain'||args.method==='wallet_addEthereumChain')){window.__mimiWalletSync();}return result;});},on:function(event,handler){if(!listeners[event]){listeners[event]=[]}listeners[event].push(handler);return provider;},addListener:function(event,handler){return provider.on(event,handler);},removeListener:function(event,handler){if(!listeners[event]){return provider;}listeners[event]=listeners[event].filter(function(item){return item!==handler;});return provider;},off:function(event,handler){return provider.removeListener(event,handler);},sendAsync:function(payload,callback){provider.request({method:payload&&payload.method,params:(payload&&payload.params)||[]}).then(function(result){if(callback){callback(null,{id:payload&&payload.id,jsonrpc:'2.0',result:result});}}).catch(function(error){if(callback){callback(error,null);}});},send:function(payloadOrMethod,params){if(typeof payloadOrMethod==='string'){return provider.request({method:payloadOrMethod,params:Array.isArray(params)?params:[]});}var payload=payloadOrMethod||{};return {id:payload.id||null,jsonrpc:'2.0',result:nativeRequest(payload.method,payload.params||[])};}};"
                + "Object.defineProperty(provider,'selectedAddress',{get:function(){var accounts=JSON.parse(window." + JS_BRIDGE_NAME + ".getAccountsJson()||'[]');return accounts.length?accounts[0]:null;}});"
                + "Object.defineProperty(provider,'chainId',{get:function(){return window." + JS_BRIDGE_NAME + ".getChainIdHex();}});"
                + "Object.defineProperty(provider,'networkVersion',{get:function(){return window." + JS_BRIDGE_NAME + ".getNetworkVersion();}});"
                + "window.__mimiWalletSync=function(){var accounts=JSON.parse(window." + JS_BRIDGE_NAME + ".getAccountsJson()||'[]');var chainId=window." + JS_BRIDGE_NAME + ".getChainIdHex();var prevAccounts=window.__mimiWalletAccounts||[];var prevChainId=window.__mimiWalletChainId||null;if(JSON.stringify(prevAccounts)!==JSON.stringify(accounts)){window.__mimiWalletAccounts=accounts;emit('accountsChanged',accounts);}if(prevChainId!==chainId){window.__mimiWalletChainId=chainId;emit('chainChanged',chainId);}if(!window.__mimiWalletDidConnect&&chainId){window.__mimiWalletDidConnect=true;emit('connect',{chainId:chainId});}};"
                + "var providerInfo={uuid:'6f3b1dc4-dcc2-4a52-b436-4d4f2b0b7f3a',name:'MiMi Money',icon:" + providerIcon + ",rdns:'app.mimimoney.wallet'};"
                + "function announceProvider(){try{window.dispatchEvent(new CustomEvent('eip6963:announceProvider',{detail:{info:providerInfo,provider:provider}}));}catch(e){}}"
                + "window.addEventListener('eip6963:requestProvider',announceProvider);"
                + "syncOrigin();window.__mimiWalletInjected=true;if(window.ethereum&&Array.isArray(window.ethereum.providers)){var exists=false;for(var i=0;i<window.ethereum.providers.length;i++){if(window.ethereum.providers[i]&&window.ethereum.providers[i].isMimiWallet){exists=true;break;}}if(!exists){window.ethereum.providers.push(provider);}window.ethereum.selectedProvider=provider;}else{provider.providers=[provider];provider.selectedProvider=provider;window.ethereum=provider;}if(!window.web3){window.web3={currentProvider:provider};}announceProvider();setTimeout(announceProvider,0);setTimeout(announceProvider,250);try{window.dispatchEvent(new Event('ethereum#initialized'));}catch(e){if(document.createEvent){var evt=document.createEvent('Event');evt.initEvent('ethereum#initialized',false,false);window.dispatchEvent(evt);}}window.__mimiWalletSync();"
                + "})();";
    }

    @SuppressLint("ResourceType")
    private static String walletProviderIconDataUri(Context context) {
        if (context == null) {
            return WALLET_PROVIDER_ICON_FALLBACK_DATA_URI;
        }
        try (InputStream input = context.getResources().openRawResource(R.drawable.mimi_wallet_provider_icon_128);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return WALLET_PROVIDER_ICON_FALLBACK_DATA_URI;
        }
    }

    public static final class JsBridge {
        private final Context appContext;
        private final WebView webView;
        private volatile String originUrl;

        public JsBridge(Context appContext, WebView webView) {
            this.appContext = appContext;
            this.webView = webView;
            this.originUrl = webView != null ? webView.getUrl() : null;
        }

        @JavascriptInterface
        public void setOriginUrl(String url) {
            originUrl = url == null ? "" : url.trim();
        }

        @JavascriptInterface
        public String getAccountsJson() {
            JSONArray result = new JSONArray();
            if (!isSecureWebOrigin()) {
                return result.toString();
            }
            String address = PreferenceManager.getWalletAddress(appContext);
            if (address != null && !address.trim().isEmpty()) {
                result.put(address);
            }
            return result.toString();
        }

        @JavascriptInterface
        public String getChainIdHex() {
            if (!isSecureWebOrigin()) {
                return "";
            }
            return "0x" + Long.toHexString(WalletConfig.getChainId(appContext));
        }

        @JavascriptInterface
        public String getNetworkVersion() {
            if (!isSecureWebOrigin()) {
                return "";
            }
            return String.valueOf(WalletConfig.getChainId(appContext));
        }

        @JavascriptInterface
        public String request(String method, String paramsJson) {
            try {
                if (!isSecureWebOrigin()) {
                    return error(4100, "Wallet requests are only allowed from secure HTTPS dApps");
                }
                String safeMethod = method == null ? "" : method.trim();
                if (safeMethod.isEmpty()) {
                    return error(-32600, "Missing wallet method");
                }
                JSONArray params = parseParams(paramsJson);
                switch (safeMethod) {
                    case "eth_accounts":
                        return success(new JSONArray(getAccountsJson()));
                    case "eth_requestAccounts": {
                        JSONArray accounts = new JSONArray(getAccountsJson());
                        if (accounts.length() == 0) {
                            return error(4100, "Wallet has not been created yet");
                        }
                        return success(accounts);
                    }
                    case "eth_coinbase": {
                        JSONArray accounts = new JSONArray(getAccountsJson());
                        return success(accounts.length() > 0 ? accounts.optString(0) : JSONObject.NULL);
                    }
                    case "eth_chainId":
                        return success(getChainIdHex());
                    case "net_version":
                        return success(getNetworkVersion());
                    case "wallet_getPermissions":
                        return success(getPermissions());
                    case "wallet_requestPermissions":
                        return handlePermissionRequest(params);
                    case "eth_sendTransaction":
                        return handleSendTransaction(params);
                    case "personal_sign":
                        return handlePersonalSign(params);
                    case "eth_sign":
                        return handleEthSign(params);
                    case "eth_signTypedData":
                    case "eth_signTypedData_v3":
                    case "eth_signTypedData_v4":
                        return handleTypedDataSign(safeMethod, params);
                    case "wallet_switchEthereumChain":
                        return handleSwitchEthereumChain(params);
                    case "wallet_addEthereumChain":
                        return handleAddEthereumChain(params);
                    default:
                        if (safeMethod.startsWith("personal_")) {
                            return error(4200, safeMethod + " is not supported by the in-app wallet yet");
                        }
                        if (!isRpcPassThroughAllowed(safeMethod)) {
                            return error(4200, "Unsupported wallet method: " + safeMethod);
                        }
                        return proxyRpc(safeMethod, params);
                }
            } catch (RpcException e) {
                return error(e.code, e.getMessage());
            } catch (Exception e) {
                String message = e.getMessage();
                return error(-32603, message == null || message.trim().isEmpty() ? "Wallet bridge failed" : message);
            }
        }

        private String handleSendTransaction(JSONArray params) throws Exception {
            JSONObject tx = requireObject(params, 0, "Transaction object is required");
            Credentials credentials = loadCredentials();
            String walletAddress = normalizeAddress(credentials.getAddress());
            String from = normalizeAddress(optString(tx, "from"));
            if (from.isEmpty()) {
                from = walletAddress;
            }
            if (!walletAddress.equalsIgnoreCase(from)) {
                throw new RpcException(4100, "Transaction must be sent from the active wallet address");
            }

            String to = normalizeAddress(optString(tx, "to"));
            String data = normalizeHexData(optString(tx, "data"));
            BigInteger valueWei = parseQuantity(tx.opt("value"), BigInteger.ZERO, false);
            BigInteger gasPrice = parseQuantity(tx.opt("gasPrice"), null, false);
            BigInteger gasLimit = parseQuantity(tx.opt("gas"), null, false);
            BigInteger nonce = parseQuantity(tx.opt("nonce"), null, false);
            if (to.isEmpty() && "0x".equals(data)) {
                throw new RpcException(-32602, "Transaction must include a recipient or contract data");
            }

            StringBuilder summary = new StringBuilder();
            summary.append("Dapp: ").append(getOriginLabel()).append("\n\n")
                    .append("From: ").append(from).append("\n")
                    .append("To: ").append(to.isEmpty() ? "(contract deployment)" : to).append("\n")
                    .append("Value: ").append(formatNativeValue(valueWei, WalletConfig.getCurrencySymbol(appContext))).append("\n");
            if (!"0x".equals(data)) {
                summary.append("Data bytes: ").append(Math.max(0, (data.length() - 2) / 2)).append("\n");
            }
            if (gasLimit != null) {
                summary.append("Gas limit: ").append(gasLimit.toString()).append("\n");
            }
            if (gasPrice != null) {
                summary.append("Gas price: ").append(formatGwei(gasPrice)).append(" gwei\n");
            }
            requireApproval("Approve transaction", summary.toString());

            Web3j web3 = Web3Provider.get(appContext);
            if (nonce == null) {
                EthGetTransactionCount txCount = web3.ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING).send();
                if (txCount.hasError()) {
                    throw rpcError(txCount.getError(), "Unable to fetch transaction nonce");
                }
                nonce = txCount.getTransactionCount();
            }
            if (gasPrice == null) {
                gasPrice = web3.ethGasPrice().send().getGasPrice();
            }
            if (gasLimit == null) {
                gasLimit = estimateGas(web3, walletAddress, nonce, gasPrice, to, valueWei, data);
            }
            RawTransaction rawTransaction;
            if (to.isEmpty()) {
                rawTransaction = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, valueWei, data);
            } else if (!"0x".equals(data)) {
                rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, valueWei, data);
            } else {
                rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, valueWei);
            }
            byte[] signedMessage = signEip155(rawTransaction, WalletConfig.getChainId(appContext), credentials);
            EthSendTransaction response = web3.ethSendRawTransaction(Numeric.toHexString(signedMessage)).send();
            if (response.hasError()) {
                throw rpcError(response.getError(), "Transaction was rejected by RPC");
            }
            String txHash = response.getTransactionHash();
            if (txHash == null || txHash.trim().isEmpty()) {
                throw new RpcException(-32603, "RPC did not return a transaction hash");
            }
            return success(txHash);
        }

        private String handlePersonalSign(JSONArray params) throws Exception {
            SignPayload payload = parseMessageSignPayload(params, "personal_sign");
            Credentials credentials = loadCredentials();
            String walletAddress = normalizeAddress(credentials.getAddress());
            if (!walletAddress.equalsIgnoreCase(payload.address)) {
                throw new RpcException(4100, "Signature must use the active wallet address");
            }
            String message = "Dapp: " + getOriginLabel() + "\n\n"
                    + "Account: " + walletAddress + "\n"
                    + "Method: personal_sign\n"
                    + "Payload bytes: " + payload.data.length + "\n\n"
                    + payload.preview;
            requireApproval("Approve signature", message);
            return success(signatureToHex(Sign.signPrefixedMessage(payload.data, credentials.getEcKeyPair())));
        }

        private String handleEthSign(JSONArray params) throws Exception {
            SignPayload payload = parseMessageSignPayload(params, "eth_sign");
            Credentials credentials = loadCredentials();
            String walletAddress = normalizeAddress(credentials.getAddress());
            if (!walletAddress.equalsIgnoreCase(payload.address)) {
                throw new RpcException(4100, "Signature must use the active wallet address");
            }
            String message = "Dapp: " + getOriginLabel() + "\n\n"
                    + "Account: " + walletAddress + "\n"
                    + "Method: eth_sign\n"
                    + "Payload bytes: " + payload.data.length + "\n\n"
                    + payload.preview;
            requireApproval("Approve signature", message);
            return success(signatureToHex(Sign.signMessage(payload.data, credentials.getEcKeyPair())));
        }

        private String handleTypedDataSign(String method, JSONArray params) throws Exception {
            TypedDataRequest request = parseTypedDataRequest(method, params);
            Credentials credentials = loadCredentials();
            String walletAddress = normalizeAddress(credentials.getAddress());
            if (!walletAddress.equalsIgnoreCase(request.address)) {
                throw new RpcException(4100, "Signature must use the active wallet address");
            }
            Eip712Json.TypedDataHashResult hashResult = Eip712Json.hash(request.payload);
            if (hashResult.domainChainId != null && hashResult.domainChainId.longValue() != WalletConfig.getChainId(appContext)) {
                throw new RpcException(4901, "Typed data chainId does not match the active network");
            }
            StringBuilder message = new StringBuilder();
            message.append("Dapp: ").append(getOriginLabel()).append("\n\n")
                    .append("Account: ").append(walletAddress).append("\n")
                    .append("Method: ").append(method).append("\n")
                    .append("Primary type: ").append(hashResult.primaryType).append("\n");
            if (hashResult.domainName != null && !hashResult.domainName.isEmpty()) {
                message.append("Domain: ").append(hashResult.domainName).append("\n");
            }
            if (hashResult.verifyingContract != null && !hashResult.verifyingContract.isEmpty()) {
                message.append("Verifying contract: ").append(hashResult.verifyingContract).append("\n");
            }
            if (hashResult.domainChainId != null) {
                message.append("Domain chainId: ").append(hashResult.domainChainId).append("\n");
            }
            message.append("\nDigest: ").append(hashResult.digestHex);
            requireApproval("Approve typed data signature", message.toString());
            return success(signatureToHex(Sign.signMessage(hashResult.digest, credentials.getEcKeyPair(), false)));
        }

        private String handleSwitchEthereumChain(JSONArray params) throws Exception {
            JSONObject request = requireObject(params, 0, "Chain request is required");
            long chainId = parseChainId(request.opt("chainId"));
            if (chainId == WalletConfig.getChainId(appContext)) {
                return success(JSONObject.NULL);
            }
            WalletConfig.NetworkDefinition network = WalletConfig.findNetworkByChainId(appContext, chainId);
            if (network == null) {
                throw new RpcException(4902, "Requested chain is not configured in the wallet");
            }
            String message = "Dapp: " + getOriginLabel() + "\n\n"
                    + "Switch wallet network to:\n"
                    + network.displayName + " (chainId " + network.chainId + ")\n"
                    + network.rpcUrl;
            requireApproval("Switch network", message);
            if (!WalletConfig.switchToChainId(appContext, chainId)) {
                throw new RpcException(4902, "Requested chain is not configured in the wallet");
            }
            return success(JSONObject.NULL);
        }

        private String handleAddEthereumChain(JSONArray params) throws Exception {
            JSONObject request = requireObject(params, 0, "Network definition is required");
            long chainId = parseChainId(request.opt("chainId"));
            String chainName = optString(request, "chainName");
            if (chainName.isEmpty()) {
                chainName = "Chain " + chainId;
            }
            String rpcUrl = firstNonEmptyString(request.optJSONArray("rpcUrls"));
            if (rpcUrl.isEmpty()) {
                throw new RpcException(-32602, "wallet_addEthereumChain requires at least one rpcUrls entry");
            }
            if (!isHttpsUrl(rpcUrl)) {
                throw new RpcException(-32602, "wallet_addEthereumChain requires an HTTPS RPC URL");
            }
            JSONObject nativeCurrency = request.optJSONObject("nativeCurrency");
            String currencySymbol = nativeCurrency != null ? safeTrim(nativeCurrency.optString("symbol")) : "";
            if (currencySymbol.isEmpty()) {
                currencySymbol = WalletConfig.getCurrencySymbol(appContext);
            }
            if (currencySymbol.isEmpty()) {
                currencySymbol = "ETH";
            }
            String explorerBase = firstNonEmptyString(request.optJSONArray("blockExplorerUrls"));
            if (!explorerBase.isEmpty() && !isHttpsUrl(explorerBase)) {
                throw new RpcException(-32602, "wallet_addEthereumChain requires an HTTPS block explorer URL");
            }
            boolean testnet = inferTestnet(chainName, chainId, currencySymbol, rpcUrl);
            String message = "Dapp: " + getOriginLabel() + "\n\n"
                    + "Add and switch wallet network:\n"
                    + chainName + " (chainId " + chainId + ")\n"
                    + "Currency: " + currencySymbol + "\n"
                    + "RPC: " + rpcUrl
                    + (explorerBase.isEmpty() ? "" : "\nExplorer: " + explorerBase);
            requireApproval("Add network", message);
            WalletConfig.NetworkDefinition network = new WalletConfig.NetworkDefinition(
                    "",
                    chainName,
                    chainId,
                    currencySymbol,
                    rpcUrl,
                    explorerBase,
                    null,
                    testnet
            );
            WalletConfig.NetworkDefinition saved = WalletConfig.addOrUpdateCustomNetwork(appContext, network);
            if (saved == null) {
                throw new RpcException(-32603, "Unable to save requested network");
            }
            if (!WalletConfig.selectNetwork(appContext, saved)) {
                throw new RpcException(-32603, "Unable to activate requested network");
            }
            return success(JSONObject.NULL);
        }

        private JSONArray parseParams(String paramsJson) throws Exception {
            if (paramsJson == null || paramsJson.trim().isEmpty() || "undefined".equals(paramsJson)) {
                return new JSONArray();
            }
            Object parsed = new JSONTokener(paramsJson).nextValue();
            if (parsed instanceof JSONArray) {
                return (JSONArray) parsed;
            }
            JSONArray wrapped = new JSONArray();
            wrapped.put(parsed);
            return wrapped;
        }

        private JSONArray getPermissions() throws Exception {
            JSONArray permissions = new JSONArray();
            JSONArray accounts = new JSONArray(getAccountsJson());
            if (accounts.length() == 0) {
                return permissions;
            }
            JSONObject permission = new JSONObject();
            permission.put("parentCapability", "eth_accounts");
            permissions.put(permission);
            return permissions;
        }

        private String handlePermissionRequest(JSONArray params) throws Exception {
            if (params.length() == 0) {
                return success(getPermissions());
            }
            JSONObject requested = params.optJSONObject(0);
            if (requested != null && requested.has("eth_accounts")) {
                JSONArray accounts = new JSONArray(getAccountsJson());
                if (accounts.length() == 0) {
                    return error(4100, "Wallet has not been created yet");
                }
                return success(getPermissions());
            }
            return error(4200, "Only eth_accounts permission is supported");
        }

        private boolean isRpcPassThroughAllowed(String method) {
            return (method.startsWith("eth_") || method.startsWith("net_") || method.startsWith("web3_"))
                    && !BLOCKED_METHODS.contains(method)
                    && !method.startsWith("eth_sign");
        }

        private String proxyRpc(String method, JSONArray params) throws Exception {
            JSONObject payload = new JSONObject();
            payload.put("jsonrpc", "2.0");
            payload.put("id", 1);
            payload.put("method", method);
            payload.put("params", params == null ? new JSONArray() : params);
            Request request = new Request.Builder()
                    .url(WalletConfig.getRpcUrl(appContext))
                    .post(RequestBody.create(JSON, payload.toString()))
                    .build();
            try (okhttp3.Response response = HTTP.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("RPC HTTP " + response.code());
                }
                ResponseBody body = response.body();
                String rawBody = body != null ? body.string() : "";
                JSONObject rpcResponse = new JSONObject(rawBody);
                if (rpcResponse.has("error") && !rpcResponse.isNull("error")) {
                    Object error = rpcResponse.get("error");
                    if (error instanceof JSONObject) {
                        return error((JSONObject) error);
                    }
                    return error(-32000, String.valueOf(error));
                }
                return success(rpcResponse.has("result") ? rpcResponse.get("result") : JSONObject.NULL);
            }
        }

        private Credentials loadCredentials() throws Exception {
            String mnemonic = PreferenceManager.getWalletMnemonic(appContext);
            String password = PreferenceManager.getWalletPassword(appContext);
            if (mnemonic == null || mnemonic.trim().isEmpty() || password == null) {
                throw new RpcException(4100, "Wallet has not been created yet");
            }
            return WalletUtils.loadBip39Credentials(password, mnemonic);
        }

        private BigInteger estimateGas(Web3j web3, String from, BigInteger nonce, BigInteger gasPrice,
                                       String to, BigInteger valueWei, String data) throws Exception {
            if ((data == null || "0x".equals(data)) && to != null && !to.isEmpty()) {
                return BigInteger.valueOf(21000L);
            }
            Transaction estimateRequest = new Transaction(from, nonce, gasPrice, null,
                    to == null || to.isEmpty() ? null : to, valueWei, data);
            EthEstimateGas response = web3.ethEstimateGas(estimateRequest).send();
            if (response.hasError()) {
                throw rpcError(response.getError(), "Unable to estimate gas");
            }
            BigInteger amountUsed = response.getAmountUsed();
            if (amountUsed == null || amountUsed.signum() <= 0) {
                throw new RpcException(-32603, "Unable to estimate gas");
            }
            return amountUsed;
        }

        private SignPayload parseMessageSignPayload(JSONArray params, String method) throws Exception {
            if (params.length() < 2) {
                throw new RpcException(-32602, method + " requires an address and message payload");
            }
            Object first = params.opt(0);
            Object second = params.opt(1);
            String firstString = objectToString(first);
            String secondString = objectToString(second);
            boolean firstIsAddress = isHexAddress(firstString);
            boolean secondIsAddress = isHexAddress(secondString);
            String address;
            Object payload;
            if (firstIsAddress && !secondIsAddress) {
                address = normalizeAddress(firstString);
                payload = second;
            } else if (secondIsAddress && !firstIsAddress) {
                address = normalizeAddress(secondString);
                payload = first;
            } else if (firstIsAddress) {
                address = normalizeAddress(firstString);
                payload = second;
            } else if (secondIsAddress) {
                address = normalizeAddress(secondString);
                payload = first;
            } else {
                throw new RpcException(-32602, method + " requires one address parameter");
            }
            String payloadText = objectToString(payload);
            return new SignPayload(address, decodeData(payloadText), previewPayload(payloadText));
        }

        private TypedDataRequest parseTypedDataRequest(String method, JSONArray params) throws Exception {
            if (params.length() < 2) {
                throw new RpcException(-32602, method + " requires an address and typed data payload");
            }
            Object first = params.opt(0);
            Object second = params.opt(1);
            String firstString = objectToString(first);
            String secondString = objectToString(second);
            boolean firstIsAddress = isHexAddress(firstString);
            boolean secondIsAddress = isHexAddress(secondString);
            String address;
            Object payload;
            if (firstIsAddress && !secondIsAddress) {
                address = normalizeAddress(firstString);
                payload = second;
            } else if (secondIsAddress && !firstIsAddress) {
                address = normalizeAddress(secondString);
                payload = first;
            } else if (firstIsAddress) {
                address = normalizeAddress(firstString);
                payload = second;
            } else if (secondIsAddress) {
                address = normalizeAddress(secondString);
                payload = first;
            } else {
                throw new RpcException(-32602, method + " requires one address parameter");
            }
            return new TypedDataRequest(address, payload);
        }

        private void requireApproval(String title, String message) throws Exception {
            Activity activity = requireActivity();
            if (!awaitConfirmation(activity, title, message)) {
                throw new RpcException(4001, "User rejected the request");
            }
            if (!PreferenceManager.isRequireAuthForTransactions(appContext)) {
                return;
            }
            String existingPin = PreferenceManager.getWalletPin(appContext);
            if (existingPin == null || existingPin.isEmpty()) {
                String newPin = awaitNewPin(activity);
                if (newPin == null || newPin.isEmpty()) {
                    throw new RpcException(4001, "PIN setup was cancelled");
                }
                PreferenceManager.setWalletPin(appContext, newPin);
            } else if (!awaitVerifyPin(activity, existingPin)) {
                throw new RpcException(4001, "PIN verification failed");
            }
        }

        private Activity requireActivity() throws RpcException {
            Activity activity = unwrapActivity(webView != null ? webView.getContext() : null);
            if (activity == null || activity.isFinishing()) {
                throw new RpcException(-32603, "Unable to show wallet approval UI right now");
            }
            return activity;
        }

        private Activity unwrapActivity(Context context) {
            Context current = context;
            while (current instanceof ContextWrapper) {
                if (current instanceof Activity) {
                    return (Activity) current;
                }
                current = ((ContextWrapper) current).getBaseContext();
            }
            return current instanceof Activity ? (Activity) current : null;
        }

        private boolean awaitConfirmation(Activity activity, String title, String message) throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean finished = new AtomicBoolean(false);
            final boolean[] approved = new boolean[]{false};
            activity.runOnUiThread(() -> {
                ScrollView scrollView = new ScrollView(activity);
                TextView textView = new TextView(activity);
                int pad = dp(activity, 20);
                textView.setPadding(pad, pad, pad, pad);
                textView.setText(message);
                scrollView.addView(textView);
                new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle(title)
                        .setView(scrollView)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> completeDialog(finished, latch, approved, true))
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> completeDialog(finished, latch, approved, false))
                        .setOnCancelListener(dialog -> completeDialog(finished, latch, approved, false))
                        .show();
            });
            latch.await(APPROVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return approved[0];
        }

        private String awaitNewPin(Activity activity) throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean finished = new AtomicBoolean(false);
            final String[] result = new String[1];
            activity.runOnUiThread(() -> {
                LinearLayout container = new LinearLayout(activity);
                container.setOrientation(LinearLayout.VERTICAL);
                int pad = dp(activity, 20);
                container.setPadding(pad, pad, pad, pad);
                EditText first = new EditText(activity);
                first.setHint("Set PIN");
                first.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                EditText second = new EditText(activity);
                second.setHint("Confirm PIN");
                second.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                container.addView(first);
                container.addView(second);
                new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle("Set wallet PIN")
                        .setView(container)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            String p1 = first.getText().toString().trim();
                            String p2 = second.getText().toString().trim();
                            result[0] = p1.equals(p2) && !p1.isEmpty() ? p1 : null;
                            completeDialog(finished, latch, null, false);
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> completeDialog(finished, latch, null, false))
                        .setOnCancelListener(dialog -> completeDialog(finished, latch, null, false))
                        .show();
            });
            latch.await(APPROVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result[0];
        }

        private boolean awaitVerifyPin(Activity activity, String existingPin) throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean finished = new AtomicBoolean(false);
            final boolean[] approved = new boolean[]{false};
            activity.runOnUiThread(() -> {
                EditText pin = new EditText(activity);
                int pad = dp(activity, 20);
                pin.setPadding(pad, pad, pad, pad);
                pin.setHint("Wallet PIN");
                pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle("Enter wallet PIN")
                        .setView(pin)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> completeDialog(finished, latch, approved, existingPin.equals(pin.getText().toString().trim())))
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> completeDialog(finished, latch, approved, false))
                        .setOnCancelListener(dialog -> completeDialog(finished, latch, approved, false))
                        .show();
            });
            latch.await(APPROVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return approved[0];
        }

        private void completeDialog(AtomicBoolean finished, CountDownLatch latch, boolean[] approved, boolean value) {
            if (approved != null) {
                approved[0] = value;
            }
            if (finished.compareAndSet(false, true)) {
                latch.countDown();
            }
        }

        private int dp(Context context, int dp) {
            float density = context.getResources().getDisplayMetrics().density;
            return Math.max(dp, (int) (dp * density));
        }

        private JSONObject requireObject(JSONArray params, int index, String errorMessage) throws Exception {
            if (index >= params.length()) {
                throw new RpcException(-32602, errorMessage);
            }
            Object value = params.opt(index);
            if (value instanceof JSONObject) {
                return (JSONObject) value;
            }
            if (value instanceof String) {
                Object parsed = new JSONTokener((String) value).nextValue();
                if (parsed instanceof JSONObject) {
                    return (JSONObject) parsed;
                }
            }
            throw new RpcException(-32602, errorMessage);
        }

        private String objectToString(Object value) {
            if (value == null || value == JSONObject.NULL) {
                return "";
            }
            return value instanceof String ? (String) value : String.valueOf(value);
        }

        private String optString(JSONObject object, String key) {
            return object == null ? "" : safeTrim(object.optString(key, ""));
        }

        private String safeTrim(String value) {
            return value == null ? "" : value.trim();
        }

        private String normalizeAddress(String address) throws RpcException {
            String trimmed = safeTrim(address);
            if (trimmed.isEmpty()) {
                return "";
            }
            String normalized = trimmed.startsWith("0x") || trimmed.startsWith("0X") ? trimmed : "0x" + trimmed;
            if (!isHexAddress(normalized)) {
                throw new RpcException(-32602, "Invalid wallet address: " + trimmed);
            }
            return normalized;
        }

        private boolean isHexAddress(String value) {
            return value != null && value.matches("^(0x)?[0-9a-fA-F]{40}$");
        }

        private String normalizeHexData(String value) throws RpcException {
            String trimmed = safeTrim(value);
            if (trimmed.isEmpty()) {
                return "0x";
            }
            String normalized = trimmed.startsWith("0x") || trimmed.startsWith("0X") ? trimmed : "0x" + trimmed;
            String clean = Numeric.cleanHexPrefix(normalized);
            if ((clean.length() % 2) != 0 || !clean.matches("[0-9a-fA-F]*")) {
                throw new RpcException(-32602, "Invalid hex data");
            }
            return Numeric.prependHexPrefix(clean.toLowerCase(Locale.US));
        }

        private byte[] decodeData(String value) {
            String trimmed = safeTrim(value);
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                try {
                    return Numeric.hexStringToByteArray(trimmed);
                } catch (Exception ignore) {
                    return trimmed.getBytes(UTF_8);
                }
            }
            return trimmed.getBytes(UTF_8);
        }

        private String previewPayload(String payload) {
            String trimmed = payload == null ? "" : payload.trim();
            if (trimmed.isEmpty()) {
                return "Payload: (empty)";
            }
            if (trimmed.length() > 280) {
                trimmed = trimmed.substring(0, 280) + "…";
            }
            return "Payload:\n" + trimmed;
        }

        private BigInteger parseQuantity(Object raw, BigInteger defaultValue, boolean allowNegative) throws RpcException {
            if (raw == null || raw == JSONObject.NULL) {
                return defaultValue;
            }
            BigInteger value;
            if (raw instanceof Number) {
                value = new BigInteger(String.valueOf(raw));
            } else {
                String text = safeTrim(String.valueOf(raw));
                if (text.isEmpty()) {
                    return defaultValue;
                }
                try {
                    value = (text.startsWith("0x") || text.startsWith("0X")) ? Numeric.decodeQuantity(text) : new BigInteger(text);
                } catch (Exception e) {
                    throw new RpcException(-32602, "Invalid numeric value: " + text);
                }
            }
            if (!allowNegative && value.signum() < 0) {
                throw new RpcException(-32602, "Negative values are not allowed");
            }
            return value;
        }

        private long parseChainId(Object raw) throws RpcException {
            BigInteger chainId = parseQuantity(raw, null, false);
            if (chainId == null || chainId.signum() <= 0) {
                throw new RpcException(-32602, "Invalid chainId");
            }
            if (chainId.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new RpcException(-32602, "chainId is too large");
            }
            return chainId.longValue();
        }

        private String firstNonEmptyString(JSONArray array) {
            if (array == null) {
                return "";
            }
            for (int i = 0; i < array.length(); i++) {
                String value = safeTrim(array.optString(i, ""));
                if (!value.isEmpty()) {
                    return value;
                }
            }
            return "";
        }

        private boolean inferTestnet(String chainName, long chainId, String symbol, String rpcUrl) {
            String text = (safeTrim(chainName) + " " + safeTrim(symbol) + " " + safeTrim(rpcUrl)).toLowerCase(Locale.US);
            return text.contains("test") || text.contains("sepolia") || text.contains("goerli")
                    || text.contains("amoy") || text.contains("mumbai") || text.contains("fuji")
                    || text.contains("alfajores") || text.contains("devnet")
                    || chainId == 97L || chainId == 11155111L || chainId == 84532L || chainId == 80002L
                    || chainId == 421614L || chainId == 43113L || chainId == 44787L;
        }

        private String getOriginLabel() {
            String url = originUrl;
            if (url == null || url.trim().isEmpty()) {
                return "this dapp";
            }
            try {
                java.net.URI uri = new java.net.URI(url);
                String host = uri.getHost();
                if (host != null && !host.trim().isEmpty()) {
                    return host;
                }
            } catch (Exception ignore) {
            }
            return url;
        }

        private boolean isSecureWebOrigin() {
            return isHttpsUrl(originUrl);
        }

        private boolean isHttpsUrl(String value) {
            try {
                java.net.URI uri = new java.net.URI(value == null ? "" : value.trim());
                return "https".equalsIgnoreCase(uri.getScheme())
                        && uri.getHost() != null
                        && !uri.getHost().trim().isEmpty()
                        && uri.getUserInfo() == null;
            } catch (Exception e) {
                return false;
            }
        }

        private RpcException rpcError(Response.Error error, String fallback) {
            if (error == null) {
                return new RpcException(-32603, fallback);
            }
            String message = error.getMessage();
            return new RpcException(error.getCode(), message == null || message.trim().isEmpty() ? fallback : message);
        }

        private String formatNativeValue(BigInteger valueWei, String symbol) {
            BigDecimal value = Convert.fromWei(new BigDecimal(valueWei), Convert.Unit.ETHER);
            String amount = value.stripTrailingZeros().toPlainString();
            return (amount == null || amount.isEmpty() ? value.toPlainString() : amount) + " " + symbol;
        }

        private String formatGwei(BigInteger gasPriceWei) {
            BigDecimal gwei = new BigDecimal(gasPriceWei).divide(new BigDecimal("1000000000"));
            return gwei.stripTrailingZeros().toPlainString();
        }

        private String signatureToHex(Sign.SignatureData signature) {
            byte[] signed = new byte[65];
            System.arraycopy(signature.getR(), 0, signed, 0, 32);
            System.arraycopy(signature.getS(), 0, signed, 32, 32);
            signed[64] = signature.getV()[0];
            return Numeric.toHexString(signed);
        }

        private String success(Object result) {
            try {
                JSONObject response = new JSONObject();
                response.put("result", result == null ? JSONObject.NULL : result);
                return response.toString();
            } catch (JSONException e) {
                return "{\"error\":{\"code\":-32603,\"message\":\"Wallet bridge failed\"}}";
            }
        }

        private String error(int code, String message) {
            try {
                JSONObject error = new JSONObject();
                error.put("code", code);
                error.put("message", message);
                return error(error);
            } catch (JSONException e) {
                return "{\"error\":{\"code\":-32603,\"message\":\"Wallet bridge failed\"}}";
            }
        }

        private String error(JSONObject error) {
            try {
                JSONObject response = new JSONObject();
                response.put("error", error);
                return response.toString();
            } catch (JSONException e) {
                return "{\"error\":{\"code\":-32603,\"message\":\"Wallet bridge failed\"}}";
            }
        }
    }

    private static byte[] signEip155(RawTransaction rawTx, long chainId, Credentials credentials) {
        byte[] encoded = encodeForSign(rawTx, chainId);
        Sign.SignatureData sig = Sign.signMessage(encoded, credentials.getEcKeyPair());
        int recId = (sig.getV()[0] & 0xFF) - 27;
        long v = (chainId * 2L) + 35L + recId;
        return encodeWithSignature(rawTx, BigInteger.valueOf(v), sig);
    }

    private static byte[] encodeForSign(RawTransaction rawTx, long chainId) {
        return RlpEncoder.encode(new RlpList(asRlpValues(rawTx, BigInteger.valueOf(chainId))));
    }

    private static byte[] encodeWithSignature(RawTransaction rawTx, BigInteger v, Sign.SignatureData sig) {
        List<RlpType> values = asRlpBaseValues(rawTx);
        values.add(RlpString.create(v));
        values.add(RlpString.create(new BigInteger(1, sig.getR())));
        values.add(RlpString.create(new BigInteger(1, sig.getS())));
        return RlpEncoder.encode(new RlpList(values));
    }

    private static List<RlpType> asRlpValues(RawTransaction rawTx, BigInteger chainIdOrV) {
        List<RlpType> result = asRlpBaseValues(rawTx);
        result.add(RlpString.create(chainIdOrV));
        result.add(RlpString.create(BigInteger.ZERO));
        result.add(RlpString.create(BigInteger.ZERO));
        return result;
    }

    private static List<RlpType> asRlpBaseValues(RawTransaction rawTx) {
        List<RlpType> result = new ArrayList<>();
        result.add(RlpString.create(rawTx.getNonce()));
        result.add(RlpString.create(rawTx.getGasPrice()));
        result.add(RlpString.create(rawTx.getGasLimit()));
        String to = rawTx.getTo();
        result.add(to == null || to.length() == 0 ? RlpString.create("") : RlpString.create(Numeric.hexStringToByteArray(to)));
        result.add(RlpString.create(rawTx.getValue()));
        String data = rawTx.getData();
        result.add(RlpString.create(Numeric.hexStringToByteArray(data == null ? "" : data)));
        return result;
    }

    private static final class RpcException extends Exception {
        private final int code;

        private RpcException(int code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static final class SignPayload {
        private final String address;
        private final byte[] data;
        private final String preview;

        private SignPayload(String address, byte[] data, String preview) {
            this.address = address;
            this.data = data;
            this.preview = preview;
        }
    }

    private static final class TypedDataRequest {
        private final String address;
        private final Object payload;

        private TypedDataRequest(String address, Object payload) {
            this.address = address;
            this.payload = payload;
        }
    }
}
