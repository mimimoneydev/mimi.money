package com.money.mimi.fragments.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.money.mimi.R;
import com.money.mimi.helpers.PreferenceManager;

public class WalletReceiveFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_receive, container, false);

        TextView tvAddress = view.findViewById(R.id.tv_wallet_address);
        Button btnCopy = view.findViewById(R.id.btn_copy_address);
        Button btnShare = view.findViewById(R.id.btn_share_address);
        ImageView ivQr = view.findViewById(R.id.iv_qr_code);

        Context context = requireContext();
        final String address = PreferenceManager.getWalletAddress(context.getApplicationContext()) == null
                ? ""
                : PreferenceManager.getWalletAddress(context.getApplicationContext());
        tvAddress.setText(address);

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
                Toast.makeText(context, R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("wallet_address", address));
            Toast.makeText(context, R.string.wallet_msg_copied, Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            if (address.isEmpty()) {
                Toast.makeText(context, R.string.wallet_no_wallet_generate_first, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, address);
            startActivity(Intent.createChooser(i, getString(R.string.wallet_share_address)));
        });

        return view;
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
            return null;
        }
    }
}
