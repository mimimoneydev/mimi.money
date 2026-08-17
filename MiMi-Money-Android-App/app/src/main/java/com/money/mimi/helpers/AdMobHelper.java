package com.money.mimi.helpers;

import android.content.Context;

import com.money.mimi.BuildConfig;
import com.money.mimi.R;

/** Centralized AdMob ID validation and safe test-ad selection. */
public final class AdMobHelper {

    private static final String SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    private static final String TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    private static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";

    private AdMobHelper() {
    }

    public static String bannerId(Context context, String configuredId) {
        return resolve(context, configuredId, TEST_BANNER_ID);
    }

    public static String interstitialId(Context context, String configuredId) {
        return resolve(context, configuredId, TEST_INTERSTITIAL_ID);
    }

    public static String rewardedId(Context context, String configuredId) {
        return resolve(context, configuredId, TEST_REWARDED_ID);
    }

    public static boolean isValidAdUnitId(String value) {
        return value != null && value.trim().matches("ca-app-pub-\\d+/\\d+");
    }

    private static String resolve(Context context, String configuredId, String testId) {
        if (BuildConfig.DEBUG || usesSampleAppId(context)) {
            return testId;
        }
        String normalized = configuredId == null ? null : configuredId.trim();
        return isValidAdUnitId(normalized) ? normalized : null;
    }

    private static boolean usesSampleAppId(Context context) {
        return context == null || SAMPLE_APP_ID.equals(context.getString(R.string.admob_app_id));
    }
}
