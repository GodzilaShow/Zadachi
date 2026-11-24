package de.godzila.zadachi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Планирует напоминания через AlarmManager и хранит список, чтобы восстановить после перезагрузки. */
public class Reminders {
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("reminders", Context.MODE_PRIVATE);
    }

    private static PendingIntent pi(Context c, String id, String title, String body, int flags) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("de.godzila.zadachi.REMIND." + id);
        i.putExtra("id", id);
        i.putExtra("title", title);
        i.putExtra("body", body);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static synchronized void sync(Context c, String json) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sp = prefs(c);
        for (String id : sp.getStringSet("ids", new HashSet<>())) {
            PendingIntent old = pi(c, id, "", "", PendingIntent.FLAG_NO_CREATE);
            if (old != null) { am.cancel(old); old.cancel(); }
        }
        Set<String> ids = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(json);
            long now = System.currentTimeMillis();
            for (int k = 0; k < arr.length(); k++) {
                JSONObject o = arr.getJSONObject(k);
                long when = o.getLong("when");
                if (when <= now) continue;
                String id = o.getString("id");
                schedule(c, am, id, when, o.optString("title", "Задача"), o.optString("body", ""));
                ids.add(id);
            }
        } catch (Exception ignored) { }
        sp.edit().putString("json", json).putStringSet("ids", ids).apply();
    }

    private static void schedule(Context c, AlarmManager am, String id, long when, String title, String body) {
        PendingIntent p = pi(c, id, title, body, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p);
        }
    }

    public static void restore(Context c) {
        sync(c, prefs(c).getString("json", "[]"));
    }
}
