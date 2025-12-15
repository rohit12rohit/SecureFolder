package com.example.securefolder.ui.login;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.example.securefolder.MainActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.IntruderCapture;
import com.example.securefolder.utils.KeyManager;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etPassword;
    private AppPreferences appPreferences;
    private IntruderCapture intruderCapture;

    // Permission Launcher: This pops up the "Allow Camera?" dialog
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initIntruderCam();
                } else {
                    Toast.makeText(this, "Camera denied. Intruder Selfie disabled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        appPreferences = new AppPreferences(this);
        etPassword = findViewById(R.id.etLoginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnForgot = findViewById(R.id.btnForgot);
        Button btnBiometric = findViewById(R.id.btnBiometric);

        // Check Setup
        if (!appPreferences.isSetupDone()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        // REQUEST CAMERA PERMISSION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initIntruderCam();
        } else {
            // This line triggers the popup
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        // Setup Biometrics
        setupBiometrics(btnBiometric);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnForgot.setOnClickListener(v -> showRecoveryDialog());
    }

    private void initIntruderCam() {
        // Find the preview view. Make sure you updated activity_login.xml from Part 2!
        intruderCapture = new IntruderCapture(this, this, findViewById(R.id.cameraPreview));
    }

    private void setupBiometrics(Button btnBio) {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(LoginActivity.this, "Biometrics Verified. Please enter password to decrypt Vault.", Toast.LENGTH_LONG).show();
            }
        });

        // Only show if hardware available
        btnBio.setVisibility(View.VISIBLE);
        btnBio.setOnClickListener(v -> {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Login")
                    .setSubtitle("Log in using your biometric credential")
                    .setNegativeButtonText("Use Password")
                    .build();
            biometricPrompt.authenticate(promptInfo);
        });
    }

    private void attemptLogin() {
        String inputPass = etPassword.getText().toString();
        if (inputPass.isEmpty()) {
            etPassword.setError("Enter Password");
            return;
        }

        boolean success = KeyManager.unlockVault(this, inputPass);

        if (success) {
            appPreferences.resetFailedAttempts();
            Toast.makeText(this, "Vault Unlocked", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            appPreferences.incrementFailedAttempts();
            int failed = appPreferences.getFailedAttempts();
            etPassword.setError("Incorrect Password. Failed: " + failed);
            etPassword.setText("");

            // TRIGGER INTRUDER SELFIE
            if (failed >= 3 && intruderCapture != null) {
                intruderCapture.takeSelfie();
            }
        }
    }

    private void showRecoveryDialog() {
        if (!appPreferences.isRecoveryEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Recovery Unavailable")
                    .setMessage("Recovery was not set up for this vault. Data cannot be restored.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputCode = new EditText(this);
        inputCode.setHint("Recovery Code");
        layout.addView(inputCode);

        final EditText inputNewPass = new EditText(this);
        inputNewPass.setHint("New Password");
        layout.addView(inputNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Reset", (dialog, which) -> {
            String code = inputCode.getText().toString().trim();
            String newPass = inputNewPass.getText().toString().trim();

            if (code.isEmpty() || newPass.length() < 6) {
                Toast.makeText(this, "Invalid Code or Weak Password", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean recovered = KeyManager.resetPasswordWithRecovery(this, code, newPass);
            if (recovered) {
                Toast.makeText(this, "Password Reset! Please Login.", Toast.LENGTH_LONG).show();
                appPreferences.resetFailedAttempts();
            } else {
                Toast.makeText(this, "Invalid Recovery Code.", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}