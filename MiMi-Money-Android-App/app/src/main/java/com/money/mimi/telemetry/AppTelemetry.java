package com.money.mimi.telemetry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.money.mimi.BuildConfig;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Privacy-safe access point for app telemetry.
 *
 * Never pass wallet addresses, transaction hashes, amounts, messages, URLs, recovery phrases,
 * private keys, PINs, or arbitrary server error text to this class.
 */
@SuppressLint("LogNotTimber") // This bootstrap layer must work before the app logger is configured.
public final class AppTelemetry {
    private static final String TAG = "MiMiTelemetry";
    private static final OperationTrace NO_OP_TRACE = () -> { };

    private static volatile FirebaseAnalytics analytics;
    private static volatile FirebaseCrashlytics crashlytics;
    private static volatile FirebasePerformance performance;

    private AppTelemetry() { }

    /** Initializes telemetry defensively so an unavailable SDK cannot break app startup. */
    public static void initialize(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        try {
            analytics = FirebaseAnalytics.getInstance(appContext);
        } catch (RuntimeException | LinkageError error) {
            logInitializationFailure("analytics", error);
        }
        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE);
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE);
        } catch (RuntimeException | LinkageError error) {
            logInitializationFailure("crashlytics", error);
        }
        try {
            performance = FirebasePerformance.getInstance();
        } catch (RuntimeException | LinkageError error) {
            logInitializationFailure("performance", error);
        }
    }

    public static void logFeatureOpened(@NonNull String feature) {
        FirebaseAnalytics instance = analytics;
        if (instance == null) return;
        Bundle parameters = new Bundle();
        parameters.putString("feature", TelemetrySanitizer.identifier(feature, "unknown"));
        try {
            instance.logEvent("feature_open", parameters);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to record analytics event", error);
        }
    }

    public static void logOperationResult(@NonNull String operation, boolean successful) {
        FirebaseAnalytics instance = analytics;
        if (instance == null) return;
        Bundle parameters = new Bundle();
        parameters.putString("operation", TelemetrySanitizer.identifier(operation, "unknown"));
        parameters.putString("result", successful ? "success" : "failure");
        try {
            instance.logEvent("operation_result", parameters);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to record analytics result", error);
        }
    }

    /** Records a sanitized non-fatal report while retaining only the original stack frames. */
    public static void recordNonFatal(@NonNull String operation, Throwable error) {
        FirebaseCrashlytics instance = crashlytics;
        if (instance == null) return;
        String safeOperation = TelemetrySanitizer.identifier(operation, "unknown");
        String errorType = error == null
                ? "unknown"
                : TelemetrySanitizer.identifier(error.getClass().getSimpleName(), "exception");
        SanitizedTelemetryException report =
                new SanitizedTelemetryException(safeOperation + ":" + errorType);
        if (error != null) report.setStackTrace(error.getStackTrace());
        try {
            instance.recordException(report);
        } catch (RuntimeException loggingError) {
            Log.w(TAG, "Unable to record non-fatal report", loggingError);
        }
    }

    public static OperationTrace startTrace(@NonNull String name) {
        FirebasePerformance instance = performance;
        if (instance == null) return NO_OP_TRACE;
        try {
            Trace trace = instance.newTrace(TelemetrySanitizer.identifier(name, "operation"));
            trace.start();
            return new FirebaseOperationTrace(trace);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to start performance trace", error);
            return NO_OP_TRACE;
        }
    }

    private static void logInitializationFailure(String component, Throwable error) {
        Log.w(TAG, "Firebase " + component + " unavailable; app will continue", error);
    }

    public interface OperationTrace {
        void stop();
    }

    private static final class FirebaseOperationTrace implements OperationTrace {
        private final Trace trace;
        private final AtomicBoolean stopped = new AtomicBoolean();

        private FirebaseOperationTrace(Trace trace) {
            this.trace = trace;
        }

        @Override
        public void stop() {
            if (!stopped.compareAndSet(false, true)) return;
            try {
                trace.stop();
            } catch (RuntimeException error) {
                Log.w(TAG, "Unable to stop performance trace", error);
            }
        }
    }

    private static final class SanitizedTelemetryException extends Exception {
        private SanitizedTelemetryException(String message) {
            super(message, null, false, true);
        }
    }
}
