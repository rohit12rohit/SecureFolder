package com.example.securefolder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.ui.login.LoginActivity;
import com.example.securefolder.utils.KeyManager;

/**
 * All activities inside the Vault MUST extend this class.
 * It enforces:
 * 1. Screenshot Prevention (FLAG_SECURE)
 * 2. Session Validity (Process Death Protection)
 */
public abstract class BaseSecureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // BLOCK SCREENSHOTS / RECENTS PREVIEW
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        validateSession();
    }

    /**
     * CRITICAL FIX:
     * Checks if the Master Key is still in memory.
     * If the OS killed the process in the background, the static KeyManager.key will be null.
     * We must redirect to Login immediately to prevent crashes.
     */
    private void validateSession() {
        if (KeyManager.getMasterKey() == null) {
            // Session lost due to Process Death or Timeout
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}