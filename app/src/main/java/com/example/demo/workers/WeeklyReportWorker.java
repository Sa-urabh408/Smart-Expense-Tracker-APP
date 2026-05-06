package com.example.demo.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.demo.MainActivity;
import com.example.demo.R;
import com.example.demo.data.TransactionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Worker to generate and send a weekly expense report notification.
 */
public class WeeklyReportWorker extends Worker {

    private static final String CHANNEL_ID = "weekly_report_channel";
    private final TransactionRepository repository;

    public WeeklyReportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = new TransactionRepository((android.app.Application) context.getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.failure();
        }

        String userId = user.getUid();
        
        // Calculate the past 7 days range
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfWeek = cal.getTimeInMillis();
        
        cal.add(Calendar.DAY_OF_YEAR, -7);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startOfWeek = cal.getTimeInMillis();

        final double[] totalWeeklyExpense = {0.0};
        CountDownLatch latch = new CountDownLatch(1);

        repository.getWeeklyExpense(userId, startOfWeek, endOfWeek).observeForever(expense -> {
            totalWeeklyExpense[0] = expense != null ? expense : 0.0;
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            return Result.failure();
        }

        sendNotification(totalWeeklyExpense[0]);
        return Result.success();
    }

    private void sendNotification(double weeklyExpense) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Weekly Reports",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Shows your weekly expense summary");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        String message = "You spent " + format.format(weeklyExpense) + " this week. Tap to view details.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_expense)
                .setContentTitle("Weekly Expense Report")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }
}
