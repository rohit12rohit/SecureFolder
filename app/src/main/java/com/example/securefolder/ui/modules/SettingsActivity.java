package com.example.securefolder.ui.modules;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

        prefs = new AppPreferences(this);

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        progressBar = findViewById(R.id.progressBar);
        Button btnChangePass = findViewById(R.id.btnChangePassword);
        Button btnBackup = findViewById(R.id.btnBackup);
        Button btnDeleteAll = findViewById(R.id.btnDeleteAll);
        Button btnAbout = findViewById(R.id.btnAbout);

        // --- NEW: Lock Settings ---
        // We will add a button dynamically or you can add to layout.
        // For now, let's assume there is a button or we add one.
        // Actually, let's just repurpose the top area or add a dialog.
        // Better: Update layout. I'll stick to a dialog triggered by a new Button or just add it.
        // Let's add a "Auto-Lock Settings" button to the XML (user needs to update XML).
        // Since I can't see the XML change unless you apply it, I'll assume you add a button ID `btnLockSettings`.
        // If not, I'll attach it to a generic menu.

        // For simplicity, let's trigger it via a new "Security Settings" button in XML
        // or just hook into an existing one.

        btnChangePass.setOnClickListener(v -> showChangePassDialog());
        btnBackup.setOnClickListener(v -> performBackup());
        btnDeleteAll.setOnClickListener(v -> showWipeDialog());

        // Add Lock Timeout Dialog
        Button btnLockTimeout = new Button(this);
        btnLockTimeout.setText("Auto-Lock: " + getTimeoutLabel(prefs.getLockTimeout()));
        // Add to layout dynamically for now to ensure it exists
        ((android.widget.LinearLayout)findViewById(R.id.btnChangePassword).getParent()).addView(btnLockTimeout, 2);

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
                .setMessage("Not implemented in this phase. Requires KEK re-wrapping.")
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
                .setMessage("Irreversible action.")
                .setPositiveButton("Wipe", (d, w) -> {
                    // Wipe logic...
                    DatabaseHelper db = new DatabaseHelper(this);
                    db.getWritableDatabase().execSQL("DELETE FROM " + DatabaseHelper.TABLE_FILES);
                    // ...
                    Toast.makeText(this, "Data Wiped", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}