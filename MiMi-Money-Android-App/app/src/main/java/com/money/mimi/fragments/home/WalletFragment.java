package com.money.mimi.fragments.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.money.mimi.R;
import com.money.mimi.agents.CrooAgent;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.wallet.WalletConfig;
import com.money.mimi.wallet.WalletLogoResolver;
import com.money.mimi.wallet.Web3Provider;
import com.money.mimi.wallet.WalletSettingsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wallet screen with header actions, tabs (Coins, NFTs, Activity) and a settings button.
 */
public class WalletFragment extends Fragment {
    private static final String BANK_DAPP_URL = "https://bank.mimi.money";
    private static final String PAY_DAPP_URL = "https://pay.mimi.money";
    private static final String AI_AGENT_STORE_URL = "https://agent.croo.network";
    private static final long CROO_BASE_CHAIN_ID = 8453L;
    private static final int MAX_NETWORK_NAME_LENGTH = 80;
    private static final int MAX_NETWORK_SYMBOL_LENGTH = 16;
    private static final int MAX_NETWORK_URL_LENGTH = 512;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Spinner networkSpinner;
    private Button addNetworkButton;
    private Button deleteNetworkButton;
    private View walletMainContent;
    private View walletChildContainer;
    private NetworkSpinnerAdapter networkAdapter;
    private final ArrayList<WalletConfig.NetworkDefinition> visibleNetworks = new ArrayList<>();
    private boolean suppressNetworkCallbacks;
    private boolean walletChildFullscreen;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wallet_fragment, container, false);

        tabLayout = view.findViewById(R.id.wallet_tab_layout);
        viewPager = view.findViewById(R.id.wallet_view_pager);
        networkSpinner = view.findViewById(R.id.spinner_wallet_network);
        addNetworkButton = view.findViewById(R.id.btn_add_network);
        deleteNetworkButton = view.findViewById(R.id.btn_delete_network);
        walletMainContent = view.findViewById(R.id.wallet_main_content);
        walletChildContainer = view.findViewById(R.id.wallet_child_container);

        setupWalletPager();
        setupNetworkControls();


        // Quick actions
        Button btnSend = view.findViewById(R.id.btn_send);
        Button btnReceive = view.findViewById(R.id.btn_receive);
        Button btnBank = view.findViewById(R.id.btn_bank);
        Button btnDapps = view.findViewById(R.id.btn_dapps);
        Button btnAiAgents = view.findViewById(R.id.btn_ai_agents);
        Button btnPay = view.findViewById(R.id.btn_pay);
        View btnScanDappsQr = view.findViewById(R.id.btn_scan_dapps_qr);
        btnSend.setOnClickListener(v -> openWalletChild(new WalletSendFragment(), "wallet_send"));
        btnReceive.setOnClickListener(v -> openWalletChild(new WalletReceiveFragment(), "wallet_receive"));
        btnBank.setOnClickListener(v -> openTrustedDapp(BANK_DAPP_URL));
        btnDapps.setOnClickListener(v -> openWalletChild(new WalletDappsFragment(), "wallet_dapps", true));
        btnAiAgents.setOnClickListener(v -> openWalletChild(
                new AiAgentMarketplaceFragment(), "wallet_ai_agents"));
        if (btnScanDappsQr != null) {
            btnScanDappsQr.setOnClickListener(v -> startDappQrScan());
        }
        btnPay.setOnClickListener(v -> openTrustedDapp(PAY_DAPP_URL));

        syncWalletContentVisibility();

        return view;
    }

    private void startDappQrScan() {
        if (getContext() == null) return;
        String walletAddress = PreferenceManager.getWalletAddress(getContext().getApplicationContext());
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            Toast.makeText(getContext(), R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
            return;
        }
        com.google.zxing.integration.android.IntentIntegrator integrator =
                com.google.zxing.integration.android.IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.wallet_scan_dapp_qr));
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.setCaptureActivity(com.money.mimi.wallet.PortraitCaptureActivity.class);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        com.google.zxing.integration.android.IntentResult scanResult =
                com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            handleDappQrResult(scanResult.getContents());
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleDappQrResult(@Nullable String contents) {
        if (getContext() == null) return;
        if (contents == null || contents.trim().isEmpty()) return;
        String trimmed = contents.trim();
        String url = extractDappWebsiteUrl(trimmed);
        if (url != null) {
            openWalletChild(WalletDappsFragment.newInstance(url), "wallet_dapps", true);
            return;
        }
        String lower = trimmed.toLowerCase(Locale.US);
        if (lower.startsWith("wc:") || lower.startsWith("walletconnect:")) {
            if (getActivity() != null) {
                com.money.mimi.wallet.MimiWalletConnect.pair(getActivity(), trimmed);
            }
            openWalletChild(WalletDappsFragment.newWalletConnectInstance(trimmed), "wallet_dapps", true);
        } else {
            Toast.makeText(getContext(), R.string.wallet_dapp_invalid_qr, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private String extractDappWebsiteUrl(String raw) {
        try {
            Uri uri = Uri.parse(raw == null ? "" : raw.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
            if ("https".equals(scheme) && isSecureHttpsUrl(raw)) {
                return raw.trim();
            }
            if ("http".equals(scheme) && uri.getHost() != null && !uri.getHost().trim().isEmpty()) {
                Uri upgraded = uri.buildUpon().scheme("https").build();
                String upgradedUrl = upgraded.toString();
                return isSecureHttpsUrl(upgradedUrl) ? upgradedUrl : null;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private void setupWalletPager() {
        if (getContext() == null) return;
        int currentItem = viewPager.getCurrentItem();
        viewPager.setAdapter(null);
        viewPager.setAdapter(new WalletPagerAdapter(getChildFragmentManager(), getContext().getApplicationContext()));
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(Math.min(currentItem, 2), false);
    }

    private void setupNetworkControls() {
        if (getContext() == null) return;
        networkAdapter = new NetworkSpinnerAdapter(getContext(), visibleNetworks);
        networkSpinner.setAdapter(networkAdapter);

        refreshNetworkSelector();

        addNetworkButton.setOnClickListener(v -> showAddNetworkDialog());
        if (deleteNetworkButton != null) {
            deleteNetworkButton.setOnClickListener(v -> showDeleteNetworkDialog());
        }

        networkSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressNetworkCallbacks || getContext() == null) return;
                if (position < 0 || position >= visibleNetworks.size()) return;
                WalletConfig.NetworkDefinition selected = visibleNetworks.get(position);
                String currentKey = PreferenceManager.getWalletSelectedNetworkKey(getContext().getApplicationContext());
                if (selected.key.equalsIgnoreCase(currentKey)) return;
                PreferenceManager.setWalletSelectedNetworkKey(getContext().getApplicationContext(), selected.key);
                Web3Provider.reset();
                setupWalletPager();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void refreshNetworkSelector() {
        if (getContext() == null) return;
        suppressNetworkCallbacks = true;

        List<WalletConfig.NetworkDefinition> networks = WalletConfig.getAvailableNetworks(getContext().getApplicationContext());
        visibleNetworks.clear();
        visibleNetworks.addAll(networks);

        networkAdapter.notifyDataSetChanged();

        int selection = 0;
        String selectedKey = PreferenceManager.getWalletSelectedNetworkKey(getContext().getApplicationContext());
        for (int i = 0; i < visibleNetworks.size(); i++) {
            if (visibleNetworks.get(i).key.equalsIgnoreCase(selectedKey)) {
                selection = i;
                break;
            }
        }
        if (!visibleNetworks.isEmpty()) {
            PreferenceManager.setWalletSelectedNetworkKey(getContext().getApplicationContext(), visibleNetworks.get(selection).key);
            networkSpinner.setSelection(selection, false);
        }

        suppressNetworkCallbacks = false;
        updateDeleteNetworkButtonVisibility();
    }

    private void showAddNetworkDialog() {
        if (getContext() == null) return;

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        int verticalPadding = (int) (8 * getResources().getDisplayMetrics().density);
        content.setPadding(horizontalPadding, verticalPadding, horizontalPadding, 0);

        EditText nameInput = createNetworkInput(R.string.wallet_network_name, InputType.TYPE_CLASS_TEXT);
        EditText rpcInput = createNetworkInput(R.string.wallet_network_rpc_url, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        EditText chainIdInput = createNetworkInput(R.string.wallet_network_chain_id, InputType.TYPE_CLASS_NUMBER);
        EditText symbolInput = createNetworkInput(R.string.wallet_network_currency_symbol, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        EditText explorerInput = createNetworkInput(R.string.wallet_network_explorer_url, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        content.addView(nameInput);
        content.addView(rpcInput);
        content.addView(chainIdInput);
        content.addView(symbolInput);
        content.addView(explorerInput);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.wallet_add_network_title)
                .setView(content)
                .setNegativeButton(R.string.wallet_network_cancel, null)
                .setPositiveButton(R.string.wallet_network_save, null)
                .create();

        dialog.setOnShowListener(shownDialog -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (getContext() == null) return;
            WalletConfig.NetworkDefinition network = buildNetworkFromInputs(
                    nameInput,
                    rpcInput,
                    chainIdInput,
                    symbolInput,
                    explorerInput
            );
            if (network == null) {
                return;
            }
            WalletConfig.NetworkDefinition saved = WalletConfig.addOrUpdateCustomNetwork(getContext().getApplicationContext(), network);
            if (saved == null) {
                Toast.makeText(getContext(), R.string.wallet_network_add_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            PreferenceManager.setWalletSelectedNetworkKey(getContext().getApplicationContext(), saved.key);
            Web3Provider.reset();
            refreshNetworkSelector();
            setupWalletPager();
            Toast.makeText(getContext(), R.string.wallet_network_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showDeleteNetworkDialog() {
        if (getContext() == null) return;
        List<WalletConfig.NetworkDefinition> customNetworks =
                WalletConfig.getCustomNetworkDefinitions(getContext().getApplicationContext());
        if (customNetworks.isEmpty()) {
            Toast.makeText(getContext(), R.string.wallet_network_delete_empty, Toast.LENGTH_SHORT).show();
            updateDeleteNetworkButtonVisibility();
            return;
        }

        boolean[] checked = new boolean[customNetworks.size()];
        ListView listView = new ListView(getContext());
        listView.setDivider(null);
        listView.setAdapter(new DeleteNetworkAdapter(getContext(), customNetworks, checked));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.wallet_network_delete_title)
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
            ArrayList<String> keysToDelete = new ArrayList<>();
            for (int i = 0; i < customNetworks.size(); i++) {
                if (checked[i]) {
                    keysToDelete.add(customNetworks.get(i).key);
                }
            }
            if (keysToDelete.isEmpty()) {
                Toast.makeText(getContext(), R.string.wallet_network_delete_none_selected, Toast.LENGTH_SHORT).show();
                return;
            }
            boolean deleted = WalletConfig.deleteCustomNetworks(getContext().getApplicationContext(), keysToDelete);
            if (!deleted) {
                Toast.makeText(getContext(), R.string.wallet_network_delete_empty, Toast.LENGTH_SHORT).show();
                updateDeleteNetworkButtonVisibility();
                dialog.dismiss();
                return;
            }
            Web3Provider.reset();
            refreshNetworkSelector();
            setupWalletPager();
            Toast.makeText(getContext(), R.string.wallet_network_delete_removed, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void updateDeleteNetworkButtonVisibility() {
        if (deleteNetworkButton == null || getContext() == null) return;
        List<WalletConfig.NetworkDefinition> customNetworks =
                WalletConfig.getCustomNetworkDefinitions(getContext().getApplicationContext());
        deleteNetworkButton.setVisibility(customNetworks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private EditText createNetworkInput(int hintResId, int inputType) {
        EditText input = new EditText(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        input.setLayoutParams(params);
        input.setHint(hintResId);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(maxNetworkInputLength(hintResId))
        });
        return input;
    }

    private WalletConfig.NetworkDefinition buildNetworkFromInputs(EditText nameInput, EditText rpcInput,
                                                                  EditText chainIdInput, EditText symbolInput,
                                                                  EditText explorerInput) {
        String name = getInputText(nameInput);
        String rpcUrl = getInputText(rpcInput);
        String chainIdText = getInputText(chainIdInput);
        String symbol = getInputText(symbolInput);
        String explorerUrl = getInputText(explorerInput);

        if (name.isEmpty() || rpcUrl.isEmpty() || chainIdText.isEmpty() || symbol.isEmpty()) {
            Toast.makeText(getContext(), R.string.wallet_network_invalid_required, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!isSecureHttpsUrl(rpcUrl) || (!explorerUrl.isEmpty() && !isSecureHttpsUrl(explorerUrl))) {
            Toast.makeText(getContext(), R.string.wallet_network_invalid_url, Toast.LENGTH_SHORT).show();
            return null;
        }

        long chainId;
        try {
            chainId = Long.parseLong(chainIdText);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.wallet_network_invalid_chain_id, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (chainId <= 0L) {
            Toast.makeText(getContext(), R.string.wallet_network_invalid_chain_id, Toast.LENGTH_SHORT).show();
            return null;
        }

        return new WalletConfig.NetworkDefinition(
                null,
                name,
                chainId,
                symbol,
                rpcUrl,
                explorerUrl,
                null,
                false
        );
    }

    private String getInputText(EditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private int maxNetworkInputLength(int hintResId) {
        if (hintResId == R.string.wallet_network_currency_symbol) return MAX_NETWORK_SYMBOL_LENGTH;
        if (hintResId == R.string.wallet_network_rpc_url || hintResId == R.string.wallet_network_explorer_url) {
            return MAX_NETWORK_URL_LENGTH;
        }
        return MAX_NETWORK_NAME_LENGTH;
    }

    private boolean isSecureHttpsUrl(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty()
                    && uri.getUserInfo() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private void openTrustedDapp(String url) {
        if (!isSecureHttpsUrl(url)) {
            Toast.makeText(getContext(), R.string.wallet_dapp_invalid_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        openWalletChild(WalletDappsFragment.newInstance(url), "wallet_dapps", true);
    }

    private void openSettings() {
        if (getActivity() == null) return;
        Intent i = new Intent(getActivity(), WalletSettingsActivity.class);
        startActivity(i);
    }

    private void openWalletChild(Fragment fragment, String backStackName) {
        openWalletChild(fragment, backStackName, false);
    }

    private void openWalletChild(Fragment fragment, String backStackName, boolean fullscreen) {
        if (!isAdded()) return;
        FragmentManager childManager = getChildFragmentManager();
        if (childManager.isStateSaved()) return;
        walletChildFullscreen = fullscreen;
        if (fullscreen && getActivity() instanceof com.money.mimi.activities.main.MainActivity) {
            ((com.money.mimi.activities.main.MainActivity) getActivity()).enterWalletFullscreen();
        }
        if (walletMainContent != null) walletMainContent.setVisibility(View.GONE);
        if (walletChildContainer != null) walletChildContainer.setVisibility(View.VISIBLE);
        childManager
                .beginTransaction()
                .replace(R.id.wallet_child_container, fragment)
                .addToBackStack(backStackName)
                .commit();
    }

    public void openDappUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            openDappNotifications();
            return;
        }
        openWalletChild(WalletDappsFragment.newInstance(url), "wallet_dapps", true);
    }

    public void openDappNotifications() {
        openWalletChild(WalletDappsFragment.newNotificationsInstance(), "wallet_dapps", true);
    }

    /** Opens a validated CROO listing; the wallet browser still requires explicit transaction approval. */
    public void openCrooAgentCheckout(String agentId) {
        if (!CrooAgent.isValidId(agentId)) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.ai_agents_invalid_agent, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (getContext() == null || !WalletConfig.switchToChainId(
                getContext().getApplicationContext(), CROO_BASE_CHAIN_ID)) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.ai_agents_base_unavailable, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        refreshNetworkSelector();
        setupWalletPager();
        openTrustedDapp(AI_AGENT_STORE_URL + "/agents/" + agentId.trim());
    }

    private void syncWalletContentVisibility() {
        if (walletMainContent == null || walletChildContainer == null) return;
        boolean showingChild = getChildFragmentManager().getBackStackEntryCount() > 0
                || getChildFragmentManager().findFragmentById(R.id.wallet_child_container) != null;
        walletMainContent.setVisibility(showingChild ? View.GONE : View.VISIBLE);
        walletChildContainer.setVisibility(showingChild ? View.VISIBLE : View.GONE);
    }

    public boolean onBackPressed() {
        if (getChildFragmentManager().getBackStackEntryCount() == 0) return false;
        Fragment child = getChildFragmentManager().findFragmentById(R.id.wallet_child_container);
        if (child instanceof WalletDappWebFragment && ((WalletDappWebFragment) child).onBackPressed()) {
            return true;
        }
        return closeWalletChild();
    }

    public boolean closeWalletChild() {
        FragmentManager childManager = getChildFragmentManager();
        if (childManager.isStateSaved() || childManager.getBackStackEntryCount() == 0) return false;
        childManager.popBackStackImmediate();
        if (walletChildFullscreen && getActivity() instanceof com.money.mimi.activities.main.MainActivity) {
            ((com.money.mimi.activities.main.MainActivity) getActivity()).exitWalletFullscreen();
        }
        walletChildFullscreen = false;
        syncWalletContentVisibility();
        return true;
    }

    @Override
    public void onDestroyView() {
        if (walletChildFullscreen && getActivity() instanceof com.money.mimi.activities.main.MainActivity) {
            ((com.money.mimi.activities.main.MainActivity) getActivity()).exitWalletFullscreen();
        }
        walletChildFullscreen = false;
        super.onDestroyView();
    }

    private static class WalletPagerAdapter extends FragmentPagerAdapter {
        private static final int COUNT = 3;
        private final android.content.Context context;

        WalletPagerAdapter(FragmentManager fm, android.content.Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new WalletCoinsFragment();
                case 1:
                    return new WalletNftsFragment();
                case 2:
                default:
                    return new WalletActivityFragment();
            }
        }

        @Override
        public int getCount() {
            return COUNT;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return context.getString(R.string.wallet_tab_coins);
                case 1:
                    return context.getString(R.string.wallet_tab_nfts);
                case 2:
                default:
                    return context.getString(R.string.wallet_tab_activity);
            }
        }
    }

    private static class NetworkSpinnerAdapter extends ArrayAdapter<WalletConfig.NetworkDefinition> {

        NetworkSpinnerAdapter(android.content.Context context, List<WalletConfig.NetworkDefinition> networks) {
            super(context, R.layout.item_wallet_network_spinner, networks);
            setDropDownViewResource(R.layout.item_wallet_network_spinner);
        }

        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            return getNetworkView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, ViewGroup parent) {
            return getNetworkView(position, convertView, parent);
        }

        private View getNetworkView(int position, @Nullable View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_network_spinner, parent, false);
            }
            WalletConfig.NetworkDefinition network = getItem(position);
            ImageView logo = view.findViewById(R.id.img_network_logo);
            TextView name = view.findViewById(R.id.txt_network_name);
            if (network != null) {
                logo.setImageResource(WalletLogoResolver.getNetworkLogoRes(network.key));
                name.setText(network.displayName);
            }
            return view;
        }
    }

    private static class DeleteNetworkAdapter extends ArrayAdapter<WalletConfig.NetworkDefinition> {
        private final boolean[] checked;

        DeleteNetworkAdapter(android.content.Context context, List<WalletConfig.NetworkDefinition> networks, boolean[] checked) {
            super(context, R.layout.item_wallet_network_delete_row, networks);
            this.checked = checked;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_network_delete_row, parent, false);
            }
            WalletConfig.NetworkDefinition network = getItem(position);
            ImageView logo = view.findViewById(R.id.img_delete_network_logo);
            TextView title = view.findViewById(R.id.txt_delete_network_title);
            TextView subtitle = view.findViewById(R.id.txt_delete_network_subtitle);
            CheckBox checkBox = view.findViewById(R.id.check_delete_network);
            if (network != null) {
                logo.setImageResource(WalletLogoResolver.getNetworkLogoRes(network.key));
                title.setText(network.displayName);
                subtitle.setText(network.currencySymbol + " • Chain ID " + network.chainId);
            }
            checkBox.setChecked(position >= 0 && position < checked.length && checked[position]);
            return view;
        }
    }
}
