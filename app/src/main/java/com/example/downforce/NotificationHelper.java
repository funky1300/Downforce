package com.example.downforce;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class NotificationHelper {

    public static final String CHANNEL_ID = "downforce_races";

    /** One action button. isBroadcast=true → silent (no app open). false → opens activity. */
    public static class Action {
        public final String label;
        public final Intent intent;
        public final boolean isBroadcast;

        public Action(String label, Intent intent, boolean isBroadcast) {
            this.label = label;
            this.intent = intent;
            this.isBroadcast = isBroadcast;
        }
    }

    private NotificationHelper() {}

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Race Reminders", NotificationManager.IMPORTANCE_HIGH);
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static void sendImmediateNotification(Context ctx, String title, String message,
                                                 List<Action> actions) {
        createChannel(ctx);
        int id = (int) System.currentTimeMillis();
        ctx.getSystemService(NotificationManager.class)
                .notify(id, build(ctx, title, message, actions, id));
    }

    public static boolean scheduleNotification(Context ctx, int notificationId, long timeInMillis,
                                               String title, String message, List<Action> actions) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return false;

        Intent intent = new Intent(ctx, NotificationReceiver.class)
                .putExtra(NotificationReceiver.EXTRA_TITLE, title)
                .putExtra(NotificationReceiver.EXTRA_MESSAGE, message)
                .putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                .putParcelableArrayListExtra(NotificationReceiver.EXTRA_ACTION_INTENTS, intentsOf(actions))
                .putStringArrayListExtra(NotificationReceiver.EXTRA_ACTION_LABELS, labelsOf(actions))
                .putIntegerArrayListExtra(NotificationReceiver.EXTRA_ACTION_FLAGS, flagsOf(actions));

        PendingIntent pi = PendingIntent.getBroadcast(ctx, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
        return true;
    }

    static android.app.Notification build(Context ctx, String title, String message,
                                          List<Action> actions, int baseId) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (actions != null) {
            for (int i = 0; i < actions.size(); i++) {
                Action a = actions.get(i);
                int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
                int reqCode = baseId * 100 + i + 1; // unique per (notification, button)

                PendingIntent pi = a.isBroadcast
                        ? PendingIntent.getBroadcast(ctx, reqCode, a.intent, flags)
                        : PendingIntent.getActivity(ctx, reqCode, a.intent, flags);
                b.addAction(0, a.label, pi);
            }
        }
        return b.build();
    }

    private static ArrayList<Intent> intentsOf(List<Action> a) {
        ArrayList<Intent> out = new ArrayList<>();
        if (a != null) for (Action x : a) out.add(x.intent);
        return out;
    }
    private static ArrayList<String> labelsOf(List<Action> a) {
        ArrayList<String> out = new ArrayList<>();
        if (a != null) for (Action x : a) out.add(x.label);
        return out;
    }
    private static ArrayList<Integer> flagsOf(List<Action> a) {
        ArrayList<Integer> out = new ArrayList<>();
        if (a != null) for (Action x : a) out.add(x.isBroadcast ? 1 : 0);
        return out;
    }
}