package com.example.smarthomeauto;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppInitializer.initializeDatabase(this);
    }
}
