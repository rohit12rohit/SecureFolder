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
import com.example.securefolder.utils.KeyManager;

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
            checkLockout(activity);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            lastBackgroundTimestamp = System.currentTimeMillis();
            // Optional: Immediate lock on background?
            // KeyManager.clearKey();
        }
    }

    private void checkLockout(Activity activity) {
        if (activity instanceof LoginActivity) return;

        AppPreferences prefs = new AppPreferences(this);
        long timeout = prefs.getLockTimeout();
        long diff = System.currentTimeMillis() - lastBackgroundTimestamp;

        if (lastBackgroundTimestamp > 0 && diff >= timeout) {
            forceLock(activity);
        }
    }

    public void forceLock(Activity activity) {
        // FIX: WIPE KEY FROM MEMORY
        KeyManager.clearKey();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        activity.finish();
    }

    // --- PANIC SWITCH ---

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
            if (z < -9.0 && isFaceUp) {
                isFaceUp = false;
                // PANIC: Lock and Wipe Key
                KeyManager.clearKey();

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