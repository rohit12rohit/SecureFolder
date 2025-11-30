package com.example.securefolder;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.ui.HomeActivity;
import com.example.securefolder.ui.login.LoginActivity;
import com.example.securefolder.ui.login.SignupActivity;
import com.example.securefolder.utils.AppPreferences;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use a dynamic layout for permission checking
        setContentView(R.layout.activity_main);

        // If we are logged in, go straight to Home
        if (getIntent().getBooleanExtra("IS_LOGGED_IN", false)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        layoutPermission = findViewById(R.id.layoutPermissionContainer);
        Button btnGrant = findViewById(R.id.btnGrantPermission);

        // CHECK PERMISSION
        if (!hasAllFilesPermission()) {
            // Show the permission button, hide the loader
            layoutPermission.setVisibility(View.VISIBLE);
            btnGrant.setOnClickListener(v -> requestPermission());
        } else {
            // Already have permission, proceed to App
            proceedToApp();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check again in case user came back from Settings
        if (hasAllFilesPermission() && layoutPermission.getVisibility() == View.VISIBLE) {
            proceedToApp();
        }
    }

    private boolean hasAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; // For older Android versions, legacy permissions are enough
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
    }

    private void proceedToApp() {
        AppPreferences prefs = new AppPreferences(this);
        if (prefs.isSetupDone()) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, SignupActivity.class));
        }
        finish();
    }
}