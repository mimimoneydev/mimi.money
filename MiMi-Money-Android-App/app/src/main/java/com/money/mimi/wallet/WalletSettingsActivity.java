package com.money.mimi.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;

import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.math.BigInteger;

public class WalletSettingsActivity extends AppCompatActivity {

    private static final int REQ_CONFIRM_DEVICE_CREDENTIAL = 1001;
    private static final String WALLET_RECOVERY_PASSPHRASE = "";
    private String pendingSecretToReveal;
    private int pendingSecretTitleRes;

    private TextView tvAddress;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_settings);
        setTitle(R.string.wallet_settings_title);

        tvAddress = findViewById(R.id.tv_wallet_address);
        progress = findViewById(R.id.progress);
        Button btnCopy = findViewById(R.id.btn_copy_address);
        Button btnShowSeed = findViewById(R.id.btn_show_seed);
        Button btnShowPrivateKey = findViewById(R.id.btn_show_private_key);
        Button btnSecurity = findViewById(R.id.btn_security);

        updateUI();

        btnCopy.setOnClickListener(v -> copyAddress());
        btnShowSeed.setOnClickListener(v -> showSeed());
        btnShowPrivateKey.setOnClickListener(v -> showPrivateKey());
        if (btnSecurity != null) btnSecurity.setOnClickListener(v -> openSecurityWithPin());
    }

    private void updateUI() {
        String address = PreferenceManager.getWalletAddress(this);
        if (address == null) address = "";
        tvAddress.setText(address);
    }

    private void copyAddress() {
        String address = PreferenceManager.getWalletAddress(this);
        if (address == null || address.isEmpty()) return;
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("wallet_address", address));
        Toast.makeText(this, R.string.wallet_msg_copied, Toast.LENGTH_SHORT).show();
    }

    private void showSeed() {
        String seed = PreferenceManager.getWalletMnemonic(this);
        if (seed == null || seed.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.wallet_settings_seed_phrase)
                    .setMessage(getString(R.string.wallet_no_seed_generate_first))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        revealProtectedSecret(seed, R.string.wallet_settings_seed_phrase, R.string.auth_reveal_seed_phrase);
    }

    private void showPrivateKey() {
        String privateKey = getWalletPrivateKey();
        if (privateKey == null || privateKey.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.wallet_settings_private_key)
                    .setMessage(getString(R.string.wallet_no_private_key_generate_first))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        revealProtectedSecret(privateKey, R.string.wallet_settings_private_key, R.string.auth_reveal_private_key);
    }

    private void revealProtectedSecret(String secret, int titleResId, int authPromptResId) {
        android.app.KeyguardManager kg = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (kg != null && kg.isKeyguardSecure()) {
            pendingSecretToReveal = secret;
            pendingSecretTitleRes = titleResId;
            android.content.Intent intent = kg.createConfirmDeviceCredentialIntent(getString(titleResId), getString(authPromptResId));
            if (intent != null) {
                startActivityForResult(intent, REQ_CONFIRM_DEVICE_CREDENTIAL);
                return;
            }
        }
        // Fallback to app-level PIN with user choice
        String existingPin = PreferenceManager.getWalletPin(this);
        if (existingPin == null || existingPin.isEmpty()) {
            promptPinChoiceThenProceed(secret, titleResId);
        } else {
            promptVerifyPinThenShow(existingPin, secret, titleResId);
        }
    }

    private void promptPinChoiceThenProceed(final String secret, final int titleResId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallet_seed_pin_prompt_title)
                .setMessage(getString(R.string.wallet_seed_pin_prompt_message))
                .setPositiveButton(R.string.wallet_seed_pin_prompt_yes, (d, w) -> {
                    promptSetPinThenShow(secret, titleResId);
                })
                .setNegativeButton(R.string.wallet_seed_pin_prompt_no, (d, w) -> {
                    revealSecret(secret, titleResId);
                })
                .show();
    }

    private void promptSetPinThenShow(final String secret, final int titleResId) {
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
                                    revealSecret(secret, titleResId);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void promptVerifyPinThenShow(final String existingPin, final String secret, final int titleResId) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setHint(getString(R.string.wallet_enter_pin));
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallet_enter_pin)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (existingPin.equals(et.getText().toString())) {
                        revealSecret(secret, titleResId);
                    } else {
                        Toast.makeText(this, R.string.wallet_pin_wrong, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void revealSecret(String secret, int titleResId) {
        new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(secret)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String getWalletPrivateKey() {
        String mnemonic = PreferenceManager.getWalletMnemonic(this);
        if (mnemonic == null || mnemonic.isEmpty()) {
            return null;
        }
        String password = PreferenceManager.getWalletPassword(this);
        if (password == null) {
            password = WALLET_RECOVERY_PASSPHRASE;
        }
        try {
            Credentials credentials = WalletUtils.loadBip39Credentials(password, mnemonic);
            BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
            return "0x" + String.format("%064x", privateKey);
        } catch (Exception e) {
            return null;
        }
    }

    private void openSecurityWithPin() {
        String existingPin = PreferenceManager.getWalletPin(this);
        if (existingPin == null || existingPin.isEmpty()) {
            promptSetPinThenOpenSecurity();
        } else {
            promptVerifyPinThenOpenSecurity(existingPin);
        }
    }

    private void promptSetPinThenOpenSecurity() {
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
                                    openSecurity();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void promptVerifyPinThenOpenSecurity(final String existingPin) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setHint(getString(R.string.wallet_enter_pin));
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallet_enter_pin)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (existingPin.equals(et.getText().toString())) {
                        openSecurity();
                    } else {
                        Toast.makeText(this, R.string.wallet_pin_wrong, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openSecurity() {
        android.content.Intent i = new android.content.Intent(this, com.money.mimi.wallet.WalletSecurityActivity.class);
        startActivity(i);
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void generateWallet() {
        setBusy(true);
        new Thread(() -> {
            try {
                File destDir = new File(getFilesDir(), "wallets");
                if (!destDir.exists()) destDir.mkdirs();

                Bip39Wallet wallet = WalletUtils.generateBip39Wallet(WALLET_RECOVERY_PASSPHRASE, destDir);
                String mnemonic = wallet.getMnemonic();
                Credentials credentials = WalletUtils.loadBip39Credentials(WALLET_RECOVERY_PASSPHRASE, mnemonic);
                final String address = credentials.getAddress();

                PreferenceManager.setWalletPassword(this, WALLET_RECOVERY_PASSPHRASE);
                PreferenceManager.setWalletMnemonic(this, mnemonic);
                PreferenceManager.setWalletAddress(this, address);
                // Backward compatibility with previous storage
                PreferenceManager.setMobileNumber(this, address);
                PreferenceManager.setWalletAddress(this, address);

                runOnUiThread(() -> {
                    setBusy(false);
                    updateUI();
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.wallet_settings_seed_phrase)
                            .setMessage(mnemonic)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {

                    setBusy(false);
                    Toast.makeText(this, getString(R.string.wallet_error_generating_wallet), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONFIRM_DEVICE_CREDENTIAL) {
            if (resultCode == RESULT_OK && pendingSecretToReveal != null) {
                revealSecret(pendingSecretToReveal, pendingSecretTitleRes);
                pendingSecretToReveal = null;
                pendingSecretTitleRes = 0;
            } else {
                Toast.makeText(this, R.string.wallet_pin_wrong, Toast.LENGTH_SHORT).show();
                pendingSecretToReveal = null;
                pendingSecretTitleRes = 0;
            }
        }
    }

}
