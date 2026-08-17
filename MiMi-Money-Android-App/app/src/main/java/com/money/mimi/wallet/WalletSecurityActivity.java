package com.money.mimi.wallet;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;

public class WalletSecurityActivity extends AppCompatActivity {

    private SwitchCompat switchBiometric;
    private SwitchCompat switchRequireTx;
    private SwitchCompat switchDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_security);
        setTitle(R.string.wallet_security_title);

        switchBiometric = findViewById(R.id.switch_biometric);
        switchRequireTx = findViewById(R.id.switch_require_tx);
        switchDarkTheme = findViewById(R.id.switch_dark_theme);
        View rowChangePin = findViewById(R.id.row_change_pin);

        // Init states
        boolean bio = PreferenceManager.isSecurityBiometricEnabled(this);
        boolean reqTx = PreferenceManager.isRequireAuthForTransactions(this);
        boolean dark = PreferenceManager.isDarkThemeEnabled(this);
        switchBiometric.setChecked(bio);
        switchRequireTx.setChecked(reqTx);
        switchDarkTheme.setChecked(dark);

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) ->
                PreferenceManager.setSecurityBiometricEnabled(this, isChecked));
        switchRequireTx.setOnCheckedChangeListener((buttonView, isChecked) ->
                PreferenceManager.setRequireAuthForTransactions(this, isChecked));
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceManager.setDarkThemeEnabled(this, isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            recreate();
        });

        rowChangePin.setOnClickListener(v -> changePinFlow());
    }

    private void changePinFlow() {
        final String existingPin = PreferenceManager.getWalletPin(this);
        if (existingPin == null || existingPin.isEmpty()) {
            promptSetPin(() -> Toast.makeText(this, R.string.wallet_set_pin, Toast.LENGTH_SHORT).show());
        } else {
            // Verify current PIN first
            final android.widget.EditText et = new android.widget.EditText(this);
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            et.setHint(getString(R.string.wallet_enter_pin));
            new AlertDialog.Builder(this)
                    .setTitle(R.string.wallet_enter_pin)
                    .setView(et)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        if (existingPin.equals(et.getText().toString())) {
                            promptSetPin(() -> Toast.makeText(this, R.string.wallet_security_change_pin, Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, R.string.wallet_pin_wrong, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void promptSetPin(Runnable onSuccess) {
        final android.widget.EditText et1 = new android.widget.EditText(this);
        et1.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et1.setHint(getString(R.string.wallet_enter_pin));
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallet_set_pin)
                .setView(et1)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String p1 = et1.getText().toString();
                    final android.widget.EditText et2 = new android.widget.EditText(this);
                    et2.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                    et2.setHint(getString(R.string.wallet_confirm_pin));
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.wallet_confirm_pin)
                            .setView(et2)
                            .setPositiveButton(android.R.string.ok, (d2, w2) -> {
                                String p2 = et2.getText().toString();
                                if (!p1.equals(p2)) {
                                    Toast.makeText(this, R.string.wallet_pin_mismatch, Toast.LENGTH_SHORT).show();
                                } else {
                                    PreferenceManager.setWalletPin(this, p1);
                                    if (onSuccess != null) onSuccess.run();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

