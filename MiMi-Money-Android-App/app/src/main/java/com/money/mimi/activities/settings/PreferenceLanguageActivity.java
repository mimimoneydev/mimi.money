package com.money.mimi.activities.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.activities.main.welcome.SplashScreenActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Abderrahim El imame on 3/13/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class PreferenceLanguageActivity extends AppCompatActivity {


    @BindView(R.id.indicator_english)
    TextView indicatorEnglish;
    @BindView(R.id.indicator_french)
    TextView indicatorFrench;
    @BindView(R.id.indicator_german)
    TextView indicatorGerman;
    @BindView(R.id.indicator_romaneste)
    TextView indicatorRomaneste;
    @BindView(R.id.indicator_srbski)
    TextView indicatorSrbski;
    @BindView(R.id.indicator_bosnisch)
    TextView indicatorBosnisch;
    @BindView(R.id.indicator_turski)
    TextView indicatorTurski;
    @BindView(R.id.indicator_tschechisch)
    TextView indicatorTschechisch;
    @BindView(R.id.indicator_grichisch)
    TextView indicatorGrichisch;
    @BindView(R.id.indicator_spanisch)
    TextView indicatorSpanisch;
    @BindView(R.id.indicator_kroatisch)
    TextView indicatorKroatisch;
    @BindView(R.id.indicator_indonesisch)
    TextView indicatorIndonesisch;
    @BindView(R.id.indicator_mazedonisch)
    TextView indicatorMazedonisch;
    @BindView(R.id.indicator_rusisch)
    TextView indicatorRusisch;
    @BindView(R.id.indicator_slovenisch)
    TextView indicatorSlovenisch;

    @BindView(R.id.english_btn)
    LinearLayout EnglishBtn;
    @BindView(R.id.french_btn)
    LinearLayout FrenchBtn;
    @BindView(R.id.german_btn)
    LinearLayout GermanBtn;
    @BindView(R.id.swahili_btn)
    LinearLayout SwahiliBtn;
    @BindView(R.id.srbski_btn)
    LinearLayout SrbskiBtn;
    @BindView(R.id.arabic_btn)
    LinearLayout ArabicBtn;
    @BindView(R.id.turkish_btn)
    LinearLayout TurkishBtn;
    @BindView(R.id.chinese_btn)
    LinearLayout ChineseBtn;
    @BindView(R.id.portuguese_btn)
    LinearLayout PortugueseBtn;
    @BindView(R.id.spanish_btn)
    LinearLayout SpanishBtn;
    @BindView(R.id.kroatisch_btn)
    LinearLayout KroatischBtn;
    @BindView(R.id.indonesian_btn)
    LinearLayout IndonesianBtn;
    @BindView(R.id.mazedonisch_btn)
    LinearLayout MazedonischBtn;
    @BindView(R.id.russian_btn)
    LinearLayout RussianBtn;
    @BindView(R.id.slovenisch_btn)
    LinearLayout SlovenischBtn;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        ButterKnife.bind(this);
        setupToolbar();
        applyThemeColors();

        EnglishBtn.setOnClickListener(view -> {
            if (indicatorEnglish.getVisibility() == View.GONE) {
                ChangeLanguage("en", "US");
            }
        });
        FrenchBtn.setOnClickListener(view -> {
            if (indicatorFrench.getVisibility() == View.GONE) {
                ChangeLanguage("fr", null);
            }
        });
        ////////////
        GermanBtn.setOnClickListener(view -> {
            if (indicatorGerman.getVisibility() == View.GONE) {
                ChangeLanguage("de", null);
            }
        });
        SwahiliBtn.setOnClickListener(view -> {
            if (indicatorRomaneste.getVisibility() == View.GONE) {
                ChangeLanguage("sw", null);
            }
        });
        SrbskiBtn.setOnClickListener(view -> {
            if (indicatorSrbski.getVisibility() == View.GONE) {
                ChangeLanguage("de", null);
            }
        });
        ArabicBtn.setOnClickListener(view -> {
            if (indicatorBosnisch.getVisibility() == View.GONE) {
                ChangeLanguage("ar", null);
            }
        });
        TurkishBtn.setOnClickListener(view -> {
            if (indicatorTurski.getVisibility() == View.GONE) {
                ChangeLanguage("tr", null);
            }
        });
        ChineseBtn.setOnClickListener(view -> {
            if (indicatorTschechisch.getVisibility() == View.GONE) {
                ChangeLanguage("zh", null);
            }
        });
        PortugueseBtn.setOnClickListener(view -> {
            if (indicatorGrichisch.getVisibility() == View.GONE) {
                ChangeLanguage("pt", null);
            }
        });
        SpanishBtn.setOnClickListener(view -> {
            if (indicatorSpanisch.getVisibility() == View.GONE) {
                ChangeLanguage("es", null);
            }
        });
        KroatischBtn.setOnClickListener(view -> {
            if (indicatorKroatisch.getVisibility() == View.GONE) {
                ChangeLanguage("hr", null);
            }
        });
        IndonesianBtn.setOnClickListener(view -> {
            if (indicatorIndonesisch.getVisibility() == View.GONE) {
                ChangeLanguage("id", null);
            }
        });
        MazedonischBtn.setOnClickListener(view -> {
            if (indicatorMazedonisch.getVisibility() == View.GONE) {
                ChangeLanguage("mk", null);
            }
        });
        RussianBtn.setOnClickListener(view -> {
            if (indicatorRusisch.getVisibility() == View.GONE) {
                ChangeLanguage("ru", null);
            }
        });
        SlovenischBtn.setOnClickListener(view -> {
            if (indicatorSlovenisch.getVisibility() == View.GONE) {
                ChangeLanguage("sl", null);
            }
        });
        // Show English, French, Spanish, Arabic, Swahili, Portuguese
        // Also show: German, Turkish, Chinese, Indonesian, Russian
        // Keep hidden: Serbisch, Kroatisch, Mazedonisch, Slovenisch
        SrbskiBtn.setVisibility(View.GONE);
        KroatischBtn.setVisibility(View.GONE);
        MazedonischBtn.setVisibility(View.GONE);
        SlovenischBtn.setVisibility(View.GONE);


        loadLocale();
    }


    /**
     * method to setup toolbar
     */
    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_language);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppHelper.LogCat("onConfigurationChanged " + newConfig.locale);
    }


    public void ChangeLanguage(String lang, String country) {
        if (lang.equalsIgnoreCase(""))
            return;
        saveLocale(lang);
        if (country == null)
            setDefaultLocale(this, new Locale(lang));
        else
            setDefaultLocale(this, new Locale(lang, country));
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.you_need_to_restart_the_application);
        alert.setPositiveButton(R.string.ok, (dialog, which) -> {
            Intent mainIntent = new Intent(this, SplashScreenActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });
        alert.setCancelable(false);
        alert.show();
    }

    @SuppressWarnings("deprecation")
    protected void setDefaultLocale(Context context, Locale locale) {
        Locale.setDefault(locale);
        Configuration appConfig = new Configuration();
        appConfig.locale = locale;
        context.getResources().updateConfiguration(appConfig, context.getResources().getDisplayMetrics());

    }

    public void reload() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();

        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    public void saveLocale(String lang) {
        PreferenceManager.setLanguage(this, lang);
    }

    public void loadLocale() {
        String language = PreferenceManager.getLanguage(this);
        AppHelper.LogCat("language " + language + " getDefault " + Locale.getDefault());
        switch (language) {
            case "fr":
                indicatorGerman.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.VISIBLE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "en":
                indicatorEnglish.setVisibility(View.VISIBLE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "de":
                indicatorGerman.setVisibility(View.VISIBLE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "sr":
                indicatorSrbski.setVisibility(View.VISIBLE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "ro":
                indicatorRomaneste.setVisibility(View.VISIBLE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "tr":
                indicatorTurski.setVisibility(View.VISIBLE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "bs":
                indicatorBosnisch.setVisibility(View.VISIBLE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "zh":
                indicatorTschechisch.setVisibility(View.VISIBLE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "el":
                indicatorGrichisch.setVisibility(View.VISIBLE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "es":
                indicatorSpanisch.setVisibility(View.VISIBLE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "hr":
                indicatorKroatisch.setVisibility(View.VISIBLE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "id":
                indicatorIndonesisch.setVisibility(View.VISIBLE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "mk":
                indicatorMazedonisch.setVisibility(View.VISIBLE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "ru":
                indicatorRusisch.setVisibility(View.VISIBLE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "sl":
                indicatorSlovenisch.setVisibility(View.VISIBLE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);

                break;
            case "ar":
                indicatorBosnisch.setVisibility(View.VISIBLE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "pt":
                indicatorGrichisch.setVisibility(View.VISIBLE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorRomaneste.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;
            case "sw":
                indicatorRomaneste.setVisibility(View.VISIBLE);
                indicatorFrench.setVisibility(View.GONE);
                indicatorEnglish.setVisibility(View.GONE);
                indicatorGerman.setVisibility(View.GONE);
                indicatorSrbski.setVisibility(View.GONE);
                indicatorBosnisch.setVisibility(View.GONE);
                indicatorTurski.setVisibility(View.GONE);
                indicatorTschechisch.setVisibility(View.GONE);
                indicatorGrichisch.setVisibility(View.GONE);
                indicatorSpanisch.setVisibility(View.GONE);
                indicatorKroatisch.setVisibility(View.GONE);
                indicatorIndonesisch.setVisibility(View.GONE);
                indicatorMazedonisch.setVisibility(View.GONE);
                indicatorRusisch.setVisibility(View.GONE);
                indicatorSlovenisch.setVisibility(View.GONE);

                break;


        }

    }
    private void applyThemeColors() {
        // Set background to theme-aware color and update text colors for dark mode
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        android.view.ViewGroup content = (android.view.ViewGroup) findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            View root = content.getChildAt(0);
            if (root != null) root.setBackgroundColor(tv.data);
            applyPrimaryTextColorRec(root);
        }
    }

    private void applyPrimaryTextColorRec(View v) {
        if (v == null) return;
        if (v instanceof TextView) {
            int id = v.getId();
            if (id != View.NO_ID) {
                try {
                    String name = getResources().getResourceEntryName(id);
                    if (name != null && (name.startsWith("indicator_") || "short_description_language".equals(name))) {
                        return; // keep accent color for indicators and header
                    }
                } catch (Exception ignored) {}
            }
            ((TextView) v).setTextColor(AppHelper.getColor(this, R.color.colorPrimaryText));
        } else if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                applyPrimaryTextColorRec(g.getChildAt(i));
            }
        }
    }

}