package com.money.mimi.fragments.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.money.mimi.R;
import com.money.mimi.helpers.notifications.DappNoticeStore;
import com.money.mimi.models.notifications.DappNotice;
import com.money.mimi.wallet.DappBrowserSupport;
import com.money.mimi.wallet.MimiWalletConnect;
import com.money.mimi.wallet.PortraitCaptureActivity;

import java.text.DateFormat;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

abstract class WalletDappWebFragment extends Fragment {
    protected static final String ARG_URL = "dapp_url";
    protected static final String ARG_WALLETCONNECT_URI = "walletconnect_uri";
    protected static final String ARG_OPEN_NOTIFICATIONS = "open_notifications";

    private static final String RECENTS_PREF = "wallet_dapp_browser_recents";
    private static final String RECENTS_KEY = "items";
    private static final int MAX_RECENTS = 8;
    private static final Set<String> ALLOWED_EXTERNAL_SCHEMES = new HashSet<>(Arrays.asList(
            "mailto",
            "tel",
            "sms"
    ));

    private static final DappShortcut[] FEATURED_DAPPS = new DappShortcut[]{
            new DappShortcut("PancakeSwap", "https://pancakeswap.finance/", "#22C7D6", "P", R.drawable.dapp_logo_pancakeswap),
            new DappShortcut("Uniswap", "https://app.uniswap.org/", "#FF2A8A", "U", R.drawable.dapp_logo_uniswap),
            new DappShortcut("Aave", "https://app.aave.com/", "#7B5CF0", "A", R.drawable.dapp_logo_aave),
            new DappShortcut("GMX", "https://app.gmx.io/", "#111820", "G", R.drawable.dapp_logo_gmx),
            new DappShortcut("OpenSea", "https://opensea.io/", "#2081E2", "O", R.drawable.dapp_logo_opensea),
            new DappShortcut("Lido", "https://stake.lido.fi/", "#56C8F3", "L", R.drawable.dapp_logo_lido),
            new DappShortcut("Aerodrome", "https://aerodrome.finance/", "#F5F2EE", "A", R.drawable.dapp_logo_aerodrome),
            new DappShortcut("Galxe", "https://app.galxe.com/", "#101010", "G", R.drawable.dapp_logo_galxe)
    };

