package com.example.downforce;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String CHANNEL_ID = "downforce_races";
    private static final String CHANNEL_NAME = "Race Reminders";
    private static final String CHANNEL_DESC = "Notifications about upcoming F1 races";

    private NotificationHelper() {} // static-only

    /**
     * Creates the notification channel. Safe to call multiple times — the
     * system ignores duplicates. Required on Android 8.0+ (API 26+); a no-op
     * below that.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    /**
     * Fire a notification right now. Use for user-action feedback.
     * Caller is responsible for ensuring POST_NOTIFICATIONS is granted on API 33+.
     */
    public static void sendImmediateNotification(Context context, String title, String message) {
        createChannel(context); // idempotent; cheap to ensure

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(uniqueId(), builder.build());
        }
    }

    /**
     * Schedule a notification at an exact wall-clock time.
     *
     * @param notificationId stable id — pass the same id to overwrite a prior
     *                       schedule (e.g., race id). Use a fresh id for
     *                       independent notifications.
     * @param timeInMillis   epoch millis (System.currentTimeMillis() basis).
     * @return true if scheduled; false if the OS denied exact-alarm permission.
     */
    public static boolean scheduleNotification(Context context,
                                               int notificationId,
                                               long timeInMillis,
                                               String title,
                                               String message) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return false;

        // On API 31+ exact alarms can be revoked by the user. Check before scheduling.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission not granted; skipping schedule for " + notificationId);
            return false;
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        // FLAG_IMMUTABLE required on API 31+. UPDATE_CURRENT lets a re-schedule
        // with the same id replace the prior pending alarm cleanly.
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // setExactAndAllowWhileIdle survives Doze. setExact alone does not.
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
        Log.d(TAG, "Scheduled notification " + notificationId + " for " + timeInMillis);
        return true;
    }

    /** Cancel a previously scheduled notification by id. */
    public static void cancelScheduled(Context context, int notificationId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    /**
     * Convenience: send the user to the system screen where they can grant
     * "Alarms & reminders" permission. Call this if scheduleNotification
     * returns false on API 31+.
     */
    public static void openExactAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + context.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    private static int uniqueId() {
        // Truncate to int; collisions are vanishingly unlikely in a session.
        return (int) (System.currentTimeMillis() & 0x7FFFFFFF);
    }
}