package com.example.securefolder.ui.modules;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.BuildConfig;
import com.example.securefolder.R;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.BackupHelper;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private AppPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (KeyManager.getMasterKey() == null) { finish(); return; }

        prefs = new AppPreferences(this);
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        progressBar = findViewById(R.id.progressBar);
        Button btnChangePass = findViewById(R.id.btnChangePassword);
        Button btnBackup = findViewById(R.id.btnBackup);
        Button btnDeleteAll = findViewById(R.id.btnDeleteAll);

        // Setup Buttons
        btnChangePass.setOnClickListener(v -> showChangePassDialog());
        btnBackup.setOnClickListener(v -> performBackup());
        btnDeleteAll.setOnClickListener(v -> showWipeDialog());

        // Setup Auto-Lock UI (Dynamic)
        setupAutoLockUI();
    }

    private void setupAutoLockUI() {
        // Find container
        LinearLayout container = (LinearLayout) findViewById(R.id.btnChangePassword).getParent();

        Button btnLockTimeout = new Button(this);
        btnLockTimeout.setText("Auto-Lock: " + getTimeoutLabel(prefs.getLockTimeout()));
        container.addView(btnLockTimeout, 1); // Add below first button

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputNewPass = new EditText(this);
        inputNewPass.setHint("New Password");
        layout.addView(inputNewPass);

        final EditText inputConfirm = new EditText(this);
        inputConfirm.setHint("Confirm Password");
        layout.addView(inputConfirm);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPass = inputNewPass.getText().toString();
            String confirm = inputConfirm.getText().toString();

            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = KeyManager.changePassword(this, newPass);
            if (success) {
                Toast.makeText(this, "Password Changed Successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
                            .setMessage("Saved to: " + path)
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
                .setTitle("Wipe ALL Data?")
                .setMessage("This will permanently delete all files, notes, and passwords. This cannot be undone.")
                .setPositiveButton("Wipe Everything", (d, w) -> {
                    // 1. Clear Database
                    DatabaseHelper db = new DatabaseHelper(this);
                    db.getWritableDatabase().execSQL("DELETE FROM " + DatabaseHelper.TABLE_FILES);
                    db.getWritableDatabase().execSQL("DELETE FROM " + DatabaseHelper.TABLE_NOTES);
                    db.getWritableDatabase().execSQL("DELETE FROM " + DatabaseHelper.TABLE_PASSWORDS);

                    // 2. Recursive Delete of Vault Folder
                    File vaultRoot = new File(getExternalFilesDir(null), "Vault");
                    deleteRecursive(vaultRoot);

                    // 3. Clear Prefs
                    prefs.clearAllData();
                    KeyManager.clearKey();

                    Toast.makeText(this, "App Reset Complete", Toast.LENGTH_SHORT).show();
                    finishAffinity(); // Close app
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
        }
    }
}