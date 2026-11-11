package com.rohanchauhan.dailyroutine;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;

public class AlarmService extends Service {
    static final ArrayList<String[]> ITEMS = new ArrayList<>(); // {taskId, title, day}
    private MediaPlayer mp;
    private Vibrator vb;
    private PowerManager.WakeLock wl;
    private final Handler h = new Handler(Looper.getMainLooper());
    private float vol = 0.3f;
    private final Runnable ramp = new Runnable() {
        @Override public void run() {
            vol = Math.min(1f, vol + 0.1f);
            try { if (mp != null) mp.setVolume(vol, vol); } catch (Exception ignored) { }
            if (vol < 1f) h.postDelayed(this, 1000);
        }
    };
    private final Runnable timeout = new Runnable() {
        @Override public void run() {
            Alarms.log(AlarmService.this, "Missed (auto-stopped): " + titles());
            stopSelf();
        }
    };

    static ArrayList<String[]> snap() { synchronized (ITEMS) { return new ArrayList<>(ITEMS); } }

    static String titles() {
        StringBuilder sb = new StringBuilder();
        for (String[] x : snap()) { if (sb.length() > 0) sb.append(" • "); sb.append(x[1]); }
        return sb.toString();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i == null) { stopSelf(); return START_NOT_STICKY; }
        String tid = String.valueOf(i.getStringExtra("tid"));
        String title = String.valueOf(i.getStringExtra("title"));
        synchronized (ITEMS) {
            boolean has = false;
            for (String[] x : ITEMS) if (x[0].equals(tid)) has = true;
            if (!has) ITEMS.add(new String[]{tid, title, Alarms.day()});
        }
        Alarms.channel(this);
        Notification n = build(this, i.getStringExtra("body"));
        if (Build.VERSION.SDK_INT >= 29) startForeground(7001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(7001, n);
        if (mp == null) begin();
        h.removeCallbacks(timeout);
        h.postDelayed(timeout, Alarms.ringSec(this) * 1000L);
        return START_NOT_STICKY;
    }

    private void begin() {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            mp.setDataSource(this, u);
            mp.setLooping(true);
            mp.prepare();
            mp.setVolume(vol, vol);
            mp.start();
            h.postDelayed(ramp, 1000);
        } catch (Exception e) {
            Alarms.log(this, "Sound error: " + e.getClass().getSimpleName());
        }
        try {
            vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pat = {0, 600, 400};
            if (vb != null) {
                if (Build.VERSION.SDK_INT >= 26) vb.vibrate(VibrationEffect.createWaveform(pat, 0));
                else vb.vibrate(pat, 0);
            }
        } catch (Exception ignored) { }
        try {
            wl = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "routine:alarm");
            wl.acquire(Alarms.ringSec(this) * 1000L + 10000L);
        } catch (Exception ignored) { }
    }

    static Notification build(Context c, String body) {
        PendingIntent fs = PendingIntent.getActivity(c, 2, new Intent(c, AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(c, Alarms.CH)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(titles())
                .setContentText(body == null ? "Routine alert" : body)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(fs, true)
                .setContentIntent(fs)
                .addAction(0, "Done", Alarms.pi(c, 11, new Intent(c, AlarmReceiver.class).setAction(Alarms.A_DONE)))
                .addAction(0, "Snooze", Alarms.pi(c, 12, new Intent(c, AlarmReceiver.class).setAction(Alarms.A_SNOOZE)))
                .addAction(0, "Stop", Alarms.pi(c, 13, new Intent(c, AlarmReceiver.class).setAction(Alarms.A_STOP)))
                .build();
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception ignored) { }
        try { if (vb != null) vb.cancel(); } catch (Exception ignored) { }
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) { }
        synchronized (ITEMS) { ITEMS.clear(); }
        sendBroadcast(new Intent(Alarms.A_FIN).setPackage(getPackageName()));
        super.onDestroy();
    }
}
