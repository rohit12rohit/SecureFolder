package com.example.securefolder;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.securefolder.ui.login.LoginActivity;
import com.example.securefolder.utils.AppPreferences;

public class SecureApplication extends Application implements Application.ActivityLifecycleCallbacks, SensorEventListener {

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private long lastBackgroundTimestamp = 0;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isFaceUp = true;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App entered foreground
            checkLockout(activity);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App entered background
            lastBackgroundTimestamp = System.currentTimeMillis();
        }
    }

    private void checkLockout(Activity activity) {
        if (activity instanceof LoginActivity) return;

        // If app was in background for more than 30 seconds (example), lock it.
        // Or if you want immediate lock, just remove the time check.
        long diff = System.currentTimeMillis() - lastBackgroundTimestamp;
        if (lastBackgroundTimestamp > 0 && diff > 1000) { // 1 second buffer
            forceLock(activity);
        }
    }

    public void forceLock(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (activity != null) activity.finish();
    }

    // --- PANIC SWITCH (Face Down Lock) ---

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float z = event.values[2];
            // If phone is face down (Z axis < -9)
            if (z < -9.0 && isFaceUp) {
                isFaceUp = false;
                // Trigger Lock
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else if (z > 0) {
                isFaceUp = true;
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}