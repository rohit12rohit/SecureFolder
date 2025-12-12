package com.example.securefolder.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.SecurityUtils;
import com.google.android.material.textfield.TextInputEditText;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etPassword, etConfirmPassword;
    private RadioGroup radioGroup;
    private AppPreferences appPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        appPreferences = new AppPreferences(this);
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

        // Determine Policy
        int policy = SecurityUtils.POLICY_RECOMMENDED;
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rbSimple) policy = SecurityUtils.POLICY_SIMPLE;
        else if (selectedId == R.id.rbStrong) policy = SecurityUtils.POLICY_STRONG;

        // Validate
        if (!SecurityUtils.isValidPassword(pass, policy)) {
            etPassword.setError("Password does not meet the selected policy requirements.");
            return;
        }

        // Save Credentials
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword(pass, salt);

        if (hash != null) {
            appPreferences.savePasswordData(hash, salt);
            // Move to Recovery Code Screen
            Intent intent = new Intent(this, RecoveryCodeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}