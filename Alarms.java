package com.rohanchauhan.dailyroutine;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class Alarms {
    static final String A_FIRE = "com.rohanchauhan.dailyroutine.FIRE";
    static final String A_LATER = "com.rohanchauhan.dailyroutine.LATER";
    static final String A_DONE = "com.rohanchauhan.dailyroutine.DONE";
    static final String A_SNOOZE = "com.rohanchauhan.dailyroutine.SNOOZE";
    static final String A_STOP = "com.rohanchauhan.dailyroutine.STOP";
    static final String A_FIN = "com.rohanchauhan.dailyroutine.FINISH";
    static final String CH = "routine_alarm_v1";

    static SharedPreferences p(Context c) { return c.getSharedPreferences("routine", 0); }
    static boolean master(Context c) { return p(c).getBoolean("master", true); }
    static int snoozeMin(Context c) { return p(c).getInt("snooze", 5); }
    static int ringSec(Context c) { return p(c).getInt("ring", 60); }
    static String day() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    static JSONArray list(Context c) {
        try { return new JSONArray(p(c).getString("alarms", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        return ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
    }

    static PendingIntent pi(Context c, int code, Intent i) {
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static Intent fireIntent(Context c, int rid) {
        return new Intent(c, AlarmReceiver.class).setAction(A_FIRE).putExtra("rid", rid);
    }

    /** Next trigger strictly after "after". Time is wall-clock, so DST/timezone safe. */
    static long next(JSONObject a, long after) {
        Calendar cal = Calendar.getInstance();
        for (int d = 0; d <= 1; d++) {
            cal.setTimeInMillis(after);
            cal.add(Calendar.DAY_OF_YEAR, d);
            cal.set(Calendar.HOUR_OF_DAY, a.optInt("h"));
            cal.set(Calendar.MINUTE, a.optInt("m"));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() > after) return cal.getTimeInMillis();
        }
        return -1;
    }

    /** setAlarmClock = same API the system Clock app uses. Exempt from Doze and battery saver. */
    static void setClock(Context c, long t, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent show = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show), pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            log(c, "Exact alarm permission missing, using inexact fallback");
        }
    }

    static long scheduleOne(Context c, JSONObject a) {
        long t = next(a, System.currentTimeMillis());
        if (t > 0) setClock(c, t, pi(c, a.optInt("rid"), fireIntent(c, a.optInt("rid"))));
        return t;
    }

    static long scheduleAll(Context c) {
        if (!master(c)) return -1;
        JSONArray l = list(c);
        long first = -1;
        for (int k = 0; k < l.length(); k++) {
            JSONObject o = l.optJSONObject(k);
            if (o == null || !o.optBoolean("on", true)) continue;
            long t = scheduleOne(c, o);
            if (t > 0 && (first < 0 || t < first)) first = t;
        }
        return first;
    }

    static long scheduleAllPeek(Context c) {
        if (!master(c)) return -1;
        JSONArray l = list(c);
        long first = -1;
        for (int k = 0; k < l.length(); k++) {
            JSONObject o = l.optJSONObject(k);
            if (o == null || !o.optBoolean("on", true)) continue;
            long t = next(o, System.currentTimeMillis());
            if (t > 0 && (first < 0 || t < first)) first = t;
        }
        return first;
    }

    static void cancel(Context c, int rid) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pi(c, rid, fireIntent(c, rid)));
    }

    static void channel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CH) != null) return;
        NotificationChannel ch = new NotificationChannel(CH, "Routine alarms", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setBypassDnd(true);
        ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static void log(Context c, String msg) {
        try {
            JSONArray old = new JSONArray(p(c).getString("log", "[]"));
            JSONArray n = new JSONArray();
            n.put(new SimpleDateFormat("dd MMM HH:mm:ss", Locale.US).format(new Date()) + "  " + msg);
            for (int i = 0; i < old.length() && i < 14; i++) n.put(old.get(i));
            p(c).edit().putString("log", n.toString()).apply();
        } catch (Exception ignored) { }
    }

    static void pend(Context c, String tid, String day) {
        try {
            JSONArray a = new JSONArray(p(c).getString("pend", "[]"));
            a.put(tid + "|" + day);
            p(c).edit().putString("pend", a.toString()).apply();
        } catch (Exception ignored) { }
    }
}
