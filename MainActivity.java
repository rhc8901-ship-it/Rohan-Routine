package com.rohanchauhan.dailyroutine;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(RoutineAlarmPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
