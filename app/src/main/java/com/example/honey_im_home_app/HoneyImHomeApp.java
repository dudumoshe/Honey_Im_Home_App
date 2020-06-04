package com.example.honey_im_home_app;

import android.app.Application;

public class HoneyImHomeApp extends Application {
    LocationTracker locationTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        locationTracker = new LocationTracker(this);
    }
}
