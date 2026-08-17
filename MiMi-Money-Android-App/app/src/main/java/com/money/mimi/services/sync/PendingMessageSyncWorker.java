package com.money.mimi.services.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.services.MainService;

/** Performs bounded outbound synchronization without keeping a foreground service alive. */
public final class PendingMessageSyncWorker extends Worker {
    public PendingMessageSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (PreferenceManager.getToken(context) == null
                || PreferenceManager.isNeedProvideInfo(context)) {
            return Result.success();
        }
        try {
            return MainService.syncPendingMessagesBlocking(context)
                    ? Result.success() : Result.retry();
        } catch (RuntimeException e) {
            AppHelper.LogCat("PendingMessageSyncWorker failed: " + e.getMessage());
            return getRunAttemptCount() < 4 ? Result.retry() : Result.failure();
        }
    }
}
