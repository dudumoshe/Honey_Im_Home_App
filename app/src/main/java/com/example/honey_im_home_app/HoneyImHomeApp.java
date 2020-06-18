package com.example.honey_im_home_app;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class HoneyImHomeApp extends Application {
    LocationTracker locationTracker;
    BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        locationTracker = new LocationTracker(this);
        this.broadcastReceiver = new LocalSendSmsBroadcastReceiver();
        registerReceiver(this.broadcastReceiver, new IntentFilter(LocalSendSmsBroadcastReceiver.actionName));
    }
}
