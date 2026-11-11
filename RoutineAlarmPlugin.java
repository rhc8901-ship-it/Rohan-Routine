package com.rohanchauhan.dailyroutine;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.app.NotificationManagerCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "RoutineAlarm", permissions = {
        @Permission(strings = {Manifest.permission.POST_NOTIFICATIONS}, alias = "notif")})
public class RoutineAlarmPlugin extends Plugin {

    @PluginMethod
    public void schedule(PluginCall c) {
        Context x = getContext();
        JSONArray old = Alarms.list(x);
        for (int k = 0; k < old.length(); k++) {
            JSONObject o = old.optJSONObject(k);
            if (o != null) Alarms.cancel(x, o.optInt("rid"));
        }
        JSArray arr = c.getArray("alarms");
        x.getSharedPreferences("routine", 0).edit()
                .putString("alarms", arr == null ? "[]" : arr.toString())
                .putBoolean("master", c.getBoolean("master", true))
                .putInt("snooze", c.getInt("snooze", 5))
                .putInt("ring", c.getInt("ring", 60)).apply();
        Alarms.channel(x);
        long nx = Alarms.scheduleAll(x);
        JSObject r = new JSObject();
        r.put("next", nx);
        c.resolve(r);
    }

    @PluginMethod
    public void status(PluginCall c) {
        Context x = getContext();
        JSObject r = new JSObject();
        r.put("notif", NotificationManagerCompat.from(x).areNotificationsEnabled());
        r.put("exact", Alarms.canExact(x));
        r.put("battery", ((PowerManager) x.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(x.getPackageName()));
        boolean fs = true;
        if (Build.VERSION.SDK_INT >= 34) fs = ((NotificationManager) x.getSystemService(Context.NOTIFICATION_SERVICE)).canUseFullScreenIntent();
        r.put("full", fs);
        r.put("log", Alarms.p(x).getString("log", "[]"));
        r.put("next", Alarms.scheduleAllPeek(x));
        c.resolve(r);
    }

    @PluginMethod
    public void requestNotif(PluginCall c) {
        if (Build.VERSION.SDK_INT < 33 || getPermissionState("notif") == PermissionState.GRANTED) { c.resolve(); return; }
        requestPermissionForAlias("notif", c, "permDone");
    }

    @PermissionCallback
    private void permDone(PluginCall c) { c.resolve(); }

    @PluginMethod
    public void open(PluginCall c) {
        String pg = c.getString("page", "app");
        String pk = getContext().getPackageName();
        Uri u = Uri.parse("package:" + pk);
        Intent i;
        if (pg.equals("exact") && Build.VERSION.SDK_INT >= 31) i = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", u);
        else if (pg.equals("battery")) i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, u);
        else if (pg.equals("notif")) i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pk);
        else if (pg.equals("full") && Build.VERSION.SDK_INT >= 34) i = new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", u);
        else i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, u);
        try {
            getContext().startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            getContext().startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        c.resolve();
    }

    @PluginMethod
    public void test(PluginCall c) {
        Context x = getContext();
        Alarms.channel(x);
        int s = c.getInt("seconds", 10);
        Intent n = new Intent(x, AlarmReceiver.class).setAction(Alarms.A_LATER)
                .putExtra("tid", "test").putExtra("title", "Test alarm").putExtra("body", "Your alarms are working.");
        Alarms.setClock(x, System.currentTimeMillis() + s * 1000L, Alarms.pi(x, 777, n));
        c.resolve();
    }

    @PluginMethod
    public void pending(PluginCall c) {
        Context x = getContext();
        JSObject r = new JSObject();
        r.put("items", Alarms.p(x).getString("pend", "[]"));
        Alarms.p(x).edit().remove("pend").apply();
        c.resolve(r);
    }
}
