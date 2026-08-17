package com.money.mimi.activities.main.welcome;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.TelephonyManager;
import com.google.android.material.textfield.TextInputEditText;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.activities.CountryActivity;
import com.money.mimi.activities.main.MainActivity;
import com.money.mimi.activities.main.PreMainActivity;
import com.money.mimi.adapters.others.TextWatcherAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.CountriesFetcher;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.StartupPermissions;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.models.CountriesModel;
import com.money.mimi.models.auth.LoginModel;
import com.money.mimi.ui.CustomProgressView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;


/**
 * Created by Abderrahim El imame on 09/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String WALLET_RECOVERY_PASSPHRASE = "";
    private static final String HOSTED_TERMS_PRIVACY_URL = "https://com.mimi.money/api.php?cmd=GetApplicationPrivacy";

    @BindView(R.id.numberPhone)
    AppCompatEditText phoneNumberWrapper;
    @BindView(R.id.btn_request_sms)
    AppCompatTextView btnNext;
    @BindView(R.id.btn_restore_wallet)
    AppCompatTextView btnRestoreWallet;
    @BindView(R.id.secret_recovery_phrase)
    AppCompatEditText secretRecoveryPhraseWrapper;
    @BindView(R.id.btn_request_sms_kit)
    AppCompatTextView btnNextKit;

    @BindView(R.id.progress_bar_load)
    CustomProgressView progressBarLoad;

    @BindView(R.id.progress_bar_load_kit)
    CustomProgressView progressBarLoadKit;

    @BindView(R.id.btn_change_number)
    AppCompatImageView changeNumberBtn;
    @BindView(R.id.viewPagerVertical)
    ViewPager viewPager;

    @BindView(R.id.country_code)
    AppCompatTextView countryCode;
    @BindView(R.id.short_description_phone)
    AppCompatTextView shortDescriptionPhone;
    @BindView(R.id.country_name)
    AppCompatTextView countryName;

    @BindView(R.id.current_mobile_number)
    TextView currentMobileNumber;
    @BindView(R.id.numberPhone_layout_sv)
    NestedScrollView numberPhoneLayoutSv;
    @BindView(R.id.layout_verification_sv)
    NestedScrollView layoutVerificationSv;

    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.logo)
    LinearLayout LogoWelcome;


    @BindView(R.id.registrationTerms)
    TextView registrationTerms;
    @BindView(R.id.terms_privacy_text)
    TextView termsPrivacyText;




    private CountriesModel mSelectedCountry;
    private CountriesFetcher.CountryList mCountries;
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + "closeWelcomeActivity")) {
                finish();
            }
        }
    };

    private final CompositeDisposable disposables = new CompositeDisposable();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(this);
        initializerView();
        StartupPermissions.requestRequiredPermissions(this);

    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            toolbarTitle.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            countryCode.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            currentMobileNumber.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            registrationTerms.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            termsPrivacyText.setTypeface(AppHelper.setTypeFace(this, "Futura"));

        }
    }


    public void getAppSettings() {
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


            int currentAppVersion;
            if (PreferenceManager.getVersionApp(WhatsCloneApplication.getInstance()) != 0) {
                currentAppVersion = PreferenceManager.getVersionApp(WhatsCloneApplication.getInstance());
            } else {
                currentAppVersion = AppHelper.getAppVersionCode(WhatsCloneApplication.getInstance());
            }
            if (currentAppVersion != 0 && currentAppVersion < settingsResponse.getAppVersion()) {
                PreferenceManager.setVersionApp(this, currentAppVersion);
                PreferenceManager.setIsOutDate(this, true);
            } else {
                PreferenceManager.setIsOutDate(this, false);
            }
        }, throwable -> {
            AppHelper.LogCat("Error get settings info Welcome " + throwable.getMessage());
        }));
    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        /**
         * Checking if user already connected
         */

        if (PreferenceManager.getToken(this) != null) {
            NotificationsManager.SetupBadger(this);
            if (PreferenceManager.isHasBackup(this)) {
                Intent intent = new Intent(this, PreMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                AnimationsUtil.setSlideInAnimation(this);
            } else {
                getAppSettings();
                if (PreferenceManager.isNeedProvideInfo(this)) {
                    Intent intent = new Intent(this, CompleteRegistrationActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    AnimationsUtil.setSlideInAnimation(this);
                } else {

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    AnimationsUtil.setSlideInAnimation(this);
                }
            }
        } else {

            mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(getPackageName() + "closeWelcomeActivity");
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
            setTypeFaces();
            if (AppConstants.ENABLE_FACEBOOK_ACCOUNT_KIT) {
                btnNextKit.setText(getString(R.string.get_started));
                btnNextKit.setEnabled(true);
                btnNextKit.setVisibility(View.VISIBLE);
                LogoWelcome.setVisibility(View.VISIBLE);
                layoutVerificationSv.setVisibility(View.GONE);
                numberPhoneLayoutSv.setVisibility(View.GONE);
                viewPager.setVisibility(View.GONE);
            } else {
                btnNextKit.setVisibility(View.GONE);
                btnNext.setText(getString(R.string.next));
                btnNext.setEnabled(true);
                btnNext.setVisibility(View.VISIBLE);
                LogoWelcome.setVisibility(View.GONE);
                layoutVerificationSv.setVisibility(View.GONE);
                numberPhoneLayoutSv.setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.VISIBLE);
            }
            hideKeyboard();

            mCountries = CountriesFetcher.getCountries(this);

            int defaultIdx = mCountries.indexOfIso(AppConstants.DEFAULT_COUNTRY_CODE);
            if (defaultIdx < 0 && !mCountries.isEmpty()) {
                defaultIdx = 0;
            }
            if (defaultIdx >= 0) {
                mSelectedCountry = mCountries.get(defaultIdx);
            }
            countryCode.setText("");
            countryName.setText("");
            shortDescriptionPhone.setText(getString(R.string.enter_wallet_address));

            btnNext.setOnClickListener(this);
            btnRestoreWallet.setOnClickListener(this);
            btnNextKit.setOnClickListener(this);
            countryCode.setOnClickListener(this);
            changeNumberBtn.setOnClickListener(this);
            ViewPagerAdapter adapter = new ViewPagerAdapter();
            viewPager.setAdapter(adapter);

            /**
             * Checking if the device is waiting for sms
             * showing the user Code screen
             */
            // Wallet-only flow: no OTP/SMS screens
            setOnKeyboardDone();
            loadHostedTermsPrivacyText();

        }

    }

    private void loadHostedTermsPrivacyText() {
        termsPrivacyText.setText(R.string.dialog_loading);
        disposables.add(Observable.fromCallable(this::fetchHostedTermsPrivacyText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hostedText -> {
                    if (hostedText != null && !hostedText.trim().isEmpty()) {
                        termsPrivacyText.setText(hostedText.trim());
                    } else {
                        termsPrivacyText.setText("");
                    }
                }, throwable -> {
                    AppHelper.LogCat("Welcome privacy terms load failed: " + throwable.getMessage());
                    termsPrivacyText.setText("");
                }));
    }

    private String fetchHostedTermsPrivacyText() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        Request request = new Request.Builder()
                .url(HOSTED_TERMS_PRIVACY_URL)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            JSONObject json = new JSONObject(body);
            if (response.isSuccessful() && json.optBoolean("success")) {
                return json.optString("message", "");
            }
            throw new IllegalStateException(json.optString("message", "Unable to load hosted terms"));
        }
    }



    /**
     * method to validate user information (repurposed for wallet address)
     */
    private void validateInformation() {
        hideKeyboard();
        String walletAddress = phoneNumberWrapper.getText().toString().trim();
        if (walletAddress != null && !walletAddress.isEmpty()) {
            PreferenceManager.setMobileNumber(this, walletAddress);
            PreferenceManager.setWalletAddress(this, walletAddress);
            PreferenceManager.setWalletAddress(this, walletAddress);
            requestForSMS(walletAddress, getDeviceCountry());
        } else {
            generateWalletAndLogin();
        }
    }

    private String getDeviceCountry() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String simCountry = tm.getSimCountryIso();
                if (simCountry != null && simCountry.length() == 2) {
                    return simCountry.toUpperCase(Locale.US);
                }
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) {
                    return networkCountry.toUpperCase(Locale.US);
                }
            }
        } catch (Exception e) {
            AppHelper.LogCat("TelephonyManager error: " + e.getMessage());
        }
        String localeCountry = Locale.getDefault().getCountry();
        if (localeCountry != null && localeCountry.length() == 2) {
            return localeCountry.toUpperCase(Locale.US);
        }
        return "WALLET";
    }

    /**
     * method to resend a request for SMS
     *
     * @param mobile this is parameter of ResendRequestForSMS method
     */

    /**
     * method to send an SMS request to provider
     *
     * @param mobile  this the first parameter of  requestForSMS method
     * @param country this the second parameter of requestForSMS  method
     */
    private void requestForSMS(String mobile, String country) {
        LoginModel loginModel = new LoginModel();
        loginModel.setCountry(country);
        loginModel.setMobile(mobile);
        loginModel.setWalletAddress(mobile);

        if (AppConstants.ENABLE_FACEBOOK_ACCOUNT_KIT) {
            progressBarLoadKit.setVisibility(View.VISIBLE);
            progressBarLoadKit.setColor(AppHelper.getColor(this, R.color.colorWhite));
            btnNextKit.setText(getString(R.string.set_back_and_keep_calm_you_will_receive_an_sms_of_verification_kit));
            btnNextKit.setEnabled(false);
        } else {

            progressBarLoad.setVisibility(View.VISIBLE);
            progressBarLoad.setColor(AppHelper.getColor(this, R.color.colorWhite));
            btnNext.setText(getString(R.string.please_wait_a_moment));
            btnNext.setEnabled(false);
        }

        disposables.add(APIHelper.initializeAuthService().join(loginModel).subscribe(joinModelResponse -> {
            if (joinModelResponse.isSuccess()) {
                if (!joinModelResponse.isSmsVerification()) {
                    PreferenceManager.setIsWaitingForSms(WelcomeActivity.this, false);
                    // Store user data
                    PreferenceManager.setIsNewUser(WelcomeActivity.this, true);
                    PreferenceManager.setID(WelcomeActivity.this, joinModelResponse.getUserID());
                    PreferenceManager.setToken(WelcomeActivity.this, joinModelResponse.getToken());
                    PreferenceManager.setWalletAddress(WelcomeActivity.this, PreferenceManager.getMobileNumber(WelcomeActivity.this));

                    // Navigate based on user state
                    if (joinModelResponse.isHasBackup()) {
                        if (joinModelResponse.getBackup_hash() != null) {
                            PreferenceManager.saveBackupFolder(WelcomeActivity.this, joinModelResponse.getBackup_hash());
                        }
                        PreferenceManager.setHasBackup(WelcomeActivity.this, true);
                        Intent intent = new Intent(WelcomeActivity.this, PreMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } else if (joinModelResponse.isHasProfile()) {
                        PreferenceManager.setHasBackup(WelcomeActivity.this, false);
                        PreferenceManager.setIsNeedInfo(WelcomeActivity.this, false);
                        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } else {
                        PreferenceManager.setHasBackup(WelcomeActivity.this, false);
                        PreferenceManager.setIsNeedInfo(WelcomeActivity.this, true);
                        Intent intent = new Intent(WelcomeActivity.this, CompleteRegistrationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    finish();
                } else {
                    // Legacy SMS path disabled: default to direct navigation to complete registration
                    PreferenceManager.setIsWaitingForSms(WelcomeActivity.this, false);
                    PreferenceManager.setIsNewUser(WelcomeActivity.this, true);
                    PreferenceManager.setID(WelcomeActivity.this, joinModelResponse.getUserID());
                    PreferenceManager.setToken(WelcomeActivity.this, joinModelResponse.getToken());
                    PreferenceManager.setWalletAddress(WelcomeActivity.this, PreferenceManager.getMobileNumber(WelcomeActivity.this));
                    PreferenceManager.setHasBackup(WelcomeActivity.this, false);
                    PreferenceManager.setIsNeedInfo(WelcomeActivity.this, true);
                    Intent intent = new Intent(WelcomeActivity.this, CompleteRegistrationActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            } else {
                if (AppConstants.ENABLE_FACEBOOK_ACCOUNT_KIT) {
                    btnNextKit.setText(getString(R.string.get_started));
                    btnNextKit.setEnabled(true);
                    progressBarLoadKit.setVisibility(View.GONE);
                } else {

                    btnNext.setText(getString(R.string.next));
                    btnNext.setEnabled(true);
                    progressBarLoad.setVisibility(View.GONE);
                }
            }

        }, throwable -> {
            if (AppConstants.ENABLE_FACEBOOK_ACCOUNT_KIT) {
                btnNextKit.setText(getString(R.string.get_started));
                btnNextKit.setEnabled(true);
                progressBarLoadKit.setVisibility(View.GONE);
            } else {

                btnNext.setText(getString(R.string.next));
                btnNext.setEnabled(true);
                progressBarLoad.setVisibility(View.GONE);
            }
            AppHelper.LogCat("Failed to login into  account " + throwable.getMessage());
            AppHelper.CustomToast(WelcomeActivity.this, getString(R.string.unexpected_reponse_from_server));
            hideKeyboard();
        }));

    }

    /**
     * this if you disabled verification by sms
     *
     * @param code
     */


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_request_sms:
                validateInformation();
                break;
            case R.id.btn_restore_wallet:
                restoreWallet();
                break;
            case R.id.country_code:
                Intent mIntent = new Intent(this, CountryActivity.class);
                startActivityForResult(mIntent, AppConstants.SELECT_COUNTRY);
                break;
            case R.id.btn_change_number:
                viewPager.setCurrentItem(0);
                PreferenceManager.setID(this, 0);
                PreferenceManager.setToken(this, null);
                PreferenceManager.setMobileNumber(this, null);
                PreferenceManager.setIsWaitingForSms(this, false);
                break;
        }
    }

    private class ViewPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((View) object);
        }

        public Object instantiateItem(View collection, int position) {

            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.numberPhone_layout;
                    break;
                case 1:
                    resId = R.id.layout_verification;
                    break;
            }
            return findViewById(resId);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        if (mLocalBroadcastManager != null)
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == AppConstants.SELECT_COUNTRY && data != null) {
            phoneNumberWrapper.setEnabled(true);
            numberPhoneLayoutSv.pageScroll(View.FOCUS_DOWN);
            String codeIso = data.getStringExtra("countryIso");
            int defaultIdx = mCountries != null ? mCountries.indexOfIso(codeIso) : -1;
            if (defaultIdx >= 0) {
                mSelectedCountry = mCountries.get(defaultIdx);
                this.countryCode.setText(mSelectedCountry.getDial_code());
                this.countryName.setText("");
                shortDescriptionPhone.setText(getString(R.string.enter_wallet_address));
            } else {
                AppHelper.LogCat("Selected country was not found: " + codeIso);
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Hide keyboard from phoneEdit field
     */
    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && phoneNumberWrapper != null) {
            inputMethodManager.hideSoftInputFromWindow(phoneNumberWrapper.getWindowToken(), 0);
        }
    }


    /**
     * Set hint number for country
     */


    /**
     * Get PhoneNumber object
     *
     * @return PhoneNumber | null on error
     */


    /**
     * Check if number is valid
     *
     * @return boolean
     */

    public void setOnKeyboardDone() {
        phoneNumberWrapper.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
            }
            return false;
        });
    }


    private void generateWalletAndLogin() {
        progressBarLoad.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);
        btnRestoreWallet.setEnabled(false);
        new Thread(() -> {
            try {
                java.io.File destDir = new java.io.File(getFilesDir(), "wallets");
                if (!destDir.exists()) destDir.mkdirs();
                org.web3j.crypto.Bip39Wallet wallet = org.web3j.crypto.WalletUtils.generateBip39Wallet(WALLET_RECOVERY_PASSPHRASE, destDir);
                String mnemonic = wallet.getMnemonic();
                org.web3j.crypto.Credentials credentials = org.web3j.crypto.WalletUtils.loadBip39Credentials(WALLET_RECOVERY_PASSPHRASE, mnemonic);
                final String address = credentials.getAddress();

                PreferenceManager.setWalletPassword(WelcomeActivity.this, WALLET_RECOVERY_PASSPHRASE);
                PreferenceManager.setWalletMnemonic(WelcomeActivity.this, mnemonic);
                PreferenceManager.setWalletAddress(WelcomeActivity.this, address);
                PreferenceManager.setMobileNumber(WelcomeActivity.this, address);
                PreferenceManager.setWalletAddress(WelcomeActivity.this, address);

                runOnUiThread(() -> {
                    progressBarLoad.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    btnRestoreWallet.setEnabled(true);
                    phoneNumberWrapper.setText(address);
                    requestForSMS(address, getDeviceCountry());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBarLoad.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    btnRestoreWallet.setEnabled(true);
                    phoneNumberWrapper.setError("Wallet creation failed");
                });
            }
        }).start();
    }

    private void restoreWallet() {
        if (secretRecoveryPhraseWrapper.getVisibility() != View.VISIBLE) {
            secretRecoveryPhraseWrapper.setVisibility(View.VISIBLE);
            secretRecoveryPhraseWrapper.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(secretRecoveryPhraseWrapper, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }

        hideKeyboard();
        String mnemonic = normalizeMnemonic(secretRecoveryPhraseWrapper.getText().toString());
        if (mnemonic.isEmpty()) {
            secretRecoveryPhraseWrapper.setError(getString(R.string.enter_secret_recovery_phrase));
            return;
        }
        if (!isLikelyMnemonic(mnemonic)) {
            secretRecoveryPhraseWrapper.setError(getString(R.string.invalid_secret_recovery_phrase));
            return;
        }

        progressBarLoad.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);
        btnRestoreWallet.setEnabled(false);
        new Thread(() -> {
            try {
                org.web3j.crypto.Credentials credentials = org.web3j.crypto.WalletUtils.loadBip39Credentials(WALLET_RECOVERY_PASSPHRASE, mnemonic);
                final String address = credentials.getAddress();

                PreferenceManager.setWalletPassword(WelcomeActivity.this, WALLET_RECOVERY_PASSPHRASE);
                PreferenceManager.setWalletMnemonic(WelcomeActivity.this, mnemonic);
                PreferenceManager.setWalletAddress(WelcomeActivity.this, address);
                PreferenceManager.setMobileNumber(WelcomeActivity.this, address);

                runOnUiThread(() -> {
                    progressBarLoad.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    btnRestoreWallet.setEnabled(true);
                    phoneNumberWrapper.setText(address);
                    requestForSMS(address, getDeviceCountry());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBarLoad.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    btnRestoreWallet.setEnabled(true);
                    secretRecoveryPhraseWrapper.setError(getString(R.string.wallet_restore_failed));
                });
            }
        }).start();
    }

    private static String normalizeMnemonic(String mnemonic) {
        if (mnemonic == null) return "";
        return mnemonic.trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
    }

    private static boolean isLikelyMnemonic(String mnemonic) {
        int wordCount = mnemonic.split(" ").length;
        return wordCount == 12 || wordCount == 15 || wordCount == 18 || wordCount == 21 || wordCount == 24;
    }

    private static String randomPassword(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }



}
