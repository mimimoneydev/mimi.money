package com.money.mimi.fragments.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.wallet.NftCollection;
import com.money.mimi.wallet.TokenInfo;
import com.money.mimi.wallet.WalletConfig;
import com.money.mimi.wallet.Web3Provider;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Activity tab: shows on-chain transactions via Explorer API when available;
 * falls back to pure-RPC (native + ERC-20/721 transfers) when explorer is unavailable.
 */
public class WalletActivityFragment extends Fragment {

    private ArrayAdapter<String> adapter;
    private ArrayList<String> items = new ArrayList<>();
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet_activity, container, false);
        ListView list = view.findViewById(R.id.tx_list);
        emptyView = view.findViewById(R.id.tv_activity_empty);
        items.add(getString(R.string.wallet_loading));
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);
        fetchTransactions();
        return view;
    }

    private void fetchTransactions() {
        final android.content.Context appCtx = (getContext() != null) ? getContext().getApplicationContext() : null;
        if (appCtx == null) return;
        final String address = PreferenceManager.getWalletAddress(appCtx);
        if (address == null || address.isEmpty()) {
            setEmpty(getString(R.string.wallet_no_wallet_generate_first));
            return;
        }
        final String explorerBase = WalletConfig.getExplorerBase(appCtx);
        final String explorerApiKey = WalletConfig.getExplorerApiKey(appCtx);
        final String nativeSymbol = WalletConfig.getCurrencySymbol(appCtx);
        if (TextUtils.isEmpty(explorerBase)) {
            loadActivityViaRpc();
            return;
        }
        // Try explorer first; if unauthorized or fails, fall back to RPC-only.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(explorerBase)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ExplorerApi api = retrofit.create(ExplorerApi.class);
        Call<EtherscanResponse<List<Tx>>> call = (explorerApiKey == null || explorerApiKey.isEmpty())
                ? api.txlist("account", "txlist", address, "desc")
                : api.txlistWithKey("account", "txlist", address, "desc", explorerApiKey);
        call.enqueue(new Callback<EtherscanResponse<List<Tx>>>() {
            @Override
            public void onResponse(Call<EtherscanResponse<List<Tx>>> call, Response<EtherscanResponse<List<Tx>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().result != null) {
                    List<Tx> txs = response.body().result;
                    ArrayList<String> lines = new ArrayList<>();
                    for (Tx t : txs) {
                        boolean outgoing = address.equalsIgnoreCase(t.from);
                        String dir = outgoing ? getString(R.string.wallet_activity_dir_sent) : getString(R.string.wallet_activity_dir_received);
                        String amt = weiToEtherString(t.value) + " " + nativeSymbol;
                        String when = formatTs(t.timeStamp);
                        lines.add(dir + " " + amt + (outgoing ? " " + getString(R.string.wallet_activity_to) + " " : " " + getString(R.string.wallet_activity_from) + " ") + shortAddr(outgoing ? t.to : t.from) + " • " + when);
                    }
                    if (lines.isEmpty()) {
                        // Explorer returned no native tx; fall back to RPC for tokens/native in recent blocks
                        loadActivityViaRpc();
                    } else {
                        setList(lines);
                    }
                } else {
                    // Unauthorized or other failure -> RPC fallback
                    loadActivityViaRpc();
                }
            }

            @Override
            public void onFailure(Call<EtherscanResponse<List<Tx>>> call, Throwable t) {
                if (!isAdded()) return;
                loadActivityViaRpc();
            }
        });
    }

    // ---------------- RPC fallback: native transfers + ERC-20/ERC-721 Transfer logs ----------------
    private void loadActivityViaRpc() {
        final android.content.Context appCtx = (getContext() != null) ? getContext().getApplicationContext() : null;
        final String owner = appCtx != null ? PreferenceManager.getWalletAddress(appCtx) : null;
        if (TextUtils.isEmpty(owner)) { setEmpty(getString(R.string.wallet_no_wallet_generate_first)); return; }
        items.clear();
        items.add(getString(R.string.wallet_loading));
        adapter.notifyDataSetChanged();
        // Pre-fetch strings from application context to avoid IllegalStateException if fragment detaches
        final String sDirSent = appCtx != null ? appCtx.getString(R.string.wallet_activity_dir_sent) : "";
        final String sDirReceived = appCtx != null ? appCtx.getString(R.string.wallet_activity_dir_received) : "";
        final String sTo = appCtx != null ? appCtx.getString(R.string.wallet_activity_to) : "";
        final String sFrom = appCtx != null ? appCtx.getString(R.string.wallet_activity_from) : "";
        final String sEmpty = appCtx != null ? appCtx.getString(R.string.wallet_empty_activity) : "";
        final String nativeSymbol = appCtx != null ? WalletConfig.getCurrencySymbol(appCtx) : "";
        new Thread(() -> {
            try {
                Web3j web3 = Web3Provider.get(appCtx);
                BigInteger latest = web3.ethBlockNumber().send().getBlockNumber();
                BigInteger lookback = BigInteger.valueOf(1500); // recent blocks to scan
                BigInteger fromBlock = latest.subtract(lookback);
                if (fromBlock.signum() < 0) fromBlock = BigInteger.ZERO;

                Map<BigInteger, Long> blockTs = new HashMap<>();
                List<ActivityLine> lines = new ArrayList<>();

                // 1) Token transfers: ERC-20 from imported tokens, ERC-721 from imported collections
                String transferTopic = EventEncoder.encode(new Event(
                        "Transfer",
                        Arrays.<TypeReference<?>>asList(
                                new TypeReference<Address>(true) {},
                                new TypeReference<Address>(true) {},
                                new TypeReference<Uint256>(true) {}
                        )));
                String ownerTopic = toTopicAddress(owner);

                // ERC-20
                List<TokenInfo> tokens = PreferenceManager.getWalletTokens(appCtx);
                if (tokens != null && !tokens.isEmpty()) {
                    List<String> tokenAddrs = new ArrayList<>();
                    Map<String, TokenInfo> tokenMap = new HashMap<>();
                    for (TokenInfo t : tokens) { if (!TextUtils.isEmpty(t.address)) { String a = t.address.toLowerCase(); tokenAddrs.add(a); tokenMap.put(a, t); } }
                    if (!tokenAddrs.isEmpty()) {
                        // from = owner
                        EthFilter fFrom = new EthFilter(new DefaultBlockParameterNumber(fromBlock), new DefaultBlockParameterNumber(latest), tokenAddrs)
                                .addSingleTopic(transferTopic)
                                .addOptionalTopics(ownerTopic);
                        // to = owner (topics[2])
                        EthFilter fTo = new EthFilter(new DefaultBlockParameterNumber(fromBlock), new DefaultBlockParameterNumber(latest), tokenAddrs)
                                .addSingleTopic(transferTopic)
                                .addOptionalTopics(null, ownerTopic);
                        addErc20Logs(web3, fFrom, tokenMap, owner, true, blockTs, lines);
                        addErc20Logs(web3, fTo, tokenMap, owner, false, blockTs, lines);
                    }
                }

                // ERC-721
                List<NftCollection> nfts = PreferenceManager.getWalletNfts(appCtx);
                if (nfts != null && !nfts.isEmpty()) {
                    List<String> nftAddrs = new ArrayList<>();
                    Map<String, NftCollection> nftMap = new HashMap<>();
                    for (NftCollection c : nfts) { if (!TextUtils.isEmpty(c.address)) { String a = c.address.toLowerCase(); nftAddrs.add(a); nftMap.put(a, c); } }
                    if (!nftAddrs.isEmpty()) {
                        EthFilter fFrom = new EthFilter(new DefaultBlockParameterNumber(fromBlock), new DefaultBlockParameterNumber(latest), nftAddrs)
                                .addSingleTopic(transferTopic)
                                .addOptionalTopics(ownerTopic);
                        EthFilter fTo = new EthFilter(new DefaultBlockParameterNumber(fromBlock), new DefaultBlockParameterNumber(latest), nftAddrs)
                                .addSingleTopic(transferTopic)
                                .addOptionalTopics(null, ownerTopic);
                        addErc721Logs(web3, fFrom, nftMap, owner, true, blockTs, lines);
                        addErc721Logs(web3, fTo, nftMap, owner, false, blockTs, lines);
                    }
                }

                // 2) Native transfers: scan recent blocks for txs involving owner
                int nativeLimit = 30; int nativeFound = 0;
                BigInteger b = latest;
                while (b.compareTo(fromBlock) >= 0 && nativeFound < nativeLimit) {
                    EthBlock eb = web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(b), true).send();
                    EthBlock.Block blk = eb.getBlock();
                    if (blk != null && blk.getTransactions() != null) {
                        long ts = blk.getTimestamp().longValue();
                        blockTs.put(b, ts);
                        for (EthBlock.TransactionResult tr : blk.getTransactions()) {
                            EthBlock.TransactionObject tx = (EthBlock.TransactionObject) tr.get();
                            String from = safeLower(tx.getFrom());
                            String to = safeLower(tx.getTo());
                            String ownerLower = owner.toLowerCase();
                            if (ownerLower.equals(from) || (to != null && ownerLower.equals(to))) {
                                boolean outgoing = ownerLower.equals(from);
                                String dir = outgoing ? sDirSent : sDirReceived;
                                String amt = weiToEtherString(tx.getValue().toString()) + " " + nativeSymbol;
                                String other = outgoing ? to : from;
                                String when = formatTsSeconds(ts);
                                lines.add(new ActivityLine(b, ts, dir + " " + amt + (outgoing ? " " + sTo + " " : " " + sFrom + " ") + shortAddr(other) + " • " + when));
                                nativeFound++;
                                if (nativeFound >= nativeLimit) break;
                            }
                        }
                    }
                    b = b.subtract(BigInteger.ONE);
                }

                // Sort by block/time desc and render up to 50 items
                Collections.sort(lines, new Comparator<ActivityLine>() {
                    @Override public int compare(ActivityLine o1, ActivityLine o2) { return o2.ts < o1.ts ? -1 : (o2.ts == o1.ts ? 0 : 1); }
                });
                ArrayList<String> out = new ArrayList<>();
                int max = Math.min(50, lines.size());
                for (int i = 0; i < max; i++) out.add(lines.get(i).line);

                if (out.isEmpty()) {
                    setEmpty(sEmpty);
                } else {
                    setList(out);
                }
            } catch (Exception e) {
                setEmpty(sEmpty);
            }
        }).start();
    }

    private void addErc20Logs(Web3j web3, EthFilter filter, Map<String, TokenInfo> tokenMap, String owner, boolean outgoing,
                               Map<BigInteger, Long> blockTs, List<ActivityLine> lines) throws Exception {
        EthLog logs = web3.ethGetLogs(filter).send();
        if (logs == null || logs.getLogs() == null) return;
        String ownerTopic = toTopicAddress(owner);
        for (EthLog.LogResult lr : logs.getLogs()) {
            Log log = (Log) lr.get();
            List<String> topics = log.getTopics();
            if (topics == null || topics.size() < 3) continue;
            String contract = safeLower(log.getAddress());
            TokenInfo info = tokenMap.get(contract);
            if (info == null) continue;
            boolean isOutgoing = outgoing;
            String fromTopic = topics.get(1);
            String toTopic = topics.size() > 2 ? topics.get(2) : null;
            String other = isOutgoing ? topicToAddress(toTopic) : topicToAddress(fromTopic);
            BigInteger raw = hexToBigInt(log.getData());
            BigDecimal div = BigDecimal.ONE;
            for (int i = 0; i < info.decimals; i++) div = div.multiply(BigDecimal.TEN);
            BigDecimal norm = new BigDecimal(raw).divide(div);
            long ts = ensureBlockTs(web3, blockTs, log.getBlockNumber());
            String when = formatTsSeconds(ts);
            String dir = isOutgoing ? getString(R.string.wallet_activity_dir_sent) : getString(R.string.wallet_activity_dir_received);
            lines.add(new ActivityLine(log.getBlockNumber(), ts, dir + " " + norm.stripTrailingZeros().toPlainString() + " " + info.symbol + (isOutgoing ? " to " : " from ") + shortAddr(other) + " • " + when));
        }
    }

    private void addErc721Logs(Web3j web3, EthFilter filter, Map<String, NftCollection> nftMap, String owner, boolean outgoing,
                                Map<BigInteger, Long> blockTs, List<ActivityLine> lines) throws Exception {
        EthLog logs = web3.ethGetLogs(filter).send();
        if (logs == null || logs.getLogs() == null) return;
        for (EthLog.LogResult lr : logs.getLogs()) {
            Log log = (Log) lr.get();
            List<String> topics = log.getTopics();
            if (topics == null || topics.size() < 4) continue; // need tokenId in topic[3]
            String contract = safeLower(log.getAddress());
            NftCollection info = nftMap.get(contract);
            if (info == null) continue;
            String fromTopic = topics.get(1);
            String toTopic = topics.get(2);
            boolean isOutgoing = toTopicAddress(owner).equalsIgnoreCase(fromTopic);
            String other = isOutgoing ? topicToAddress(toTopic) : topicToAddress(fromTopic);
            BigInteger tokenId = topicToBigInt(topics.get(3));
            long ts = ensureBlockTs(web3, blockTs, log.getBlockNumber());
            String when = formatTsSeconds(ts);
            String dir = isOutgoing ? "Sent" : "Received";
            String sym = TextUtils.isEmpty(info.symbol) ? getString(R.string.wallet_nft_fallback_symbol) : info.symbol;
            lines.add(new ActivityLine(log.getBlockNumber(), ts, dir + " " + getString(R.string.wallet_nft_item_format, sym, tokenId.toString()) + (isOutgoing ? " " + getString(R.string.wallet_activity_to) + " " : " " + getString(R.string.wallet_activity_from) + " ") + shortAddr(other) + " • " + when));
        }
    }

    private static class ActivityLine { ActivityLine(BigInteger bn, long ts, String line){ this.bn=bn; this.ts=ts; this.line=line; } BigInteger bn; long ts; String line; }

    private long ensureBlockTs(Web3j web3, Map<BigInteger, Long> map, BigInteger bn) throws Exception {
        Long ts = map.get(bn);
        if (ts != null) return ts;
        EthBlock eb = web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(bn), false).send();
        EthBlock.Block blk = eb.getBlock();
        long v = (blk == null || blk.getTimestamp() == null) ? 0L : blk.getTimestamp().longValue();
        map.put(bn, v);
        return v;
    }

    private String toTopicAddress(String addr) { String a = addr.startsWith("0x") ? addr.substring(2) : addr; String pad = "000000000000000000000000" + a.toLowerCase(); return "0x" + pad.substring(pad.length() - 64); }
    private String topicToAddress(String topic) { if (topic == null) return null; String t = topic.startsWith("0x") ? topic.substring(2) : topic; if (t.length() < 64) return null; String last40 = t.substring(24); return "0x" + last40; }
    private BigInteger topicToBigInt(String topic) { if (topic == null) return BigInteger.ZERO; String t = topic.startsWith("0x") ? topic.substring(2) : topic; return new BigInteger(t, 16); }
    private BigInteger hexToBigInt(String hex) { if (hex == null) return BigInteger.ZERO; String h = hex.startsWith("0x") ? hex.substring(2) : hex; if (h.isEmpty()) return BigInteger.ZERO; return new BigInteger(h, 16); }
    private String safeLower(String a) { return a == null ? null : a.toLowerCase(); }

    private void setList(final ArrayList<String> lines) {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            emptyView.setOnClickListener(null);
            emptyView.setVisibility(View.GONE);
            items.clear();
            items.addAll(lines);
            adapter.notifyDataSetChanged();
        });
    }

    private void setEmptyWithExplorerLink(final String address, final String msg) {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            items.clear();
            adapter.notifyDataSetChanged();
            emptyView.setText(msg);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setOnClickListener(v -> {
                try {
                    String url = WalletConfig.getExplorerAddressUrl(getContext(), address);
                    if (url != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {}
            });
        });
    }

    private void setEmpty(final String msg) {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            items.clear();
            adapter.notifyDataSetChanged();
            emptyView.setText(msg);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setOnClickListener(null);
        });
    }

    private String weiToEtherString(String weiStr) {
        try {
            BigDecimal wei = new BigDecimal(weiStr);
            BigDecimal ether = wei.divide(new BigDecimal("1000000000000000000"));
            return ether.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return "0";
        }
    }

    private String formatTs(String ts) {
        try {
            long s = Long.parseLong(ts);
            Date d = new Date(s * 1000);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatTsSeconds(long s) {
        try {
            Date d = new Date(s * 1000);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d);
        } catch (Exception e) { return ""; }
    }

    private String shortAddr(String a) {
        if (a == null || a.length() < 10) return a;
        return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
    }

    interface ExplorerApi {
        @GET("api")
        Call<EtherscanResponse<List<Tx>>> txlist(@Query("module") String module,
                                                @Query("action") String action,
                                                @Query("address") String address,
                                                @Query("sort") String sort);
        @GET("api")
        Call<EtherscanResponse<List<Tx>>> txlistWithKey(@Query("module") String module,
                                                       @Query("action") String action,
                                                       @Query("address") String address,
                                                       @Query("sort") String sort,
                                                       @Query("apikey") String apikey);
    }

    static class EtherscanResponse<T> {
        public String status;
        public String message;
        public T result;
    }

    static class Tx {
        public String hash;
        public String from;
        public String to;
        public String value;
        public String timeStamp;
        public String isError;
    }
}
