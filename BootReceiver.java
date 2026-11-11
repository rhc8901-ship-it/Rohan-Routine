package com.rohanchauhan.dailyroutine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arms every alarm after reboot, app update, clock/timezone change or permission change. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Alarms.scheduleAll(c);
        Alarms.log(c, "Alarms restored (" + i.getAction() + ")");
    }
}
