package com.example.securefolder.ui.modules;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.BuildConfig;
import com.example.securefolder.R;
import com.example.securefolder.ui.login.SignupActivity;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.BackupHelper;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;

public class SettingsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private AppPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new AppPreferences(this);

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        progressBar = findViewById(R.id.progressBar);
        Button btnChangePass = findViewById(R.id.btnChangePassword);
        Button btnBackup = findViewById(R.id.btnBackup);
        Button btnDeleteAll = findViewById(R.id.btnDeleteAll);

        // This assumes you might add a standard button in XML later,
        // but for now we dynamically add the lock button as per your prototype.
        LinearLayout container = (LinearLayout) btnChangePass.getParent();
        Button btnLockTimeout = new Button(this);
        btnLockTimeout.setText("Auto-Lock: " + getTimeoutLabel(prefs.getLockTimeout()));
        container.addView(btnLockTimeout, 2);

        btnChangePass.setOnClickListener(v -> showChangePassDialog());
        btnBackup.setOnClickListener(v -> performBackup());
        btnDeleteAll.setOnClickListener(v -> showWipeDialog());

        btnLockTimeout.setOnClickListener(v -> {
            String[] options = {"Immediately", "5 Seconds", "30 Seconds", "1 Minute"};
            long[] values = {0, 5000, 30000, 60000};

            new AlertDialog.Builder(this)
                    .setTitle("Auto-Lock Timeout")
                    .setItems(options, (d, which) -> {
                        prefs.setLockTimeout(values[which]);
                        btnLockTimeout.setText("Auto-Lock: " + options[which]);
                    })
                    .show();
        });
    }

    private String getTimeoutLabel(long ms) {
        if (ms == 0) return "Immediately";
        if (ms == 5000) return "5 Seconds";
        if (ms == 30000) return "30 Seconds";
        if (ms == 60000) return "1 Minute";
        return "Custom";
    }

    private void showChangePassDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("To change your password, please use the 'Forgot Password' feature on the login screen for now.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void performBackup() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String path = BackupHelper.createSecureBackup(this);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (path != null) {
                    new AlertDialog.Builder(this)
                            .setTitle("Backup Successful")
                            .setMessage("Saved to Downloads folder.\nFile: " + path)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(this, "Backup Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showWipeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Wipe All Data?")
                .setMessage("This will delete all files, reset the app, and remove your password. This action is irreversible.")
                .setPositiveButton("Wipe", (d, w) -> {
                    // 1. Wipe Database
                    DatabaseHelper db = new DatabaseHelper(this);
                    db.getWritableDatabase().execSQL("DELETE FROM " + DatabaseHelper.TABLE_FILES);

                    // 2. Clear Preferences (Keys, Flags)
                    prefs.clearAllData();

                    // 3. Clear Key from RAM
                    KeyManager.clearKey();

                    Toast.makeText(this, "Vault Wiped", Toast.LENGTH_SHORT).show();

                    // 4. Restart to Signup
                    Intent intent = new Intent(this, SignupActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}