package com.example.applaunchscheduler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class LaunchMonitorService extends Service {

    private Handler handler;
    private UsageStatsManager usageStatsManager;
    private SharedPreferences prefs;

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            checkForegroundApp();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        prefs = getSharedPreferences("schedule", MODE_PRIVATE);
        startForeground(1, createNotification());
        handler.post(checker);
    }

    private Notification createNotification() {
        String channelId = "monitor";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Monitor", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("App monitor running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void checkForegroundApp() {
        long end = System.currentTimeMillis();
        long begin = end - 1000;
        UsageEvents events = usageStatsManager.queryEvents(begin, end);
        UsageEvents.Event event = new UsageEvents.Event();
        String pkg = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                pkg = event.getPackageName();
            }
        }
        if (pkg != null && isRestricted(pkg)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private boolean isRestricted(String pkg) {
        Set<String> pkgs = new HashSet<>(prefs.getStringSet("packages", new HashSet<>()));
        if (!pkgs.contains(pkg)) return false;
        int startMin = prefs.getInt("startMinutes", 0);
        int endMin = prefs.getInt("endMinutes", 23 * 60 + 59);
        Calendar cal = Calendar.getInstance();
        int now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        if (startMin <= endMin) {
            return now >= startMin && now <= endMin;
        } else {
            return now >= startMin || now <= endMin;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checker);
        super.onDestroy();
    }
}
