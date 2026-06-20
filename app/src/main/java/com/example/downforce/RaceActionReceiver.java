package com.example.downforce;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.NotificationManager;
import android.widget.Toast;

/**
 * RaceActionReceiver — BroadcastReceiver #2 of 3.
 *
 * PURPOSE (why): handles the "Watched" / "Skipped" buttons on the post-race
 * notification, so the user can log whether they watched without opening the app.
 *
 * HOW (how): each button is a broadcast PendingIntent with a different action.
 * onReceive() reads the action and saves "watched"/"skipped" to SharedPreferences
 * ("race_history", key race_{id}), then cancels the notification. WrappedActivity
 * later reads these values.
 *
 * Bagrut: BroadcastReceiver (req 10) + SharedPreferences (req 10).
 */
public class RaceActionReceiver extends BroadcastReceiver {

    public static final String ACTION_WATCHED = "com.example.downforce.RACE_WATCHED";
    public static final String ACTION_SKIPPED = "com.example.downforce.RACE_SKIPPED";
    public static final String EXTRA_RACE_ID = "race_id";
    public static final String EXTRA_RACE_NAME = "race_name";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private static final String PREFS = "race_history";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        int raceId = intent.getIntExtra(EXTRA_RACE_ID, -1);
        String raceName = intent.getStringExtra(EXTRA_RACE_NAME);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        if (action == null) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = "race_" + raceId;

        if (ACTION_WATCHED.equals(action)) {
            prefs.edit().putString(key, "watched").apply();
            Toast.makeText(ctx, "Marked '" + raceName + "' as watched", Toast.LENGTH_SHORT).show();
        } else if (ACTION_SKIPPED.equals(action)) {
            prefs.edit().putString(key, "skipped").apply();
            Toast.makeText(ctx, "Marked '" + raceName + "' as skipped", Toast.LENGTH_SHORT).show();
        }

        // Dismiss the notification once the user picks an option
        if (notificationId != -1) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(notificationId);
        }
    }
}