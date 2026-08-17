package com.money.mimi.fragments.home;

import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.money.mimi.R;
import com.money.mimi.adapters.recyclerView.calls.CallsAdapter;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.interfaces.LoadingData;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.presenters.calls.CallsPresenter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.RealmList;

/**
 * Created by Abderrahim El imame on 12/3/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CallsFragment extends Fragment implements LoadingData {

    @BindView(R.id.CallsList)
    RecyclerView CallsList;
    @BindView(R.id.empty)
    LinearLayout emptyConversations;

    @BindView(R.id.swipeCalls)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private CallsAdapter mCallsAdapter;
    private CallsPresenter mCallsPresenter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_calls, container, false);
        ButterKnife.bind(this, mView);
        initializerView();
        mCallsPresenter = new CallsPresenter(this);
        mCallsPresenter.onCreate();
        return mView;
    }


    /**
     * method to initialize the view
     */
    private void initializerView() {
        setHasOptionsMenu(true);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mCallsAdapter = new CallsAdapter(CallsList);
        mCallsAdapter.setAdContext(getActivity());
        CallsList.setLayoutManager(mLinearLayoutManager);
        CallsList.setAdapter(mCallsAdapter);
        CallsList.setItemAnimator(new DefaultItemAnimator());
        CallsList.getItemAnimator().setChangeDuration(0);

        //fix slow recyclerview start
        CallsList.setHasFixedSize(true);
        CallsList.setItemViewCacheSize(10);
        CallsList.setDrawingCacheEnabled(true);
        CallsList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        ///fix slow recyclerview end
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorAccent, R.color.colorGreenLight);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mCallsPresenter.onRefresh());

    }


    /**
     * method to show calls list
     *
     * @param callsModelList this is parameter for  UpdateCalls  method
     */
    public void UpdateCalls(List<CallsModel> callsModelList) {

        if (callsModelList.size() != 0) {
            CallsList.setVisibility(View.VISIBLE);
            emptyConversations.setVisibility(View.GONE);
            RealmList<CallsModel> callsModels = new RealmList<CallsModel>();
            callsModels.addAll(callsModelList);
            mCallsAdapter.setCalls(callsModels);
        } else {
            CallsList.setVisibility(View.GONE);
            emptyConversations.setVisibility(View.VISIBLE);
        }
    }

    public void refreshInlineAds() {
        if (mCallsAdapter != null) {
            mCallsAdapter.refreshAds();
        }
    }

    @Override
    public void onDestroyView() {
        if (mCallsAdapter != null) {
            mCallsAdapter.releaseAds();
        }
        super.onDestroyView();
    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        switch (pusher.getAction()) {
            case AppConstants.EVENT_BUS_CALL_NEW_ROW:
                new Handler().postDelayed(() -> addCallEventMainThread(pusher.getCallId()), 500);
                break;
            case AppConstants.EVENT_UPDATE_CALL_OLD_ROW:
                new Handler().postDelayed(() -> mCallsAdapter.updateCallItem(pusher.getCallId()), 500);
                break;
            case AppConstants.EVENT_BUS_DELETE_CALL_ITEM:
                if (pusher.getCallId() != 0)
                    mCallsAdapter.DeleteCallItem(pusher.getCallId());
                else
                    mCallsPresenter.onRefresh();
                break;
        }
    }

    /**
     * method to add a new call to list calls
     *
     * @param callId this is the parameter for addCallEventMainThread
     */

    private void addCallEventMainThread(int callId) {
        mCallsAdapter.addCallItem(callId);
        CallsList.setVisibility(View.VISIBLE);
        emptyConversations.setVisibility(View.GONE);
        CallsList.scrollToPosition(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCallsPresenter != null)
            mCallsPresenter.onDestroy();
    }

    @Override
    public void onShowLoading() {
        if (!mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onHideLoading() {
        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
    }


    @Override
    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat(throwable);
        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
    }

}
