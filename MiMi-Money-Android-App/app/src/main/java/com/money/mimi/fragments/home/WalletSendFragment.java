package com.money.mimi.fragments.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.telemetry.AppTelemetry;
import com.money.mimi.wallet.PortraitCaptureActivity;
import com.money.mimi.wallet.WalletConfig;
import com.money.mimi.wallet.Web3Provider;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WalletSendFragment extends Fragment {

    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("0x[a-fA-F0-9]{40}");

    private EditText etTo;
    private EditText etAmount;
    private EditText etGasPriceGwei;
    private EditText etGasLimit;
    private TextView tvFee;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_send, container, false);

        etTo = view.findViewById(R.id.et_to);
        etAmount = view.findViewById(R.id.et_amount);
        etGasPriceGwei = view.findViewById(R.id.et_gas_price_gwei);
        etGasLimit = view.findViewById(R.id.et_gas_limit);
        tvFee = view.findViewById(R.id.tv_fee_estimate);
        progress = view.findViewById(R.id.progress);
        Button btnSend = view.findViewById(R.id.btn_send_now);
        Button btnEstimate = view.findViewById(R.id.btn_estimate_fee);

        etTo.setInputType(InputType.TYPE_CLASS_TEXT);
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        etGasLimit.setText("21000");

        btnEstimate.setOnClickListener(v -> fetchGasPriceAndEstimate());
        btnSend.setOnClickListener(v -> maybeAuthThenSend());

        View btnScan = view.findViewById(R.id.btn_scan_qr);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> startLiveQrScan());
        }

        return view;
    }

    private void fetchGasPriceAndEstimate() {
        new Thread(() -> {
            try {
                BigInteger gp = Web3Provider.get(requireContext()).ethGasPrice().send().getGasPrice();
                runOnUiThreadIfAdded(() -> {
                    BigDecimal gwei = new BigDecimal(gp).divide(new BigDecimal("1000000000"));
                    etGasPriceGwei.setText(gwei.stripTrailingZeros().toPlainString());
                    updateFeeFromFields();
                });
            } catch (Exception e) {
                runOnUiThreadIfAdded(this::updateFeeFromFields);
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
            tvFee.setText(getString(R.string.wallet_estimated_fee_value, feeEther.stripTrailingZeros().toPlainString(), WalletConfig.getCurrencySymbol(requireContext())));
        } catch (Exception e) {
            tvFee.setText(getString(R.string.wallet_estimated_fee_placeholder));
        }
    }

    private void maybeAuthThenSend() {
        if (!PreferenceManager.isRequireAuthForTransactions(requireContext())) {
            doSend();
            return;
        }
        final String existingPin = PreferenceManager.getWalletPin(requireContext());
        if (existingPin == null || existingPin.isEmpty()) {
            promptSetPinThenSend();
        } else {
            promptVerifyPinThenSend(existingPin);
        }
    }

    private void promptVerifyPinThenSend(final String existingPin) {
        final EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setHint(getString(R.string.wallet_enter_pin));
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.wallet_enter_pin)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (existingPin.equals(et.getText().toString())) {
                        doSend();
                    } else {
                        Toast.makeText(requireContext(), R.string.wallet_pin_wrong, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void promptSetPinThenSend() {
        final EditText et1 = new EditText(requireContext());
        et1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et1.setHint(getString(R.string.wallet_enter_pin));
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.wallet_set_pin)
                .setView(et1)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String p1 = et1.getText().toString();
                    final EditText et2 = new EditText(requireContext());
                    et2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                    et2.setHint(getString(R.string.wallet_confirm_pin));
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.wallet_confirm_pin)
                            .setView(et2)
                            .setPositiveButton(android.R.string.ok, (d2, w2) -> {
                                String p2 = et2.getText().toString();
                                if (!p1.equals(p2)) {
                                    Toast.makeText(requireContext(), R.string.wallet_pin_mismatch, Toast.LENGTH_SHORT).show();
                                } else {
                                    PreferenceManager.setWalletPin(requireContext(), p1);
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
            Toast.makeText(requireContext(), R.string.wallet_send_fill_all, Toast.LENGTH_SHORT).show();
            return;
        }
        final String mnemonic = PreferenceManager.getWalletMnemonic(requireContext());
        final String password = PreferenceManager.getWalletPassword(requireContext());
        if (mnemonic == null || password == null) {
            Toast.makeText(requireContext(), R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
            return;
        }
        AppTelemetry.logFeatureOpened("wallet_send");
        final AppTelemetry.OperationTrace sendTrace = AppTelemetry.startTrace("wallet_send");
        setBusy(true);
        new Thread(() -> {
            try {
                Web3j web3 = Web3Provider.get(requireContext());
                Credentials cred = org.web3j.crypto.WalletUtils.loadBip39Credentials(password, mnemonic);
                String fromAddress = cred.getAddress();

                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                BigInteger gasPrice;
                String gasPriceGweiStr = etGasPriceGwei.getText().toString().trim();
                if (!gasPriceGweiStr.isEmpty()) {
                    BigDecimal gwei = new BigDecimal(gasPriceGweiStr);
                    gasPrice = gwei.multiply(new BigDecimal("1000000000")).toBigInteger();
                } else {
                    EthGasPrice gasPriceResp = web3.ethGasPrice().send();
                    gasPrice = gasPriceResp.getGasPrice();
                }

                String gasLimitStr = etGasLimit.getText().toString().trim();
                BigInteger gasLimit = gasLimitStr.isEmpty() ? BigInteger.valueOf(21000) : new BigInteger(gasLimitStr);
                BigInteger valueWei = Convert.toWei(new BigDecimal(amountStr), Convert.Unit.ETHER).toBigInteger();

                RawTransaction rawTx = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, valueWei);
                byte[] signedMessage = signEip155(rawTx, WalletConfig.getChainId(requireContext()), cred);
                String hexValue = Numeric.toHexString(signedMessage);
                EthSendTransaction resp = web3.ethSendRawTransaction(hexValue).send();

                sendTrace.stop();
                AppTelemetry.logOperationResult("wallet_send", !resp.hasError());
                if (resp.hasError()) {
                    AppTelemetry.recordNonFatal("wallet_send_rpc_rejected",
                            new IllegalStateException("rpc_rejected"));
                }

                runOnUiThreadIfAdded(() -> {
                    setBusy(false);
                    if (resp.hasError()) {
                        Toast.makeText(requireContext(), getString(R.string.wallet_error_sending) + ": " + resp.getError().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        String txHash = resp.getTransactionHash();
                        Toast.makeText(requireContext(), getString(R.string.wallet_tx_sent) + ": " + txHash, Toast.LENGTH_LONG).show();
                        getParentFragmentManager().popBackStack();
                    }
                });
            } catch (Exception e) {
                sendTrace.stop();
                AppTelemetry.logOperationResult("wallet_send", false);
                AppTelemetry.recordNonFatal("wallet_send", e);
                runOnUiThreadIfAdded(() -> {
                    setBusy(false);
                    Toast.makeText(requireContext(), getString(R.string.wallet_error_sending), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void startLiveQrScan() {
        com.google.zxing.integration.android.IntentIntegrator integrator =
                com.google.zxing.integration.android.IntentIntegrator.forSupportFragment(this);
        integrator.setBeepEnabled(false);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        com.google.zxing.integration.android.IntentResult scanResult =
                com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                String address = extractWalletAddress(scanResult.getContents());
                if (address != null) {
                    etTo.setText(address);
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "No wallet address found in QR", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String extractWalletAddress(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
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
        Matcher m2 = ETH_ADDRESS_PATTERN.matcher(text);
        if (m2.find()) return m2.group(0);
        return null;
    }

    private void runOnUiThreadIfAdded(Runnable runnable) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (isAdded()) runnable.run();
        });
    }

    private static byte[] signEip155(RawTransaction rawTx, long chainId, Credentials credentials) {
        byte[] encoded = encodeForSign(rawTx, chainId);
        Sign.SignatureData sig = Sign.signMessage(encoded, credentials.getEcKeyPair());
        int recId = (sig.getV()[0] & 0xFF) - 27;
        long v = (chainId * 2L) + 35L + recId;
        return encodeWithSignature(rawTx, BigInteger.valueOf(v), sig);
    }

    private static byte[] encodeForSign(RawTransaction rawTx, long chainId) {
        List<RlpType> values = asRlpValues(rawTx, BigInteger.valueOf(chainId), true);
        return RlpEncoder.encode(new RlpList(values));
    }

    private static byte[] encodeWithSignature(RawTransaction rawTx, BigInteger v, Sign.SignatureData sig) {
        List<RlpType> values = asRlpValues(rawTx, v, false);
        values.remove(values.size() - 1);
        values.add(RlpString.create(v));
        values.add(RlpString.create(new BigInteger(1, sig.getR())));
        values.add(RlpString.create(new BigInteger(1, sig.getS())));
        return RlpEncoder.encode(new RlpList(values));
    }

    private static List<RlpType> asRlpValues(RawTransaction rawTx, BigInteger chainIdOrV, boolean forSignature) {
        List<RlpType> result = new ArrayList<>();
        result.add(RlpString.create(rawTx.getNonce()));
        result.add(RlpString.create(rawTx.getGasPrice()));
        result.add(RlpString.create(rawTx.getGasLimit()));
        String to = rawTx.getTo();
        result.add(to == null || to.length() == 0
                ? RlpString.create("")
                : RlpString.create(Numeric.hexStringToByteArray(to)));
        result.add(RlpString.create(rawTx.getValue()));
        String data = rawTx.getData();
        if (data == null) data = "";
        result.add(RlpString.create(Numeric.hexStringToByteArray(data)));

        result.add(RlpString.create(chainIdOrV));
        result.add(RlpString.create(BigInteger.ZERO));
        result.add(RlpString.create(BigInteger.ZERO));
        return result;
    }
}
