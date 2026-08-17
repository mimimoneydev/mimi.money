package com.money.mimi.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ReceiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        setTitle(getString(R.string.wallet_action_receive) + " " + WalletConfig.getCurrencySymbol(this));

        TextView tvAddress = findViewById(R.id.tv_wallet_address);
        Button btnCopy = findViewById(R.id.btn_copy_address);
        Button btnShare = findViewById(R.id.btn_share_address);
        ImageView ivQr = findViewById(R.id.iv_qr_code);

        final String address = PreferenceManager.getWalletAddress(this) == null ? "" : PreferenceManager.getWalletAddress(this);
        tvAddress.setText(address);

        // Generate and show QR code of the wallet address
        if (ivQr != null) {
            if (address.isEmpty()) {
                ivQr.setVisibility(View.GONE);
            } else {
                Bitmap qr = generateQrBitmap(address, 512);
                if (qr != null) {
                    ivQr.setImageBitmap(qr);
                    ivQr.setVisibility(View.VISIBLE);
                } else {
                    ivQr.setVisibility(View.GONE);
                }
            }
        }

        btnCopy.setOnClickListener(v -> {
            if (address.isEmpty()) {
                Toast.makeText(this, R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("wallet_address", address));
            Toast.makeText(this, R.string.wallet_msg_copied, Toast.LENGTH_SHORT).show();
        });
        btnShare.setOnClickListener(v -> {
            if (address.isEmpty()) {
                Toast.makeText(this, R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, address);
            startActivity(Intent.createChooser(i, getString(R.string.wallet_share_address)));
        });
    }

    private Bitmap generateQrBitmap(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
