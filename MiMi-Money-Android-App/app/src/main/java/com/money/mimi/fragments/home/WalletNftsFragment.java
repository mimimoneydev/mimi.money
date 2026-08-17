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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.wallet.NftCollection;
import com.money.mimi.wallet.Web3Provider;

import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WalletNftsFragment extends Fragment {
    private static final OkHttpClient HTTP = new OkHttpClient();
    private static final int MAX_SAVED_NFT_COLLECTIONS = 100;
    private static final int MAX_NFT_TEXT_LENGTH = 80;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet_nfts, container, false);
        TextView empty = view.findViewById(R.id.tv_nfts_empty);
        GridView grid = view.findViewById(R.id.nft_grid);
        Button btnAdd = view.findViewById(R.id.btn_add_nft);
        Button btnDelete = view.findViewById(R.id.btn_delete_nft);

        final ArrayList<NftRow> items = new ArrayList<>();
        final NftAdapter adapter = new NftAdapter(getContext(), items);
        grid.setAdapter(adapter);

        updateDeleteButtonVisibility(btnDelete);
        btnAdd.setOnClickListener(v -> showAddNftDialog(adapter, items, empty, btnDelete));
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteNftDialog(adapter, items, empty, btnDelete));
        }

        loadNfts(adapter, items, empty);
        return view;
    }

    private void loadNfts(final NftAdapter adapter, final ArrayList<NftRow> items, final TextView empty) {
        if (getContext() == null) return;
        final Context app = getContext().getApplicationContext();
        new Thread(() -> {
            try {
                String owner = PreferenceManager.getWalletAddress(app);
                if (TextUtils.isEmpty(owner)) {
                    postSet(adapter, items, empty, new ArrayList<>(), getString(R.string.wallet_no_wallet_generate_first));
                    return;
                }
                ArrayList<NftCollection> collections = PreferenceManager.getWalletNfts(app);
                ArrayList<NftRow> lines = new ArrayList<>();
                for (NftCollection c : collections) {
                    if (c == null || TextUtils.isEmpty(c.address)) continue;
                    try {
                        BigInteger bal = erc721BalanceOf(owner, c.address);
                        for (BigInteger i = BigInteger.ZERO; i.compareTo(bal) < 0; i = i.add(BigInteger.ONE)) {
                            BigInteger tokenId = erc721TokenOfOwnerByIndex(owner, c.address, i);
                            String sym = TextUtils.isEmpty(c.symbol) ? getString(R.string.wallet_nft_fallback_symbol) : c.symbol;
                            String imageUrl = erc721TokenImageUrl(c.address, tokenId);
                            if (TextUtils.isEmpty(imageUrl)) imageUrl = c.logoUrl;
                            lines.add(new NftRow(
                                    getString(R.string.wallet_nft_item_format, sym, tokenId.toString()),
                                    TextUtils.isEmpty(c.name) ? c.address : c.name,
                                    imageUrl
                            ));
                        }
                    } catch (Exception ignore) {}
                }
                postSet(adapter, items, empty, lines, getString(R.string.wallet_empty_nfts));
            } catch (Exception e) {
                postSet(adapter, items, empty, new ArrayList<>(), getString(R.string.wallet_empty_nfts));
            }
        }).start();
    }

    private void showAddNftDialog(final NftAdapter adapter, final ArrayList<NftRow> items, final TextView empty, final Button deleteButton) {
        EditText input = new EditText(getContext());
        input.setHint(getString(R.string.wallet_nft_contract_hint));
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.wallet_add_nft_title))
                .setView(input)
                .setPositiveButton(getString(R.string.wallet_action_add), (d, which) -> {
                    String addr = input.getText().toString().trim();
                    if (!isValidEvmAddress(addr)) {
                        Toast.makeText(getContext(), getString(R.string.wallet_failed_add_nft), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addr = normalizeEvmAddress(addr);
                    final String contractAddress = addr;
                    new Thread(() -> {
                        try {
                            if (getContext() == null) return;
                            Context app = getContext().getApplicationContext();
                            String name = cleanNftText(erc721Name(contractAddress));
                            String symbol = cleanNftText(erc721Symbol(contractAddress));
                            String logoUrl = safeRemoteUrl(firstOwnedNftImageUrl(app, contractAddress));
                            ArrayList<NftCollection> list = PreferenceManager.getWalletNfts(app);
                            if (list.size() >= MAX_SAVED_NFT_COLLECTIONS) {
                                throw new IllegalStateException("Too many saved NFT collections");
                            }
                            for (NftCollection c : list) {
                                if (c != null && c.address != null && c.address.equalsIgnoreCase(contractAddress)) return;
                            }
                            list.add(new NftCollection(contractAddress, name, symbol, logoUrl));
                            PreferenceManager.setWalletNfts(app, list);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> updateDeleteButtonVisibility(deleteButton));
                            }
                            loadNfts(adapter, items, empty);
                        } catch (Exception ex) {
                            if (getActivity() != null) getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), getString(R.string.wallet_failed_add_nft), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDeleteNftDialog(final NftAdapter adapter, final ArrayList<NftRow> items, final TextView empty, final Button deleteButton) {
        if (getContext() == null) return;
        Context app = getContext().getApplicationContext();
        ArrayList<NftCollection> collections = getSavedNftCollections(app);
        if (collections.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.wallet_delete_nft_empty), Toast.LENGTH_SHORT).show();
            updateDeleteButtonVisibility(deleteButton);
            return;
        }

        boolean[] checked = new boolean[collections.size()];
        ListView listView = new ListView(getContext());
        listView.setDivider(null);
        listView.setAdapter(new DeleteNftAdapter(getContext(), collections, checked));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.wallet_delete_nft_title))
                .setView(listView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= checked.length) return;
            checked[position] = !checked[position];
            ((ArrayAdapter<?>) parent.getAdapter()).notifyDataSetChanged();
        });

        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean removedAny = false;
            for (int i = checked.length - 1; i >= 0; i--) {
                if (checked[i]) {
                    collections.remove(i);
                    removedAny = true;
                }
            }
            if (!removedAny) {
                Toast.makeText(getContext(), getString(R.string.wallet_delete_nft_none_selected), Toast.LENGTH_SHORT).show();
                return;
            }
            PreferenceManager.setWalletNfts(app, collections);
            Toast.makeText(getContext(), getString(R.string.wallet_delete_nft_removed), Toast.LENGTH_SHORT).show();
            updateDeleteButtonVisibility(deleteButton);
            loadNfts(adapter, items, empty);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private ArrayList<NftCollection> getSavedNftCollections(Context app) {
        ArrayList<NftCollection> saved = PreferenceManager.getWalletNfts(app);
        ArrayList<NftCollection> valid = new ArrayList<>();
        for (NftCollection collection : saved) {
            NftCollection normalized = normalizeCollection(collection);
            if (normalized != null) {
                valid.add(normalized);
            }
        }
        if (valid.size() != saved.size()) {
            PreferenceManager.setWalletNfts(app, valid);
        }
        return valid;
    }

    private void updateDeleteButtonVisibility(Button deleteButton) {
        if (deleteButton == null || getContext() == null) return;
        ArrayList<NftCollection> collections = getSavedNftCollections(getContext().getApplicationContext());
        deleteButton.setVisibility(collections.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String firstOwnedNftImageUrl(Context app, String contract) {
        try {
            String owner = PreferenceManager.getWalletAddress(app);
            if (TextUtils.isEmpty(owner)) return null;
            BigInteger balance = erc721BalanceOf(owner, contract);
            if (balance.compareTo(BigInteger.ZERO) <= 0) return null;
            BigInteger tokenId = erc721TokenOfOwnerByIndex(owner, contract, BigInteger.ZERO);
            return erc721TokenImageUrl(contract, tokenId);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String formatCollectionLabel(NftCollection collection) {
        if (collection == null) return "";
        String title = !TextUtils.isEmpty(collection.name) ? collection.name : collection.address;
        if (TextUtils.isEmpty(title)) title = getString(R.string.wallet_nft_fallback_symbol);
        String symbol = collection.symbol;
        if (!TextUtils.isEmpty(symbol) && !symbol.equalsIgnoreCase(title)) {
            title = title + " (" + symbol + ")";
        }
        return title;
    }

    private void postSet(final NftAdapter adapter, final ArrayList<NftRow> items, final TextView empty, final ArrayList<NftRow> lines, final String emptyText) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            items.clear();
            items.addAll(lines);
            adapter.notifyDataSetChanged();
            if (lines.isEmpty()) {
                empty.setText(emptyText);
                empty.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.GONE);
            }
        });
    }

    // ------------- ERC-721 helpers -------------
    private BigInteger erc721BalanceOf(String owner, String contract) throws Exception {
        Function f = new Function("balanceOf", Arrays.asList(new Address(owner)), Arrays.asList(new TypeReference<Uint256>(){}));
        return asUint256(contract, f);
    }

    private BigInteger erc721TokenOfOwnerByIndex(String owner, String contract, BigInteger index) throws Exception {
        Function f = new Function("tokenOfOwnerByIndex", Arrays.asList(new Address(owner), new Uint256(index)), Arrays.asList(new TypeReference<Uint256>(){}));
        return asUint256(contract, f);
    }

    private String erc721Name(String contract) throws Exception {
        Function f = new Function("name", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>(){}));
        return asUtf8(contract, f);
    }

    private String erc721Symbol(String contract) throws Exception {
        Function f = new Function("symbol", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>(){}));
        return asUtf8(contract, f);
    }

    private String erc721TokenImageUrl(String contract, BigInteger tokenId) {
        try {
            Function f = new Function("tokenURI", Arrays.asList(new Uint256(tokenId)), Arrays.asList(new TypeReference<Utf8String>(){}));
            String uri = asUtf8(contract, f);
            return imageUrlFromMetadataUri(uri);
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
            String image = object.optString("image", object.optString("image_url", object.optString("animation_url", "")));
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

    private static NftCollection normalizeCollection(NftCollection collection) {
        if (collection == null || !isValidEvmAddress(collection.address)) {
            return null;
        }
        return new NftCollection(
                normalizeEvmAddress(collection.address),
                cleanNftText(collection.name),
                cleanNftText(collection.symbol),
                safeRemoteUrl(collection.logoUrl)
        );
    }

    private static boolean isValidEvmAddress(String address) {
        String value = address == null ? "" : address.trim();
        return value.matches("(?i)^0x[0-9a-f]{40}$");
    }

    private static String normalizeEvmAddress(String address) {
        return Numeric.prependHexPrefix(Numeric.cleanHexPrefix(address == null ? "" : address.trim()).toLowerCase());
    }

    private static String cleanNftText(String value) {
        String clean = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return clean.length() <= MAX_NFT_TEXT_LENGTH ? clean : clean.substring(0, MAX_NFT_TEXT_LENGTH);
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

    private BigInteger asUint256(String contract, Function f) throws Exception {
        Context app = getContext() == null ? null : getContext().getApplicationContext();
        Web3j web3 = Web3Provider.get(app);
        String from = app == null ? null : PreferenceManager.getWalletAddress(app);
        String data = FunctionEncoder.encode(f);
        EthCall response = web3.ethCall(Transaction.createEthCallTransaction(from, contract, data), DefaultBlockParameterName.LATEST).send();
        String value = response.getValue();
        try {
            java.util.List<org.web3j.abi.datatypes.Type> out = FunctionReturnDecoder.decode(value, f.getOutputParameters());
            if (out.isEmpty()) return BigInteger.ZERO;
            return ((Uint256) out.get(0)).getValue();
        } catch (Exception e) { return BigInteger.ZERO; }
    }

    private String asUtf8(String contract, Function f) throws Exception {
        Context app = getContext() == null ? null : getContext().getApplicationContext();
        Web3j web3 = Web3Provider.get(app);
        String from = app == null ? null : PreferenceManager.getWalletAddress(app);
        String data = FunctionEncoder.encode(f);
        EthCall response = web3.ethCall(Transaction.createEthCallTransaction(from, contract, data), DefaultBlockParameterName.LATEST).send();
        String value = response.getValue();
        java.util.List<org.web3j.abi.datatypes.Type> out = FunctionReturnDecoder.decode(value, f.getOutputParameters());
        if (out.isEmpty()) return "";
        return ((Utf8String) out.get(0)).getValue();
    }

    private static class NftRow {
        final String title;
        final String subtitle;
        final String imageUrl;

        NftRow(String title, String subtitle, String imageUrl) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageUrl = imageUrl;
        }
    }

    private static class NftAdapter extends ArrayAdapter<NftRow> {
        NftAdapter(Context context, List<NftRow> items) {
            super(context, R.layout.item_wallet_nft_row, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_nft_row, parent, false);
            }
            NftRow row = getItem(position);
            ImageView logo = view.findViewById(R.id.img_nft_logo);
            TextView title = view.findViewById(R.id.txt_nft_title);
            TextView subtitle = view.findViewById(R.id.txt_nft_subtitle);
            if (row != null) {
                title.setText(row.title);
                subtitle.setText(row.subtitle);
                if (!TextUtils.isEmpty(row.imageUrl)) {
                    Glide.with(getContext()).load(row.imageUrl).placeholder(R.drawable.ic_wallet_nft_generic).error(R.drawable.ic_wallet_nft_generic).into(logo);
                } else {
                    logo.setImageResource(R.drawable.ic_wallet_nft_generic);
                }
            }
            return view;
        }
    }

    private class DeleteNftAdapter extends ArrayAdapter<NftCollection> {
        private final boolean[] checked;

        DeleteNftAdapter(Context context, List<NftCollection> items, boolean[] checked) {
            super(context, R.layout.item_wallet_nft_delete_row, items);
            this.checked = checked;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_nft_delete_row, parent, false);
            }
            NftCollection collection = getItem(position);
            ImageView logo = view.findViewById(R.id.img_delete_nft_logo);
            TextView title = view.findViewById(R.id.txt_delete_nft_title);
            TextView subtitle = view.findViewById(R.id.txt_delete_nft_subtitle);
            CheckBox checkBox = view.findViewById(R.id.check_delete_nft);

            if (collection != null) {
                title.setText(formatCollectionLabel(collection));
                subtitle.setText(collection.address);
                if (!TextUtils.isEmpty(collection.logoUrl)) {
                    Glide.with(getContext()).load(collection.logoUrl).placeholder(R.drawable.ic_wallet_nft_generic).error(R.drawable.ic_wallet_nft_generic).into(logo);
                } else {
                    logo.setImageResource(R.drawable.ic_wallet_nft_generic);
                }
            }
            checkBox.setChecked(position >= 0 && position < checked.length && checked[position]);
            return view;
        }
    }
}
