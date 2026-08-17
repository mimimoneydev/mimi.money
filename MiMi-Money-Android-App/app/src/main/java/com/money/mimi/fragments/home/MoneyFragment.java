package com.money.mimi.fragments.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.BuildConfig;
import com.money.mimi.activities.main.MainActivity;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.wallet.DappBrowserSupport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MoneyFragment extends Fragment {
    private static final String TAG = "MoneyFragment";
    private static final int INPUT_FILE_REQUEST_CODE = 1201;
    private static final int REQ_RECORD_AUDIO = 1202;
    private static final int REQ_LOCATION = 1203;
    private static final int REQ_FILE_PERMISSION = 1204;
    private static final String DEFAULT_URL = "https://peers.mimi.money";
    private static final int MAX_MAIN_FRAME_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1500L;
    private static final long BLANK_PAGE_CHECK_DELAY_MS = 1200L;
    // Use a modern user agent for better site/video compatibility
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MimiMoneyAndroid/1";
    private static final Set<String> ALLOWED_EXTERNAL_SCHEMES = new HashSet<>(Arrays.asList(
            "tel",
            "mailto",
            "sms",
            "smsto",
            "geo",
            "market"
    ));
    private static final Set<String> BLOCKED_DOWNLOAD_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".aab",
            ".dex",
            ".jar",
            ".js",
            ".sh",
            ".bat",
            ".cmd",
            ".exe",
            ".msi",
            ".scr",
            ".com",
            ".pif",
            ".vbs",
            ".ps1"
    ));

    // Pinch-to-zoom thresholds for full screen toggle
    private static final float SCALE_THRESHOLD_ZOOM_OUT = 1.5f; // stretch to magnify (enter full screen)
    private static final float SCALE_THRESHOLD_ZOOM_IN = 0.7f;  // pinch to shrink (exit full screen)

    private WebView webView;
    private RelativeLayout noNetLayout;
    private TextView noteText;
    private ProgressBar progress;
    private Button retry;
    private SwipeRefreshLayout swipeContainer;

    private ValueCallback<Uri[]> filePathCallback;
    private ValueCallback<Uri[]> mPendingFilePathCallback;
    private WebChromeClient.FileChooserParams mPendingFileChooserParams;
    private Uri camPhotoUri;
    private Uri camVideoUri;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalSystemUiVisibility;
    private ViewGroup rootContainer;
    private boolean isUiFullscreen = false;

    private PermissionRequest mPendingPermissionRequest;
    private GeolocationPermissions.Callback mGeoCallback;
    private String mGeoOrigin;

    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean isPageLoaded = false;
    private Handler retryHandler;
    private Runnable retryRunnable;
    private int mainFrameRetryCount = 0;
    private String currentMainFrameUrl = DEFAULT_URL;
    private boolean hasStartedInitialLoad = false;
    private boolean mainFrameLoadFailed = false;

    // Pinch gesture detection
    private ScaleGestureDetector scaleGestureDetector;
    private boolean isScaling = false;
    private float scaleFactor = 1.0f;

    public MoneyFragment() {}

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_money, container, false);
        rootContainer = (ViewGroup) v;
        webView = v.findViewById(R.id.webView_earn_fragment);
        noNetLayout = v.findViewById(R.id.no_data_internet_layout);
        noteText = v.findViewById(R.id.notification_text);
        progress = v.findViewById(R.id.progressBar);
        retry = v.findViewById(R.id.retry);
        swipeContainer = v.findViewById(R.id.swipeContainer);
        retryHandler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        // Enable cookies including third-party cookies
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }


        // Setup swipe to refresh
        if (swipeContainer != null) {
            swipeContainer.setOnRefreshListener(() -> {
                mainFrameRetryCount = 0;
                retryCurrentPage();
                new Handler().postDelayed(() -> {
                    if (swipeContainer != null) swipeContainer.setRefreshing(false);
                }, 1000);
            });
        }
        if (retry != null) {
            retry.setOnClickListener(view -> {
                mainFrameRetryCount = 0;
                retryCurrentPage();
            });
        }

        // WebView back key handling
        attachWebViewBackHandler();
        // The MainActivity retains every home tab. Start this request as soon as the
        // view exists instead of waiting until the user first selects Money.
        webView.post(this::preloadInitialPageIfNeeded);

        return v;
    }


    private void startTimeout() {
        cancelTimeout(); // Cancel any existing timeout
        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                // 45 seconds elapsed, page hasn't loaded
                if (!isPageLoaded) {
                    handleMainFrameLoadFailure(currentMainFrameUrl);
                }
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 45000); // 45 seconds
    }

    private void cancelTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        timeoutRunnable = null;
        timeoutHandler = null;
    }

    private void cancelRetry() {
        if (retryHandler != null && retryRunnable != null) {
            retryHandler.removeCallbacks(retryRunnable);
        }
        retryRunnable = null;
    }

    private void showLoadingState() {
        if (!isAdded() || webView == null || noNetLayout == null) return;
        webView.setVisibility(View.GONE);
        noNetLayout.setVisibility(View.VISIBLE);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (retry != null) retry.setVisibility(View.GONE);
        if (noteText != null) {
            noteText.setText(getString(R.string.loading));
            noteText.setVisibility(View.VISIBLE);
        }
    }

    private void showContentState() {
        if (!isAdded() || webView == null || noNetLayout == null) return;
        if (mainFrameLoadFailed) return;
        webView.setVisibility(View.VISIBLE);
        noNetLayout.setVisibility(View.GONE);
        if (progress != null) progress.setVisibility(View.GONE);
        if (swipeContainer != null) swipeContainer.setRefreshing(false);
    }

    private void showNoInternetWarning() {
        if (!isAdded() || webView == null || noNetLayout == null) return;
        mainFrameLoadFailed = true;
        cancelRetry();
        webView.setVisibility(View.GONE);
        noNetLayout.setVisibility(View.VISIBLE);
        if (progress != null) progress.setVisibility(View.GONE);
        if (swipeContainer != null) swipeContainer.setRefreshing(false);
        if (retry != null) retry.setVisibility(View.VISIBLE);
        if (noteText != null) {
            noteText.setText(getString(R.string.check_connection_try_again));
            noteText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            preloadInitialPageIfNeeded();
            if (webView != null) webView.onResume();
        } else if (webView != null && isPageLoaded) {
            // Keep the rendered DOM/history in memory without allowing hidden media
            // or JavaScript timers to continue consuming resources in the background.
            webView.onPause();
        }
    }

    public void preloadInitialPageIfNeeded() {
        if (hasStartedInitialLoad || !isAdded() || webView == null) {
            return;
        }
        hasStartedInitialLoad = true;
        checkInternetAndLoad();
    }


    // Dark mode helpers - use app's preference, not system UI mode
    private boolean isDarkModeEnabled() {
        Context c = getContext();
        if (c == null) return false;
        return PreferenceManager.isDarkThemeEnabled(c);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint()) preloadInitialPageIfNeeded();
        if (webView != null && getUserVisibleHint()) webView.onResume();
        // Reapply dark mode when fragment resumes (e.g., after toggling in settings)
        if (webView != null) {
            applyWebDark(webView);
            DappBrowserSupport.injectProvider(webView);
        }
    }

    @Override
    public void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    private void runJs(WebView v, String js) {
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= 19) {
            v.evaluateJavascript(js, null);
        } else {
            v.loadUrl("javascript:" + js);
        }
    }

    private void applyWebDark(final WebView v) {
        if (v == null) return;

        // Try Android's native forceDark for API 29+ (WebView version dependent)
        try {
            WebSettings s = v.getSettings();
            java.lang.reflect.Method m = s.getClass().getMethod("setForceDark", int.class);
            m.invoke(s, isDarkModeEnabled() ? 2 : 0);
        } catch (Throwable ignored) {
            // setForceDark not available, fall back to CSS injection
        }

        // CSS injection for broader compatibility
        if (isDarkModeEnabled()) {
            // Use a handler to ensure DOM is ready
            v.post(() -> {
                String darkCss = "(function(){" +
                        "var id='_mimi_darkstyle';" +
                        "var existing=document.getElementById(id);" +
                        "if(existing){existing.parentNode.removeChild(existing);}" +
                        "var css='html,body{background-color:#121212!important;color:#e0e0e0!important;}' +" +
                        "'*{border-color:#333!important;}' +" +
                        "'a{color:#8ab4f8!important;}' +" +
                        "'img,video,picture,canvas,svg{filter:brightness(0.9)!important;}';" +
                        "var s=document.createElement('style');" +
                        "s.id=id;" +
                        "s.type='text/css';" +
                        "s.appendChild(document.createTextNode(css));" +
                        "(document.head||document.documentElement).appendChild(s);" +
                        "})();";
                runJs(v, darkCss);

                // Also try the invert filter as fallback after a delay
                new Handler().postDelayed(() -> {
                    String invertCss = "(function(){" +
                            "var id='_mimi_darkstyle2';" +
                            "var existing=document.getElementById(id);" +
                            "if(!existing){" +
                            "var css='html{filter:invert(0.9) hue-rotate(180deg);}' +" +
                            "'img,video,picture,canvas,iframe{filter:invert(1) hue-rotate(180deg)!important;}';" +
                            "var s=document.createElement('style');" +
                            "s.id=id;" +
                            "s.type='text/css';" +
                            "s.appendChild(document.createTextNode(css));" +
                            "(document.head||document.documentElement).appendChild(s);" +
                            "}})();";
                    runJs(v, invertCss);
                }, 500);
            });
        } else {
            removeInjectedDarkCss(v);
        }
    }

    private void removeInjectedDarkCss(WebView v) {
        if (v == null) return;
        runJs(v, "(function(){" +
                "var s1=document.getElementById('_mimi_darkstyle');" +
                "var s2=document.getElementById('_mimi_darkstyle2');" +
                "if(s1){s1.parentNode.removeChild(s1);}" +
                "if(s2){s2.parentNode.removeChild(s2);}" +
                "})();");
    }

    private void applyMoneyResponsiveLayout(WebView v, String url) {
        if (v == null || !isMoneyWebOrigin(url)) return;
        runJs(v, "(function(){" +
                "var head=document.head||document.getElementsByTagName('head')[0]||document.documentElement;" +
                "var meta=document.querySelector('meta[name=\"viewport\"]');" +
                "if(!meta){meta=document.createElement('meta');meta.name='viewport';head.appendChild(meta);}" +
                "meta.setAttribute('content','width=device-width, initial-scale=1, maximum-scale=1');" +
                "document.documentElement.setAttribute('data-insets-consumed','true');" +
                "var id='_mimi_money_responsive';" +
                "var style=document.getElementById(id);" +
                "if(!style){style=document.createElement('style');style.id=id;head.appendChild(style);}" +
                "style.textContent=" +
                "'html[data-insets-consumed=\"true\"]{--safe-bottom:0px!important;}' +" +
                "'html,body{width:100%!important;max-width:100%!important;min-width:0!important;margin:0!important;overflow-x:hidden!important;-webkit-text-size-adjust:100%!important;}'+" +
                "'*,*:before,*:after{box-sizing:border-box;}'+" +
                "'img,video,iframe,canvas,table{max-width:100%!important;}';" +
                "})();");
    }

    private boolean isMoneyWebOrigin(String url) {
        if (url == null) return false;
        try {
            Uri uri = Uri.parse(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "peers.mimi.money".equalsIgnoreCase(uri.getHost());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void checkInternetAndLoad() {
        if (isInternetConnected(getContext())) {
            cancelRetry();
            mainFrameRetryCount = 0;
            showLoadingState();
            loadUrl(DEFAULT_URL);
        } else {
            showNoInternetWarning();
        }
    }

    private void retryCurrentPage() {
        if (!isInternetConnected(getContext())) {
            showNoInternetWarning();
            return;
        }
        String retryUrl = isSecureHttpUrl(currentMainFrameUrl)
                ? currentMainFrameUrl
                : DEFAULT_URL;
        cancelRetry();
        mainFrameLoadFailed = false;
        showLoadingState();
        loadUrl(retryUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setBuiltInZoomControls(false); // Disable built-in zoom because pinch-to-zoom is used for full screen toggle
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setTextZoom(100);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            s.setAllowFileAccessFromFileURLs(false);
            s.setAllowUniversalAccessFromFileURLs(false);
        }
        s.setLoadsImagesAutomatically(true);
        s.setSaveFormData(false);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setGeolocationEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(true);
        }

        // Autoplay allowed (like webtoapp) - videos can autoplay
        s.setMediaPlaybackRequiresUserGesture(false);

        // Custom user agent for better site/video compatibility
        s.setUserAgentString(USER_AGENT);

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setBackgroundColor(Color.TRANSPARENT);

        // Hardware acceleration for video playback
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (isAdded() && getActivity() != null) {
            getActivity().getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        webView.removeJavascriptInterface("accessibility");
        webView.removeJavascriptInterface("accessibilityTraversal");
        DappBrowserSupport.attach(webView, getContext());
        applyWebDark(webView);

        // WebViewClient with SSL error handling and external URL scheme handling
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView v, String url, Bitmap icon) {
                if (!isAdded()) return;
                mainFrameLoadFailed = false;
                if (!isSecureHttpUrl(url)) {
                    v.stopLoading();
                    showNoInternetWarning();
                    return;
                }
                currentMainFrameUrl = url;
                showLoadingState();
                isPageLoaded = false;
                startTimeout();
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                if (!isAdded()) return;
                if (mainFrameLoadFailed) {
                    return;
                }
                isPageLoaded = true;
                cancelTimeout();
                cancelRetry();
                showContentState();
                if (isDarkModeEnabled()) applyWebDark(v);
                else removeInjectedDarkCss(v);
                applyMoneyResponsiveLayout(v, url);
                DappBrowserSupport.injectProvider(v);
                scheduleBlankContentRecovery(v, url);
                if (!getUserVisibleHint()) {
                    v.onPause();
                }
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (!isAdded()) return;
                boolean isMainFrame = req == null || req.isForMainFrame();
                if (isMainFrame) {
                    isPageLoaded = true;
                    cancelTimeout();
                    mainFrameLoadFailed = true;
                }
                // Log error details for debugging
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && req != null && err != null) {
                    String message = "WebView error: code=" + err.getErrorCode() + ", description=" + err.getDescription() + ", url=" + req.getUrl() + ", isForMainFrame=" + req.isForMainFrame();
                    if (req.isForMainFrame()) Log.e(TAG, message);
                    else Log.d(TAG, message);
                } else if (req != null) {
                    Log.e(TAG, "WebView error (old API) for url: " + req.getUrl());
                }
                // Only show error UI for main frame errors; subresource errors should not break the page.
                if (isMainFrame) {
                    String failedUrl = req != null && req.getUrl() != null ? req.getUrl().toString() : currentMainFrameUrl;
                    v.stopLoading();
                    handleMainFrameLoadFailure(failedUrl);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                handler.cancel();
                isPageLoaded = true;
                cancelTimeout();
                Log.e(TAG, "WebView SSL error: primary=" + error.getPrimaryError() + ", url=" + error.getUrl());
                if (!isAdded()) return;
                showNoInternetWarning();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && request.getUrl() != null) {
                    return handleExternalUrl(request.getUrl().toString());
                }
                return false;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.w(TAG, "WebView renderer gone. didCrash=" + detail.didCrash() + ", priority=" + detail.rendererPriorityAtExit());
                } else {
                    Log.w(TAG, "WebView renderer gone.");
                }
                recoverCrashedWebView();
                return true;
            }
        });

        // WebChromeClient with geolocation, file chooser, fullscreen, permissions
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int newProgress) {
                if (!isAdded()) return;
                if (mainFrameLoadFailed) return;
                if (newProgress == 100) {
                    showContentState();
                } else {
                    if (progress != null) progress.setVisibility(View.VISIBLE);
                    if (newProgress > 20) {
                        webView.setVisibility(View.VISIBLE);
                        noNetLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Nullable
            @Override
            public Bitmap getDefaultVideoPoster() {
                // Return a default poster for videos (like webtoapp)
                if (mCustomView == null) return null;
                return BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_play);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!isSecureHttpUrl(origin)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                // Geolocation permission dialog (like webtoapp)
                mGeoCallback = callback;
                mGeoOrigin = origin;
                new AlertDialog.Builder(getContext())
                        .setTitle("Location")
                        .setMessage("Would like to use your Current Location")
                        .setCancelable(true)
                        .setPositiveButton("Allow", (dialog, which) -> {
                            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {
                                callback.invoke(origin, true, true);
                            } else {
                                requestPermissions(new String[]{
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                }, REQ_LOCATION);
                            }
                        })
                        .setNegativeButton("Don't Allow", (dialog, which) -> callback.invoke(origin, false, true))
                        .show();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCb, FileChooserParams params) {
                return handleFileChooser(filePathCb, params);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request == null || request.getOrigin() == null || !isSecureHttpUrl(request.getOrigin().toString())) {
                        if (request != null) request.deny();
                        return;
                    }
                    String[] res = request.getResources();
                    boolean wantsAudio = false;
                    boolean unsupported = false;
                    for (String r : res) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) wantsAudio = true;
                        else unsupported = true;
                    }
                    if (unsupported) {
                        request.deny();
                        return;
                    }
                    if (wantsAudio) {
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED) {
                            request.grant(res);
                        } else {
                            mPendingPermissionRequest = request;
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                        }
                    } else {
                        request.grant(res);
                    }
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                try {
                    if (mCustomView != null) {
                        onHideCustomView();
                        return;
                    }
                    mCustomView = view;
                    mOriginalSystemUiVisibility = getActivity().getWindow().getDecorView().getSystemUiVisibility();
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    mCustomViewCallback = callback;
                    ((FrameLayout) getActivity().getWindow().getDecorView()).addView(mCustomView,
                            new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT));
                    getActivity().getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    webView.setVisibility(View.GONE);
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).enterMoneyFullscreen();
                        isUiFullscreen = true;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "onShowCustomView: " + ex.getMessage());
                }
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });

        // Download listener
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                if (!isSafeDownload(url, mimeType, contentDisposition)) {
                    Log.w(TAG, "Blocked unsafe download: " + url + " mime=" + mimeType);
                    return;
                }
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                fileName = sanitizeDownloadFileName(fileName);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) req.addRequestHeader("Cookie", cookies);
                req.addRequestHeader("User-Agent", userAgent);
                Context c = getContext();
                if (c != null) {
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalFilesDir(c, Environment.DIRECTORY_DOWNLOADS, fileName);
                    DownloadManager dm = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) dm.enqueue(req);
                }
            } catch (Exception ignored) {}
        });

        // Pinch-to-zoom gesture detector for full screen toggle
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                scaleFactor = 1.0f;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                // Limit scale factor to avoid extreme values
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

                // Check thresholds for full screen toggle
                if (scaleFactor > SCALE_THRESHOLD_ZOOM_OUT && !isUiFullscreen) {
                    // Enter full screen
                    MainActivity a = (getActivity() instanceof MainActivity) ? (MainActivity) getActivity() : null;
                    if (a != null) {
                        a.enterMoneyFullscreen();
                        isUiFullscreen = true;
                        // Reset scaling state
                        isScaling = false;
                        scaleFactor = 1.0f;
                    }
                } else if (scaleFactor < SCALE_THRESHOLD_ZOOM_IN && isUiFullscreen) {
                    // Exit full screen
                    MainActivity a = (getActivity() instanceof MainActivity) ? (MainActivity) getActivity() : null;
                    if (a != null) {
                        a.exitMoneyFullscreen();
                        isUiFullscreen = false;
                        // Reset scaling state
                        isScaling = false;
                        scaleFactor = 1.0f;
                    }
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
                scaleFactor = 1.0f;
            }
        });

        // Pass touch events to the scale gesture detector
        webView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            // Allow WebView to handle touch events as well (for scrolling, etc.)
            return false;
        });
    }

    // Handle external URL schemes (tel, mailto, sms, geo, market)
    private boolean handleExternalUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.US) : "";

            if (scheme.equals("https")) {
                return false;
            }
            if (scheme.equals("http")) {
                String upgraded = upgradeToHttps(url);
                if (isSecureHttpUrl(upgraded) && webView != null) {
                    webView.loadUrl(upgraded);
                }
                return true;
            }

            Context ctx = getContext();
            if (ctx == null) return true;

            if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, uri);
                ctx.startActivity(intent);
                return true;
            }
            if (url.startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                ctx.startActivity(intent);
                return true;
            }
            if (url.startsWith("sms:")) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse(url.replaceFirst("^sms:", "smsto:")));
                ctx.startActivity(intent);
                return true;
            }
            if (url.startsWith("geo:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                ctx.startActivity(intent);
                return true;
            }
            if (url.startsWith("market:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                ctx.startActivity(intent);
                return true;
            }

            if (!ALLOWED_EXTERNAL_SCHEMES.contains(scheme)) {
                Log.w(TAG, "Blocked external scheme: " + scheme);
                return true;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            ctx.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "URL handling error: " + e.getMessage());
            return true;
        }
    }

    // File chooser with camera capture (like webtoapp)
    private boolean handleFileChooser(ValueCallback<Uri[]> filePathCb, WebChromeClient.FileChooserParams params) {
        Log.d(TAG, "handleFileChooser called, acceptTypes=" + Arrays.toString(params.getAcceptTypes()) + ", isCaptureEnabled=" + params.isCaptureEnabled());
        if (Build.VERSION.SDK_INT < 21) return false;
        if (getActivity() == null) return false;

        // Store pending callback and params in case we need to request permissions
        mPendingFilePathCallback = filePathCb;
        mPendingFileChooserParams = params;

        if (!filePermission()) {
            // Permissions missing, we have requested them.
            // Return true to indicate we will handle the file chooser asynchronously.
            return true;
        }

        // Permissions already granted, proceed with chooser.
        // Clear pending fields as we are about to start the chooser.
        mPendingFilePathCallback = null;
        mPendingFileChooserParams = null;

        filePathCallback = filePathCb;
        camPhotoUri = null;
        camVideoUri = null;

        // Determine accept types
        boolean includePhoto = false;
        boolean includeVideo = false;
        boolean includeAll = false;
        String mimeType = "*/*";
        for (String acceptTypes : params.getAcceptTypes()) {
            // Split by comma, trimming whitespace
            String[] splitTypes = acceptTypes.split("\\s*,\\s*");
            for (String acceptType : splitTypes) {
                switch (acceptType) {
                    case "*/*":
                        includeAll = true;
                        includePhoto = true;
                        includeVideo = true;
                        break;
                    case "image/*":
                        includePhoto = true;
                        mimeType = "image/*";
                        break;
                    case "video/*":
                        includeVideo = true;
                        mimeType = "video/*";
                        break;
                    case "application/pdf":
                        mimeType = "application/pdf";
                        break;
                    case "text/plain":
                        mimeType = "text/plain";
                        break;
                    default:
                        // If unknown, fallback to */*
                        if (!includeAll) mimeType = acceptType;
                        break;
                }
            }
        }

        if (params.getAcceptTypes().length == 0) {
            includePhoto = true;
            includeVideo = true;
            includeAll = true;
        }

        // If capture is enabled, launch camera directly without chooser
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && params.isCaptureEnabled()) {
            // Capture-only mode
            if (includePhoto && !includeVideo) {
                // Only photo capture
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        Uri photoUri = FileProvider.getUriForFile(getActivity(), getFileProviderAuthority(), photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        camPhotoUri = photoUri;
                        startActivityForResult(takePictureIntent, INPUT_FILE_REQUEST_CODE);
                        return true;
                    }
                }
            } else if (includeVideo && !includePhoto) {
                // Only video capture
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File videoFile = null;
                    try {
                        videoFile = createVideoFile();
                    } catch (IOException ex) {
                        Log.e(TAG, "Video file creation failed", ex);
                    }
                    if (videoFile != null) {
                        Uri videoUri = FileProvider.getUriForFile(getActivity(), getFileProviderAuthority(), videoFile);
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                        takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        camVideoUri = videoUri;
                        startActivityForResult(takeVideoIntent, INPUT_FILE_REQUEST_CODE);
                        return true;
                    }
                }
            }
            // If both photo and video capture are possible, fallback to chooser
        }

        Intent takePictureIntent = null;
        Intent takeVideoIntent = null;

        if (includePhoto) {
            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e(TAG, "Image file creation failed", ex);
                }
                if (photoFile != null) {
                    Uri photoUri = FileProvider.getUriForFile(getActivity(), getFileProviderAuthority(), photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    camPhotoUri = photoUri;
                } else {
                    camPhotoUri = null;
                    takePictureIntent = null;
                }
            }
        }

        if (includeVideo) {
            takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                File videoFile = null;
                try {
                    videoFile = createVideoFile();
                } catch (IOException ex) {
                    Log.e(TAG, "Video file creation failed", ex);
                }
                if (videoFile != null) {
                    Uri videoUri = FileProvider.getUriForFile(getActivity(), getFileProviderAuthority(), videoFile);
                    takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                    takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    camVideoUri = videoUri;
                } else {
                    camVideoUri = null;
                    takeVideoIntent = null;
                }
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType(mimeType);
        // Enable multiple selection if mode is MODE_OPEN_MULTIPLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        Intent[] intentArray;
        if (takePictureIntent != null && takeVideoIntent != null) {
            intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
        } else if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else if (takeVideoIntent != null) {
            intentArray = new Intent[]{takeVideoIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        return true;
    }

    private boolean filePermission() {
        if (Build.VERSION.SDK_INT >= 23 && getActivity() != null) {
            boolean cameraGranted = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
            if (!cameraGranted) {
                List<String> permissionsToRequest = new ArrayList<>();
                if (!cameraGranted) {
                    permissionsToRequest.add(Manifest.permission.CAMERA);
                }
                requestPermissions(permissionsToRequest.toArray(new String[0]), REQ_FILE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @SuppressLint("SimpleDateFormat")
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @SuppressLint("SimpleDateFormat")
    private File createVideoFile() throws IOException {
        String fileName = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String newName = "file_" + fileName + "_";
        File sdDirectory = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(newName, ".3gp", sdDirectory);
    }

    private void hideCustomView() {
        if (mCustomView == null) return;
        try {
            // Remove from window decor view (where we added it)
            ((FrameLayout) getActivity().getWindow().getDecorView()).removeView(mCustomView);
            // Restore original system UI visibility
            getActivity().getWindow().getDecorView().setSystemUiVisibility(mOriginalSystemUiVisibility);
        } catch (Exception e) {
            // Fallback: try removing from rootContainer
            if (rootContainer != null) rootContainer.removeView(mCustomView);
        }
        mCustomView = null;
        if (mCustomViewCallback != null) mCustomViewCallback.onCustomViewHidden();
        webView.setVisibility(View.VISIBLE);
        // Keep app UI fullscreen after exiting video fullscreen
    }

    private void attachWebViewBackHandler() {
        if (webView == null) return;
        webView.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
            }
            return false;
        });
    }

    private void recoverCrashedWebView() {
        if (!isAdded() || getContext() == null || swipeContainer == null) return;
        cancelTimeout();
        cancelRetry();
        String reloadUrl = isSecureHttpUrl(currentMainFrameUrl) ? currentMainFrameUrl : DEFAULT_URL;

        try {
            if (webView != null) {
                webView.stopLoading();
                webView.setDownloadListener(null);
                webView.setOnTouchListener(null);
                webView.setOnKeyListener(null);
                webView.setWebChromeClient(null);
                webView.setWebViewClient(null);
                swipeContainer.removeView(webView);
                webView.destroy();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to fully destroy crashed WebView", e);
        }

        webView = new WebView(getContext());
        webView.setId(R.id.webView_earn_fragment);
        swipeContainer.addView(webView, new SwipeRefreshLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        attachWebViewBackHandler();
        mainFrameRetryCount = 0;
        showLoadingState();
        loadUrl(reloadUrl);
    }

    private void handleMainFrameLoadFailure(String failedUrl) {
        if (!isAdded()) return;
        cancelTimeout();
        mainFrameLoadFailed = true;
        if (webView != null) {
            webView.stopLoading();
        }
        String retryUrl = isSecureHttpUrl(failedUrl) ? failedUrl : currentMainFrameUrl;
        if (isInternetConnected(getContext()) && isSecureHttpUrl(retryUrl) && mainFrameRetryCount < MAX_MAIN_FRAME_RETRIES) {
            scheduleMainFrameRetry(retryUrl);
        } else {
            showNoInternetWarning();
        }
    }

    private void scheduleMainFrameRetry(String url) {
        cancelRetry();
        mainFrameRetryCount++;
        showLoadingState();
        retryRunnable = () -> {
            if (!isAdded() || webView == null) return;
            currentMainFrameUrl = url;
            mainFrameLoadFailed = false;
            webView.loadUrl(url);
        };
        if (retryHandler == null) retryHandler = new Handler();
        retryHandler.postDelayed(retryRunnable, RETRY_DELAY_MS);
    }

    private void scheduleBlankContentRecovery(WebView finishedWebView, String url) {
        if (finishedWebView == null) return;
        finishedWebView.postDelayed(() -> {
            if (!isAdded() || webView == null) return;
            if (webView.getProgress() >= 100
                    && webView.getContentHeight() == 0
                    && isInternetConnected(getContext())
                    && mainFrameRetryCount < MAX_MAIN_FRAME_RETRIES
                    && isSecureHttpUrl(url)) {
                Log.w(TAG, "WebView finished with blank content, retrying: " + url);
                handleMainFrameLoadFailure(url);
            }
        }, BLANK_PAGE_CHECK_DELAY_MS);
    }

    private String getFileProviderAuthority() {
        Context context = getContext();
        String packageName = context != null ? context.getPackageName() : BuildConfig.APPLICATION_ID;
        return packageName + ".provider";
    }

    public void loadUrl(String url) {
        if (!isSecureHttpUrl(url)) {
            url = upgradeToHttps(url);
        }
        if (!isSecureHttpUrl(url)) {
            showNoInternetWarning();
            return;
        }
        currentMainFrameUrl = url;
        configureWebView();
        isPageLoaded = false;
        mainFrameLoadFailed = false;
        startTimeout();
        webView.loadUrl(url);
    }

    private boolean isSecureHttpUrl(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !uri.getHost().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private String upgradeToHttps(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.toLowerCase(Locale.US).startsWith("http://")) {
            return "https://" + trimmed.substring("http://".length());
        }
        return trimmed;
    }

    private boolean isSafeDownload(String url, String mimeType, String contentDisposition) {
        if (!isSecureHttpUrl(url)) return false;
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType).toLowerCase(Locale.US);
        for (String extension : BLOCKED_DOWNLOAD_EXTENSIONS) {
            if (fileName.endsWith(extension)) return false;
        }
        String safeMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        return !safeMime.contains("application/java-archive")
                && !safeMime.contains("application/x-msdownload")
                && !safeMime.contains("application/x-sh");
    }

    private String sanitizeDownloadFileName(String fileName) {
        String safeName = fileName == null ? "download" : fileName.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        return safeName.trim().isEmpty() ? "download" : safeName;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO && mPendingPermissionRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    mPendingPermissionRequest.grant(mPendingPermissionRequest.getResources());
                } catch (Exception ignored) {}
            } else {
                try {
                    mPendingPermissionRequest.deny();
                } catch (Exception ignored) {}
            }
            mPendingPermissionRequest = null;
        }
        if (requestCode == REQ_LOCATION && mGeoCallback != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGeoCallback.invoke(mGeoOrigin, true, true);
            } else {
                mGeoCallback.invoke(mGeoOrigin, false, true);
            }
            mGeoCallback = null;
            mGeoOrigin = null;
        }
        if (requestCode == REQ_FILE_PERMISSION) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                // If permission was denied, cancel the pending file callback
                if (mPendingFilePathCallback != null) {
                    mPendingFilePathCallback.onReceiveValue(null);
                    mPendingFilePathCallback = null;
                    mPendingFileChooserParams = null;
                }
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }
            } else {
                // Permission granted, process pending file chooser if any
                if (mPendingFilePathCallback != null && mPendingFileChooserParams != null) {
                    ValueCallback<Uri[]> pendingCb = mPendingFilePathCallback;
                    WebChromeClient.FileChooserParams pendingParams = mPendingFileChooserParams;
                    // Clear pending fields before calling handleFileChooser to avoid loops
                    mPendingFilePathCallback = null;
                    mPendingFileChooserParams = null;
                    handleFileChooser(pendingCb, pendingParams);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, "onActivityResult requestCode=" + requestCode + ", resultCode=" + resultCode + ", intent=" + intent);

        if (requestCode == INPUT_FILE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= 21) {
                Uri[] results = null;

                // Handle cancelled
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "File chooser cancelled");
                    if (filePathCallback != null) {
                        filePathCallback.onReceiveValue(null);
                        filePathCallback = null;
                    }
                    camPhotoUri = null;
                    camVideoUri = null;
                    return;
                }

                if (resultCode == Activity.RESULT_OK) {
                    if (filePathCallback == null) {
                        Log.e(TAG, "filePathCallback is null, cannot process result");
                        return;
                    }

                    ClipData clipData = null;
                    String stringData = null;

                    if (intent != null) {
                        try {
                            clipData = intent.getClipData();
                            stringData = intent.getDataString();
                            Log.d(TAG, "clipData=" + clipData + ", stringData=" + stringData);
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting file chooser result", e);
                        }
                    }

                    // Determine if this is a camera capture result (no clipData or stringData)
                    if (clipData == null && stringData == null) {
                        // Check intent action to distinguish between photo and video capture
                        String action = intent != null ? intent.getAction() : null;
                        if (action != null) {
                            if (action.equals(MediaStore.ACTION_IMAGE_CAPTURE)) {
                                // Photo capture
                                if (camPhotoUri != null) {
                                    results = new Uri[]{camPhotoUri};
                                }
                            } else if (action.equals(MediaStore.ACTION_VIDEO_CAPTURE)) {
                                // Video capture
                                if (camVideoUri != null) {
                                    results = new Uri[]{camVideoUri};
                                }
                            }
                        }
                        // If action is null or not recognized, fallback to whichever URI is available
                        if (results == null) {
                            if (camPhotoUri != null) {
                                results = new Uri[]{camPhotoUri};
                            } else if (camVideoUri != null) {
                                results = new Uri[]{camVideoUri};
                            }
                        }
                    } else {
                        // User selected file(s) from gallery or file manager
                        if (clipData != null) {
                            // Multiple files selected
                            int numSelectedFiles = clipData.getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < numSelectedFiles; i++) {
                                results[i] = clipData.getItemAt(i).getUri();
                            }
                        } else if (stringData != null) {
                            // Single file selected
                            results = new Uri[]{Uri.parse(stringData)};
                        } else {
                            // Try to get bitmap from camera intent extras (fallback for older devices)
                            try {
                                if (intent != null && intent.getExtras() != null) {
                                    Bitmap camPhoto = (Bitmap) intent.getExtras().get("data");
                                    if (camPhoto != null && getActivity() != null) {
                                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                        camPhoto.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                                        String path = MediaStore.Images.Media.insertImage(
                                                getActivity().getContentResolver(), camPhoto, null, null);
                                        if (path != null) {
                                            results = new Uri[]{Uri.parse(path)};
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                if (filePathCallback != null) {
                    Log.d(TAG, "Sending file chooser results: " + (results == null ? "null" : Arrays.toString(results)));
                    filePathCallback.onReceiveValue(results);
                    filePathCallback = null;
                } else {
                    Log.e(TAG, "filePathCallback is null when trying to send results");
                }
                camPhotoUri = null;
                camVideoUri = null;
            }
        }
    }

    @Override
    public void onDestroyView() {
        cancelTimeout();
        cancelRetry();
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (mPendingFilePathCallback != null) {
            mPendingFilePathCallback.onReceiveValue(null);
            mPendingFilePathCallback = null;
        }
        mPendingFileChooserParams = null;
        camPhotoUri = null;
        camVideoUri = null;
        if (mPendingPermissionRequest != null) {
            try {
                mPendingPermissionRequest.deny();
            } catch (Exception ignored) {
            }
            mPendingPermissionRequest = null;
        }
        if (mGeoCallback != null) {
            try {
                mGeoCallback.invoke(mGeoOrigin, false, false);
            } catch (Exception ignored) {
            }
            mGeoCallback = null;
            mGeoOrigin = null;
        }
        hideCustomView();
        if (webView != null) {
            webView.stopLoading();
            webView.setDownloadListener(null);
            webView.setOnTouchListener(null);
            webView.setOnKeyListener(null);
            webView.removeJavascriptInterface("MimiWalletAndroid");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        hasStartedInitialLoad = false;
        rootContainer = null;
        scaleGestureDetector = null;
        super.onDestroyView();
    }

    public boolean onBackPressed() {
        if (mCustomView != null) {
            hideCustomView();
            return true;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return true;
    }

    private boolean isInternetConnected(Context ctx) {
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    // State saving/restoring (like webtoapp)
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null && webView != null) {
            webView.restoreState(savedInstanceState);
        }
    }
}
