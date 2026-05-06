package com.example.demo.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.demo.MainActivity;
import com.example.demo.R;
import com.example.demo.models.Subscription;

import java.util.Calendar;
import java.util.List;

/**
 * Handles subscription renewal notifications.
 * Creates a dedicated channel and fires alerts based on urgency.
 */
public class SubscriptionNotificationManager {

    private static final String CHANNEL_ID = "sub_alerts";
    private static final int SUMMARY_NOTIFICATION_ID = 9000;

    /**
     * Create the subscription alerts notification channel.
     * Call once at app startup.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.sub_alert_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.sub_alert_channel_desc));
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(Color.parseColor("#7c3aed"));

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Check all active subscriptions and fire appropriate notifications.
     */
    public static void checkAndNotify(Context context, List<Subscription> subs) {
        for (Subscription sub : subs) {
            if (!sub.isActive()) continue;

            long days = sub.daysLeft();
            if (days == 1) {
                fireAlert(context, sub, "URGENT");
            } else if (days >= 2 && days <= 3) {
                fireAlert(context, sub, "HIGH");
            } else if (days >= 4 && days <= 7) {
                fireAlert(context, sub, "WARNING");
            }
        }

        // Monthly summary on 1st of month
        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.DAY_OF_MONTH) == 1) {
            fireMonthlySummary(context, subs);
        }
    }

    /**
     * Fire an individual subscription alert notification.
     */
    private static void fireAlert(Context context, Subscription sub, String type) {
        String title;
        String body;
        int price = (int) sub.getPrice();

        switch (type) {
            case "URGENT":
                title = "⚡ " + sub.getName() + " KAL renew hoga!";
                body = "₹" + price + " kal debit hoga — ready rakho";
                break;
            case "HIGH":
                title = "🔴 " + sub.getName() + " " + sub.daysLeft() + " din mein";
                body = "₹" + price + " — renewal coming up";
                break;
            case "WARNING":
                title = "🟡 " + sub.getName() + " is hafte renew hoga";
                body = "₹" + price + " • " + sub.daysLeft() + " din baaki";
                break;
            default:
                return;
        }

        // Deep link to app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, sub.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_subscription)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(Color.parseColor("#7c3aed"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context)
                    .notify(sub.getId().hashCode(), builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    /**
     * Fire a monthly summary notification on the 1st of each month.
     */
    private static void fireMonthlySummary(Context context, List<Subscription> subs) {
        double total = 0;
        int active = 0;
        for (Subscription s : subs) {
            if (s.isActive()) {
                total += s.monthlyPrice();
                active++;
            }
        }

        if (active == 0) return;

        String title = "📊 Monthly Subscription Summary";
        String body = "Is mahine ₹" + (int) total + " subscriptions pe • " + active + " active";

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, SUMMARY_NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_subscription)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(Color.parseColor("#1D9E75"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context)
                    .notify(SUMMARY_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted
        }
    }
}
