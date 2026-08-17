package com.money.mimi.helpers;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.money.mimi.R;
import com.money.mimi.activities.main.MainActivity;

/**
 * Keeps activity content outside status, navigation and display-cutout areas.
 *
 * Android 15+ enforces edge-to-edge for apps targeting recent SDKs. Most of
 * MiMi's screens predate that behavior, so applying their original padding
 * plus the current safe-area insets prevents toolbars and bottom actions from
 * being obscured without changing the layouts themselves.
 */
public final class SystemBarInsets {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private SystemBarInsets() {
    }

    public static void install(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // ActivityLifecycleCallbacks.onActivityCreated is dispatched from
                // Activity.super.onCreate(), before the subclass has finished
                // requesting window features or installing its content view.
                // Defer all decor access until that synchronous onCreate returns.
                MAIN_HANDLER.post(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        apply(activity);
                    }
                });
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(
                    @NonNull Activity activity,
                    @NonNull Bundle outState
            ) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    private static void apply(@NonNull Activity activity) {
        // MainActivity already has tailored inset handling for its status scrim,
        // bottom tabs and floating action button.
        if (activity instanceof MainActivity || isFloatingWindow(activity)) {
            return;
        }

        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }

        Window window = activity.getWindow();
        ViewGroup decor = (ViewGroup) window.getDecorView();
        View statusBarProtection = createProtectionView(decor, Gravity.TOP);
        View navigationBarProtection = createProtectionView(decor, Gravity.BOTTOM);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(window, decor);

        final int initialLeft = content.getPaddingLeft();
        final int initialTop = content.getPaddingTop();
        final int initialRight = content.getPaddingRight();
        final int initialBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            boolean fullscreen = (window.getAttributes().flags
                    & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
            int safeTop = fullscreen ? 0 : systemBars.top;
            int statusBarColor = opaqueOrFallback(
                    window.getStatusBarColor(),
                    ContextCompat.getColor(activity, R.color.colorPrimaryDark)
            );
            int navigationBarColor = opaqueOrFallback(
                    window.getNavigationBarColor(),
                    Color.BLACK
            );
            updateProtection(statusBarProtection, safeTop, statusBarColor);
            updateProtection(navigationBarProtection, systemBars.bottom, navigationBarColor);
            insetsController.setAppearanceLightStatusBars(isLight(statusBarColor));
            insetsController.setAppearanceLightNavigationBars(isLight(navigationBarColor));
            view.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + safeTop,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    @NonNull
    private static View createProtectionView(@NonNull ViewGroup decor, int gravity) {
        View protection = new View(decor.getContext());
        protection.setClickable(false);
        protection.setFocusable(false);
        protection.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                gravity
        );
        decor.addView(protection, params);
        return protection;
    }

    private static void updateProtection(@NonNull View protection, int height, int color) {
        ViewGroup.LayoutParams params = protection.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            protection.setLayoutParams(params);
        }
        if (!(protection.getBackground() instanceof ColorDrawable)
                || ((ColorDrawable) protection.getBackground()).getColor() != color) {
            protection.setBackgroundColor(color);
        }
        protection.bringToFront();
    }

    private static int opaqueOrFallback(int color, int fallback) {
        return Color.alpha(color) == 0 ? fallback : color;
    }

    private static boolean isLight(int color) {
        return ColorUtils.calculateLuminance(color) > 0.5d;
    }

    private static boolean isFloatingWindow(@NonNull Activity activity) {
        android.content.res.TypedArray attributes = activity.obtainStyledAttributes(
                new int[]{android.R.attr.windowIsFloating}
        );
        try {
            return attributes.getBoolean(0, false);
        } finally {
            attributes.recycle();
        }
    }
}
