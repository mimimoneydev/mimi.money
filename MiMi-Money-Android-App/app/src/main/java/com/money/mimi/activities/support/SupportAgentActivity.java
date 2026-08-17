package com.money.mimi.activities.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.money.mimi.BuildConfig;
import com.money.mimi.R;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.telemetry.AppTelemetry;

import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Pattern;

/** Hosts the first-party MiMi customer support chat. */
public final class SupportAgentActivity extends AppCompatActivity {
    private static final String SUPPORT_URL = "https://support.mimi.money/customer/";
    private static final String SUPPORT_HOST = "support.mimi.money";
    private static final String SEND_PREFS = "mimi_support_wallet_handoff";
    private static final String SEND_KEY_PREFIX = "wallet_sent_v1_";
    private static final Pattern WALLET_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private static final int MAX_INJECTION_ATTEMPTS = 5;

    private WebView webView;
    private ProgressBar progress;
    private View errorView;
    private int injectionAttempts;
    private AppTelemetry.OperationTrace pageLoadTrace;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_agent);
        AppTelemetry.logFeatureOpened("support_agent");

        Toolbar toolbar = findViewById(R.id.support_toolbar);
        toolbar.setTitle(R.string.support_agent);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar.setNavigationContentDescription(android.R.string.cancel);
        toolbar.setNavigationOnClickListener(view -> finish());

        progress = findViewById(R.id.support_progress);
        errorView = findViewById(R.id.support_error);
        findViewById(R.id.support_retry).setOnClickListener(view -> retry());
        webView = findViewById(R.id.support_web_view);
        configureWebView();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(SUPPORT_URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Required by the first-party support client.
        settings.setDomStorageEnabled(true); // Keeps the server-issued conversation token.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (isTrustedSupportUrl(url)) {
                    stopPageLoadTrace();
                    pageLoadTrace = AppTelemetry.startTrace("support_page_load");
                    showLoading();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isTrustedSupportUrl(url)) return;
                stopPageLoadTrace();
                AppTelemetry.logOperationResult("support_page_load", true);
                progress.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                sendWalletAddressOnce(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl().toString());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    reportPageLoadFailure("support_page_load_network", null);
                    showError();
                }
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (isTrustedSupportUrl(failingUrl)) {
                    reportPageLoadFailure("support_page_load_network", null);
                    showError();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                reportPageLoadFailure("support_page_load_tls", null);
                showError();
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AppHelper.LogCat("Support WebView renderer exited; crashed=" + detail.didCrash());
                } else {
                    AppHelper.LogCat("Support WebView renderer exited");
                }
                reportPageLoadFailure("support_renderer_exit",
                        new IllegalStateException("renderer_exit"));
                view.destroy();
                webView = null;
                showError();
                return true;
            }
        });
    }

    private boolean handleNavigation(String url) {
        if (isTrustedSupportUrl(url)) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (RuntimeException e) {
                AppHelper.LogCat("No browser available for support link: " + e.getMessage());
            }
        }
        return true;
    }

    private static boolean isTrustedSupportUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return "https".equalsIgnoreCase(uri.getScheme())
                && SUPPORT_HOST.equalsIgnoreCase(uri.getHost());
    }

    private void sendWalletAddressOnce(WebView target, String pageUrl) {
        if (!SUPPORT_URL.equals(pageUrl) || target == null) return;
        String walletAddress = PreferenceManager.getWalletAddress(this);
        if (walletAddress == null) return;
        walletAddress = walletAddress.trim();
        if (!WALLET_PATTERN.matcher(walletAddress).matches()) {
            AppHelper.LogCat("Support wallet handoff skipped: no valid public wallet address");
            return;
        }

        String normalizedAddress = walletAddress.toLowerCase(Locale.US);
        String preferenceKey = SEND_KEY_PREFIX + normalizedAddress;
        if (getSharedPreferences(SEND_PREFS, Context.MODE_PRIVATE).getBoolean(preferenceKey, false)) {
            return;
        }

        String addressJson = JSONObject.quote(walletAddress);
        String markerJson = JSONObject.quote("mimi_wallet_sent_v1_" + normalizedAddress);
        String script = "(function(){"
                + "var key=" + markerJson + ";"
                + "if(localStorage.getItem(key)==='1'){return 'already';}"
                + "var form=document.getElementById('chat-form');"
                + "var input=document.getElementById('message');"
                + "if(!form||!input){return 'not_ready';}"
                + "input.value='Wallet address: '+" + addressJson + ";"
                + "input.dispatchEvent(new Event('input',{bubbles:true}));"
                + "localStorage.setItem(key,'1');"
                + "if(form.requestSubmit){form.requestSubmit();}"
                + "else{form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));}"
                + "return 'submitted';"
                + "})()";

        injectionAttempts++;
        target.evaluateJavascript(script, result -> {
            if ("\"submitted\"".equals(result) || "\"already\"".equals(result)) {
                getSharedPreferences(SEND_PREFS, Context.MODE_PRIVATE)
                        .edit().putBoolean(preferenceKey, true).apply();
            } else if (injectionAttempts < MAX_INJECTION_ATTEMPTS && webView != null) {
                webView.postDelayed(() -> sendWalletAddressOnce(webView, SUPPORT_URL), 300L);
            }
        });
    }

    private void showLoading() {
        progress.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void reportPageLoadFailure(String operation, Throwable error) {
        boolean activeLoad = pageLoadTrace != null;
        stopPageLoadTrace();
        if (activeLoad) AppTelemetry.logOperationResult("support_page_load", false);
        if (error != null) AppTelemetry.recordNonFatal(operation, error);
    }

    private void stopPageLoadTrace() {
        if (pageLoadTrace == null) return;
        pageLoadTrace.stop();
        pageLoadTrace = null;
    }

    private void showError() {
        progress.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        if (webView != null) webView.setVisibility(View.GONE);
    }

    private void retry() {
        injectionAttempts = 0;
        if (webView == null) {
            recreate();
            return;
        }
        showLoading();
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(SUPPORT_URL);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        stopPageLoadTrace();
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
