package com.example.downforce;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        int id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis());

        // Channel must exist; createChannel is idempotent.
        NotificationHelper.createChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "Race reminder")
                .setContentText(message != null ? message : "")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message != null ? message : ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(id, builder.build());
        }
    }
}