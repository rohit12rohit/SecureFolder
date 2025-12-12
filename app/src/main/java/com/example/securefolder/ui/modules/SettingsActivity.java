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
import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        progressBar = findViewById(R.id.progressBar);
        Button btnChangePass = findViewById(R.id.btnChangePassword);
        Button btnBackup = findViewById(R.id.btnBackup);
        Button btnDeleteAll = findViewById(R.id.btnDeleteAll);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnChangePass.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Change Password")
                    .setMessage("Changing password requires re-encrypting all data. Feature coming in Phase 8.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        btnBackup.setOnClickListener(v -> performBackup());

        btnDeleteAll.setOnClickListener(v -> showWipeDialog());

        btnAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Secure Folder")
                    .setMessage("Vault Technology: AES-256 GCM\nHash: PBKDF2\n\nYour data is stored locally.")
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    private void performBackup() {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Preparing Encrypted Backup...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String path = BackupHelper.createSecureBackup(this);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (path != null) {
                    new AlertDialog.Builder(this)
                            .setTitle("Backup Successful")
                            .setMessage("Saved to Downloads:\n" + new File(path).getName() + "\n\nKEEP THIS FILE SAFE. You need your current password to restore it.")
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
                .setTitle("DANGER: Wipe All Data?")
                .setMessage("This will permanently delete all photos, videos, notes, and passwords. This cannot be undone.")
                .setPositiveButton("Wipe Everything", (dialog, which) -> wipeAllData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void wipeAllData() {
        DatabaseHelper db = new DatabaseHelper(this);
        db.getWritableDatabase().delete(DatabaseHelper.TABLE_FILES, null, null);
        db.getWritableDatabase().delete(DatabaseHelper.TABLE_NOTES, null, null);
        db.getWritableDatabase().delete(DatabaseHelper.TABLE_PASSWORDS, null, null);

        File vaultDir = new File(getExternalFilesDir(null), "Vault");
        deleteRecursive(vaultDir);

        new AppPreferences(this).clearAllData();
        Toast.makeText(this, "Vault Wiped. App will close.", Toast.LENGTH_LONG).show();
        finishAffinity();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.exists() && fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        if (fileOrDirectory.exists()) fileOrDirectory.delete();
    }
}