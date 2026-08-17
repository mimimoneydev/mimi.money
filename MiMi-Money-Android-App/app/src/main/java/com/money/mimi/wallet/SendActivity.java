package com.money.mimi.wallet;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.telemetry.AppTelemetry;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.InputStream;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {

    private EditText etTo;
    private EditText etAmount;
    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("0x[a-fA-F0-9]{40}");

    private EditText etGasPriceGwei;
    private EditText etGasLimit;
    private TextView tvFee;
    private ProgressBar progress;

    private static final int REQ_QR_CAPTURE = 1001;
    private static final int REQ_CAMERA_PERMISSION = 1002;
    private Uri qrImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        setTitle(getString(R.string.wallet_action_send) + " " + WalletConfig.getCurrencySymbol(this));

        etTo = findViewById(R.id.et_to);
        etAmount = findViewById(R.id.et_amount);
        etGasPriceGwei = findViewById(R.id.et_gas_price_gwei);
        etGasLimit = findViewById(R.id.et_gas_limit);
        tvFee = findViewById(R.id.tv_fee_estimate);
        progress = findViewById(R.id.progress);
        Button btnSend = findViewById(R.id.btn_send_now);
        Button btnEstimate = findViewById(R.id.btn_estimate_fee);

        etTo.setInputType(InputType.TYPE_CLASS_TEXT);
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});

        etGasLimit.setText("21000");

        btnEstimate.setOnClickListener(v -> fetchGasPriceAndEstimate());
        btnSend.setOnClickListener(v -> maybeAuthThenSend());

        View btnScan = findViewById(R.id.btn_scan_qr);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> startLiveQrScan());
        }

    }

    private void fetchGasPriceAndEstimate() {
        new Thread(() -> {
            try {
                BigInteger gp = Web3Provider.get(this).ethGasPrice().send().getGasPrice();
                runOnUiThread(() -> {
                    BigDecimal gwei = new BigDecimal(gp).divide(new BigDecimal("1000000000"));
                    etGasPriceGwei.setText(gwei.stripTrailingZeros().toPlainString());
                    updateFeeFromFields();
                });
            } catch (Exception e) {
                runOnUiThread(this::updateFeeFromFields);
            }
        }).start();
    }

    private void updateFeeFromFields() {
        String gp = etGasPriceGwei.getText().toString().trim();
        String gl = etGasLimit.getText().toString().trim();
        if (gp.isEmpty() || gl.isEmpty()) {
            tvFee.setText(getString(R.string.wallet_estimated_fee_placeholder));
            return;
        }
        try {
            BigDecimal weiPerGas = new BigDecimal(gp).multiply(new BigDecimal("1000000000"));
            BigDecimal gas = new BigDecimal(gl);
            BigDecimal feeWei = weiPerGas.multiply(gas);
            BigDecimal feeEther = Convert.fromWei(feeWei, Convert.Unit.ETHER);
            tvFee.setText(getString(R.string.wallet_estimated_fee_value, feeEther.stripTrailingZeros().toPlainString(), WalletConfig.getCurrencySymbol(this)));
        } catch (Exception e) {
            tvFee.setText(getString(R.string.wallet_estimated_fee_placeholder));
        }
    }

    private void maybeAuthThenSend() {
        if (!PreferenceManager.isRequireAuthForTransactions(this)) {
            doSend();
            return;
        }
        final String existingPin = PreferenceManager.getWalletPin(this);
        if (existingPin == null || existingPin.isEmpty()) {
            promptSetPinThenSend();
        } else {
            promptVerifyPinThenSend(existingPin);
        }
    }

    private void promptVerifyPinThenSend(final String existingPin) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setHint(getString(R.string.wallet_enter_pin));
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.wallet_enter_pin)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (existingPin.equals(et.getText().toString())) {
                        doSend();
                    } else {
                        Toast.makeText(this, R.string.wallet_pin_wrong, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void promptSetPinThenSend() {
        final android.widget.EditText et1 = new android.widget.EditText(this);
        et1.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et1.setHint(getString(R.string.wallet_enter_pin));
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.wallet_set_pin)
                .setView(et1)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String p1 = et1.getText().toString();
                    final android.widget.EditText et2 = new android.widget.EditText(this);
                    et2.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                    et2.setHint(getString(R.string.wallet_confirm_pin));
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(R.string.wallet_confirm_pin)
                            .setView(et2)
                            .setPositiveButton(android.R.string.ok, (d2, w2) -> {
                                String p2 = et2.getText().toString();
                                if (!p1.equals(p2)) {
                                    Toast.makeText(this, R.string.wallet_pin_mismatch, Toast.LENGTH_SHORT).show();
                                } else {
                                    PreferenceManager.setWalletPin(this, p1);
                                    doSend();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void doSend() {
        final String to = etTo.getText().toString().trim();
        final String amountStr = etAmount.getText().toString().trim();
        if (to.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, R.string.wallet_send_fill_all, Toast.LENGTH_SHORT).show();
            return;
        }
        final String mnemonic = PreferenceManager.getWalletMnemonic(this);
        final String password = PreferenceManager.getWalletPassword(this);
        if (mnemonic == null || password == null) {
            Toast.makeText(this, R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
            return;
        }
        AppTelemetry.logFeatureOpened("wallet_send");
        final AppTelemetry.OperationTrace sendTrace = AppTelemetry.startTrace("wallet_send");
        setBusy(true);
        new Thread(() -> {
            try {
                Web3j web3 = Web3Provider.get(this);
                Credentials cred = org.web3j.crypto.WalletUtils.loadBip39Credentials(password, mnemonic);
                String fromAddress = cred.getAddress();

                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                // Gas price: use user input if provided, else fetch from RPC
                BigInteger gasPrice;
                String gasPriceGweiStr = etGasPriceGwei.getText().toString().trim();
                if (!gasPriceGweiStr.isEmpty()) {
                    BigDecimal gwei = new BigDecimal(gasPriceGweiStr);
                    gasPrice = gwei.multiply(new BigDecimal("1000000000")).toBigInteger();
                } else {
                    EthGasPrice gasPriceResp = web3.ethGasPrice().send();
                    gasPrice = gasPriceResp.getGasPrice();
                }

                // Gas limit: default 21000 if not provided
                BigInteger gasLimit;
                String gasLimitStr = etGasLimit.getText().toString().trim();
                gasLimit = gasLimitStr.isEmpty() ? BigInteger.valueOf(21000) : new BigInteger(gasLimitStr);

                BigInteger valueWei = Convert.toWei(new BigDecimal(amountStr), Convert.Unit.ETHER).toBigInteger();

                // Create legacy transaction and sign with EIP-155 for the active EVM network.
                RawTransaction rawTx = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, valueWei);
                byte[] signedMessage = signEip155(rawTx, WalletConfig.getChainId(this), cred);
                String hexValue = Numeric.toHexString(signedMessage);
                EthSendTransaction resp = web3.ethSendRawTransaction(hexValue).send();

                runOnUiThread(() -> {
                    sendTrace.stop();
                    setBusy(false);
                    if (resp.hasError()) {
                        AppTelemetry.logOperationResult("wallet_send", false);
                        AppTelemetry.recordNonFatal("wallet_send_rpc_rejected",
                                new IllegalStateException("rpc_rejected"));
                        Toast.makeText(this, getString(R.string.wallet_error_sending) + ": " + resp.getError().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        AppTelemetry.logOperationResult("wallet_send", true);
                        String txHash = resp.getTransactionHash();
                        Toast.makeText(this, getString(R.string.wallet_tx_sent) + ": " + txHash, Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    sendTrace.stop();
                    AppTelemetry.logOperationResult("wallet_send", false);
                    AppTelemetry.recordNonFatal("wallet_send", e);
                    setBusy(false);
                    Toast.makeText(this, getString(R.string.wallet_error_sending), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static byte[] signEip155(RawTransaction rawTx, long chainId, Credentials credentials) {
        byte[] encoded = encodeForSign(rawTx, chainId);
        Sign.SignatureData sig = Sign.signMessage(encoded, credentials.getEcKeyPair());
        int recId = (sig.getV()[0] & 0xFF) - 27; // 0 or 1
        long v = (chainId * 2L) + 35L + recId;
        return encodeWithSignature(rawTx, BigInteger.valueOf(v), sig);
    }

    private static byte[] encodeForSign(RawTransaction rawTx, long chainId) {
        List<RlpType> values = asRlpValues(rawTx, BigInteger.valueOf(chainId), true);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    private static byte[] encodeWithSignature(RawTransaction rawTx, BigInteger v, Sign.SignatureData sig) {
        List<RlpType> values = asRlpValues(rawTx, v, false);
        // replace last three with v,r,s
        values.remove(values.size() - 1);
        values.add(RlpString.create(v));
        values.add(RlpString.create(new BigInteger(1, sig.getR())));
        values.add(RlpString.create(new BigInteger(1, sig.getS())));
        return RlpEncoder.encode(new RlpList(values));
    }

    private static List<RlpType> asRlpValues(RawTransaction rawTx, BigInteger chainIdOrV, boolean forSignature) {
        List<RlpType> result = new ArrayList<>();
        // nonce, gasPrice, gasLimit
        result.add(RlpString.create(rawTx.getNonce()));
        result.add(RlpString.create(rawTx.getGasPrice()));
        result.add(RlpString.create(rawTx.getGasLimit()));
        // to
        String to = rawTx.getTo();
        if (to == null || to.length() == 0) {
            result.add(RlpString.create(""));
        } else {
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        }
        // value, data
        result.add(RlpString.create(rawTx.getValue()));
        String data = rawTx.getData();
        if (data == null) data = "";
        result.add(RlpString.create(Numeric.hexStringToByteArray(data)));

        if (forSignature) {
            // EIP-155: include chainId, 0, 0
            result.add(RlpString.create(chainIdOrV));
            result.add(RlpString.create(BigInteger.ZERO));
            result.add(RlpString.create(BigInteger.ZERO));
        } else {
            // placeholder; will be replaced by encodeWithSignature
            result.add(RlpString.create(chainIdOrV));
            result.add(RlpString.create(BigInteger.ZERO));
            result.add(RlpString.create(BigInteger.ZERO));
        }
        return result;
    }


    private void tryStartQrScan() {
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        } else {
            startCameraCapture();
        }
    }

    private void startCameraCapture() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "Camera unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            File imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (imagesDir != null && !imagesDir.exists()) imagesDir.mkdirs();
            File photo = File.createTempFile("qr_capture_", ".jpg", imagesDir);
            qrImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, qrImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_QR_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open camera", Toast.LENGTH_SHORT).show();
        }
    }
    private void startLiveQrScan() {
        com.google.zxing.integration.android.IntentIntegrator integrator =
                new com.google.zxing.integration.android.IntentIntegrator(this);
        integrator.setBeepEnabled(false);
        // Use our CaptureActivity which starts in portrait, then allows rotation
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        // Allow rotation to follow device orientation after launch
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraCapture();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle live scanner result first
        com.google.zxing.integration.android.IntentResult scanResult =
                com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                String address = extractWalletAddress(scanResult.getContents());
                if (address != null) {
                    etTo.setText(address);
                } else {
                    Toast.makeText(this, "No wallet address found in QR", Toast.LENGTH_SHORT).show();
                }
            }
            return; // handled (either scanned or cancelled)
        }

        // Fallback: camera photo decode flow
        if (requestCode == REQ_QR_CAPTURE && resultCode == RESULT_OK) {
            String text = null;
            try {
                if (qrImageUri != null) {
                    text = decodeQrFromUri(qrImageUri);
                } else if (data != null && data.getExtras() != null) {
                    Object extra = data.getExtras().get("data");
                    if (extra instanceof Bitmap) {
                        text = decodeQrFromBitmap((Bitmap) extra);
                    }
                }
            } catch (Exception ignored) {}

            if (text != null && !text.isEmpty()) {
                String address = extractWalletAddress(text);
                if (address != null) {
                    etTo.setText(address);
                } else {
                    Toast.makeText(this, "No wallet address found in QR", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No QR code found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String decodeQrFromUri(Uri uri) {
        try {
            // Decode bounds first to avoid OOM on large images
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream is1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is1, null, bounds);
            if (is1 != null) try { is1.close(); } catch (Exception ignored) {}
            int maxDim = 1024;
            int inSampleSize = 1;
            int w = bounds.outWidth;
            int h = bounds.outHeight;
            while ((w / inSampleSize) > maxDim || (h / inSampleSize) > maxDim) {
                inSampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inSampleSize = Math.max(1, inSampleSize);
            InputStream is2 = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is2, null, opts);
            if (is2 != null) try { is2.close(); } catch (Exception ignored) {}
            if (bmp == null) return null;
            return decodeQrFromBitmap(bmp);
        } catch (Exception e) {
            return null;
        }
    }

    private String decodeQrFromBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            java.util.Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.EnumMap<>(com.google.zxing.DecodeHintType.class);
            hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS, java.util.Arrays.asList(com.google.zxing.BarcodeFormat.QR_CODE, com.google.zxing.BarcodeFormat.DATA_MATRIX, com.google.zxing.BarcodeFormat.AZTEC));
            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(hints);
            Result result = reader.decode(bBitmap);
            return result.getText();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractWalletAddress(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        // Handle EIP-681 like "ethereum:0xabc...?value=..." and variants (ethereum://)
        String lower = text.toLowerCase();
        if (lower.startsWith("ethereum:")) {
            String s = text.substring(text.indexOf(':') + 1);
            while (s.startsWith("/")) s = s.substring(1);
            int idx0x = s.toLowerCase().indexOf("0x");
            if (idx0x >= 0) s = s.substring(idx0x);
            int q = s.indexOf('?');
            if (q > 0) s = s.substring(0, q);
            int at = s.indexOf('@');
            if (at > 0) s = s.substring(0, at);
            Matcher m = ETH_ADDRESS_PATTERN.matcher(s);
            if (m.find()) return m.group(0);
        }
        // Or plain address anywhere in the payload
        Matcher m2 = ETH_ADDRESS_PATTERN.matcher(text);
        if (m2.find()) return m2.group(0);
        return null;
    }

}
