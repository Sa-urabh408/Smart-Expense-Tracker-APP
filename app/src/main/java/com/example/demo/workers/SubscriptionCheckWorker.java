package com.example.demo.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.demo.models.Subscription;
import com.example.demo.utils.SubscriptionNotificationManager;
import com.example.demo.utils.SubscriptionStorage;

import java.util.List;

/**
 * WorkManager periodic worker that runs every 24 hours.
 * Loads subscriptions from SharedPreferences and fires renewal notifications.
 * Works even when the app is closed.
 */
public class SubscriptionCheckWorker extends Worker {

    public SubscriptionCheckWorker(@NonNull Context context,
                                    @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<Subscription> subs = SubscriptionStorage.load(getApplicationContext());
            SubscriptionNotificationManager.checkAndNotify(getApplicationContext(), subs);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
