package com.money.mimi.activities.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Pair;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.money.mimi.R;
import com.money.mimi.activities.NewConversationContactsActivity;
import com.money.mimi.activities.groups.AddMembersToGroupActivity;
import com.money.mimi.activities.main.welcome.WelcomeActivity;
import com.money.mimi.activities.messages.TransferMessageContactsActivity;
import com.money.mimi.activities.search.SearchCallsActivity;
import com.money.mimi.activities.search.SearchConversationsActivity;
import com.money.mimi.activities.settings.SettingsActivity;
import com.money.mimi.activities.status.StatusActivity;
import com.money.mimi.adapters.others.HomeTabsAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.AdMobHelper;
import com.money.mimi.services.MainService;
import com.money.mimi.helpers.ForegroundRuning;
import com.money.mimi.helpers.OutDateHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.RateHelper;
import com.money.mimi.helpers.StartupPermissions;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.models.calls.CallsInfoModel;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.socket.client.Socket;
import io.reactivex.disposables.CompositeDisposable;

import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;
import static com.money.mimi.app.AppConstants.EVENT_BUS_NEW_USER_JOINED;

/**
 * Created by Abderrahim El imame on 01/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class MainActivity extends AppCompatActivity implements NetworkListener {

    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(R.id.main_activity)
    View mView;
    @BindView(R.id.adParentLyout)
    LinearLayout adParentLyout;
    @BindView(R.id.adParentLyoutBottom)
    LinearLayout adParentLyoutBottom;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.app_bar)
    Toolbar toolbar;
    @BindView(R.id.main_view)
    LinearLayout MainView;

    @BindView(R.id.floatingBtnMain)
    FloatingActionButton floatingBtnMain;


    InterstitialAd mInterstitialAd;
    boolean actionModeStarted = false;

    private boolean spaceFullscreen = false;

    private AdView mAdViewWallet;
    private AdView mAdViewMoney;
    private AdView mAdViewSpace;
    private boolean mobileAdsInitialized;
    private int mCurrentTab = 0;
    private boolean fullScreenIntentPromptShown = false;
    private String configurationLanguageTag;


    HomeTabsAdapter mFragmentStatePagerAdapter;

    private Socket mSocket;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configurationLanguageTag = getConfigurationLanguageTag(getResources().getConfiguration());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        configureSystemBars();
        applySystemWindowInsets();
        Permissions();
        seedSampleConversations();
        initializerView();
        setupToolbar();
        EventBus.getDefault().register(this);

        RateHelper.appLaunched(this);
        if (!AppHelper.isServiceRunning(this, MainService.class)) {
            AppHelper.startMainService(this);
        }
        connectToServer();
        PreferenceManager.setIsNeedInfo(this, false);
        handleDappNoticeIntent(getIntent());

        new Handler().postDelayed(() -> {
            getContactInfo();
            checkIfUserSession();
            fetchAppSettingsAndInitAds();
            loadCounter();
        }, 1000);


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDappNoticeIntent(intent);
    }

    private void handleDappNoticeIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        boolean openNotifications = intent.getBooleanExtra("open_dapp_notifications", false);
        String dappUrl = intent.getStringExtra("open_dapp_url");
        if (!openNotifications && (dappUrl == null || dappUrl.trim().isEmpty())) {
            return;
        }
        if (viewPager == null) {
            return;
        }
        viewPager.post(() -> openWalletDappTarget(dappUrl, openNotifications, 0));
    }

    private void openWalletDappTarget(String dappUrl, boolean openNotifications, int attempt) {
        if (isFinishing() || viewPager == null || tabLayout == null || mFragmentStatePagerAdapter == null) {
            return;
        }
        TabLayout.Tab walletTab = tabLayout.getTabAt(2);
        if (walletTab != null && tabLayout.getSelectedTabPosition() != 2) {
            walletTab.select();
        } else {
            viewPager.setCurrentItem(2, false);
        }

        Fragment fragment = mFragmentStatePagerAdapter.getRegisteredFragment(2);
        if (fragment instanceof com.money.mimi.fragments.home.WalletFragment) {
            com.money.mimi.fragments.home.WalletFragment walletFragment =
                    (com.money.mimi.fragments.home.WalletFragment) fragment;
            if (openNotifications) {
                walletFragment.openDappNotifications();
            } else {
                walletFragment.openDappUrl(dappUrl);
            }
            return;
        }

        if (attempt < 6) {
            viewPager.postDelayed(() -> openWalletDappTarget(dappUrl, openNotifications, attempt + 1), 150);
        }
    }


    private void connectToServer() {

        WhatsCloneApplication app = (WhatsCloneApplication) getApplication();
        mSocket = app.getSocket();
        if (mSocket != null && mSocket.connected()) {
            JSONObject json = new JSONObject();
            try {
                json.put("connected", true);
                json.put("senderId", PreferenceManager.getID(this));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit(AppConstants.SOCKET_IS_ONLINE, json);
        } else {
            AppHelper.startMainService(this);
        }
    }


    private void initializerAds() {
        requestNewInterstitial();
    }


    private void requestNewInterstitial() {
        String adUnitId = AdMobHelper.interstitialId(this, PreferenceManager.getUnitInterstitialAdID(this));
        if (adUnitId == null) return;
        AdRequest adRequest = new AdRequest.Builder()
                
                .build();
        InterstitialAd.load(this, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        requestNewInterstitial();
                        if (!isFinishing() && !isDestroyed()) {
                            AppHelper.LaunchActivity(MainActivity.this, SettingsActivity.class);
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        mInterstitialAd = null;
                        if (!isFinishing() && !isDestroyed()) {
                            AppHelper.LaunchActivity(MainActivity.this, SettingsActivity.class);
                        }
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                mInterstitialAd = null;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        String newLanguageTag = getConfigurationLanguageTag(newConfig);
        if (configurationLanguageTag != null
                && !configurationLanguageTag.equals(newLanguageTag)) {
            configurationLanguageTag = newLanguageTag;
            // MainActivity handles locale changes itself in the manifest, so force a
            // clean activity/WebView lifecycle to reload localized web content.
            recreate();
            return;
        }
        configurationLanguageTag = newLanguageTag;

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Anchored adaptive sizes depend on the current width. Never reuse an
            // AdView whose size was calculated for the previous orientation.
            destroyAllBannerAds();
            switch (viewPager.getCurrentItem()) {
                case 0:
                    updateBannerForTab(0);
                    break;
                case 1:
                    updateBannerForTab(1);
                    break;
                case 2:
                    updateBannerForTab(2);
                    break;
                case 3:
                    updateBannerForTab(3);
                    break;
                case 4:
                    updateBannerForTab(4);
                    break;
            }
        }
    }

    private static String getConfigurationLanguageTag(Configuration configuration) {
        if (configuration == null || configuration.getLocales().isEmpty()) {
            return "";
        }
        return configuration.getLocales().get(0).toLanguageTag();
    }

    @SuppressWarnings("deprecation")
    protected void setDefaultLocale(Context context, Locale locale) {
        Locale.setDefault(locale);
        Configuration appConfig = new Configuration();
        appConfig.locale = locale;
        context.getResources().updateConfiguration(appConfig, context.getResources().getDisplayMetrics());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_conversations:
                RateHelper.significantEvent(this);
                if (AppHelper.isAndroid5()) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, new Pair<>(toolbar, "searchBar"));
                    Intent mIntent = new Intent(this, SearchConversationsActivity.class);
                    startActivity(mIntent, options.toBundle());
                } else {
                    AppHelper.LaunchActivity(this, SearchConversationsActivity.class);
                }
                break;/*
            case R.id.search_contacts:
                if (AppHelper.isAndroid5()) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, new Pair<>(toolbar, "searchBar"));
                    Intent mIntent = new Intent(this, SearchContactsActivity.class);
                    startActivity(mIntent, options.toBundle());
                } else {
                    AppHelper.LaunchActivity(this, SearchContactsActivity.class);
                }
                break;*/
            case R.id.search_calls:
                if (AppHelper.isAndroid5()) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, new Pair<>(toolbar, "searchBar"));
                    Intent mIntent = new Intent(this, SearchCallsActivity.class);
                    startActivity(mIntent, options.toBundle());
                } else {
                    AppHelper.LaunchActivity(this, SearchCallsActivity.class);
                }

                break;
            case R.id.new_group:
                RateHelper.significantEvent(this);
                PreferenceManager.clearMembers(this);
                AppHelper.LaunchActivity(this, AddMembersToGroupActivity.class);
                break;
            case R.id.settings:
                RateHelper.significantEvent(this);
                if (PreferenceManager.ShowInterstitialrAds(this)) {
                    try {
                        if (mInterstitialAd != null) {
                            mInterstitialAd.show(MainActivity.this);
                        } else {
                            AppHelper.LaunchActivity(this, SettingsActivity.class);
                        }
                    } catch (Exception e) {
                        AppHelper.LaunchActivity(this, SettingsActivity.class);
                    }

                } else {
                    AppHelper.LaunchActivity(this, SettingsActivity.class);
                }
                break;
            case R.id.status:
                RateHelper.significantEvent(this);
                AppHelper.LaunchActivity(this, StatusActivity.class);
                break;

            case R.id.wallet_settings:
                startActivity(new Intent(this, com.money.mimi.wallet.WalletSettingsActivity.class));
                break;

            case R.id.clear_log_call:
                RateHelper.significantEvent(this);
                removeCallsLog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void removeCallsLog() {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        AppHelper.showDialog(this, getString(R.string.delete_call_dialog));
        realm.executeTransactionAsync(realm1 -> {
            RealmResults<CallsModel> callsModels = realm1.where(CallsModel.class).findAll();
            for (CallsModel callsModel : callsModels) {
                RealmResults<CallsInfoModel> callsInfoModel = realm1.where(CallsInfoModel.class).equalTo("callId", callsModel.getId()).findAll();
                callsInfoModel.deleteAllFromRealm();
            }
            callsModels.deleteAllFromRealm();
        }, () -> {
            AppHelper.hideDialog();
            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_DELETE_CALL_ITEM, 0));
        }, error -> {
            AppHelper.LogCat(error.getMessage());
            AppHelper.hideDialog();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        switch (tabLayout.getSelectedTabPosition()) {
            case 1:
                getMenuInflater().inflate(R.menu.calls_menu, menu);
                break;
            case 0:
                getMenuInflater().inflate(R.menu.conversations_menu, menu);
                break;
            case 2:
                getMenuInflater().inflate(R.menu.wallet_menu, menu);
                break;
            case 3:
                getMenuInflater().inflate(R.menu.wallet_menu, menu);
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * method to setup toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        }
    }

    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(AppHelper.getColor(this, R.color.colorPrimaryDark));
            window.setNavigationBarColor(AppHelper.getColor(this, R.color.colorWhite));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }


    /**
     * method to initialize the view
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initializerView() {

        registerFCM();

        if (PreferenceManager.isOutDate(this)) {
            OutDateHelper.appLaunched(this);
            OutDateHelper.significantEvent(this);
        }
        mFragmentStatePagerAdapter = new HomeTabsAdapter(getSupportFragmentManager());
        // Money and Space are WebView-backed tabs. Keep the full home pager alive so
        // both pages can preload at startup and retain their exact navigation/scroll
        // state while the user moves between tabs.
        viewPager.setOffscreenPageLimit(mFragmentStatePagerAdapter.getCount() - 1);
        viewPager.setAdapter(mFragmentStatePagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentTab = position;
                updateBannerForTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        viewPager.setCurrentItem(0);
        tabLayout.getTabAt(1).setCustomView(R.layout.custom_tab_calls);
        tabLayout.getTabAt(0).setCustomView(R.layout.custom_tab_messages);
        tabLayout.getTabAt(2).setCustomView(R.layout.custom_tab_wallet);
        tabLayout.getTabAt(3).setCustomView(R.layout.custom_tab_money);
        tabLayout.getTabAt(4).setCustomView(R.layout.custom_tab_space);
        tabLayout.getTabAt(1).getCustomView().setBackgroundColor(AppHelper.getColor(this, R.color.colorUnSelected));
        tabLayout.getTabAt(2).getCustomView().setBackgroundColor(AppHelper.getColor(this, R.color.colorUnSelected));
        tabLayout.getTabAt(3).getCustomView().setBackgroundColor(AppHelper.getColor(this, R.color.colorUnSelected));
        tabLayout.getTabAt(4).getCustomView().setBackgroundColor(AppHelper.getColor(this, R.color.colorUnSelected));
        tabLayout.getTabAt(0).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
        ((TextView) findViewById(R.id.title_tabs_calls)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
        ((TextView) findViewById(R.id.title_tabs_wallet)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
        ((TextView) findViewById(R.id.title_tabs_money)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
        ((TextView) findViewById(R.id.title_tabs_space)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
        ((TextView) findViewById(R.id.title_tabs_messages)).setTextSize(16);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Drawable icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_chat_white_24dp);
                final FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) floatingBtnMain.getLayoutParams();

                switch (tab.getPosition()) {
                    case 1:
                        p.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                        p.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        floatingBtnMain.setLayoutParams(p);
                        floatingBtnMain.show();
                        mCurrentTab = 1;
                        updateBannerForTab(1);
                        if (findViewById(R.id.appbar) != null) findViewById(R.id.appbar).setVisibility(View.VISIBLE);
                        icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_call_white_24dp);
                        viewPager.setCurrentItem(1);

                        View vCallsCounter = findViewById(R.id.counterTabCalls);
                        if (vCallsCounter instanceof TextView) {
                            ((TextView) vCallsCounter).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            vCallsCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        }
                        View vMsgCounter = findViewById(R.id.counterTabMessages);
                        if (vMsgCounter != null) vMsgCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vWalletCounter = findViewById(R.id.counterTabWallet);
                        if (vWalletCounter != null) vWalletCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMoneyCounter = findViewById(R.id.counterTabSpace);
                        if (vMoneyCounter != null) vMoneyCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));

                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                        }
                        View vTitleCalls = findViewById(R.id.title_tabs_calls);
                        if (vTitleCalls instanceof TextView) {
                            ((TextView) vTitleCalls).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                            ((TextView) vTitleCalls).setTextSize(16);
                        }
                        floatingBtnMain.setImageDrawable(icon);
                        break;
                    case 0:
                        p.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                        p.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        floatingBtnMain.setLayoutParams(p);
                        floatingBtnMain.show();
                        mCurrentTab = 0;
                        updateBannerForTab(0);
                        if (findViewById(R.id.appbar) != null) findViewById(R.id.appbar).setVisibility(View.VISIBLE);
                        icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_chat_white_24dp);
                        viewPager.setCurrentItem(0);
                        View vMsgCounter2 = findViewById(R.id.counterTabMessages);
                        if (vMsgCounter2 instanceof TextView) {
                            ((TextView) vMsgCounter2).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            vMsgCounter2.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        }
                        View vCallsCounter2 = findViewById(R.id.counterTabCalls);
                        if (vCallsCounter2 != null) vCallsCounter2.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vWalletCounter2 = findViewById(R.id.counterTabWallet);
                        if (vWalletCounter2 != null) vWalletCounter2.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMoneyCounter2 = findViewById(R.id.counterTabSpace);
                        if (vMoneyCounter2 != null) vMoneyCounter2.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));

                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                        }
                        View vTitleMessages = findViewById(R.id.title_tabs_messages);
                        if (vTitleMessages instanceof TextView) {
                            ((TextView) vTitleMessages).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                            ((TextView) vTitleMessages).setTextSize(16);
                        }
                        floatingBtnMain.setImageDrawable(icon);
                        break;
                    case 2:
                        // Hide FAB on Wallet tab without mangling its size; ensure no leftover animation
                        floatingBtnMain.hide();
                        floatingBtnMain.clearAnimation();
                        floatingBtnMain.setVisibility(View.GONE);
                        viewPager.setCurrentItem(2);
                        mCurrentTab = 2;
                        updateBannerForTab(2);
                        if (findViewById(R.id.appbar) != null) findViewById(R.id.appbar).setVisibility(View.VISIBLE);
                        View vWalletCounter3 = findViewById(R.id.counterTabWallet);
                        if (vWalletCounter3 instanceof TextView) {
                            ((TextView) vWalletCounter3).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            vWalletCounter3.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        }
                        View vCallsCounter3 = findViewById(R.id.counterTabCalls);
                        if (vCallsCounter3 != null) vCallsCounter3.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMsgCounter3 = findViewById(R.id.counterTabMessages);
                        if (vMsgCounter3 != null) vMsgCounter3.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMoneyCounter3 = findViewById(R.id.counterTabSpace);
                        if (vMoneyCounter3 != null) vMoneyCounter3.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));

                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                        }
                        View vTitleWallet = findViewById(R.id.title_tabs_wallet);
                        if (vTitleWallet instanceof TextView) {
                            ((TextView) vTitleWallet).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                            ((TextView) vTitleWallet).setTextSize(16);
                        }
                        break;
                    case 3:
                        // Hide FAB on Money tab without mangling its size; ensure no leftover animation
                        floatingBtnMain.hide();
                        floatingBtnMain.clearAnimation();
                        floatingBtnMain.setVisibility(View.GONE);
                        viewPager.setCurrentItem(3);
                        mCurrentTab = 3;
                        updateBannerForTab(3);
                        if (findViewById(R.id.appbar) != null) findViewById(R.id.appbar).setVisibility(View.GONE);
                        View vMoneyCounter4 = findViewById(R.id.counterTabMoney);
                        if (vMoneyCounter4 instanceof TextView) {
                            ((TextView) vMoneyCounter4).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            vMoneyCounter4.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        }
                        View vCallsCounter4 = findViewById(R.id.counterTabCalls);
                        if (vCallsCounter4 != null) vCallsCounter4.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMsgCounter4 = findViewById(R.id.counterTabMessages);
                        if (vMsgCounter4 != null) vMsgCounter4.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vWalletCounter4 = findViewById(R.id.counterTabWallet);
                        if (vWalletCounter4 != null) vWalletCounter4.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vSpaceCounter4 = findViewById(R.id.counterTabSpace);
                        if (vSpaceCounter4 != null) vSpaceCounter4.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));

                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                        }
                        View vTitleMoney = findViewById(R.id.title_tabs_money);
                        if (vTitleMoney instanceof TextView) {
                            ((TextView) vTitleMoney).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                            ((TextView) vTitleMoney).setTextSize(16);
                        }
                        break;
                    case 4:
                        // Hide FAB on Space tab without mangling its size; ensure no leftover animation
                        floatingBtnMain.hide();
                        floatingBtnMain.clearAnimation();
                        floatingBtnMain.setVisibility(View.GONE);
                        viewPager.setCurrentItem(4);
                        mCurrentTab = 4;
                        updateBannerForTab(4);
                        if (findViewById(R.id.appbar) != null) findViewById(R.id.appbar).setVisibility(View.GONE);
                        View vSpaceCounter5 = findViewById(R.id.counterTabSpace);
                        if (vSpaceCounter5 instanceof TextView) {
                            ((TextView) vSpaceCounter5).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            vSpaceCounter5.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        }
                        View vCallsCounter5 = findViewById(R.id.counterTabCalls);
                        if (vCallsCounter5 != null) vCallsCounter5.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMsgCounter5 = findViewById(R.id.counterTabMessages);
                        if (vMsgCounter5 != null) vMsgCounter5.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vWalletCounter5 = findViewById(R.id.counterTabWallet);
                        if (vWalletCounter5 != null) vWalletCounter5.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        View vMoneyCounter5 = findViewById(R.id.counterTabMoney);
                        if (vMoneyCounter5 != null) vMoneyCounter5.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));

                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                        }
                        View vTitleSpace = findViewById(R.id.title_tabs_space);
                        if (vTitleSpace instanceof TextView) {
                            ((TextView) vTitleSpace).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                            ((TextView) vTitleSpace).setTextSize(16);
                        }
                        break;
                    default:
                        break;
                }

                if (tab.getPosition() != 0) {
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_ACTION_MODE_FINISHED));
                }
                if (tab.getPosition() == 0 || tab.getPosition() == 1) {
                    final Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_for_button_animtion_enter);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) { }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            floatingBtnMain.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });
                    floatingBtnMain.startAnimation(animation);
                } else {
                    floatingBtnMain.clearAnimation();
                    floatingBtnMain.setVisibility(View.GONE);
                }
                // Refresh the toolbar menu when switching tabs
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1:
                        View uCallsCounter = findViewById(R.id.counterTabCalls);
                        if (uCallsCounter != null) uCallsCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        if (uCallsCounter instanceof TextView) {
                            ((TextView) uCallsCounter).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        }
                        View uTitleCalls = findViewById(R.id.title_tabs_calls);
                        if (uTitleCalls instanceof TextView) {
                            ((TextView) uTitleCalls).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            ((TextView) uTitleCalls).setTextSize(13);
                        }
                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        }
                        break;
                    case 0:
                        View uMsgCounter = findViewById(R.id.counterTabMessages);
                        if (uMsgCounter != null) uMsgCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        if (uMsgCounter instanceof TextView) {
                            ((TextView) uMsgCounter).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        }
                        View uTitleMessages = findViewById(R.id.title_tabs_messages);
                        if (uTitleMessages instanceof TextView) {
                            ((TextView) uTitleMessages).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            ((TextView) uTitleMessages).setTextSize(13);
                        }
                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        }
                        break;
                    case 2:
                        View uWalletCounter = findViewById(R.id.counterTabWallet);
                        if (uWalletCounter != null) uWalletCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        if (uWalletCounter instanceof TextView) {
                            ((TextView) uWalletCounter).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        }
                        View uTitleWallet = findViewById(R.id.title_tabs_wallet);
                        if (uTitleWallet instanceof TextView) {
                            ((TextView) uTitleWallet).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            ((TextView) uTitleWallet).setTextSize(13);
                        }
                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        }
                        break;
                    case 3:
                        View uMoneyCounter = findViewById(R.id.counterTabMoney);
                        if (uMoneyCounter != null) uMoneyCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        if (uMoneyCounter instanceof TextView) {
                            ((TextView) uMoneyCounter).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        }
                        View uTitleMoney = findViewById(R.id.title_tabs_money);
                        if (uTitleMoney instanceof TextView) {
                            ((TextView) uTitleMoney).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            ((TextView) uTitleMoney).setTextSize(13);
                        }
                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        }
                        break;
                    case 4:
                        View uSpaceCounter = findViewById(R.id.counterTabSpace);
                        if (uSpaceCounter != null) uSpaceCounter.setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        if (uSpaceCounter instanceof TextView) {
                            ((TextView) uSpaceCounter).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        }
                        View uTitleSpace = findViewById(R.id.title_tabs_space);
                        if (uTitleSpace instanceof TextView) {
                            ((TextView) uTitleSpace).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorPrimary));
                            ((TextView) uTitleSpace).setTextSize(13);
                        }
                        if (tabLayout.getTabAt(tab.getPosition()).getCustomView() != null) {
                            tabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {


            }
        });

        floatingBtnMain.setOnClickListener(view -> {
            switch (tabLayout.getSelectedTabPosition()) {
                case 1:
                    RateHelper.significantEvent(this);
                    Intent intent = new Intent(this, TransferMessageContactsActivity.class);
                    intent.putExtra("forCall", true);
                    startActivity(intent);
                    AnimationsUtil.setSlideInAnimation(this);
                    break;
                case 0:
                    RateHelper.significantEvent(this);
                    AppHelper.LaunchActivity(this, NewConversationContactsActivity.class);
                    break;

            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private void preloadMoneyAndSpaceTabs(int attempt) {
        if (mFragmentStatePagerAdapter == null || viewPager == null || isFinishing()) {
            return;
        }

        boolean preloadedMoney = false;
        boolean preloadedSpace = false;

        Fragment money = mFragmentStatePagerAdapter.getRegisteredFragment(3);
        if (money instanceof com.money.mimi.fragments.home.MoneyFragment) {
            ((com.money.mimi.fragments.home.MoneyFragment) money).preloadInitialPageIfNeeded();
            preloadedMoney = true;
        }

        Fragment space = mFragmentStatePagerAdapter.getRegisteredFragment(4);
        if (space instanceof com.money.mimi.fragments.home.SpaceFragment) {
            ((com.money.mimi.fragments.home.SpaceFragment) space).preloadInitialPageIfNeeded();
            preloadedSpace = true;
        }

        if ((!preloadedMoney || !preloadedSpace) && attempt < 6) {
            viewPager.postDelayed(() -> preloadMoneyAndSpaceTabs(attempt + 1), 150);
        }
    }


    private void fetchAppSettingsAndInitAds() {
        disposables.add(APIHelper.initialApiUsersContacts().getAppSettings().subscribe(settingsResponse -> {
            PreferenceManager.setUnitBannerAdsID(this, settingsResponse.getUnitBannerID());
            PreferenceManager.setShowBannerAds(this, settingsResponse.isAdsBannerStatus());
            PreferenceManager.setUnitVideoAdsID(this, settingsResponse.getUnitVideoID());
            PreferenceManager.setAppVideoAdsID(this, settingsResponse.getAppID());
            PreferenceManager.setShowVideoAds(this, settingsResponse.isAdsVideoStatus());
            PreferenceManager.setUnitInterstitialAdID(this, settingsResponse.getUnitInterstitialID());
            PreferenceManager.setShowInterstitialAds(this, settingsResponse.isAdsInterstitialStatus());
            PreferenceManager.setUnitWalletBannerAdsID(this, settingsResponse.getUnitWalletBannerID());
            PreferenceManager.setShowWalletBannerAds(this, settingsResponse.isAdsWalletBannerStatus());
            PreferenceManager.setUnitMoneyBannerAdsID(this, settingsResponse.getUnitMoneyBannerID());
            PreferenceManager.setShowMoneyBannerAds(this, settingsResponse.isAdsMoneyBannerStatus());
            PreferenceManager.setUnitSpaceBannerAdsID(this, settingsResponse.getUnitSpaceBannerID());
            PreferenceManager.setShowSpaceBannerAds(this, settingsResponse.isAdsSpaceBannerStatus());

            if (PreferenceManager.ShowInterstitialrAds(this)) {
                if (PreferenceManager.getUnitInterstitialAdID(this) != null) {
                    initializerAds();
                }
            }
            showMainAds();
        }, throwable -> {
            AppHelper.LogCat("Error fetching app settings " + throwable.getMessage());
            showMainAds();
        }));
    }

    private void showMainAds() {
        adParentLyout.removeAllViews();
        adParentLyoutBottom.removeAllViews();
        destroyAllBannerAds();
        refreshInlineAds();
        updateBannerForTab(mCurrentTab);
    }

    private void refreshInlineAds() {
        if (mFragmentStatePagerAdapter == null) {
            return;
        }
        Fragment conversations = mFragmentStatePagerAdapter.getRegisteredFragment(0);
        if (conversations instanceof com.money.mimi.fragments.home.ConversationsFragment) {
            ((com.money.mimi.fragments.home.ConversationsFragment) conversations).refreshInlineAds();
        }
        Fragment calls = mFragmentStatePagerAdapter.getRegisteredFragment(1);
        if (calls instanceof com.money.mimi.fragments.home.CallsFragment) {
            ((com.money.mimi.fragments.home.CallsFragment) calls).refreshInlineAds();
        }
    }

    private AdView createBannerAd(String adUnitId, LinearLayout targetLayout) {
        adUnitId = AdMobHelper.bannerId(this, adUnitId);
        if (adUnitId == null || targetLayout == null) return null;
        if (!mobileAdsInitialized) {
            mobileAdsInitialized = true;
            MobileAds.initialize(this, status -> { });
        }

        int widthDp = getAdaptiveBannerWidthDp(targetLayout);

        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp));
        adView.setAdUnitId(adUnitId);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (!isFinishing() && !isDestroyed()
                        && adView.getParent() == targetLayout
                        && !spaceFullscreen) {
                    targetLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                if (adView.getParent() == targetLayout) {
                    targetLayout.removeView(adView);
                    targetLayout.setVisibility(View.GONE);
                } else if (targetLayout.getChildCount() == 0) {
                    targetLayout.setVisibility(View.GONE);
                }
                clearBannerReference(adView);
                adView.destroy();
            }
        });
        return adView;
    }

    @SuppressWarnings("deprecation")
    private int getAdaptiveBannerWidthDp(View target) {
        int widthPixels;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
            Rect bounds = windowMetrics.getBounds();
            widthPixels = bounds.width();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            widthPixels = displayMetrics.widthPixels;
        }
        if (target != null) {
            widthPixels -= target.getPaddingLeft() + target.getPaddingRight();
        }
        float density = getResources().getDisplayMetrics().density;
        return Math.max(1, Math.round(widthPixels / density));
    }

    private void clearBannerReference(AdView adView) {
        if (mAdViewWallet == adView) mAdViewWallet = null;
        if (mAdViewMoney == adView) mAdViewMoney = null;
        if (mAdViewSpace == adView) mAdViewSpace = null;
    }

    private void updateBannerForTab(int tabPosition) {
        adParentLyout.removeAllViews();
        adParentLyoutBottom.removeAllViews();
        destroyInactiveBannerAds(tabPosition);
        AdView adToShow = null;
        boolean newlyCreated = false;
        boolean useBottomBanner = false;

        switch (tabPosition) {
            case 0:
            case 1:
                adParentLyout.setVisibility(View.GONE);
                adParentLyoutBottom.setVisibility(View.GONE);
                return;
            case 2:
                if (mAdViewWallet == null && PreferenceManager.ShowWalletBannerAds(this)) {
                    mAdViewWallet = createBannerAd(
                            PreferenceManager.getUnitWalletBannerAdsID(this), adParentLyoutBottom);
                    newlyCreated = mAdViewWallet != null;
                }
                adToShow = mAdViewWallet;
                useBottomBanner = true;
                break;
            case 3:
                if (mAdViewMoney == null && PreferenceManager.ShowMoneyBannerAds(this)) {
                    mAdViewMoney = createBannerAd(
                            PreferenceManager.getUnitMoneyBannerAdsID(this), adParentLyoutBottom);
                    newlyCreated = mAdViewMoney != null;
                }
                adToShow = mAdViewMoney;
                useBottomBanner = true;
                break;
            case 4:
                if (mAdViewSpace == null && PreferenceManager.ShowSpaceBannerAds(this)) {
                    mAdViewSpace = createBannerAd(
                            PreferenceManager.getUnitSpaceBannerAdsID(this), adParentLyoutBottom);
                    newlyCreated = mAdViewSpace != null;
                }
                adToShow = mAdViewSpace;
                useBottomBanner = true;
                break;
        }

        if (adToShow != null) {
            LinearLayout targetLayout = useBottomBanner ? adParentLyoutBottom : adParentLyout;
            targetLayout.setVisibility(newlyCreated ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            targetLayout.addView(adToShow, params);

            if (newlyCreated) {
                adToShow.loadAd(new AdRequest.Builder().build());
            }

            LinearLayout hiddenLayout = useBottomBanner ? adParentLyout : adParentLyoutBottom;
            hiddenLayout.setVisibility(View.GONE);
        } else {
            adParentLyout.setVisibility(View.GONE);
            adParentLyoutBottom.setVisibility(View.GONE);
        }
    }

    private void destroyInactiveBannerAds(int activeTab) {
        if (activeTab != 2) {
            destroyAd(mAdViewWallet);
            mAdViewWallet = null;
        }
        if (activeTab != 3) {
            destroyAd(mAdViewMoney);
            mAdViewMoney = null;
        }
        if (activeTab != 4) {
            destroyAd(mAdViewSpace);
            mAdViewSpace = null;
        }
    }

    private void destroyAllBannerAds() {
        destroyAd(mAdViewWallet);
        destroyAd(mAdViewMoney);
        destroyAd(mAdViewSpace);
        mAdViewWallet = null;
        mAdViewMoney = null;
        mAdViewSpace = null;
    }

    private void seedSampleConversations() {
        if (PreferenceManager.isSampleDataSeeded(this)) {
            return;
        }
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        try {
            realm.executeTransaction(r -> {
                int myId = PreferenceManager.getID(this);
                String[][] sampleContacts = {
                        {"9", "Alice", "0x1111111111111111111111111111111111111111", "Available"},
                        {"10", "Bob", "0x2222222222222222222222222222222222222222", "At work"},
                        {"11", "Charlie", "0x3333333333333333333333333333333333333333", "Busy"},
                        {"12", "Diana", "0x4444444444444444444444444444444444444444", "Hey there! I am using MiMi"},
                        {"13", "Eve", "0x5555555555555555555555555555555555555555", "Exploring web3"},
                        {"14", "Frank", "0x6666666666666666666666666666666666666666", "Crypto enthusiast"},
                };
                String[][][] sampleMessages = {
                        {{"9", "Hey! How are you?", "2026-04-10T10:00:00.000+03:00"},
                                {String.valueOf(myId), "I am good! Just checking out the new wallet feature", "2026-04-10T10:01:00.000+03:00"},
                                {"9", "Nice! Have you tried sending ETH yet?", "2026-04-10T10:02:00.000+03:00"},
                                {String.valueOf(myId), "Not yet, still setting up my wallet. Will try soon!", "2026-04-10T10:03:00.000+03:00"}},
                        {{"10", "Can we schedule a meeting for tomorrow?", "2026-04-10T09:30:00.000+03:00"},
                                {String.valueOf(myId), "Sure, what time works for you?", "2026-04-10T09:31:00.000+03:00"},
                                {"10", "How about 2pm?", "2026-04-10T09:32:00.000+03:00"},
                                {String.valueOf(myId), "2pm works! I will send the agenda later", "2026-04-10T09:33:00.000+03:00"},
                                {"10", "Great, thanks!", "2026-04-10T09:34:00.000+03:00"}},
                        {{"11", "Did you see the ETH price today?", "2026-04-10T08:15:00.000+03:00"},
                                {String.valueOf(myId), "Yeah, it went up 5%! Bullish", "2026-04-10T08:16:00.000+03:00"},
                                {"11", "I think we are heading for a breakout", "2026-04-10T08:17:00.000+03:00"},
                                {String.valueOf(myId), "Agreed. Time to HODL!", "2026-04-10T08:18:00.000+03:00"}},
                        {{"12", "Thank you so much! You are the first one to wish me!", "2026-04-10T07:46:00.000+03:00"},
                                {String.valueOf(myId), "Happy birthday! Hope you have an amazing day!", "2026-04-10T07:45:00.000+03:00"},
                                {String.valueOf(myId), "Of course! Are you doing anything special?", "2026-04-10T07:47:00.000+03:00"},
                                {"12", "Just a small dinner with friends. You should come!", "2026-04-10T07:48:00.000+03:00"},
                                {String.valueOf(myId), "I would love to! Send me the details", "2026-04-10T07:49:00.000+03:00"}},
                        {                        {"13", "Have you tried the new Money section?", "2026-04-09T18:20:00.000+03:00"},
                                {String.valueOf(myId), "Yes! The Uniswap integration is really smooth", "2026-04-09T18:21:00.000+03:00"},
                                {"13", "I also liked the Aave lending page. Super easy to use", "2026-04-09T18:22:00.000+03:00"},
                                {String.valueOf(myId), "DeFi on mobile is finally getting user-friendly", "2026-04-09T18:23:00.000+03:00"}},
                        {{"14", "Hey, can you help me set up my wallet?", "2026-04-09T16:00:00.000+03:00"},
                                {String.valueOf(myId), "Sure! Just go to the Wallet tab and tap Create Wallet", "2026-04-09T16:01:00.000+03:00"},
                                {"14", "Done! It generated a wallet address for me", "2026-04-09T16:05:00.000+03:00"},
                                {String.valueOf(myId), "Now you can receive tokens. Share your QR code to get paid", "2026-04-09T16:06:00.000+03:00"},
                                {"14", "Awesome, thanks for the help!", "2026-04-09T16:07:00.000+03:00"},
                                {String.valueOf(myId), "No problem! Welcome to web3!", "2026-04-09T16:08:00.000+03:00"}},
                };
                int msgId = 100;
                int convId = 100;
                for (int i = 0; i < sampleContacts.length; i++) {
                    int contactId = Integer.parseInt(sampleContacts[i][0]);
                    String contactName = sampleContacts[i][1];
                    String contactPhone = sampleContacts[i][2];
                    String contactStatus = sampleContacts[i][3];

                    ContactsModel contact = new ContactsModel();
                    contact.setId(contactId);
                    contact.setContactID(contactId);
                    contact.setUsername(contactName);
                    contact.setWalletAddress(contactPhone);
                    contact.setLinked(true);
                    contact.setActivate(true);
                    contact.setExist(true);
                    contact.setStatus(contactStatus);
                    contact.setStatus_date(String.valueOf(System.currentTimeMillis() / 1000));
                    r.copyToRealmOrUpdate(contact);

                    int currentConvId = convId++;
                    String[][] msgs = sampleMessages[i];
                    String lastMsgText = msgs[msgs.length - 1][1];
                    String lastMsgDate = msgs[msgs.length - 1][2];
                    int lastMsgId = msgId + msgs.length - 1;

                    RealmList<MessagesModel> messageList = new RealmList<>();
                    for (String[] msg : msgs) {
                        int senderId = Integer.parseInt(msg[0]);
                        MessagesModel msgModel = new MessagesModel();
                        msgModel.setId(msgId);
                        msgModel.setMessage(msg[1]);
                        msgModel.setDate(msg[2]);
                        msgModel.setUsername(senderId == myId ? "Me" : contactName);
                        msgModel.setWalletAddress(contactPhone);
                        msgModel.setStatus(AppConstants.IS_SEEN);
                        msgModel.setGroup(false);
                        msgModel.setConversationID(currentConvId);
                        msgModel.setSenderID(senderId);
                        msgModel.setRecipientID(senderId == myId ? contactId : myId);
                        msgModel.setFileUpload(true);
                        msgModel.setFileDownLoad(true);
                        r.copyToRealmOrUpdate(msgModel);
                        messageList.add(r.where(MessagesModel.class).equalTo("id", msgId).findFirst());
                        msgId++;
                    }

                    ConversationsModel convModel = new ConversationsModel();
                    convModel.setId(currentConvId);
                    convModel.setRecipientID(contactId);
                    convModel.setRecipientUsername(contactName);
                    convModel.setRecipientPhone(contactPhone);
                    convModel.setRecipientImage(null);
                    convModel.setLastMessage(lastMsgText);
                    convModel.setLastMessageId(lastMsgId);
                    convModel.setMessageDate(lastMsgDate);
                    convModel.setStatus(AppConstants.IS_SEEN);
                    convModel.setUnreadMessageCounter("0");
                    convModel.setGroup(false);
                    convModel.setCreatedOnline(true);
                    convModel.setMessages(messageList);
                    r.copyToRealmOrUpdate(convModel);
                }
            });
            PreferenceManager.setSampleDataSeeded(this, true);
            AppHelper.LogCat("Sample conversations seeded successfully");
        } catch (Exception e) {
            AppHelper.LogCat("Error seeding sample conversations: " + e.getMessage());
        } finally {
            realm.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeAd(mAdViewWallet);
        resumeAd(mAdViewMoney);
        resumeAd(mAdViewSpace);
        MainView.setVisibility(View.GONE);
        WhatsCloneApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    protected void onPause() {
        pauseAd(mAdViewWallet);
        pauseAd(mAdViewMoney);
        pauseAd(mAdViewSpace);
        super.onPause();
        MainView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    @Override
    protected void onDestroy() {
        destroyAllBannerAds();
        mInterstitialAd = null;
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        disposables.clear();
        AppHelper.LogCat("Realm.getGlobalInstanceCount " + Realm.getGlobalInstanceCount(WhatsCloneApplication.getRealmDatabaseConfiguration()));
    }

    private static void pauseAd(AdView adView) {
        if (adView != null) adView.pause();
    }

    private static void resumeAd(AdView adView) {
        if (adView != null) adView.resume();
    }

    private static void destroyAd(AdView adView) {
        if (adView != null) adView.destroy();
    }


    private void Permissions() {
        StartupPermissions.requestRequiredPermissions(this);

        if (!hasContactPermissions()) {
            AppHelper.LogCat("Please request contacts permission for wallet contact sync.");
            return;
        }

        requestFullScreenCallAccessIfNeeded();
    }

    private void requestFullScreenCallAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < 34 || fullScreenIntentPromptShown
                || NotificationsManager.canUseFullScreenIntent(this)) {
            return;
        }

        fullScreenIntentPromptShown = true;
        new AlertDialog.Builder(this)
                .setTitle("Allow full-screen calls")
                .setMessage("Enable full-screen notifications so incoming MiMi calls can wake the locked phone.")
                .setPositiveButton("Open settings", (dialog, which) -> {
                    try {
                        startActivity(NotificationsManager.buildFullScreenIntentSettingsIntent(this));
                    } catch (Exception e) {
                        AppHelper.LogCat("Open full-screen notification settings failed: " + e.getMessage());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean hasContactPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestWalletContactSync() {
        Intent intent = new Intent(this, MainService.class);
        intent.setAction(AppConstants.ACTION_SYNC_WALLET_CONTACTS);
        AppHelper.startMainService(this, intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AppConstants.CONTACTS_PERMISSION_REQUEST_CODE) {
            AppHelper.hidePermissionsDialog();
            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CONTACTS_PERMISSION));
        }
        // Pass through other results to fragments (including file upload results)
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConstants.CONTACTS_PERMISSION_REQUEST_CODE
                || requestCode == AppConstants.PERMISSION_REQUEST_CODE) {
            if (hasContactPermissions()) {
                AppHelper.hidePermissionsDialog();
                requestWalletContactSync();
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CONTACTS_PERMISSION));
            }
            if (requestCode == AppConstants.CONTACTS_PERMISSION_REQUEST_CODE) {
                Permissions();
            } else {
                requestFullScreenCallAccessIfNeeded();
            }
        }
    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        switch (pusher.getAction()) {
            case EVENT_BUS_MESSAGE_COUNTER:
                new Handler().postDelayed(this::loadCounter, 500);
                break;
            case EVENT_BUS_NEW_USER_JOINED:
                JSONObject jsonObject = pusher.getJsonObject();
                try {
                    String phone = jsonObject.getString("walletAddress");
                    int senderId = jsonObject.getInt("senderId");
                    new Handler().postDelayed(() -> {
                        Intent mIntent = new Intent("new_user_joined_notification_whatsclone");
                        mIntent.setPackage(getPackageName());
                        mIntent.putExtra("conversationID", 0);
                        mIntent.putExtra("recipientID", senderId);
                        mIntent.putExtra("walletAddress", phone);
                        mIntent.putExtra("message", AppConstants.JOINED_MESSAGE_SMS);
                        sendBroadcast(mIntent);
                    }, 2500);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case AppConstants.EVENT_BUS_NEW_CONTACT_ADDED:
                break;
            case AppConstants.EVENT_BUS_START_CONVERSATION:
                if (viewPager.getCurrentItem() == 4)
                    viewPager.setCurrentItem(1);
                break;
            case AppConstants.EVENT_BUS_ACTION_MODE_STARTED:
                actionModeStarted();
                break;
            case AppConstants.EVENT_BUS_ACTION_MODE_DESTROYED:
                actionModeDestroyed();
                break;

        }


    }


    private void actionModeDestroyed() {
        if (actionModeStarted) {
            actionModeStarted = false;
            tabLayout.setBackgroundColor(AppHelper.getColor(this, R.color.colorPrimary));
            if (AppHelper.isAndroid5()) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(AppHelper.getColor(this, R.color.colorPrimaryDark));
            }
        }
    }

    private void actionModeStarted() {
        if (!actionModeStarted) {
            actionModeStarted = true;
            tabLayout.setBackgroundColor(AppHelper.getColor(this, R.color.colorActionMode));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(AppHelper.getColor(this, R.color.colorActionMode));
            }
        }

    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnecting, boolean isConnected) {
        if (!isConnecting && !isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_not_available), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
        } else if (isConnecting && isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_available), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
        } else {
            AppHelper.Snackbar(this, mView, getString(R.string.waiting_for_network), AppConstants.MESSAGE_COLOR_WARNING, AppConstants.TEXT_COLOR);

        }
    }


    /**
     * methdo to loadCircleImage number of unread messages
     */
    private void loadCounter() {
        int messageCounter = 0;
        try {
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            List<ConversationsModel> conversationsModel1 = realm.where(ConversationsModel.class)
                    .notEqualTo("UnreadMessageCounter", "0")
                    .findAll();
            if (conversationsModel1.size() != 0) {
                messageCounter = conversationsModel1.size();
            }

            View msgCounterView = findViewById(R.id.counterTabMessages);
            if (msgCounterView != null) {
                if (messageCounter == 0) {
                    msgCounterView.setVisibility(View.GONE);
                } else {
                    msgCounterView.setVisibility(View.VISIBLE);
                    if (msgCounterView instanceof TextView) {
                        ((TextView) msgCounterView).setText(String.valueOf(messageCounter));
                    }
                }
            }
            if (!realm.isClosed())
                realm.close();

        } catch (Exception e) {
            AppHelper.LogCat("loadCounter main activity " + e.getMessage());
        }
        NotificationsManager.SetupBadger(this);

    }


    private void getContactInfo() {

        disposables.add(
                APIHelper.initialApiUsersContacts().getContactInfo(PreferenceManager.getID(this))
                        .subscribe(contactsModel -> {
                            AppHelper.LogCat("getContactInfo ");
                        }, throwable -> {
                            AppHelper.LogCat(throwable.getMessage());
                        })
        );
    }

    /**
     * method to check if user connect in an other device
     */
    public void checkIfUserSession() {

        disposables.add(
                APIHelper.initialApiUsersContacts().checkIfUserSession()
                        .subscribe(networkModel -> {
                            if (!networkModel.isConnected()) {
                                if (ForegroundRuning.get().isForeground()) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                                    alert.setMessage(R.string.your_session_expired);
                                    alert.setPositiveButton(R.string.ok, (dialog, which) -> {
                                        PreferenceManager.setToken(MainActivity.this, null);
                                        PreferenceManager.setID(MainActivity.this, 0);
                                        PreferenceManager.setSocketID(MainActivity.this, null);
                                        PreferenceManager.setWalletAddress(MainActivity.this, null);
                                        PreferenceManager.setIsWaitingForSms(MainActivity.this, false);
                                        PreferenceManager.setMobileNumber(MainActivity.this, null);
                                        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                                    alert.setCancelable(false);
                                    alert.show();
                                }
                            }
                        }, throwable -> {
                            AppHelper.LogCat("checkIfUserSession MainActivity " + throwable.getMessage());
                        })
        );
    }


    @Override
    public void onBackPressed() {
        if (viewPager != null && viewPager.getCurrentItem() == 2) {
            androidx.fragment.app.Fragment f = null;
            if (mFragmentStatePagerAdapter != null) {
                f = mFragmentStatePagerAdapter.getRegisteredFragment(2);
            }
            if (f == null) {
                String tag = "android:switcher:" + R.id.viewpager + ":" + 2;
                f = getSupportFragmentManager().findFragmentByTag(tag);
            }
            if (f instanceof com.money.mimi.fragments.home.WalletFragment) {
                if (((com.money.mimi.fragments.home.WalletFragment) f).onBackPressed()) return;
            }
        }
        // Delegate back to MoneyFragment or SpaceFragment when those tabs are visible; never exit app from them
        if (viewPager != null && viewPager.getCurrentItem() == 3) {
            String tag = "android:switcher:" + R.id.viewpager + ":" + 3;
            androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
            if (f instanceof com.money.mimi.fragments.home.MoneyFragment) {
                if (((com.money.mimi.fragments.home.MoneyFragment) f).onBackPressed()) return;
            }
            return; // Money tab active but not handled above; swallow back to avoid exiting app
        }
        if (viewPager != null && viewPager.getCurrentItem() == 4) {
            String tag = "android:switcher:" + R.id.viewpager + ":" + 4;
            androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
            if (f instanceof com.money.mimi.fragments.home.SpaceFragment) {
                if (((com.money.mimi.fragments.home.SpaceFragment) f).onBackPressed()) return;
            }
            return; // Space tab active but not handled above; swallow back to avoid exiting app
        }
        super.onBackPressed();
    }

    public void enterSpaceFullscreen() {
        spaceFullscreen = true;
        // Hide app bar and tabs, ads, and main FAB
        View appBar = findViewById(R.id.appbar);
        if (appBar != null) appBar.setVisibility(View.GONE);
        setBottomNavigationVisible(false);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE);
        if (adParentLyout != null) adParentLyout.setVisibility(View.GONE);
        if (adParentLyoutBottom != null) adParentLyoutBottom.setVisibility(View.GONE);
        if (floatingBtnMain != null) {
            floatingBtnMain.clearAnimation();
            floatingBtnMain.setVisibility(View.GONE);
        }
        // Also hide wedge overlay under tabs
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) wedgeOverlay.setVisibility(View.GONE);
        // Enter fullscreen (keep status bar visible)
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public void exitSpaceFullscreen() {
        spaceFullscreen = false;
        // Exit fullscreen (show status bar)
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Show app bar and tabs again
        View appBar = findViewById(R.id.appbar);
        if (appBar != null) {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() >= 3) {
                appBar.setVisibility(View.GONE);
            } else {
                appBar.setVisibility(View.VISIBLE);
            }
        }
        if (tabLayout != null) tabLayout.setVisibility(View.VISIBLE);
        setBottomNavigationVisible(true);
        if (adParentLyout != null) adParentLyout.setVisibility(View.GONE);
        // Show wedge overlay back under tabs
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) wedgeOverlay.setVisibility(View.VISIBLE);
        updateBannerForTab(mCurrentTab);
    }

    public void enterMoneyFullscreen() {
        spaceFullscreen = true;
        // Hide app bar and tabs, ads, and main FAB
        View appBar = findViewById(R.id.appbar);
        if (appBar != null) appBar.setVisibility(View.GONE);
        setBottomNavigationVisible(false);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE);
        if (adParentLyout != null) adParentLyout.setVisibility(View.GONE);
        if (adParentLyoutBottom != null) adParentLyoutBottom.setVisibility(View.GONE);
        if (floatingBtnMain != null) {
            floatingBtnMain.clearAnimation();
            floatingBtnMain.setVisibility(View.GONE);
        }
        // Also hide wedge overlay under tabs
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) wedgeOverlay.setVisibility(View.GONE);
        // Enter fullscreen (keep status bar visible)
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public void exitMoneyFullscreen() {
        spaceFullscreen = false;
        // Exit fullscreen (show status bar)
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Show app bar and tabs again
        View appBar = findViewById(R.id.appbar);
        if (appBar != null) {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() >= 3) {
                appBar.setVisibility(View.GONE);
            } else {
                appBar.setVisibility(View.VISIBLE);
            }
        }
        if (tabLayout != null) tabLayout.setVisibility(View.VISIBLE);
        setBottomNavigationVisible(true);
        if (adParentLyout != null) adParentLyout.setVisibility(View.GONE);
        // Show wedge overlay back under tabs
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) wedgeOverlay.setVisibility(View.VISIBLE);
        updateBannerForTab(mCurrentTab);
    }

    public void enterWalletFullscreen() {
        View appBar = findViewById(R.id.appbar);
        if (appBar != null) appBar.setVisibility(View.GONE);
        setBottomNavigationVisible(false);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE);
        if (adParentLyout != null) adParentLyout.setVisibility(View.GONE);
        if (adParentLyoutBottom != null) adParentLyoutBottom.setVisibility(View.GONE);
        if (floatingBtnMain != null) {
            floatingBtnMain.clearAnimation();
            floatingBtnMain.setVisibility(View.GONE);
        }
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) wedgeOverlay.setVisibility(View.GONE);
    }

    public void exitWalletFullscreen() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View appBar = findViewById(R.id.appbar);
        if (appBar != null) appBar.setVisibility(View.VISIBLE);
        setBottomNavigationVisible(true);
        if (tabLayout != null) tabLayout.setVisibility(View.VISIBLE);
        if (adParentLyout != null) adParentLyout.setVisibility(View.GONE);
        if (floatingBtnMain != null) {
            floatingBtnMain.clearAnimation();
            floatingBtnMain.setVisibility(View.GONE);
        }
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) wedgeOverlay.setVisibility(View.VISIBLE);
        updateBannerForTab(mCurrentTab);
    }

    @SuppressWarnings("deprecation")
    private void applySystemWindowInsets() {
        View root = findViewById(R.id.main_activity);
        View statusBarScrim = findViewById(R.id.status_bar_scrim);
        View bottomTabsContainer = findViewById(R.id.bottom_tabs_container);
        if (root == null || bottomTabsContainer == null) {
            return;
        }

        final int baseBottomPadding = bottomTabsContainer.getPaddingBottom();
        final int baseFabBottomMargin;
        if (floatingBtnMain != null && floatingBtnMain.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            baseFabBottomMargin = ((FrameLayout.LayoutParams) floatingBtnMain.getLayoutParams()).bottomMargin;
        } else {
            baseFabBottomMargin = 0;
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusTop = insets.getSystemWindowInsetTop();
            int navigationBottom = insets.getSystemWindowInsetBottom();

            if (statusBarScrim != null) {
                ViewGroup.LayoutParams statusParams = statusBarScrim.getLayoutParams();
                if (statusParams != null && statusParams.height != statusTop) {
                    statusParams.height = statusTop;
                    statusBarScrim.setLayoutParams(statusParams);
                }
            }

            bottomTabsContainer.setPadding(
                    bottomTabsContainer.getPaddingLeft(),
                    bottomTabsContainer.getPaddingTop(),
                    bottomTabsContainer.getPaddingRight(),
                    baseBottomPadding + navigationBottom);

            if (floatingBtnMain != null && floatingBtnMain.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) floatingBtnMain.getLayoutParams();
                params.bottomMargin = baseFabBottomMargin + navigationBottom;
                floatingBtnMain.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void setBottomNavigationVisible(boolean visible) {
        View bottomTabsContainer = findViewById(R.id.bottom_tabs_container);
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (bottomTabsContainer != null) {
            bottomTabsContainer.setVisibility(visibility);
        }
        if (tabLayout != null) {
            tabLayout.setVisibility(visibility);
        }
        View wedgeOverlay = findViewById(R.id.bottom_wedge_overlay);
        if (wedgeOverlay != null) {
            wedgeOverlay.setVisibility(visibility);
        }
    }

    private void registerFCM() {
        try {
            Intent fcmIntent = new Intent(this, com.money.mimi.services.firebase.RegistrationIntentService.class);
            com.money.mimi.services.firebase.RegistrationIntentService.enqueueWork(this, fcmIntent);
        } catch (Exception e) {
            AppHelper.LogCat("registerFCM error: " + e.getMessage());
        }
    }


}
