package com.example.downforce;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationReceiver — BroadcastReceiver #1 of 3.
 *
 * PURPOSE (why): turns a scheduled alarm into a visible notification.
 *
 * HOW (how): when the AlarmManager alarm fires, the system sends a broadcast
 * here. onReceive() reads the title/message/buttons from the Intent extras and
 * builds + shows the notification via NotificationHelper.build().
 *
 * Bagrut: BroadcastReceiver (req 10) + Notification (req 6).
 */
public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "id";
    public static final String EXTRA_ACTION_LABELS = "action_labels";
    public static final String EXTRA_ACTION_INTENTS = "action_intents";
    public static final String EXTRA_ACTION_FLAGS = "action_flags";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        int id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis());

        ArrayList<String> labels = intent.getStringArrayListExtra(EXTRA_ACTION_LABELS);
        ArrayList<Intent> intents = intent.getParcelableArrayListExtra(EXTRA_ACTION_INTENTS);
        ArrayList<Integer> flags = intent.getIntegerArrayListExtra(EXTRA_ACTION_FLAGS);

        List<NotificationHelper.Action> actions = new ArrayList<>();
        if (labels != null && intents != null && flags != null) {
            int n = Math.min(labels.size(), Math.min(intents.size(), flags.size()));
            for (int i = 0; i < n; i++) {
                actions.add(new NotificationHelper.Action(
                        labels.get(i), intents.get(i), flags.get(i) == 1));
            }
        }

        ctx.getSystemService(NotificationManager.class)
                .notify(id, NotificationHelper.build(ctx, title, message, actions, id));
    }
}