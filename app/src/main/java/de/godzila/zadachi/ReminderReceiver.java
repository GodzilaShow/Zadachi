package de.godzila.zadachi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.MY_PACKAGE_REPLACED".equals(action)) {
            Reminders.restore(c);
            return;
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel("tasks", "Напоминания", NotificationManager.IMPORTANCE_HIGH);
        ch.enableVibration(true);
        ch.setLightColor(Color.parseColor("#FF5A1F"));
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String id = intent.getStringExtra("id");
        Notification n = new Notification.Builder(c, "tasks")
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(Color.parseColor("#FF5A1F"))
                .setContentTitle(intent.getStringExtra("title"))
                .setContentText(intent.getStringExtra("body"))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        try { nm.notify(id == null ? 1 : id.hashCode(), n); } catch (SecurityException ignored) { }
    }
}
