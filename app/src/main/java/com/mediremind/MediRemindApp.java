package com.mediremind;

import android.app.Application;

import com.mediremind.notifications.NotificationHelper;

public class MediRemindApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Create notification channels on app start (required for Android 8+)
        NotificationHelper.createNotificationChannels(this);
    }
}
