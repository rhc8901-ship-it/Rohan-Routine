import sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
P = "com.rohanchauhan.dailyroutine"
perms = ["POST_NOTIFICATIONS", "USE_EXACT_ALARM", "RECEIVE_BOOT_COMPLETED", "VIBRATE", "WAKE_LOCK",
         "USE_FULL_SCREEN_INTENT", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PLAYBACK",
         "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"]
add = "".join('<uses-permission android:name="android.permission.%s"/>' % x for x in perms)
add += '<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32"/>'
acts = ["android.intent.action.BOOT_COMPLETED", "android.intent.action.MY_PACKAGE_REPLACED",
        "android.intent.action.TIME_SET", "android.intent.action.TIMEZONE_CHANGED",
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"]
comp = ('<receiver android:name="%s.AlarmReceiver" android:exported="false"/>' % P
        + '<receiver android:name="%s.BootReceiver" android:exported="true"><intent-filter>' % P
        + "".join('<action android:name="%s"/>' % a for a in acts) + '</intent-filter></receiver>'
        + '<service android:name="%s.AlarmService" android:exported="false" android:foregroundServiceType="mediaPlayback"/>' % P
        + '<activity android:name="%s.AlarmActivity" android:exported="false" android:launchMode="singleInstance" '
          'android:taskAffinity="" android:excludeFromRecents="true" android:showWhenLocked="true" '
          'android:turnScreenOn="true" android:theme="@android:style/Theme.DeviceDefault.NoActionBar"/>' % P)
assert "<application" in s and "</application>" in s
s = s.replace("<application", add + "<application", 1).replace("</application>", comp + "</application>", 1)
open(p, "w", encoding="utf-8").write(s)
print("manifest patched")
