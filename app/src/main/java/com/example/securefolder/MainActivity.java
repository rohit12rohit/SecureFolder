package com.example.securefolder;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.ui.HomeActivity;
import com.example.securefolder.ui.login.LoginActivity;
import com.example.securefolder.ui.login.SignupActivity;
import com.example.securefolder.utils.AppPreferences;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No Layout needed, this is just a router.

        AppPreferences prefs = new AppPreferences(this);
        Intent intent;

        if (prefs.isSetupDone()) {
            // User has an account -> Login
            intent = new Intent(this, LoginActivity.class);
        } else {
            // New User -> Signup
            intent = new Intent(this, SignupActivity.class);
        }

        startActivity(intent);
        finish();
    }
}