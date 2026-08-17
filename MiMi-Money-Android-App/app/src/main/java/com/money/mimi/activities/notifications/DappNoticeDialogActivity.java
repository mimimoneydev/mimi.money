package com.money.mimi.activities.notifications;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.money.mimi.R;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.notifications.DappNoticeStore;

public class DappNoticeDialogActivity extends Activity {
    private static final int DIALOG_TEXT_PRIMARY = Color.rgb(33, 33, 33);
    private static final int DIALOG_TEXT_SECONDARY = Color.rgb(117, 117, 117);
    private static final int DIALOG_LINK_TEXT = Color.rgb(25, 118, 210);
    private static final int DIALOG_ACTION_BACKGROUND = Color.rgb(96, 125, 139);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showNoticeDialog();
    }

    private void showNoticeDialog() {
        String title = getIntentString("title", getString(R.string.app_name));
        String message = getIntentString("msg", "");
        String image = DappNoticeStore.normalizeImageUrl(getIntentString("image", ""));
        String link = getIntentString("link", "");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, padding, padding, padding);
        content.setBackground(createDialogBackground());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            content.setElevation(dp(8));
        }

        if (isHttpUrl(image)) {
            ImageView imageView = new ImageView(this);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(220)
            );
            imageParams.setMargins(0, 0, 0, dp(16));
            imageView.setLayoutParams(imageParams);
            content.addView(imageView);
            AppHelper.LogCat("Dapp dialog image: " + image);
            Glide.with(this)
                    .load(image)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
        } else {
            AppHelper.LogCat("Dapp dialog image missing or invalid: " + getIntentString("image", ""));
        }

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(DIALOG_TEXT_PRIMARY);
        titleView.setTextSize(22);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.START);
        content.addView(titleView);

        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextColor(DIALOG_TEXT_SECONDARY);
        messageView.setLinkTextColor(DIALOG_LINK_TEXT);
        messageView.setTextSize(16);
        messageView.setAutoLinkMask(Linkify.ALL);
        messageView.setLinksClickable(true);
        messageView.setPadding(0, dp(8), 0, 0);
        content.addView(messageView);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.setMargins(0, dp(16), 0, 0);
        actions.setLayoutParams(actionsParams);

        Button readMore = createDialogButton("READ MORE");
        readMore.setVisibility(isHttpUrl(link) ? View.VISIBLE : View.GONE);
        readMore.setOnClickListener(v -> openLink(link));
        actions.addView(readMore);

        Button close = createDialogButton("CLOSE");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
        );
        closeParams.setMargins(isHttpUrl(link) ? dp(12) : 0, 0, 0, 0);
        close.setLayoutParams(closeParams);
        close.setOnClickListener(v -> finish());
        actions.addView(close);
        content.addView(actions);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        dialog.setOnCancelListener(d -> finish());
        dialog.setOnDismissListener(d -> {
            if (!isFinishing()) {
                finish();
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private GradientDrawable createDialogBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(10));
        return background;
    }

    private Button createDialogButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(DIALOG_ACTION_BACKGROUND);
        background.setCornerRadius(0);
        button.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
        );
        button.setLayoutParams(params);
        return button;
    }

    private void openLink(String link) {
        Intent intent = new Intent(this, com.money.mimi.activities.main.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_dapp_url", link);
        startActivity(intent);
        finish();
    }

    private String getIntentString(String key, String fallback) {
        String value = getIntent() == null ? null : getIntent().getStringExtra(key);
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private boolean isHttpUrl(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && !TextUtils.isEmpty(uri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
