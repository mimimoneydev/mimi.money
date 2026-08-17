package com.money.mimi.activities.search;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.money.mimi.R;
import com.money.mimi.adapters.others.TextWatcherAdapter;
import com.money.mimi.adapters.recyclerView.calls.CallsAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.presenters.calls.SearchCallsPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.RealmList;

/**
 * Created by Abderrahim El imame on 8/12/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class SearchCallsActivity extends AppCompatActivity {


    @BindView(R.id.close_btn_search_view)
    ImageView closeBtn;
    @BindView(R.id.search_input)
    TextInputEditText searchInput;
    @BindView(R.id.clear_btn_search_view)
    ImageView clearBtn;
    @BindView(R.id.searchList)
    RecyclerView searchList;

    @BindView(R.id.empty)
    LinearLayout emptyLayout;
    private CallsAdapter mCallsAdapter;
    private SearchCallsPresenter mSearchCallsPresenter;
    private final List<CallsModel> allCalls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        searchInput.setFocusable(true);
        initializerView();
        initializerSearchView(searchInput, clearBtn);
        setTypeFaces();
        mSearchCallsPresenter = new SearchCallsPresenter(this);
        mSearchCallsPresenter.onCreate();
    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            searchInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    /**
     * method to initialize the  view
     */
    private void initializerView() {
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        searchList.setLayoutManager(mLinearLayoutManager);
        mCallsAdapter = new CallsAdapter();
        searchList.setAdapter(mCallsAdapter);
        //fix slow recyclerview start
        searchList.setHasFixedSize(true);
        searchList.setItemViewCacheSize(10);
        searchList.setDrawingCacheEnabled(true);
        searchList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        ///fix slow recyclerview end
        closeBtn.setOnClickListener(v -> closeSearchView());
        clearBtn.setOnClickListener(v -> clearSearchView());
    }

    /**
     * method to show calls list
     *
     * @param contactsModelList this is parameter for  ShowContacts method
     */
    public void ShowCalls(List<CallsModel> contactsModelList) {


        allCalls.clear();
        if (contactsModelList != null) {
            allCalls.addAll(contactsModelList);
        }

        if (!allCalls.isEmpty()) {
            RealmList<CallsModel> callsModels = new RealmList<CallsModel>();
            callsModels.addAll(allCalls);
            mCallsAdapter.setCalls(callsModels);
            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        } else {
            searchList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to clear/reset the search view
     */
    public void clearSearchView() {
        if (searchInput.getText() != null) {
            searchInput.setText("");
            showCalls(allCalls);
            searchList.setVisibility(allCalls.isEmpty() ? View.GONE : View.VISIBLE);
            emptyLayout.setVisibility(allCalls.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * method to close the search view
     */
    public void closeSearchView() {
        finish();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSearchCallsPresenter != null) {
            mSearchCallsPresenter.onDestroy();
        }
    }


    /**
     * method to initialize the search view
     */
    public void initializerSearchView(TextInputEditText searchInput, ImageView clearSearchBtn) {

        final Context context = this;
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

        });
        searchInput.addTextChangedListener(new TextWatcherAdapter() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                clearSearchBtn.setVisibility(View.GONE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString();
                if (mCallsAdapter != null) {
                    mCallsAdapter.setString(query);
                }
                Search(query.trim());
                clearSearchBtn.setVisibility(query.length() == 0 ? View.GONE : View.VISIBLE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    clearSearchBtn.setVisibility(View.GONE);
                    showCalls(allCalls);
                }
            }
        });

    }

    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("Search contacts " + throwable.getMessage());
    }

    /**
     * method to start searching
     *
     * @param string this  is parameter for Search method
     */
    public void Search(String string) {

        List<CallsModel> filteredModelList;
        filteredModelList = FilterList(string);
        if (filteredModelList.size() != 0) {
            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            showCalls(filteredModelList);
            searchList.scrollToPosition(0);
        } else {
            searchList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to filter the list of calls
     *
     * @param query this parameter for FilterList  method
     * @return this for what method will return
     */
    private List<CallsModel> FilterList(String query) {
        List<CallsModel> filteredCalls = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredCalls.addAll(allCalls);
            return filteredCalls;
        }

        String normalizedQuery = query.toLowerCase(Locale.getDefault());
        for (CallsModel callsModel : allCalls) {
            if (matchesCall(callsModel, normalizedQuery)) {
                filteredCalls.add(callsModel);
            }
        }
        return filteredCalls;
    }

    private boolean matchesCall(CallsModel callsModel, String normalizedQuery) {
        if (callsModel == null || normalizedQuery == null) {
            return false;
        }

        ContactsModel contactsModel = callsModel.getContactsModel();
        if (containsIgnoreCase(contactsModel != null ? contactsModel.getUsername() : null, normalizedQuery)) {
            return true;
        }
        if (containsIgnoreCase(callsModel.getWalletAddress(), normalizedQuery)) {
            return true;
        }
        if (containsIgnoreCase(callsModel.getType(), normalizedQuery)) {
            return true;
        }
        return containsIgnoreCase(callsModel.getDate(), normalizedQuery);
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(normalizedQuery);
    }

    private void showCalls(List<CallsModel> callsModels) {
        RealmList<CallsModel> visibleCalls = new RealmList<>();
        if (callsModels != null) {
            visibleCalls.addAll(callsModels);
        }
        mCallsAdapter.setCalls(visibleCalls);
    }
}