    private WebView webView;
    private ProgressBar progress;
    private RelativeLayout noNetLayout;
    private TextView noNetText;
    private ProgressBar noNetProgress;
    private Button retryButton;
    private LinearLayout walletConnectState;
    private TextView walletConnectUriText;
    private TextView emptyState;
    private ScrollView homeView;
    private EditText urlInput;
    private LinearLayout recentList;
    private GridLayout dappGrid;
    private TextView tabsButton;
    private LinearLayout notificationsPanel;
    private LinearLayout notificationsList;
    private TextView notificationsEmpty;
    private TextView notificationsBadge;
    private ScrollView notificationsScroll;
    private String currentUrl;
    private String lastRequestedUrl;
    private boolean webViewConfigured;
    private boolean showingNoInternet;
    private boolean noticesReceiverRegistered;
    private final BroadcastReceiver noticesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNotifications();
        }
    };

    protected static void putUrlArgs(Fragment fragment, @Nullable String url) {
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
    }

    protected static void putWalletConnectArgs(Fragment fragment, String walletConnectUri) {
        Bundle args = new Bundle();
        args.putString(ARG_WALLETCONNECT_URI, walletConnectUri);
        fragment.setArguments(args);
    }

    protected static void putNotificationsArgs(Fragment fragment) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_OPEN_NOTIFICATIONS, true);
        fragment.setArguments(args);
    }

    protected int getEmptyStateTextRes() {
        return 0;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet_dapps, container, false);
        bindViews(view);
        setupBrowserControls(view);
        populateDappHome();

        String url = getArguments() == null ? null : getArguments().getString(ARG_URL);
        String walletConnectUri = getArguments() == null ? null : getArguments().getString(ARG_WALLETCONNECT_URI);
        boolean openNotifications = getArguments() != null && getArguments().getBoolean(ARG_OPEN_NOTIFICATIONS, false);
        if (openNotifications) {
            showNotificationsPanel();
        } else if (url != null && !url.trim().isEmpty()) {
            loadDapp(normalizeInputToUrl(url.trim()), true);
        } else if (walletConnectUri != null && !walletConnectUri.trim().isEmpty()) {
            handleWalletConnectUri(walletConnectUri.trim());
        } else {
            showStartSurface();
        }
        updateTabsBadge();
        return view;
    }

    private void bindViews(View view) {
        webView = view.findViewById(R.id.wallet_dapps_webview);
        progress = view.findViewById(R.id.wallet_dapps_progress);
        noNetLayout = view.findViewById(R.id.no_data_internet_layout);
        noNetText = view.findViewById(R.id.notification_text);
        noNetProgress = view.findViewById(R.id.progressBar);
        retryButton = view.findViewById(R.id.retry);
        walletConnectState = view.findViewById(R.id.wallet_dapps_walletconnect_state);
        walletConnectUriText = view.findViewById(R.id.wallet_dapps_walletconnect_uri);
        emptyState = view.findViewById(R.id.wallet_dapps_empty_state);
        homeView = view.findViewById(R.id.wallet_dapps_home);
        urlInput = view.findViewById(R.id.wallet_dapps_url_input);
        recentList = view.findViewById(R.id.wallet_dapps_recent_list);
        dappGrid = view.findViewById(R.id.wallet_dapps_grid);
        tabsButton = view.findViewById(R.id.wallet_dapps_tabs_button);
        notificationsPanel = view.findViewById(R.id.wallet_dapps_notifications_panel);
        notificationsList = view.findViewById(R.id.wallet_dapps_notifications_list);
        notificationsEmpty = view.findViewById(R.id.wallet_dapps_notifications_empty);
        notificationsBadge = view.findViewById(R.id.wallet_dapps_notifications_badge);
        notificationsScroll = view.findViewById(R.id.wallet_dapps_notifications_scroll);
        Button copyWalletConnectUri = view.findViewById(R.id.wallet_dapps_copy_walletconnect_uri);
        if (copyWalletConnectUri != null) {
            copyWalletConnectUri.setOnClickListener(v -> copyWalletConnectUri());
        }
        Button clearNotifications = view.findViewById(R.id.wallet_dapps_notifications_clear);
        if (clearNotifications != null) {
            clearNotifications.setOnClickListener(v -> {
                DappNoticeStore.clear(getContext());
                refreshNotifications();
            });
        }
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> retryLastRequestedDapp());
        }
    }

    private void setupBrowserControls(View view) {
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                openFromAddressBar();
                return true;
            }
            return false;
        });

        ImageButton scanButton = view.findViewById(R.id.wallet_dapps_scan_qr);
        ImageButton notificationsButton = view.findViewById(R.id.wallet_dapps_notifications);
        ImageButton closeButton = view.findViewById(R.id.wallet_dapps_close);
        ImageButton homeButton = view.findViewById(R.id.wallet_dapps_home_button);
        ImageButton bookmarkButton = view.findViewById(R.id.wallet_dapps_bookmark_button);
        ImageButton searchButton = view.findViewById(R.id.wallet_dapps_search_button);
        ImageButton menuButton = view.findViewById(R.id.wallet_dapps_menu_button);
        ImageButton newTabButton = view.findViewById(R.id.wallet_dapps_new_tab_button);
        View tabsHitArea = view.findViewById(R.id.wallet_dapps_tabs_hit_area);

        scanButton.setOnClickListener(v -> startQrScan());
        notificationsButton.setOnClickListener(v -> showNotificationsPanel());
        closeButton.setOnClickListener(v -> closeBrowser());
        homeButton.setOnClickListener(v -> focusAddressBar());
        bookmarkButton.setOnClickListener(v -> handleDoneButton());
        searchButton.setOnClickListener(v -> focusAddressBar());
        menuButton.setOnClickListener(this::showBrowserMenu);
        newTabButton.setOnClickListener(v -> openNewTab());
        tabsButton.setOnClickListener(v -> showHome());
        if (tabsHitArea != null) {
            tabsHitArea.setOnClickListener(v -> showHome());
        }
    }

    private void populateDappHome() {
        populateRecentDapps();
        populateFeaturedDapps();
    }

    private void populateRecentDapps() {
        if (recentList == null) return;
        recentList.removeAllViews();
        List<DappShortcut> recents = loadRecentDapps();
        if (recents.isEmpty()) {
            int count = Math.min(5, FEATURED_DAPPS.length);
            for (int i = 0; i < count; i++) {
                recents.add(FEATURED_DAPPS[i]);
            }
        }
        for (DappShortcut dapp : recents) {
            recentList.addView(createRecentItem(dapp));
        }
    }

    private void populateFeaturedDapps() {
        if (dappGrid == null) return;
        dappGrid.removeAllViews();
        for (DappShortcut dapp : FEATURED_DAPPS) {
            dappGrid.addView(createFeaturedItem(dapp));
        }
    }

    private View createRecentItem(DappShortcut dapp) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(86), LinearLayout.LayoutParams.WRAP_CONTENT);
        item.setLayoutParams(itemParams);

        View icon = createDappIconButton(dapp, dp(56), 18, false, true);
        TextView label = createLabel(dapp.title, getColorCompat(R.color.colorPrimaryText), 13, 1);
        label.setPadding(0, dp(6), 0, 0);
        item.addView(icon);
        item.addView(label);
        return item;
    }

    private View createFeaturedItem(DappShortcut dapp) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), dp(4), dp(4), dp(12));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(params);

        View icon = createDappIconButton(dapp, dp(74), 26, false, true);
        TextView label = createLabel(dapp.title, getColorCompat(R.color.colorPrimaryText), 14, 1);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, dp(8), 0, 0);
        item.addView(icon);
        item.addView(label);
        return item;
    }

    private View createDappIconButton(DappShortcut dapp, int size, int textSize, boolean round, boolean showBackground) {
        FrameLayout frame = new FrameLayout(requireContext());
        boolean hasLogo = dapp.logoResId != 0;
        frame.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        frame.setClickable(true);
        frame.setFocusable(true);
        frame.setContentDescription(dapp.title);
        frame.setOnClickListener(v -> loadDapp(dapp.url, true));
        TypedValue ripple = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true);
        frame.setForeground(getResources().getDrawable(ripple.resourceId));

        if (showBackground) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor(dapp.color));
            bg.setCornerRadius(round ? size / 2f : dp(14));
            if ("#F5F2EE".equalsIgnoreCase(dapp.color)) {
                bg.setStroke(dp(1), getColorCompat(R.color.walletDappDivider));
            }
            frame.setBackground(bg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                frame.setElevation(dp(2));
            }
        }
        if (hasLogo) {
            ImageView logo = new ImageView(requireContext());
            int inset = showBackground ? Math.max(dp(round ? 6 : 8), size / 9) : dp(2);
            FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(size - (inset * 2), size - (inset * 2), Gravity.CENTER);
            logo.setLayoutParams(logoParams);
            logo.setAdjustViewBounds(true);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            logo.setImageResource(dapp.logoResId);
            frame.addView(logo);
        } else {
            TextView initial = new TextView(requireContext());
            initial.setLayoutParams(new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            initial.setGravity(Gravity.CENTER);
            initial.setText(dapp.initial);
            initial.setTextColor("#F5F2EE".equalsIgnoreCase(dapp.color) ? getColorCompat(R.color.colorPrimaryText) : Color.WHITE);
            initial.setTextSize(textSize);
            initial.setTypeface(Typeface.DEFAULT_BOLD);
            frame.addView(initial);
        }
        return frame;
    }

    private TextView createLabel(String text, int color, int textSize, int maxLines) {
        TextView label = new TextView(requireContext());
        label.setGravity(Gravity.CENTER);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(textSize);
        label.setMaxLines(maxLines);
        label.setSingleLine(maxLines == 1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return label;
    }

    private void showStartSurface() {
        int textRes = getEmptyStateTextRes();
        if (textRes != 0) {
            showEmptyState(textRes);
        } else {
            showHome();
        }
    }

    private void showHome() {
        if (progress != null) progress.setVisibility(View.GONE);
        hideNoInternetWarning();
        if (walletConnectState != null) walletConnectState.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (notificationsPanel != null) notificationsPanel.setVisibility(View.GONE);
        if (homeView != null) homeView.setVisibility(View.VISIBLE);
        populateRecentDapps();
        if (urlInput != null) urlInput.setText("");
        hideKeyboard();
    }

    private void showEmptyState(int textRes) {
        if (homeView != null) homeView.setVisibility(View.GONE);
        hideNoInternetWarning();
        if (webView != null) webView.setVisibility(View.GONE);
        if (progress != null) progress.setVisibility(View.GONE);
        if (walletConnectState != null) walletConnectState.setVisibility(View.GONE);
        if (notificationsPanel != null) notificationsPanel.setVisibility(View.GONE);
        if (emptyState != null) {
            emptyState.setText(textRes);
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private void showWalletConnectState(String walletConnectUri) {
        if (homeView != null) homeView.setVisibility(View.GONE);
        hideNoInternetWarning();
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (progress != null) progress.setVisibility(View.GONE);
        if (notificationsPanel != null) notificationsPanel.setVisibility(View.GONE);
        if (walletConnectState != null) walletConnectState.setVisibility(View.VISIBLE);
        if (walletConnectUriText != null) walletConnectUriText.setText(walletConnectUri);
        if (urlInput != null) urlInput.setText(walletConnectUri);
    }

    private void showNotificationsPanel() {
        hideKeyboard();
        if (progress != null) progress.setVisibility(View.GONE);
        hideNoInternetWarning();
        if (walletConnectState != null) walletConnectState.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (homeView != null) homeView.setVisibility(View.GONE);
        if (notificationsPanel != null) notificationsPanel.setVisibility(View.VISIBLE);
        DappNoticeStore.markAllRead(getContext());
        refreshNotifications();
    }

    private void refreshNotifications() {
        if (getContext() == null) {
            return;
        }
        updateNotificationsBadge();
        if (notificationsPanel == null || notificationsList == null || notificationsEmpty == null) {
            return;
        }
        List<DappNotice> notices = DappNoticeStore.getNotices(getContext());
        notificationsList.removeAllViews();
        boolean isEmpty = notices.isEmpty();
        notificationsEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (notificationsScroll != null) {
            notificationsScroll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        notificationsList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        for (DappNotice notice : notices) {
            notificationsList.addView(createNoticeItem(notice));
        }
    }

    private void updateNotificationsBadge() {
        if (notificationsBadge == null || getContext() == null) {
            return;
        }
        int unread = DappNoticeStore.getUnreadCount(getContext());
        if (unread <= 0) {
            notificationsBadge.setVisibility(View.GONE);
            return;
        }
        notificationsBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
        notificationsBadge.setVisibility(View.VISIBLE);
    }

    private View createNoticeItem(DappNotice notice) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(params);
        item.setBackground(createNoticeBackground(!notice.isRead()));

        String imageUrl = notice.getImage();
        if (isHttpUrl(imageUrl)) {
            ImageView image = new ImageView(requireContext());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(58), dp(58));
            imageParams.setMargins(0, 0, dp(12), 0);
            image.setLayoutParams(imageParams);
            image.setBackground(createNoticeImageBackground());
            image.setContentDescription(TextUtils.isEmpty(notice.getTitle()) ? getString(R.string.app_name) : notice.getTitle());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(requireContext().getApplicationContext())
                    .load(imageUrl)
                    .asBitmap()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(image);
            item.addView(image);
        }

        LinearLayout textColumn = new LinearLayout(requireContext());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView title = new TextView(requireContext());
        title.setText(TextUtils.isEmpty(notice.getTitle()) ? getString(R.string.app_name) : notice.getTitle());
        title.setTextColor(getColorCompat(R.color.colorPrimaryText));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(title);

        TextView message = new TextView(requireContext());
        message.setText(notice.getMessage());
        message.setTextColor(getColorCompat(R.color.colorSecondaryText));
        message.setTextSize(14);
        message.setPadding(0, dp(6), 0, 0);
        textColumn.addView(message);

        TextView meta = new TextView(requireContext());
        meta.setText(formatNoticeTime(notice.getReceivedAt()));
        meta.setTextColor(getColorCompat(R.color.colorSecondaryText));
        meta.setTextSize(12);
        meta.setPadding(0, dp(8), 0, 0);
        textColumn.addView(meta);

        if (!TextUtils.isEmpty(notice.getLink()) && isHttpUrl(notice.getLink())) {
            TextView link = new TextView(requireContext());
            link.setText(R.string.wallet_dapp_notification_open_link);
            link.setTextColor(getColorCompat(R.color.colorAccent));
            link.setTextSize(14);
            link.setTypeface(Typeface.DEFAULT_BOLD);
            link.setPadding(0, dp(10), 0, 0);
            textColumn.addView(link);
            item.setOnClickListener(v -> loadDapp(normalizeInputToUrl(notice.getLink()), true));
        }
        item.addView(textColumn);
        return item;
    }

    private GradientDrawable createNoticeBackground(boolean unread) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getColorCompat(unread ? R.color.walletDappNoticeUnreadSurface : R.color.walletDappNoticeSurface));
        bg.setStroke(dp(1), getColorCompat(R.color.walletDappDivider));
        bg.setCornerRadius(dp(8));
        return bg;
    }

    private GradientDrawable createNoticeImageBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getColorCompat(R.color.walletDappPanelSurface));
        bg.setStroke(dp(1), getColorCompat(R.color.walletDappDivider));
        bg.setCornerRadius(dp(8));
        return bg;
    }

    private String formatNoticeTime(long timeMillis) {
        if (timeMillis <= 0) {
            return "";
        }
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timeMillis));
    }

    private void openFromAddressBar() {
        if (urlInput == null) return;
        String raw = urlInput.getText() == null ? "" : urlInput.getText().toString().trim();
        if (raw.isEmpty()) return;
        hideKeyboard();
        handleDappPayload(raw);
    }

    private void handleDoneButton() {
        if (urlInput != null) {
            String raw = urlInput.getText() == null ? "" : urlInput.getText().toString().trim();
            if (!raw.isEmpty() && (currentUrl == null || !raw.equalsIgnoreCase(currentUrl))) {
                openFromAddressBar();
                return;
            }
        }
        bookmarkCurrentDapp();
    }

    private void openNewTab() {
        currentUrl = null;
        if (progress != null) progress.setVisibility(View.GONE);
        if (webView != null) {
            webView.stopLoading();
            webView.setVisibility(View.GONE);
        }
        showHome();
        focusAddressBar();
    }

    private void handleDappPayload(String raw) {
        String lower = raw.toLowerCase(Locale.US);
        if (lower.startsWith("wc:") || lower.startsWith("walletconnect:")) {
            handleWalletConnectUri(raw);
            return;
        }
        String url = normalizeInputToUrl(raw);
        if (!isSecureHttpUrl(url)) {
            showBlockedUrl();
            return;
        }
        loadDapp(url, true);
    }

    private void handleWalletConnectUri(String uri) {
        if (getActivity() != null) {
            MimiWalletConnect.pair(getActivity(), uri);
        }
        showWalletConnectState(uri);
    }

    private String normalizeInputToUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        String lower = value.toLowerCase(Locale.US);
        if (lower.startsWith("https://")) {
            return value;
        }
        if (lower.startsWith("http://")) {
            return "https://" + value.substring("http://".length());
        }
        if (looksLikeDomain(value)) {
            return "https://" + value;
        }
        try {
            return "https://search.brave.com/search?q=" + URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "https://search.brave.com/search?q=" + value.replace(" ", "+");
        }
    }

    private boolean looksLikeDomain(String value) {
        return value.contains(".") && !value.contains(" ") && !value.startsWith(".");
    }

    private void loadDapp(String url, boolean remember) {
        if (webView == null || getContext() == null) return;
        if (!isSecureHttpUrl(url)) {
            showBlockedUrl();
            return;
        }
        lastRequestedUrl = url;
        if (urlInput != null) urlInput.setText(url);
        if (!isInternetConnected()) {
            showNoInternetWarning();
            return;
        }
        currentUrl = url;
        if (homeView != null) homeView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (walletConnectState != null) walletConnectState.setVisibility(View.GONE);
        if (notificationsPanel != null) notificationsPanel.setVisibility(View.GONE);
        hideNoInternetWarning();
        webView.setVisibility(View.VISIBLE);
        configureWebView();
        webView.loadUrl(url);
        if (remember) {
            saveRecentDapp(inferTitle(url), url);
            populateRecentDapps();
        }
    }

    private void configureWebView() {
        if (webViewConfigured) return;
        webViewConfigured = true;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        webView.removeJavascriptInterface("accessibility");
        webView.removeJavascriptInterface("accessibilityTraversal");
        DappBrowserSupport.attach(webView, getContext());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progress == null) return;
                if (showingNoInternet) {
                    progress.setVisibility(View.GONE);
                    return;
                }
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (currentUrl != null && title != null && !title.trim().isEmpty()) {
                    saveRecentDapp(cleanTitle(title), currentUrl);
                    populateRecentDapps();
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!isSecureHttpUrl(url)) {
                    view.stopLoading();
                    showBlockedUrl();
                    return;
                }
                if (!isInternetConnected()) {
                    view.stopLoading();
                    lastRequestedUrl = url;
                    showNoInternetWarning();
                    return;
                }
                hideNoInternetWarning();
                currentUrl = url;
                lastRequestedUrl = url;
                if (urlInput != null) urlInput.setText(url);
                if (progress != null) progress.setVisibility(View.VISIBLE);
                DappBrowserSupport.injectProvider(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isSecureHttpUrl(url)) {
                    return;
                }
                currentUrl = url;
                if (urlInput != null) urlInput.setText(url);
                if (progress != null) progress.setVisibility(View.GONE);
                hideNoInternetWarning();
                DappBrowserSupport.injectProvider(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                boolean mainFrame = request == null || request.isForMainFrame();
                if (mainFrame) {
                    if (request != null && request.getUrl() != null) {
                        lastRequestedUrl = request.getUrl().toString();
                    }
                    showNoInternetWarning();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                lastRequestedUrl = failingUrl;
                showNoInternetWarning();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (handler != null) {
                    handler.cancel();
                }
                if (error != null && error.getUrl() != null) {
                    lastRequestedUrl = error.getUrl();
                }
                showNoInternetWarning();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && request.getUrl() != null) {
                    return handleUrl(request.getUrl().toString());
                }
                return false;
            }
        });
    }

    private boolean handleUrl(String url) {
        if (url == null) return true;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        if ("https".equals(scheme) && isSecureHttpUrl(url)) {
            if (!isInternetConnected()) {
                lastRequestedUrl = url;
                showNoInternetWarning();
                return true;
            }
            return false;
        }
        if ("http".equals(scheme)) {
            String upgraded = normalizeInputToUrl(url);
            if (isSecureHttpUrl(upgraded) && webView != null) {
                loadDapp(upgraded, true);
            } else {
                showBlockedUrl();
            }
            return true;
        }
        if ("wc".equals(scheme) || "walletconnect".equals(scheme)) {
            handleWalletConnectUri(url);
            return true;
        }
        if (!ALLOWED_EXTERNAL_SCHEMES.contains(scheme)) {
            showBlockedUrl();
            return true;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private void startQrScan() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.wallet_scan_dapp_qr));
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            String contents = scanResult.getContents();
            if (contents != null && !contents.trim().isEmpty()) {
                handleDappPayload(contents.trim());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void bookmarkCurrentDapp() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            showHome();
            return;
        }
        saveRecentDapp(inferTitle(currentUrl), currentUrl);
        populateRecentDapps();
        if (getContext() != null) {
            Toast.makeText(getContext(), R.string.wallet_dapp_bookmarked, Toast.LENGTH_SHORT).show();
        }
    }

    private void retryLastRequestedDapp() {
        if (lastRequestedUrl == null || lastRequestedUrl.trim().isEmpty()) {
            showHome();
            return;
        }
        loadDapp(lastRequestedUrl, false);
    }

    private void showNoInternetWarning() {
        showingNoInternet = true;
        if (webView != null) {
            webView.stopLoading();
            webView.setVisibility(View.GONE);
        }
        if (homeView != null) homeView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (walletConnectState != null) walletConnectState.setVisibility(View.GONE);
        if (notificationsPanel != null) notificationsPanel.setVisibility(View.GONE);
        if (progress != null) progress.setVisibility(View.GONE);
        if (noNetLayout != null) noNetLayout.setVisibility(View.VISIBLE);
        if (noNetProgress != null) noNetProgress.setVisibility(View.GONE);
        if (retryButton != null) retryButton.setVisibility(View.VISIBLE);
        if (noNetText != null && isAdded()) {
            noNetText.setText(getString(R.string.check_connection_try_again));
            noNetText.setVisibility(View.VISIBLE);
        }
    }

    private void hideNoInternetWarning() {
        showingNoInternet = false;
        if (noNetLayout != null) noNetLayout.setVisibility(View.GONE);
        if (noNetProgress != null) noNetProgress.setVisibility(View.GONE);
        if (retryButton != null) retryButton.setVisibility(View.GONE);
    }

    private boolean isInternetConnected() {
        Context context = getContext();
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void showBrowserMenu(View anchor) {
        if (getContext() == null || anchor == null) return;
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.wallet_dapp_menu_reload);
        menu.getMenu().add(0, 2, 1, R.string.wallet_dapp_menu_copy_url);
        menu.getMenu().add(0, 3, 2, R.string.wallet_dapp_menu_share);
        menu.getMenu().add(0, 4, 3, R.string.wallet_dapp_menu_open_external);
        boolean hasPage = currentUrl != null && !currentUrl.trim().isEmpty();
        menu.getMenu().findItem(1).setEnabled(webView != null && webView.getVisibility() == View.VISIBLE);
        menu.getMenu().findItem(2).setEnabled(hasPage);
        menu.getMenu().findItem(3).setEnabled(hasPage);
        menu.getMenu().findItem(4).setEnabled(hasPage);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                if (webView != null && webView.getVisibility() == View.VISIBLE) webView.reload();
                return true;
            }
            if (id == 2) {
                copyCurrentUrl();
                return true;
            }
            if (id == 3) {
                shareCurrentUrl();
                return true;
            }
            if (id == 4) {
                openCurrentUrlExternally();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void copyCurrentUrl() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;
        if (getContext() == null) return;
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("dapp_url", currentUrl));
            Toast.makeText(getContext(), R.string.wallet_msg_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentUrl() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;
        if (getActivity() == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, currentUrl);
        startActivity(Intent.createChooser(intent, currentUrl));
    }

    private void openCurrentUrlExternally() {
        if (currentUrl == null || currentUrl.trim().isEmpty() || !isSecureHttpUrl(currentUrl)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void closeBrowser() {
        Fragment parent = getParentFragment();
        if (parent instanceof WalletFragment && ((WalletFragment) parent).closeWalletChild()) {
            return;
        }
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    boolean onBackPressed() {
        hideKeyboard();
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                showHome();
            }
            return true;
        }
        if (walletConnectState != null && walletConnectState.getVisibility() == View.VISIBLE) {
            showHome();
            return true;
        }
        if (notificationsPanel != null && notificationsPanel.getVisibility() == View.VISIBLE) {
            showHome();
            return true;
        }
        return true;
    }

    private void focusAddressBar() {
        if (urlInput == null || getContext() == null) return;
        urlInput.requestFocus();
        urlInput.selectAll();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        if (urlInput == null || getContext() == null) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        urlInput.clearFocus();
    }

    private void copyWalletConnectUri() {
        if (getContext() == null || walletConnectUriText == null) return;
        CharSequence uri = walletConnectUriText.getText();
        if (uri == null || uri.length() == 0) return;
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("walletconnect_uri", uri));
            Toast.makeText(getContext(), R.string.wallet_dapp_walletconnect_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private List<DappShortcut> loadRecentDapps() {
        List<DappShortcut> result = new ArrayList<>();
        if (getContext() == null) return result;
        SharedPreferences prefs = requireContext().getSharedPreferences(RECENTS_PREF, Context.MODE_PRIVATE);
        String raw = prefs.getString(RECENTS_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return result;
        String[] rows = raw.split("\\n");
        for (String row : rows) {
            String[] parts = row.split("\\|", 2);
            if (parts.length == 2 && isSecureHttpUrl(parts[1])) {
                result.add(new DappShortcut(parts[0], parts[1], colorFor(parts[0]), initialFor(parts[0]), logoFor(parts[0], parts[1])));
            }
        }
        return result;
    }

    private void saveRecentDapp(String title, String url) {
        if (getContext() == null || !isSecureHttpUrl(url)) return;
        String cleanTitle = cleanTitle(title);
        List<DappShortcut> recents = loadRecentDapps();
        List<DappShortcut> next = new ArrayList<>();
        next.add(new DappShortcut(cleanTitle, url, colorFor(cleanTitle), initialFor(cleanTitle), logoFor(cleanTitle, url)));
        for (DappShortcut existing : recents) {
            if (!existing.url.equalsIgnoreCase(url) && next.size() < MAX_RECENTS) {
                next.add(existing);
            }
        }
        StringBuilder raw = new StringBuilder();
        for (DappShortcut item : next) {
            if (raw.length() > 0) raw.append('\n');
            raw.append(item.title.replace("|", " ")).append('|').append(item.url);
        }
        requireContext().getSharedPreferences(RECENTS_PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(RECENTS_KEY, raw.toString())
                .apply();
    }

    private String inferTitle(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) return url;
            if (host.startsWith("www.")) host = host.substring(4);
            String[] parts = host.split("\\.");
            return parts.length > 0 ? capitalize(parts[0]) : host;
        } catch (Exception e) {
            return url;
        }
    }

    private String cleanTitle(String title) {
        String value = title == null ? "" : title.trim().replace('\n', ' ');
        if (value.isEmpty()) return currentUrl == null ? "dApp" : inferTitle(currentUrl);
        return value.length() > 24 ? value.substring(0, 24) : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "dApp";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private String initialFor(String title) {
        String clean = title == null ? "" : title.trim();
        return clean.isEmpty() ? "D" : clean.substring(0, 1).toUpperCase(Locale.US);
    }

    private String colorFor(String title) {
        String[] colors = {"#3F51F5", "#0097A7", "#F9A825", "#455A64", "#FF7043", "#1976D2", "#7B1FA2", "#111820"};
        int hash = title == null ? 0 : Math.abs(title.hashCode());
        return colors[hash % colors.length];
    }

    private boolean isSecureHttpUrl(String value) {
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

    private boolean isHttpUrl(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void showBlockedUrl() {
        if (getContext() != null) {
            Toast.makeText(getContext(), R.string.wallet_dapp_invalid_qr, Toast.LENGTH_SHORT).show();
        }
    }

    private static int logoFor(String title, String url) {
        String value = ((title == null ? "" : title) + " " + (url == null ? "" : url)).toLowerCase(Locale.US);
        if (value.contains("pancakeswap")) return R.drawable.dapp_logo_pancakeswap;
        if (value.contains("uniswap")) return R.drawable.dapp_logo_uniswap;
        if (value.contains("aave")) return R.drawable.dapp_logo_aave;
        if (value.contains("gmx")) return R.drawable.dapp_logo_gmx;
        if (value.contains("opensea")) return R.drawable.dapp_logo_opensea;
        if (value.contains("lido")) return R.drawable.dapp_logo_lido;
        if (value.contains("aerodrome")) return R.drawable.dapp_logo_aerodrome;
        if (value.contains("galxe")) return R.drawable.dapp_logo_galxe;
        return 0;
    }

    private void updateTabsBadge() {
        if (tabsButton == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.TRANSPARENT);
        bg.setStroke(dp(2), getColorCompat(R.color.walletDappIconTint));
        bg.setCornerRadius(dp(8));
        tabsButton.setBackground(bg);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getColorCompat(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(resId, requireContext().getTheme());
        }
        return getResources().getColor(resId);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerNoticeReceiver();
        refreshNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    public void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        unregisterNoticeReceiver();
        super.onStop();
    }

    private void registerNoticeReceiver() {
        if (getContext() == null || noticesReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(DappNoticeStore.ACTION_NOTICES_CHANGED);
        ContextCompat.registerReceiver(
                requireContext(), noticesReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        noticesReceiverRegistered = true;
    }

    private void unregisterNoticeReceiver() {
        if (getContext() == null || !noticesReceiverRegistered) {
            return;
        }
        try {
            requireContext().unregisterReceiver(noticesReceiver);
        } catch (Exception ignored) {
        }
        noticesReceiverRegistered = false;
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.stopLoading();
            webView.removeJavascriptInterface("MimiWalletAndroid");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        progress = null;
        walletConnectState = null;
        walletConnectUriText = null;
        emptyState = null;
        homeView = null;
        urlInput = null;
        recentList = null;
        dappGrid = null;
        tabsButton = null;
        notificationsPanel = null;
        notificationsList = null;
        notificationsEmpty = null;
        notificationsBadge = null;
        notificationsScroll = null;
        webViewConfigured = false;
        super.onDestroyView();
    }

    private static final class DappShortcut {
        final String title;
        final String url;
        final String color;
        final String initial;
        final int logoResId;

        DappShortcut(String title, String url, String color, String initial) {
            this(title, url, color, initial, 0);
        }

        DappShortcut(String title, String url, String color, String initial, int logoResId) {
            this.title = title;
            this.url = url;
            this.color = color;
            this.initial = initial;
            this.logoResId = logoResId;
        }
    }
}
