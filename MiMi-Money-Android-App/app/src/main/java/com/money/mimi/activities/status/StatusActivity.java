package com.money.mimi.activities.status;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.money.mimi.R;
import com.money.mimi.adapters.recyclerView.StatusAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.AdMobHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.interfaces.NetworkListener;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.status.StatusModel;
import com.money.mimi.presenters.users.StatusPresenter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;


/**
 * Created by Abderrahim El imame on 28/04/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class StatusActivity extends AppCompatActivity implements NetworkListener {
    private RewardedAd mRewardedAd;


    @BindView(R.id.currentStatus)
    EmojiconTextView currentStatus;
    @BindView(R.id.editCurrentStatusBtn)
    AppCompatTextView editCurrentStatusBtn;
    @BindView(R.id.StatusList)
    RecyclerView StatusList;
    @BindView(R.id.ParentLayoutStatus)
    LinearLayout ParentLayoutStatus;


    private StatusAdapter mStatusAdapter;
    private StatusPresenter mStatusPresenter;
    private int statusID;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        ButterKnife.bind(this);

        if (PreferenceManager.ShowVideoAds(this)) {
            if (PreferenceManager.getUnitVideoAdsID(this) != null) {
                loadRewardedVideoAd();
            }
        }
        setTypeFaces();
        initializerView();
        mStatusPresenter = new StatusPresenter(this);
        mStatusPresenter.onCreate();
        setupToolbar();


    }

    private void showRewardedVideo() {
        if (mRewardedAd != null && !isFinishing() && !isDestroyed()) {
            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mRewardedAd = null;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mRewardedAd = null;
                }
            });
            mRewardedAd.show(this, (OnUserEarnedRewardListener) rewardItem -> { });
        }
    }

    private void loadRewardedVideoAd() {
        String adUnitId = AdMobHelper.rewardedId(this, PreferenceManager.getUnitVideoAdsID(this));
        if (adUnitId == null) return;
        RewardedAd.load(this, adUnitId, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        showRewardedVideo();
                    }

                    @Override
                    public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                        mRewardedAd = null;
                    }
                });
    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            currentStatus.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * method to initialize the view
     */
    public void initializerView() {
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mStatusAdapter = new StatusAdapter(this);
        StatusList.setLayoutManager(mLinearLayoutManager);
        StatusList.setAdapter(mStatusAdapter);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.editCurrentStatusBtn)
    void launchEditStatus(View v) {
        Intent mIntent = new Intent(this, EditStatusActivity.class);
        mIntent.putExtra("statusID", statusID);
        mIntent.putExtra("currentStatus", currentStatus.getText().toString().trim());
        startActivity(mIntent);
    }

    /**
     * method to show status list
     *
     * @param statusModels this is parameter for  ShowStatus   method
     */
    public void ShowStatus(List<StatusModel> statusModels) {
        mStatusAdapter.setStatus(statusModels);
        for (StatusModel statusModel : statusModels) {
            if (statusModel.getCurrent()) {
                String status = UtilsString.unescapeJava(statusModel.getStatus());
                currentStatus.setText(status);
                break;
            }
        }
        mStatusPresenter.getCurrentStatus();
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        mStatusPresenter.onEventPush(pusher);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.status_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                AnimationsUtil.setSlideOutAnimation(this);
                break;
            case R.id.deleteStatus:
                mStatusPresenter.DeleteAllStatus();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRewardedAd = null;
        mStatusPresenter.onDestroy();
    }

    /**
     * method to show the current status
     *
     * @param statusModel this is parameter for  ShowCurrentStatus   method
     */
    public void ShowCurrentStatus(String statusModel) {
        String status = UtilsString.unescapeJava(statusModel);
        currentStatus.setText(status);
    }

    /**
     * method to show the current status
     *
     * @param statusModel this is parameter for  ShowCurrentStatus   method
     */
    public void ShowCurrentStatus(StatusModel statusModel) {
        statusID = statusModel.getId();
        String status = UtilsString.unescapeJava(statusModel.getStatus());
        currentStatus.setText(status);

    }

    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("status error" + throwable.getMessage());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusPresenter.onResume();
        WhatsCloneApplication.getInstance().setConnectivityListener(this);
    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnecting, boolean isConnected) {
        if (!isConnecting && !isConnected) {
            AppHelper.Snackbar(this, ParentLayoutStatus, getString(R.string.connection_is_not_available), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
        } else if (isConnecting && isConnected) {
            AppHelper.Snackbar(this, ParentLayoutStatus, getString(R.string.connection_is_available), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
        } else {
            AppHelper.Snackbar(this, ParentLayoutStatus, getString(R.string.waiting_for_network), AppConstants.MESSAGE_COLOR_WARNING, AppConstants.TEXT_COLOR);

        }
    }

    public void deleteStatus(int statusID) {
        mStatusAdapter.DeleteStatusItem(statusID);
    }
}
