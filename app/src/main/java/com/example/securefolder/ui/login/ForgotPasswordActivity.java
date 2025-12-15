package com.example.securefolder.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.MainActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.KeyManager;
import com.example.securefolder.utils.SecurityUtils;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout layoutRecoveryInput, layoutResetPassword;
    private TextInputEditText etRecoveryCode, etNewPass, etConfirmNewPass;
    private Button btnVerify, btnSavePass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        layoutRecoveryInput = findViewById(R.id.layoutRecoveryInput);
        layoutResetPassword = findViewById(R.id.layoutResetPassword);

        etRecoveryCode = findViewById(R.id.etRecoveryCode);
        etNewPass = findViewById(R.id.etNewPassword);
        etConfirmNewPass = findViewById(R.id.etConfirmNewPassword);

        btnVerify = findViewById(R.id.btnVerifyCode);
        btnSavePass = findViewById(R.id.btnSavePassword);

        // STEP 1: Verify Recovery Code
        btnVerify.setOnClickListener(v -> {
            String code = etRecoveryCode.getText().toString().trim();
            if (code.isEmpty()) {
                etRecoveryCode.setError("Enter code");
                return;
            }

            // Attempt to unlock vault with the code
            boolean success = KeyManager.unlockWithRecovery(this, code);
            if (success) {
                Toast.makeText(this, "Recovery Successful", Toast.LENGTH_SHORT).show();
                layoutRecoveryInput.setVisibility(View.GONE);
                layoutResetPassword.setVisibility(View.VISIBLE);
            } else {
                etRecoveryCode.setError("Invalid Recovery Code");
            }
        });

        // STEP 2: Set New Password
        btnSavePass.setOnClickListener(v -> {
            String p1 = etNewPass.getText().toString();
            String p2 = etConfirmNewPass.getText().toString();

            if (p1.isEmpty() || !p1.equals(p2)) {
                etConfirmNewPass.setError("Passwords do not match");
                return;
            }
            if (!SecurityUtils.isValidPassword(p1, SecurityUtils.POLICY_RECOMMENDED)) {
                etNewPass.setError("Password too weak (Min 6 chars)");
                return;
            }

            // Re-encrypt the DEK with the new password
            boolean changed = KeyManager.changePassword(this, p1);
            if (changed) {
                Toast.makeText(this, "Password Reset Complete", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("IS_LOGGED_IN", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Error resetting password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}