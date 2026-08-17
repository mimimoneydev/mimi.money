package com.money.mimi.fragments.home;

import android.app.AlertDialog;
import android.content.Context;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.wallet.TokenInfo;
import com.money.mimi.wallet.WalletConfig;
import com.money.mimi.wallet.WalletLogoResolver;
import com.money.mimi.wallet.Web3Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Coins tab: native balance + imported ERC-20 tokens.
 */
public class WalletCoinsFragment extends Fragment {
    private static final OkHttpClient HTTP = new OkHttpClient();
    private static final int MAX_SAVED_TOKENS = 100;
    private static final int MAX_TOKEN_TEXT_LENGTH = 80;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet_coins, container, false);
        ListView listView = view.findViewById(R.id.coins_list);
        Button btnAdd = view.findViewById(R.id.btn_add_token);
        Button btnDelete = view.findViewById(R.id.btn_delete_token);
        final ArrayList<WalletAssetRow> items = new ArrayList<>();
        items.add(WalletAssetRow.message(getString(R.string.wallet_loading)));
        final WalletAssetAdapter adapter = new WalletAssetAdapter(getContext(), items);
        listView.setAdapter(adapter);

        updateDeleteButtonVisibility(btnDelete);
        btnAdd.setOnClickListener(v -> showAddTokenDialog(adapter, items, btnDelete));
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteTokenDialog(adapter, items, btnDelete));
        }

        loadBalances(adapter, items);
        return view;
    }

    private void loadBalances(final WalletAssetAdapter adapter, final ArrayList<WalletAssetRow> items) {
        if (getContext() == null) return;
        final Context app = getContext().getApplicationContext();
        final String sNoWallet = app.getString(R.string.wallet_no_wallet_generate_first);
        final String sTokenFmt = app.getString(R.string.wallet_token_balance_line);
        final String sTokenErr = app.getString(R.string.wallet_token_balance_error);
        final String sError = app.getString(R.string.wallet_balance_unavailable);
        new Thread(() -> {
            try {
                String address = PreferenceManager.getWalletAddress(app);
                if (TextUtils.isEmpty(address)) {
                    postSet(adapter, items, Arrays.asList(WalletAssetRow.message(sNoWallet)));
                    return;
                }
                List<WalletAssetRow> lines = new ArrayList<>();
                BigInteger nativeBalanceWei = fetchNativeBalanceWei(app, address);
                BigDecimal ether = Convert.fromWei(new BigDecimal(nativeBalanceWei), Convert.Unit.ETHER);
                WalletConfig.NetworkDefinition network = WalletConfig.getActiveNetwork(app);
                String nativeValue = ether.stripTrailingZeros().toPlainString();
                lines.add(new WalletAssetRow(
                        WalletConfig.getCurrencySymbol(app),
                        network.displayName,
                        nativeValue,
                        null,
                        WalletLogoResolver.getNetworkLogoRes(network.key)
                ));

                ArrayList<TokenInfo> tokens = getSavedTokens(app);
                boolean updatedLogos = false;
                for (TokenInfo t : tokens) {
                    if (t == null || TextUtils.isEmpty(t.address)) continue;
                    try {
                        if (TextUtils.isEmpty(t.logoUrl)) {
                            t.logoUrl = tokenMetadataImageUrl(t.address);
                            updatedLogos = updatedLogos || !TextUtils.isEmpty(t.logoUrl);
                        }
                        BigInteger raw = erc20BalanceOf(address, t.address);
                        BigDecimal denom = new BigDecimal(raw);
                        BigDecimal div = BigDecimal.ONE;
                        for (int i = 0; i < t.decimals; i++) div = div.multiply(BigDecimal.TEN);
                        BigDecimal normalized = denom.divide(div);
                        lines.add(new WalletAssetRow(
                                t.symbol,
                                TextUtils.isEmpty(t.name) ? t.address : t.name,
                                normalized.stripTrailingZeros().toPlainString(),
                                t.logoUrl,
                                R.drawable.ic_network_generic
                        ));
                    } catch (Exception ignore) {
                        lines.add(new WalletAssetRow(
                                t.symbol,
                                TextUtils.isEmpty(t.name) ? t.address : t.name,
                                String.format(sTokenErr, t.symbol),
                                t.logoUrl,
                                R.drawable.ic_network_generic
                        ));
                    }
                }
                if (updatedLogos) {
                    PreferenceManager.setWalletTokens(app, tokens);
                }
                postSet(adapter, items, lines);
            } catch (Exception e) {
                postSet(adapter, items, Arrays.asList(WalletAssetRow.message(sError)));
            }
        }).start();
    }

    private BigInteger fetchNativeBalanceWei(Context app, String address) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("jsonrpc", "2.0");
        payload.put("id", 1);
        payload.put("method", "eth_getBalance");
        payload.put("params", new JSONArray().put(address).put("latest"));

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                payload.toString()
        );
        Request request = new Request.Builder()
                .url(WalletConfig.getRpcUrl(app))
                .post(body)
                .build();
        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Balance RPC HTTP " + response.code());
            }
            String rawBody = response.body() != null ? response.body().string() : "";
            JSONObject rpcResponse = new JSONObject(rawBody);
            if (rpcResponse.has("error") && !rpcResponse.isNull("error")) {
                JSONObject error = rpcResponse.optJSONObject("error");
                throw new IllegalStateException(error != null
                        ? error.optString("message", "Balance RPC returned an error")
                        : String.valueOf(rpcResponse.get("error")));
            }
            String result = rpcResponse.optString("result", "");
            if (TextUtils.isEmpty(result)) {
                throw new IllegalStateException("Balance RPC returned no balance");
            }
            return Numeric.decodeQuantity(result);
        }
    }

    private void showAddTokenDialog(final WalletAssetAdapter adapter, final ArrayList<WalletAssetRow> items, final Button deleteButton) {
        EditText input = new EditText(getContext());
        input.setHint(getString(R.string.wallet_token_contract_hint));
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.wallet_import_token_title))
                .setView(input)
                .setPositiveButton(getString(R.string.wallet_action_add), (d, which) -> {
                    String addr = input.getText().toString().trim();
                    if (!isValidEvmAddress(addr)) {
                        Toast.makeText(getContext(), getString(R.string.wallet_failed_add_token), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addr = normalizeEvmAddress(addr);
                    final Context app = getContext().getApplicationContext();
                    final String tokenAddress = addr;
                    new Thread(() -> {
                        try {
                            String symbol = cleanTokenText(erc20Symbol(tokenAddress));
                            String name = cleanTokenText(erc20Name(tokenAddress));
                            int decimals = erc20Decimals(tokenAddress);
                            String logoUrl = safeRemoteUrl(tokenMetadataImageUrl(tokenAddress));
                            ArrayList<TokenInfo> tokens = getSavedTokens(app);
                            if (tokens.size() >= MAX_SAVED_TOKENS) {
                                throw new IllegalStateException("Too many saved tokens");
                            }
                            // Prevent dup
                            for (TokenInfo t : tokens) {
                                if (t != null && t.address != null && t.address.equalsIgnoreCase(tokenAddress)) {
                                    return;
                                }
                            }
                            tokens.add(new TokenInfo(tokenAddress, symbol, name, decimals, logoUrl));
                            PreferenceManager.setWalletTokens(app, tokens);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> updateDeleteButtonVisibility(deleteButton));
                            }
                            loadBalances(adapter, items);
                        } catch (Exception ex) {
                            if (getActivity() != null) getActivity().runOnUiThread(() ->
                                    Toast.makeText(app, app.getString(R.string.wallet_failed_add_token), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDeleteTokenDialog(final WalletAssetAdapter adapter, final ArrayList<WalletAssetRow> items, final Button deleteButton) {
        if (getContext() == null) return;
        Context app = getContext().getApplicationContext();
        ArrayList<TokenInfo> tokens = getSavedTokens(app);
        if (tokens.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.wallet_delete_token_empty), Toast.LENGTH_SHORT).show();
            updateDeleteButtonVisibility(deleteButton);
            return;
        }

        boolean[] checked = new boolean[tokens.size()];
        ListView tokenList = new ListView(getContext());
        tokenList.setDivider(null);
        tokenList.setAdapter(new DeleteTokenAdapter(getContext(), tokens, checked));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.wallet_delete_token_title))
                .setView(tokenList)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        tokenList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= checked.length) return;
            checked[position] = !checked[position];
            ((ArrayAdapter<?>) parent.getAdapter()).notifyDataSetChanged();
        });

        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean removedAny = false;
            for (int i = checked.length - 1; i >= 0; i--) {
                if (checked[i]) {
                    tokens.remove(i);
                    removedAny = true;
                }
            }
            if (!removedAny) {
                Toast.makeText(getContext(), getString(R.string.wallet_delete_token_none_selected), Toast.LENGTH_SHORT).show();
                return;
            }
            PreferenceManager.setWalletTokens(app, tokens);
            Toast.makeText(getContext(), getString(R.string.wallet_delete_token_removed), Toast.LENGTH_SHORT).show();
            updateDeleteButtonVisibility(deleteButton);
            loadBalances(adapter, items);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private ArrayList<TokenInfo> getSavedTokens(Context app) {
        ArrayList<TokenInfo> saved = PreferenceManager.getWalletTokens(app);
        ArrayList<TokenInfo> valid = new ArrayList<>();
        for (TokenInfo token : saved) {
            TokenInfo normalized = normalizeToken(token);
            if (normalized != null) {
                valid.add(normalized);
            }
        }
        if (valid.size() != saved.size()) {
            PreferenceManager.setWalletTokens(app, valid);
        }
        return valid;
    }

    private void updateDeleteButtonVisibility(Button deleteButton) {
        if (deleteButton == null || getContext() == null) return;
        ArrayList<TokenInfo> tokens = getSavedTokens(getContext().getApplicationContext());
        deleteButton.setVisibility(tokens.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String formatTokenLabel(TokenInfo token) {
        if (token == null) return "";
        String symbol = token.symbol;
        String name = token.name;
        if (!TextUtils.isEmpty(symbol) && !TextUtils.isEmpty(name) && !symbol.equalsIgnoreCase(name)) {
            return symbol + " (" + name + ")";
        }
        if (!TextUtils.isEmpty(symbol)) return symbol;
        if (!TextUtils.isEmpty(name)) return name;
        if (!TextUtils.isEmpty(token.address)) return token.address;
        return getString(R.string.wallet_add_token_button);
    }

    private void postSet(final WalletAssetAdapter adapter, final ArrayList<WalletAssetRow> items, final List<WalletAssetRow> lines) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            items.clear();
            items.addAll(lines);
            adapter.notifyDataSetChanged();
        });
    }

    // ---------------- ERC-20 helpers -----------------
    private String erc20Symbol(String contract) throws Exception {
        Function f = new Function("symbol", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>(){}));
        List<org.web3j.abi.datatypes.Type> out = callFunction(contract, f);
        return out.isEmpty() ? "?" : ((Utf8String) out.get(0)).getValue();
    }

    private String erc20Name(String contract) throws Exception {
        Function f = new Function("name", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>(){}));
        List<org.web3j.abi.datatypes.Type> out = callFunction(contract, f);
        return out.isEmpty() ? contract : ((Utf8String) out.get(0)).getValue();
    }

    private int erc20Decimals(String contract) throws Exception {
        try {
            Function f = new Function("decimals", Arrays.asList(), Arrays.asList(new TypeReference<Uint8>(){}));
            List<org.web3j.abi.datatypes.Type> out = callFunction(contract, f);
            if (!out.isEmpty()) return ((Uint8) out.get(0)).getValue().intValue();
        } catch (Exception ignore) {}
        Function f2 = new Function("decimals", Arrays.asList(), Arrays.asList(new TypeReference<Uint256>(){}));
        List<org.web3j.abi.datatypes.Type> out2 = callFunction(contract, f2);
        return out2.isEmpty() ? 18 : ((Uint256) out2.get(0)).getValue().intValue();
    }

    private BigInteger erc20BalanceOf(String owner, String contract) throws Exception {
        Function f = new Function("balanceOf", Arrays.asList(new Address(owner)), Arrays.asList(new TypeReference<Uint256>(){}));
        List<org.web3j.abi.datatypes.Type> out = callFunction(contract, f);
        return out.isEmpty() ? BigInteger.ZERO : ((Uint256) out.get(0)).getValue();
    }

    private String tokenMetadataImageUrl(String contract) {
        try {
            Function f = new Function("tokenURI", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>(){}));
            List<org.web3j.abi.datatypes.Type> out = callFunction(contract, f);
            if (out.isEmpty()) return null;
            return imageUrlFromMetadataUri(((Utf8String) out.get(0)).getValue());
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String imageUrlFromMetadataUri(String uri) {
        try {
            String resolved = resolveMetadataUri(uri);
            if (TextUtils.isEmpty(resolved)) return null;
            String json;
            if (resolved.startsWith("data:application/json;base64,")) {
                byte[] decoded = android.util.Base64.decode(resolved.substring("data:application/json;base64,".length()), android.util.Base64.DEFAULT);
                json = new String(decoded, "UTF-8");
            } else if (resolved.startsWith("data:application/json,")) {
                json = java.net.URLDecoder.decode(resolved.substring("data:application/json,".length()), "UTF-8");
            } else if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
                Request request = new Request.Builder().url(resolved).build();
                try (Response response = HTTP.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) return null;
                    json = response.body().string();
                }
            } else {
                return null;
            }
            JSONObject object = new JSONObject(json);
            String image = object.optString("image", object.optString("image_url", object.optString("logo", object.optString("logoURI", ""))));
            return safeRemoteUrl(resolveMetadataUri(image));
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String resolveMetadataUri(String uri) {
        if (uri == null) return null;
        String trimmed = uri.trim();
        if (trimmed.startsWith("ipfs://ipfs/")) {
            return "https://ipfs.io/ipfs/" + trimmed.substring("ipfs://ipfs/".length());
        }
        if (trimmed.startsWith("ipfs://")) {
            return "https://ipfs.io/ipfs/" + trimmed.substring("ipfs://".length());
        }
        return trimmed;
    }

    private static TokenInfo normalizeToken(TokenInfo token) {
        if (token == null || !isValidEvmAddress(token.address)) {
            return null;
        }
        int decimals = token.decimals < 0 || token.decimals > 36 ? 18 : token.decimals;
        return new TokenInfo(
                normalizeEvmAddress(token.address),
                cleanTokenText(token.symbol),
                cleanTokenText(token.name),
                decimals,
                safeRemoteUrl(token.logoUrl)
        );
    }

    private static boolean isValidEvmAddress(String address) {
        String value = address == null ? "" : address.trim();
        return value.matches("(?i)^0x[0-9a-f]{40}$");
    }

    private static String normalizeEvmAddress(String address) {
        return Numeric.prependHexPrefix(Numeric.cleanHexPrefix(address == null ? "" : address.trim()).toLowerCase());
    }

    private static String cleanTokenText(String value) {
        String clean = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return clean.length() <= MAX_TOKEN_TEXT_LENGTH ? clean : clean.substring(0, MAX_TOKEN_TEXT_LENGTH);
    }

    private static String safeRemoteUrl(String value) {
        try {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.length() > 1024) return null;
            android.net.Uri uri = android.net.Uri.parse(trimmed);
            String scheme = uri.getScheme();
            if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty()
                    && uri.getUserInfo() == null) {
                return trimmed;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private List<org.web3j.abi.datatypes.Type> callFunction(String contract, Function function) throws Exception {
        String from = null;
        Context app = null;
        try {
            Context ctx = getContext();
            if (ctx != null) {
                app = ctx.getApplicationContext();
                from = PreferenceManager.getWalletAddress(app);
            }
        } catch (Exception ignore) {}
        Web3j web3 = Web3Provider.get(app);
        String data = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(from, contract, data),
                DefaultBlockParameterName.LATEST
        ).send();
        String value = response.getValue();
        return FunctionReturnDecoder.decode(value, function.getOutputParameters());
    }

    private static class WalletAssetRow {
        final String title;
        final String subtitle;
        final String value;
        final String imageUrl;
        final int fallbackLogoRes;

        WalletAssetRow(String title, String subtitle, String value, String imageUrl, int fallbackLogoRes) {
            this.title = title;
            this.subtitle = subtitle;
            this.value = value;
            this.imageUrl = imageUrl;
            this.fallbackLogoRes = fallbackLogoRes;
        }

        static WalletAssetRow message(String message) {
            return new WalletAssetRow(message, "", "", null, R.drawable.ic_network_generic);
        }
    }

    private static class WalletAssetAdapter extends ArrayAdapter<WalletAssetRow> {
        WalletAssetAdapter(Context context, List<WalletAssetRow> items) {
            super(context, R.layout.item_wallet_asset_row, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_asset_row, parent, false);
            }
            WalletAssetRow row = getItem(position);
            ImageView logo = view.findViewById(R.id.img_asset_logo);
            TextView title = view.findViewById(R.id.txt_asset_title);
            TextView subtitle = view.findViewById(R.id.txt_asset_subtitle);
            TextView value = view.findViewById(R.id.txt_asset_value);
            if (row != null) {
                title.setText(row.title);
                subtitle.setText(row.subtitle);
                subtitle.setVisibility(TextUtils.isEmpty(row.subtitle) ? View.GONE : View.VISIBLE);
                value.setText(row.value);
                value.setVisibility(TextUtils.isEmpty(row.value) ? View.GONE : View.VISIBLE);
                if (!TextUtils.isEmpty(row.imageUrl)) {
                    Glide.with(getContext()).load(row.imageUrl).placeholder(row.fallbackLogoRes).error(row.fallbackLogoRes).into(logo);
                } else {
                    logo.setImageResource(row.fallbackLogoRes);
                }
            }
            return view;
        }
    }

    private class DeleteTokenAdapter extends ArrayAdapter<TokenInfo> {
        private final boolean[] checked;

        DeleteTokenAdapter(Context context, List<TokenInfo> items, boolean[] checked) {
            super(context, R.layout.item_wallet_token_delete_row, items);
            this.checked = checked;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_token_delete_row, parent, false);
            }
            TokenInfo token = getItem(position);
            ImageView logo = view.findViewById(R.id.img_delete_token_logo);
            TextView title = view.findViewById(R.id.txt_delete_token_title);
            TextView subtitle = view.findViewById(R.id.txt_delete_token_subtitle);
            CheckBox checkBox = view.findViewById(R.id.check_delete_token);

            if (token != null) {
                title.setText(formatTokenLabel(token));
                subtitle.setText(token.address);
                if (!TextUtils.isEmpty(token.logoUrl)) {
                    Glide.with(getContext()).load(token.logoUrl).placeholder(R.drawable.ic_network_generic).error(R.drawable.ic_network_generic).into(logo);
                } else {
                    logo.setImageResource(R.drawable.ic_network_generic);
                }
            }
            checkBox.setChecked(position >= 0 && position < checked.length && checked[position]);
            return view;
        }
    }
}
