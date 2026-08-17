package com.money.mimi.activities.popups;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.presenters.users.StatusPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Abderrahim El imame on 28/04/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class StatusDelete extends Activity {

    @BindView(R.id.deleteStatus)
    TextView deleteStatus;

    private StatusPresenter mStatusPresenter;
    private int statusID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_status_delete);
        ButterKnife.bind(this);
        setTypeFaces();
        if (getIntent().hasExtra("statusID") && getIntent().getExtras().getInt("statusID") != 0) {
            statusID = getIntent().getExtras().getInt("statusID");
        }
        mStatusPresenter = new StatusPresenter(this);
    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            deleteStatus.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.deleteStatus)
    public void DeleteStatus() {
        mStatusPresenter.DeleteStatus(statusID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusPresenter.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStatusPresenter.onDestroy();
    }
}
