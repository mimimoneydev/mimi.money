package com.money.mimi.activities.profile;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.presenters.users.EditProfilePresenter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by abderrahimelimame on 6/9/16.
 * Email : abderrahim.elimame@gmail.com
 */

public class EditUsernameActivity extends AppCompatActivity {
    @BindView(R.id.cancelStatus)
    TextView cancelStatusBtn;
    @BindView(R.id.OkStatus)
    TextView OkStatusBtn;
    @BindView(R.id.StatusWrapper)
    EditText StatusWrapper;
    @BindView(R.id.emoticonBtn)
    ImageView emoticonBtn;

    private String oldName;
    private int targetUserID;
    private EditProfilePresenter mEditProfilePresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_status);
        ButterKnife.bind(this);
        initializerView();
        setTypeFaces();
        mEditProfilePresenter = new EditProfilePresenter(this, true);
        mEditProfilePresenter.onCreate();
        if (getIntent().getExtras() != null) {
            oldName = getIntent().getStringExtra("currentUsername");
            targetUserID = getIntent().getIntExtra("targetUserID", PreferenceManager.getID(this));
        } else {
            targetUserID = PreferenceManager.getID(this);
        }
        StatusWrapper.setText(oldName);

    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            cancelStatusBtn.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            OkStatusBtn.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            StatusWrapper.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        emoticonBtn.setVisibility(View.GONE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_edit_name);
        cancelStatusBtn.setOnClickListener(v -> finish());
        OkStatusBtn.setOnClickListener(v -> {
            String newUsername = StatusWrapper.getText().toString().trim();
            try {
                mEditProfilePresenter.EditCurrentName(newUsername, false, targetUserID);
            } catch (Exception e) {
                AppHelper.LogCat("Edit  name  Exception " + e.getMessage());
            }

        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    public int getTargetUserID() {
        return targetUserID;
    }
}
