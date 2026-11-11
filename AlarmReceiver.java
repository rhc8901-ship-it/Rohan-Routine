package com.rohanchauhan.dailyroutine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (a == null) return;
        if (a.equals(Alarms.A_FIRE)) {
            int rid = i.getIntExtra("rid", -1);
            JSONArray l = Alarms.list(c);
            for (int k = 0; k < l.length(); k++) {
                JSONObject o = l.optJSONObject(k);
                if (o == null || o.optInt("rid") != rid) continue;
                Alarms.scheduleOne(c, o); // re-arm tomorrow first, so one failure never kills the chain
                if (Alarms.master(c) && o.optBoolean("on", true))
                    ring(c, o.optString("tid"), o.optString("title"), o.optString("body"));
            }
        } else if (a.equals(Alarms.A_LATER)) {
            ring(c, i.getStringExtra("tid"), i.getStringExtra("title"), i.getStringExtra("body"));
        } else if (a.equals(Alarms.A_DONE)) {
            for (String[] x : AlarmService.snap()) Alarms.pend(c, x[0], x[2]);
            Alarms.log(c, "Marked done");
            c.stopService(new Intent(c, AlarmService.class));
        } else if (a.equals(Alarms.A_SNOOZE)) {
            int m = Alarms.snoozeMin(c);
            ArrayList<String[]> s = AlarmService.snap();
            for (String[] x : s) {
                Intent n = new Intent(c, AlarmReceiver.class).setAction(Alarms.A_LATER)
                        .putExtra("tid", x[0]).putExtra("title", x[1]).putExtra("body", "Snoozed reminder");
                Alarms.setClock(c, System.currentTimeMillis() + m * 60000L,
                        Alarms.pi(c, 800000 + Math.abs(x[0].hashCode() % 100000), n));
            }
            Alarms.log(c, "Snoozed " + m + " min");
            c.stopService(new Intent(c, AlarmService.class));
        } else if (a.equals(Alarms.A_STOP)) {
            Alarms.log(c, "Dismissed");
            c.stopService(new Intent(c, AlarmService.class));
        }
    }

    static void ring(Context c, String tid, String title, String body) {
        Alarms.log(c, "Fired: " + title);
        Intent s = new Intent(c, AlarmService.class).putExtra("tid", tid).putExtra("title", title).putExtra("body", body);
        try {
            ContextCompat.startForegroundService(c, s);
        } catch (Exception e) {
            Alarms.log(c, "Service blocked: " + e.getClass().getSimpleName());
        }
    }
}
