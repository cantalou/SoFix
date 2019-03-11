package com.wy.sofix.app;

import android.app.Application;


public class SoFixApplication extends Application {
    public static Application globalContext;

    @Override
    public void onCreate() {
        super.onCreate();
        globalContext = this;
    }
}
