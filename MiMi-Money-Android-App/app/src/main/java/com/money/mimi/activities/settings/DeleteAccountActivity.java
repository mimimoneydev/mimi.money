package com.money.mimi.activities.settings;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;

import android.os.Bundle;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;



import com.money.mimi.R;
import com.money.mimi.activities.CountryActivity;
import com.money.mimi.activities.main.welcome.WelcomeActivity;

import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIService;
import com.money.mimi.api.apiServices.UsersService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.CountriesFetcher;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;

import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.models.CountriesModel;

import com.money.mimi.ui.CustomProgressView;



import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;



/**
 * Created by Abderrahim El imame on 09/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class DeleteAccountActivity extends AppCompatActivity implements View.OnClickListener {


    @BindView(R.id.numberPhone)
    AppCompatEditText phoneNumberWrapper;
    @BindView(R.id.btn_request_sms)
    AppCompatTextView btnNext;

    @BindView(R.id.progress_bar_load)
    CustomProgressView progressBarLoad;

    @BindView(R.id.country_code)
    AppCompatTextView countryCode;
    @BindView(R.id.short_description_phone)
    AppCompatTextView shortDescriptionPhone;
    @BindView(R.id.country_name)
    AppCompatTextView countryName;

    @BindView(R.id.numberPhone_layout_sv)
    NestedScrollView numberPhoneLayoutSv;

    @BindView(R.id.app_bar)
    Toolbar toolbar;


    private CountriesModel mSelectedCountry;
    private CountriesFetcher.CountryList mCountries;
    private Realm realm;
    private UsersService mUsersContactsDelete;
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + "closeDeleteAccountActivity")) {
                AppHelper.showDialog(DeleteAccountActivity.this, getString(R.string.deleting));
                new Handler().postDelayed(() -> {
                    AppHelper.hideDialog();
                    PreferenceManager.setToken(DeleteAccountActivity.this, null);
                    PreferenceManager.setID(DeleteAccountActivity.this, 0);
                    PreferenceManager.setSocketID(DeleteAccountActivity.this, null);
                    PreferenceManager.setWalletAddress(DeleteAccountActivity.this, null);
                    PreferenceManager.setIsWaitingForSms(DeleteAccountActivity.this, false);
                    PreferenceManager.setMobileNumber(DeleteAccountActivity.this, null);
                    PreferenceManager.setLastBackup(DeleteAccountActivity.this, null);
                    NotificationsManager.SetupBadger(DeleteAccountActivity.this);
                    RealmBackupRestore.deleteData(DeleteAccountActivity.this);
                    AppHelper.deleteCache(DeleteAccountActivity.this);
                    Intent mIntent1 = new Intent(DeleteAccountActivity.this, WelcomeActivity.class);
                    mIntent1.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mIntent1);
                    finishAffinity();
                }, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);
        ButterKnife.bind(this);
        realm = WhatsCloneApplication.getRealmDatabaseInstance();
        initializerView();
        APIService mApiServiceDelete = APIService.with(this);
        mUsersContactsDelete = new UsersService(realm, this, mApiServiceDelete);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getPackageName() + "closeDeleteAccountActivity");
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }


    /**
     * method to initialize the view
     */
    private void initializerView() {
        // Simplified: wallet-only flow. No Account Kit, no OTP, no SMS permissions.
        hideKeyboard();
        mCountries = CountriesFetcher.getCountries(this);

        int defaultIdx = mCountries.indexOfIso(AppConstants.DEFAULT_COUNTRY_CODE);
        if (defaultIdx < 0 && !mCountries.isEmpty()) {
            defaultIdx = 0;
        }
        if (defaultIdx >= 0) {
            mSelectedCountry = mCountries.get(defaultIdx);
        }
        countryCode.setText(mSelectedCountry.getDial_code());
        countryName.setText(mSelectedCountry.getName());
        shortDescriptionPhone.setText(getString(R.string.enter_wallet_address));

        btnNext.setOnClickListener(this);
        countryCode.setOnClickListener(this);
        countryName.setOnClickListener(this);
        setOnKeyboardDone();
        setupToolbar();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }




    /**
     * method to validate user information
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
            phoneNumberWrapper.setError(getString(R.string.enter_a_val_number));
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
     * method to send an SMS request to provider
     *
     * @param mobile this the first parameter of  requestForSMS method
     */
    private void requestForSMS(String mobile, String country) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_message_delete_account);
        builder.setPositiveButton(R.string.Yes, (dialog, whichButton) -> {
            // Wallet-only: show single progress and call delete + confirm directly when SMS is disabled.
            progressBarLoad.setVisibility(View.VISIBLE);
            progressBarLoad.setColor(AppHelper.getColor(this, R.color.colorWhite));
            btnNext.setText(getString(R.string.set_back_and_keep_calm_you_will_receive_an_sms_of_verification));
            btnNext.setEnabled(false);

            mUsersContactsDelete.deleteAccount(mobile, country).subscribe(response -> {
                if (response.isSuccess()) {
                    if (!response.isSmsVerification() && response.getCode() != null && !response.getCode().isEmpty()) {
                        // Directly confirm deletion without SMS/OTP
                        mUsersContactsDelete.deleteAccountConfirmation(response.getCode()).subscribe(statusResponse -> {
                            progressBarLoad.setVisibility(View.GONE);
                            btnNext.setText(getString(R.string.next));
                            btnNext.setEnabled(true);
                            if (statusResponse.isSuccess()) {
                                LocalBroadcastManager.getInstance(this)
                                        .sendBroadcast(new Intent(getPackageName() + "closeDeleteAccountActivity"));
                            } else {
                                AppHelper.CustomToast(DeleteAccountActivity.this, statusResponse.getMessage());
                            }
                        }, throwable -> {
                            progressBarLoad.setVisibility(View.GONE);
                            btnNext.setText(getString(R.string.next));
                            btnNext.setEnabled(true);
                            AppHelper.LogCat("delete account confirm error " + throwable.getMessage());
                            AppHelper.CustomToast(DeleteAccountActivity.this, getString(R.string.delete_account_failed_please_try_later));
                        });
                    } else {
                        // Backend still expects SMS flow (not supported anymore)
                        progressBarLoad.setVisibility(View.GONE);
                        btnNext.setText(getString(R.string.next));
                        btnNext.setEnabled(true);
                        AppHelper.CustomToast(DeleteAccountActivity.this, getString(R.string.delete_account_failed_please_try_later));
                    }
                } else {
                    progressBarLoad.setVisibility(View.GONE);
                    btnNext.setText(getString(R.string.next));
                    btnNext.setEnabled(true);
                    AppHelper.CustomToast(DeleteAccountActivity.this, response.getMessage());
                }
            }, throwable -> {
                progressBarLoad.setVisibility(View.GONE);
                btnNext.setText(getString(R.string.next));
                btnNext.setEnabled(true);
                hideKeyboard();
                AppHelper.LogCat("delete  account " + throwable.getMessage());
                AppHelper.CustomToast(DeleteAccountActivity.this, getString(R.string.delete_account_failed_please_try_later));
            });
        });
        builder.setNegativeButton(R.string.No, (dialog, whichButton) -> {});
        builder.show();
    }

    /**
     * this if you disabled verification by sms
     *
     * @param code
     */


    /**
     * method to verify the code received by user then activating the user
     */




    @Override
    public void onClick(View view) {
        Intent mIntent;
        switch (view.getId()) {
            case R.id.btn_request_sms:
                validateInformation();
                break;
            case R.id.country_code:
                mIntent = new Intent(this, CountryActivity.class);
                startActivityForResult(mIntent, AppConstants.SELECT_COUNTRY);
                break;
            case R.id.country_name:
                mIntent = new Intent(this, CountryActivity.class);
                startActivityForResult(mIntent, AppConstants.SELECT_COUNTRY);
                break;
        }
    }






    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        realm.close();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == AppConstants.SELECT_COUNTRY && data != null) {
            numberPhoneLayoutSv.pageScroll(View.FOCUS_DOWN);
            String codeIso = data.getStringExtra("countryIso");
            int defaultIdx = mCountries != null ? mCountries.indexOfIso(codeIso) : -1;
            if (defaultIdx >= 0) {
                mSelectedCountry = mCountries.get(defaultIdx);
                this.countryCode.setText(mSelectedCountry.getDial_code());
                this.countryName.setText(mSelectedCountry.getName());
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


    public void setOnKeyboardDone() {
        phoneNumberWrapper.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateInformation();
            }
            return false;
        });
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
