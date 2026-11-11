package com.rohanchauhan.dailyroutine;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

/** Full-screen alarm shown over the lock screen. Built in code, no XML needed. */
public class AlarmActivity extends Activity {
    private TextView ttl;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver fin = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { finish(); }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            String t = AlarmService.titles();
            if (t.isEmpty()) { finish(); return; }
            ttl.setText(t);
            h.postDelayed(this, 1000);
        }
    };

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setGravity(Gravity.CENTER);
        r.setPadding(dp(28), dp(40), dp(28), dp(40));
        r.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF0B0E1C, 0xFF2A2F7A}));
        TextView lab = text("ROUTINE ALERT", 13, 0xFFB7BEFF, false);
        lab.setLetterSpacing(0.15f);
        TextClock clk = new TextClock(this);
        clk.setFormat12Hour("h:mm a");
        clk.setFormat24Hour("h:mm a");
        clk.setTextSize(58);
        clk.setTextColor(0xFFFFFFFF);
        clk.setGravity(Gravity.CENTER);
        ttl = text("", 26, 0xFFFFFFFF, true);
        ttl.setPadding(0, dp(12), 0, dp(36));
        r.addView(lab);
        r.addView(clk);
        r.addView(ttl);
        r.addView(btn("Done", 0xFF16A36A, Alarms.A_DONE));
        r.addView(btn("Snooze " + Alarms.snoozeMin(this) + " min", 0x40FFFFFF, Alarms.A_SNOOZE));
        r.addView(btn("Dismiss", 0x22FFFFFF, Alarms.A_STOP));
        setContentView(r);
    }

    private TextView text(String s, int sp, int col, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(col);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button btn(String t, int col, final String act) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(18);
        b.setTextColor(0xFFFFFFFF);
        GradientDrawable g = new GradientDrawable();
        g.setColor(col);
        g.setCornerRadius(dp(18));
        b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60));
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> {
            sendBroadcast(new Intent(this, AlarmReceiver.class).setAction(act));
            finish();
        });
        return b;
    }

    @Override protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, fin, new IntentFilter(Alarms.A_FIN), ContextCompat.RECEIVER_NOT_EXPORTED);
        h.post(tick);
    }

    @Override protected void onStop() {
        super.onStop();
        h.removeCallbacks(tick);
        try { unregisterReceiver(fin); } catch (Exception ignored) { }
    }
}
