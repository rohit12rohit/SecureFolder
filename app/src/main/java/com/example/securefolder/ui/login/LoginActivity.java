package com.example.securefolder.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.MainActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.AppPreferences;
import com.example.securefolder.utils.KeyManager;
import com.example.securefolder.utils.SecurityUtils;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etPassword;
    private AppPreferences appPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        appPreferences = new AppPreferences(this);
        etPassword = findViewById(R.id.etLoginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String inputPass = etPassword.getText().toString();

        if (inputPass.isEmpty()) {
            etPassword.setError("Enter Password");
            return;
        }

        String storedHash = appPreferences.getPasswordHash();
        String storedSalt = appPreferences.getPasswordSalt();

        // Check password against stored hash
        String inputHash = SecurityUtils.hashPassword(inputPass, storedSalt);

        if (storedHash != null && storedHash.equals(inputHash)) {
            // 1. Reset Fail Count
            appPreferences.resetFailedAttempts();

            // 2. GENERATE MASTER KEY FOR SESSION (Important!)
            // This allows us to decrypt files during this session
            KeyManager.generateMasterKey(this, inputPass);

            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_LOGGED_IN", true);
            startActivity(intent);
            finish();
        } else {
            appPreferences.incrementFailedAttempts();
            int failed = appPreferences.getFailedAttempts();
            etPassword.setError("Incorrect Password. Failed attempts: " + failed);
        }
    }
}