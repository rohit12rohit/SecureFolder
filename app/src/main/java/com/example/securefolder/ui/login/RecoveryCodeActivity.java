package com.example.securefolder.ui.login;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.MainActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.KeyManager;
import com.example.securefolder.utils.SecurityUtils;
import java.io.File;
import java.io.FileWriter;

public class RecoveryCodeActivity extends AppCompatActivity {

    private String recoveryCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery_code);

        TextView tvCode = findViewById(R.id.tvRecoveryCode);
        Button btnCopy = findViewById(R.id.btnCopy);
        Button btnSave = findViewById(R.id.btnSaveFile);
        Button btnFinish = findViewById(R.id.btnFinish);

        // 1. Generate Code
        recoveryCode = SecurityUtils.generateRecoveryCode();
        tvCode.setText(recoveryCode);

        // 2. SECURE THE VAULT WITH THIS CODE (Crucial Fix)
        // Since setupVault() was just called in SignupActivity, KeyManager.cachedMasterKey is populated.
        boolean recoverySetupSuccess = KeyManager.setupRecovery(this, recoveryCode);

        if (!recoverySetupSuccess) {
            Toast.makeText(this, "CRITICAL ERROR: Failed to setup recovery.", Toast.LENGTH_LONG).show();
            // In a real app, you might want to abort or retry
        }

        // Copy Logic
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Recovery Code", recoveryCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Save File Logic
        btnSave.setOnClickListener(v -> saveCodeToFile());

        // Finish Logic
        btnFinish.setOnClickListener(v -> {
            new AppPreferences(this).setSetupDone(true);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void saveCodeToFile() {
        try {
            File file = new File(getExternalFilesDir(null), "SecureFolder-Recovery.txt");
            FileWriter writer = new FileWriter(file);
            writer.write("IMPORTANT: SECURE FOLDER RECOVERY CODE\n");
            writer.write("Keep this safe. If you lose your password, you lose your data.\n\n");
            writer.write("Code: " + recoveryCode);
            writer.flush();
            writer.close();
            Toast.makeText(this, "Saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}