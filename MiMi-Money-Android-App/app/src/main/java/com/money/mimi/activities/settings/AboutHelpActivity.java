package com.money.mimi.activities.settings;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.activities.PrivacyActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Abderrahim El imame on 8/17/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class AboutHelpActivity extends AppCompatActivity {
    @BindView(R.id.app_bar)
    Toolbar toolbar;
    @BindView(R.id.about_app_text)
    TextView abboutText;
    @BindView(R.id.lang_app_text)
    TextView langAppText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_help);
        ButterKnife.bind(this);
        setupToolbar();
        setTypeFaces();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_feed_back);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            abboutText.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            langAppText.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }


    @SuppressWarnings("unused")
    @OnClick(R.id.privacy_termes)
    public void launchPrivacyActivity() {
        AppHelper.LaunchActivity(this, PrivacyActivity.class);
    }


    @SuppressWarnings("unused")
    @OnClick(R.id.about_app)
    public void launchAboutActivity() {
        AppHelper.LaunchActivity(this, AboutActivity.class);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.lang_app)
    public void launchLanguageActivity() {
        AppHelper.LaunchActivity(this, PreferenceLanguageActivity.class);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }
}
