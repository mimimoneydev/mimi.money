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
import com.money.mimi.adapters.recyclerView.messages.ConversationsAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.presenters.messages.SearchConversationsPresenter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by Abderrahim El imame on 8/12/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class SearchConversationsActivity extends AppCompatActivity {


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

    private ConversationsAdapter mConversationsAdapter;
    private SearchConversationsPresenter mSearchConversationsPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        searchInput.setFocusable(true);
        initializerSearchView(searchInput, clearBtn);
        initializerView();
        setTypeFaces();
        mSearchConversationsPresenter = new SearchConversationsPresenter(this);
        mSearchConversationsPresenter.onCreate();
    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            searchInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        searchList.setLayoutManager(mLinearLayoutManager);
        mConversationsAdapter = new ConversationsAdapter();
        searchList.setAdapter(mConversationsAdapter);
        closeBtn.setOnClickListener(v -> closeSearchView());
        clearBtn.setOnClickListener(v -> clearSearchView());
        //fix slow recyclerview start
        searchList.setHasFixedSize(true);
        searchList.setItemViewCacheSize(10);
        searchList.setDrawingCacheEnabled(true);
        searchList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        ///fix slow recyclerview end

    }

    /**
     * method to show conversations list
     *
     * @param conversationsModels this is the parameter for  ShowConversation  method
     */
    public void ShowConversation(List<ConversationsModel> conversationsModels) {
        if (conversationsModels.size() != 0) {
            RealmList<ConversationsModel> conversationsModels1 = new RealmList<ConversationsModel>();
            conversationsModels1.addAll(conversationsModels);
            mConversationsAdapter.setConversations(conversationsModels1);
            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        } else {
            searchList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to clear/reset   the search view
     */
    public void clearSearchView() {
        if (searchInput.getText() != null)
            searchInput.setText("");
        searchList.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
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
        mSearchConversationsPresenter.onDestroy();
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
                mConversationsAdapter.setString(s.toString());
                Search(s.toString().trim());
                clearSearchBtn.setVisibility(View.VISIBLE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    clearSearchBtn.setVisibility(View.GONE);
                }
            }
        });

    }

    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("Conversation search " + throwable.getMessage());
    }

    /**
     * method to start searching
     *
     * @param string this is parameter for  Search  method
     */
    public void Search(String string) {

        final List<ConversationsModel> filteredModelList;
        filteredModelList = FilterList(string);
        if (filteredModelList.size() != 0) {

            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            mConversationsAdapter.animateTo(filteredModelList);
            searchList.scrollToPosition(0);
        } else {
            searchList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to filter the conversation list
     *
     * @param query this is parameter for   method
     * @return this is what  method will return
     */
    private List<ConversationsModel> FilterList(String query) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<ConversationsModel> conversationsModels = realm.where(ConversationsModel.class)
                .contains("RecipientUsername", query, Case.INSENSITIVE)
                .findAll();
        return conversationsModels;
    }
}
