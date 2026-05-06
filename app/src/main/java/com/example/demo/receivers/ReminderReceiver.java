package com.example.demo.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.demo.utils.NotificationHelper;

/**
 * BroadcastReceiver for daily expense tracking reminder notifications.
 * Triggered by AlarmManager at 8:00 PM each day and on device boot.
 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Show the reminder notification
        NotificationHelper.showReminderNotification(context);

        // If device just booted, reschedule the daily alarm
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationHelper.scheduleDailyReminder(context);
        }
    }
}
