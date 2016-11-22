package it.snipsnap.slyce_messaging_example;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by sambains on 05/09/2016.
 */

public class SlyceMessagingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            LeakCanary.install(this);
        }
    }
}
