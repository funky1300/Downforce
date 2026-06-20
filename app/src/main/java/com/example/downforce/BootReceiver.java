package com.example.downforce;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * BootReceiver — BroadcastReceiver #3 of 3.
 *
 * PURPOSE (why): Android deletes all AlarmManager alarms when the device reboots.
 * This re-schedules every future race notification after a restart.
 *
 * HOW (how): listens for ACTION_BOOT_COMPLETED, re-fetches the calendar from
 * OpenF1, and re-registers the alarms. Uses goAsync() to keep the receiver alive
 * while the network call runs (onReceive normally must return quickly), then
 * pendingResult.finish() when done.
 *
 * Bagrut: BroadcastReceiver (req 10).
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // goAsync() keeps the receiver alive while the network call runs in background
        PendingResult pendingResult = goAsync();

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://api.openf1.org/v1/meetings?year=2026";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray meetings = new JSONArray(response);
                        long now = System.currentTimeMillis();

                        for (int i = 0; i < meetings.length(); i++) {
                            JSONObject obj = meetings.getJSONObject(i);
                            String name = obj.optString("meeting_name", "");
                            if (name.contains("Testing")) continue;

                            Race race = new Race(
                                    obj.optInt("meeting_key", i), name,
                                    obj.optString("location", ""),
                                    obj.optString("date_start", ""),
                                    obj.optString("date_end", ""),
                                    obj.optString("circuit_image", ""),
                                    obj.optString("country_flag", "")
                            );

                            long beforeStart = race.endDate.toInstant().toEpochMilli() - (10 * 60 * 1000);
                            if (beforeStart > now) {
                                NotificationHelper.scheduleNotification(context,
                                        race.getId() * 10, beforeStart,
                                        "Race Starting Soon!",
                                        race.getName() + " starts in 10 minutes.", null);
                            }

                            long afterEnd = race.endDate.toInstant().toEpochMilli() + (2 * 60 * 60 * 1000);
                            if (afterEnd > now) {
                                int notifId = race.getId() * 10 + 1;

                                Intent watched = new Intent(context, RaceActionReceiver.class)
                                        .setAction(RaceActionReceiver.ACTION_WATCHED)
                                        .putExtra(RaceActionReceiver.EXTRA_RACE_ID, race.getId())
                                        .putExtra(RaceActionReceiver.EXTRA_RACE_NAME, race.getName())
                                        .putExtra(RaceActionReceiver.EXTRA_NOTIFICATION_ID, notifId);

                                Intent skipped = new Intent(context, RaceActionReceiver.class)
                                        .setAction(RaceActionReceiver.ACTION_SKIPPED)
                                        .putExtra(RaceActionReceiver.EXTRA_RACE_ID, race.getId())
                                        .putExtra(RaceActionReceiver.EXTRA_RACE_NAME, race.getName())
                                        .putExtra(RaceActionReceiver.EXTRA_NOTIFICATION_ID, notifId);

                                List<NotificationHelper.Action> actions = Arrays.asList(
                                        new NotificationHelper.Action("✓ Watched", watched, true),
                                        new NotificationHelper.Action("✗ Skipped", skipped, true)
                                );

                                NotificationHelper.scheduleNotification(context, notifId, afterEnd,
                                        "Did you watch?",
                                        "The " + race.getName() + " ended 2 hours ago.", actions);
                            }
                        }
                    } catch (Exception ignored) {
                        // silently fail — notifications just won't be rescheduled
                    } finally {
                        pendingResult.finish();
                    }
                },
                error -> pendingResult.finish());

        queue.add(request);
    }
}
