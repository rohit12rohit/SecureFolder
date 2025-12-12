package com.example.securefolder.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.KeyManager;
import com.example.securefolder.utils.SecurityUtils;
import com.google.android.material.textfield.TextInputEditText;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etPassword, etConfirmPassword;
    private RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        radioGroup = findViewById(R.id.radioGroupPolicy);
        Button btnSignup = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(v -> handleSignup());
    }

    private void handleSignup() {
        String pass = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (pass.isEmpty()) {
            etPassword.setError("Password required");
            return;
        }

        if (!pass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Validate Policy
        int policy = SecurityUtils.POLICY_RECOMMENDED;
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rbSimple) policy = SecurityUtils.POLICY_SIMPLE;
        else if (selectedId == R.id.rbStrong) policy = SecurityUtils.POLICY_STRONG;

        if (!SecurityUtils.isValidPassword(pass, policy)) {
            etPassword.setError("Password does not meet the selected policy requirements.");
            return;
        }

        // INITIALIZE VAULT (Core Arch Change)
        boolean success = KeyManager.setupVault(this, pass);

        if (success) {
            Toast.makeText(this, "Vault Initialized Successfully", Toast.LENGTH_SHORT).show();
            // Go to Recovery
            Intent intent = new Intent(this, RecoveryCodeActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Error initializing encryption", Toast.LENGTH_LONG).show();
        }
    }
}